package org.rsmod.content.minigames.motherlodemine.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants.motherlodeInRestrictedArea
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// Crossing destinations must clear cave footprints and surrounding invisible walls.
class MotherlodeEntranceScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(DwarvenCave) { cross(it.loc, DwarvenCaveTile, InsideDwarvenSide) }
        onOpLoc1(DwarvenTunnel) { cross(it.loc, DwarvenTunnelTile, DwarvenMineLanding) }
        onOpLoc1(GuildCave) { cross(it.loc, GuildCaveTile, InsideGuildSide) }
        onOpLoc1(GuildTunnel) { cross(it.loc, GuildTunnelTile, MiningGuildLanding) }
        onOpLoc1("loc.motherlode_shortcut") { useShortcut(it.loc) }
    }

    private suspend fun ProtectedAccess.useShortcut(tunnel: BoundLocInfo) {
        val destination = ShortcutDestinations[tunnel.coords] ?: return
        if (player.agilityLvl < 54) {
            mes("You need an Agility level of 54 to use this shortcut.")
            return
        }
        // TODO: require the medium Falador Diary for the eastern pair when diaries are implemented.
        arriveDelay()
        faceSquare(tunnel.coords)
        anim("seq.human_pipesqueeze")
        soundSynth("synth.squeeze_in")
        delay(2)
        telejump(destination)
        faceSquare(destination.translate(destination.x - tunnel.coords.x, destination.z - tunnel.coords.z))
        anim("seq.human_pipeunsqueeze")
        soundSynth("synth.squeeze_out")
        delay(2)
        resetAnim()
    }

    private suspend fun ProtectedAccess.cross(
        loc: BoundLocInfo,
        placement: CoordGrid,
        destination: CoordGrid,
    ) {
        if (loc.coords != placement) {
            return
        }
        arriveDelay()
        telejump(destination)
        if (player.motherlodeInRestrictedArea) {
            player.motherlodeInRestrictedArea = false
        }
    }

    internal companion object {
        val ShortcutDestinations = mapOf(
            CoordGrid(3744, 5642, 0) to CoordGrid(3745, 5646, 0),
            CoordGrid(3745, 5645, 0) to CoordGrid(3744, 5641, 0),
            CoordGrid(3760, 5670, 0) to CoordGrid(3765, 5671, 0),
            CoordGrid(3764, 5671, 0) to CoordGrid(3759, 5670, 0),
        )
        private const val DwarvenCave = "loc.motherlode_entrance"

        private const val DwarvenTunnel = "loc.motherlode_exit"

        private const val GuildCave = "loc.motherlode_entrance_guild"

        private const val GuildTunnel = "loc.motherlode_exit_guild"

        private val DwarvenCaveTile = CoordGrid(3059, 9764, 0)

        private val DwarvenTunnelTile = CoordGrid(3728, 5693, 0)

        private val GuildCaveTile = CoordGrid(3055, 9743, 0)

        private val GuildTunnelTile = CoordGrid(3717, 5678, 0)

        private val InsideDwarvenSide = CoordGrid(3728, 5692, 0)

        private val DwarvenMineLanding = CoordGrid(3060, 9766, 0)

        private val InsideGuildSide = CoordGrid(3718, 5678, 0)

        private val MiningGuildLanding = CoordGrid(3054, 9744, 0)
    }
}
