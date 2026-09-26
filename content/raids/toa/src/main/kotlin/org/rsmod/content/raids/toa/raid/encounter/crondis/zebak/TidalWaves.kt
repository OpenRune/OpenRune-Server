package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.min
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

/**
 * Tidal Waves (zebak_0_invocation capture, T = the tick Zebak animates; Offline_Scape
 * Zebak.landWaves had every step one tick earlier and the next auto at 16):
 * - T: the throw animation; the next auto comes at T+15;
 * - T+1: 16 acid pools and 6-8 jugs at random, landing by distance ([lob]);
 * - T+5: the call animation;
 * - T+7: rocks fall on the wave side and the camera shakes (reset at T+9);
 * - T+14, 21, 28: a row of 21 waves with a gap: 3 wide, one narrower every two path levels (wiki).
 *   The second row's gap mirrors the first;
 * - T+46: over (the capture's next special started then); the next Tidal Waves comes from the
 *   other side.
 */
internal class TidalWaves(private val room: ZebakEncounter) : ZebakSpecial {
    private val deps = room.raid.deps
    private var ticks = -1
    private val landings = ZebakLandings()
    private val fromSouth = room.waves.fromSouth

    /** Offline_Scape `waveSkipX`: the previous row's gap, or -1 for a fresh one. */
    private var lastGap = -1

    override fun step(): Boolean {
        val boss = room.zebak ?: return false
        if (boss.hitpoints <= 0) return false
        ticks++
        if (room.targets().isNotEmpty()) landings.run(ticks)
        when (ticks) {
            0 -> windUp(boss)
            THROW_TICK -> launch()
            CALL_TICK -> {
                boss.anim(ZebakSeqs.CALL_WAVES)
                room.tail?.anim(ZebakSeqs.TAIL_CALL_WAVES)
            }
            ROCKS_TICK -> if (!rocksFall()) return false
            CAMERA_RESET_TICK -> {
                val targets = room.targets()
                if (targets.isEmpty()) return false
                for (player in targets) Camera.camShakeResetAll(player)
            }
            in ROW_TICKS -> spawnRow()
            END_TICK -> {
                room.waves.fromSouth = !fromSouth
                return false
            }
        }
        return true
    }

    private fun windUp(boss: Npc) {
        boss.anim(ZebakSeqs.RANGED)
        room.tail?.resetAnim()
        room.attackCountdown = NEXT_ATTACK
    }

    /** Capture: the throw sound plays here too (Offline_Scape had none for this special). */
    private fun launch() {
        val jugs = room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, emptyList())
            .take(deps.random.of(ZebakJugs.THROWN_MIN, ZebakJugs.THROWN_MAX))
        val acid = room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, emptyList())
            .take(ACID_POOLS)
        val middle = room.coords(ZebakCoords.MIDDLE)
        deps.worldRepo.soundArea(middle, ZebakSynths.JUGS_SHOOT, radius = SOUND_RADIUS)
        val mouth = room.coords(ZebakCoords.PROJECTILE_START)
        for (tile in acid) {
            val flight = deps.worldRepo.lob(ZebakSpots.ACID, mouth, tile)
            landings.at(ticks + flight) { room.poison.land(listOf(tile)) }
        }
        for (tile in jugs) {
            val flight = deps.worldRepo.lob(ZebakSpots.JUG, mouth, tile)
            landings.at(ticks + flight) { room.jugs.land(listOf(tile), room.targets()) }
        }
    }

    private fun rocksFall(): Boolean {
        val targets = room.targets()
        if (targets.isEmpty()) return false
        for (player in targets) {
            for ((synth, delay) in ZebakSynths.WAVES_LAND) player.soundSynth(synth, delay = delay)
        }
        val base = room.coords(if (fromSouth) ZebakCoords.WAVE_SOUTH else ZebakCoords.WAVE_NORTH)
        val splash = spotanim(ZebakSpots.WATER_SPLASH)
        val rocks = spotanim(ZebakSpots.ROCK_FALL)
        for (i in 0 until ROCK_SPOTS) {
            val tile = base.translate(i * ROCK_SPACING, 0)
            deps.worldRepo.spotanimMap(splash, tile, delay = SPLASH_DELAY)
            deps.worldRepo.spotanimMap(rocks, tile)
        }
        for (player in targets) {
            Camera.camShakeResetAll(player)
            // Offline_Scape: left-right 7, up-down 6, front-back 7.
            Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, 7, 0, 0)
            Camera.camShake(player, CamShakeAxis.UP_DOWN, 6, 0, 0)
            Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, 7, 0, 0)
            player.soundSynth(ZebakSynths.RUMBLING)
        }
        return true
    }

    private fun spawnRow() {
        val base = room.coords(if (fromSouth) ZebakCoords.WAVE_SOUTH else ZebakCoords.WAVE_NORTH)
        val gap = if (lastGap == -1) deps.random.of(0, GAP_RANGE) else GAP_RANGE - lastGap
        val gapWidth = GAP_WIDTH - min(2, room.pathLevel / 2)
        val dz = if (fromSouth) 1 else -1
        for (x in 0 until ROW_LENGTH) {
            // The columns by Zebak never have the gap; it widens away from the middle.
            val column = x - SOLID_COLUMNS
            val towards = if (gap < GAP_RANGE / 2) 1 else -1
            val inGap = column >= 0 && (0 until gapWidth).any { column == gap + it * towards }
            if (!inGap) room.waves.spawn(base.translate(x, 0), dz)
        }
        lastGap = if (lastGap == -1) gap else -1
    }

    private companion object {
        const val THROW_TICK = 1
        const val CALL_TICK = 5
        const val ROCKS_TICK = 7
        const val CAMERA_RESET_TICK = 9
        val ROW_TICKS = intArrayOf(14, 21, 28)
        const val END_TICK = 46
        const val NEXT_ATTACK = 15
        const val SOUND_RADIUS = 15
        const val ACID_POOLS = 16
        const val ROW_LENGTH = 21
        const val SOLID_COLUMNS = 4
        const val GAP_RANGE = 12
        const val GAP_WIDTH = 3
        const val ROCK_SPOTS = 7
        const val ROCK_SPACING = 3
        const val SPLASH_DELAY = 200
    }
}

