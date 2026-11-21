package org.rsmod.content.quest.area.varrock.shieldofarrav

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.generic.locs.autotravel.AutoTraveller
import org.rsmod.content.generic.locs.autotravel.TravelKind
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Black Arm hideout stairs land where capture 1710 puts the player, next to the cupboard
 * room. Every other placement of these staircases keeps the generic landing.
 */
class BlackArmStairs @Inject constructor(private val traveller: AutoTraveller) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(STAIRS) { climb(it.loc, up = true, from = STAIRS_BOTTOM, to = LANDING_TOP) }
        onOpLoc1(STAIRS_TOP) { climb(it.loc, up = false, from = STAIRS_UPPER, to = LANDING_BOTTOM) }
    }

    private suspend fun ProtectedAccess.climb(
        stairs: BoundLocInfo,
        up: Boolean,
        from: CoordGrid,
        to: CoordGrid,
    ) {
        if (stairs.coords != from) {
            val kind = if (up) TravelKind.Up else TravelKind.Down
            with(traveller) { travel(stairs, kind, up) }
            return
        }
        arriveDelay()
        telejump(to)
    }

    private companion object {
        const val STAIRS = "loc.fai_varrock_stairs"
        const val STAIRS_TOP = "loc.fai_varrock_stairs_top"
        val STAIRS_BOTTOM = CoordGrid(3188, 3389, 0)
        val STAIRS_UPPER = CoordGrid(3188, 3390, 1)
        val LANDING_TOP = CoordGrid(3188, 3392, 1)
        val LANDING_BOTTOM = CoordGrid(3188, 3388, 0)
    }
}
