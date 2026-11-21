package org.rsmod.content.minigames.sepulchre

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import kotlin.math.sign
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.HallowedStrangeTilesRow
import org.rsmod.game.MapClock
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal class StrangeTile(
    val coords: CoordGrid,
    val destination: CoordGrid,
    val forward: Boolean,
) {
    val ready: SpotanimType
        get() = spotanim(if (forward) READY_FORWARD else READY_BACKWARD)

    val telegraph: SpotanimType
        get() = spotanim(if (forward) TELEGRAPH_FORWARD else TELEGRAPH_BACKWARD)

    val activate: SpotanimType
        get() = spotanim(if (forward) ACTIVATE_FORWARD else ACTIVATE_BACKWARD)

    val arrive: SpotanimType
        get() = spotanim(if (forward) ARRIVE_FORWARD else ARRIVE_BACKWARD)

    private fun spotanim(name: String): SpotanimType = SpotanimType(name.asRSCM(RSCMType.SPOTANIM))

    companion object {
        fun loadAll(): Map<Int, List<StrangeTile>> =
            HallowedStrangeTilesRow.all().associate { row ->
                val tiles =
                    pairs(row.forward, forward = true) + pairs(row.backward, forward = false)
                row.floor to tiles
            }

        private fun pairs(tiles: List<CoordGrid>, forward: Boolean): List<StrangeTile> =
            tiles.chunked(2).map { (coords, destination) ->
                StrangeTile(coords, destination, forward)
            }

        private const val READY_FORWARD = "spotanim.hallowed_telepad_ready_forward"
        private const val READY_BACKWARD = "spotanim.hallowed_telepad_ready_backward"
        private const val TELEGRAPH_FORWARD = "spotanim.hallowed_telepad_telegraph_forward"
        private const val TELEGRAPH_BACKWARD = "spotanim.hallowed_telepad_telegraph_backward"
        private const val ACTIVATE_FORWARD = "spotanim.hallowed_telepad_activate_forward"
        private const val ACTIVATE_BACKWARD = "spotanim.hallowed_telepad_activate_backward"
        private const val ARRIVE_FORWARD = "spotanim.hallowed_telepad_activate_forward_rev"
        private const val ARRIVE_BACKWARD = "spotanim.hallowed_telepad_activate_backward_rev"
    }
}

internal object StrangeTileTiming {
    const val COOLDOWN = 6
    const val LIGHT_CHANCE = 25
    const val IMMUNE_TICKS = 3

    fun isArmed(sinceReady: Int): Boolean = sinceReady == 0 || sinceReady == 1

    fun mayLight(sinceReady: Int): Boolean = sinceReady >= COOLDOWN
}

/**
 * The strange tiles of floors 2 to 5. They light up only while someone is timed on their floor: a
 * tile lights at random once its cooldown has passed, ready for a tick and then telegraphing for
 * one. Whoever ends either tick on it is teleported to its destination on the next and cannot be
 * sent again until two ticks after landing.
 */
class SepulchreStrangeTileScript
@Inject
constructor(
    private val runs: SepulchreRuns,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val random: GameRandom,
) : PluginScript() {
    private lateinit var floors: List<FloorTiles>

    override fun ScriptContext.startup() {
        floors = StrangeTile.loadAll().map { (floor, tiles) -> FloorTiles(floor, tiles) }
        onEvent<GameLifecycle.StartCycle> { floors.forEach(::light) }
        onEvent<GameLifecycle.LateCycle> { floors.forEach(::arm) }
        onPlayerQueueWithArgs<StrangeTile>(TELEPAD_QUEUE) { jump(it.args) }
    }

    private fun light(floor: FloorTiles) {
        if (runs.onFloor(RunPhase.Start, floor.number).isEmpty()) {
            floor.active = false
            return
        }
        val now = mapClock.cycle
        if (!floor.active) {
            floor.active = true
            floor.lastReady.fill(now - StrangeTileTiming.COOLDOWN)
        }
        for ((index, tile) in floor.tiles.withIndex()) {
            val since = now - floor.lastReady[index]
            if (since == 1) {
                worldRepo.spotanimMap(tile.telegraph, tile.coords)
            } else if (
                StrangeTileTiming.mayLight(since) &&
                    random.of(StrangeTileTiming.LIGHT_CHANCE) == 0
            ) {
                floor.lastReady[index] = now
                worldRepo.spotanimMap(tile.ready, tile.coords)
            }
        }
    }

    private fun arm(floor: FloorTiles) {
        if (!floor.active) {
            return
        }
        val now = mapClock.cycle - 1
        for (player in runs.onFloor(RunPhase.Late, floor.number)) {
            if (player.currentFloor != floor.number) {
                continue
            }
            if (player.telepadImmunity > 0) {
                player.telepadImmunity--
                continue
            }
            if (player.busy) {
                continue
            }
            val index = floor.indexOf(player.coords)
            if (index >= 0 && StrangeTileTiming.isArmed(now - floor.lastReady[index])) {
                player.strongQueue(TELEPAD_QUEUE, 1, floor.tiles[index])
            }
        }
    }

    private fun ProtectedAccess.jump(tile: StrangeTile) {
        if (player.busy) {
            return
        }
        val landing = landing(tile) ?: return
        worldRepo.spotanimMap(tile.activate, tile.coords)
        worldRepo.spotanimMap(tile.arrive, landing)
        teleport(landing, collision, TeleportType.Exempt)
        player.telepadImmunity = StrangeTileTiming.IMMUNE_TICKS
    }

    private fun landing(tile: StrangeTile): CoordGrid? {
        var at = tile.destination
        while (at != tile.coords) {
            if (!collision.isWalkBlocked(at)) {
                return at
            }
            at = at.translate((tile.coords.x - at.x).sign, (tile.coords.z - at.z).sign)
        }
        return null
    }

    private class FloorTiles(val number: Int, val tiles: List<StrangeTile>) {
        val lastReady = IntArray(tiles.size)
        var active = false
        private val byCoords = tiles.withIndex().associate { (index, tile) -> tile.coords to index }

        fun indexOf(coords: CoordGrid): Int = byCoords[coords] ?: -1
    }

    private companion object {
        const val TELEPAD_QUEUE = "queue.hallowed_telepad"
    }
}
