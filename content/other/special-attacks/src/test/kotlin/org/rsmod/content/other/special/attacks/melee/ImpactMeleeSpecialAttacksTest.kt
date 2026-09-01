package org.rsmod.content.other.special.attacks.melee

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ImpactMeleeSpecialAttacksTest {
    @Test
    fun `bandos drain spends down stats in order, stopping once the amount is spent`() {
        // Wiki's own worked example: 20 Defence, 50 Attack, 20 Prayer, an 80 hit drains
        // 20 Defence, 20 Prayer (skipping Strength=0), then 40 Attack.
        val order = listOf(20, 0, 20, 50, 0, 0) // Defence, Strength, Prayer, Attack, Magic, Ranged
        assertEquals(listOf(20, 0, 20, 40, 0, 0), BandosStatDrain.distribute(80, order))
    }

    @Test
    fun `bandos drain stops entirely once every stat is exhausted`() {
        val order = listOf(5, 5)
        assertEquals(listOf(5, 5), BandosStatDrain.distribute(50, order))
    }

    @Test
    fun `bandos drain of zero touches nothing`() {
        assertEquals(listOf(0, 0), BandosStatDrain.distribute(0, listOf(10, 10)))
    }

    @Test
    fun `dragon warhammer reduces player defence by 30 percent, rounded down`() {
        // Wiki: floor(0.30 * 75) = 22.
        assertEquals(22, DragonWarhammerDefenceReduction.playerDrain(75))
        assertEquals(0, DragonWarhammerDefenceReduction.playerDrain(0))
    }

    @Test
    fun `dragon warhammer leaves npc defence at 70 percent, rounded down, and stacks multiplicatively`() {
        // Wiki: 75 -> 53 -> 38 across two successful hits (not 75 -> 52 -> 34, which would be
        // subtracting a fixed 30% of the *original* value each time).
        val first = DragonWarhammerDefenceReduction.npcRemaining(75)
        assertEquals(53, first)
        assertEquals(38, DragonWarhammerDefenceReduction.npcRemaining(first))
    }
}
