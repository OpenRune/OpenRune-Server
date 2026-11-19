package org.rsmod.content.quest.area.varrock.shieldofarrav

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

class ShieldOfArravDoors @Inject constructor(private val locRepo: LocRepository) {
    /**
     * Steps the player through a gang door to the tile across it. With [openDoor], the door is
     * swapped for an invisible wall and its open leaf for [OPEN_TICKS], as the Black Arm door does
     * in capture 3219.
     */
    suspend fun pass(access: ProtectedAccess, door: BoundLocInfo, openDoor: String? = null) {
        val waypoints = route(door, access.player.coords)
        access.soundSynth(DOOR_SOUND)
        if (openDoor != null) {
            val openedAt = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)
            val openTicks = OPEN_TICKS + waypoints.size
            locRepo.change(door, INVISIBLE_WALL, openTicks)
            locRepo.add(openedAt, openDoor, openTicks, door.turnAngle(1), door.shape)
        }
        access.forcedWalk(waypoints, waypoints.size)
    }

    /**
     * Opens a cupboard or chest the way capture 1710 does: face it the tick after arriving, play
     * the open animation and sound a tick later, then swap in [openLoc] on the next tick.
     */
    suspend fun openContainer(access: ProtectedAccess, loc: BoundLocInfo, openLoc: String) {
        access.arriveDelay()
        access.delay(1)
        access.anim(OPEN_ANIM, delay = SOUND_DELAY)
        access.soundSynth(OPEN_CONTAINER_SOUND, delay = SOUND_DELAY)
        access.delay(1)
        locRepo.change(loc, openLoc, OPEN_TICKS)
        access.soundSynth(CLOSE_CONTAINER_SOUND, delay = CLOSE_SOUND_DELAY)
    }

    companion object {
        const val DOOR_SOUND = "synth.nicedoor_open"
        const val LOCKED_SOUND = "synth.locked"
        const val INVISIBLE_WALL = "loc.inviswall"

        /**
         * Ticks a door or container stays open. Doors add a tick per step of the walk through, so
         * a player who opened one from beside it is clear before it shuts.
         */
        const val OPEN_TICKS = 3

        /** True on the door's own side of its wall edge, which includes the tiles beside it. */
        fun onDoorSide(door: BoundLocInfo, coords: CoordGrid): Boolean =
            when (door.angle.id) {
                WEST_EDGE -> coords.x >= door.coords.x
                NORTH_EDGE -> coords.z <= door.coords.z
                EAST_EDGE -> coords.x <= door.coords.x
                else -> coords.z >= door.coords.z
            }

        private fun route(door: BoundLocInfo, coords: CoordGrid): List<CoordGrid> {
            if (!onDoorSide(door, coords)) {
                return listOf(door.coords)
            }
            val beyond = beyond(door)
            return if (coords == door.coords) listOf(beyond) else listOf(door.coords, beyond)
        }

        private fun beyond(door: BoundLocInfo): CoordGrid {
            val c = door.coords
            return when (door.angle.id) {
                WEST_EDGE -> CoordGrid(c.x - 1, c.z, c.level)
                NORTH_EDGE -> CoordGrid(c.x, c.z + 1, c.level)
                EAST_EDGE -> CoordGrid(c.x + 1, c.z, c.level)
                else -> CoordGrid(c.x, c.z - 1, c.level)
            }
        }

        private const val OPEN_ANIM = "seq.human_openchest"
        private const val OPEN_CONTAINER_SOUND = "synth.cupboard_open"
        private const val CLOSE_CONTAINER_SOUND = "synth.cupboard_close"
        private const val SOUND_DELAY = 10
        private const val CLOSE_SOUND_DELAY = 45
        private const val WEST_EDGE = 0
        private const val NORTH_EDGE = 1
        private const val EAST_EDGE = 2
    }
}
