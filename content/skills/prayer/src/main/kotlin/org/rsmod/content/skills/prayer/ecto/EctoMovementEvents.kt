package org.rsmod.content.skills.prayer.ecto

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EctoMovementEvents @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.ahoy_trapdoor") { openTrapdoor(it.vis) }
        onOpLoc2("loc.ahoy_trapdoor_open") { closeTrapdoor(it.vis) }
        onOpLoc1("loc.ahoy_trapdoor_open") { climbTrapdoorDown() }
        onOpLoc1("loc.ahoy_ladder_from_cellar") { telejump(CoordGrid(3654, 3519, 0)) }
        onOpLoc1("loc.ahoy_cavern_stairs_top") { climbStairsDown() }
        onOpLoc1("loc.ahoy_cavern_stairs") { climbStairsUp() }
        onOpLoc1("loc.ahoy_tower_stairs_lv1") { telejump(CoordGrid(3666, 3522, 1)) }
        onOpLoc1("loc.ahoy_tower_stairs_lv1_top") { telejump(CoordGrid(3666, 3517, 0)) }
    }

    private suspend fun ProtectedAccess.openTrapdoor(loc: BoundLocInfo) {
        locChange(loc, "loc.ahoy_trapdoor_open")
    }

    private suspend fun ProtectedAccess.closeTrapdoor(loc: BoundLocInfo) {
        locChange(loc, "loc.ahoy_trapdoor")
    }

    private suspend fun ProtectedAccess.climbTrapdoorDown() {
        anim("seq.human_pickupfloor")
        delay(1)
        telejump(CoordGrid(3669, 9888, 3))
    }

    private fun ProtectedAccess.climbStairsDown() {
        val dest = when (player.coords.level) {
            3 -> CoordGrid(3688, 9888, 2)
            2 -> CoordGrid(3675, 9888, 1)
            1 -> CoordGrid(3683, 9888, 0)
            else -> null
        }
        if (dest == null) {
            return
        }
        telejump(dest)
    }

    private fun ProtectedAccess.climbStairsUp() {
        val dest = when (player.coords.level) {
            0 -> CoordGrid(3687, 9888, 1)
            1 -> CoordGrid(3671, 9888, 2)
            2 -> CoordGrid(3692, 9888, 3)
            else -> null
        }
        if (dest == null) {
            return
        }
        telejump(dest)
    }

    private fun locChange(loc: BoundLocInfo, next: String) {
        val duration = Int.MAX_VALUE
        locRepo.del(loc, duration)
        locRepo.add(loc.coords, next, duration, loc.angle, loc.shape)
    }
}
