package org.alter.interfaces.settings.tabs

import org.alter.api.ext.syncVarbit
import org.alter.api.ext.toggleVarbit
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.interfaces.ifMoveTop
import org.alter.interfaces.settings.configs.setting_components


class DisplaySettingsEvents : PluginEvent() {

    override fun init() {

        onButton(setting_components.brightness_bobble_container) {
            player.syncVarbit("varbits.option_brightness_remember")
        }

        onButton(setting_components.zoom_toggle) {
            player.toggleVarbit("varbits.camera_zoom_mouse_disabled")
        }

        onButton(setting_components.client_type_buttons) {
            player.toggleClientType(slot)
        }

    }

    private fun Player.toggleClientType(comsub: Int) {
        when (comsub) {
            1 -> ifMoveTop("interfaces.toplevel")
            2 -> ifMoveTop("interfaces.toplevel_osrs_stretch")
            3 -> ifMoveTop("interfaces.toplevel_pre_eoc")
            else -> error("Invalid comsub: $comsub")
        }
    }

}
