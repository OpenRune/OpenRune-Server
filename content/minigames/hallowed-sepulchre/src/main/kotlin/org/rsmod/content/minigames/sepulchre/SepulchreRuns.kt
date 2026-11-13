package org.rsmod.content.minigames.sepulchre

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.game.MapClock
import org.rsmod.game.entity.PathingEntity.Companion.INVALID_SLOT
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

internal enum class RunPhase {
    Start,
    Late,
}

/**
 * The players who are on a floor, so the per-tick handlers need not scan everyone online. Anyone
 * with a floor set is found in the floors' zones, or was logged in with one; they stay tracked
 * until the floor is cleared, so a player who left the floors with it still set is caught too.
 * Each phase groups them by floor once per tick.
 */
@Singleton
class SepulchreRuns
@Inject
constructor(private val registry: PlayerRegistry, private val mapClock: MapClock) {
    private val zones: List<ZoneKey> by lazy {
        SepulchreFloors.load().all.flatMap { ZoneKey.covering(it.southWest, it.northEast) }
    }
    private val tracked = LinkedHashSet<Player>()
    private val snapshots = Array(RunPhase.entries.size) { Snapshot() }

    internal fun trackLogin(player: Player) {
        if (player.currentFloor != 0) {
            tracked += player
        }
    }

    internal fun release(player: Player) {
        tracked -= player
    }

    internal fun players(phase: RunPhase): List<Player> = snapshot(phase).all

    internal fun onFloor(phase: RunPhase, floor: Int): List<Player> =
        snapshot(phase).byFloor[floor].orEmpty()

    private fun snapshot(phase: RunPhase): Snapshot {
        val snapshot = snapshots[phase.ordinal]
        if (snapshot.cycle == mapClock.cycle) {
            return snapshot
        }
        for (player in registry.findAll(zones)) {
            if (player.currentFloor != 0) {
                tracked += player
            }
        }
        tracked.removeIf { it.currentFloor == 0 || it.slotId == INVALID_SLOT }
        snapshot.cycle = mapClock.cycle
        snapshot.all = tracked.toList()
        snapshot.byFloor = snapshot.all.groupBy { it.currentFloor }
        return snapshot
    }

    private class Snapshot {
        var cycle = -1
        var all: List<Player> = emptyList()
        var byFloor: Map<Int, List<Player>> = emptyMap()
    }
}
