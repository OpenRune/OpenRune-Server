package dev.openrune.tables

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object SettingConfigs {

    const val SETTING_ID = 0
    const val VARP = 1
    const val VARBIT_ID = 2
    const val DEFAULT = 3
    const val DROPDOWN_ENUM = 4
    const val MIN = 5
    const val MAX = 6
    const val PROMPT = 7
    const val ENABLE_TOGGLE = 8

    fun settings() = dbTable("dbtable.settings_configs", serverOnly = true) {
        column("setting_id", SETTING_ID, VarType.INT)
        column("varp", VARP, VarType.VARP)
        column("varbit", VARBIT_ID, VarType.INT)
        column("default", DEFAULT, VarType.INT)
        column("dropdown_enum", DROPDOWN_ENUM, VarType.INT)
        column("min", MIN, VarType.INT)
        column("max", MAX, VarType.INT)
        column("prompt", PROMPT, VarType.STRING)
        column("enable_toggle", ENABLE_TOGGLE, VarType.DBROW)

        row("dbrow.setting_1") {
            column(SETTING_ID, 1)
            columnRSCM(VARBIT_ID, "varbit.chatbox_scrollbarside")
        }

        row("dbrow.setting_2") {
            column(SETTING_ID, 2)
            columnRSCM(VARBIT_ID, "varbit.side_transparency")
        }

        row("dbrow.setting_3") {
            column(SETTING_ID, 3)
            columnRSCM(VARBIT_ID, "varbit.option_hidexptolevel")
        }

        row("dbrow.setting_4") {
            column(SETTING_ID, 4)
            columnRSCM(VARBIT_ID, "varbit.option_prayertooltips")
        }

        row("dbrow.setting_5") {
            column(SETTING_ID, 5)
            columnRSCM(VARBIT_ID, "varbit.hitsplat_tint_disabled")
        }

        row("dbrow.setting_6") {
            column(SETTING_ID, 6)
            columnRSCM(VARBIT_ID, "varbit.option_satooltips")
        }

        row("dbrow.setting_7") {
            column(SETTING_ID, 7)
            columnRSCM(VARBIT_ID, "varbit.option_hide_rooftops")
        }

        row("dbrow.setting_8") {
            column(SETTING_ID, 8)
            columnRSCM(VARBIT_ID, "varbit.orbs_disabled")
        }

        row("dbrow.setting_9") {
            column(SETTING_ID, 9)
            columnRSCM(VARBIT_ID, "varbit.wiki_icon_disabled")
        }

        row("dbrow.setting_10") {
            column(SETTING_ID, 10)
            columnRSCM(VARBIT_ID, "varbit.hpbar_hud_boss_disabled")
        }

        row("dbrow.setting_11") {
            column(SETTING_ID, 11)
            columnRSCM(VARBIT_ID, "varbit.chatbox_transparency")
        }

        row("dbrow.setting_13") {
            column(SETTING_ID, 13)
            columnRSCM(VARBIT_ID, "varbit.camera_zoom_mouse_disabled")
        }

        row("dbrow.setting_16") {
            column(SETTING_ID, 16)
            columnRSCM(VARBIT_ID, "varbit.stone_combat_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_17") {
            column(SETTING_ID, 17)
            columnRSCM(VARBIT_ID, "varbit.stone_prayer_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_18") {
            column(SETTING_ID, 18)
            columnRSCM(VARBIT_ID, "varbit.stone_options1_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_19") {
            column(SETTING_ID, 19)
            columnRSCM(VARBIT_ID, "varbit.stone_stats_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_20") {
            column(SETTING_ID, 20)
            columnRSCM(VARBIT_ID, "varbit.stone_magic_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_21") {
            column(SETTING_ID, 21)
            columnRSCM(VARBIT_ID, "varbit.stone_options2_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_22") {
            column(SETTING_ID, 22)
            columnRSCM(VARBIT_ID, "varbit.stone_journal_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_23") {
            column(SETTING_ID, 23)
            columnRSCM(VARBIT_ID, "varbit.stone_friends_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_24") {
            column(SETTING_ID, 24)
            columnRSCM(VARBIT_ID, "varbit.stone_clanchat_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_25") {
            column(SETTING_ID, 25)
            columnRSCM(VARBIT_ID, "varbit.stone_inv_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_26") {
            column(SETTING_ID, 26)
            columnRSCM(VARBIT_ID, "varbit.stone_account_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_27") {
            column(SETTING_ID, 27)
            columnRSCM(VARBIT_ID, "varbit.stone_music_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_28") {
            column(SETTING_ID, 28)
            columnRSCM(VARBIT_ID, "varbit.stone_worn_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_29") {
            column(SETTING_ID, 29)
            columnRSCM(VARBIT_ID, "varbit.stone_logout_key")
            column(DROPDOWN_ENUM, 1161)
        }

        row("dbrow.setting_33") {
            column(SETTING_ID, 33)
            columnRSCM(VARBIT_ID, "varbit.music_unlock_text_toggle")
        }

        row("dbrow.setting_34") {
            column(SETTING_ID, 34)
            columnRSCM(VARP, "varp.option_chat")
        }

        row("dbrow.setting_35") {
            column(SETTING_ID, 35)
            columnRSCM(VARP, "varp.option_pm")
        }

        row("dbrow.setting_36") {
            column(SETTING_ID, 36)
            columnRSCM(VARBIT_ID, "varbit.hide_pm_alongside_chatbox")
        }

        row("dbrow.setting_37") {
            column(SETTING_ID, 37)
            columnRSCM(VARP, "varp.option_chatfilter_disabled")
        }

        row("dbrow.setting_38") {
            column(SETTING_ID, 38)
            columnRSCM(VARBIT_ID, "varbit.option_lootnotification_on")
            column(MIN, 0)
            column(MAX, 500000000)
            column(PROMPT, "Set threshold value:")
            columnRSCM(ENABLE_TOGGLE, "dbrow.setting_38")
        }

        row("dbrow.setting_39") {
            column(SETTING_ID, 39)
            columnRSCM(VARBIT_ID, "varbit.option_lootnotification_value")
        }

        row("dbrow.setting_40") {
            column(SETTING_ID, 40)
            columnRSCM(VARBIT_ID, "varbit.option_lootnotification_untradeables")
        }

        row("dbrow.setting_41") {
            column(SETTING_ID, 41)
            columnRSCM(VARBIT_ID, "varbit.boss_killcount_filtered")
        }

        row("dbrow.setting_42") {
            column(SETTING_ID, 42)
            columnRSCM(VARBIT_ID, "varbit.option_dropwarning_on")
        }

        row("dbrow.setting_43") {
            column(SETTING_ID, 43)
            columnRSCM(VARBIT_ID, "varbit.option_dropwarning_value")
            column(MIN, 0)
            column(MAX, 500000000)
            column(PROMPT, "Set threshold value:")
            columnRSCM(ENABLE_TOGGLE, "dbrow.setting_42")
        }

        row("dbrow.setting_46") {
            column(SETTING_ID, 46)
            columnRSCM(VARBIT_ID, "varbit.transparent_chatbox_blockclick")
        }

        row("dbrow.setting_47") {
            column(SETTING_ID, 47)
            columnRSCM(VARBIT_ID, "varbit.tli_storebutton_toggle_mobile")
        }

        row("dbrow.setting_48") {
            column(SETTING_ID, 48)
            columnRSCM(VARP, "varp.option_mouse")
        }

        row("dbrow.setting_49") {
            column(SETTING_ID, 49)
            columnRSCM(VARBIT_ID, "varbit.mousecam_disabled")
        }

        row("dbrow.setting_50") {
            column(SETTING_ID, 50)
            columnRSCM(VARBIT_ID, "varbit.followerops_deprioritised")
        }

        row("dbrow.setting_51") {
            column(SETTING_ID, 51)
            columnRSCM(VARBIT_ID, "varbit.desktop_shiftclickdrop_enabled")
        }

        row("dbrow.setting_54") {
            column(SETTING_ID, 54)
            columnRSCM(VARBIT_ID, "varbit.hotkey_cannot_close_sidepanel")
        }

        row("dbrow.setting_55") {
            column(SETTING_ID, 55)
            columnRSCM(VARP, "varp.option_attackpriority")
        }

        row("dbrow.setting_56") {
            column(SETTING_ID, 56)
            columnRSCM(VARP, "varp.option_attackpriority_npc")
        }

        row("dbrow.setting_57") {
            column(SETTING_ID, 57)
            columnRSCM(VARBIT_ID, "varbit.keybinding_esc_to_close")
        }

        row("dbrow.setting_59") {
            column(SETTING_ID, 59)
            columnRSCM(VARBIT_ID, "varbit.option_acceptaid")
        }

        row("dbrow.setting_60") {
            column(SETTING_ID, 60)
            columnRSCM(VARBIT_ID, "varbit.bounty_teleport_warning")
        }

        row("dbrow.setting_61") {
            column(SETTING_ID, 61)
            columnRSCM(VARBIT_ID, "varbit.dareeyak_teleport_warning")
        }

        row("dbrow.setting_62") {
            column(SETTING_ID, 62)
            columnRSCM(VARBIT_ID, "varbit.carrallagar_teleport_warning")
        }

        row("dbrow.setting_63") {
            column(SETTING_ID, 63)
            columnRSCM(VARBIT_ID, "varbit.annakarl_teleport_warning")
        }

        row("dbrow.setting_64") {
            column(SETTING_ID, 64)
            columnRSCM(VARBIT_ID, "varbit.ghorrock_teleport_warning")
        }

        row("dbrow.setting_65") {
            column(SETTING_ID, 65)
            columnRSCM(VARBIT_ID, "varbit.alchemy_warning_untradeables")
        }

        row("dbrow.setting_66") {
            column(SETTING_ID, 66)
            columnRSCM(VARBIT_ID, "varbit.alchemy_warning_valuethreshold")
            column(MIN, 0)
            column(MAX, 500000000)
            column(PROMPT, "Set value threshold for alchemy warnings:")
        }

        row("dbrow.setting_67") {
            column(SETTING_ID, 67)
            columnRSCM(VARBIT_ID, "varbit.teletab_ice_plateau_warning")
        }

        row("dbrow.setting_68") {
            column(SETTING_ID, 68)
            columnRSCM(VARBIT_ID, "varbit.teletab_cemetery_warning")
        }

        row("dbrow.setting_69") {
            column(SETTING_ID, 69)
            columnRSCM(VARBIT_ID, "varbit.teletab_wildycrabs_warning")
        }

        row("dbrow.setting_70") {
            column(SETTING_ID, 70)
            columnRSCM(VARBIT_ID, "varbit.teletab_dareeyak_warning")
        }

        row("dbrow.setting_71") {
            column(SETTING_ID, 71)
            columnRSCM(VARBIT_ID, "varbit.teletab_carrallangar_warning")
        }

        row("dbrow.setting_72") {
            column(SETTING_ID, 72)
            columnRSCM(VARBIT_ID, "varbit.teletab_annakarl_warning")
        }

        row("dbrow.setting_73") {
            column(SETTING_ID, 73)
            columnRSCM(VARBIT_ID, "varbit.teletab_ghorrock_warning")
        }

        row("dbrow.setting_81") {
            column(SETTING_ID, 81)
            columnRSCM(VARBIT_ID, "varbit.br_custom_fog_colour")
        }

        row("dbrow.setting_82") {
            column(SETTING_ID, 82)
            columnRSCM(VARBIT_ID, "varbit.option_precise_timing")
        }

        row("dbrow.setting_83") {
            column(SETTING_ID, 83)
            columnRSCM(VARBIT_ID, "varbit.option_separate_hours")
        }

        row("dbrow.setting_84") {
            column(SETTING_ID, 84)
            columnRSCM(VARBIT_ID, "varbit.gravestone_supplypiles_disabled")
        }

        row("dbrow.setting_85") {
            column(SETTING_ID, 85)
            columnRSCM(VARBIT_ID, "varbit.option_collection_new_item")
        }

        row("dbrow.setting_86") {
            column(SETTING_ID, 86)
            columnRSCM(VARBIT_ID, "varbit.side_container_steel")
        }

        row("dbrow.setting_87") {
            column(SETTING_ID, 87)
            columnRSCM(VARP, "varp.option_chat_colour_public_opaque")
        }

        row("dbrow.setting_88") {
            column(SETTING_ID, 88)
            columnRSCM(VARP, "varp.option_chat_colour_public_transparent")
        }

        row("dbrow.setting_89") {
            column(SETTING_ID, 89)
            columnRSCM(VARP, "varp.option_chat_colour_private_opaque")
        }

        row("dbrow.setting_90") {
            column(SETTING_ID, 90)
            columnRSCM(VARP, "varp.option_chat_colour_private_transparent")
        }

        row("dbrow.setting_91") {
            column(SETTING_ID, 91)
            columnRSCM(VARP, "varp.option_chat_colour_private_split")
        }

        row("dbrow.setting_92") {
            column(SETTING_ID, 92)
            columnRSCM(VARP, "varp.option_chat_colour_autochat_opaque")
        }

        row("dbrow.setting_93") {
            column(SETTING_ID, 93)
            columnRSCM(VARP, "varp.option_chat_colour_autochat_transparent")
        }

        row("dbrow.setting_94") {
            column(SETTING_ID, 94)
            columnRSCM(VARP, "varp.option_chat_colour_broadcast_opaque")
        }

        row("dbrow.setting_95") {
            column(SETTING_ID, 95)
            columnRSCM(VARP, "varp.option_chat_colour_broadcast_transparent")
        }

        row("dbrow.setting_96") {
            column(SETTING_ID, 96)
            columnRSCM(VARP, "varp.option_chat_colour_broadcast_split")
        }

        row("dbrow.setting_97") {
            column(SETTING_ID, 97)
            columnRSCM(VARP, "varp.option_chat_colour_friendschat_opaque")
        }

        row("dbrow.setting_98") {
            column(SETTING_ID, 98)
            columnRSCM(VARP, "varp.option_chat_colour_friendschat_transparent")
        }

        row("dbrow.setting_99") {
            column(SETTING_ID, 99)
            columnRSCM(VARP, "varp.option_chat_colour_clanchat_opaque")
        }

        row("dbrow.setting_100") {
            column(SETTING_ID, 100)
            columnRSCM(VARP, "varp.option_chat_colour_clanchat_transparent")
        }

        row("dbrow.setting_101") {
            column(SETTING_ID, 101)
            columnRSCM(VARP, "varp.option_chat_colour_tradereq_opaque")
        }

        row("dbrow.setting_102") {
            column(SETTING_ID, 102)
            columnRSCM(VARP, "varp.option_chat_colour_tradereq_transparent")
        }

        row("dbrow.setting_103") {
            column(SETTING_ID, 103)
            columnRSCM(VARP, "varp.option_chat_colour_challengereq_opaque")
        }

        row("dbrow.setting_104") {
            column(SETTING_ID, 104)
            columnRSCM(VARP, "varp.option_chat_colour_challengereq_transparent")
        }

        row("dbrow.setting_105") {
            column(SETTING_ID, 105)
            columnRSCM(VARP, "varp.option_chat_colour_guestclan_opaque")
        }

        row("dbrow.setting_106") {
            column(SETTING_ID, 106)
            columnRSCM(VARP, "varp.option_chat_colour_guestclan_transparent")
        }

        row("dbrow.setting_110") {
            column(SETTING_ID, 110)
            columnRSCM(VARBIT_ID, "varbit.music_area_mode")
        }

        row("dbrow.setting_111") {
            column(SETTING_ID, 111)
            columnRSCM(VARBIT_ID, "varbit.hpbar_hud_standard_disabled")
        }

        row("dbrow.setting_112") {
            column(SETTING_ID, 112)
            columnRSCM(VARBIT_ID, "varbit.tile_highlighting_disabled")
        }

        row("dbrow.setting_113") {
            column(SETTING_ID, 113)
            columnRSCM(VARP, "varp.option_highlighting_player_tile_colour")
        }

        row("dbrow.setting_114") {
            column(SETTING_ID, 114)
            columnRSCM(VARBIT_ID, "varbit.mouseover_tooltips_disabled")
        }

        row("dbrow.setting_115") {
            column(SETTING_ID, 115)
            columnRSCM(VARBIT_ID, "varbit.antidrag_enabled")
        }

        row("dbrow.setting_116") {
            column(SETTING_ID, 116)
            columnRSCM(VARBIT_ID, "varbit.regen_indicator_disabled")
        }

        row("dbrow.setting_118") {
            column(SETTING_ID, 118)
            columnRSCM(VARBIT_ID, "varbit.cox_helper_disabled")
        }

        row("dbrow.setting_119") {
            column(SETTING_ID, 119)
            columnRSCM(VARBIT_ID, "varbit.settings_chat_timestamps")
        }

        row("dbrow.setting_120") {
            column(SETTING_ID, 120)
            columnRSCM(VARBIT_ID, "varbit.fishing_spot_indicator_disabled")
        }

        row("dbrow.setting_121") {
            column(SETTING_ID, 121)
            columnRSCM(VARBIT_ID, "varbit.fishing_spot_indicator_all_fish_disabled")
        }

        row("dbrow.setting_122") {
            column(SETTING_ID, 122)
            columnRSCM(VARBIT_ID, "varbit.fishing_spot_indicator_more_info_disabled")
        }

        row("dbrow.setting_123") {
            column(SETTING_ID, 123)
            columnRSCM(VARBIT_ID, "varbit.attack_style_enabled")
        }

        row("dbrow.setting_124") {
            column(SETTING_ID, 124)
            columnRSCM(VARBIT_ID, "varbit.buff_bar_hidden")
        }

        row("dbrow.setting_125") {
            column(SETTING_ID, 125)
            columnRSCM(VARBIT_ID, "varbit.buff_home_teleport_disabled")
        }

        row("dbrow.setting_126") {
            column(SETTING_ID, 126)
            columnRSCM(VARBIT_ID, "varbit.buff_minigame_teleport_disabled")
        }

        row("dbrow.setting_127") {
            column(SETTING_ID, 127)
            columnRSCM(VARBIT_ID, "varbit.buff_league_relics_hidden")
        }

        row("dbrow.setting_128") {
            column(SETTING_ID, 128)
            columnRSCM(VARBIT_ID, "varbit.buff_bar_tooltips_hidden")
        }

        row("dbrow.setting_129") {
            column(SETTING_ID, 129)
            columnRSCM(VARBIT_ID, "varbit.buff_teleblock_disabled")
        }

        row("dbrow.setting_130") {
            column(SETTING_ID, 130)
            columnRSCM(VARBIT_ID, "varbit.buff_charge_spell_disabled")
        }

        row("dbrow.setting_131") {
            column(SETTING_ID, 131)
            columnRSCM(VARBIT_ID, "varbit.buff_godwars_altar_disabled")
        }

        row("dbrow.setting_132") {
            column(SETTING_ID, 132)
            columnRSCM(VARBIT_ID, "varbit.buff_dragonfire_shield_cooldown_disabled")
        }

        row("dbrow.setting_133") {
            column(SETTING_ID, 133)
            columnRSCM(VARBIT_ID, "varbit.buff_imbued_heart_cooldown_disabled")
        }

        row("dbrow.setting_134") {
            column(SETTING_ID, 134)
            columnRSCM(VARBIT_ID, "varbit.buff_vengeance_cooldown_disabled")
        }

        row("dbrow.setting_135") {
            column(SETTING_ID, 135)
            columnRSCM(VARBIT_ID, "varbit.buff_vengeance_active_disabled")
        }

        row("dbrow.setting_136") {
            column(SETTING_ID, 136)
            columnRSCM(VARBIT_ID, "varbit.buff_stamina_duration_disabled")
        }

        row("dbrow.setting_137") {
            column(SETTING_ID, 137)
            columnRSCM(VARBIT_ID, "varbit.buff_prayer_enhance_duration_disabled")
        }

        row("dbrow.setting_138") {
            column(SETTING_ID, 138)
            columnRSCM(VARBIT_ID, "varbit.buff_overload_duration_disabled")
        }

        row("dbrow.setting_139") {
            column(SETTING_ID, 139)
            columnRSCM(VARBIT_ID, "varbit.buff_magic_imbue_duration_disabled")
        }

        row("dbrow.setting_140") {
            column(SETTING_ID, 140)
            columnRSCM(VARBIT_ID, "varbit.buff_sire_stun_duration_disabled")
        }

        row("dbrow.setting_141") {
            column(SETTING_ID, 141)
            columnRSCM(VARBIT_ID, "varbit.buff_freeze_duration_disabled")
        }

        row("dbrow.setting_142") {
            column(SETTING_ID, 142)
            columnRSCM(VARBIT_ID, "varbit.buff_sotd_duration_disabled")
        }

        row("dbrow.setting_143") {
            column(SETTING_ID, 143)
            columnRSCM(VARBIT_ID, "varbit.buff_divine_potion_duration_disabled")
        }

        row("dbrow.setting_144") {
            column(SETTING_ID, 144)
            columnRSCM(VARBIT_ID, "varbit.buff_antifire_duration_disabled")
        }

        row("dbrow.setting_145") {
            column(SETTING_ID, 145)
            columnRSCM(VARBIT_ID, "varbit.buff_antipoison_duration_disabled")
        }

        row("dbrow.setting_146") {
            column(SETTING_ID, 146)
            columnRSCM(VARBIT_ID, "varbit.always_on_top")
        }

        row("dbrow.setting_147") {
            column(SETTING_ID, 147)
            columnRSCM(VARBIT_ID, "varbit.mouseover_text_enabled")
        }

        row("dbrow.setting_149") {
            column(SETTING_ID, 149)
            columnRSCM(VARBIT_ID, "varbit.stat_boosts_hud_tooltips_hidden")
        }

        row("dbrow.setting_150") {
            column(SETTING_ID, 150)
            columnRSCM(VARBIT_ID, "varbit.stat_boosts_hud_num_displays")
        }

        row("dbrow.setting_151") {
            column(SETTING_ID, 151)
            columnRSCM(VARBIT_ID, "varbit.stat_boosts_hud_display_relative")
        }

        row("dbrow.setting_152") {
            column(SETTING_ID, 152)
            columnRSCM(VARBIT_ID, "varbit.buff_corruption_disabled")
        }

        row("dbrow.setting_153") {
            column(SETTING_ID, 153)
            columnRSCM(VARBIT_ID, "varbit.buff_mark_of_darkness_disabled")
        }

        row("dbrow.setting_154") {
            column(SETTING_ID, 154)
            columnRSCM(VARBIT_ID, "varbit.buff_shadow_veil_disabled")
        }

        row("dbrow.setting_155") {
            column(SETTING_ID, 155)
            columnRSCM(VARBIT_ID, "varbit.buff_death_charge_disabled")
        }

        row("dbrow.setting_156") {
            column(SETTING_ID, 156)
            columnRSCM(VARBIT_ID, "varbit.buff_ward_of_arceuus_disabled")
        }

        row("dbrow.setting_157") {
            column(SETTING_ID, 157)
            columnRSCM(VARBIT_ID, "varbit.buff_resurrection_disabled")
        }

        row("dbrow.setting_159") {
            column(SETTING_ID, 159)
            columnRSCM(VARBIT_ID, "varbit.ca_task_popup")
        }

        row("dbrow.setting_160") {
            column(SETTING_ID, 160)
            columnRSCM(VARBIT_ID, "varbit.ca_task_recompletion_notifications")
        }

        row("dbrow.setting_161") {
            column(SETTING_ID, 161)
            columnRSCM(VARBIT_ID, "varbit.ca_failure_notifications_enabled")
        }

        row("dbrow.setting_162") {
            column(SETTING_ID, 162)
            columnRSCM(VARBIT_ID, "varbit.ca_failure_notifications_enabled")
        }

        row("dbrow.setting_163") {
            column(SETTING_ID, 163)
            columnRSCM(VARBIT_ID, "varbit.agility_helper_disabled")
        }

        row("dbrow.setting_164") {
            column(SETTING_ID, 164)
            columnRSCM(VARBIT_ID, "varbit.agility_helper_highlight_obstacles_enabled")
        }

        row("dbrow.setting_165") {
            column(SETTING_ID, 165)
            columnRSCM(VARBIT_ID, "varbit.agility_helper_highlight_shortcuts")
        }

        row("dbrow.setting_166") {
            column(SETTING_ID, 166)
            columnRSCM(VARBIT_ID, "varbit.minimap_zoom_lock_disabled")
        }

        row("dbrow.setting_170") {
            column(SETTING_ID, 170)
            columnRSCM(VARBIT_ID, "varbit.buff_ammo_disabled")
        }

        row("dbrow.setting_171") {
            column(SETTING_ID, 171)
            columnRSCM(VARBIT_ID, "varbit.option_collection_new_item")
        }

        row("dbrow.setting_172") {
            column(SETTING_ID, 172)
            columnRSCM(VARBIT_ID, "varbit.highlighting_tile_mouseover_enabled")
        }

        row("dbrow.setting_173") {
            column(SETTING_ID, 173)
            columnRSCM(VARBIT_ID, "varbit.highlighting_tile_mouseover_alwaysontop")
        }

        row("dbrow.setting_174") {
            column(SETTING_ID, 174)
            columnRSCM(VARP, "varp.option_highlighting_tile_mouseover_colour")
        }

        row("dbrow.setting_175") {
            column(SETTING_ID, 175)
            columnRSCM(VARBIT_ID, "varbit.highlighting_tile_current_enabled")
        }

        row("dbrow.setting_176") {
            column(SETTING_ID, 176)
            columnRSCM(VARBIT_ID, "varbit.highlighting_tile_current_alwaysontop")
        }

        row("dbrow.setting_177") {
            column(SETTING_ID, 177)
            columnRSCM(VARP, "varp.option_highlighting_tile_current_colour")
        }

        row("dbrow.setting_178") {
            column(SETTING_ID, 178)
            columnRSCM(VARBIT_ID, "varbit.highlighting_tile_destination_enabled")
        }

        row("dbrow.setting_179") {
            column(SETTING_ID, 179)
            columnRSCM(VARBIT_ID, "varbit.highlighting_tile_destination_alwaysontop")
        }

        row("dbrow.setting_180") {
            column(SETTING_ID, 180)
            columnRSCM(VARP, "varp.option_highlighting_tile_destination_colour")
        }

        row("dbrow.setting_181") {
            column(SETTING_ID, 181)
            columnRSCM(VARBIT_ID, "varbit.tli_storebutton_toggle_desktop")
        }

        row("dbrow.setting_182") {
            column(SETTING_ID, 182)
            columnRSCM(VARBIT_ID, "varbit.iron_noloot_icon_disabled")
        }

        row("dbrow.setting_183") {
            column(SETTING_ID, 183)
            columnRSCM(VARBIT_ID, "varbit.iron_noloot_message_disabled")
        }

        row("dbrow.setting_184") {
            column(SETTING_ID, 184)
            columnRSCM(VARBIT_ID, "varbit.slayer_helper_disabled")
        }

        row("dbrow.setting_185") {
            column(SETTING_ID, 185)
            columnRSCM(VARBIT_ID, "varbit.logout_notification_disabled")
        }

        row("dbrow.setting_186") {
            column(SETTING_ID, 186)
            columnRSCM(VARBIT_ID, "varbit.buff_poison_damage_disabled")
        }

        row("dbrow.setting_187") {
            column(SETTING_ID, 187)
            columnRSCM(VARBIT_ID, "varbit.ore_respawn_timer")
        }

        row("dbrow.setting_188") {
            column(SETTING_ID, 188)
            columnRSCM(VARBIT_ID, "varbit.woodcutting_respawn_timer")
        }

        row("dbrow.setting_189") {
            column(SETTING_ID, 189)
            columnRSCM(VARBIT_ID, "varbit.birdnest_notification_disabled")
        }

        row("dbrow.setting_190") {
            column(SETTING_ID, 190)
            columnRSCM(VARBIT_ID, "varbit.mouseover_entity_highlights_enabled")
        }

        row("dbrow.setting_191") {
            column(SETTING_ID, 191)
            columnRSCM(VARBIT_ID, "varbit.popout_xptracker_disabled")
        }

        row("dbrow.setting_192") {
            column(SETTING_ID, 192)
            columnRSCM(VARBIT_ID, "varbit.gravestone_disable_warning")
        }

        row("dbrow.setting_193") {
            column(SETTING_ID, 193)
            columnRSCM(VARP, "varp.option_chat_colour_gimchat_opaque")
        }

        row("dbrow.setting_194") {
            column(SETTING_ID, 194)
            columnRSCM(VARP, "varp.option_chat_colour_gimchat_transparent")
        }

        row("dbrow.setting_195") {
            column(SETTING_ID, 195)
            columnRSCM(VARBIT_ID, "varbit.option_chatbox_mode_autoset")
        }

        row("dbrow.setting_196") {
            column(SETTING_ID, 196)
            columnRSCM(VARP, "varp.option_chat_colour_clanbroadcast_opaque")
        }

        row("dbrow.setting_197") {
            column(SETTING_ID, 197)
            columnRSCM(VARP, "varp.option_chat_colour_clanbroadcast_transparent")
        }

        row("dbrow.setting_198") {
            column(SETTING_ID, 198)
            columnRSCM(VARP, "varp.option_chat_colour_gimbroadcast_opaque")
        }

        row("dbrow.setting_199") {
            column(SETTING_ID, 199)
            columnRSCM(VARP, "varp.option_chat_colour_gimbroadcast_transparent")
        }

        row("dbrow.setting_200") {
            column(SETTING_ID, 200)
            columnRSCM(VARBIT_ID, "varbit.option_gim_loot_broadcast_threshold")
        }

        row("dbrow.setting_201") {
            column(SETTING_ID, 201)
            columnRSCM(VARBIT_ID, "varbit.option_gim_loot_broadcasts_disabled")
        }

        row("dbrow.setting_202") {
            column(SETTING_ID, 202)
            columnRSCM(VARBIT_ID, "varbit.option_gim_levelup_broadcasts_disabled")
        }

        row("dbrow.setting_203") {
            column(SETTING_ID, 203)
            columnRSCM(VARBIT_ID, "varbit.option_gim_quest_broadcasts_disabled")
        }

        row("dbrow.setting_204") {
            column(SETTING_ID, 204)
            columnRSCM(VARBIT_ID, "varbit.option_gim_ca_broadcasts_disabled")
        }

        row("dbrow.setting_205") {
            column(SETTING_ID, 205)
            columnRSCM(VARBIT_ID, "varbit.trade_delay_disabled")
        }

        row("dbrow.setting_206") {
            column(SETTING_ID, 206)
            columnRSCM(VARBIT_ID, "varbit.skull_prevent_enabled")
        }

        row("dbrow.setting_207") {
            column(SETTING_ID, 207)
            columnRSCM(VARBIT_ID, "varbit.runinvert_mode")
        }

        row("dbrow.setting_208") {
            column(SETTING_ID, 208)
            columnRSCM(VARBIT_ID, "varbit.buff_desert_damage_disabled")
        }

        row("dbrow.setting_209") {
            column(SETTING_ID, 209)
            columnRSCM(VARBIT_ID, "varbit.mouseover_tooltips_hide_opcount")
        }

        row("dbrow.setting_210") {
            column(SETTING_ID, 210)
            columnRSCM(VARBIT_ID, "varbit.agility_helper_highlight_shortcuts_overlay_disabled")
        }

        row("dbrow.setting_212") {
            column(SETTING_ID, 212)
            columnRSCM(VARBIT_ID, "varbit.interact_entity_highlights_disabled")
        }

        row("dbrow.setting_213") {
            column(SETTING_ID, 213)
            columnRSCM(VARBIT_ID, "varbit.buff_cannon_ammo_disabled")
        }

        row("dbrow.setting_214") {
            column(SETTING_ID, 214)
            columnRSCM(VARBIT_ID, "varbit.questlist_sort_type")
        }

        row("dbrow.setting_215") {
            column(SETTING_ID, 215)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_lackreqs")
        }

        row("dbrow.setting_216") {
            column(SETTING_ID, 216)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_lackrecs")
        }

        row("dbrow.setting_217") {
            column(SETTING_ID, 217)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_not_started")
        }

        row("dbrow.setting_218") {
            column(SETTING_ID, 218)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_in_progress")
        }

        row("dbrow.setting_219") {
            column(SETTING_ID, 219)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_completed")
        }

        row("dbrow.setting_220") {
            column(SETTING_ID, 220)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_miniquests")
        }

        row("dbrow.setting_221") {
            column(SETTING_ID, 221)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_quests")
        }

        row("dbrow.setting_222") {
            column(SETTING_ID, 222)
            columnRSCM(VARBIT_ID, "varbit.questlist_larger_text")
        }

        row("dbrow.setting_223") {
            column(SETTING_ID, 223)
            columnRSCM(VARBIT_ID, "varbit.questlist_disable_text_shadow")
        }

        row("dbrow.setting_224") {
            column(SETTING_ID, 224)
            columnRSCM(VARP, "varp.questlist_colour_not_started")
        }

        row("dbrow.setting_225") {
            column(SETTING_ID, 225)
            columnRSCM(VARP, "varp.questlist_colour_in_progress")
        }

        row("dbrow.setting_226") {
            column(SETTING_ID, 226)
            columnRSCM(VARP, "varp.questlist_colour_completed")
        }

        row("dbrow.setting_227") {
            column(SETTING_ID, 227)
            columnRSCM(VARP, "varp.questlist_colour_unavailable")
        }

        row("dbrow.setting_229") {
            column(SETTING_ID, 229)
            columnRSCM(VARBIT_ID, "varbit.option_haptic_on_op_disabled")
        }

        row("dbrow.setting_230") {
            column(SETTING_ID, 230)
            columnRSCM(VARBIT_ID, "varbit.option_haptic_on_drag_disabled")
        }

        row("dbrow.setting_231") {
            column(SETTING_ID, 231)
            columnRSCM(VARBIT_ID, "varbit.option_haptic_on_minimenu_open_disabled")
        }

        row("dbrow.setting_232") {
            column(SETTING_ID, 232)
            columnRSCM(VARBIT_ID, "varbit.option_haptic_on_minimenu_entry_hover_enabled")
        }

        row("dbrow.setting_233") {
            column(SETTING_ID, 233)
            columnRSCM(VARBIT_ID, "varbit.option_minimenu_long_press_time")
        }

        row("dbrow.setting_234") {
            column(SETTING_ID, 234)
            columnRSCM(VARBIT_ID, "varbit.examine_price_ge_enabled")
        }

        row("dbrow.setting_235") {
            column(SETTING_ID, 235)
            columnRSCM(VARBIT_ID, "varbit.examine_price_alch_enabled")
        }

        row("dbrow.setting_236") {
            column(SETTING_ID, 236)
            columnRSCM(VARBIT_ID, "varbit.antidrag_delay")
        }

        row("dbrow.setting_237") {
            column(SETTING_ID, 237)
            columnRSCM(VARBIT_ID, "varbit.antidrag_key")
            column(DROPDOWN_ENUM, 4302)
        }

        row("dbrow.setting_238") {
            column(SETTING_ID, 238)
            columnRSCM(VARBIT_ID, "varbit.antidrag_disablekey")
            column(DROPDOWN_ENUM, 4302)
        }

        row("dbrow.setting_239") {
            column(SETTING_ID, 239)
            columnRSCM(VARBIT_ID, "varbit.questlist_disable_headers")
        }

        row("dbrow.setting_240") {
            column(SETTING_ID, 240)
            columnRSCM(VARBIT_ID, "varbit.questlist_hide_unavailable")
        }

        row("dbrow.setting_241") {
            column(SETTING_ID, 241)
            columnRSCM(VARBIT_ID, "varbit.stat_boosts_hud_hidden")
        }

        row("dbrow.setting_242") {
            column(SETTING_ID, 242)
            columnRSCM(VARBIT_ID, "varbit.tog_helper_disabled")
        }

        row("dbrow.setting_243") {
            column(SETTING_ID, 243)
            columnRSCM(VARBIT_ID, "varbit.hunter_trap_timers_disabled")
        }

        row("dbrow.setting_244") {
            column(SETTING_ID, 244)
            columnRSCM(VARBIT_ID, "varbit.buff_water_charges_disabled")
        }

        row("dbrow.setting_245") {
            column(SETTING_ID, 245)
            columnRSCM(VARBIT_ID, "varbit.herbiboar_helper_disabled")
        }

        row("dbrow.setting_246") {
            column(SETTING_ID, 246)
            columnRSCM(VARBIT_ID, "varbit.buff_cannon_decay_disabled")
        }

        row("dbrow.setting_247") {
            column(SETTING_ID, 247)
            columnRSCM(VARBIT_ID, "varbit.cannon_hud_disabled")
        }

        row("dbrow.setting_248") {
            column(SETTING_ID, 248)
            columnRSCM(VARBIT_ID, "varbit.cannon_low_notification_enabled")
        }

        row("dbrow.setting_249") {
            column(SETTING_ID, 249)
            columnRSCM(VARBIT_ID, "varbit.cannon_low_amount")
        }

        row("dbrow.setting_250") {
            column(SETTING_ID, 250)
            columnRSCM(VARBIT_ID, "varbit.cannon_no_ammo_notification_enabled")
        }

        row("dbrow.setting_251") {
            column(SETTING_ID, 251)
            columnRSCM(VARBIT_ID, "varbit.popout_panel_desktop_disabled")
        }

        row("dbrow.setting_252") {
            column(SETTING_ID, 252)
            columnRSCM(VARBIT_ID, "varbit.popout_panel_mobile_enabled")
        }

        row("dbrow.setting_253") {
            column(SETTING_ID, 253)
            columnRSCM(VARBIT_ID, "varbit.wilderness_lever_blockwarning_standard")
        }

        row("dbrow.setting_254") {
            column(SETTING_ID, 254)
            columnRSCM(VARBIT_ID, "varbit.wilderness_lever_blockwarning_highrisk")
        }

        row("dbrow.setting_255") {
            column(SETTING_ID, 255)
            columnRSCM(VARBIT_ID, "varbit.wildy_hub_warning")
        }

        row("dbrow.setting_256") {
            column(SETTING_ID, 256)
            columnRSCM(VARBIT_ID, "varbit.wildy_canoe_warning")
        }

        row("dbrow.setting_257") {
            column(SETTING_ID, 257)
            columnRSCM(VARBIT_ID, "varbit.option_content_recommender_hide")
        }

        row("dbrow.setting_258") {
            column(SETTING_ID, 258)
            columnRSCM(VARBIT_ID, "varbit.option_npc_indicators_name")
        }

        row("dbrow.setting_259") {
            column(SETTING_ID, 259)
            columnRSCM(VARBIT_ID, "varbit.option_npc_indicators_tile")
        }

        row("dbrow.setting_260") {
            column(SETTING_ID, 260)
            columnRSCM(VARBIT_ID, "varbit.option_npc_indicators_outline_enabled")
        }

        row("dbrow.setting_261") {
            column(SETTING_ID, 261)
            columnRSCM(VARBIT_ID, "varbit.option_npc_indicators_enabled")
        }

        row("dbrow.setting_262") {
            column(SETTING_ID, 262)
            columnRSCM(VARP, "varp.option_npc_indicators_colour")
        }

        row("dbrow.setting_263") {
            column(SETTING_ID, 263)
            columnRSCM(VARP, "varp.option_npc_indicators_text_colour")
        }

        row("dbrow.setting_264") {
            column(SETTING_ID, 264)
            columnRSCM(VARBIT_ID, "varbit.option_npc_name_all")
        }

        row("dbrow.setting_265") {
            column(SETTING_ID, 265)
            columnRSCM(VARBIT_ID, "varbit.option_minimap_icons_max_zoom_disabled")
        }

        row("dbrow.setting_266") {
            column(SETTING_ID, 266)
            columnRSCM(VARP, "varp.option_npc_text_all_colour")
        }

        row("dbrow.setting_268") {
            column(SETTING_ID, 268)
            columnRSCM(VARBIT_ID, "varbit.blast_furnace_helper_disabled")
        }

        row("dbrow.setting_269") {
            column(SETTING_ID, 269)
            columnRSCM(VARBIT_ID, "varbit.blast_furnace_highlights_disabled")
        }

        row("dbrow.setting_270") {
            column(SETTING_ID, 270)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_overlay_enabled")
        }

        row("dbrow.setting_271") {
            column(SETTING_ID, 271)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_enabled")
        }

        row("dbrow.setting_272") {
            column(SETTING_ID, 272)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_worldmap_marker_enabled")
        }

        row("dbrow.setting_273") {
            column(SETTING_ID, 273)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_target_world_arrow_enabled")
        }

        row("dbrow.setting_274") {
            column(SETTING_ID, 274)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_menu_highlight_enabled")
        }

        row("dbrow.setting_275") {
            column(SETTING_ID, 275)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_infobox_enabled")
        }

        row("dbrow.setting_276") {
            column(SETTING_ID, 276)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_infobox_show_clue_text")
        }

        row("dbrow.setting_277") {
            column(SETTING_ID, 277)
            columnRSCM(VARBIT_ID, "varbit.option_cluehelper_target_highlight_enabled")
        }

        row("dbrow.setting_278") {
            column(SETTING_ID, 278)
            columnRSCM(VARBIT_ID, "varbit.option_item_retrieval_warning_disabled")
        }

        row("dbrow.setting_279") {
            column(SETTING_ID, 279)
            columnRSCM(VARBIT_ID, "varbit.hitsplat_maxhit_disabled")
        }

        row("dbrow.setting_280") {
            column(SETTING_ID, 280)
            columnRSCM(VARBIT_ID, "varbit.settings_hitsplat_threshold")
            column(MIN, 2)
            column(MAX, 500)
            column(PROMPT, "Set value threshold for max hits (2-500):")
        }

        row("dbrow.setting_281") {
            column(SETTING_ID, 281)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_baba_yaga_disabled")
        }

        row("dbrow.setting_282") {
            column(SETTING_ID, 282)
            columnRSCM(VARBIT_ID, "varbit.trawler_camera_static")
        }

        row("dbrow.setting_283") {
            column(SETTING_ID, 283)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_barrows_disabled")
        }

        row("dbrow.setting_284") {
            column(SETTING_ID, 284)
            columnRSCM(VARBIT_ID, "varbit.buff_statrenewal_duration_disabled")
        }

        row("dbrow.setting_285") {
            column(SETTING_ID, 285)
            columnRSCM(VARBIT_ID, "varbit.buff_apmekens_sight_disabled")
        }

        row("dbrow.setting_286") {
            column(SETTING_ID, 286)
            columnRSCM(VARBIT_ID, "varbit.buff_akkha_burn_duration_disabled")
        }

        row("dbrow.setting_287") {
            column(SETTING_ID, 287)
            columnRSCM(VARBIT_ID, "varbit.buff_akkha_darkness_duration_disabled")
        }

        row("dbrow.setting_288") {
            column(SETTING_ID, 288)
            columnRSCM(VARBIT_ID, "varbit.buff_akkha_freeze_duration_disabled")
        }

        row("dbrow.setting_289") {
            column(SETTING_ID, 289)
            columnRSCM(VARBIT_ID, "varbit.buff_zebak_bleed_duration_disabled")
        }

        row("dbrow.setting_290") {
            column(SETTING_ID, 290)
            columnRSCM(VARBIT_ID, "varbit.buff_toa_midraidloot_healing_duration_disabled")
        }

        row("dbrow.setting_291") {
            column(SETTING_ID, 291)
            columnRSCM(VARBIT_ID, "varbit.buff_toa_midraidloot_prayer_duration_disabled")
        }

        row("dbrow.setting_292") {
            column(SETTING_ID, 292)
            columnRSCM(VARBIT_ID, "varbit.buff_toa_midraidloot_energy_duration_disabled")
        }

        row("dbrow.setting_293") {
            column(SETTING_ID, 293)
            columnRSCM(VARBIT_ID, "varbit.buff_toa_midraidloot_stats_duration_disabled")
        }

        row("dbrow.setting_294") {
            column(SETTING_ID, 294)
            columnRSCM(VARBIT_ID, "varbit.settings_warning_confirmation_charter_ships")
        }

        row("dbrow.setting_295") {
            column(SETTING_ID, 295)
            columnRSCM(VARBIT_ID, "varbit.settings_warning_confirmation_farming_protection")
        }

        row("dbrow.setting_296") {
            column(SETTING_ID, 296)
            columnRSCM(VARBIT_ID, "varbit.wilderness_lever_first_op")
        }

        row("dbrow.setting_297") {
            column(SETTING_ID, 297)
            columnRSCM(VARBIT_ID, "varbit.option_ge_price_buy_warning_disabled")
        }

        row("dbrow.setting_298") {
            column(SETTING_ID, 298)
            columnRSCM(VARBIT_ID, "varbit.option_ge_price_sell_warning_disabled")
        }

        row("dbrow.setting_299") {
            column(SETTING_ID, 299)
            columnRSCM(VARBIT_ID, "varbit.hpbar_hud_boss_name_disabled")
        }

        row("dbrow.setting_300") {
            column(SETTING_ID, 300)
            columnRSCM(VARBIT_ID, "varbit.hpbar_hud_boss_compact_enabled")
        }

        row("dbrow.setting_301") {
            column(SETTING_ID, 301)
            columnRSCM(VARBIT_ID, "varbit.hpbar_hud_boss_percentage_enabled")
        }

        row("dbrow.setting_302") {
            column(SETTING_ID, 302)
            columnRSCM(VARBIT_ID, "varbit.settings_muspah_messages")
        }

        row("dbrow.setting_303") {
            column(SETTING_ID, 303)
            columnRSCM(VARBIT_ID, "varbit.show_dialogue_in_chatbox")
        }

        row("dbrow.setting_304") {
            column(SETTING_ID, 304)
            columnRSCM(VARBIT_ID, "varbit.show_mesbox_in_chatbox")
        }

        row("dbrow.setting_305") {
            column(SETTING_ID, 305)
            columnRSCM(VARBIT_ID, "varbit.take_ammo_toggle")
        }

        row("dbrow.setting_306") {
            column(SETTING_ID, 306)
            columnRSCM(VARBIT_ID, "varbit.take_runes_toggle")
        }

        row("dbrow.setting_307") {
            column(SETTING_ID, 307)
            columnRSCM(VARBIT_ID, "varbit.buff_wbr_exit_cave")
        }

        row("dbrow.setting_308") {
            column(SETTING_ID, 308)
            columnRSCM(VARBIT_ID, "varbit.wildy_hub_warning_highrisk")
        }

        row("dbrow.setting_309") {
            column(SETTING_ID, 309)
            columnRSCM(VARBIT_ID, "varbit.wildy_canoe_warning_highrisk")
        }

        row("dbrow.setting_311") {
            column(SETTING_ID, 311)
            columnRSCM(VARBIT_ID, "varbit.combat_level_decimal_enabled")
        }

        row("dbrow.setting_312") {
            column(SETTING_ID, 312)
            columnRSCM(VARBIT_ID, "varbit.tradeoption_disabled")
        }

        row("dbrow.setting_313") {
            column(SETTING_ID, 313)
            columnRSCM(VARBIT_ID, "varbit.purgeignored_permbanned_disabled")
        }

        row("dbrow.setting_314") {
            column(SETTING_ID, 314)
            columnRSCM(VARBIT_ID, "varbit.purgeignored_permmuted_enabled")
        }

        row("dbrow.setting_316") {
            column(SETTING_ID, 316)
            columnRSCM(VARBIT_ID, "varbit.option_fletch_dart_or_bolt_makex")
        }

        row("dbrow.setting_317") {
            column(SETTING_ID, 317)
            columnRSCM(VARBIT_ID, "varbit.osm_minimap_toggle")
        }

        row("dbrow.setting_320") {
            column(SETTING_ID, 320)
            columnRSCM(VARBIT_ID, "varbit.popout_loottools_disabled")
        }

        row("dbrow.setting_321") {
            column(SETTING_ID, 321)
            columnRSCM(VARBIT_ID, "varbit.ground_items_enabled")
        }

        row("dbrow.setting_322") {
            column(SETTING_ID, 322)
            columnRSCM(VARP, "varp.ground_items_t5_colour")
        }

        row("dbrow.setting_323") {
            column(SETTING_ID, 323)
            columnRSCM(VARP, "varp.ground_items_t4_colour")
        }

        row("dbrow.setting_324") {
            column(SETTING_ID, 324)
            columnRSCM(VARP, "varp.ground_items_t3_colour")
        }

        row("dbrow.setting_325") {
            column(SETTING_ID, 325)
            columnRSCM(VARP, "varp.ground_items_t2_colour")
        }

        row("dbrow.setting_326") {
            column(SETTING_ID, 326)
            columnRSCM(VARP, "varp.ground_items_t1_colour")
        }

        row("dbrow.setting_327") {
            column(SETTING_ID, 327)
            columnRSCM(VARP, "varp.ground_items_custom_colour")
        }

        row("dbrow.setting_328") {
            column(SETTING_ID, 328)
            columnRSCM(VARP, "varp.ground_items_t5_threshold")
        }

        row("dbrow.setting_329") {
            column(SETTING_ID, 329)
            columnRSCM(VARP, "varp.ground_items_t4_threshold")
        }

        row("dbrow.setting_330") {
            column(SETTING_ID, 330)
            columnRSCM(VARP, "varp.ground_items_t3_threshold")
        }

        row("dbrow.setting_331") {
            column(SETTING_ID, 331)
            columnRSCM(VARP, "varp.ground_items_t2_threshold")
        }

        row("dbrow.setting_332") {
            column(SETTING_ID, 332)
            columnRSCM(VARP, "varp.ground_items_t1_threshold")
        }

        row("dbrow.setting_333") {
            column(SETTING_ID, 333)
            columnRSCM(VARBIT_ID, "varbit.ground_items_price_type")
        }

        row("dbrow.setting_334") {
            column(SETTING_ID, 334)
            columnRSCM(VARBIT_ID, "varbit.ground_items_despawn_time_enabled_desktop")
        }

        row("dbrow.setting_335") {
            column(SETTING_ID, 335)
            columnRSCM(VARBIT_ID, "varbit.ground_items_despawn_time_enabled_mobile")
        }

        row("dbrow.setting_336") {
            column(SETTING_ID, 336)
            columnRSCM(VARBIT_ID, "varbit.ground_items_visibility_time_enabled_desktop")
        }

        row("dbrow.setting_337") {
            column(SETTING_ID, 337)
            columnRSCM(VARBIT_ID, "varbit.ground_items_visibility_time_enabled_mobile")
        }

        row("dbrow.setting_338") {
            column(SETTING_ID, 338)
            columnRSCM(VARBIT_ID, "varbit.ground_items_edit_mode_enabled_desktop")
        }

        row("dbrow.setting_339") {
            column(SETTING_ID, 339)
            columnRSCM(VARBIT_ID, "varbit.ground_items_edit_mode_enabled_mobile")
        }

        row("dbrow.setting_340") {
            column(SETTING_ID, 340)
            columnRSCM(VARBIT_ID, "varbit.ground_items_modifier_key")
            column(DROPDOWN_ENUM, 4975)
        }

        row("dbrow.setting_343") {
            column(SETTING_ID, 343)
            columnRSCM(VARBIT_ID, "varbit.option_loottracker_disabled")
        }

        row("dbrow.setting_344") {
            column(SETTING_ID, 344)
            columnRSCM(VARBIT_ID, "varbit.option_loottracker_ignore_consumed_drops")
        }

        row("dbrow.setting_346") {
            column(SETTING_ID, 346)
            columnRSCM(VARBIT_ID, "varbit.ground_items_display_price_disabled")
        }

        row("dbrow.setting_347") {
            column(SETTING_ID, 347)
            columnRSCM(VARBIT_ID, "varbit.worldswitcher_disable_confirmation")
        }

        row("dbrow.setting_348") {
            column(SETTING_ID, 348)
            columnRSCM(VARBIT_ID, "varbit.music_enableloop")
        }

        row("dbrow.setting_350") {
            column(SETTING_ID, 350)
            columnRSCM(VARBIT_ID, "varbit.ground_items_max_lines")
        }

        row("dbrow.setting_352") {
            column(SETTING_ID, 352)
            columnRSCM(VARBIT_ID, "varbit.lootingbag_useallitems")
        }

        row("dbrow.setting_353") {
            column(SETTING_ID, 353)
            columnRSCM(VARBIT_ID, "varbit.lootingbag_ignore_food")
        }

        row("dbrow.setting_354") {
            column(SETTING_ID, 354)
            columnRSCM(VARBIT_ID, "varbit.emptyondeath_runepouch")
        }

        row("dbrow.setting_355") {
            column(SETTING_ID, 355)
            columnRSCM(VARBIT_ID, "varbit.emptyondeath_tacklebox")
        }

        row("dbrow.setting_356") {
            column(SETTING_ID, 356)
            columnRSCM(VARBIT_ID, "varbit.emptyondeath_boltpouch")
        }

        row("dbrow.setting_357") {
            column(SETTING_ID, 357)
            columnRSCM(VARBIT_ID, "varbit.emptyondeath_seedbox")
        }

        row("dbrow.setting_358") {
            column(SETTING_ID, 358)
            columnRSCM(VARBIT_ID, "varbit.emptyondeath_essencepouch")
        }

        row("dbrow.setting_359") {
            column(SETTING_ID, 359)
            columnRSCM(VARBIT_ID, "varbit.emptyondeath_herbsack")
        }

        row("dbrow.setting_360") {
            column(SETTING_ID, 360)
            columnRSCM(VARBIT_ID, "varbit.option_skill_guide")
        }

        row("dbrow.setting_362") {
            column(SETTING_ID, 362)
            columnRSCM(VARBIT_ID, "varbit.compass_reorientation_setheight")
        }

        row("dbrow.setting_363") {
            column(SETTING_ID, 363)
            columnRSCM(VARBIT_ID, "varbit.buff_group_gathering_woodcutting_disabled")
        }

        row("dbrow.setting_364") {
            column(SETTING_ID, 364)
            columnRSCM(VARBIT_ID, "varbit.buff_woodcutting_leprechaun_rainbows_setting")
        }

        row("dbrow.setting_365") {
            column(SETTING_ID, 365)
            columnRSCM(VARBIT_ID, "varbit.crm_surprisepopup_blocked")
        }

        row("dbrow.setting_366") {
            column(SETTING_ID, 366)
            columnRSCM(VARBIT_ID, "varbit.ground_items_untradeable_items_disabled")
        }

        row("dbrow.setting_367") {
            column(SETTING_ID, 367)
            columnRSCM(VARBIT_ID, "varbit.quest_start_highlight")
        }

        row("dbrow.setting_368") {
            column(SETTING_ID, 368)
            columnRSCM(VARBIT_ID, "varbit.quest_start_highlight_filter_requirements")
        }

        row("dbrow.setting_369") {
            column(SETTING_ID, 369)
            columnRSCM(VARBIT_ID, "varbit.buff_scurrius_food_pile")
        }

        row("dbrow.setting_370") {
            column(SETTING_ID, 370)
            columnRSCM(VARBIT_ID, "varbit.poison_karambwan_warning_disabled")
        }

        row("dbrow.setting_371") {
            column(SETTING_ID, 371)
            columnRSCM(VARBIT_ID, "varbit.settings_gravestone_autoequip")
        }

        row("dbrow.setting_372") {
            column(SETTING_ID, 372)
            columnRSCM(VARBIT_ID, "varbit.rightclick_report_abuse_disabled")
        }

        row("dbrow.setting_373") {
            column(SETTING_ID, 373)
            columnRSCM(VARBIT_ID, "varbit.settings_barbarian_potion_makex")
        }

        row("dbrow.setting_374") {
            column(SETTING_ID, 374)
            columnRSCM(VARBIT_ID, "varbit.option_trail_reminder_beginner")
        }

        row("dbrow.setting_375") {
            column(SETTING_ID, 375)
            columnRSCM(VARBIT_ID, "varbit.option_trail_reminder_easy")
        }

        row("dbrow.setting_376") {
            column(SETTING_ID, 376)
            columnRSCM(VARBIT_ID, "varbit.option_trail_reminder_medium")
        }

        row("dbrow.setting_377") {
            column(SETTING_ID, 377)
            columnRSCM(VARBIT_ID, "varbit.option_trail_reminder_hard")
        }

        row("dbrow.setting_378") {
            column(SETTING_ID, 378)
            columnRSCM(VARBIT_ID, "varbit.option_trail_reminder_elite")
        }

        row("dbrow.setting_379") {
            column(SETTING_ID, 379)
            columnRSCM(VARBIT_ID, "varbit.option_trail_reminder_master")
        }

        row("dbrow.setting_380") {
            column(SETTING_ID, 380)
            columnRSCM(VARBIT_ID, "varbit.auto_smash_vials")
        }

        row("dbrow.setting_381") {
            column(SETTING_ID, 381)
            columnRSCM(VARBIT_ID, "varbit.brut_smash_pots_automatically")
        }

        row("dbrow.setting_382") {
            column(SETTING_ID, 382)
            columnRSCM(VARBIT_ID, "varbit.settings_hit_sounds")
        }

        row("dbrow.setting_383") {
            column(SETTING_ID, 383)
            columnRSCM(VARBIT_ID, "varbit.settings_hd_beta_enabled")
        }

        row("dbrow.setting_384") {
            column(SETTING_ID, 384)
            columnRSCM(VARBIT_ID, "varbit.settings_hd_beta_full_hd_or_fake_sd")
        }

        row("dbrow.setting_385") {
            column(SETTING_ID, 385)
            columnRSCM(VARBIT_ID, "varbit.buff_deadman_overload_potion")
        }

        row("dbrow.setting_386") {
            column(SETTING_ID, 386)
            columnRSCM(VARBIT_ID, "varbit.death_waivecosmeticsprotection")
        }

        row("dbrow.setting_387") {
            column(SETTING_ID, 387)
            columnRSCM(VARBIT_ID, "varbit.settings_transmit_pronouns")
        }

        row("dbrow.setting_388") {
            column(SETTING_ID, 388)
            columnRSCM(VARBIT_ID, "varbit.buff_prayer_regeneration_potion_duration_disabled")
        }

        row("dbrow.setting_389") {
            column(SETTING_ID, 389)
            columnRSCM(VARBIT_ID, "varbit.runenergy_autoenable")
            column(MIN, 0)
            column(MAX, 100)
            column(PROMPT, "Set energy threshold for auto-enabling run mode:")
        }

        row("dbrow.setting_390") {
            column(SETTING_ID, 390)
            columnRSCM(VARBIT_ID, "varbit.buff_goading_potion_duration_disabled")
        }

        row("dbrow.setting_391") {
            column(SETTING_ID, 391)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_start_index_toggle")
        }

        row("dbrow.setting_392") {
            column(SETTING_ID, 392)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_start_index")
        }

        row("dbrow.setting_394") {
            column(SETTING_ID, 394)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_spacing")
        }

        row("dbrow.setting_396") {
            column(SETTING_ID, 396)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_visual_feedback")
        }

        row("dbrow.setting_397") {
            column(SETTING_ID, 397)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_target")
        }

        row("dbrow.setting_398") {
            column(SETTING_ID, 398)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_entity")
        }

        row("dbrow.setting_399") {
            column(SETTING_ID, 399)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_interface")
        }

        row("dbrow.setting_400") {
            column(SETTING_ID, 400)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_walk")
        }

        row("dbrow.setting_401") {
            column(SETTING_ID, 401)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_global_click")
        }

        row("dbrow.setting_402") {
            column(SETTING_ID, 402)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_player")
        }

        row("dbrow.setting_403") {
            column(SETTING_ID, 403)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_use")
        }

        row("dbrow.setting_404") {
            column(SETTING_ID, 404)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_cancel")
        }

        row("dbrow.setting_405") {
            column(SETTING_ID, 405)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_filter_examine")
        }

        row("dbrow.setting_406") {
            column(SETTING_ID, 406)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_reordering_toggle")
        }

        row("dbrow.setting_408") {
            column(SETTING_ID, 408)
            columnRSCM(VARBIT_ID, "varbit.osm_hide_hotkeys")
        }

        row("dbrow.setting_409") {
            column(SETTING_ID, 409)
            columnRSCM(VARBIT_ID, "varbit.settings_mobile_tile_highlights_hidden")
        }

        row("dbrow.setting_410") {
            column(SETTING_ID, 410)
            columnRSCM(VARBIT_ID, "varbit.settings_mobile_tile_highlight_enabled")
        }

        row("dbrow.setting_413") {
            column(SETTING_ID, 413)
            columnRSCM(VARBIT_ID, "varbit.settings_mobile_taptodrop_enabled")
        }

        row("dbrow.setting_414") {
            column(SETTING_ID, 414)
            columnRSCM(VARBIT_ID, "varbit.settings_osm_hotkeys_open_sidepanels_on_left")
        }

        row("dbrow.setting_415") {
            column(SETTING_ID, 415)
            columnRSCM(VARBIT_ID, "varbit.settings_osm_hotkeys_show_empty_hotkeys")
        }

        row("dbrow.setting_416") {
            column(SETTING_ID, 416)
            columnRSCM(VARBIT_ID, "varbit.option_npc_indicators_tagging_enabled")
        }

        row("dbrow.setting_417") {
            column(SETTING_ID, 417)
            columnRSCM(VARBIT_ID, "varbit.popout_hiscores_globalclick_enabled")
        }

        row("dbrow.setting_418") {
            column(SETTING_ID, 418)
            columnRSCM(VARBIT_ID, "varbit.buff_league_masteries_hidden")
        }

        row("dbrow.setting_419") {
            column(SETTING_ID, 419)
            columnRSCM(VARBIT_ID, "varbit.settings_minimenu_scrollbar_toggle")
        }

        row("dbrow.setting_420") {
            column(SETTING_ID, 420)
            columnRSCM(VARBIT_ID, "varbit.dream_plankspell_plain")
        }

        row("dbrow.setting_421") {
            column(SETTING_ID, 421)
            columnRSCM(VARBIT_ID, "varbit.dream_plankspell_oak")
        }

        row("dbrow.setting_422") {
            column(SETTING_ID, 422)
            columnRSCM(VARBIT_ID, "varbit.dream_plankspell_teak")
        }

        row("dbrow.setting_423") {
            column(SETTING_ID, 423)
            columnRSCM(VARBIT_ID, "varbit.dream_plankspell_mahogany")
        }

        row("dbrow.setting_424") {
            column(SETTING_ID, 424)
            columnRSCM(VARBIT_ID, "varbit.bank_depositbox_oplocu_askquantity")
        }

        row("dbrow.setting_425") {
            column(SETTING_ID, 425)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_combat_disabled")
        }

        row("dbrow.setting_426") {
            column(SETTING_ID, 426)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_atmospherics_disabled")
        }

        row("dbrow.setting_427") {
            column(SETTING_ID, 427)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_shamans_disabled")
        }

        row("dbrow.setting_428") {
            column(SETTING_ID, 428)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_olm_disabled")
        }

        row("dbrow.setting_429") {
            column(SETTING_ID, 429)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_monkey_madness_disabled")
        }

        row("dbrow.setting_430") {
            column(SETTING_ID, 430)
            columnRSCM(VARBIT_ID, "varbit.settings_mobile_show_filter_stones_enabled")
        }

        row("dbrow.setting_431") {
            column(SETTING_ID, 431)
            columnRSCM(VARBIT_ID, "varbit.clan_disable_lastseen")
        }

        row("dbrow.setting_432") {
            column(SETTING_ID, 432)
            columnRSCM(VARBIT_ID, "varbit.alchemy_warning_forinventory_disabled")
        }

        row("dbrow.setting_433") {
            column(SETTING_ID, 433)
            columnRSCM(VARBIT_ID, "varbit.settings_didyouknow_disabled")
        }

        row("dbrow.setting_434") {
            column(SETTING_ID, 434)
            columnRSCM(VARP, "varp.option_chat_colour_didyouknow_opaque")
        }

        row("dbrow.setting_435") {
            column(SETTING_ID, 435)
            columnRSCM(VARP, "varp.option_chat_colour_didyouknow_transparent")
        }

        row("dbrow.setting_436") {
            column(SETTING_ID, 436)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_snow_disabled")
        }

        row("dbrow.setting_437") {
            column(SETTING_ID, 437)
            columnRSCM(VARBIT_ID, "varbit.buff_surge_potion_cooldown_disabled")
        }

        row("dbrow.setting_439") {
            column(SETTING_ID, 439)
            columnRSCM(VARBIT_ID, "varbit.settings_hd_new_renderer_toggle")
        }

        row("dbrow.setting_441") {
            column(SETTING_ID, 441)
            columnRSCM(VARBIT_ID, "varbit.settings_world_map_hotkey_disabled")
        }

        row("dbrow.setting_442") {
            column(SETTING_ID, 442)
            columnRSCM(VARBIT_ID, "varbit.settings_interface_resizing")
        }

        row("dbrow.setting_443") {
            column(SETTING_ID, 443)
            columnRSCM(VARBIT_ID, "varbit.buff_bowstring_spool_disabled")
        }

        row("dbrow.setting_444") {
            column(SETTING_ID, 444)
            columnRSCM(VARBIT_ID, "varbit.buff_ent_totems_bonus_xp_disabled")
        }

        row("dbrow.setting_445") {
            column(SETTING_ID, 445)
            columnRSCM(VARBIT_ID, "varbit.option_camera_effect_dom_disabled")
        }

        row("dbrow.setting_446") {
            column(SETTING_ID, 446)
            columnRSCM(VARBIT_ID, "varbit.settings_farming_dig_disable_warning")
        }

        row("dbrow.setting_447") {
            column(SETTING_ID, 447)
            columnRSCM(VARBIT_ID, "varbit.settings_mobile_minimenu_icon_enabled")
        }

        row("dbrow.setting_448") {
            column(SETTING_ID, 448)
            columnRSCM(VARBIT_ID, "varbit.settings_birdhouse_seed_warning_enabled")
        }

        row("dbrow.setting_449") {
            column(SETTING_ID, 449)
            columnRSCM(VARP, "varp.settings_birdhouse_seed_warning_value")
        }

        row("dbrow.setting_450") {
            column(SETTING_ID, 450)
            columnRSCM(VARBIT_ID, "varbit.option_cannonballs_load_x")
        }

        row("dbrow.setting_451") {
            column(SETTING_ID, 451)
            columnRSCM(VARBIT_ID, "varbit.option_hidey_holes_equipped")
        }

        row("dbrow.setting_452") {
            column(SETTING_ID, 452)
            columnRSCM(VARBIT_ID, "varbit.settings_ca_open_last_interface_disabled")
        }

        row("dbrow.setting_453") {
            column(SETTING_ID, 453)
            columnRSCM(VARBIT_ID, "varbit.poll_booth_highlight_disabled")
        }

        row("dbrow.setting_454") {
            column(SETTING_ID, 454)
            columnRSCM(VARBIT_ID, "varbit.settings_sailing_side_panel_onlyonboat")
        }

        row("dbrow.setting_455") {
            column(SETTING_ID, 455)
            columnRSCM(VARBIT_ID, "varbit.sailing_charting_drink_crate_warning")
        }

        row("dbrow.setting_456") {
            column(SETTING_ID, 456)
            columnRSCM(VARBIT_ID, "varbit.settings_sailing_charting_popup")
        }

        row("dbrow.setting_457") {
            column(SETTING_ID, 457)
            columnRSCM(VARBIT_ID, "varbit.sailing_port_task_cancel_warning_disabled")
        }

        row("dbrow.setting_458") {
            column(SETTING_ID, 458)
            columnRSCM(VARBIT_ID, "varbit.sailing_current_duck_hintarrow_disabled")
        }

        row("dbrow.setting_459") {
            column(SETTING_ID, 459)
            columnRSCM(VARBIT_ID, "varbit.sailing_barracuda_trials_hintarrow_disabled")
        }

        row("dbrow.setting_460") {
            column(SETTING_ID, 460)
            columnRSCM(VARBIT_ID, "varbit.settings_colourful_fade_disabled")
        }

        row("dbrow.setting_461") {
            column(SETTING_ID, 461)
            columnRSCM(VARBIT_ID, "varbit.keep_sailing_boat_when_leaving_helm")
        }

        row("dbrow.setting_462") {
            column(SETTING_ID, 462)
            columnRSCM(VARBIT_ID, "varbit.sailing_start_boat_when_setting_heading")
        }

        row("dbrow.setting_463") {
            column(SETTING_ID, 463)
            columnRSCM(VARBIT_ID, "varbit.port_task_loading_bay_warning_dismissed")
        }

        row("dbrow.setting_464") {
            column(SETTING_ID, 464)
            columnRSCM(VARBIT_ID, "varbit.sailing_show_charting_hints")
        }

        row("dbrow.setting_465") {
            column(SETTING_ID, 465)
            columnRSCM(VARBIT_ID, "varbit.settings_sailing_dropitem_warning")
        }

        row("dbrow.setting_466") {
            column(SETTING_ID, 466)
            columnRSCM(VARBIT_ID, "varbit.sailing_show_customisation_material_messages")
        }

        row("dbrow.setting_467") {
            column(SETTING_ID, 467)
            columnRSCM(VARBIT_ID, "varbit.settings_cargo_in_hold_warning")
        }

        row("dbrow.setting_469") {
            column(SETTING_ID, 469)
            columnRSCM(VARBIT_ID, "varbit.settings_ironman_cargo_warning")
        }

        row("dbrow.setting_470") {
            column(SETTING_ID, 470)
            columnRSCM(VARBIT_ID, "varbit.settings_cargo_hold_privacy")
        }

        row("dbrow.setting_471") {
            column(SETTING_ID, 471)
            columnRSCM(VARBIT_ID, "varbit.sailing_warning_teleportoffboat")
        }

        row("dbrow.setting_472") {
            column(SETTING_ID, 472)
            columnRSCM(VARBIT_ID, "varbit.settings_new_menu_interface")
        }

        row("dbrow.setting_473") {
            column(SETTING_ID, 473)
            columnRSCM(VARBIT_ID, "varbit.settings_new_menu_transparent_interface_disabled")
        }

        row("dbrow.setting_474") {
            column(SETTING_ID, 474)
            columnRSCM(VARBIT_ID, "varbit.settings_runepouch_loadout_names_disabled")
        }

        row("dbrow.setting_475") {
            column(SETTING_ID, 475)
            columnRSCM(VARBIT_ID, "varbit.settings_fairyring_mobile_keyboard_autoopen_disabled")
        }

        row("dbrow.setting_476") {
            column(SETTING_ID, 476)
            columnRSCM(VARBIT_ID, "varbit.settings_quetzalwhistle_default_tp")
        }

        row("dbrow.setting_477") {
            column(SETTING_ID, 477)
            columnRSCM(VARP, "varp.musicplay")
        }

        row("dbrow.setting_478") {
            column(SETTING_ID, 478)
            columnRSCM(VARBIT_ID, "varbit.music_current_playlist")
        }

        row("dbrow.setting_479") {
            column(SETTING_ID, 479)
            columnRSCM(VARBIT_ID, "varbit.use_previous_music_mode_on_login")
        }

        row("dbrow.setting_480") {
            column(SETTING_ID, 480)
            columnRSCM(VARBIT_ID, "varbit.settings_music_default_track_on_area_entry")
        }

        row("dbrow.setting_481") {
            column(SETTING_ID, 481)
            columnRSCM(VARBIT_ID, "varbit.dont_update_music_on_playlist_change")
        }

        row("dbrow.setting_482") {
            column(SETTING_ID, 482)
            columnRSCM(VARBIT_ID, "varbit.use_shuffle_mode_on_manual_music_selection")
        }

        row("dbrow.setting_483") {
            column(SETTING_ID, 483)
            columnRSCM(VARBIT_ID, "varbit.settings_music_player_hide_tracks")
        }

        row("dbrow.setting_484") {
            column(SETTING_ID, 484)
            columnRSCM(VARBIT_ID, "varbit.settings_music_player_disable_text_shadow")
        }

        row("dbrow.setting_485") {
            column(SETTING_ID, 485)
            columnRSCM(VARP, "varp.music_player_colour_locked")
        }

        row("dbrow.setting_486") {
            column(SETTING_ID, 486)
            columnRSCM(VARP, "varp.music_player_colour_unlocked")
        }

        row("dbrow.setting_487") {
            column(SETTING_ID, 487)
            columnRSCM(VARP, "varp.music_player_colour_playing")
        }

        row("dbrow.setting_488") {
            column(SETTING_ID, 488)
            columnRSCM(VARP, "varp.music_player_colour_unavailable")
        }

        row("dbrow.setting_493") {
            column(SETTING_ID, 493)
            columnRSCM(VARBIT_ID, "varbit.option_level_up_chatbox_list")
        }

        row("dbrow.setting_494") {
            column(SETTING_ID, 494)
            columnRSCM(VARBIT_ID, "varbit.option_level_up_guide_list_disabled")
        }

        row("dbrow.setting_500") {
            column(SETTING_ID, 500)
            columnRSCM(VARBIT_ID, "varbit.league_teleport_dragons_stepping_stones_warning")
        }

        row("dbrow.setting_501") {
            column(SETTING_ID, 501)
            columnRSCM(VARBIT_ID, "varbit.settings_sailing_wind_on_orb_disabled")
        }

    }
}
