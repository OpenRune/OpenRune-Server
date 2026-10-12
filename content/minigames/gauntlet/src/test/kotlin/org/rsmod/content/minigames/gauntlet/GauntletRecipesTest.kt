package org.rsmod.content.minigames.gauntlet

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GauntletRecipesTest {
    @Test fun `basic weapons always require twenty shards and a frame in either mode`() {
        for (corrupted in listOf(false, true)) for (style in listOf("melee", "ranged", "magic")) {
            val recipe = GauntletRecipes.resolve(corrupted) { false }.single { it.product == gauntletObj("${style}_t1", corrupted) }
            assertEquals(listOf(gauntletObj("crystal_shard", corrupted) to 20, gauntletObj("generic_component", corrupted) to 1), recipe.materials)
        }
    }

    @Test fun `attuned weapons consume the basic weapon and sixty shards`() {
        for (corrupted in listOf(false, true)) for (style in listOf("melee", "ranged", "magic")) {
            val basic = gauntletObj("${style}_t1", corrupted)
            val recipe = GauntletRecipes.resolve(corrupted) { it == basic }.single { it.product == gauntletObj("${style}_t2", corrupted) }
            assertEquals(listOf(gauntletObj("crystal_shard", corrupted) to 60, basic to 1), recipe.materials)
        }
    }

    @Test fun `armour consumes forty sixty eighty shards and the previous tier`() {
        for (corrupted in listOf(false, true)) for (piece in listOf("helmet", "chestplate", "platelegs")) for (tier in 1..3) {
            val previous = gauntletObj("${piece}_t${tier - 1}", corrupted)
            val recipe = GauntletRecipes.resolve(corrupted) { it == previous }.single { it.product == gauntletObj("${piece}_t$tier", corrupted) }
            assertEquals(listOf(40, 60, 80)[tier - 1], recipe.shards)
            if (tier > 1) assertTrue(previous to 1 in recipe.materials)
        }
    }

    @Test fun `mode specific resources never become normal resources in corrupted recipes`() {
        val recipes = GauntletRecipes.resolve(true) { false }
        for (recipe in recipes) for ((obj, _) in recipe.materials) {
            if (obj != "obj.gauntlet_food") assertTrue(obj.endsWith("_hm"), obj)
        }
        assertEquals(40, recipes.single { it.product == "obj.gauntlet_teleport_crystal_hm" }.shards)
    }
}
