package org.rsmod.api.music.plugin.scripts

import org.rsmod.api.player.music.MusicPlayMode
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class MusicTabScript : PluginScript() {
    private var Player.playMode by enumVarp<MusicPlayMode>("varp.musicplay")

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.music:area") { selectPlayMode(MusicPlayMode.Area, "area") }
        onIfOverlayButton("component.music:shuffle") {
            selectPlayMode(MusicPlayMode.Random, "shuffle")
        }
        onIfOverlayButton("component.music:single") {
            selectPlayMode(MusicPlayMode.Manual, "single")
        }
    }

    private fun ProtectedAccess.selectPlayMode(mode: MusicPlayMode, name: String) {
        player.playMode = mode
        mes("You are now using the $name music mode.")
    }
}
