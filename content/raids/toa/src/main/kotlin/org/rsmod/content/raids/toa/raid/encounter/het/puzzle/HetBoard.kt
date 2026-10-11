package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import org.rsmod.api.random.GameRandom
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

internal class HetMirror(var rotation: Int, val loose: Boolean, var dirty: Boolean) {
    lateinit var loc: LocInfo

    val clean: Boolean
        get() = !dirty
}

internal class HetBoard(private val room: HetPuzzleEncounter) {
    private val deps = room.raid.deps
    private val barriers = HashMap<CoordGrid, Barrier>()
    private val mirrors = HashMap<CoordGrid, HetMirror>()
    private val broken = ArrayList<LocInfo>()
    private var generation = 0

    private class Barrier(val type: HetBarrierType, val rotation: Int, var loc: LocInfo)

    private val level: Int by lazy { room.coords(HetCoords.SEAL).level }
    private val boundsMin: CoordGrid by lazy { room.coords(HetCoords.MIN) }
    private val boundsMax: CoordGrid by lazy { room.coords(HetCoords.MAX) }
    private val casterMin: CoordGrid by lazy { room.coords(HetCoords.CASTER_MIN) }
    private val casterMax: CoordGrid by lazy { room.coords(HetCoords.CASTER_MAX) }
    private val targetMin: CoordGrid by lazy { room.coords(HetCoords.TARGET_MIN) }
    private val targetMax: CoordGrid by lazy { room.coords(HetCoords.TARGET_MAX) }
    private val sealMin: CoordGrid by lazy { room.coords(HetCoords.SEAL_MIN) }
    private val sealMax: CoordGrid by lazy { room.coords(HetCoords.SEAL_MAX) }

    val field = HetField { x, z -> cellAt(CoordGrid(x, z, level)) }

    fun build(layout: HetLayout) {
        clear()
        generation++
        for (spec in layout.barriers) {
            val tile = room.coords(spec.tile)
            val loc = addLoc(tile, spec.type.rising, spec.rotation, LocShape.CentrepieceStraight)
            barriers[tile] = Barrier(spec.type, spec.rotation, loc)
        }
        val current = generation
        room.schedule(RISE_TICKS) { if (current == generation) solidify() }

        val groupRun = room.teamSize > 1
        for (spec in layout.fixedMirrors) {
            val dirty = groupRun && deps.random.randomBoolean(DIRTY_CHANCE)
            addMirror(room.coords(spec.tile), HetMirror(spec.rotation, loose = false, dirty))
        }
        for (spawn in HetCoords.MIRROR_SPAWNS) {
            addMirror(
                room.coords(spawn.tile),
                HetMirror(spawn.rotation, loose = true, dirty = false),
            )
        }
    }

    fun clear() {
        generation++
        for (barrier in barriers.values) erase(barrier.loc)
        for (mirror in mirrors.values) erase(mirror.loc)
        for (loc in broken) erase(loc)
        barriers.clear()
        mirrors.clear()
        broken.clear()
    }

    fun mirrorAt(tile: CoordGrid): HetMirror? = mirrors[tile]

    fun isBreakable(tile: CoordGrid): Boolean = barriers[tile]?.type?.breakable == true

    fun breakBarrier(tile: CoordGrid): Boolean {
        val barrier = barriers[tile] ?: return false
        if (!barrier.type.breakable) return false
        barriers.remove(tile)
        broken += addLoc(tile, BROKEN_LOC, barrier.rotation, LocShape.CentrepieceStraight)
        return true
    }

    fun canHold(tile: CoordGrid): Boolean =
        tile !in mirrors &&
            tile !in barriers &&
            !insideStatue(tile) &&
            !outOfBounds(tile) &&
            !deps.collision.isWalkBlocked(tile)

    fun place(tile: CoordGrid, mirror: HetMirror) {
        addMirror(tile, mirror)
        deps.worldRepo.spotanimMap(spotanim(HetSpots.MIRROR_PLACE), tile)
    }

    fun restore(tile: CoordGrid, mirror: HetMirror) = addMirror(tile, mirror)

    fun take(tile: CoordGrid): HetMirror? {
        val mirror = mirrors[tile] ?: return null
        if (!mirror.loose || mirror.dirty) return null
        mirrors.remove(tile)
        erase(mirror.loc)
        return mirror
    }

    fun rotate(tile: CoordGrid, delta: Int) {
        val mirror = mirrors[tile] ?: return
        mirror.rotation = Math.floorMod(mirror.rotation + delta, ROTATIONS)
        refresh(tile, mirror)
    }

