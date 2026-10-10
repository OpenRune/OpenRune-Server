package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class MainHallEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    var selectedPath: ToaPath? = null
        private set

    private val levelIncreases = IntArray(ToaPath.entries.size)

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
        if (raid.supplies.offerIfDue(raid)) {
            spawn(SPIRIT, coords(SPIRIT_TILE))
            deps.locRepo.add(
                coords(DEPOSIT_POT_TILE),
                DEPOSIT_POT,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
        for (path in ToaPath.entries) {
            val door = if (path in raid.pathsCompleted) path.doorClosed else path.doorOpen
            spawnDoor(path, door)
        }

        val wardensDoor =
            if (raid.pathsCompleted.size == ToaPath.entries.size) {
                WARDENS_DOOR_OPEN
            } else {
                WARDENS_DOOR
            }
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
                player.mes(
                    "You hear a mysterious rumbling coming from the Path of ${path.pathName}."
                )
            }
        }

        if (raid.supplies.announce(player)) {
            player.mes("<col=0000b2>A helpful spirit has arrived with some supplies.")
        }
    }

    override fun onLeave(player: Player) {
        raid.supplies.forfeit(player)
    }

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

    private fun addPathLevels() {
        if (!raid.pathLevelsInitialised) {
            raid.pathLevelsInitialised = true
            val base =
                when {
                    raid.isActive(ToaInvocationKey.Pathmaster) -> 3
                    raid.isActive(ToaInvocationKey.Pathfinder) -> 2
                    raid.isActive(ToaInvocationKey.Pathseeker) -> 1
                    else -> 0
                }
            levelIncreases.fill(base)
        }

        if (raid.isActive(ToaInvocationKey.WalkThePath)) {
            val rolls =
                when (raid.pathsCompleted.size) {
                    1 -> 2
                    2, 3 -> 1
                    else -> 0
                }
            val remaining = ToaPath.entries.filter { it !in raid.pathsCompleted }
            for (path in deps.random.shuffled(remaining).take(rolls)) {
                levelIncreases[path.ordinal]++
            }
        }

        for (i in levelIncreases.indices) {
            raid.pathLevels[i] += levelIncreases[i]
        }
    }

    private companion object {
        const val WARDENS_DOOR = "loc.toa_nexus_wardens_door"
        const val WARDENS_DOOR_OPEN = "loc.toa_nexus_wardens_door_open"

        val WARDENS_DOOR_TILE = CoordGrid(3548, 5134, 0)
        const val WARDENS_DOOR_DELAY = 3

        const val SPIRIT = "npc.toa_midraidloot_trader"

        val SPIRIT_TILE = CoordGrid(3548, 5154, 0)

        const val DEPOSIT_POT = "loc.toa_pottery_bankdeposit"
        val DEPOSIT_POT_TILE = CoordGrid(3546, 5154, 0)
    }
}
