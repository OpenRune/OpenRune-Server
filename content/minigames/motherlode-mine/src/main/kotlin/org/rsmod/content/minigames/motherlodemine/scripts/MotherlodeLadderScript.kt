package org.rsmod.content.minigames.motherlodemine.scripts

import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.isMotherlodeUpperFloor
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.motherlodeInRestrictedArea
import org.rsmod.content.minigames.motherlodemine.configs.migrateMotherlodeUnlocks
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeUpperFloorUnlocked
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// Unlock and positional ladder varbit are independent; derive position again on login.
class MotherlodeLadderScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(LADDER_BOTTOM_ACTIVE) { climbUp(it.vis) }
        onOpLoc1(LADDER_TOP_ACTIVE) { climbDown(it.vis) }
        onPlayerLogin {
            player.migrateMotherlodeUnlocks()
            val upstairs = player.onMotherlodeUpperFloor()
            if (player.motherlodeInRestrictedArea != upstairs) {
                player.motherlodeInRestrictedArea = upstairs
            }
        }
    }

    private suspend fun ProtectedAccess.climbUp(ladder: BoundLocInfo) {
        arriveDelay()
        if (!player.motherlodeUpperFloorUnlocked) {
            mes("You need to buy access to the upper floor from Prospector Percy first.")
            return
        }
        if (climb(ladder, CoordGrid(3755, 5675, 0))) {
            player.motherlodeInRestrictedArea = true
        }
    }

    private suspend fun ProtectedAccess.climbDown(ladder: BoundLocInfo) {
        arriveDelay()
        if (climb(ladder, CoordGrid(3755, 5673, 0))) {
            player.motherlodeInRestrictedArea = false
        }
    }

    internal suspend fun ProtectedAccess.climb(ladder: BoundLocInfo, dest: CoordGrid): Boolean {
        player.abortRoute()
        player.resetFaceEntity()
        player.resetPendingFaceSquare()
        if (ladder.id == LADDER_BOTTOM_ACTIVE.asRSCM() && player.coords != ladder.coords) {
            if (player.coords.level != ladder.coords.level || player.coords.chebyshevDistance(ladder.coords) != 1) {
                return false
            }
            // Normal routing reaches the ladder; only its blocked landing needs a scripted step.
            forcedWalk(ladder.coords, crossTiles = 1)
        }
        val angle = player.calculateAngle(ladder.coords, ladder.adjustedWidth, ladder.adjustedLength)
            ?: if (dest.z > ladder.coords.z) Direction.North.angle else Direction.South.angle
        // The lower landing shares the ladder tile, so facing its square can have no bearing.
        player.resetPendingFaceSquare()
        player.pendingFaceAngle = EntityFaceAngle(angle)
        delay(1)
        anim(CLIMB_ANIM)
        delay(1)
        telejump(dest, TeleportType.Exempt)
        return true
    }

    private companion object {
        private const val LADDER_BOTTOM_ACTIVE = "loc.motherlode_ladder_bottom_active"
        private const val LADDER_TOP_ACTIVE = "loc.motherlode_ladder_top_active"

        private const val CLIMB_ANIM = "seq.human_reachforladder"

        private fun Player.onMotherlodeUpperFloor(): Boolean =
            coords.isMotherlodeUpperFloor() &&
                coords.x in MotherlodeConstants.SCAN_X1..MotherlodeConstants.SCAN_X2 &&
                coords.z in MotherlodeConstants.SCAN_Z1..MotherlodeConstants.SCAN_Z2
    }
}
