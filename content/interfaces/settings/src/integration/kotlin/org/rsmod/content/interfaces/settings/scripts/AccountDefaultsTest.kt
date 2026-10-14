package org.rsmod.content.interfaces.settings.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player

class AccountDefaultsTest {
    @Test
    fun GameTestState.`a new account starts with the live default settings`() =
        runGameTest(AllSettingsScript::class) {
            val fresh = Player().also { VarPlayerIntMapSetter.set(it, NEW_ACCOUNT, 1) }
            registerPlayer(player = fresh)

            assertEquals(1, fresh.vars["varp.option_attackpriority"])
            assertEquals(2, fresh.vars["varp.option_attackpriority_npc"])
            assertEquals(13, fresh.vars["varbit.stone_inv_key"])
            assertEquals(11, fresh.vars["varbit.questlist_sort_type"])
            assertEquals(1, fresh.vars["varp.option_run"])
            assertEquals(25, fresh.vars["varp.option_areasounds"])
            assertEquals(31, fresh.vars["varbit.xpdrops_counter_type"])
            assertEquals(0, fresh.vars["varbit.bounty_teleport_warning"])
            assertEquals(0, fresh.vars["varbit.bank_hidedepositinv"])
        }

    @Test
    fun GameTestState.`an existing account keeps its own settings on login`() =
        runGameTest(AllSettingsScript::class) {
            val existing = Player().also { VarPlayerIntMapSetter.set(it, ATTACK_PRIORITY, 3) }
            registerPlayer(player = existing)

            assertEquals(3, existing.vars[ATTACK_PRIORITY])
            assertEquals(0, existing.vars["varp.option_run"])
        }

    @Test
    fun GameTestState.`the level-up pop-up dropdown sets both of its varbits`() =
        runGameTest {
            val setting = Settings.getSetting(SettingUtils.LEVEL_UP_POPUPS_SETTING_ID)

            SettingUtils.setDropdown(player, 0, setting)
            assertEquals(1, player.vars[MESSAGE_DISABLED])
            assertEquals(0, player.vars[POPUP_LIST])

            SettingUtils.setDropdown(player, 2, setting)
            assertEquals(0, player.vars[MESSAGE_DISABLED])
            assertEquals(1, player.vars[POPUP_LIST])

            SettingUtils.setDropdown(player, 1, setting)
            assertEquals(0, player.vars[MESSAGE_DISABLED])
            assertEquals(0, player.vars[POPUP_LIST])
        }

    private companion object {
        const val NEW_ACCOUNT = "varbit.new_player_account"
        const val ATTACK_PRIORITY = "varp.option_attackpriority"
        const val MESSAGE_DISABLED = "varbit.option_level_up_message_disabled"
        const val POPUP_LIST = "varbit.option_level_up_popup_list"
    }
}
