package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.api.instances.InstanceSession
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class WardensSecondEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    override fun arrival(): Arrival =
        Arrival(coords(room.challengeSpawn ?: room.spawn), Direction.North)

    override fun onComplete() {
        schedule(CRYSTAL_DELAY) {
            deps.locRepo.add(
                coords(CRYSTAL_TILE),
                CRYSTAL,
                Int.MAX_VALUE,
                LocAngle.West,
                LocShape.CentrepieceStraight,
            )
        }
    }

    companion object {
        const val CRYSTAL = "loc.toa_teleport_crystal_continue_wardens"
        private val CRYSTAL_TILE = CoordGrid(3936, 5154, 1)
        private const val CRYSTAL_DELAY = 12
    }
}
