package org.rsmod.content.raids.toa.raid

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartyListScript
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.party.ToaPartyManager.currentParty
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.content.raids.toa.raid.ToaRaidManager.toaController
import org.rsmod.game.entity.player.Appearance
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Entering and abandoning the raid. Port of Offline_Scape TOARaidEntryAction,
 * TOAManager.enterRaid/enter and TOAExitAction, with the timings, messages and
 * vars taken from Jesse's vanilla capture (enter, Scabaras path, abandon).
 *
 * Each room is its own small instance region (see [ToaRoom]). This slice only
 * builds the main hall; the path doors come next.
 */
class ToaRaidScript @Inject constructor(
    private val regionRepo: RegionRepository,
    private val partyList: ToaPartyListScript,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1("loc.toa_lobby_raid_entry") { raidEntrance() }
        for (exit in EXIT_LOCS) {
            onOpLoc1(exit) { abandonRaid() }
        }

        ToaRaidManager.releaseRooms = this@ToaRaidScript::endRaid

        onPlayerLogout {
            // Keep toa_mycontroller set (logout = true): the player is saved at
            // an instance coordinate, and PrepareLogin below needs to know.
            ToaRaidManager.leave(player, logout = true)
        }

        // PrepareLogin runs before the login rebuild. Moving the player here
        // (not in onPlayerLogin) is what MisthalinMystery does, for the same
        // reason: by Login the client has already been sent the old region.
        onEvent<SessionStateEvent.PrepareLogin> {
            if (player.toaController > 0) {
                player.coords = OUTSIDE
            }
        }

        onPlayerLogin {
            // All raid vars are saved like any other var; nobody is in a raid
            // at login, so clear them. (ToaPartyListScript also sets
            // toa_mycontroller to -1 here; both agree.)
            ToaRaidManager.resetClientVars(player)
        }
    }

    // ------------------------------------------------------------------
    // Entrance (loc 46089)
    // ------------------------------------------------------------------

    private suspend fun ProtectedAccess.raidEntrance() {
        arriveDelay()

        val party = player.currentParty
        if (party == null) {
            // Offline_Scape: OptionDialogue with these two options, the first
            // opening the grouping board.
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

        val raid = ToaRaidManager.raidFor(party) ?: startRaid(party) ?: return
        enterRaid(raid)
    }

    /**
     * Only the leader can start the raid (Offline_Scape enterRaid). Builds the
     * main hall first so that a full region pool fails before any state changes.
     */
    private suspend fun ProtectedAccess.startRaid(party: ToaLobbyParty): ToaRaid? {
        if (!party.isLeader(player)) {
            val leaderName = party.leader?.displayName ?: party.leaderName
            mesbox("Your leader, $leaderName, must enter first.")
            return null
        }

        val mainHall = buildRoom(ToaRoom.MAIN_HALL)
        if (mainHall == null) {
            mes("The Tombs of Amascut are unavailable at the moment. Please try again shortly.")
            return null
        }

        val raid = ToaRaidManager.start(party, mainHall)
        ToaPartyManager.onRaidStarted(party)

        // Offline_Scape: tell everyone else to follow.
        val pronoun = player.appearance.objectPronoun()
        for (member in party.members) {
            if (member != player) {
                member.mes(
                    "${player.displayName} has entered the Tombs of Amascut. " +
                        "Step inside to join $pronoun..."
                )
            }
        }
        return raid
    }

    /**
     * Capture timeline (ticks relative to the click being handled):
     * +0 message, close 773, fade out; +1 hide minimap; +3 rebuild + teleport
     * (+ toa_mycontroller); +4 fade in, minimap back; +6 HUD vars, close the
     * fade, open toa_hud (481) and send the names.
     */
    private suspend fun ProtectedAccess.enterRaid(raid: ToaRaid) {
        mes("You enter the Tombs of Amascut (${raid.settings.mode} Mode)...")
        ifCloseSub(ToaPartyManager.LOBBY_HUD)
        fadeOut()
        delay(1)
        minimapHideMap()
        delay(2)

        val room = ToaRoom.MAIN_HALL
        val region = raid.rooms.getValue(room)
        // Must happen before the teleport: the lobby-area exit it triggers
        // checks isInRaid() to keep the player in their party.
        ToaRaidManager.markEntered(player, raid, room)
        ToaPartyManager.setPartyStatus(player, ToaPartyManager.PARTY_STATUS_IN_PARTY)

        // Offline_Scape MAIN_HALL: spawn (3550, 5161), x randomised by 0..2
        // (capture landed on 3552). Faces south.
        val spawn = MAIN_HALL_SPAWN.translateX(random.of(0, MAIN_HALL_SPAWN_X_SPREAD))
        telejump(region.normal[spawn])
        faceDirection(Direction.South)

        delay(1)
        minimapReset()
        fadeIn()
        delay(2)

        closeFadeOverlayNow()
        ifOpenFullOverlay(TOA_HUD) // same slot as the fade, hence after closing it
        ToaRaidManager.refreshHud(raid) // this player + everyone already inside
    }

    // ------------------------------------------------------------------
    // Abandoning (the room exits, see EXIT_LOCS)
    // ------------------------------------------------------------------

    /**
     * Capture: mesbox, then "Abandon the raid?" choice. On yes: message and
     * fade (+0), hide minimap (+1), rebuild to the lobby + vars reset (+3),
     * fade in (+4), fade closed and 773 back (+6).
     */
    private suspend fun ProtectedAccess.abandonRaid() {
        arriveDelay()
        if (player.currentRaid == null) {
            // Offline_Scape TOAExitAction: not in a raid (shouldn't happen) -> just out.
            telejump(OUTSIDE)
            return
        }

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
        fadeOut() // replaces toa_hud in overlay_atmosphere
        delay(1)
        minimapHideMap()
        delay(2)

        // Leave before teleporting: once out of the raid, walking into the
        // lobby area reopens 773 with the (now empty) party list.
        ToaRaidManager.leave(player, logout = false)
        telejump(OUTSIDE)
        faceDirection(Direction.North)

        delay(1)
        minimapReset()
        fadeIn()
        delay(2)
        closeFadeOverlayNow()
    }

    // ------------------------------------------------------------------
    // Regions
    // ------------------------------------------------------------------

    /**
     * Builds a room instance and protects it, so the repository doesn't
     * reclaim it while it's momentarily empty (for example before anyone has
     * arrived, or between players moving rooms).
     */
    private fun buildRoom(room: ToaRoom): Region? {
        val region = regionRepo.add(room.template) ?: return null
        regionRepo.protect(region)
        return region
    }

    /**
     * Releases every room of an ended raid. Unprotecting is enough: the
     * repository reclaims empty, unprotected regions the next time one is
     * allocated (MisthalinBossFight does the same).
     */
    private fun endRaid(raid: ToaRaid) {
        for (region in raid.rooms.values) {
            regionRepo.unprotect(region)
        }
        raid.rooms.clear()
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun ProtectedAccess.fadeOut() =
        fadeOverlay(
            startColour = 0,
            startTransparency = 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = FADE_CYCLES,
        )

    private fun ProtectedAccess.fadeIn() =
        fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = FADE_CYCLES,
        )

    /** "him" / "her" / "them", from the player's pronoun setting. */
    private fun Appearance.objectPronoun(): String =
        when (subjectPronoun()) {
            "He" -> "him"
            "She" -> "her"
            else -> "them"
        }

    private companion object {
        const val TOA_HUD = "interface.toa_hud"

        /** Capture: fade_overlay script 948 with duration 50 both ways. */
        const val FADE_CYCLES = 50

        val MAIN_HALL_SPAWN = CoordGrid(3550, 5161, 0)
        const val MAIN_HALL_SPAWN_X_SPREAD = 2

        /**
         * Offline_Scape TOAExitAction's loc list (45128, 45129, 45453, 45543,
         * 45144, 46055, 45844). Every room's way out leads to the same
         * abandon dialogue. The capture used toa_door_exit in the Scabaras room.
         */
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

        /** Offline_Scape TOAManager.OUTSIDE_LOCATION; capture landed here too. */
        val OUTSIDE = CoordGrid(3358, 9113, 0)
    }
}
