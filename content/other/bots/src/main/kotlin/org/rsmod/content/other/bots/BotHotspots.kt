package org.rsmod.content.other.bots

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.api.table.BotSpawnsRow
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid

/**
 * Populates a spawn row only while a real player is close to it, so the world can list many areas
 * while the server only ever runs the bots someone could actually see.
 *
 * A row wakes once a player comes within [WAKE_MARGIN] tiles of its edge - far enough out that the
 * bots are already in place before they are drawn, and close enough that a teleport straight into
 * the area fills it on arrival. It sleeps once nobody has been near it, or near any of its bots,
 * for [SLEEP_TICKS].
 */
@Singleton
class BotHotspots
@Inject
constructor(private val players: PlayerList, private val world: BotWorld, config: ServerConfig) {
    private val hotspots by lazy { BotSpawnsRow.all().mapIndexed(::Hotspot) }
    private val allowed = config.gameplay.botsEnabled

    val awake: Int
        get() = hotspots.count { it.awake }

    val total: Int
        get() = hotspots.size

    var enabled = allowed
        private set

    fun enable(): Boolean {
        enabled = allowed
        return enabled
    }

    fun disable() {
        enabled = false
        sleepAll()
    }

    fun tick() {
        if (!enabled) {
            return
        }
        for (hotspot in hotspots) {
            update(hotspot)
        }
    }

    fun sleepAll() {
        for (hotspot in hotspots) {
            hotspot.awake = false
            hotspot.idleTicks = 0
        }
        world.despawnAll()
    }

    private fun update(hotspot: Hotspot) {
        val row = hotspot.row
        if (!hotspot.awake) {
            if (isPlayerNear(row.coords, row.radius + WAKE_MARGIN)) {
                wake(hotspot)
            }
            return
        }
        if (isPlayerNear(row.coords, row.radius + KEEP_MARGIN) || isBotSeen(hotspot)) {
            hotspot.idleTicks = 0
            return
        }
        if (++hotspot.idleTicks >= SLEEP_TICKS) {
            hotspot.awake = false
            hotspot.idleTicks = 0
            world.despawn(hotspot.id)
        }
    }

    private fun wake(hotspot: Hotspot) {
        val row = hotspot.row
        val activity = BotActivity.valueOf(row.activity)
        hotspot.awake = true
        repeat(row.count) {
            world.spawn(activity, row.coords, row.radius, row.styles, row.bank, hotspot.id)
        }
    }

    private fun isPlayerNear(coords: CoordGrid, distance: Int): Boolean =
        players.any { it.coords.chebyshevDistance(coords) <= distance }

    private fun isBotSeen(hotspot: Hotspot): Boolean =
        players.any { world.isAnyNear(hotspot.id, it.coords, SEEN_DISTANCE) }

    private class Hotspot(val id: Int, val row: BotSpawnsRow) {
        var awake = false
        var idleTicks = 0
    }

    private companion object {
        private const val WAKE_MARGIN = 32
        private const val KEEP_MARGIN = 48
        private const val SEEN_DISTANCE = 32
        private const val SLEEP_TICKS = 100
    }
}
