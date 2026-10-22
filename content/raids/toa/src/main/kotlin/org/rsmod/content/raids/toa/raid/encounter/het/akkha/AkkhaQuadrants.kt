package org.rsmod.content.raids.toa.raid.encounter.het.akkha

import kotlin.math.abs
import org.rsmod.api.player.lockOverheads
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal enum class Quadrant(
    val gfx: String,
    val gfxHeight: Int,
    val origin: CoordGrid,
    val trailOrb: String,
) {
    NE(AkkhaSpots.DARKNESS, 0, CoordGrid(3681, 5408, 1), AkkhaNpcs.TRAIL_DARKNESS),
    SE(AkkhaSpots.LIGHTNING, 0, CoordGrid(3681, 5407, 1), AkkhaNpcs.TRAIL_LIGHTNING),
    SW(AkkhaSpots.FREEZE, 0, CoordGrid(3680, 5407, 1), AkkhaNpcs.TRAIL_FREEZE),
    NW(AkkhaSpots.BURN, 124, CoordGrid(3680, 5408, 1), AkkhaNpcs.TRAIL_BURN);

    companion object {
        private val CENTRE = CoordGrid(3681, 5408, 1)

        fun of(static: CoordGrid): Quadrant {
            val east = static.x >= CENTRE.x
            val north = static.z >= CENTRE.z
            return when {
                east && north -> NE
                east -> SE
                north -> NW
                else -> SW
            }
        }
    }
}

internal class AkkhaQuadrants(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps

    private class Cell(val static: CoordGrid, val tile: CoordGrid, val distance: Int)

    private val cells: Map<Quadrant, List<Cell>> by lazy { classify() }

    val tileCounts: Map<Quadrant, Int>
        get() = cells.mapValues { it.value.size }

    fun quadrantOf(player: Player): Quadrant? {
        val static = room.staticCoords(player.coords)
        if (static == CoordGrid.NULL || !room.room.inChallengeArea(static)) return null
        return Quadrant.of(static)
    }

    fun openTiles(): List<CoordGrid> = cells.values.flatMap { list -> list.map { it.tile } }

    fun memoryBlast(quadrant: Quadrant) = blast(quadrant, MEMORY_BASE_DELAY, MEMORY_STEP)

    fun shadowSlam(quadrant: Quadrant) = blast(quadrant, SLAM_BASE_DELAY, -MEMORY_STEP)

    private fun blast(quadrant: Quadrant, base: Int, step: Int) {
        val struck = HashSet<Player>()
        val byLanding = HashMap<Int, MutableSet<CoordGrid>>()
        for (cell in cells.getValue(quadrant)) {
            val delay = base + step * cell.distance
            deps.worldRepo.spotanimMap(
                spotanim(quadrant.gfx),
                cell.tile,
                height = quadrant.gfxHeight,
                delay = delay,
            )
            val landing = (delay + CYCLES_PER_TICK - 1) / CYCLES_PER_TICK
            byLanding.getOrPut(landing) { HashSet() } += cell.tile
        }
        for ((landing, tiles) in byLanding) {
            room.schedule(landing) {
                for (player in room.targets()) {
                    if (player.coords !in tiles || !struck.add(player)) continue
                    room.hazardDamage(player, BLAST_DAMAGE, BLAST_SPREAD)
                    room.effects.apply(quadrant, player)
                }
            }
        }
    }

    private fun classify(): Map<Quadrant, List<Cell>> {
        val min = checkNotNull(room.room.challengeMin)
        val max = checkNotNull(room.room.challengeMax)
        val result = Quadrant.entries.associateWith { ArrayList<Cell>() }
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                val static = CoordGrid(x, z, min.level)
                if (!room.room.inChallengeArea(static)) continue
                val tile = room.coords(static)
                if (!room.isOpenFloor(tile)) continue
                val quadrant = Quadrant.of(static)
                val origin = quadrant.origin
                val distance = abs(x - origin.x) + abs(z - origin.z)
                result.getValue(quadrant) += Cell(static, tile, distance)
            }
        }
        return result
    }

    private companion object {
        const val CYCLES_PER_TICK = 30
        const val MEMORY_BASE_DELAY = 30
        const val SLAM_BASE_DELAY = 66
        const val MEMORY_STEP = 3
        const val BLAST_DAMAGE = 14
        const val BLAST_SPREAD = 2
    }
}

internal class AkkhaEffects(private val room: AkkhaEncounter) {
    private val deps = room.raid.deps

