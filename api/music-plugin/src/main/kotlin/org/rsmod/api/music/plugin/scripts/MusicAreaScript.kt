package org.rsmod.api.music.plugin.scripts

import dev.openrune.area
import dev.openrune.types.aconverted.AreaType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.music.MusicPlayMode
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.table.MusicClassicRow
import org.rsmod.api.table.MusicModernRow
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MusicAreaScript @Inject constructor(private val musicPlayer: MusicPlayer) :
    PluginScript() {
    private var Player.playMode by enumVarp<MusicPlayMode>(varps.musicplay)

    override fun ScriptContext.startup() {
        val scriptAreas = loadScriptAreas()
        for (area in scriptAreas) {
            onArea(area) { playAreaMusic(area) }
        }
        onPlayerLogin { player.setDefaultModes() }
    }

    private fun ProtectedAccess.playAreaMusic(area: AreaType) {
        musicPlayer.enterArea(player, area)
    }

    private fun Player.setDefaultModes() {
        playMode = MusicPlayMode.Area
    }

    private fun loadScriptAreas(): List<AreaType> {
        val areas = mutableListOf<AreaType>()

        MusicClassicRow.all().forEach {
            error("ADD CLASSIC MUSIC WE HAVE DATA NOW")
            //            val area = it.area
            //            val autoScript = it.auto_script
            //            if (autoScript) {
            //                areas += area
            //            }
        }

        MusicModernRow.all().forEach {
            val area = area(it.area)
            val autoScript = it.autoScript
            if (autoScript) {
                areas += area
            }
        }

        return areas
    }
}
