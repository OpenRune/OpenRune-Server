package org.rsmod.content.raids.toa.raid

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Debug commands for walking the raid end to end while the rooms are still empty.
 *
 * - `::toacomplete` finishes the room you're in, as if its puzzle or boss were beaten. In the
 *   first Wardens room it moves everyone on to the second.
 * - `::toanext` takes the room's way forward without reaching its loc (useful while a puzzle's
 *   end barrier still blocks the exit).
 * - `::toapoints` shows your hidden reward points: total, loot (total minus the starting 5,000)
 *   and the current room's.
 */
class ToaRaidCheatScript @Inject constructor(private val launcher: ProtectedAccessLauncher) :
    PluginScript() {

    override fun ScriptContext.startup() {
        onCommand("toacomplete") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Complete your current Tombs of Amascut room"
            cheat { completeRoom() }
        }
        onCommand("toanext") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Move on to the next Tombs of Amascut room"
            cheat { nextRoom() }
        }
        onCommand("toapoints") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Show your Tombs of Amascut reward points"
            cheat { showPoints() }
        }
    }

    private fun Cheat.showPoints() {
        val points = player.currentRaid?.points
        if (points == null) {
            player.mes("You are not in a Tombs of Amascut raid.")
            return
        }
        val room = points.roomPoints(player)
        val loot = points.lootPoints(player)
        player.mes("Points: total ${points.total(player)}, loot $loot, room $room.")
        player.mes("TOA plugin should show: Total ${loot + room}, Room $room.")
    }

    private fun Cheat.completeRoom() {
        val raid = player.currentRaid
        val room = raid?.encounterOf(player)
        if (room == null) {
            player.mes("You are not inside a Tombs of Amascut room.")
            return
        }
        if (room.stage == ToaStage.COMPLETED) {
            player.mes("${room.room} is already complete.")
            return
        }
        room.debugComplete()
        player.mes("Completed ${room.room}.")
    }

    private fun Cheat.nextRoom() {
        val raid = player.currentRaid
        val room = raid?.encounterOf(player)
        if (raid == null || room == null) {
            player.mes("You are not inside a Tombs of Amascut room.")
            return
        }
        val next = room.room.next
        if (next == null) {
            player.mes("${room.room} has no next room. Use a path door.")
            return
        }
        launcher.launch(player, busyText = "Close your dialog first.") {
            proceed(raid, next, "has proceeded to the next challenge")
        }
    }
}
