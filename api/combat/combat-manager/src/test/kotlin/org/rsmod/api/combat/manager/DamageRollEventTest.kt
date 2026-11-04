package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.ItemServerType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
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
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.Inventory
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DamageRollEventTest {
    @Test
    fun `no subscriber rolls the natural range`() {
        val f = Fixture()

        assertEquals(ROLLED, f.melee())

        assertEquals(1..MAX_HIT, f.rolledRange.captured)
    }

    @Test
    fun `a subscriber that changes nothing rolls the same range`() {
        val f = Fixture()
        f.onRoll {}

        assertEquals(ROLLED, f.melee())

        assertEquals(1..MAX_HIT, f.rolledRange.captured)
    }

    @Test
    fun `the event reports the natural range and style`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll { seen += this }

        f.melee()

        val event = seen.single()
        assertEquals(HitType.Melee, event.type)
        assertEquals(1, event.minHit)
        assertEquals(MAX_HIT, event.maxHit)
        assertFalse(event.missed)
        assertEquals(f.npc, event.target)
    }

    @Test
    fun `a raised max hit changes the rolled range`() {
        val f = Fixture()
        f.onRoll { maxHit += 5 }

        f.melee()

        assertEquals(1..MAX_HIT + 5, f.rolledRange.captured)
    }

    @Test
    fun `a raised minimum changes the rolled range`() {
        val f = Fixture()
        f.onRoll { minHit += 3 }

        f.melee()

        assertEquals(4..MAX_HIT, f.rolledRange.captured)
    }

    @Test
    fun `min equal to max guarantees a max hit`() {
        val f = Fixture()
        f.onRoll { minHit = maxHit }

        f.melee()

        assertEquals(MAX_HIT..MAX_HIT, f.rolledRange.captured)
    }

    @Test
    fun `a set damage skips the roll`() {
        val f = Fixture()
        f.onRoll { damage = 77 }

        assertEquals(77, f.melee())

        verify(exactly = 0) { f.random.of(any<IntRange>()) }
    }

    @Test
    fun `a minimum above the max cannot break the roll`() {
        val f = Fixture()
        f.onRoll { minHit = MAX_HIT + 50 }

        f.melee()

        assertEquals(MAX_HIT..MAX_HIT, f.rolledRange.captured)
    }

    @Test
    fun `negative values cannot produce negative damage`() {
        val f = Fixture()
        f.onRoll { damage = -5 }

        assertEquals(0, f.melee())
    }

    @Test
    fun `a miss stays zero by default but reports itself`() {
        val f = Fixture(accuracy = false)
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll { seen += this }

        assertEquals(0, f.melee())

        assertTrue(seen.single().missed)
        verify(exactly = 0) { f.random.of(any<IntRange>()) }
    }

    @Test
    fun `a relic can decide a minimum hit applies on a miss`() {
        val f = Fixture(accuracy = false)
        f.onRoll { if (missed) damage = 3 }

        assertEquals(3, f.melee())
    }

    @Test
    fun `a relic can ignore misses so a minimum hit only applies to landed hits`() {
        val f = Fixture(accuracy = false)
        f.onRoll { if (!missed) minHit += 3 }

        assertEquals(0, f.melee())
    }

    @Test
    fun `a miss publishes nothing without a subscriber`() {
        val f = Fixture(accuracy = false)

        assertEquals(0, f.melee())
    }

    @Test
    fun `a ranged miss also publishes`() {
        val f = Fixture(accuracy = false)
        f.onRoll { if (missed && type == HitType.Ranged) damage = 2 }

        val attack = CombatAttack.Ranged(mockk(relaxed = true), null, null)

        assertEquals(2, f.manager.rollRangedDamage(f.player, f.npc, attack))
    }

    @Test
    fun `a spell uses its own range including a zero minimum`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll { seen += this }

        f.spell(baseMaxHit = 30)

        val event = seen.single()
        assertEquals(HitType.Magic, event.type)
        assertEquals(0, event.minHit)
        assertEquals(30, event.maxHit)
        assertEquals(0..30, f.rolledRange.captured)
    }

    @Test
    fun `a spell roll says which spell it is`() {
        val f = Fixture()
        val spell = mockk<ItemServerType>(relaxed = true)
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll { seen += this }

        f.spell(baseMaxHit = 30, spell = spell)

        assertSame(spell, seen.single().spell)
    }

    @Test
    fun `a spell splash says which spell it is`() {
        val f = Fixture()
        val spell = mockk<ItemServerType>(relaxed = true)
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll { seen += this }

        f.manager.queueSplashHit(f.player, f.npc, spell, 30, 2)

        assertSame(spell, seen.single().spell)
    }

    @Test
    fun `melee and staff rolls have no spell`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll { seen += this }

        f.melee()
        f.manager.rollStaffMaxHit(f.player, f.npc, 25, 1.0)

        assertEquals(listOf(null, null), seen.map { it.spell })
    }

    @Test
    fun `a staff rolls through the event`() {
        val f = Fixture()
        f.onRoll { damage = 11 }

        assertEquals(11, f.manager.rollStaffMaxHit(f.player, f.npc, 25, 1.0))
    }

    @Test
    fun `a spell splash can deal damage and is marked as a miss`() {
        val f = Fixture()
        val seen = mutableListOf<PlayerAttackEvents.DamageRoll>()
        f.onRoll {
            seen += this
            damage = 4
        }

        val hit = f.manager.queueSplashHit(f.player, f.npc, null, 30, 2)

        assertTrue(seen.single().missed)
        assertEquals(4, hit.damage)
    }

    @Test
    fun `a spell splash stays zero by default`() {
        val f = Fixture()

        assertEquals(0, f.manager.queueSplashHit(f.player, f.npc, null, 30, 2).damage)
    }

    @Test
    fun `the attack result reports the final max hit and damage`() {
        val f = Fixture()
        val results = mutableListOf<PlayerAttackEvents.AttackResult>()
        f.events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) { results += this }
        f.onRoll {
            maxHit = 50
            damage = 50
        }

        f.manager.beginAttack(f.player, f.npc, AttackKind.Melee, spell = null)
        f.melee()
        f.manager.finishAttack(f.player)

        val roll = results.single().rolls.single()
        assertEquals(50, roll.maxHit)
        assertEquals(50, roll.damage)
    }

    @Test
    fun `the attack result reports damage dealt on a miss`() {
        val f = Fixture(accuracy = false)
        val results = mutableListOf<PlayerAttackEvents.AttackResult>()
        f.events.subscribeUnbound(PlayerAttackEvents.AttackResult::class.java) { results += this }
        f.onRoll { if (missed) damage = 3 }

        f.manager.beginAttack(f.player, f.npc, AttackKind.Melee, spell = null)
        f.melee()
        f.manager.finishAttack(f.player)

        val roll = results.single().rolls.single()
        assertFalse(roll.success)
        assertEquals(3, roll.damage)
    }

    @OptIn(InternalApi::class)
    private class Fixture(accuracy: Boolean = true) {
        val events = EventBus()
        val player =
            Player().apply { worn = Inventory(InventoryServerType(size = 14), arrayOfNulls(14)) }
        val npc =
            Npc(ServerCacheManager.getNpc("npc.bob".asRSCM(RSCMType.NPC))!!, CoordGrid.ZERO).apply {
                slotId = 1
                assignUid()
            }
        val rolledRange = slot<IntRange>()
        val random = mockk<GameRandom> { every { of(capture(rolledRange)) } returns ROLLED }

        private val accuracyFormulae =
            mockk<AccuracyFormulae> {
                every {
                    rollMeleeAccuracy(any<Player>(), any<Npc>(), any(), any(), any(), any(), any())
                } returns accuracy
                every {
                    rollRangedAccuracy(any<Player>(), any<Npc>(), any(), any(), any(), any(), any())
                } returns accuracy
            }
        private val maxHitFormulae =
            mockk<MaxHitFormulae> {
                every { getMeleeMaxHit(any<Player>(), any<Npc>(), any(), any(), any(), any()) } returns
                    MAX_HIT
                every { getStaffMaxHit(any<Player>(), any<Npc>(), any(), any()) } returns MAX_HIT
                every {
                    getSpellMaxHitRange(any<Player>(), any<Npc>(), any(), any(), any(), any(), any())
                } answers { 0..arg<Int>(4) }
            }
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

        fun onRoll(action: PlayerAttackEvents.DamageRoll.() -> Unit) {
            events.subscribeUnbound(PlayerAttackEvents.DamageRoll::class.java, action)
        }

        fun melee(): Int =
            manager.rollMeleeDamage(
                player,
                npc,
                CombatAttack.Melee(null, null, null, CombatStance.entries.first()),
            )

        fun spell(baseMaxHit: Int, spell: ItemServerType = mockk(relaxed = true)): Int =
            manager.rollSpellMaxHit(
                player,
                npc,
                spell,
                null,
                baseMaxHit,
                attackRate = 5,
                sunfireRune = false,
            )
    }

    private companion object {
        const val MAX_HIT = 20
        const val ROLLED = 9

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
