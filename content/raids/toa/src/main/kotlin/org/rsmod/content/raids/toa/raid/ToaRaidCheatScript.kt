package org.rsmod.content.raids.toa.raid

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onCommand
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.party.ToaPartySettings
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaRewardEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.wardens.WardensEncounter
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRaidCheatScript
@Inject
constructor(
    private val launcher: ProtectedAccessLauncher,
    private val deps: ToaRaidDeps,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onCommand("toababa") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Enter Ba-Ba's room, starting a raid if you are not in one"
            cheat { enterRoom(ToaRoom.APMEKEN_BOSS) }
        }
        onCommand("toawardens") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Enter the Wardens room in front of Osmumten, starting a raid if needed"
            cheat {
                enterRoom(ToaRoom.WARDENS_FIRST_ROOM) {
                    (it as? WardensEncounter)?.arriveInFrontOfGhost()
                }
            }
        }
        onCommand("toareward") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Set your loot points and enter the reward room, starting a raid if needed"
            invalidArgs = REWARD_USAGE
            cheat {
                val loot = args[0].toIntOrNull() ?: return@cheat player.mes(REWARD_USAGE)
                val force = args.getOrNull(1)?.lowercase() == "unique"
                enterRoom(ToaRoom.REWARD_ROOM) {
                    it.raid.points.setLoot(player, loot)
                    (it as? ToaRewardEncounter)?.forceUnique = force
                }
            }
        }
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
        onCommand("toalobby") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Teleport to the Tombs of Amascut lobby"
            cheat { toLobby() }
        }
        onCommand("toapoints") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Show your Tombs of Amascut reward points"
            cheat { showPoints() }
        }
    }

    private fun Cheat.enterRoom(room: ToaRoom, prepare: (ToaEncounter) -> Unit = {}) {
        launcher.launch(player, busyText = "Close your dialog first.") {
            val inside = player.currentRaid
            val raid = inside ?: startRaid(player)
            if (raid == null) {
                mes(TOA_UNAVAILABLE)
                return@launch
            }
            val target = resolveTarget(raid, room, checkLeader = false) ?: return@launch
            prepare(target)
            travel(target, fromLobby = inside == null)
        }
    }

    private fun startRaid(player: Player): ToaRaid? {
        val cycle = deps.mapClock.cycle
        val party =
            player.currentParty
                ?: ToaPartyManager.createParty(player, ToaPartySettings(), cycle)
                ?: return null
        ToaRaidManager.raidFor(party)?.let { return it }
        val raid = ToaRaidManager.start(party, deps) ?: return null
        ToaPartyManager.onRaidStarted(party)
        return raid
    }

    private fun Cheat.toLobby() {
        launcher.launch(player, busyText = "Close your dialog first.") {
            if (player.currentRaid != null) {
                exitRaid()
            } else {
                telejump(TOA_OUTSIDE, TeleportType.Exempt)
                faceDirection(Direction.North)
            }
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

    private companion object {
        const val REWARD_USAGE = "Usage: ::toareward <loot points> [unique]"
    }
}
