package org.rsmod.content.minigames.motherlodemine.scripts

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.content.minigames.motherlodemine.MotherlodeMachine
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeStrutScript
@Inject
constructor(private val machine: MotherlodeMachine, private val invisibleLvls: InvisibleLevels) : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.StartCycle> { machine.tick() }
        onOpLoc1(MotherlodeConstants.STRUT_BROKEN_LOC) { repair(it.loc) }
    }

    internal suspend fun ProtectedAccess.repair(strut: BoundLocInfo) {
        arriveDelay()

        if (!hasHammer()) {
            mes("You need a hammer to repair this.")
            return
        }
        if (!machine.isStrutBroken(strut.coords)) {
            mes("This strut has already been repaired.")
            return
        }

        var elapsed = 0
        try {
            while (machine.isStrutBroken(strut.coords) && hasHammer()) {
                if (elapsed % REPAIR_ANIM_CADENCE == 0) {
                    anim(repairAnim())
                    soundSynth("synth.hammer_wood")
                }
                delay(1)
                elapsed++
                if (!machine.isStrutBroken(strut.coords) || !hasHammer()) return

                val success =
                    statRandom(
                        "stat.smithing",
                        MotherlodeConstants.STRUT_REPAIR_LOW,
                        MotherlodeConstants.STRUT_REPAIR_HIGH,
                        invisibleLvls,
                    )
                if (success && machine.repairStrut(strut.coords)) {
                    val xp = MotherlodeConstants.STRUT_REPAIR_XP_MULTIPLIER * player.smithingLvl
                    statAdvance("stat.smithing", xp)
                    mes("You repair the strut.")
                    return
                }
            }
        } finally {
            resetAnim()
        }
    }

    private fun ProtectedAccess.hasHammer(): Boolean =
        MotherlodeConstants.HAMMER_TYPES.any { it in inv || it in player.worn }

    private fun ProtectedAccess.repairAnim(): String {
        val hasImcando =
            "obj.imcando_hammer" in inv ||
                "obj.imcando_hammer" in player.worn ||
                "obj.imcando_hammer_offhand" in player.worn
        return if (hasImcando) {
            MotherlodeConstants.STRUT_REPAIR_ANIM_IMCANDO
        } else {
            MotherlodeConstants.STRUT_REPAIR_ANIM
        }
    }

    private companion object {
        // Two ticks per hammer swing; per-tick resets would clip the sequence.
        const val REPAIR_ANIM_CADENCE = 2
    }
}
