package org.rsmod.api.music.plugin.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.music.MusicPlayMode
import org.rsmod.api.player.music.MusicPlayer
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MusicTabScript @Inject constructor(private val musicPlayer: MusicPlayer) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.music:area") { selectPlayMode(MusicPlayMode.Area, "area") }
        onIfOverlayButton("component.music:shuffle") {
            selectPlayMode(MusicPlayMode.Random, "shuffle")
        }
        onIfOverlayButton("component.music:single") {
            selectPlayMode(MusicPlayMode.Manual, "single")
        }
        onIfOverlayButton("component.music:skip") { musicPlayer.skip(player) }
    }

    private fun ProtectedAccess.selectPlayMode(mode: MusicPlayMode, name: String) {
        musicPlayer.selectPlayMode(player, mode)
        mes("You are now using the $name music mode.")
        if (mode == MusicPlayMode.Area) {
            musicPlayer.playNextIfIdle(player)
        }
    }
}
