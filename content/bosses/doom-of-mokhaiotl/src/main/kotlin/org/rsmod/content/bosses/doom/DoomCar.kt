package org.rsmod.content.bosses.doom

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.Angles
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossEncounter
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.runAbility
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.VarExpr
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.soundSynth
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

internal object DoomCar {
    const val ENTRY_PHASE = "burrow_entry"
    const val PHASE = "burrowed"
    const val ENTER_ABILITY = "burrow_enter"
    const val EMERGE_ABILITY = "burrow_emerge"
    const val ROCK_HIT_ABILITY = "burrow_rock_hit"
    const val SHOT_ABILITY = "burrow_shot"
    const val ENTER_EXT = "doom.burrow_enter"
    const val RUSH_EXT = "doom.burrow_rushes"

    const val POST_EMERGE = 1
    const val POST_SHOCKWAVE = 2
    const val CHARGE_TICKS = 20
    const val FIRST_TELEGRAPH_DELAY = 3
    const val TELEGRAPH_TICKS = 3
    const val PLAIN_RUSHES = 2
    const val RUSH_GAP = 14
    const val EMERGE_DELAY = 4
    const val SLAM_DELAY = 6
    const val NEXT_RUSH_DELAY = 4
    const val POST_EMERGE_ROCK_DELAY = 5

    private const val SLAM_RADIUS_SQUARED = 250
    private const val CRUMBLE_RANGE = 15

    val restartCharge: Effect =
        sequence(
            headbar(DoomCharge.CHARGE_BAR, fromPercent = 0, toPercent = 100, cycles = CHARGE_TICKS * DoomCharge.CYCLES_PER_TICK),
            setVarn(DoomVarns.CHARGE_END, VarExpr.Now + CHARGE_TICKS),
        )

    fun enter(delve: DoomDelve): Effect =
        sequence(
            anim("seq.dom_burrow"),
            spotanim("spotanim.vfx_dom_burrow"),
            external(ENTER_EXT, params = delve),
            wait(5),
            transitionTo(PHASE),
            setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_BURROW),
            setVarn(DoomVarns.GUARANTEED_HIT, 1),
            restartCharge,
            external(RUSH_EXT, params = delve),
        )

    fun chargeTick(delve: DoomDelve): Effect =
        whenever(
            varnExpired(DoomVarns.CHARGE_END),
            sequence(DoomCharge.beam(delve), restartCharge),
            spotanim("spotanim.vfx_beam_charge_up_burrow_01", slot = DoomCharge.CHARGE_UP_SLOT),
        )

    fun emerge(postEmergenceRock: AbilityRef): Effect =
        sequence(
            transitionTo(FIGHT_PHASE),
            setVarn(DoomVarns.CHARGE, DoomVarns.CHARGE_NONE),
            setVarn(DoomVarns.CHARGE_END, 0),
            setVarn(DoomVarns.GUARANTEED_HIT, 0),
            clearHeadbar(DoomCharge.CHARGE_BAR),
            setVarn(DoomVarns.ROTATION_STEP, POST_EMERGE),
            setVarn(DoomVarns.STEP_ATTACKS, 0),
            anim("seq.dom_burrowed_emerge"),
            spotanim("spotanim.vfx_dom_burrowed_emerge"),
            forceNext(postEmergenceRock),
            nextAttackIn(POST_EMERGE_ROCK_DELAY),
        )

    fun destination(centre: CoordGrid, player: CoordGrid, spawnCentre: CoordGrid): CoordGrid {
        val octant = ((Angles.bearing(centre, player) + 128) % Angles.FULL_TURN) / 256
        val (dx, dz) = OCTANTS[octant]
        val distance = centre.chebyshevDistance(player) + 4
        return CoordGrid(
            (centre.x + dx * distance).coerceIn(spawnCentre.x - 10, spawnCentre.x + 10),
            (centre.z + dz * distance).coerceIn(spawnCentre.z - 10, spawnCentre.z + 10),
            centre.level,
        )
    }

    fun path(from: CoordGrid, to: CoordGrid): List<CoordGrid> {
        val tiles = mutableListOf<CoordGrid>()
        var x = from.x
        var z = from.z
        val dx = abs(to.x - x)
        val dz = abs(to.z - z)
        val sx = (to.x - x).sign
        val sz = (to.z - z).sign
        var error = dx - dz
        while (x != to.x || z != to.z) {
            val doubled = error * 2
            if (doubled > -dz) {
                error -= dz
                x += sx
            }
            if (doubled < dx) {
                error += dx
                z += sz
            }
            tiles += CoordGrid(x, z, from.level)
        }
        return tiles
    }

    fun sweptTiles(steps: List<CoordGrid>, size: Int): Set<CoordGrid> =
        steps.flatMap { sw ->
            (0 until size).flatMap { dx -> (0 until size).map { dz -> sw.translate(dx, dz) } }
        }.toSet()

    fun slamTiles(centre: CoordGrid, bossSw: CoordGrid, size: Int, rocks: Set<CoordGrid>, floor: Iterable<CoordGrid>): Set<CoordGrid> =
        if (centre in rocks) emptySet() else floor.filter { tile ->
            val dx = tile.x - centre.x
            val dz = tile.z - centre.z
            dx * dx + dz * dz <= SLAM_RADIUS_SQUARED && tile !in rocks &&
                !inside(bossSw, size, tile) && !shadowed(centre, tile, bossSw, size, rocks)
        }.toSet()

    fun exposedRocks(centre: CoordGrid, bossSw: CoordGrid, size: Int, rocks: Set<CoordGrid>): List<CoordGrid> =
        if (centre in rocks) listOf(centre) else rocks.filter {
            centre.chebyshevDistance(it) <= CRUMBLE_RANGE && !inside(bossSw, size, it) &&
                !shadowed(centre, it, bossSw, size, rocks)
        }

    private fun shadowed(centre: CoordGrid, tile: CoordGrid, bossSw: CoordGrid, size: Int, rocks: Set<CoordGrid>): Boolean =
        path(centre, tile).dropLast(1).any { it in rocks && !inside(bossSw, size, it) }

    private fun inside(sw: CoordGrid, size: Int, tile: CoordGrid): Boolean =
        tile.x - sw.x in 0 until size && tile.z - sw.z in 0 until size

    private val OCTANTS = listOf(0 to -1, -1 to -1, -1 to 0, -1 to 1, 0 to 1, 1 to 1, 1 to 0, 1 to -1)
}

