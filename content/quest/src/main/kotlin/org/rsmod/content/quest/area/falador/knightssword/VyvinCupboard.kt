package org.rsmod.content.quest.area.falador.knightssword

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.PORTRAIT
import org.rsmod.content.quest.area.falador.knightssword.KnightsSwordQuest.Companion.STAGE_LOOKING_FOR_PORTRAIT
import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VyvinCupboard
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val objRepo: ObjRepository,
    private val knightsSword: KnightsSwordQuest,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(SHUT) { open(it.loc) }
        onOpLoc1(OPEN) { search() }
        onOpLoc2(OPEN) { shut(it.loc) }
    }

    // Anim and synth on the click tick; the loc changes a tick later.
    private suspend fun ProtectedAccess.open(cupboard: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_SEQ)
        soundSynth(OPEN_SYNTH)
        delay(1)
        locRepo.change(cupboard, OPEN, DURATION)
    }

    private suspend fun ProtectedAccess.shut(cupboard: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_SEQ)
        delay(1)
        locRepo.change(cupboard, SHUT, DURATION)
    }

    private suspend fun ProtectedAccess.search() {
        arriveDelay()
        val watching = vyvinWatching()
        if (watching != null) {
            // TODO(content): [verify:in_game] when Sir Vyvin stops the search; the wiki says
            // "if he is inside the room and looking towards the player".
            startDialogue(watching) {
                chatNpc(
                    angry,
                    "HEY! Just WHAT do you THINK you are DOING??? STAY OUT of MY cupboard!",
                )
                mesbox("Maybe you need to get Sir Vyvin further away from his cupboard...")
            }
            return
        }
        // UNVERIFIED: stage 6 gives a portrait like stages 5 and 7 (guess)
        val canTake =
            with(knightsSword) { stage(player) } >= STAGE_LOOKING_FOR_PORTRAIT &&
                inv.count(PORTRAIT) == 0
        if (!canTake) {
            // UNVERIFIED: same box before stage 5 (guess)
            mesbox("There is just a load of junk in here.")
            return
        }
        mesbox("You find a small portrait in here which you take.")
        invAddOrDrop(objRepo, PORTRAIT)
    }

    private fun ProtectedAccess.vyvinWatching(): Npc? =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).firstOrNull {
            it.isType(SIR_VYVIN) &&
                it.coords.level == player.coords.level &&
                lineOfSight(it.coords, player.coords)
        }

    private companion object {
        const val SHUT = "loc.vyvincupboardshut"
        const val OPEN = "loc.vyvincupboardopen"
        const val SIR_VYVIN = "npc.sir_vyvin"
        const val OPEN_SEQ = "seq.human_openchest"
        const val OPEN_SYNTH = "synth.cupboard_open"
        const val DURATION = 500
    }
}
