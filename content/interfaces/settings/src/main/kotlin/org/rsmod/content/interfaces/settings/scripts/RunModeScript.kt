package org.rsmod.content.interfaces.settings.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RunModeScript
@Inject
constructor(private val eventBus: EventBus, private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton("component.orbs:runbutton") { player.selectRunToggle() }
        onIfOverlayButton("component.settings_side:runmode") { player.selectRunToggle() }
        onPlayerQueue("queue.runmode_toggle") { toggleRun() }
    }

    private fun Player.selectRunToggle() {
        if ("queue.runmode_toggle" in queueList) {
            return
        }
        ifClose(eventBus)
        val toggled = protectedAccess.launch(this) { toggleRun() }
        if (!toggled) {
            strongQueue("queue.runmode_toggle", 1)
        }
    }
}
