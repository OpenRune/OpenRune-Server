package org.rsmod.content.areas.misc.warriorsguild

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.content.generic.locs.doors.DoorTranslations
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

/**
 * Guild doors never stay open: the leaf is lifted for a couple of cycles, an inert open leaf is
 * shown beside it, and the player takes a single step through.
 */
@Singleton
class GuildDoors @Inject constructor(private val locRepo: LocRepository) {
    /**
     * The tile the player steps onto if they pass through [door] from where they stand. The side is
     * judged against the wall line, not the door tile, as a player can reach a wall from beside it.
     */
    fun destination(access: ProtectedAccess, door: BoundLocInfo): CoordGrid =
        if (onDoorSide(access.coords, door)) beyond(door) else door.coords

    private fun onDoorSide(coords: CoordGrid, door: BoundLocInfo): Boolean =
        when (door.angle) {
            LocAngle.West -> coords.x >= door.coords.x
            LocAngle.North -> coords.z <= door.coords.z
            LocAngle.East -> coords.x <= door.coords.x
            LocAngle.South -> coords.z >= door.coords.z
        }

    suspend fun pass(
        access: ProtectedAccess,
        door: BoundLocInfo,
        openLoc: String,
        sound: String = DOOR_SOUND,
        partner: String? = null,
    ) {
        val dest = destination(access, door)
        val start = if (dest == door.coords) beyond(door) else door.coords
        if (access.coords != start) {
            access.walk(start)
            access.delay(APPROACH_TICKS)
            if (access.coords != start) return
        }
        val openCoords = DoorTranslations.translateOpen(door.coords, door.shape, door.angle)
        locRepo.del(door, OPEN_CYCLES)
        locRepo.add(openCoords, openLoc, OPEN_CYCLES, openAngle(door, partner), door.shape)
        access.soundSynth(sound)
        access.playerWalk(dest)
    }

    /**
     * A leaf of a double door swings to the outer side, away from its [partner]. The same pair of
     * loc types is placed in both handednesses, so the side is found from where the partner stands.
     */
    private fun openAngle(door: BoundLocInfo, partner: String?): LocAngle {
        val clockwise = door.turnAngle(rotations = 1)
        if (partner == null) return clockwise
        val towardsPartner = locRepo.findLoc(edgeNeighbour(door.coords, clockwise), partner)
        return if (towardsPartner) door.turnAngle(rotations = 3) else clockwise
    }

    private fun edgeNeighbour(coords: CoordGrid, angle: LocAngle): CoordGrid =
        when (angle) {
            LocAngle.West -> coords.translateX(-1)
            LocAngle.North -> coords.translateZ(1)
            LocAngle.East -> coords.translateX(1)
            LocAngle.South -> coords.translateZ(-1)
        }

    private fun beyond(door: BoundLocInfo): CoordGrid = edgeNeighbour(door.coords, door.angle)

    companion object {
        const val DOOR_SOUND = "synth.big_wooden_door_open"
        const val FRONT_DOOR_SOUND = "synth.new_door_open"
        private const val OPEN_CYCLES = 2

        /** Scripts resume before movement, so a one-tile step is only visible a tick later. */
        private const val APPROACH_TICKS = 2
    }
}
