package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import io.mockk.every
import io.mockk.mockk
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
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.player.events.AttackKind
import org.rsmod.api.player.events.PlayerAttackEvents
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid

/**
 * Proves the plan's worked example: a relic can make any attack bounce from an `AttackResult`
 * handler, one queued hop at a time, with a limit or with none, and with no further engine hook.
 */
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class AttackResultBounceTest {
    @Test
    fun `an attack records its rolls and hit`() {
        val f = Fixture()

        f.attack()

        val result = f.results.single()
        assertEquals(AttackKind.Ranged, result.kind)
        assertEquals(4, result.cycles)
        assertEquals(1, result.hits.size)
        val roll = result.rolls.single()
        assertTrue(roll.success)
        assertEquals(MAX_HIT, roll.maxHit)
        assertEquals(ROLLED, roll.damage)
    }

    @Test
    fun `a miss is recorded and no damage is rolled`() {
        val f = Fixture(accuracy = false)

        f.attack()

        val roll = f.results.single().rolls.single()
        assertFalse(roll.success)
        assertEquals(0, roll.damage)
    }

    @Test
    fun `waiting on the attack delay publishes nothing`() {
        val f = Fixture()

        f.manager.beginAttack(f.player, f.victims[0], AttackKind.Ranged, spell = null)
        f.manager.finishAttack(f.player)

        assertTrue(f.results.isEmpty())
    }

    @Test
    fun `a bounce limit is respected`() {
        val f = Fixture()
        f.bounceOnResult(limit = 3)

        f.attack()
        f.drainQueue()

        assertEquals(3, f.bounces)
        assertEquals(1, f.results.size, "bounces must not publish another result")
    }

    @Test
    fun `an unlimited chain ends when the chance fails, without recursion`() {
        val f = Fixture()
        var hopsAllowed = 5_000
        f.bounceOnResult(limit = Int.MAX_VALUE) { hopsAllowed-- > 0 }

        f.attack()
        f.drainQueue()

        assertEquals(5_000, f.bounces)
        assertEquals(1, f.results.size)
    }

    @Test
    fun `a chain stops as soon as the caster is gone`() {
        val f = Fixture()
        var casterPresent = true
        f.bounceOnResult(limit = Int.MAX_VALUE) { casterPresent }

        f.attack()
        repeat(2 * HOP_DELAY) { f.runQueueOnce() }
        val before = f.bounces
        casterPresent = false
        f.drainQueue()

        assertEquals(2, before)
        assertEquals(before, f.bounces)
        assertTrue(f.queue.isEmpty)
    }

    @Test
    fun `each attack starts its own chain`() {
        val f = Fixture()
        f.bounceOnResult(limit = 2)

        f.attack()
        f.attack()
        f.drainQueue()

        assertEquals(4, f.bounces)
        assertEquals(2, f.results.size)
    }

    @OptIn(InternalApi::class)
    private class Fixture(accuracy: Boolean = true) {
        val events = EventBus()
        val queue = WorldQueueList()
        val results = mutableListOf<PlayerAttackEvents.AttackResult>()
        val player =
            Player().apply { worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14)) }
        val victims = List(3) { npc(slot = it + 1) }
        var bounces = 0

        private val accuracyFormulae =
            mockk<AccuracyFormulae> {
                every {
                    rollRangedAccuracy(
                        any<Player>(),
                        any<Npc>(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns accuracy
            }
        private val maxHitFormulae =
            mockk<MaxHitFormulae> {
                every {
                    getRangedMaxHit(any<Player>(), any<Npc>(), any(), any(), any(), any())
                } returns MAX_HIT
            }
        private val random =
            mockk<GameRandom> { every { of(any<IntRange>()) } answers { ROLLED } }
        private val noopModifier =
            object : NpcHitModifier {
                override fun HitBuilder.modify(target: Npc) = Unit
            }
        val manager =
            PlayerAttackManager(
                random = random,
                eventBus = events,
                worldRepo = mockk(relaxed = true),
                accuracy = accuracyFormulae,
                maxHits = maxHitFormulae,
                npcHitModifier = noopModifier,
                playerHitModifier = mockk(relaxed = true),
                npcInteractions = mockk(relaxed = true),
                npcTInteractions = mockk(relaxed = true),
                playerInteractions = mockk(relaxed = true),
                playerTInteractions = mockk(relaxed = true),
                pvpPlayerHitHooks = emptySet(),
            )

        init {
            events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) {
                results += this
            }
        }

        fun attack() {
            val target = victims[0]
            manager.beginAttack(player, target, AttackKind.Ranged, spell = null)
            manager.setNextAttackDelay(player, 4)
            val damage =
                if (rollAccuracy(target)) manager.rollRangedMaxHit(player, target, null, null, 1.0, 0)
                else 0
            manager.queueRangedHit(player, target, null, damage, clientDelay = 30, hitDelay = 2)
            manager.finishAttack(player)
        }

        fun bounceOnResult(limit: Int, keepGoing: () -> Boolean = { true }) {
            events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) {
                val first = rolls.first()
                hop(first.target as Npc, first.maxHit, remaining = limit, keepGoing)
            }
        }

        private fun hop(from: Npc, maxHit: Int, remaining: Int, keepGoing: () -> Boolean) {
            if (remaining <= 0) {
                return
            }
            queue.add(HOP_DELAY) {
                if (keepGoing()) {
                    val next = victims.first { it !== from }
                    val damage = if (rollAccuracy(next)) random.of(1..maxHit) else 0
                    manager.queueRangedHit(player, next, null, damage, 0, HOP_DELAY)
                    bounces++
                    hop(next, maxHit, remaining - 1, keepGoing)
                }
            }
        }

        private fun rollAccuracy(target: Npc): Boolean =
            manager.rollRangedAccuracy(player, target, null, null, null, 1.0)

        fun runQueueOnce() {
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                iterator.next().remainingCycles--
            }
            iterator.cleanUp()
            val fire = queue.iterator()
            while (fire.hasNext()) {
                val entry = fire.next()
                if (entry.remainingCycles <= 0) {
                    fire.remove()
                    entry.action()
                }
            }
            fire.cleanUp()
        }

        fun drainQueue() {
            var safety = 100_000
            while (queue.isNotEmpty && safety-- > 0) {
                runQueueOnce()
            }
            check(safety > 0) { "queue did not drain" }
        }

        private fun npc(slot: Int): Npc {
            val type = ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!
            return Npc(type, CoordGrid.ZERO).apply {
                slotId = slot
                assignUid()
            }
        }
    }

    private companion object {
        const val MAX_HIT = 20
        const val ROLLED = 9
        const val HOP_DELAY = 2

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
