package org.rsmod.content.interfaces.settings.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType

object AccountDefaultVarsTable {
    const val VARP = 0
    const val VARBIT_ID = 1
    const val VALUE = 2

    fun accountDefaultVars() =
        dbTable("dbtable.account_default_vars", serverOnly = true) {
            column("varp", VARP, VarType.VARP)
            column("varbit", VARBIT_ID, VarType.INT)
            column("value", VALUE, VarType.INT)

            row("dbrow.account_default_option_run") {
                columnRSCM(VARP, "varp.option_run")
                column(VALUE, 1)
            }

            row("dbrow.account_default_com_stance") {
                columnRSCM(VARP, "varp.com_stance")
                column(VALUE, 1)
            }

            row("dbrow.account_default_option_master_volume") {
                columnRSCM(VARP, "varp.option_master_volume")
                column(VALUE, 100)
            }

            row("dbrow.account_default_option_music") {
                columnRSCM(VARP, "varp.option_music")
                column(VALUE, 20)
            }

            row("dbrow.account_default_option_sounds") {
                columnRSCM(VARP, "varp.option_sounds")
                column(VALUE, 45)
            }

            row("dbrow.account_default_option_areasounds") {
                columnRSCM(VARP, "varp.option_areasounds")
                column(VALUE, 25)
            }

            row("dbrow.account_default_xpdrops_enabled") {
                columnRSCM(VARBIT_ID, "varbit.xpdrops_enabled")
                column(VALUE, 1)
            }

            row("dbrow.account_default_xpdrops_size") {
                columnRSCM(VARBIT_ID, "varbit.xpdrops_size")
                column(VALUE, 1)
            }

            row("dbrow.account_default_xpdrops_duration") {
                columnRSCM(VARBIT_ID, "varbit.xpdrops_duration")
                column(VALUE, 3)
            }

            row("dbrow.account_default_xpdrops_counter_type") {
                columnRSCM(VARBIT_ID, "varbit.xpdrops_counter_type")
                column(VALUE, 31)
            }

            row("dbrow.account_default_xpdrops_progress_type") {
                columnRSCM(VARBIT_ID, "varbit.xpdrops_progress_type")
                column(VALUE, 31)
            }

            row("dbrow.account_default_resizable_stone_arrangement") {
                columnRSCM(VARBIT_ID, "varbit.resizable_stone_arrangement")
                column(VALUE, 1)
            }

            row("dbrow.account_default_osm_fn_mode_button") {
                columnRSCM(VARBIT_ID, "varbit.osm_fn_mode_button")
                column(VALUE, 1)
            }

            row("dbrow.account_default_chat_filter_private") {
                columnRSCM(VARBIT_ID, "varbit.chat_filter_private")
                column(VALUE, 1)
            }

            row("dbrow.account_default_esc_to_close_desktop") {
                columnRSCM(VARBIT_ID, "varbit.esc_to_close_desktop")
                column(VALUE, 1)
            }

            row("dbrow.account_default_settings_disable_tooltip_in_interface") {
                columnRSCM(VARBIT_ID, "varbit.settings_disable_tooltip_in_interface")
                column(VALUE, 1)
            }

            row("dbrow.account_default_option_settings_declutter") {
                columnRSCM(VARBIT_ID, "varbit.option_settings_declutter")
                column(VALUE, 1)
            }

            row("dbrow.account_default_cr_quests_include_members") {
                columnRSCM(VARBIT_ID, "varbit.cr_quests_include_members")
                column(VALUE, 1)
            }

            row("dbrow.account_default_bank_showincinerator") {
                columnRSCM(VARBIT_ID, "varbit.bank_showincinerator")
                column(VALUE, 1)
            }

            row("dbrow.account_default_osm_hotkey_0") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_0")
                column(VALUE, 22)
            }

            row("dbrow.account_default_osm_hotkey_1") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_1")
                column(VALUE, 1)
            }

            row("dbrow.account_default_osm_hotkey_2") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_2")
                column(VALUE, 25)
            }

            row("dbrow.account_default_osm_hotkey_3") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_3")
                column(VALUE, 24)
            }

            row("dbrow.account_default_osm_hotkey_4") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_4")
                column(VALUE, 3)
            }

            row("dbrow.account_default_osm_hotkey_loadout1_0") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout1_0")
                column(VALUE, 22)
            }

            row("dbrow.account_default_osm_hotkey_loadout1_1") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout1_1")
                column(VALUE, 23)
            }

            row("dbrow.account_default_osm_hotkey_loadout1_2") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout1_2")
                column(VALUE, 8)
            }

            row("dbrow.account_default_osm_hotkey_loadout1_3") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout1_3")
                column(VALUE, 9)
            }

            row("dbrow.account_default_osm_hotkey_loadout1_4") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout1_4")
                column(VALUE, 21)
            }

            row("dbrow.account_default_osm_hotkey_loadout2_0") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout2_0")
                column(VALUE, 24)
            }

            row("dbrow.account_default_osm_hotkey_loadout2_1") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout2_1")
                column(VALUE, 23)
            }

            row("dbrow.account_default_osm_hotkey_loadout2_2") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout2_2")
                column(VALUE, 8)
            }

            row("dbrow.account_default_osm_hotkey_loadout2_3") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout2_3")
                column(VALUE, 9)
            }

            row("dbrow.account_default_osm_hotkey_loadout2_4") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout2_4")
                column(VALUE, 1)
            }

            row("dbrow.account_default_osm_hotkey_loadout3_0") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout3_0")
                column(VALUE, 6)
            }

            row("dbrow.account_default_osm_hotkey_loadout3_1") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout3_1")
                column(VALUE, 1)
            }

            row("dbrow.account_default_osm_hotkey_loadout3_2") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout3_2")
                column(VALUE, 15)
            }

            row("dbrow.account_default_osm_hotkey_loadout3_3") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout3_3")
                column(VALUE, 9)
            }

            row("dbrow.account_default_osm_hotkey_loadout3_4") {
                columnRSCM(VARBIT_ID, "varbit.osm_hotkey_loadout3_4")
                column(VALUE, 24)
            }
        }
}
