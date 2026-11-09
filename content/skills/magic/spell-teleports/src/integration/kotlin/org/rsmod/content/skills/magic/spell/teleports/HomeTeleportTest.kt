package org.rsmod.content.skills.magic.spell.teleports

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import dev.openrune.ServerCacheManager
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.SpellQuestRequirement
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.spells.MagicSpellModule
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.api.utils.time.epochMinute
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class HomeTeleportTest {
    @Test
    fun GameTestState.`home teleport lands on the 24th tick and starts the cooldown`() =
        runInjectedGameTest(SpellTeleportScript::class, HomeTeleportTestModule(), SpellTeleportScript::class) {
            prepare()
            castHome()

            advance(ticks = 21)
            assertEquals(0, player.vars[BUSY])
            advance(ticks = 1)
            assertEquals(1, player.vars[BUSY])
            advance(ticks = 2)
            assertEquals(START, player.coords)
            assertEquals(0, player.vars[TIMER])

            advance(ticks = 1)
            assertTrue(player.coords.chebyshevDistance(LUMBRIDGE) <= 1, "${player.coords}")
            assertEquals(0, player.vars[BUSY])
            assertTrue(epochMinute() - player.vars[TIMER] in 0..1, "${player.vars[TIMER]}")
        }

    @Test
    fun GameTestState.`walking away cancels the cast without a cooldown`() =
        runInjectedGameTest(SpellTeleportScript::class, HomeTeleportTestModule(), SpellTeleportScript::class) {
            prepare()
            castHome()
            advance(ticks = 5)

            player.clearPendingAction()
            advance(ticks = 30)

            assertEquals(START, player.coords)
            assertEquals(0, player.vars[TIMER])
        }

    @Test
    fun GameTestState.`a hit cancels the cast`() =
        runInjectedGameTest(SpellTeleportScript::class, HomeTeleportTestModule(), SpellTeleportScript::class) {
            prepare()
            castHome()
            advance(ticks = 3)

            player.queueHit(1, HitType.Typeless, 1, NoopPlayerHitModifier)
            advance(ticks = 30)

            assertEquals(START, player.coords)
            assertEquals(0, player.vars[TIMER])
        }

    @Test
    fun GameTestState.`casting while moving is refused`() =
        runInjectedGameTest(SpellTeleportScript::class, HomeTeleportTestModule(), SpellTeleportScript::class) {
            prepare()
            player.routeDestination.add(START.translate(0, 5))
            castHome()
            advance(ticks = 1)

            assertMessageSent("You can't use that teleport at the moment.")
            player.routeDestination.clear()
            advance(ticks = 30)
            assertTrue(player.coords.chebyshevDistance(LUMBRIDGE) > 1, "${player.coords}")
            assertEquals(0, player.vars[TIMER])
        }

    @Test
    fun GameTestState.`casting on cooldown names the minutes left`() =
        runInjectedGameTest(SpellTeleportScript::class, HomeTeleportTestModule(), SpellTeleportScript::class) {
            prepare()
            VarPlayerIntMapSetter.set(player, TIMER, epochMinute() - 12)
            castHome()
            advance(ticks = 1)

            assertMessageSent("You need to wait another 18 minutes to cast this spell.")
            advance(ticks = 30)
            assertEquals(START, player.coords)
        }

    private fun GameTestScope.prepare() {
        allocZoneCollision(START)
        for (dx in -1..1) for (dz in -1..1) allocZoneCollision(LUMBRIDGE.translate(dx, dz))
        player.teleport(START)
        player.ifOpenOverlay("interface.magic_spellbook", "component.toplevel_osrs_stretch:side6")
        // A fresh world's map clock is still within the active-combat window of `lastcombat` 0.
        advance(ticks = 10)
    }

    private fun GameTestScope.castHome() {
        player.ifButton(ServerCacheManager.fromComponent("component.magic_spellbook:teleport_home_standard"))
    }

    private class HomeTeleportTestModule : AbstractModule() {
        override fun configure() {
            install(MagicSpellModule())
            Multibinder.newSetBinder(binder(), SpellQuestRequirement::class.java)
        }
    }

    private companion object {
        val START = CoordGrid(0, 50, 50, 10, 10)
        val LUMBRIDGE = CoordGrid(3221, 3218, 0)
        const val BUSY = "varbit.busy"
        const val TIMER = "varp.aide_tele_timer"
    }
}
