package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * The second Wardens room (phase 3). Beating it ends the raid; 12 ticks later a teleport crystal
 * appears that leads to the reward room (Offline_Scape SecondWardenEncounter.onRoomEnd).
 */
class WardensSecondEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    /**
     * Players arrive mid-fight, so they land in the arena facing north. Offline_Scape
     * `preparePhase` put dead players on the spawn tile instead; revisit with deaths (v15).
     */
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
        /** op1 Use; handled in ToaRaidScript. */
        const val CRYSTAL = "loc.toa_teleport_crystal_continue_wardens"
        private val CRYSTAL_TILE = CoordGrid(3936, 5154, 1)
        private const val CRYSTAL_DELAY = 12
    }
}
