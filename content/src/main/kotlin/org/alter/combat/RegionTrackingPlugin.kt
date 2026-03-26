package org.alter.combat

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.WorldTickEvent

/**
 * Tracks when players enter new regions so that the 10-minute NPC aggro timer
 * can be maintained. On each world tick every online player is checked; if
 * their current region differs from the last-seen region, the entry tick is
 * recorded in [Player.regionEntryTicks] and the cached region is updated.
 */
class RegionTrackingPlugin : PluginEvent() {

    /** Maps player index → the region ID we last saw them standing in. */
    private val lastRegionByIndex = HashMap<Int, Int>()

    override fun init() {
        onEvent<WorldTickEvent> {
            val currentTick = tickCount
            world.players.forEach { player ->
                val currentRegion = player.tile.regionId
                val lastRegion = lastRegionByIndex[player.index]
                if (lastRegion != currentRegion) {
                    player.regionEntryTicks[currentRegion] = currentTick
                    lastRegionByIndex[player.index] = currentRegion
                }
            }
        }
    }
}