@Singleton
internal class DoomCars
@Inject
constructor(
    private val deps: BossDeps,
    private val acid: DoomAcid,
    private val larvae: DoomLarvae,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    fun register() {
        deps.extensionRegistry.register(DoomCar.ENTER_EXT) { ext -> enter(ext.npc, ext.target, ext.params as DoomDelve) }
        deps.extensionRegistry.register(DoomCar.RUSH_EXT) { ext -> startRushes(ext.npc, ext.target, ext.params as DoomDelve) }
    }

    private class Burrow(val encounter: BossEncounter, val player: Player, val delve: DoomDelve, val clock: Int) {
        val boss: Npc
            get() = encounter.npc

        val rushes: Int
            get() = if (delve.carSlams > 0) delve.carSlams else DoomCar.PLAIN_RUSHES
    }

    fun onHit(boss: Npc) {
        if (boss.hitpoints <= 0 || boss.vars[DoomVarns.CHARGE] != DoomVarns.CHARGE_BURROW) return
        val target = deps.encounter(boss).lastTarget ?: return
        deps.runAbility(boss, target, "restart_burrow_charge")
    }

    private fun enter(boss: Npc, player: Player, delve: DoomDelve) {
        val encounter = deps.encounter(boss)
        encounter.invulnerable = true
        boss.resetFaceEntity()
        val rocks = fallingRocks(boss, delve)
        rocks.forEach { mapAnim(ROCK_FALL, it, delay = 20) }
        shake(player, BURROW_SHAKE_TICKS)
        player.soundSynth(BURROW_RUMBLE_SYNTH, loops = BURROW_RUMBLE_LOOPS)
        player.soundSynth(BURROW_ROCK_FALL_SYNTH, delay = BURROW_ROCK_FALL_DELAY)
        player.soundSynth(BURROW_ROCK_LAND_SYNTH, delay = BURROW_ROCK_LAND_DELAY)
        deps.worldQueues.add(6) {
            if (!active(encounter, player, DoomCar.PHASE)) return@add
            for (tile in rocks) {
                deps.locRepo.add(tile, ROCK_LOC, Int.MAX_VALUE, LocAngle[deps.random.of(4)], LocShape.CentrepieceStraight)
                if (player.coords == tile) deps.runAbility(boss, player, DoomCar.ROCK_HIT_ABILITY)
            }
        }
    }

    private fun fallingRocks(boss: Npc, delve: DoomDelve): List<CoordGrid> {
        val centre = boss.coords.translate(boss.size / 2, boss.size / 2)
        val clearRadius = if (delve.burrowCentreRock) 0 else CENTRE_CLEAR_RADIUS
        val candidates =
            (DoomArena.FLOOR_MIN_X..DoomArena.FLOOR_MAX_X).flatMap { dx ->
                (DoomArena.FLOOR_MIN_Z..DoomArena.FLOOR_MAX_Z).map { dz -> boss.spawnCoords.translate(dx, dz) }
            }.filter {
                it.chebyshevDistance(centre) > clearRadius && !deps.collision.isWalkBlocked(it) &&
                    !deps.locRepo.findLoc(it, ROCK_LOC)
            }.toMutableList()
        val rocks = if (delve.burrowCentreRock) mutableListOf(centre) else mutableListOf()
        repeat((deps.random.of(delve.burrowRocks) - rocks.size).coerceAtMost(candidates.size)) {
            val weights = candidates.map { 30 - it.chebyshevDistance(centre).coerceAtMost(29) }
            var roll = deps.random.of(weights.sum())
            val index = weights.indices.first { roll -= weights[it]; roll < 0 }
            rocks += candidates.removeAt(index)
        }
        return rocks
    }

    private fun startRushes(boss: Npc, player: Player, delve: DoomDelve) {
        val encounter = deps.encounter(boss)
        encounter.invulnerable = false
        val burrow = Burrow(encounter, player, delve, deps.mapClock.cycle)
        later(burrow, DoomCar.FIRST_TELEGRAPH_DELAY) { rush(burrow, number = 1) }
    }

    private fun rush(burrow: Burrow, number: Int) {
        val boss = burrow.boss
        val player = burrow.player
        val half = boss.size / 2
        val destination = DoomCar.destination(boss.coords.translate(half, half), player.coords, boss.spawnCoords.translate(half, half))
        boss.resetFaceEntity()
        boss.faceSquare(destination)
        mapAnim(TELEGRAPH_SPAWN, destination)
        mapAnim(TELEGRAPH_MOVE, destination, delay = 60)
        val steps = DoomCar.path(boss.coords, destination.translate(-half, -half)).chunked(burrow.delve.rushSpeed)
        val last = number == burrow.rushes
        if (burrow.delve.carSlams == 0 && !last) {
            later(burrow, DoomCar.RUSH_GAP) { rush(burrow, number + 1) }
        }
        var trampled = false
        fun advance(index: Int) {
            shoot(burrow)
            val chunk = steps.getOrNull(index)
            if (chunk == null) {
                arrive(burrow, number, last)
                return
            }
            val from = boss.coords
            val swept = DoomCar.sweptTiles(listOf(from) + chunk, boss.size)
            val landing = if (index == steps.lastIndex) chunk.last().translate(half, half) else null
            swept.forEach { if (it != landing) destroyRock(player, it) }
            if (!trampled && player.coords in swept) {
                trampled = true
                shove(boss, player, swept)
                player.queueHit(boss, 1, HitType.Typeless, burrow.delve.trampleDamage, deps.playerHitModifier)
            }
            move(boss, chunk.last())
            later(burrow, 1) { advance(index + 1) }
        }
        later(burrow, DoomCar.TELEGRAPH_TICKS) { advance(0) }
    }

    /** Burrowed shots follow the boss's 2-tick clock from the transmog, not the zoom's own start. */
    private fun shoot(burrow: Burrow) {
        if (burrow.delve.carSlams == 0 || (deps.mapClock.cycle - burrow.clock) % 2 == 0) return
        deps.runAbility(burrow.boss, burrow.player, DoomCar.SHOT_ABILITY)
    }

    private fun arrive(burrow: Burrow, number: Int, last: Boolean) {
        val boss = burrow.boss
        val player = burrow.player
        boss.faceSquare(player.coords, player.size, player.size)
        if (burrow.delve.carSlams == 0) {
            if (last) later(burrow, DoomCar.EMERGE_DELAY) { emerge(burrow) }
            return
        }
        boss.anim(EXPLOSION_SEQ, delay = EXPLOSION_DELAY)
        boss.spotanim(EXPLOSION_SPOTANIM, delay = EXPLOSION_DELAY)
        later(burrow, DoomCar.SLAM_DELAY) {
            slam(burrow)
            deps.worldQueues.add(1) { if (present(burrow.encounter)) crumble(burrow) }
            if (last) emerge(burrow)
            else later(burrow, DoomCar.NEXT_RUSH_DELAY) { rush(burrow, number + 1) }
        }
    }

    private fun emerge(burrow: Burrow) {
        val boss = burrow.boss
        deps.runAbility(boss, burrow.player, DoomCar.EMERGE_ABILITY)
        boss.resetPendingFaceSquare()
        boss.facePlayer(burrow.player)
        boss.apPlayer2(burrow.player, aiPlayerInteractions)
    }

    private fun slam(burrow: Burrow) {
        val boss = burrow.boss
        val player = burrow.player
        val centre = boss.coords.translate(boss.size / 2, boss.size / 2)
        val floor = floorTiles(boss).filter { !deps.collision.isWalkBlocked(it) }
        val slammed = DoomCar.slamTiles(centre, boss.coords, boss.size, rockTiles(boss), floor)
        for (tile in slammed) {
            val delay = (centre.chebyshevDistance(tile) - RIPPLE_LEAD).coerceAtLeast(0)
            mapAnim(deps.random.pick(RIPPLES), tile, delay)
        }
        shake(player, SLAM_SHAKE_TICKS)
        player.soundSynth(SLAM_SYNTH)
        if (player.coords in slammed) {
            player.queueHit(boss, 1, HitType.Typeless, deps.random.of(SLAM_DAMAGE), deps.playerHitModifier)
        }
    }

    private fun shake(player: Player, ticks: Int) {
        for (axis in SHAKE_AXES) Camera.camShake(player, axis, random = SHAKE_RANDOM, amplitude = 0, rate = 0)
        deps.worldQueues.add(ticks) { if (player.isSlotAssigned) Camera.camReset(player) }
    }

    private fun crumble(burrow: Burrow) {
        val boss = burrow.boss
        val centre = boss.coords.translate(boss.size / 2, boss.size / 2)
        DoomCar.exposedRocks(centre, boss.coords, boss.size, rockTiles(boss)).forEach { destroyRock(burrow.player, it) }
    }

    private fun floorTiles(boss: Npc): List<CoordGrid> =
        (DoomArena.FLOOR_MIN_X..DoomArena.FLOOR_MAX_X).flatMap { dx ->
            (DoomArena.FLOOR_MIN_Z..DoomArena.FLOOR_MAX_Z).map { dz -> boss.spawnCoords.translate(dx, dz) }
        }

    private fun rockTiles(boss: Npc): Set<CoordGrid> = floorTiles(boss).filter { deps.locRepo.findLoc(it, ROCK_LOC) }.toSet()

    private fun later(burrow: Burrow, ticks: Int, block: () -> Unit) {
        deps.worldQueues.add(ticks) { if (active(burrow.encounter, burrow.player, DoomCar.PHASE)) block() }
    }

    private fun present(encounter: BossEncounter): Boolean =
        deps.encounterRegistry.isActive(encounter) && encounter.npc.isSlotAssigned && encounter.npc.hitpoints > 0

    private fun move(boss: Npc, destination: CoordGrid) {
        val from = boss.coords
        boss.teleport(deps.collision, destination)
        boss.pendingExactMove = exactMove(from, destination)
        boss.anim("seq.dom_burrowed_movement")
        boss.spotanim("spotanim.vfx_dom_burrowed_movement")
        larvae.retarget(boss)
    }

    private fun shove(boss: Npc, player: Player, swept: Set<CoordGrid>) {
        val from = player.coords
        val destination =
            (1..boss.size + 1).firstNotNullOfOrNull { radius ->
                val candidates =
                    (-radius..radius).flatMap { dx ->
                        (-radius..radius).mapNotNull { dz ->
                            if (max(abs(dx), abs(dz)) != radius) null else from.translate(dx, dz)
                        }
                    }.filter {
                        it !in swept && DoomArena.onFloor(boss.spawnCoords, it) && !deps.collision.isWalkBlocked(it)
                    }
                deps.random.pickOrNull(candidates)
            } ?: return
        player.anim("seq.agilityarena_player_spikedback")
        PathingEntityCommon.teleport(player, deps.collision, destination)
        player.pendingExactMove = exactMove(from, destination)
    }

    private fun exactMove(from: CoordGrid, destination: CoordGrid): EntityExactMove =
        EntityExactMove(
            deltaX1 = from.x - destination.x,
            deltaZ1 = from.z - destination.z,
            deltaX2 = 0,
            deltaZ2 = 0,
            clientDelay1 = 0,
            clientDelay2 = DoomCharge.CYCLES_PER_TICK,
            direction = Angles.bearing(from, destination),
        )

    private fun destroyRock(player: Player, tile: CoordGrid) {
        val rock = deps.locRepo.findExact(tile, LocShape.CentrepieceStraight)
            ?.takeIf { it.id == ROCK_LOC.asRSCM(RSCMType.LOC) } ?: return
        deps.locRepo.del(rock, Int.MAX_VALUE)
        mapAnim(ROCK_DUST, tile)
        acid.restore(player, tile)
    }

    private fun active(encounter: BossEncounter, player: Player, phase: String): Boolean =
        deps.encounterRegistry.isActive(encounter) && encounter.npc.isSlotAssigned &&
            encounter.npc.hitpoints > 0 && encounter.currentPhaseName == phase &&
            player.isValidTarget() && DoomArena.onFloor(encounter.npc.spawnCoords, player.coords)

    private fun mapAnim(name: String, tile: CoordGrid, delay: Int = 0) {
        deps.worldRepo.spotanimMap(SpotanimType(name.asRSCM(RSCMType.SPOTANIM)), tile, delay = delay)
    }

    private companion object {
        private const val ROCK_LOC = "loc.dom_rock"
        private const val ROCK_FALL = "spotanim.dt2_guardian_rock_fall"
        private const val ROCK_DUST = "spotanim.vfx_colossi_stab_dust_01"
        private const val TELEGRAPH_SPAWN = "spotanim.vfx_doom_boss_burrowed_telegraph_spawn"
        private const val TELEGRAPH_MOVE = "spotanim.vfx_doom_boss_burrowed_movement_telegraph"
        private const val EXPLOSION_SEQ = "seq.dom_burrowed_explosion"
        private const val EXPLOSION_SPOTANIM = "spotanim.vfx_dom_burrowed_explosion"
        private const val EXPLOSION_DELAY = 120
        private const val SLAM_SYNTH = "synth.dom_burrow_slam"
        private const val SLAM_SHAKE_TICKS = 3
        private const val BURROW_SHAKE_TICKS = 5
        private const val BURROW_RUMBLE_SYNTH = "synth.dom_burrow_rumble"
        private const val BURROW_RUMBLE_LOOPS = 5
        private const val BURROW_ROCK_FALL_SYNTH = "synth.dom_burrow_rock_fall"
        private const val BURROW_ROCK_FALL_DELAY = 45
        private const val BURROW_ROCK_LAND_SYNTH = "synth.dom_burrow_rock_land"
        private const val BURROW_ROCK_LAND_DELAY = 160
        private const val SHAKE_RANDOM = 5
        private const val CENTRE_CLEAR_RADIUS = 1
        private const val RIPPLE_LEAD = 2
        private val SLAM_DAMAGE = 25..39
        private val SHAKE_AXES = listOf(CamShakeAxis.LEFT_RIGHT, CamShakeAxis.UP_DOWN, CamShakeAxis.FORWARDS_BACKWARDS)
        private val RIPPLES = listOf("spotanim.vfx_area_slam_01", "spotanim.vfx_area_slam_02", "spotanim.vfx_area_slam_03")
    }
}
