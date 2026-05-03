package org.rsmod.content.interfaces.settings.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifMoveTop
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.resyncVar
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DisplaySettingsScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    private var Player.zoomDisabled by boolVarBit("varbit.camera_zoom_mouse_disabled")

    override fun ScriptContext.startup() {
        onIfOverlayButton("component.settings_side:brightness_bobble_container") {
            player.resyncVar("varbit.option_brightness_remember")
        }
        onIfOverlayButton("component.settings_side:zoom_toggle") {
            player.zoomDisabled = !player.zoomDisabled
        }
        onIfOverlayButton("component.settings_side:display_dynamic_setting_1_buttons") {
            player.toggleClientType(it.comsub)
        }
    }

    private fun Player.toggleClientType(comsub: Int) {
        when (comsub) {
            1 -> ifMoveTop("interface.toplevel", eventBus)
            2 -> ifMoveTop("interface.toplevel_osrs_stretch", eventBus)
            3 -> ifMoveTop("interface.toplevel_pre_eoc", eventBus)
            else -> error("Invalid comsub: $comsub")
        }
    }
}
