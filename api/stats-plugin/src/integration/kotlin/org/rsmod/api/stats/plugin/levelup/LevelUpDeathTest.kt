package org.rsmod.api.stats.plugin.levelup

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import org.junit.jupiter.api.Test
import org.rsmod.api.death.PlayerDeathCleanupHook
import org.rsmod.api.death.PlayerDeathHook
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.death.plugin.PlayerDeathScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.hit.HitType

class LevelUpDeathTest {
    @Test
    fun GameTestState.`hit closes a level-up opened on its cycle and death starts next cycle`() =
        runInjectedGameTest(GameTestScope::class, DeathHooks(), *SCRIPTS) {
            player.setCurrentLevel("stat.hitpoints", HITPOINTS)
            player.statAdvance("stat.attack", XP_TO_LEVEL_2, rate = 1.0, globalRate = 1.0)
            player.queueHit(1, HitType.Typeless, HITPOINTS, NoopPlayerHitModifier)

            advance()
            check(player.hitpoints == 0)
            assertFalse(player.isDelayed)
            assertFalse(player.ui.containsModal(LEVELUP_DISPLAY))

            advance()
            assertTrue(player.isDelayed)
        }

    @Test
    fun GameTestState.`level-up gained on the killing cycle does not hold off death`() =
        runInjectedGameTest(GameTestScope::class, DeathHooks(), *SCRIPTS) {
            player.setCurrentLevel("stat.hitpoints", HITPOINTS)
            player.queueHit(1, HitType.Typeless, HITPOINTS, NoopPlayerHitModifier)

            advance()
            check(player.hitpoints == 0)
            player.statAdvance("stat.attack", XP_TO_LEVEL_2, rate = 1.0, globalRate = 1.0)

            advance()
            assertTrue(player.isDelayed)
        }

    private class DeathHooks : AbstractModule() {
        override fun configure() {
            Multibinder.newSetBinder(binder(), PlayerDeathHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerDeathCleanupHook::class.java)
            Multibinder.newSetBinder(binder(), PlayerRespawnHook::class.java)
        }
    }

    private companion object {
        const val HITPOINTS = 5
        const val XP_TO_LEVEL_2 = 83.0
        const val LEVELUP_DISPLAY = "interface.levelup_display"
        val SCRIPTS =
            arrayOf(PlayerHitScript::class, PlayerDeathScript::class, LevelUpScript::class)
    }
}
