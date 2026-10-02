package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.annotations.InternalApi
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.travel
import org.rsmod.game.region.Region

class WardensFirstEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    override fun debugComplete() {
        start()
        startFinalPhase()
    }

    // TODO: flash interface 174 white here instead of the normal fade.
    @OptIn(InternalApi::class)
    fun startFinalPhase() {
        if (stage != ToaStage.STARTED) return
        val next = raid.advanceTo(ToaRoom.WARDENS_SECOND_ROOM) ?: return
        next.continueChallenge(this)
        stopTasks()
        for (player in players) {
            deps.launcher.launchLenient(player) { travel(next, fromLobby = false) }
        }
    }
}
