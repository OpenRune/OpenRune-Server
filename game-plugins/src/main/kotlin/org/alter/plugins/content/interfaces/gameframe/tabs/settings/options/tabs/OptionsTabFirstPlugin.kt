package org.alter.plugins.content.interfaces.gameframe.tabs.settings.options.tabs

import org.alter.api.ClientScript
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.attr.DISPLAY_MODE_CHANGE_ATTR
import org.alter.game.model.attr.INTERACTING_SLOT_ATTR
import org.alter.game.model.interf.DisplayMode
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.options.OptionsTab

class OptionsTabFirstPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {

        val SKULL_PROTECTION_BUTTON = 5
        val PLAYER_ATTACK_OPTION = 38
        val NPC_ATTACK_OPTION = 39
        val BRIGHTNES_BAR = 23
        val ZOOM_TOGGLE_BUTTON = 44
        val DISPLAY_MODE = 41
        val MUTE_MUSIC = 93
        val MUSIC_BAR = 104
        val MUTE_SOUND = 107
        val SOUND_BAR = 118
        val MUTE_AREA_SOUND = 122
        val AREA_SOUND_BAR = 133
        val MUSIC_UNLOCK_MESSAGE = 121
        val ACCEPT_AID_BUTTON = 29
        val RUN_MODE_BUTTON = 30
        val HOUSE_OPT_BUTTON = 31
        val BOND_BUTTON = 33
        val ALL_SETTINGS_BUTTON = 32

        val MUTE_MASTER_SOUND = 79
        val MUTE_MASTER_SOUND_BAR = 90

        val AUDIO_MUSIC_VOLUME = AttributeKey<Int>()
        val SOUND_EFFECT_VOLUME = AttributeKey<Int>()
        val AREA_SOUND_VOLUME = AttributeKey<Int>()
        val MASTER_SOUND_VOLUME = AttributeKey<Int>()

