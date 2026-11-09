package org.rsmod.api.stats.plugin.levelup

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Test
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.ui.ifOpenTop
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope

class LevelUpGuideTest {
    @Test
    fun GameTestState.`unlock level marks the guide list and hints two cycles later`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance("stat.attack", XP_TO_LEVEL_5, rate = 1.0, globalRate = 1.0)
            advance()
            assertEquals(2, player.vars[ATTACK_LIST])
            assertEquals(2, player.vars[FLASHSIDE])
            assertTrue(player.ui.containsModal(LEVELUP_DISPLAY))
            client.clearOutgoing()

            advance()
            assertEquals(emptyList<String>(), guideMessages())

            advance()
            assertEquals(
                listOf("1|Check the <col=ef1020>skill guide</col> to see what your new Attack level unlocks!"),
                guideMessages(),
            )
            assertTrue(player.ui.containsModal(LEVELUP_DISPLAY))
        }

    @Test
    fun GameTestState.`chatbox list sends the unlocked rows before the hint`() =
        runGameTest(LevelUpScript::class) {
            VarPlayerIntMapSetter.set(player, CHATBOX_LIST, 1)
            player.setBaseLevel("stat.defence", 9)
            player.statAdvance("stat.defence", XP_TO_LEVEL_10, rate = 1.0, globalRate = 1.0)
            advance()
            advance()
            client.clearOutgoing()

            advance()
            val script = client.filterIsInstance<RunClientScript>().single()
            val rows = script.values.first { it is IntArray } as IntArray
            assertEquals(listOf(10924, 10925, 10926, -1), rows.toList())
            assertEquals(emptyList<String>(), guideMessages())
            client.clearOutgoing()

            advance()
            assertEquals(
                listOf("5|Check the <col=ef1020>skill guide</col> to see what else your new Defence level unlocks!"),
                guideMessages(),
            )
        }

    @Test
    fun GameTestState.`opening the skills tab stops the flash`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance("stat.attack", XP_TO_LEVEL_5, rate = 1.0, globalRate = 1.0)
            advance()
            check(player.vars[FLASHSIDE] == 2)

            player.ifOpenTop(TOPLEVEL)
            player.ifSetEvents(STATS_STONE, -1..-1, IfEvent.Op1)
            player.ifButton(ServerCacheManager.fromComponent(STATS_STONE), op = IfButtonOp.Op1)
            advance()
            assertEquals(0, player.vars[FLASHSIDE])
            assertEquals(2, player.vars[ATTACK_LIST])
        }

    @Test
    fun GameTestState.`an unread guide list keeps its earliest level`() =
        runGameTest(LevelUpScript::class) {
            VarPlayerIntMapSetter.set(player, ATTACK_LIST, 3)
            player.statAdvance("stat.attack", XP_TO_LEVEL_5, rate = 1.0, globalRate = 1.0)
            advance()
            assertEquals(3, player.vars[ATTACK_LIST])
        }

    @Test
    fun GameTestState.`level without unlocks sends no guide prompt`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance("stat.attack", XP_TO_LEVEL_2, rate = 1.0, globalRate = 1.0)
            repeat(4) { advance() }
            assertEquals(0, player.vars[ATTACK_LIST])
            assertEquals(0, player.vars[FLASHSIDE])
            assertEquals(emptyList<String>(), guideMessages())
            assertEquals(0, client.count { it is RunClientScript })
        }

    private fun GameTestScope.guideMessages(): List<String> =
        client.filterIsInstance<MessageGame>().filter { it.type == LEVEL_UP_MESSAGE }.map { it.message }

    private companion object {
        const val XP_TO_LEVEL_2 = 83.0
        const val XP_TO_LEVEL_5 = 388.0
        const val XP_TO_LEVEL_10 = 1154.0
        const val LEVEL_UP_MESSAGE = 118
        const val ATTACK_LIST = "varbit.levelup_list_attack"
        const val FLASHSIDE = "varbit.flashside"
        const val CHATBOX_LIST = "varbit.option_level_up_chatbox_list"
        const val LEVELUP_DISPLAY = "interface.levelup_display"
        const val TOPLEVEL = "interface.toplevel_osrs_stretch"
        const val STATS_STONE = "component.toplevel_osrs_stretch:stone1"
    }
}