    fun apply(quadrant: Quadrant, player: Player) {
        val cycle = deps.mapClock.cycle
        when (quadrant) {
            Quadrant.NE -> {
                if (player.darknessCooldown > cycle) return
                player.darknessCooldown = cycle + DARKNESS_COOLDOWN
                darkness(player)
            }
            Quadrant.SE -> lightning(player)
            Quadrant.SW -> freeze(player)
            Quadrant.NW -> {
                if (player.burnCooldown > cycle) return
                player.burnCooldown = cycle + BURN_COOLDOWN
                burn(player)
            }
        }
    }

    fun isFrozen(player: Player): Boolean = player.freezeExpiry > deps.mapClock.cycle

    private fun isBurning(player: Player): Boolean = player.burnExpiry > deps.mapClock.cycle

    fun clear() {
        for (player in room.targets()) {
            player.freezeExpiry = 0
            player.darknessCooldown = 0
            player.burnCooldown = 0
            player.burnExpiry = 0
        }
    }

    private fun freeze(player: Player) {
        val alreadyFrozen = isFrozen(player)
        player.freezeExpiry = deps.mapClock.cycle + FREEZE_TICKS
        if (alreadyFrozen) return
        player.mes(FREEZE_MESSAGE)
        room.schedule(FREEZE_TICKS) {
            if (player in room.targets() && !isFrozen(player)) player.mes(FREEZE_END_MESSAGE)
        }
    }

    private fun darkness(player: Player) {
        player.mes(DARKNESS_MESSAGE)
        for (stat in COMBAT_STATS) player.statSub(stat, DARKNESS_DRAIN, 0)
        for (i in 1..DARKNESS_HITS) {
            room.schedule(i) { if (player in room.targets()) hit(player, DARKNESS_DAMAGE) }
        }
    }

    private fun lightning(player: Player) {
        player.mes(LIGHTNING_MESSAGE)
        player.lockOverheads(LIGHTNING_LOCK_TICKS)
    }

    private fun burn(player: Player) {
        if (isBurning(player)) return
        player.burnExpiry = deps.mapClock.cycle + BURN_HITS * BURN_INTERVAL + 1
        player.mes(BURN_MESSAGE)
        for (i in 1..BURN_HITS) {
            room.schedule(i * BURN_INTERVAL) {
                if (player in room.targets()) {
                    hit(player, BURN_DAMAGE)
                    player.spotanim(AkkhaSpots.BURN_TICK, BURN_SPOT_DELAY, BURN_SPOT_HEIGHT)
                    player.soundSynth(AkkhaSynths.BURN)
                    spreadBurn(player)
                }
                if (i == BURN_HITS) player.mes(BURN_END_MESSAGE)
            }
        }
    }

    private fun spreadBurn(source: Player) {
        for (other in room.targets()) {
            if (other === source || isBurning(other)) continue
            if (other.coords.chebyshevDistance(source.coords) <= 1) burn(other)
        }
    }

    private fun hit(player: Player, base: Int) = room.hazardDamage(player, base, 0)

    private companion object {
        const val DARKNESS_COOLDOWN = 4
        const val DARKNESS_DRAIN = 3
        const val DARKNESS_HITS = 5
        const val DARKNESS_DAMAGE = 5
        const val LIGHTNING_LOCK_TICKS = 8
        const val FREEZE_TICKS = 25
        const val BURN_COOLDOWN = 30
        const val BURN_HITS = 5
        const val BURN_INTERVAL = 2
        const val BURN_DAMAGE = 5
        const val BURN_SPOT_DELAY = 30
        const val BURN_SPOT_HEIGHT = 124
        const val DARKNESS_MESSAGE = "You have been consumed by darkness!"
        const val LIGHTNING_MESSAGE = "You have been struck by lightning and can't use protection prayers!"
        const val FREEZE_MESSAGE = "Your attacks have been slowed by some ice!"
        const val FREEZE_END_MESSAGE = "The ice around you melts away."
        const val BURN_MESSAGE = "You have been set alight!"
        const val BURN_END_MESSAGE = "The fire around you goes out."
        val COMBAT_STATS =
            listOf("stat.attack", "stat.strength", "stat.defence", "stat.ranged", "stat.magic")
    }
}

private var Player.freezeExpiry by intVarp("varp.toa_akkha_freeze_expiry")
private var Player.darknessCooldown by intVarp("varp.toa_akkha_darkness_cooldown")
private var Player.burnCooldown by intVarp("varp.toa_akkha_burn_cooldown")
private var Player.burnExpiry by intVarp("varp.toa_akkha_burn_expiry")
