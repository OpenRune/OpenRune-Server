package org.rsmod.api.combat.manager

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
class PlayerAttackManagerForcedMaxHitTest {
    private val accuracy = mockk<AccuracyFormulae>(relaxed = true)
    private val maxHits = mockk<MaxHitFormulae>(relaxed = true)
    private val random = mockk<GameRandom>(relaxed = true)

    private val manager =
        PlayerAttackManager(
            random,
            mockk(relaxed = true),
            mockk(relaxed = true),
            accuracy,
            maxHits,
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            mockk(relaxed = true),
            emptySet(),
        )

    init {
        every { random.of(any<IntRange>()) } returns RANDOM_ROLL
        every { maxHits.getMeleeMaxHit(any<Player>(), any<Npc>(), any(), any(), any(), any()) } returns
            MAX_HIT
    }

    @Test
    fun `melee accuracy is forced only when the melee bit is set`() {
        assertTrue(manager.rollMeleeAccuracy(source(), npc(MELEE), null, null, null, 1.0))
        assertFalse(manager.rollMeleeAccuracy(source(), npc(RANGED or MAGIC), null, null, null, 1.0))
        assertFalse(manager.rollMeleeAccuracy(source(), npc(0), null, null, null, 1.0))
    }

    @Test
    fun `ranged accuracy is forced only when the ranged bit is set`() {
        assertTrue(manager.rollRangedAccuracy(source(), npc(RANGED), null, null, null, 1.0))
        assertFalse(manager.rollRangedAccuracy(source(), npc(MELEE or MAGIC), null, null, null, 1.0))
        assertFalse(manager.rollRangedAccuracy(source(), npc(0), null, null, null, 1.0))
    }

    @Test
    fun `spell accuracy is forced only when the magic bit is set`() {
        val spell = mockk<ItemServerType>(relaxed = true)
        assertTrue(manager.rollSpellAccuracy(source(), npc(MAGIC), spell, null, false))
        assertFalse(manager.rollSpellAccuracy(source(), npc(MELEE or RANGED), spell, null, false))
        assertFalse(manager.rollSpellAccuracy(source(), npc(0), spell, null, false))
    }

    @Test
    fun `a flag with several bits forces each of its styles`() {
        val npc = npc(MELEE or MAGIC)
        assertTrue(manager.rollMeleeAccuracy(source(), npc, null, null, null, 1.0))
        assertFalse(manager.rollRangedAccuracy(source(), npc, null, null, null, 1.0))
        assertTrue(manager.rollSpellAccuracy(source(), npc, mockk(relaxed = true), null, false))
    }

    @Test
    fun `melee damage is the calculated max hit when forced and random otherwise`() {
        assertEquals(MAX_HIT, manager.rollMeleeMaxHit(source(), npc(MELEE), null, null, 1.0))
        assertEquals(RANDOM_ROLL, manager.rollMeleeMaxHit(source(), npc(MAGIC), null, null, 1.0))
        assertEquals(RANDOM_ROLL, manager.rollMeleeMaxHit(source(), npc(0), null, null, 1.0))
    }

    private fun source() = Player()

    private fun npc(forcedStyles: Int): Npc {
        val type = NpcServerType(id = 1, name = "Baboon", size = 1, hitpoints = 50)
        return Npc(type, CoordGrid(0, 1, 1, 0, 0)).apply {
            vars["varn.forced_max_hit_styles"] = forcedStyles
        }
    }

    companion object {
        private const val MELEE = 1
        private const val RANGED = 2
        private const val MAGIC = 4
        private const val MAX_HIT = 20
        private const val RANDOM_ROLL = 7

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
