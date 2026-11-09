package org.rsmod.content.minigames.gauntlet

import kotlin.math.max
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillMultiSelection
import org.rsmod.content.skills.openSkillMulti

internal suspend fun ProtectedAccess.singCrystals() {
    while (true) {
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
                defaultAmount = 1,
            )
        var selection: SkillMultiSelection? = null
        openSkillMulti(config) { selection = it }
        val selected = selection ?: return
        val recipe = recipes.first { it.product == selected.entry.internal }
        if (!craft(recipe, selected.amount)) return
    }
}

private fun ProtectedAccess.craft(recipe: GauntletRecipe, amount: Int): Boolean {
    repeat(amount) { made ->
        val failure = craftOne(recipe) ?: return@repeat
        if (made == 0) {
            mes(failure)
            return false
        }
        return sang()
    }
    return sang()
}

private fun ProtectedAccess.sang(): Boolean {
    spam("With the help of the crystal bowl, you sing a beautiful song and shape the crystals.")
    return true
}

private fun ProtectedAccess.craftOne(recipe: GauntletRecipe): String? {
    if (recipe.materials.any { (obj, count) -> inv.count(obj) < count }) {
        return "You don't have the materials to make that."
    }
    for ((obj, count) in recipe.materials) {
        invDel(inv, obj, count)
    }
    if (!invAdd(inv, recipe.product, 1).success) {
        for ((obj, count) in recipe.materials) {
            invAdd(inv, obj, count)
        }
        return "You don't have enough inventory space."
    }
    player.addGauntletPoints(GauntletPoints.forCraft(recipe.product))
    if (recipe.xp > 0) {
        statAdvance("stat.crafting", recipe.xp.toDouble())
        statAdvance("stat.smithing", recipe.xp.toDouble())
    }
    return null
}