        onLogin {
            player.setInterfaceEvents(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = 55, 0..21, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = DISPLAY_MODE,
                0..21,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = BRIGHTNES_BAR,
                0..21,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = 84, 1..3, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = 82, 1..4, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = 81, 1..5, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = 69, 0..21, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = SOUND_BAR,
                0..21,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = PLAYER_ATTACK_OPTION,
                1..5,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = NPC_ATTACK_OPTION,
                1..4,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = MUSIC_BAR,
                0..21,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = DISPLAY_MODE,
                1..3,
                setting = InterfaceEvent.ClickOp1,
            )
            player.setInterfaceEvents(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = 90, 0..21, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(
                interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB,
                component = AREA_SOUND_BAR,
                0..21,
                setting = InterfaceEvent.ClickOp1,
            )
        }

        /**
         * Changing display modes (fixed, resizable).
         */
        setWindowStatusLogic {
            val change = player.attr[DISPLAY_MODE_CHANGE_ATTR]
            val mode =
                when (change) {
                    2 ->
                        if (player.getVarbit("varbits.resizable_stone_arrangement", ) == 1
                        ) {
                            DisplayMode.RESIZABLE_LIST
                        } else {
                            DisplayMode.RESIZABLE_NORMAL
                        }
                    else -> DisplayMode.FIXED
                }
            player.toggleDisplayInterface(mode)
        }

        bind_setting(child = DISPLAY_MODE) {
            val slot = player.attr[INTERACTING_SLOT_ATTR]!!
            val mode =
                when (slot) {
                    2 -> {
                        player.setVarbit("varbits.resizable_stone_arrangement", 0)
                        DisplayMode.RESIZABLE_NORMAL
                    }
                    3 -> {
                        player.setVarbit("varbits.resizable_stone_arrangement", 1)
                        DisplayMode.RESIZABLE_LIST
                    }
                    else -> DisplayMode.FIXED
                }
            if (!(mode.isResizable() && player.interfaces.displayMode.isResizable())) {
                player.runClientScript(ClientScript("settings_client_mode"), slot - 1)
            }
            player.toggleDisplayInterface(mode)
        }

        bind_setting(child = PLAYER_ATTACK_OPTION) {
            val slot = player.attr[INTERACTING_SLOT_ATTR]!!.toInt() - 1
            player.setVarp("varp.option_attackpriority", slot)
        }

        bind_setting(child = NPC_ATTACK_OPTION) {
            val slot = player.attr[INTERACTING_SLOT_ATTR]!!.toInt() - 1
            player.setVarp("varp.option_attackpriority_npc", slot)
        }

        bind_setting(child = ACCEPT_AID_BUTTON) {
            player.toggleVarp("varp.option_aid")
        }
        bind_setting(child = SKULL_PROTECTION_BUTTON) {
            player.toggleVarbit("varbits.skull_prevent_enabled")
        }

        bind_setting(63) {
            player.setVarbit("varbits.settings_side_panel_tab", 0)
        }
        bind_setting(68) {
            player.setVarbit("varbits.settings_side_panel_tab", 1)
        }
        bind_setting(69) {
            player.setVarbit("varbits.settings_side_panel_tab", 2)
        }
        bind_setting(MUSIC_BAR) {
            player.setVarp("varp.option_music", player.getInteractingSlot() * 5)
        }
        bind_setting(SOUND_BAR) {
            player.setVarp("varp.option_sounds", player.getInteractingSlot() * 5)
        }
        bind_setting(AREA_SOUND_BAR) {
            player.setVarp("varp.option_areasounds", player.getInteractingSlot() * 5)
        }

        bind_setting(MUTE_MASTER_SOUND_BAR) {
            player.setVarp("varp.option_master_volume", player.getInteractingSlot() * 5)
        }
        bind_setting(MUTE_MUSIC) {
            if (player.getVarp("varp.option_music") == 0) {
                player.setVarp("varp.option_music", player.attr[AUDIO_MUSIC_VOLUME] ?: 100)
            } else {
                player.attr[AUDIO_MUSIC_VOLUME] = player.getVarp("varp.option_music")
                player.setVarp("varp.option_music", 0)
            }
        }


        bind_setting(ZOOM_TOGGLE_BUTTON) {
            player.toggleVarbit("varbits.camera_zoom_mouse_disabled")
        }

        bind_setting(MUTE_MASTER_SOUND) {
            if (player.getVarp("varp.option_master_volume") == 0) {
                player.setVarp("varp.option_master_volume", player.attr[MASTER_SOUND_VOLUME] ?: 100)
            } else {
                player.attr[MASTER_SOUND_VOLUME] = player.getVarp("varp.option_master_volume")
                player.setVarp("varp.option_master_volume", 0)
            }
        }

        bind_setting(MUTE_SOUND) {
            if (player.getVarp("varp.option_sounds") == 0) {
                player.setVarp("varp.option_sounds", player.attr[SOUND_EFFECT_VOLUME] ?: 100)
            } else {
                player.attr[SOUND_EFFECT_VOLUME] = player.getVarp("varp.option_sounds")
                player.setVarp("varp.option_sounds", 0)
            }
        }

        bind_setting(MUTE_AREA_SOUND) {
            if (player.getVarp("varp.option_areasounds") == 0) {
                player.setVarp("varp.option_areasounds", player.attr[AREA_SOUND_VOLUME] ?: 100)
            } else {
                player.attr[AREA_SOUND_VOLUME] = player.getVarp("varp.option_areasounds")
                player.setVarp("varp.option_areasounds", 0)
            }
        }

        bind_setting(ALL_SETTINGS_BUTTON) {
            player.openInterface(parent = 161, child = 18, interfaceId = 134)
            player.setInterfaceEvents(interfaceId = 134, component = 23, range = 0..9, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(interfaceId = 134, component = 19, range = 0..449, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(interfaceId = 134, component = 28, range = 0..41, setting = InterfaceEvent.ClickOp1)
            player.setInterfaceEvents(interfaceId = 134, component = 21, range = 0..219, setting = InterfaceEvent.ClickOp1)
        }
    }

fun bind_setting(
    child: Int,
    plugin: Plugin.() -> Unit,
) {
    onButton(interfaceId = OptionsTab.SETTINGS_INTERFACE_TAB, component = child) {
        plugin(this)
    }
}
}
