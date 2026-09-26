package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import kotlin.math.abs
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.soundSynth
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

/**
 * The Great Roar (Offline_Scape Zebak.shootJugs; its tick numbers):
 * - 0: acid, 2-3 boulders (3 solo) with acid behind each, 6-8 jugs, one lined up per boulder;
 * - 5: they land;
 * - 33: the roar animation;
 * - 36, 38, 40: roar waves. Anyone outside a boulder's safe strip (its row, the boulder to 3 tiles
 *   behind it) is knocked 2 tiles east for 20-30 (scaled). Boulders take 50 a wave; the first wave
 *   shatters every jug left;
 * - 49: over.
 *
 * Autos continue every 10 ticks. Offline_Scape kept that speed for the rest of the fight and never
 * ended the special if it couldn't place boulders; neither is copied.
 */
internal class GreatRoar(private val room: ZebakEncounter) : ZebakSpecial {
    private val deps = room.raid.deps
    private var ticks = -1
    private var boulderTiles: List<CoordGrid> = emptyList()
    private var jugTiles: List<CoordGrid> = emptyList()
    private var acidTiles: List<CoordGrid> = emptyList()
    private val boulderAcidTiles = ArrayList<CoordGrid>()

    override val attackSpeed: Int = ATTACK_SPEED

    override fun step(): Boolean {
        val boss = room.zebak ?: return false
        if (boss.hitpoints <= 0) return false
        ticks++
        when (ticks) {
            0 -> return launch(boss)
            LAND_TICK -> land()
            SCREAM_TICK -> scream(boss)
            in WAVE_TICKS -> roarWave(first = ticks == WAVE_TICKS.first())
            END_TICK -> {
                room.boulders.clear()
                return false
            }
        }
        return true
    }

    private fun launch(boss: Npc): Boolean {
        boss.anim(ZebakSeqs.RANGED)
        room.tail?.resetAnim()
        val middle = room.coords(ZebakCoords.MIDDLE)
        deps.worldRepo.soundArea(middle, ZebakSynths.JUGS_SHOOT, radius = SOUND_RADIUS)

        val boulders = boulderTiles() ?: return false
        val jugPlan = jugTiles(boulders) ?: return false
        boulderTiles = boulders
        jugTiles = jugPlan
        val acid = room.freeTiles(ZebakCoords.GROUND_MIN, ZebakCoords.GROUND_MAX, boulders)
            .take(ACID_POOLS)
            .toMutableList()

        val mouth = room.coords(ZebakCoords.PROJECTILE_START)
        for (tile in acid) deps.worldRepo.projectile(ZebakSpots.ACID, mouth, tile, ZebakProjs.LOB)
        for (boulder in boulders) {
            val behind = boulder.translate(BOULDER_ACID_DX, 0)
            if (behind in acid) continue
            acid += behind
            boulderAcidTiles += behind
            deps.worldRepo.projectile(ZebakSpots.POISON_SPREAD, mouth, behind, ZebakProjs.LOB)
        }
        acidTiles = acid
        for (boulder in boulders) {
            deps.worldRepo.projectile(ZebakSpots.BOULDER, mouth, boulder, ZebakProjs.LOB)
        }
        for (jug in jugPlan) deps.worldRepo.projectile(ZebakSpots.JUG, mouth, jug, ZebakProjs.LOB)
        return true
    }

    private fun land() {
        val targets = room.targets()
        if (targets.isEmpty()) return
        for (tile in boulderTiles) {
            room.boulders.spawn(tile)
            for (player in targets) {
                if (player.coords != tile) continue
                player.hitTypeless(deps.random.of(LANDING_MIN, LANDING_MAX))
                knockOffBoulder(player, tile)
            }
        }
        for (tile in boulderAcidTiles) room.poison.add(tile, spread = true, guaranteed = true)
        room.poison.land(acidTiles)
        room.jugs.land(jugTiles, targets)
    }

    private fun scream(boss: Npc) {
        room.attackCountdown = NEXT_ATTACK
        boss.anim(ZebakSeqs.ROAR)
        room.tail?.anim(ZebakSeqs.TAIL_ROAR)
        for (player in room.targets()) {
            for ((synth, delay) in ZebakSynths.SCREAM) player.soundSynth(synth, delay = delay)
        }
    }

    private fun roarWave(first: Boolean) {
        val min = room.coords(ZebakCoords.GROUND_MIN)
        val max = room.coords(ZebakCoords.GROUND_MAX)
        val middle = room.coords(ZebakCoords.MIDDLE)
        val dust = spotanim(ZebakSpots.DUST)
        for (x in min.x..max.x) {
            for (z in min.z..max.z) {
                val tile = CoordGrid(x, z, min.level)
                if (inSafeStrip(tile, includeBoulder = false) || !room.isOpenFloor(tile)) continue
                // Offline_Scape passed 1 + distance as the height; used as the ripple delay.
                deps.worldRepo.spotanimMap(dust, tile, delay = 1 + chebyshev(tile, middle))
            }
        }
        for (boulder in room.boulders.npcs.toList()) {
            boulder.queueNpcHit(1, HitType.Typeless, BOULDER_DAMAGE, NOOP_NPC_MODIFIER)
        }
        for (player in room.targets()) {
            if (!inSafeStrip(player.coords, includeBoulder = true)) push(player)
        }
        if (first) room.jugs.shatterAll()
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

    /** Offline_Scape movePlayer: a boulder landed on [player]; hop to the first open neighbour. */
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

    // ---- Placement (Offline_Scape getBoulderLocations / getJugsSolveLocations) ----

    /** One candidate per row within 6 rows of a random tile, each with an open tile east of it. */
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

    /**
     * One jug per boulder on a tile lined up with it, then up to as many decoys, 6-8 in total.
     * Offline_Scape's edge check compared both edges to the south-west corner; fixed here.
     */
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
        const val LAND_TICK = 5
        const val SCREAM_TICK = 33
        val WAVE_TICKS = intArrayOf(36, 38, 40)
        const val END_TICK = 49
        const val ATTACK_SPEED = 10
        const val NEXT_ATTACK = 11
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
