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
            "the H.A.M. Guard",
            pocketOwnerName("H.A.M. Guard", lowercase = true, properName = false),
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

    @Test
    fun `chest trap damage scales with current hitpoints and rounds down`() {
        assertEquals(17, trapDamage(hitpoints = 97, scale = 15, base = 300))
        assertEquals(14, trapDamage(hitpoints = 99, scale = 12, base = 300))
        assertEquals(22, trapDamage(hitpoints = 99, scale = 18, base = 450))
        assertEquals(4, trapDamage(hitpoints = 1, scale = 18, base = 450))
    }

    @Test
    fun `chest loot is named in the plural unless it has none`() {
        assertEquals("law runes", lootNoun("Law rune", 50))
        assertEquals("coal", lootNoun("Coal", 25))
        assertEquals("coins", lootNoun("Coins", 4500))
        assertEquals("prayer potion(2)", lootNoun("Prayer potion(2)", 1))
    }

    private companion object {
        const val POUCH = "obj.pickpocket_coin_pouch_ham"
        const val ORE = "obj.iron_ore"
    }
}
