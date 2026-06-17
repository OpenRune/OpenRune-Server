package org.rsmod.content.interfaces.settings.scripts

import dev.openrune.definition.type.StructType
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.enum
import org.rsmod.api.table.SettingsConfigsRow

fun StructType.getParamAsInt(key: String, default: Int = -1): Int =
    params?.get(key.asRSCM(RSCMType.PARAM)) as? Int ?: default

fun StructType.getParamAsString(key: String, default: String = ""): String =
    params?.get(key.asRSCM(RSCMType.PARAM)) as? String ?: default

fun StructType.getParamAsBoolean(key: String, default: Boolean = false): Boolean =
    when (val value = params?.get(key.asRSCM(RSCMType.PARAM))) {
        is Boolean -> value
        is Int -> value == 1
        is String -> value.equals("true", true) || value == "1"
        else -> default
    }

enum class SettingType {
    CHECKBOX,
    SLIDER,
    DROPDOWN,
    KEYBIND,
    INPUT,
    DUNNO,
    BUTTON,
    NOT_USED,
    TEXT,
    COLOUR_PICKER,
    INPUT_LIST,
}

class Setting(struct: StructType) {

    val structId: Int = struct.id
    val id: Int = struct.getParamAsInt("param.settings_setting_id")
    val type: SettingType = SettingType.entries[struct.getParamAsInt("param.settings_setting_type")]
    val name: String = struct.getParamAsString("param.settings_setting_name")
    val searchKeywords: String = struct.getParamAsString("param.settings_setting_search_keywords")
    val sliderTransmitted: Boolean = struct.getParamAsBoolean("param.settings_setting_slider_transmitted")
    val sliderNotchCount: Int = struct.getParamAsInt("param.settings_setting_slider_notch_count")
    val desktop: Boolean = struct.getParamAsBoolean("param.settings_setting_desktop")
    val mobile: Boolean = struct.getParamAsBoolean("param.settings_setting_mobile")
    val nonIronman: Boolean = struct.getParamAsBoolean("param.settings_setting_non_ironman")
    val ironman: Boolean = struct.getParamAsBoolean("param.settings_setting_ironman")
    val hasCustomRequirements: Boolean = struct.getParamAsBoolean("param.settings_setting_customreq")
    val preRequirementsEnumId: Int = struct.getParamAsInt("param.settings_setting_prereqs")
    val preRequirementsValuesEnumId: Int = struct.getParamAsInt("param.settings_setting_prereqs_values")
    val inversedPreRequirementsEnumId: Int = struct.getParamAsInt("param.settings_setting_prereqs_inversed")
    val inversedPreRequirementsValuesEnumId: Int = struct.getParamAsInt("param.settings_setting_prereqs_inversed_values")
    val toggleInversed: Boolean = struct.getParamAsBoolean("param.settings_setting_toggle_inversed")
    val chooseTransmit: Boolean = struct.getParamAsBoolean("param.settings_setting_choose_transmit")
    val mobileName: String = struct.getParamAsString("param.settings_setting_mobile_name")
    val description: String = struct.getParamAsString("param.settings_setting_description")
    val keyBindSprite: Int = struct.getParamAsInt("param.settings_setting_keybind_sprite")
    val keyBindSpriteCoordGrid: Int = struct.getParamAsInt("param.settings_setting_keybind_sprite_size_coordgrid")
    val sliderSectors: Int = struct.getParamAsInt("param.settings_setting_slider_sectors")
    val sliderSectorsTextEnumId: Int = struct.getParamAsInt("param.settings_setting_slider_sector_text")
    val sliderCustomOnOpScript: Boolean = struct.getParamAsBoolean("param.settings_setting_slider_custom_onop")
    val sliderCustomSetPos: Boolean = struct.getParamAsBoolean("param.settings_setting_slider_custom_setpos")
    val sliderDraggable: Boolean = struct.getParamAsBoolean("param.settings_setting_slider_draggable")
    val sliderDeadZone: Int = struct.getParamAsInt("param.settings_setting_slider_deadzone")
    val sliderDeadTime: Int = struct.getParamAsInt("param.settings_setting_slider_deadtime")
    val inputSingular: String = struct.getParamAsString("param.settings_setting_input_singular")
    val inputPlural: String = struct.getParamAsString("param.settings_setting_input_plural")
    val inputZero: String = struct.getParamAsString("param.settings_setting_input_zero")
    val opCheckerMessage: String = struct.getParamAsString("param.settings_setting_op_checker_message")
    val mobileOpCheckerMessage: String = struct.getParamAsString("param.settings_setting_mobile_op_checker_message")
    val collapsibleInfobox: Boolean = struct.getParamAsBoolean("param.settings_setting_collapsible_infobox")
    val hideDescription: Boolean = struct.getParamAsBoolean("param.settings_setting_description_hide")
    val enhancedClientOnly: Boolean = struct.getParamAsBoolean("param.settings_setting_enhanced_client")
    val customNameExtraText: Boolean = struct.getParamAsBoolean("param.settings_setting_name_custom_extra")
    val mobileAlwaysEnabled: Boolean = struct.getParamAsBoolean("param.settings_setting_mobile_always_enabled")
    val hasCustomCheck: Boolean = struct.getParamAsBoolean("param.settings_setting_custom_check")
    val defaultColour: Int = struct.getParamAsInt("param.settings_setting_default_colour")
    val nonDesktopOnly: Boolean = struct.getParamAsBoolean("param.settings_setting_non_desktop_only")
    val leagueWorldOnly: Boolean = struct.getParamAsBoolean("param.settings_setting_league_world_only")
    val leagueWorldEnhancedClientOnly: Boolean = struct.getParamAsBoolean("param.settings_setting_league_world_enhanced_client_only")
    val dropdownEntriesEnumId: Int = struct.getParamAsInt("param.settings_setting_dropdown_entries")
    val mobileDropDownEntriesEnumId: Int = struct.getParamAsInt("param.settings_setting_dropdown_entries_mobile")

    val row by lazy {
        SettingsConfigsRow.all().find { it.settingId == id }
    }
    val dropdownEntries = if (dropdownEntriesEnumId == -1) null else enum<Int, String>(dropdownEntriesEnumId)

}
