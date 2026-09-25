package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.annotations.InternalApi
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.travel
import org.rsmod.game.region.Region

/**
 * The first Wardens room (phases 1-2). The fight isn't completed here: when phase 2 ends, the
 * whole room moves into [ToaRoom.WARDENS_SECOND_ROOM] for phase 3, and the challenge carries on
 * there (Offline_Scape WardenEncounter.startLastPhase).
 */
class WardensFirstEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    /** `::toacomplete` skips phases 1-2. */
    override fun debugComplete() {
        start()
        startFinalPhase()
    }

    /**
     * Builds the second room, hands it the running challenge, and moves everyone here into it.
     * This room is torn down automatically once the last player has left.
     *
     * TODO (Phase B): Offline_Scape flashes interface 174 white here instead of the normal fade.
     */
    @OptIn(InternalApi::class)
    fun startFinalPhase() {
        if (stage != ToaStage.STARTED) return
        val next = raid.advanceTo(ToaRoom.WARDENS_SECOND_ROOM) ?: return
        next.continueChallenge(this)
        stopTasks()
        for (player in players) {
            // A forced move, like a cutscene: it must happen even mid-dialogue.
            deps.launcher.launchLenient(player) { travel(next, fromLobby = false) }
        }
    }
}
