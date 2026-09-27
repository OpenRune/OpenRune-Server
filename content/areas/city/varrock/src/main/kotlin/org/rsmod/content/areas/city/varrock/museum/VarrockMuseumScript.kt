package org.rsmod.content.areas.city.varrock.museum

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VarrockMuseumScript
@Inject
constructor(private val eventBus: EventBus, private val areas: AreaChecker) : PluginScript() {
    override fun ScriptContext.startup() {
        onArea("area.varrock_museum") { player.openKudosOverlay(eventBus) }
        onAreaExit("area.varrock_museum") {
            player.ifCloseOverlay("interface.vm_kudos", eventBus)
        }

        onIfOpen("interface.vm_natural_history") {
            player.ifCloseOverlay("interface.vm_kudos", eventBus)
        }
        onIfClose("interface.vm_natural_history") {
            if (areas.inArea("area.varrock_museum", player.coords)) {
                player.openKudosOverlay(eventBus)
            }
        }
    }
}

internal fun Player.openKudosOverlay(eventBus: EventBus) {
    if (vmKudos == 0 || ui.containsOverlay("interface.vm_kudos")) {
        return
    }
    ifOpenOverlay("interface.vm_kudos", "component.toplevel_osrs_stretch:overlay_hud", eventBus)
}
