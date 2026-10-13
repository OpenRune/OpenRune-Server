package org.rsmod.content.skills.thieving

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ThievingRulesTest {
    @Test
    fun `common names are lowercased with an article`() {
        assertEquals("the man", pocketOwnerName("Man", lowercase = true, properName = false))
        assertEquals("the warrior", pocketOwnerName("Warrior", true, properName = false))
    }

    @Test
    fun `names with inner capitals keep their casing`() {
        assertEquals(
            "the Al Kharid warrior",
            pocketOwnerName("Al Kharid warrior", lowercase = true, properName = false),
        )
    }

    @Test
    fun `proper names have no article`() {
        val martin = "Martin the Master Gardener"
        assertEquals(martin, pocketOwnerName(martin, lowercase = false, properName = false))
        assertEquals("Miriel", pocketOwnerName("Miriel", lowercase = false, properName = true))
    }

    @Test
    fun `names kept as written still get an article`() {
        assertEquals("the Menaphite Thug", pocketOwnerName("Menaphite Thug", false, false))
    }

    @Test
    fun `doubling an item doubles its count`() {
        assertEquals(listOf(LootDrop(ORE, 6)), LootDrop(ORE, 3).doubled(POUCH) { error("no roll") })
    }

    @Test
    fun `doubling a pouch keeps one pouch and adds a pouch's worth of coins each`() {
        var rolls = 0
        val loot = LootDrop(POUCH, 2).doubled(POUCH) { ++rolls * 10 }
        assertEquals(listOf(LootDrop(POUCH, 2), LootDrop("obj.coins", 30)), loot)
        assertEquals(2, rolls)
    }

    @Test
    fun `targets without a pouch double everything as items`() {
        assertEquals(listOf(LootDrop(POUCH, 2)), LootDrop(POUCH, 1).doubled(null) { 0 })
    }

    private companion object {
        const val POUCH = "obj.pickpocket_coin_pouch_ham"
        const val ORE = "obj.iron_ore"
    }
}
