package org.rsmod.content.raids.toa.raid.encounter.wardens

import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.player.lockOverheads
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class WardensObelisk(private val room: WardensEncounter) {
    private enum class Mode {
        OFF,
        CHARGING,
        BUSY,
        SETTLE,
    }

    private enum class Special(val duration: Int) {
        SKULL(SKULL_DURATION),
        BEAM(BEAM_DURATION),
        WINDMILL(WINDMILL_DURATION),
    }

    private val deps = room.raid.deps
    private var mode = Mode.OFF
    private var paused = false
    private var charged = 0
    private var settled = 0
    private var epoch = 0
    private var next = Special.SKULL

    private val centre: CoordGrid
        get() = room.coords(WardenGod.OBELISK_CENTRE)

    private val chargePlays: List<Int>
        get() = if (room.raid.isActive("dbrow.toa_invocation_acceleration")) FAST_PLAYS else SLOW_PLAYS

    fun start() {
        epoch++
        paused = false
        next = Special.entries[deps.random.of(Special.entries.size)]
        beginCharging()
    }

    fun stop() {
        epoch++
        mode = Mode.OFF
        paused = false
    }

    fun coreOut() {
        if (mode == Mode.OFF) return
        epoch++
        paused = true
        if (mode == Mode.BUSY) {
            room.obelisk?.anim(CLOSE_SEQ)
            beginCharging()
            charged = PLAY_SPACING
        }
    }

    fun coreBack() {
        paused = false
    }

    fun tick() {
        if (mode == Mode.OFF || paused) return
        when (mode) {
            Mode.CHARGING -> charge()
            Mode.SETTLE -> if (++settled >= SETTLE_TICKS) beginCharging()
            else -> Unit
        }
    }

    private fun beginCharging() {
        mode = Mode.CHARGING
        charged = 0
        settled = 0
    }

    private fun charge() {
        val plays = chargePlays
        val total = plays.sum() * PLAY_SPACING
        if (charged % PLAY_SPACING == 0 && charged < total) {
            room.obelisk?.anim(CHARGE_SEQS[partOf(plays, charged / PLAY_SPACING)])
        }
        charged++
        if (charged >= total) open()
    }

    private fun partOf(plays: List<Int>, index: Int): Int {
        var left = index
        for ((part, count) in plays.withIndex()) {
            if (left < count) return part
            left -= count
        }
        return plays.lastIndex
    }

    private fun open() {
        mode = Mode.BUSY
        val special = next
        next = Special.entries[(special.ordinal + 1) % Special.entries.size]
        room.obelisk?.anim(OPEN_SEQ)
        for ((index, synth) in OPEN_SOUNDS.withIndex()) {
            deps.worldRepo.soundArea(
                centre,
                synth,
                delay = SOUND_STEP * (index + 1),
                radius = SOUND_RADIUS,
            )
        }
        val token = epoch
        when (special) {
            Special.SKULL -> skull(token)
            Special.BEAM -> beam(token)
            Special.WINDMILL -> windmill(token)
        }
        later(token, special.duration) { close() }
    }

    private fun close() {
        room.obelisk?.anim(CLOSE_SEQ)
        mode = Mode.SETTLE
        settled = 0
    }

    private fun later(token: Int, ticks: Int, action: () -> Unit) {
        if (ticks <= 0) {
            action()
            return
        }
        room.schedule(ticks) { if (token == epoch) action() }
    }

    private fun skull(token: Int) {
        warnings(token, SKULL_WARN)
        later(token, SKULL_BOMB_TICK) { room.obelisk?.anim(BOMB_SEQ) }
        for (index in 0 until SKULL_COUNT) {
            later(token, SKULL_FIRST + index * SKULL_STEP) { launchSkull() }
        }
    }

    private fun beam(token: Int) {
        warnings(token, BEAM_WARN)
        later(token, 1) { room.obelisk?.anim(BEAM_SEQ) }
        later(token, BEAM_VERTICAL) { strike(line(vertical = true), STRIKE) }
        later(token, BEAM_SWEEP_A) {
            strike(WardensObeliskPatterns.beamSweepA.associateWith { sweepDelay(it, vertical = false) }, SWEEP)
        }
        later(token, BEAM_HORIZONTAL) { strike(line(vertical = false), STRIKE) }
        later(token, BEAM_SWEEP_B) {
            strike(WardensObeliskPatterns.beamSweepB.associateWith { sweepDelay(it, vertical = true) }, SWEEP)
        }
    }

    private fun windmill(token: Int) {
        warnings(token, WINDMILL_WARN)
        later(token, 1) { room.obelisk?.anim(SWEEP_SEQ) }
        for (step in 0 until WINDMILL_STEPS) {
            later(token, WINDMILL_FIRST + step * WINDMILL_STEP) {
                val set = WardensObeliskPatterns.windmill[step % WARD_SETS]
                for (tile in set) {
                    deps.worldRepo.spotanimMap(spotanim(HIGHLIGHT), room.coords(tile), delay = HIGHLIGHT_DELAY)
                }
                deps.worldRepo.soundArea(centre, WINDMILL_SOUND, radius = SOUND_RADIUS)
                room.schedule(WINDMILL_STEP) { strike(set.associateWith { 2 * ring(it) }, STRIKE) }
            }
        }
    }

    private fun warnings(token: Int, spot: String) {
        for (tick in 0 until WARN_TICKS) {
            later(token, tick) {
                val pool = WARN_RING.toMutableList()
                repeat(WARN_TILES) {
                    val tile = pool.removeAt(deps.random.of(pool.size))
                    deps.worldRepo.spotanimMap(spotanim(spot), centre.translate(tile.first, tile.second))
                }
            }
        }
    }

    private fun launchSkull() {
        val target = skullTarget()
        val source = centre
        val tile = room.coords(target)
        deps.worldRepo.spotanimMap(spotanim(SHADOW), tile)
        deps.worldRepo.soundArea(source, SKULL_SOUND, radius = SOUND_RADIUS)
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(SKULL_PROJECTILE).id,
                startHeight = SKULL_START_HEIGHT,
                endHeight = SKULL_END_HEIGHT,
                startTime = SKULL_START_TIME,
                endTime = SKULL_FLIGHT,
                angle = SKULL_ANGLE,
                progress = SKULL_PROGRESS,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = source,
                endCoord = tile,
            )
        )
        room.schedule(SKULL_IMPACT) { skullImpact(target) }
    }

    private fun skullImpact(target: CoordGrid) {
        val tiles = HashMap<CoordGrid, Int>()
        for (dx in -SKULL_RADIUS..SKULL_RADIUS) {
            for (dz in -SKULL_RADIUS..SKULL_RADIUS) {
                val tile = CoordGrid(target.x + dx, target.z + dz, target.level)
                val cardinal = (dx == 0 || dz == 0) && max(abs(dx), abs(dz)) in 1..2
                if (cardinal || !inArena(tile) || insideObelisk(tile)) continue
                tiles[tile] = SKULL_RING_DELAY * max(abs(dx), abs(dz))
            }
        }
        deps.worldRepo.soundArea(room.coords(target), SKULL_IMPACT_SOUND, radius = SOUND_RADIUS)
        strike(tiles, LIGHTNING)
    }

    private fun skullTarget(): CoordGrid {
        while (true) {
            val tile =
                CoordGrid(
                    deps.random.of(ARENA_MIN_X + 1, ARENA_MAX_X - 1),
                    deps.random.of(ARENA_MIN_Z + 2, ARENA_MAX_Z - 2),
                    1,
                )
            if (!insideObelisk(tile)) return tile
        }
    }

    private fun line(vertical: Boolean): Map<CoordGrid, Int> =
        buildMap {
            val range = if (vertical) VERTICAL_RANGE else HORIZONTAL_RANGE
            for (offset in range) {
                if (abs(offset) <= OBELISK_HALF) continue
                val tile =
                    if (vertical) CoordGrid(CENTRE.x, CENTRE.z + offset, 1)
                    else CoordGrid(CENTRE.x + offset, CENTRE.z, 1)
                put(tile, 2 * abs(offset))
            }
        }

    private fun sweepDelay(tile: CoordGrid, vertical: Boolean): Int {
        val dx = abs(tile.x - CENTRE.x)
        val dz = abs(tile.z - CENTRE.z)
        return if (vertical) SWEEP_STEP * (dz + SWEEP_B_OFFSET - dx)
        else SWEEP_STEP * (dx + SWEEP_A_OFFSET - dz)
    }

    private fun ring(tile: CoordGrid): Int =
        max(abs(tile.x - CENTRE.x), abs(tile.z - CENTRE.z))

    private fun strike(tiles: Map<CoordGrid, Int>, spot: String) {
        val struck = HashMap<Int, MutableSet<CoordGrid>>()
        for ((tile, delay) in tiles) {
            val coords = room.coords(tile)
            deps.worldRepo.spotanimMap(spotanim(spot), coords, delay = delay)
            struck.getOrPut(delay / CYCLES_PER_TICK) { HashSet() } += coords
        }
        for ((ticks, set) in struck) {
            if (ticks <= 0) damage(set) else room.schedule(ticks) { damage(set) }
        }
    }

    private fun damage(tiles: Set<CoordGrid>) {
        for (player in room.targets()) {
            if (player.coords in tiles) hit(player)
        }
    }

    private fun hit(player: Player) {
        val penetration = room.raid.isActive("dbrow.toa_invocation_penetration")
        room.hazardDamage(player, if (penetration) PENETRATION_DAMAGE else DAMAGE, DAMAGE_SPREAD)
        if (penetration) {
            player.lockOverheads(PRAYER_LOCK_TICKS)
            player.mes(PRAYERS_DISABLED)
        }
    }

    private fun inArena(tile: CoordGrid): Boolean =
        tile.x in ARENA_MIN_X..ARENA_MAX_X && tile.z in ARENA_MIN_Z..ARENA_MAX_Z

    private fun insideObelisk(tile: CoordGrid): Boolean =
        abs(tile.x - CENTRE.x) <= OBELISK_HALF && abs(tile.z - CENTRE.z) <= OBELISK_HALF

    private companion object {
        val CENTRE = WardenGod.OBELISK_CENTRE

        const val OPEN_SEQ = "seq.fx_obelisk_wardens02_open"
        const val CLOSE_SEQ = "seq.fx_obelisk_wardens02_close"
        const val BOMB_SEQ = "seq.fx_obelisk_wardens02_bomb"
        const val BEAM_SEQ = "seq.fx_obelisk_wardens02_beam"
        const val SWEEP_SEQ = "seq.fx_obelisk_wardens02_sweep"
        val CHARGE_SEQS =
            listOf(
                "seq.fx_obelisk_wardens01_charge_part1",
                "seq.fx_obelisk_wardens01_charge_part2",
                "seq.fx_obelisk_wardens01_charge_part3",
            )
        val SLOW_PLAYS = listOf(5, 9, 5)
        val FAST_PLAYS = listOf(4, 4, 4)
        const val PLAY_SPACING = 2
        const val SETTLE_TICKS = 3

        val OPEN_SOUNDS =
            listOf(
                "synth.toa_wardens_obelisk_open_1",
                "synth.toa_wardens_obelisk_open_2",
                "synth.crystal_sing",
                "synth.toa_wardens_obelisk_open_4",
            )
        const val SKULL_SOUND = "synth.toa_wardens_obelisk_skull"
        const val SKULL_IMPACT_SOUND = "synth.toa_wardens_obelisk_skull_impact"
        const val WINDMILL_SOUND = "synth.toa_wardens_obelisk_windmill"
        const val SOUND_STEP = 30
        const val SOUND_RADIUS = 9

        const val LIGHTNING_WARN = "spotanim.wardens_lightning_warn"
        const val SKULL_WARN = LIGHTNING_WARN
        const val BEAM_WARN = "spotanim.wardens_darkness_warn"
        const val WINDMILL_WARN = "spotanim.wardens_burn_warn"
        const val LIGHTNING = LIGHTNING_WARN
        const val STRIKE = "spotanim.spotanim_amascut01_strike01"
        const val HIGHLIGHT = "spotanim.spotanim_amascut01_strike01_highlight01"
        const val SWEEP = "spotanim.spotanim_wardens01_dronestrike01_fire01"
        const val SHADOW = "spotanim.gargboss_debris_shadow_120"
        const val SKULL_PROJECTILE = "spotanim.fx_wardens_bomb02"

        const val WARN_TICKS = 4
        const val WARN_TILES = 3
        const val WARN_RADIUS = 2
        val WARN_RING: List<Pair<Int, Int>> =
            buildList {
                for (dx in -WARN_RADIUS..WARN_RADIUS) {
                    for (dz in -WARN_RADIUS..WARN_RADIUS) {
                        if (max(abs(dx), abs(dz)) == WARN_RADIUS) add(dx to dz)
                    }
                }
            }

        const val SKULL_DURATION = 21
        const val SKULL_BOMB_TICK = 2
        const val SKULL_FIRST = 3
        const val SKULL_STEP = 2
        const val SKULL_COUNT = 10
        const val SKULL_IMPACT = 4
        const val SKULL_RADIUS = 3
        const val SKULL_RING_DELAY = 10
        const val SKULL_START_HEIGHT = 100
        const val SKULL_END_HEIGHT = 3
        const val SKULL_START_TIME = 23
        const val SKULL_FLIGHT = 120
        const val SKULL_ANGLE = 40
        const val SKULL_PROGRESS = 64

        const val BEAM_DURATION = 13
        const val BEAM_VERTICAL = 8
        const val BEAM_SWEEP_A = 9
        const val BEAM_HORIZONTAL = 14
        const val BEAM_SWEEP_B = 15
        const val SWEEP_STEP = 30
        const val SWEEP_A_OFFSET = 12
        const val SWEEP_B_OFFSET = 11
        val VERTICAL_RANGE = -12..12
        val HORIZONTAL_RANGE = -11..11

        const val WINDMILL_DURATION = 37
        const val WINDMILL_FIRST = 4
        const val WINDMILL_STEP = 3
        const val WINDMILL_STEPS = 12
        const val WARD_SETS = 5
        const val HIGHLIGHT_DELAY = 30

        const val ARENA_MIN_X = 3797
        const val ARENA_MAX_X = 3819
        const val ARENA_MIN_Z = 5142
        const val ARENA_MAX_Z = 5166
        const val OBELISK_HALF = 1
        const val CYCLES_PER_TICK = 30

        const val DAMAGE = 13
        const val PENETRATION_DAMAGE = 18
        const val DAMAGE_SPREAD = 2
        const val PRAYER_LOCK_TICKS = 8
        const val PRAYERS_DISABLED = "<col=ff3045>Your prayers have been disabled!</col>"
    }
}