/**
 * The waves themselves (Offline_Scape WaveNPC). They outlive the special by a few ticks. Each lives
 * 23 ticks and moves a tile a tick; on its tile it washes players along (maybe into the water),
 * washes acid away 1 in 4, and destroys blood clouds (turning bloody). A jug two tiles ahead is set
 * rolling the same way.
 */
internal class ZebakWaves(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val waves = HashMap<Npc, Wave>()

    private class Wave(val dz: Int, var ticksLeft: Int)

    /** Which edge the next Tidal Waves comes from; alternates, starts random. */
    var fromSouth = false

    fun spawn(tile: CoordGrid, dz: Int) {
        waves[room.spawn(ZebakNpcs.WAVE, tile)] = Wave(dz, LIFETIME)
    }

    /** Once a tick (config `timer = 1`). */
    fun tick(npc: Npc) {
        val wave = waves[npc] ?: return
        if (room.stage != ToaStage.STARTED || --wave.ticksLeft <= 0) {
            remove(npc)
            return
        }
        val here = npc.coords
        for (player in room.targets()) {
            if (player.coords == here) wash(player, wave.dz)
        }
        val next = here.translate(0, wave.dz)
        val ahead = next.translate(0, wave.dz)
        for (jug in room.jugs.npcs.filter { it.coords == ahead }) room.jugs.roll(jug, wave.dz)
        if (here in room.poison && deps.random.of(0, 3) == 0) room.poison.remove(here)
        val clouds = room.bloodMagic.cloudsAt(here)
        if (clouds.isNotEmpty()) {
            for (cloud in clouds) room.bloodMagic.removeCloud(cloud)
            npc.transmog(npcType(ZebakNpcs.WAVE_BLOODY), Int.MAX_VALUE)
        }
        step(npc, next)
    }

    /** Walks where it can; jumps where the step is blocked (Offline_Scape ignored collision). */
    private fun step(npc: Npc, next: CoordGrid) {
        if (room.canStep(npc.coords, next.x - npc.coords.x, next.z - npc.coords.z)) {
            npc.walk(next)
        } else {
            npc.teleport(deps.collision, next)
        }
    }

    /**
     * Up to 4 tiles along the wave. Stopped by the edge, they're thrown (5 - steps) tiles further,
     * into the water if that tile is walkable. Wiki: 6-10 scaled (Offline_Scape: 8-18).
     */
    private fun wash(player: Player, dz: Int) {
        var dest = player.coords
        var intoWater = false
        for (i in 0 until PUSH_TILES) {
            if (room.canStep(dest, 0, dz)) {
                dest = dest.translate(0, dz)
                continue
            }
            val water = dest.translate(0, dz * (WATER_JUMP - i))
            if (room.water.canSwimTo(water)) {
                dest = water
                intoWater = true
            }
            break
        }
        val facing = if (dz > 0) Direction.South else Direction.North
        room.pushPlayer(player, dest, facing, ZebakSynths.WAVE_HIT)
        player.hitTypeless(deps.random.of(room.maxHit(MIN_DAMAGE), room.maxHit(MAX_DAMAGE)))
        if (intoWater) room.water.startSwimming(player)
    }

    private fun remove(npc: Npc) {
        waves.remove(npc)
        room.despawn(npc)
    }

    fun clear() {
        for (npc in waves.keys.toList()) remove(npc)
        fromSouth = deps.random.of(0, 1) == 0
    }

    private companion object {
        const val LIFETIME = 23
        const val PUSH_TILES = 4
        const val WATER_JUMP = 5
        const val MIN_DAMAGE = 6
        const val MAX_DAMAGE = 10
    }
}