    fun push(tile: CoordGrid, dir: HetDir): Boolean {
        val mirror = mirrors[tile] ?: return false
        if (!mirror.loose || mirror.dirty) return false
        val dest = tile.translate(dir.dx, dir.dz)
        if (!canHold(dest)) return false
        mirrors.remove(tile)
        erase(mirror.loc)
        addMirror(dest, mirror)
        return true
    }

    fun clean(tile: CoordGrid): Boolean {
        val mirror = mirrors[tile] ?: return false
        if (!mirror.dirty) return false
        mirror.dirty = false
        refresh(tile, mirror)
        return true
    }

    fun orbHitsMirror(tile: CoordGrid) {
        if (room.teamSize <= 1) return
        val mirror = mirrors[tile] ?: return
        if (mirror.dirty) return
        mirror.dirty = true
        refresh(tile, mirror)
    }

    fun orbBlocked(tile: CoordGrid): Boolean =
        tile in mirrors ||
            tile in barriers ||
            insideStatue(tile) ||
            outOfBounds(tile) ||
            deps.collision.isWalkBlocked(tile)

    fun orbSpawnTile(dir: HetDir, random: GameRandom): CoordGrid? {
        val minX = boundsMin.x + HetCoords.ORB_LINE_MIN_X - HetCoords.MIN.x
        val maxX = boundsMin.x + HetCoords.ORB_LINE_MAX_X - HetCoords.MIN.x
        val minZ = boundsMin.z + HetCoords.ORB_LINE_MIN_Z - HetCoords.MIN.z
        val maxZ = boundsMin.z + HetCoords.ORB_LINE_MAX_Z - HetCoords.MIN.z
        val line = if (dir.vertical) random.of(minX, maxX) else random.of(minZ, maxZ)
        for (offset in 0..ORB_SPAWN_SCAN) {
            val tile =
                when (dir) {
                    HetDir.East -> CoordGrid(boundsMin.x + offset, line, level)
                    HetDir.West -> CoordGrid(boundsMax.x - offset, line, level)
                    HetDir.North -> CoordGrid(line, boundsMin.z + offset, level)
                    HetDir.South -> CoordGrid(line, boundsMax.z - offset, level)
                }
            if (!orbBlocked(tile)) return tile
        }
        return null
    }

    private fun solidify() {
        for (barrier in barriers.values) {
            val tile = barrier.loc.coords
            barrier.loc = addLoc(tile, barrier.type.loc, barrier.rotation, STRAIGHT)
        }
    }

    private fun cellAt(tile: CoordGrid): HetCell {
        val mirror = mirrors[tile]
        return when {
            outOfBounds(tile) -> HetCell.Wall
            mirror != null -> if (mirror.dirty) HetCell.Wall else HetCell.Mirror(mirror.rotation)
            tile in barriers -> HetCell.Wall
            tile.within(targetMin, targetMax) -> HetCell.Target
            tile.within(casterMin, casterMax) || tile.within(sealMin, sealMax) -> HetCell.Wall
            deps.collision.isWalkBlocked(tile) -> HetCell.Wall
            else -> HetCell.Open
        }
    }

    private fun insideStatue(tile: CoordGrid): Boolean =
        tile.within(casterMin, casterMax) ||
            tile.within(targetMin, targetMax) ||
            tile.within(sealMin, sealMax)

    private fun outOfBounds(tile: CoordGrid): Boolean = !tile.within(boundsMin, boundsMax)

    private fun addMirror(tile: CoordGrid, mirror: HetMirror) {
        mirrors[tile] = mirror
        mirror.loc = addLoc(tile, mirrorLoc(mirror), mirror.rotation, LocShape.CentrepieceDiagonal)
    }

    private fun refresh(tile: CoordGrid, mirror: HetMirror) {
        mirror.loc = addLoc(tile, mirrorLoc(mirror), mirror.rotation, LocShape.CentrepieceDiagonal)
    }

    private fun mirrorLoc(mirror: HetMirror): String =
        when {
            mirror.dirty -> HetLocs.MIRROR_DIRTY
            mirror.loose -> HetLocs.MIRROR
            else -> HetLocs.MIRROR_FIXED
        }

    private fun erase(loc: LocInfo) {
        deps.locRepo.add(loc.coords, HetLocs.EXIT_OPEN, Int.MAX_VALUE, loc.angle, loc.shape)
    }

    private fun addLoc(tile: CoordGrid, loc: String, rotation: Int, shape: LocShape): LocInfo =
        deps.locRepo.add(tile, loc, Int.MAX_VALUE, LocAngle[rotation], shape)

    private companion object {
        const val RISE_TICKS = 2
        const val ROTATIONS = 4
        const val DIRTY_CHANCE = 50
        const val ORB_SPAWN_SCAN = 6
        const val BROKEN_LOC = "loc.toa_het_barrier_destroyed"
        val STRAIGHT = LocShape.CentrepieceStraight
    }
}
