package org.rsmod.content.areas.misc.tutorialisland

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.hook.HeldEquipValidateHook
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class TutorialRulesTest {
    @Test
    fun GameTestState.`logs advance the woodcutting stage`() =
        runInjectedGameTest(TutorialRules::class, TestHooks()) { rules ->
            player.tutorialStage = TutorialStage.AXE_GIVEN
            player.invAdd(player.inv, TutorialObj.LOGS, 1)
            assertEquals(TutorialStage.LOGS_CUT, rules.nextStage(player))
        }

    @Test
    fun GameTestState.`a bronze bar advances the smelting stage`() =
        runInjectedGameTest(TutorialRules::class, TestHooks()) { rules ->
            player.tutorialStage = TutorialStage.COPPER_MINED
            player.invAdd(player.inv, TutorialObj.BRONZE_BAR, 1)
            assertEquals(TutorialStage.BAR_SMELTED, rules.nextStage(player))
        }

    @Test
    fun GameTestState.`an equipped sword advances the combat stage`() =
        runInjectedGameTest(TutorialRules::class, TestHooks()) { rules ->
            player.tutorialStage = TutorialStage.COMBAT_GUIDE_SWORD
            player.invAdd(player.worn, TutorialObj.BRONZE_SWORD, 1)
            assertEquals(TutorialStage.SWORD_EQUIPPED, rules.nextStage(player))
        }

    @Test
    fun GameTestState.`entering the quest house advances the quest stage`() =
        runInjectedGameTest(TutorialRules::class, TestHooks()) { rules ->
            player.tutorialStage = TutorialStage.QUEST_HOUSE_ENTERED
            player.coords = CoordGrid(3086, 3125, 0)
            assertEquals(TutorialStage.QUEST_HOUSE_INSIDE, rules.nextStage(player))
        }

    private class TestHooks : AbstractModule() {
        override fun configure() {
            Multibinder.newSetBinder(binder(), HeldEquipValidateHook::class.java)
        }
    }
}
