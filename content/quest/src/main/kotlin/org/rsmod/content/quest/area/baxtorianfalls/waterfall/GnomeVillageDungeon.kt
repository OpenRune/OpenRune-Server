package org.rsmod.content.quest.area.baxtorianfalls.waterfall

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GOLRIE_KEY
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.GOLRIE_NPC
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.READ_BOOK
import org.rsmod.content.quest.util.QuestDoors
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GnomeVillageDungeon
@Inject
constructor(
    private val waterfall: WaterfallQuest,
    private val objRepo: ObjRepository,
    private val doors: QuestDoors,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(CRATE) { searchCrate() }
        onOpLoc1(GATE) { openGate(it.loc) }
        onOpLocU(GATE, GOLRIE_KEY) { unlockGate(it.loc) }
    }

    private suspend fun ProtectedAccess.searchCrate() {
        anim(SEARCH_SEQ)
        delay(1)
        if (waterfall.stage(player) < READ_BOOK || GOLRIE_KEY in player.inv) {
            mes("You search the crate but find nothing of interest.")
            return
        }
        invAddOrDrop(objRepo, GOLRIE_KEY)
        objbox(GOLRIE_KEY, "You find a key in the crate.")
    }

    private suspend fun ProtectedAccess.openGate(gate: BoundLocInfo) {
        if (!gate.playerIsSouth(player.coords)) {
            mes("You open the gate and walk through.")
            passThrough(gate)
            return
        }
        if (GOLRIE_KEY in player.inv) {
            unlockGate(gate)
            return
        }
        soundSynth(LOCKED_SOUND)
        when {
            waterfall.stage(player) == 0 -> {
                startDialogue {
                    chatNpcSpecific(
                        GOLRIE_NAME,
                        GOLRIE_NPC,
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
                        GOLRIE_NAME,
                        GOLRIE_NPC,
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
        doors.open(this, gate, GATE, GATE_SOUND)
        walk(across)
    }

    private companion object {
        const val CRATE = "loc.golrie_crate_waterfall_quest"
        const val GATE = "loc.golrie_gate_waterfall_quest"
        const val GOLRIE_NAME = "Golrie"
        const val SEARCH_SEQ = "seq.human_pickuptable"
        const val GATE_SOUND = "synth.door_open"
        const val LOCKED_SOUND = "synth.irondoor_locked"
    }
}
