package org.rsmod.content.generic.locs.ladders

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc4
import org.rsmod.api.script.onOpLoc5
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocZoneKey
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Serves the "Top-Floor" and "Bottom-Floor" ops on stacked ladders, such as the Grand Tree's,
 * by finding how far the stack of ladders on the climber's tile reaches.
 */
class QuickFloorLadderScript @Inject constructor(private val locs: LocZoneStorage) : PluginScript() {
    override fun ScriptContext.startup() {
        for (type in ServerCacheManager.getObjects().values) {
            if (!type.isLadder()) continue
            val symbol = runCatching { RSCM.getReverseMapping(RSCMType.LOC, type.id) }.getOrNull() ?: continue
            for (op in 1..5) {
                val top = when (type.actions.getOpOrNull(op - 1)?.lowercase()) {
                    TOP_FLOOR -> true
                    BOTTOM_FLOOR -> false
                    else -> continue
                }
                bind(op, symbol) { climbTo(type, top) }
            }
        }
    }

    private fun ScriptContext.bind(op: Int, loc: String, action: suspend ProtectedAccess.() -> Unit) {
        when (op) {
            1 -> onOpLoc1(loc) { action() }
            2 -> onOpLoc2(loc) { action() }
            3 -> onOpLoc3(loc) { action() }
            4 -> onOpLoc4(loc) { action() }
            else -> onOpLoc5(loc) { action() }
        }
    }

    private suspend fun ProtectedAccess.climbTo(type: ObjectServerType, top: Boolean) {
        arriveDelay()
        val from = player.coords
        val hasLadder = { level: Int -> ladderNear(CoordGrid(from.x, from.z, level)) }
        val level = if (top) QuickFloor.top(from.level, hasLadder) else QuickFloor.bottom(from.level, hasLadder)
        if (level == from.level) {
            mes("The ladder doesn't go any further.")
            return
        }
        anim(RSCM.getReverseMapping(RSCMType.SEQ, type.param(params.climb_anim).id))
        delay(1)
        telejump(CoordGrid(from.x, from.z, level))
    }

    private fun ladderNear(centre: CoordGrid): Boolean {
        for (dx in -SEARCH_RADIUS..SEARCH_RADIUS) {
            for (dz in -SEARCH_RADIUS..SEARCH_RADIUS) {
                val coords = centre.translate(dx, dz)
                val zone = ZoneKey.from(coords)
                val key = LocZoneKey(ZoneGrid.from(coords), GROUND_LAYER)
                val packed = locs.mapLocs[zone, key] ?: continue
                val type = ServerCacheManager.getObject(LocEntity(packed).id) ?: continue
                if (type.isLadder()) return true
            }
        }
        return false
    }

    private fun ObjectServerType.isLadder(): Boolean = "ladder" in name.lowercase()

    private companion object {
        const val TOP_FLOOR = "top-floor"
        const val BOTTOM_FLOOR = "bottom-floor"
        const val GROUND_LAYER = 2
        const val SEARCH_RADIUS = 1
    }
}
