package org.rsmod.content.interfaces.settings.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias setting_components = SettingComponents

object SettingComponents : ComponentReferences() {
    val runbutton_orb = component("orbs:runbutton")
    val runmode = component("settings_side:runmode")

    val settings_tab = component("settings_side:settings_tab")
    val audio_tab = component("settings_side:audio_tab")
    val display_tab = component("settings_side:display_tab")
    val settings_open = component("settings_side:settings_open")

    val skull_prevention = component("settings_side:skull_prevention")
    val attack_priority_player_buttons = component("settings_side:attack_priority_player_buttons")
    val attack_priority_npc_buttons = component("settings_side:attack_priority_npc_buttons")
    val acceptaid = component("settings_side:acceptaid")
    val houseoptions = component("settings_side:houseoptions")
    val bondoptions = component("settings_side:bondoptions")

    val master_icon = component("settings_side:master_icon")
    val master_bobble_container = component("settings_side:master_bobble_container")
    val music_icon = component("settings_side:music_icon")
    val music_bobble_container = component("settings_side:music_bobble_container")
    val sound_icon = component("settings_side:sound_icon")
    val sound_bobble_container = component("settings_side:sound_bobble_container")
    val areasound_icon = component("settings_side:areasound_icon")
    val areasounds_bobble_container = component("settings_side:areasounds_bobble_container")
    val music_toggle = component("settings_side:music_toggle")

    val brightness_bobble_container = component("settings_side:brightness_bobble_container")
    val zoom_toggle = component("settings_side:zoom_toggle")
    val client_type_buttons = component("settings_side:display_dynamic_setting_1_buttons")
}
