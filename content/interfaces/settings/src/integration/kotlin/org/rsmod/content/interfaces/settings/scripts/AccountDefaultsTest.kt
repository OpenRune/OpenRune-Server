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

    private companion object {
        const val NEW_ACCOUNT = "varbit.new_player_account"
        const val ATTACK_PRIORITY = "varp.option_attackpriority"
    }
}
