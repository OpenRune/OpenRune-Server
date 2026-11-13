package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AttackPrepareAccuracyTest {
    @Test
    fun `prepare fires once per attack with the attack's details`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.AttackPrepare>()
        f.onPrepare { seen += this }

        f.manager.beginAttack(f.player, f.npc, AttackKind.Ranged, null, specialRequested = true)
        f.manager.setNextAttackDelay(f.player, 4)
        f.manager.setNextAttackDelay(f.player, 3)
        f.manager.finishAttack(f.player)

        val event = seen.single()
        assertEquals(f.npc, event.target)
        assertEquals(AttackKind.Ranged, event.kind)
        assertTrue(event.special)
    }

    @Test
    fun `waiting on the attack delay does not prepare`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.AttackPrepare>()
        f.onPrepare { seen += this }

        f.manager.beginAttack(f.player, f.npc, AttackKind.Melee, null)
        f.manager.finishAttack(f.player)

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `setting a delay outside an attack does not prepare`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.AttackPrepare>()
        f.onPrepare { seen += this }

        f.manager.setNextAttackDelay(f.player, 4)

        assertTrue(seen.isEmpty())
    }

    @Test
    fun `no subscriber rolls once at the attack's own multiplier`() {
        val f = Fixture(outcomes = listOf(false))

        assertFalse(f.inAttack { f.melee(multiplier = 1.25) })

        f.verifyMeleeRolls(multiplier = 1.25, times = 1)
    }

    @Test
    fun `a roll outside an attack is unaffected`() {
        val f = Fixture(outcomes = listOf(false))

        assertFalse(f.melee(multiplier = 1.0))

        f.verifyMeleeRolls(multiplier = 1.0, times = 1)
    }

    @Test
    fun `the multiplier stacks with the attack's own`() {
        val f = Fixture()
        f.onPrepare { accuracyMultiplier = 2.0 }

        f.inAttack { f.melee(multiplier = 1.25) }

        f.verifyMeleeRolls(multiplier = 2.5, times = 1)
    }

    @Test
    fun `two rolls succeed if the second does and stop there`() {
        val f = Fixture(outcomes = listOf(false, true))
        f.onPrepare { accuracyRolls = 2 }

        assertTrue(f.inAttack { f.melee(multiplier = 1.0) })

        f.verifyMeleeRolls(multiplier = 1.0, times = 2)
    }

    @Test
    fun `two rolls stop early after a first success`() {
        val f = Fixture(outcomes = listOf(true, false))
        f.onPrepare { accuracyRolls = 2 }

        assertTrue(f.inAttack { f.melee(multiplier = 1.0) })

        f.verifyMeleeRolls(multiplier = 1.0, times = 1)
    }

    @Test
    fun `two failed rolls miss`() {
        val f = Fixture(outcomes = listOf(false, false))
        f.onPrepare { accuracyRolls = 2 }

        assertFalse(f.inAttack { f.melee(multiplier = 1.0) })

        f.verifyMeleeRolls(multiplier = 1.0, times = 2)
    }

    @Test
    fun `a roll count below one still rolls once`() {
        val f = Fixture(outcomes = listOf(true))
        f.onPrepare { accuracyRolls = 0 }

        assertTrue(f.inAttack { f.melee(multiplier = 1.0) })

        f.verifyMeleeRolls(multiplier = 1.0, times = 1)
    }

    @Test
    fun `never miss skips the roll`() {
        val f = Fixture(outcomes = listOf(false))
        f.onPrepare { neverMiss = true }

        assertTrue(f.inAttack { f.melee(multiplier = 1.0) })

        f.verifyMeleeRolls(multiplier = 1.0, times = 0)
    }

    @Test
    fun `never miss applies to every kind of accuracy roll`() {
        val f = Fixture(outcomes = listOf(false))
        f.onPrepare { neverMiss = true }
        val p = f.player
        val n = f.npc

        f.inAttack {
            assertTrue(f.manager.rollMeleeAccuracy(p, n, null, null, null, 1.0))
            assertTrue(f.manager.rollRangedAccuracy(p, n, null, null, null, 1.0))
            assertTrue(f.manager.rollStaffAccuracy(p, n, null, 1.0))
            assertTrue(f.manager.rollSpellAccuracy(p, n, mockk(relaxed = true), null, false))
            assertTrue(f.manager.rollMagicalMeleeAccuracy(p, n, null, null, 1.0))
            assertTrue(f.manager.rollRangedMeleeAccuracy(p, n, null, null, null, 1.0))
            assertTrue(f.manager.rollRangedMagicAccuracy(p, n, null, null, 1.0))
            assertTrue(f.manager.rollMagicalRangedAccuracy(p, n, null, null, 1.0))
            true
        }
    }

    @Test
    fun `a spell's multiplier reaches the formula`() {
        val f = Fixture()
        f.onPrepare { accuracyMultiplier = 3.0 }
        val spell = mockk<dev.openrune.types.ItemServerType>(relaxed = true)

        f.inAttack { f.manager.rollSpellAccuracy(f.player, f.npc, spell, null, false) }

        verify(exactly = 1) {
            f.accuracy.rollSpellAccuracy(f.player, f.npc, spell, null, false, any(), 3.0)
        }
    }

    @Test
    fun `settings do not carry into the next attack`() {
        val f = Fixture(outcomes = listOf(false))
        var boost = true
        f.onPrepare {
            if (boost) {
                neverMiss = true
                accuracyMultiplier = 2.0
                boost = false
            }
        }

        assertTrue(f.inAttack { f.melee(multiplier = 1.0) })
        assertFalse(f.inAttack { f.melee(multiplier = 1.0) })

        f.verifyMeleeRolls(multiplier = 1.0, times = 1)
    }

    @Test
    fun `an extra roll made after the attack gets no settings`() {
        val f = Fixture(outcomes = listOf(false))
        var extra: Boolean? = null
        f.onPrepare { neverMiss = true }
        f.events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) {
            extra = f.melee(multiplier = 1.0)
        }

        assertTrue(f.inAttack { f.melee(multiplier = 1.0) })

        assertEquals(false, extra)
    }

    @Test
    fun `the result records one roll with the final outcome, however many rerolls`() {
        val f = Fixture(outcomes = listOf(false, true))
        val results = mutableListOf<PlayerAttackEvents.AttackResult>()
        f.events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) { results += this }
        f.onPrepare { accuracyRolls = 2 }

        f.inAttack { f.melee(multiplier = 1.0) }

        assertTrue(results.single().rolls.single().success)
    }

    @OptIn(InternalApi::class)
    private class Fixture(outcomes: List<Boolean> = listOf(true)) {
        val events = EventBus()
        val player =
            Player().apply { worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14)) }
        val npc =
            Npc(ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!, CoordGrid.ZERO).apply {
                slotId = 1
                assignUid()
            }
        val accuracy =
            mockk<AccuracyFormulae>(relaxed = true) {
                every {
                    rollMeleeAccuracy(any<Player>(), any<Npc>(), any(), any(), any(), any(), any())
                } returnsMany outcomes
            }
        val manager =
            PlayerAttackManager(
                random = mockk(relaxed = true),
                eventBus = events,
                worldRepo = mockk(relaxed = true),
                accuracy = accuracy,
                maxHits = mockk(relaxed = true),
                npcHitModifier = mockk(relaxed = true),
                playerHitModifier = mockk(relaxed = true),
                npcInteractions = mockk(relaxed = true),
                npcTInteractions = mockk(relaxed = true),
                playerInteractions = mockk(relaxed = true),
                playerTInteractions = mockk(relaxed = true),
                pvpPlayerHitHooks = emptySet(),
            )

        fun onPrepare(action: PlayerAttackEvents.AttackPrepare.() -> Unit) {
            events.subscribeUnbound(PlayerAttackEvents.AttackPrepare::class.java, action)
        }

        fun melee(multiplier: Double): Boolean =
            manager.rollMeleeAccuracy(player, npc, null, null, null, multiplier)

        fun inAttack(roll: () -> Boolean): Boolean {
            manager.beginAttack(player, npc, AttackKind.Melee, null)
            manager.setNextAttackDelay(player, 4)
            val result = roll()
            manager.finishAttack(player)
            return result
        }

        fun verifyMeleeRolls(multiplier: Double, times: Int) {
            verify(exactly = times) {
                accuracy.rollMeleeAccuracy(player, npc, null, null, null, multiplier, any())
            }
        }
    }

    private companion object {
        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
        }
    }
}
