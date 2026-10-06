package org.rsmod.content.minigames.gauntlet

import kotlin.math.max
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti

internal suspend fun ProtectedAccess.singCrystals() {
    val recipes = GauntletRecipes.resolve(player.gauntletCorrupted) { it in inv }
    val entries =
        recipes.map { recipe ->
            SkillMultiEntry(
                recipe.product,
                recipe.materials.map { (obj, count) -> Material(obj, count) },
            )
        }
    val config =
        SkillMultiConfig(
            verb = "make",
            entries = entries,
            maxCountProvider = { inventory, entry -> max(1, entry.maxCount(inventory)) },
        )
    openSkillMulti(config) { selection ->
        val recipe = recipes.first { it.product == selection.entry.internal }
        repeat(selection.amount) {
            if (!craft(recipe)) return@openSkillMulti
            delay(2)
        }
    }
}

private fun ProtectedAccess.craft(recipe: GauntletRecipe): Boolean {
    if (recipe.materials.any { (obj, count) -> inv.count(obj) < count }) {
        mes("You don't have the materials to make that.")
        return false
    }
    for ((obj, count) in recipe.materials) {
        invDel(inv, obj, count)
    }
    if (!invAdd(inv, recipe.product, 1).success) {
        for ((obj, count) in recipe.materials) {
            invAdd(inv, obj, count)
        }
        mes("You don't have enough inventory space.")
        return false
    }
    if (recipe.xp > 0) {
        statAdvance("stat.crafting", recipe.xp.toDouble())
        statAdvance("stat.smithing", recipe.xp.toDouble())
    }
    spam("With the help of the crystal bowl, you sing a beautiful song and shape the crystals.")
    return true
}
