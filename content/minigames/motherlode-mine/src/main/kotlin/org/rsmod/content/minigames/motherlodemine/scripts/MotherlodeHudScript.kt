package org.rsmod.content.minigames.motherlodemine.scripts

import jakarta.inject.Inject
import org.rsmod.api.delivery.DeliveryManager
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery.sackTransmit
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MotherlodeHudScript
@Inject
constructor(
    private val eventBus: EventBus,
    private val deliveries: DeliveryManager,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onArea(MotherlodeConstants.AREA_MOTHERLODE) {
            player.sackTransmit =
                deliveries
                    .storedCount(player, MotherlodeDelivery.PAY_DIRT_LINE)
                    .coerceIn(0, 255)
            player.ifOpenOverlay(MotherlodeConstants.HUD_MOTHERLODE, HUD_TARGET, eventBus)
        }
        onAreaExit(MotherlodeConstants.AREA_MOTHERLODE) {
            player.ifCloseOverlay(MotherlodeConstants.HUD_MOTHERLODE, eventBus)
        }
    }

    private companion object {
        const val HUD_TARGET = "component.toplevel_osrs_stretch:overlay_hud"
    }
}
