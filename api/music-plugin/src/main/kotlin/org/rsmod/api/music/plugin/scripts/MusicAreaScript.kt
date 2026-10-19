package org.rsmod.api.music.plugin.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.table.MusicModernRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MusicAreaScript @Inject constructor(private val musicPlayer: MusicPlayer) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val scriptAreas = loadScriptAreas()
        for (area in scriptAreas) {
            onArea(area) { playAreaMusic(area) }
            onAreaExit(area) { stopAreaMusic(area) }
        }
    }

    private fun ProtectedAccess.playAreaMusic(area: String) {
        musicPlayer.enterArea(player, area)
    }

    private fun ProtectedAccess.stopAreaMusic(area: String) {
        musicPlayer.exitArea(player, area)
    }

    private fun loadScriptAreas(): List<String> {
        val areas = mutableListOf<String>()

        MusicModernRow.all().forEach {
            if (it.autoScript) {
                areas += "area.${it.area}"
            }
        }

        return areas
    }
}
