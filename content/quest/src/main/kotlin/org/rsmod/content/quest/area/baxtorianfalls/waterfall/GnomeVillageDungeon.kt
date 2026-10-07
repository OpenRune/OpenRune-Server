package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GolrieKey
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GolrieNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.ReadBook
import org.rsmod.content.quest.util.QuestDoors
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The hobgoblin caves beneath the Tree Gnome Village: the odd crate in the east room hides the
 * key to the gate Golrie has locked himself behind in the west room (the gate sits on the north
 * edge of 2515,9575; Golrie's room is north of it).
 */
class GnomeVillageDungeon
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val objRepo: ObjRepository,
    private val doors: QuestDoors,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(Crate) { searchCrate() }
        onOpLoc1(Gate) { openGate(it.loc) }
        onOpLocU(Gate, GolrieKey) { unlockGate(it.loc) }
    }

    private suspend fun ProtectedAccess.searchCrate() {
        anim(SearchSeq)
        delay(1)
        if (waterfall.stage(player) < ReadBook || GolrieKey in player.inv) {
            mes("You search the crate but find nothing of interest.")
            return
        }
        invAddOrDrop(objRepo, GolrieKey)
        objbox(GolrieKey, "You find a key in the crate.")
    }

    private suspend fun ProtectedAccess.openGate(gate: BoundLocInfo) {
        if (!gate.playerIsSouth(player.coords)) {
            mes("You open the gate and walk through.")
            passThrough(gate)
            return
        }
        if (GolrieKey in player.inv) {
            unlockGate(gate)
            return
        }
        soundSynth(LockedSound)
        when {
            waterfall.stage(player) == 0 -> {
                startDialogue {
                    chatNpcSpecific(
                        GolrieName,
                        GolrieNpc,
                        angry,
                        "What are you doing down here? Leave before you get yourself into trouble.",
                    )
                }
            }
            waterfall.isComplete(player) -> mesbox("Golrie has locked himself in.")
            else -> {
                startDialogue {
                    chatPlayer(worried, "Hello, are you okay?")
                    chatNpcSpecific(
                        GolrieName,
                        GolrieNpc,
                        happy,
                        "Oh, don't worry, I'm totally fine. I locked myself in here for " +
                            "protection, but I've left the key somewhere.",
                    )
                    chatPlayer(confused, "Okay... I'll have a look for a key.")
                }
            }
        }
    }

    private fun ProtectedAccess.unlockGate(gate: BoundLocInfo) {
        if (gate.playerIsSouth(player.coords)) {
            mes("You use the key to unlock the gate.")
        }
        passThrough(gate)
    }

    private fun ProtectedAccess.passThrough(gate: BoundLocInfo) {
        val across = gate.tileAcross(player.coords)
        doors.open(this, gate, Gate, GateSound)
        walk(across)
    }

    private companion object {
        const val Crate = "loc.golrie_crate_waterfall_quest"
        const val Gate = "loc.golrie_gate_waterfall_quest"
        const val GolrieName = "Golrie"
        const val SearchSeq = "seq.human_pickuptable"
        const val GateSound = "synth.door_open"
        const val LockedSound = "synth.irondoor_locked"
    }
}
