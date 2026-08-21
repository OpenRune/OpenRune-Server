package org.rsmod.content.skills.shootingstars.scripts

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.area.checker.wildernessLevel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.skills.shootingstars.MINING_STAR
import org.rsmod.content.skills.shootingstars.ShootingStarManager
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StarTeleportScript
@Inject
constructor(
    private val stars: ShootingStarManager,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ITEM) { breakTablet(it.slot) }
    }

    private suspend fun ProtectedAccess.breakTablet(slot: Int) {
        if (!stars.active) {
            mes("There is currently no shooting star.")
            return
        }
        val dest = stars.teleportDestination()
        if (dest == null) {
            mes("There is currently no safe place to land near the shooting star.")
            return
        }

        if (!confirmWildernessTeleport(dest)) {
            return
        }

        if (invDel(inv, ITEM, count = 1, slot = slot).failure) {
            return
        }

        // Clicking the tablet already replaces the mining interaction; still clear anim/state.
        if (player.attr[MINING_STAR] == true) {
            player.attr[MINING_STAR] = false
            resetAnim()
        }

        anim(BREAK_ANIM)
        spotanim(BREAK_SPOTANIM, height = 92)
        delay(TELEPORT_DELAY)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.confirmWildernessTeleport(dest: CoordGrid): Boolean {
        val level = dest.wildernessLevel(areaChecker)
        if (level <= 0) {
            return true
        }

        mesbox(
            "<col=7f0000>Warning!</col> The shooting star is in level $level " +
                "<col=7f0000>Wilderness</col>. Other players will be able to attack you there.",
        )
        val confirm =
            choice2(
                "Yes, teleport anyway.",
                true,
                "No, I've changed my mind.",
                false,
                title = "Teleport into the Wilderness?",
            )
        return confirm
    }

    private companion object {
        private const val ITEM = "obj.poh_tablet_shootingstar"
        private const val BREAK_ANIM = "seq.poh_smash_magic_tablet"
        private const val BREAK_SPOTANIM = "spotanim.poh_absorb_tablet_magic"
        private const val TELEPORT_DELAY = 3
    }
}
