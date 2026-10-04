package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal class TidalWaves(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private var lastGap = -1

    val fromSouth = room.waves.fromSouth

    fun launch() {
        val jugs =
            room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, emptyList())
                .take(deps.random.of(ZebakJugs.THROWN_MIN, ZebakJugs.THROWN_MAX))
        val acid =
            room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, emptyList())
                .take(ACID_POOLS)
        val source = room.coords(ZebakCoords.THROW_SOUND)
        deps.worldRepo.soundArea(source, ZebakSynths.JUGS_SHOOT, radius = SOUND_RADIUS)
        val mouth = room.coords(ZebakCoords.PROJECTILE_START)
        val cycle = deps.mapClock.cycle
        for (tile in acid) {
            val flight = deps.worldRepo.lob(ZebakSpots.ACID, mouth, tile)
            room.landings.at(cycle + flight) { room.poison.land(listOf(tile)) }
        }
        for (tile in jugs) {
            val flight = deps.worldRepo.lob(ZebakSpots.JUG, mouth, tile)
            room.landings.at(cycle + flight) { room.jugs.land(listOf(tile), room.targets()) }
        }
    }

    fun shakeCameras() {
        val leftRight = deps.random.of(SHAKE_LEFT_RIGHT)
        val upDown = deps.random.of(SHAKE_UP_DOWN)
        val forwards = deps.random.of(SHAKE_FORWARDS)
        for (player in room.targets()) {
            Camera.camReset(player)
            Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, leftRight, 0, 0)
            Camera.camShake(player, CamShakeAxis.UP_DOWN, upDown, 0, 0)
            Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, forwards, 0, 0)
            player.soundSynth(ZebakSynths.RUMBLING)
        }
    }

    fun resetCameras() {
        for (player in room.targets()) Camera.camReset(player)
    }

    fun spawnRow() {
        val base = room.coords(if (fromSouth) ZebakCoords.WAVE_SOUTH else ZebakCoords.WAVE_NORTH)
        val gap = if (lastGap == -1) deps.random.of(0, GAP_RANGE) else GAP_RANGE - lastGap
        val gapWidth = GAP_WIDTH - room.pathTier
        val dz = if (fromSouth) 1 else -1
        for (x in 0 until ROW_LENGTH) {
            val column = x - SOLID_COLUMNS
            val towards = if (gap < GAP_RANGE / 2) 1 else -1
            val inGap = column >= 0 && (0 until gapWidth).any { column == gap + it * towards }
            if (!inGap) room.waves.spawn(base.translate(x, 0), dz)
        }
        lastGap = if (lastGap == -1) gap else -1
    }

    fun end() {
        room.waves.fromSouth = !fromSouth
    }

    private companion object {
        const val SOUND_RADIUS = 15
        const val ACID_POOLS = 16
        const val ROW_LENGTH = 21
        const val SOLID_COLUMNS = 4
        const val GAP_RANGE = 12
        const val GAP_WIDTH = 3
        val SHAKE_LEFT_RIGHT = 5..7
        val SHAKE_UP_DOWN = 7..8
        val SHAKE_FORWARDS = 6..6
    }
}

internal class ZebakWaves(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val waves = HashMap<Npc, Wave>()

    private class Wave(val dz: Int, var ticksLeft: Int)

    var fromSouth = true

    fun spawn(tile: CoordGrid, dz: Int) {
        val facing = if (dz > 0) Direction.North else Direction.South
        waves[room.spawn(ZebakNpcs.WAVE, tile, facing)] = Wave(dz, LIFETIME)
    }

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
        for (cloud in clouds) room.bloodMagic.removeCloud(cloud)
        if (clouds.isNotEmpty() or washDecor(here)) {
            npc.transmog(npcType(ZebakNpcs.WAVE_BLOODY), Int.MAX_VALUE)
        }
        step(npc, next)
    }

    private fun washDecor(tile: CoordGrid): Boolean {
        val decor = deps.locRepo.findExact(tile, LocShape.GroundDecor) ?: return false
        deps.locRepo.del(decor, Int.MAX_VALUE)
        return decor.id in bloodSplatIds
    }

    private fun step(npc: Npc, next: CoordGrid) {
        if (room.canStep(npc.coords, next.x - npc.coords.x, next.z - npc.coords.z)) {
            npc.walk(next)
        } else {
            npc.teleport(deps.collision, next)
        }
    }

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
        fromSouth = true
    }

    private companion object {
        val bloodSplatIds: Set<Int> by lazy {
            ZebakLocs.BLOOD_SPLATS.mapTo(HashSet()) { it.asRSCM(RSCMType.LOC) }
        }
        const val LIFETIME = 23
        const val PUSH_TILES = 4
        const val WATER_JUMP = 5
        const val MIN_DAMAGE = 6
        const val MAX_DAMAGE = 10
    }
}
