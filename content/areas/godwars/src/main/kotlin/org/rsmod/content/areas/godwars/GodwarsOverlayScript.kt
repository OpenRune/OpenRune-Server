package org.rsmod.content.areas.godwars

import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GodwarsOverlayScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onArea("area.godwars_dungeon") {
            player.ifOpenOverlay(
                "interface.godwars_overlay",
                "component.toplevel_osrs_stretch:overlay_hud",
                eventBus
            )
        }
        onAreaExit("area.godwars_dungeon") {
            player.ifCloseOverlay("interface.godwars_overlay", eventBus)
        }
    }

}
