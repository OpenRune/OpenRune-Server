package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.forcedWalk
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.raids.toa.party.ToaPartyListScript
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRaidManager.toaController
import org.rsmod.content.raids.toa.raid.encounter.MainHallEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaRaidScript
@Inject
constructor(
    private val deps: ToaRaidDeps,
    private val partyList: ToaPartyListScript,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.toa_lobby_raid_entry") { raidEntrance() }
        for (exit in EXIT_LOCS) {
            onOpLoc1(exit) { abandonRaid() }
        }

        for (path in ToaPath.entries) {
            onOpLoc1(path.doorOpen) { pathDoor(path, quick = false) }
            onOpLoc2(path.doorOpen) { pathDoor(path, quick = true) }
            onOpLoc1(path.doorUnselected) { pathDoor(path, quick = false) }
            onOpLoc2(path.doorUnselected) { pathDoor(path, quick = true) }
        }
        onOpLoc1(WARDENS_DOOR_OPEN) { wardensDoor(quick = false) }
        onOpLoc2(WARDENS_DOOR_OPEN) { wardensDoor(quick = true) }

        onOpLoc1(BARRIER) { barrier(it.loc.coords, quick = false) }
        onOpLoc2(BARRIER) { barrier(it.loc.coords, quick = true) }
        for (crystal in TELEPORT_CRYSTALS) {
            onOpLoc1(crystal) { teleportCrystal(quick = false) }
            onOpLoc2(crystal) { teleportCrystal(quick = true) }
        }
        for (entry in PUZZLE_EXITS) {
            onOpLoc1(entry) { puzzleExit(quick = false) }
            onOpLoc2(entry) { puzzleExit(quick = true) }
        }
        onOpNpc1(ToaBossEncounter.OSMUMTEN) { osmumten() }
        onOpNpc3(ToaBossEncounter.OSMUMTEN) { osmumten() }
        onApLoc1(WardensSecondEncounter.CRYSTAL) { rewardCrystal(it.loc) }

        onAreaExit(RAID_AREA) { leftRaidArea() }

        onPlayerLogout {
            ToaRaidManager.leave(player, logout = true)
        }

        onEvent<SessionStateEvent.PrepareLogin> {
            if (player.toaController > 0) {
                player.coords = TOA_OUTSIDE
            }
        }

        onPlayerLogin {
            ToaRaidManager.resetClientVars(player)
            ToaRaidManager.removeRaidItems(player)
        }
    }

    private suspend fun ProtectedAccess.raidEntrance() {
        arriveDelay()

        val party = player.currentParty
        if (party == null) {
            val form =
                choice2(
                    "Form or join a party.",
                    true,
                    "Cancel.",
                    false,
                    title = "You are currently not in a raiding party.",
                )
            if (form) {
                with(partyList) { openPartyList() }
            }
            return
        }

        val running = ToaRaidManager.raidFor(party)
        val raid: ToaRaid
        if (running == null) {
            if (!party.isLeader(player)) {
                val leaderName = party.leader?.displayName ?: party.leaderName
                mesbox("Your leader, $leaderName, must enter first.")
                return
            }
            raid = ToaRaidManager.start(party, deps) ?: run {
                mes(TOA_UNAVAILABLE)
                return
            }
            ToaPartyManager.onRaidStarted(party)
            raid.announce(
                player,
                "${player.displayName} has entered the Tombs of Amascut. " +
                    "Step inside to join ${player.objectPronoun()}...",
            )
        } else {
            raid = running
        }

        val hall = resolveTarget(raid, ToaRoom.MAIN_HALL, checkLeader = true) ?: return

        mes("You enter the Tombs of Amascut (${raid.settings.mode} Mode)...")
        travel(hall, fromLobby = true)
    }

    private suspend fun ProtectedAccess.pathDoor(path: ToaPath, quick: Boolean) {
        arriveDelay()
        val raid = player.currentRaid ?: return
        val hall = raid.encounterOf(player) as? MainHallEncounter ?: return

        val selected = hall.selectedPath
        if (selected != null) {
            if (selected != path) {
                mesbox("You can't proceed as a different path has already been selected.")
                return
            }
            val target = resolveTarget(raid, path.puzzle, checkLeader = true) ?: return
            travel(target, fromLobby = false)
            return
        }

        if (!raid.isLeader(player)) {
            mesbox("Your leader, ${raid.leaderName}, must enter first.")
            return
        }

        val question = "Do you wish to walk the Path of ${path.pathName}"
        if (raid.stragglers().isNotEmpty()) {
            if (!confirmAbandonStragglers(raid, question)) return
        } else if (!quick && !confirmYesNo("$question?")) {
            return
        }

        if (hall.destroyed || hall.selectedPath != null) return

        val target = raid.advanceTo(path.puzzle)
        if (target == null) {
            mes(TOA_UNAVAILABLE)
            return
        }
        hall.select(path)
        raid.announce(
            player,
            "${player.displayName} has chosen to walk the Path of ${path.pathName}. " +
                "Join ${player.objectPronoun()}...",
        )
        travel(target, fromLobby = false)
    }

    private suspend fun ProtectedAccess.wardensDoor(quick: Boolean) {
        arriveDelay()
        val raid = player.currentRaid ?: return
        if (raid.encounterOf(player) !is MainHallEncounter) return

        val wardens = raid.current?.takeIf { it.room == ToaRoom.WARDENS_FIRST_ROOM && !it.destroyed }
        if (wardens != null) {
            travel(wardens, fromLobby = false)
            return
        }

        if (!raid.isLeader(player)) {
            mesbox("Your leader, ${raid.leaderName}, must enter first.")
            return
        }

        val question = "Do you wish to proceed to the lower level"
        if (raid.stragglers().isNotEmpty()) {
            if (!confirmAbandonStragglers(raid, question)) return
        } else if (!quick && !confirmYesNo("$question?")) {
            return
        }

        val target = resolveTarget(raid, ToaRoom.WARDENS_FIRST_ROOM, checkLeader = true) ?: return
        raid.announce(
            player,
            "${player.displayName} has proceeded to the lower level. Join ${player.objectPronoun()}...",
        )
        travel(target, fromLobby = false)
    }

    private suspend fun ProtectedAccess.barrier(barrier: CoordGrid, quick: Boolean) {
        arriveDelay()
        val raid = player.currentRaid ?: return
        if (blockedAsGhost(raid)) return
        val room = raid.encounterOf(player) ?: return
        val inside = room.inChallengeArea(player)

        when (room.stage) {
            ToaStage.COMPLETED -> walkThroughBarrier(barrier)
            ToaStage.STARTED ->
                if (inside) {
                    mesbox("You can't leave until the challenge is over.")
                } else {
                    walkThroughBarrier(barrier)
                }
            ToaStage.NOT_STARTED -> {
                if (inside) return
                if (!confirmBeginChallenge(raid, quick)) return
                if (room.destroyed) return
                room.start()
                walkThroughBarrier(barrier)
            }
        }
    }

    private suspend fun ProtectedAccess.walkThroughBarrier(barrier: CoordGrid) {
        val step = if (barrier.x < player.coords.x) -BARRIER_STEP else BARRIER_STEP
        faceSquare(barrier)
        forcedWalk(player.coords.translate(step, 0), crossTiles = BARRIER_STEP)
    }

    private suspend fun ProtectedAccess.teleportCrystal(quick: Boolean) {
        arriveDelay()
        val raid = player.currentRaid ?: return
        if (blockedAsGhost(raid)) return
        val room = raid.encounterOf(player) ?: return
        val challengeSpawn = room.room.challengeSpawn ?: return

        val startsRoom = room.room != ToaRoom.WARDENS_FIRST_ROOM
        if (startsRoom && room.stage == ToaStage.NOT_STARTED) {
            if (!confirmBeginChallenge(raid, quick)) return
            if (room.destroyed) return
            room.start()
        }
        val dest = room.coords(challengeSpawn)
        faceSquare(dest)
        telejump(dest, TeleportType.Exempt)
    }

    private suspend fun ProtectedAccess.puzzleExit(quick: Boolean) {
        arriveDelay()
        val raid = player.currentRaid ?: return
        val room = raid.encounterOf(player) ?: return
        if (room.room.kind != ToaRoom.Kind.PUZZLE) return
        val next = room.room.next ?: return

        if (room.stage != ToaStage.COMPLETED) {
            mes("You can't proceed until the challenge is complete.")
            return
        }
        if (!quick && !confirmYesNo("Are you ready to proceed?")) return
        proceed(raid, next, "has proceeded to the next challenge")
    }

    private suspend fun ProtectedAccess.osmumten() {
        val raid = player.currentRaid ?: return
        val room = raid.encounterOf(player) ?: return
        if (room.room.kind != ToaRoom.Kind.BOSS || room.stage != ToaStage.COMPLETED) return
        proceed(raid, ToaRoom.MAIN_HALL, "has returned to the Nexus")
    }

    private suspend fun ProtectedAccess.rewardCrystal(crystal: BoundLocInfo) {
        if (!isWithinApRange(crystal, CRYSTAL_AP_RANGE)) return
        val raid = player.currentRaid ?: return
        val room = raid.encounterOf(player) ?: return
        if (room.room != ToaRoom.WARDENS_SECOND_ROOM || room.stage != ToaStage.COMPLETED) return
        proceed(raid, ToaRoom.REWARD_ROOM, "has proceeded to Osmumten's Burial Chamber")
    }

    private suspend fun ProtectedAccess.abandonRaid() {
        arriveDelay()
        val raid = player.currentRaid
        if (raid == null) {
            telejump(TOA_OUTSIDE, TeleportType.Exempt)
            return
        }
        if (blockedAsGhost(raid)) return

        mesbox(
            "You are about to <col=ad2800>abandon the raid</col>. If you do this, you " +
                "<col=ad2800>will not</col> be able to return to your current run."
        )
        val abandon =
            choice2(
                "Yes, abandon the raid.",
                true,
                "No, I want to stay.",
                false,
                title = "Abandon the raid?",
            )
        if (!abandon) return

        mes("You abandon the raid and leave the Tombs of Amascut.")
        exitRaid()
    }

    private fun ProtectedAccess.leftRaidArea() {
        if (player.pendingLogout || player.loggingOut) return
        if (player.currentRaid == null) return
        ifCloseSub(TOA_HUD)
        ToaRaidManager.leave(player, logout = false)
    }

    private suspend fun ProtectedAccess.blockedAsGhost(raid: ToaRaid): Boolean {
        if (!raid.isGhost(player)) return false
        mesbox("A mysterious force prevents you from doing that.")
        return true
    }

    private companion object {
        const val RAID_AREA = "area.toa_raid"
        const val TOA_HUD = "interface.toa_hud"
        const val WARDENS_DOOR_OPEN = "loc.toa_nexus_wardens_door_open"
        const val BARRIER = "loc.toa_path_barrier"
        const val BARRIER_STEP = 2

        const val CRYSTAL_AP_RANGE = 3

        val TELEPORT_CRYSTALS =
            listOf(
                "loc.zebak_teleport",
                "loc.kephri_teleport",
                "loc.akkha_teleport",
                "loc.baba_teleport",
                "loc.wardens_teleport",
            )

        val PUZZLE_EXITS =
            listOf(
                "loc.toa_path_crondis_continue",
                "loc.toa_scabaras_continue",
                "loc.toa_door_continue",
                "loc.toa_path_apmeken_continue",
            )

        val EXIT_LOCS =
            listOf(
                "loc.toa_door_exit",
                "loc.toa_door_exit_wardens",
                "loc.toa_crondis_exit",
                "loc.toa_zebak_exit",
                "loc.toa_entrance_kephri_main",
                "loc.toa_entrance_akkha01",
                "loc.toa_entrance_baba02",
            )
    }
}
