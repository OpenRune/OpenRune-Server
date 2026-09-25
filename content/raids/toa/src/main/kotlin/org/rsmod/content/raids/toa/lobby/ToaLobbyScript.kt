package org.rsmod.content.raids.toa.lobby

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.raid.ToaRaidManager
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The TOA lobby: the pyramid entrance and lobby exit, plus the lobby HUD (773),
 * which opens when a player walks in and closes (removing them from their
 * party) when they leave.
 *
 * Port of Offline_Scape's TOALobbyArea and TOAEntranceAction. The area polygon
 * lives in .data/raw-cache/map/area/toa_lobby.toml, its id in gamevals.toml.
 *
 * The engine fires onAreaExit for walking out, teleporting AND logging out
 * (PlayerLogoutProcess forces area exits before onPlayerLogout runs).
 */
class ToaLobbyScript : PluginScript() {

    /**
     * Drives the pyramid entrance multiloc (loc.toa_entrance):
     * 0 = toa_entrance_blocked ("Inspect"), 1 = toa_entrance_open ("Enter").
     * Vanilla sets it by completing Beneath Cursed Sands.
     */
    private var Player.entranceOpen by intVarBit("varbit.toa_entrance_open")

    override fun ScriptContext.startup() {
        onArea(LOBBY_AREA) { enterLobby() }
        onAreaExit(LOBBY_AREA) { exitLobby() }

        // The client shows whichever multiloc variant the varbit selects, and
        // the engine dispatches the op to that variant, so we handle the
        // "open" variant rather than the base loc.
        onOpLoc1("loc.toa_entrance_open") { travel(INSIDE_DEST, Direction.South) }
        onOpLoc1("loc.toa_lobby_exit") { travel(OUTSIDE_DEST, Direction.NorthWest) }

        onPlayerLogin {
            // TEMPORARY: OpenRune has no Beneath Cursed Sands quest yet, so
            // nobody could ever see "Enter". Unlock it for everyone (as
            // Offline_Scape did). Remove once the quest sets this varbit.
            player.entranceOpen = 1
        }
    }

    // ------------------------------------------------------------------
    // Entrance / exit
    // ------------------------------------------------------------------

    /**
     * Fade to black, move, fade back in. Offline_Scape: fade, set location
     * and facing, unfade 2 ticks later.
     */
    private suspend fun ProtectedAccess.travel(dest: CoordGrid, facing: Direction) {
        arriveDelay() // finish walking up to the loc first
        fadeOverlay(
            startColour = 0,
            startTransparency = 255,
            endColour = 0,
            endTransparency = 0,
            clientDuration = FADE_CYCLES,
        )
        delay(2)
        telejump(dest)
        faceDirection(facing)
        delay(1)
        fadeOverlay(
            startColour = 0,
            startTransparency = 0,
            endColour = 0,
            endTransparency = 255,
            clientDuration = FADE_CYCLES,
        )
        closeFadeOverlay(cycles = 2)
    }

    // ------------------------------------------------------------------
    // Lobby area
    // ------------------------------------------------------------------

    private fun ProtectedAccess.enterLobby() {
        ifOpenOverlay(ToaPartyManager.LOBBY_HUD, HUD_TARGET)
        // The header text comes from varbit toa_client_partystatus (client-side).
        // The names list is ours: "-" lines, or the party if the player has one.
        ToaPartyManager.sendLobbyHud(player)
    }

    private fun ProtectedAccess.exitLobby() {
        // ProtectedAccess has no ifCloseOverlay wrapper; ifCloseSub closes
        // this interface whether it's a modal or an overlay.
        ifCloseSub(ToaPartyManager.LOBBY_HUD)

        // Entering the raid also fires this exit (the room instance maps back
        // to static coords outside this polygon). Vanilla keeps you in the
        // party for the whole raid, so stop here. Offline_Scape checked
        // `nextArea instanceof TOARaidArea` for the same reason.
        if (ToaRaidManager.isInRaid(player)) return

        val removed = ToaPartyManager.onLeaveLobby(player)
        if (removed && !player.pendingLogout && !player.loggingOut) {
            player.mes("You have left the lobby, so you have been removed from your party.")
        }
    }

    private companion object {
        const val LOBBY_AREA = "area.toa_lobby"

        // Same slot as the other area HUDs (godwars, motherlode). Unverified for
        // 773: confirm against a vanilla capture of walking into the lobby.
        const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"

        // From Offline_Scape TOAEntranceAction.
        val INSIDE_DEST = CoordGrid(3359, 9128, 0)
        val OUTSIDE_DEST = CoordGrid(3357, 2713, 0)

        // 50 client cycles = 1 second, the same fade length ShipTravel uses.
        const val FADE_CYCLES = 50
    }
}
