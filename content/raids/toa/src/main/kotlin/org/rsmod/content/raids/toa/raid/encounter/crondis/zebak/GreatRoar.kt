package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.abs
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

internal class GreatRoar(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private var boulderTiles: List<CoordGrid> = emptyList()

    var launched = false
        private set

    fun launch() {
        val boulders = boulderTiles() ?: return
        val jugPlan = jugTiles(boulders) ?: return
        boulderTiles = boulders
        launched = true
        val floor = room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, boulders)
        val acid = floor.take(ACID_POOLS)
        val source = room.coords(ZebakCoords.THROW_SOUND)
        deps.worldRepo.soundArea(source, ZebakSynths.JUGS_SHOOT, radius = SOUND_RADIUS)

        for (tile in acid) throwAt(ZebakSpots.ACID, tile) { room.poison.land(listOf(tile)) }
        for (boulder in boulders) {
            val behind = boulder.translate(BOULDER_ACID_DX, 0)
            if (behind in acid) continue
            throwAt(ZebakSpots.POISON_SPREAD, behind) {
                room.poison.add(behind, spread = true, guaranteed = true)
            }
        }
        for (boulder in boulders) throwAt(ZebakSpots.BOULDER, boulder) { landBoulder(boulder) }
        for (jug in jugPlan) {
            throwAt(ZebakSpots.JUG, jug) { room.jugs.land(listOf(jug), room.targets()) }
        }
    }

    private fun throwAt(spot: String, tile: CoordGrid, landed: () -> Unit) {
        val mouth = room.coords(ZebakCoords.PROJECTILE_START)
        room.landings.at(deps.mapClock.cycle + deps.worldRepo.lob(spot, mouth, tile), landed)
    }

    private fun landBoulder(tile: CoordGrid) {
        room.boulders.spawn(tile)
        for (player in room.targets()) {
            if (player.coords != tile) continue
            player.hitTypeless(deps.random.of(LANDING_MIN, LANDING_MAX))
            knockOffBoulder(player, tile)
        }
    }

    fun roarWave(first: Boolean) {
        val min = room.coords(ZebakCoords.GROUND_MIN)
        val max = room.coords(ZebakCoords.GROUND_MAX)
        val middle = room.coords(ZebakCoords.MIDDLE)
        val dust = spotanim(ZebakSpots.DUST)
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                val tile = CoordGrid(x, z, min.level)
                if (inSafeStrip(tile, includeBoulder = false) || !room.isOpenFloor(tile)) continue
                deps.worldRepo.spotanimMap(dust, tile, delay = 1 + chebyshev(tile, middle))
            }
        }
        for (boulder in room.boulders.npcs.toList()) {
            boulder.queueNpcHit(1, HitType.Typeless, BOULDER_DAMAGE, NOOP_NPC_MODIFIER)
        }
        room.afterHazards {
            for (player in room.targets()) {
                if (!inSafeStrip(player.coords, includeBoulder = true)) push(player)
            }
        }
        if (first) room.jugs.roarHit()
    }

    fun end() {
        room.boulders.clear()
    }

    private fun inSafeStrip(tile: CoordGrid, includeBoulder: Boolean): Boolean =
        boulderTiles.any { b ->
            tile.z == b.z &&
                tile.x <= b.x + SAFE_STRIP &&
                (tile.x > b.x || (includeBoulder && tile.x == b.x))
        }

    private fun push(player: Player) {
        var dest = player.coords
        for (i in 0 until PUSH_TILES) {
            if (!room.canStep(dest, 1, 0)) break
            dest = dest.translate(1, 0)
        }
        room.pushPlayer(player, dest, Direction.West, ZebakSynths.PLAYER_PUSHED)
        val base = room.maxHit(ROAR_BASE_DAMAGE)
        player.hitTypeless(deps.random.of(base, base + ROAR_DAMAGE_SPREAD))
    }

    @OptIn(InternalApi::class)
    private fun knockOffBoulder(player: Player, boulder: CoordGrid) {
        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx == 0 && dz == 0) continue
                val next = boulder.translate(dx, dz)
                if (!room.isOpenFloor(next)) continue
                val start = player.coords
                deps.launcher.launchLenient(player) {
                    anim(ZebakSeqs.PLAYER_KNOCKED)
                    val face = faceAngle(-dx, -dz)
                    exactMove(start, next, 0, KNOCK_OFF_CYCLES, face, TeleportType.Exempt)
                }
                return
            }
        }
    }

    private fun boulderTiles(): List<CoordGrid>? {
        val free = room.freeTiles(ZebakCoords.BOULDER_MIN, ZebakCoords.BOULDER_MAX, emptyList())
        if (free.isEmpty()) return null
        val freeSet = free.toHashSet()
        val base = free[0]
        val candidates = ArrayList<CoordGrid>()
        for (dz in -BOULDER_ROWS..BOULDER_ROWS) {
            val row = ArrayList<CoordGrid>()
            for (dx in -BOULDER_COLUMNS..BOULDER_COLUMNS) {
                val tile = base.translate(dx, dz)
                val fits = tile in freeSet && tile.translate(1, 0) in freeSet
                if (fits && tile !in candidates) row += tile
            }
            if (row.isNotEmpty()) candidates += row[deps.random.of(maxExclusive = row.size)]
        }
        if (candidates.isEmpty()) return null
        val count = if (room.teamSize > 1) BOULDERS_TEAM else BOULDERS_SOLO
        return deps.random.shuffled(candidates).take(count)
    }

    private fun jugTiles(boulders: List<CoordGrid>): List<CoordGrid>? {
        val min = room.coords(ZebakCoords.GROUND_MIN)
        val max = room.coords(ZebakCoords.GROUND_MAX)
        val solving = ArrayList<CoordGrid>()
        val decoys = ArrayList<CoordGrid>()
        for (tile in room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, boulders)) {
            if (tile.x == min.x || tile.z == min.z || tile.x == max.x || tile.z == max.z) continue
            val lines = deps.random.shuffled(boulders).any { linesUp(tile, it) }
            if (lines) solving += tile else decoys += tile
        }
        if (solving.size < boulders.size) return null
        val total = deps.random.of(ZebakJugs.THROWN_MIN, ZebakJugs.THROWN_MAX)
        val picked = ArrayList(solving.take(boulders.size))
        picked += decoys.take(min(total - picked.size, picked.size))
        return picked
    }

    private fun linesUp(tile: CoordGrid, boulder: CoordGrid): Boolean {
        val dx = boulder.x - tile.x
        val dz = boulder.z - tile.z
        val adx = abs(dx)
        val adz = abs(dz)
        if ((adx == 0 || dx > 0) && adz == 0) return false
        if (adz < 2 && dx > -4 && dx < 0) return false
        if (adx > 10 || adz > 10) return false
        return abs(adz - adx) < 2 || dx in -2..0 || adz <= 1
    }

    private companion object {
        const val ACID_POOLS = 6
        const val BOULDER_ACID_DX = 2
        const val BOULDER_ROWS = 6
        const val BOULDER_COLUMNS = 3
        const val BOULDERS_TEAM = 2
        const val BOULDERS_SOLO = 3
        const val BOULDER_DAMAGE = 50
        const val SAFE_STRIP = 3
        const val PUSH_TILES = 2
        const val ROAR_BASE_DAMAGE = 20
        const val ROAR_DAMAGE_SPREAD = 10
        const val LANDING_MIN = 2
        const val LANDING_MAX = 5
        const val KNOCK_OFF_CYCLES = 30
        const val SOUND_RADIUS = 15
    }
}
