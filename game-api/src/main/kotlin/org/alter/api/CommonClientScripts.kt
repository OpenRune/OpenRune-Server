package org.alter.api

/**
 * Enum class for client-side scripts commonly used in different interfaces.
 * Each enum value corresponds to a specific functionality and provides an identifier
 * to help create [ClientScript] objects easily.
 *
 * @see ClientScript
 */
enum class CommonClientScripts(identifier : String = "", scriptID : Int = -1) {
    FOCUS_TAB("toplevel_sidebutton_switch"),
    WORLD_MAP_TILE("worldmap_transmitdata"),
    COMABT_LEVEL_SUMMARY("summary_sidepanel_combat_level_transmit"),
    SET_TEXT_ALIGN("if_settextalign"),
    CHATBOX_RESET_BACKGROUND("toplevel_chatbox_resetbackground"),
    SET_OPTIONS("objbox_setbuttons"),
    GE_SEARCH_ITEMS("meslayer_mode14"),
    INTERFACE_MENU("menu"),
    CHATBOX_MULTI("chatbox_multi_init"),
    MENU("csat_init"),
    SHOP_INIT("shop_main_init"),
    MAIN_MODAL_BACKGROUND("toplevel_mainmodal_background"),
    MAIN_MODAL_OPEN("toplevel_mainmodal_open"),
    INTERFACE_INV_INIT("interface_inv_init"),
    SKILL_MULTI_SETUP("skillmulti_setup"),
    INTRO_MUSIC_RESTORE("league_3_intro_music_restore"),
    GE_OFFER_SET_DESC("ge_offers_setdesc"),
    LOOTING_BAG_SETUP("wilderness_lootingbag_setup"),
    ACCOUNT_INFO_UPDATE("account_info_update"),
    TIME_PLAYED(scriptID = 3970),
    COMBAT_LEVEL_TRANSMIT("summary_sidepanel_combat_level_transmit"),
    ORBS_WORLDMAP_KEY("orbs_worldmap_key"),
    MEMBERS("playermember"),
    STAT_GROUP("stat_group_tooltip"),
    PLUGINS(scriptID = 876),
    QUEST_OVERVIEW("questjournal_overview_setup"),
    QUEST_JOURNAL_RESET("quest_journal_reset"),
    CLIENT_MODE("settings_client_mode"),
    CONFIRM_DESTROY("confirmdestroy_init"),
    WORLD_MAP_GOTO(scriptID = 3331),
    SCRIPT_1508(scriptID = 1508),
    CAMERA(scriptID = 626),
    CONFIRM(scriptID = 4212)
    ;

    val script : ClientScript = ClientScript(identifier, scriptID)
}