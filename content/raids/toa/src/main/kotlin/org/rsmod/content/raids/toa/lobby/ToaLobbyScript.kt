package org.rsmod.content.raids.toa.lobby

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.raid.ToaRaidManager
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaLobbyScript : PluginScript() {

    private var Player.entranceOpen by intVarBit("varbit.toa_entrance_open")

    override fun ScriptContext.startup() {
        onArea(LOBBY_AREA) { enterLobby() }
        onAreaExit(LOBBY_AREA) { exitLobby() }

        onOpLoc1("loc.toa_entrance_open") { travel(INSIDE_DEST, Direction.South) }
        onOpLoc1("loc.toa_lobby_exit") { travel(OUTSIDE_DEST, Direction.NorthWest) }
        onOpLoc1("loc.toa_bank_camel") { tryOpenBank() }
        onOpLoc1("loc.toa_invocation_board") { ifOpenMainModal("interface.toa_invocations") }

        onPlayerLogin {
            player.entranceOpen = 1
        }
    }

    private suspend fun ProtectedAccess.travel(dest: CoordGrid, facing: Direction) {
        arriveDelay()
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

    private fun ProtectedAccess.enterLobby() {
        ifOpenOverlay(ToaPartyManager.LOBBY_HUD, HUD_TARGET)
        ToaPartyManager.sendLobbyHud(player)
    }

    private fun ProtectedAccess.exitLobby() {
        ifCloseSub(ToaPartyManager.LOBBY_HUD)

        if (ToaRaidManager.isInRaid(player)) return

        val removed = ToaPartyManager.onLeaveLobby(player)
        if (removed && !player.pendingLogout && !player.loggingOut) {
            player.mes("You have left the lobby, so you have been removed from your party.")
        }
    }

    private companion object {
        const val LOBBY_AREA = "area.toa_lobby"

        const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"

        val INSIDE_DEST = CoordGrid(3359, 9128, 0)
        val OUTSIDE_DEST = CoordGrid(3357, 2713, 0)

        const val FADE_CYCLES = 50
    }
}
