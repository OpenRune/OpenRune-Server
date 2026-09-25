package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * The nexus. Port of Offline_Scape MainHallEncounter.
 *
 * Built fresh every time the party comes back (see [ToaRaid]), so its doors always show the
 * raid's progress: completed paths closed, the Wardens door open once all four are done.
 *
 * Not ported yet: the helpful spirit and its supplies (after 2 and 4 paths).
 */
class MainHallEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    /** The path the leader picked in this visit, or `null`. */
    var selectedPath: ToaPath? = null
        private set

    /** Path levels added by this visit, for the "rumbling" messages. */
    private val levelIncreases = IntArray(ToaPath.entries.size)

    /**
     * First visit: the capture's spawn, facing south. Coming back from a path: next to that path's
     * door, facing away from it (Offline_Scape TOAManager.enter).
     */
    override fun arrival(): Arrival {
        val path = raid.lastPath
        if (path != null && raid.pathsCompleted.isNotEmpty()) {
            val tile = path.returnTile.translate(0, deps.random.of(0, path.returnSpreadZ))
            return Arrival(coords(tile), path.returnFacing)
        }
        return Arrival(coords(room.randomSpawn(deps.random)), Direction.South)
    }

    override fun onBuilt() {
        addPathLevels()
        for (path in ToaPath.entries) {
            val door = if (path in raid.pathsCompleted) path.doorClosed else path.doorOpen
            spawnDoor(path, door)
        }
        // Capture: the Wardens door is added 3 ticks after the path doors.
        val wardensDoor =
            if (raid.pathsCompleted.size == ToaPath.entries.size) WARDENS_DOOR_OPEN else WARDENS_DOOR
        schedule(WARDENS_DOOR_DELAY) {
            deps.locRepo.add(
                coords(WARDENS_DOOR_TILE),
                wardensDoor,
                Int.MAX_VALUE,
                LocAngle.East,
                LocShape.CentrepieceStraight,
            )
        }
    }

    override fun onEnter(player: Player) {
        for (path in ToaPath.entries) {
            if (levelIncreases[path.ordinal] != 0) {
                player.mes("You hear a mysterious rumbling coming from the Path of ${path.pathName}.")
            }
        }
    }

    /**
     * The leader chose [path]: the other open doors become "unselected" (Offline_Scape
     * `setStartedPath`).
     */
    fun select(path: ToaPath) {
        selectedPath = path
        raid.lastPath = path
        for (other in ToaPath.entries) {
            if (other != path && other !in raid.pathsCompleted) {
                spawnDoor(other, other.doorUnselected)
            }
        }
    }

    private fun spawnDoor(path: ToaPath, loc: String) {
        deps.locRepo.add(
            coords(path.door),
            loc,
            Int.MAX_VALUE,
            path.doorAngle,
            LocShape.CentrepieceStraight,
        )
    }

    /**
     * Offline_Scape `constructed`: Pathseeker/finder/master raise every path once, on the first
     * visit. Walk the Path raises random unfinished paths after the 1st (+2), 2nd (+1) and
     * 3rd (+1) path.
     */
    private fun addPathLevels() {
        if (!raid.pathLevelsInitialised) {
            raid.pathLevelsInitialised = true
            val base =
                when {
                    raid.isActive("Pathmaster") -> 3
                    raid.isActive("Pathfinder") -> 2
                    raid.isActive("Pathseeker") -> 1
                    else -> 0
                }
            levelIncreases.fill(base)
        }

        if (raid.isActive("Walk the Path")) {
            val rolls =
                when (raid.pathsCompleted.size) {
                    1 -> 2
                    2, 3 -> 1
                    else -> 0
                }
            val remaining = ToaPath.entries.filter { it !in raid.pathsCompleted }
            repeat(rolls) {
                if (remaining.isNotEmpty()) {
                    val path = remaining[deps.random.of(maxExclusive = remaining.size)]
                    levelIncreases[path.ordinal]++
                }
            }
        }

        for (i in levelIncreases.indices) {
            raid.pathLevels[i] += levelIncreases[i]
        }
    }

    private companion object {
        const val WARDENS_DOOR = "loc.toa_nexus_wardens_door"
        const val WARDENS_DOOR_OPEN = "loc.toa_nexus_wardens_door_open"

        /** Capture: toa_nexus_wardens_door at (3548, 5134), angle 2. */
        val WARDENS_DOOR_TILE = CoordGrid(3548, 5134, 0)
        const val WARDENS_DOOR_DELAY = 3
    }
}
