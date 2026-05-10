package org.rsmod.content.skills.cooking

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.cooking.CookingAlesRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import kotlin.random.Random

class BrewingEvents @Inject constructor() : PluginScript() {

    private val ales = CookingAlesRow.all()

    data class Brewery(
        val vatVarbit: String,
        val barrelVarbit: String,
    )

    private val breweries = listOf(
        Brewery("varbit.brewing_vat_varbit_1", "varbit.brewing_barrel_varbit_1"),
        Brewery("varbit.brewing_vat_varbit_2", "varbit.brewing_barrel_varbit_2"),
        Brewery("varbit.brewing_vat_varbit_3", "varbit.brewing_barrel_varbit_3"),
        Brewery("varbit.brewing_vat_varbit_4", "varbit.brewing_barrel_varbit_4"),
    )

    private val vatChildLocs = buildList {
        addAll(listOf("loc.vat_empty", "loc.vat_water", "loc.vat_bad_ale", "loc.vat_bad_cider", "loc.vat_barley"))
        val aleNames = listOf(
            "dwarven_stout", "asgarnian_ale", "greenmans_ale", "wizards_mind_bomb",
            "dragon_bitter", "moonlight_mead", "axemans_folly", "chefs_delight",
            "slayers_respite", "cider",
        )
        aleNames.forEach { ale ->
            add("loc.vat_${ale}_hops")
            add("loc.vat_${ale}_brewing_1")
            add("loc.vat_${ale}_brewing_2")
            add("loc.vat_${ale}_normal")
            add("loc.vat_${ale}_mature")
        }
    }

    private val barrelChildLocs = buildList {
        add("loc.barrel_empty")
        add("loc.barrel_unfinished_ale")
        add("loc.barrel_bad_ale")
        add("loc.barrel_bad_cider")
        val aleNames = listOf(
            "dwarven_stout", "asgarnian_ale", "greenmans_ale", "wizards_mind_bomb",
            "dragon_bitter", "moonlight_mead", "axemans_folly", "chefs_delight",
            "slayers_respite", "cider",
        )
        aleNames.forEach { ale ->
            add("loc.barrel_$ale")
            add("loc.barrel_${ale}_mature")
        }
    }

    private val valveLocs = listOf(
        "loc.vat_valve_1",
        "loc.vat_valve_2",
        "loc.vat_valve_3",
    )

    private val valveBaseToBrewery = mapOf(23936 to 0, 23937 to 1, 55339 to 2)
    private val vatBaseToBrewery = mapOf(11670 to 0, 24956 to 1, 55338 to 2)
    private val barrelBaseToBrewery = mapOf(24957 to 0, 20795 to 1, 55340 to 2)

    private fun getBreweryFromVat(baseLocId: Int) = breweries[vatBaseToBrewery[baseLocId] ?: 0]
    private fun getBreweryFromBarrel(baseLocId: Int) = breweries[barrelBaseToBrewery[baseLocId] ?: 0]

    override fun ScriptContext.startup() {
        vatChildLocs.forEach { vat ->
            onOpLoc1(vat) { handleVatClick(it.loc.id) }
            onOpLocU(vat) { handleVatItemUse(it.loc.id, it.objType.internalName) }
        }

        valveLocs.forEach { valve ->
            onOpLoc1(valve) { turnValve(it.loc.id) }
        }

        barrelChildLocs.forEach { barrel ->
            onOpLoc1(barrel) { collectBeer(it.loc.id) }
            onOpLocU(barrel, "obj.beer_glass") { collectBeer(it.loc.id) }
        }

        onPlayerQueueWithArgs<BrewFermentTask>("queue.brewing_ferment") { ferment(it.args) }
    }

    private suspend fun ProtectedAccess.handleVatItemUse(baseLocId: Int, obj: String) {
        when (obj) {
            "obj.bucket_water" -> addWater(baseLocId)
            "obj.barley_malt" -> addMalt(baseLocId)
            "obj.brew_hyper_yeast" -> addTheStuff(baseLocId)
            "obj.ale_yeast" -> addYeast(baseLocId)
            else -> {
                if (ales.any { it.ingredient.internalName == obj }) {
                    addHopsForIngredient(obj, baseLocId)
                } else {
                    mes("Nothing interesting happens.")
                }
            }
        }
    }

    private suspend fun ProtectedAccess.handleVatClick(baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        val state = vars[brewery.vatVarbit]

        when {
            state == 0 -> {
                if (inv.count("obj.bucket_water") >= 2) addWater(baseLocId)
                else mes("The vat is empty. You need 2 buckets of water to start.")
            }
            state == 1 -> {
                if (inv.count("obj.barley_malt") >= 2) addMalt(baseLocId)
                else mes("The vat has water. You need 2 barley malt.")
            }
            state == 2 -> {
                val recipe = findRecipeInInventory()
                if (recipe != null) addHops(recipe, baseLocId)
                else mes("The vat has water and malt. Add your hops or ingredient.")
            }
            ales.any { it.vatOffset == state } -> {
                if (inv.contains("obj.ale_yeast")) addYeast(baseLocId)
                else mes("The vat has the ingredients. Add ale yeast to begin brewing.")
            }
            ales.any { it.vatOffset + 1 == state } -> {
                mes("The vat is ready. Turn the valve to transfer to the barrel.")
            }
            else -> mes("The vat is in use.")
        }
    }

    private suspend fun ProtectedAccess.findRecipeInInventory(): CookingAlesRow? {
        val available = ales.filter {
            inv.count(it.ingredient.internalName) >= it.ingredientCount && player.cookingLvl >= it.level
        }
        if (available.isEmpty()) return null
        if (available.size == 1) return available.first()

        var chosen: CookingAlesRow? = null
        openSkillMulti(SkillMultiConfig(
            verb = "brew",
            entries = available.map { SkillMultiEntry(it.result.internalName, listOf(Material(it.ingredient.internalName, it.ingredientCount))) },
        )) { selection ->
            chosen = available.firstOrNull { it.result.internalName == selection.entry.internal }
        }
        return chosen
    }

    private suspend fun ProtectedAccess.addHopsForIngredient(ingredient: String, baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        if (vars[brewery.vatVarbit] != 2) {
            mes("The vat isn't ready for ingredients yet.")
            return
        }
        val matching = ales.filter { it.ingredient.internalName == ingredient && player.cookingLvl >= it.level }
        if (matching.isEmpty()) {
            mes("You don't have the Cooking level to brew anything with that.")
            return
        }
        val recipe = if (matching.size == 1) {
            matching.first()
        } else {
            var chosen: CookingAlesRow? = null
            openSkillMulti(SkillMultiConfig(
                verb = "brew",
                entries = matching.map { SkillMultiEntry(it.result.internalName, listOf(Material(it.ingredient.internalName, it.ingredientCount))) },
            )) { selection ->
                chosen = matching.firstOrNull { it.result.internalName == selection.entry.internal }
            }
            chosen ?: return
        }
        if (inv.count(recipe.ingredient.internalName) < recipe.ingredientCount) {
            mes("You need ${recipe.ingredientCount} of that ingredient.")
            return
        }
        addHops(recipe, baseLocId)
    }

    private fun ProtectedAccess.addWater(baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        if (vars[brewery.vatVarbit] != 0) {
            mes("The vat already has something in it.")
            return
        }
        if (inv.count("obj.bucket_water") < 2) {
            mes("You need 2 buckets of water.")
            return
        }
        invDel(inv, "obj.bucket_water", 2)
        invAdd(inv, "obj.bucket_empty", 2)
        vars[brewery.vatVarbit] = 1
        mes("You add the water to the vat.")
    }

    private fun ProtectedAccess.addMalt(baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        if (vars[brewery.vatVarbit] != 1) {
            mes("You need to add water first.")
            return
        }
        if (inv.count("obj.barley_malt") < 2) {
            mes("You need 2 barley malt.")
            return
        }
        invDel(inv, "obj.barley_malt", 2)
        vars[brewery.vatVarbit] = 2
        mes("You add the barley malt to the vat.")
    }

    private fun ProtectedAccess.addHops(recipe: CookingAlesRow, baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        invDel(inv, recipe.ingredient.internalName, recipe.ingredientCount)
        vars[brewery.vatVarbit] = recipe.vatOffset
        mes("You add the ingredient to the vat.")
    }

    private fun ProtectedAccess.addTheStuff(baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        val state = vars[brewery.vatVarbit]
        val hasIngredients = state == 2 || ales.any { it.vatOffset == state }
        if (!hasIngredients) {
            mes("There's nothing to add this to.")
            return
        }
        invDel(inv, "obj.brew_hyper_yeast", 1)
        mes("You add the secret ingredient to the vat.")
    }

    private fun ProtectedAccess.addYeast(baseLocId: Int) {
        val brewery = getBreweryFromVat(baseLocId)
        val state = vars[brewery.vatVarbit]
        val recipe = ales.firstOrNull { it.vatOffset == state }
        if (recipe == null) {
            mes("The vat isn't ready for yeast. Add your ingredient first.")
            return
        }
        invDel(inv, "obj.ale_yeast", 1)
        vars[brewery.vatVarbit] = recipe.vatOffset + 1
        mes("You add the ale yeast. Now turn the valve to transfer to the barrel.")
    }

    private fun ProtectedAccess.turnValve(baseLocId: Int) {
        val breweryIndex = valveBaseToBrewery[baseLocId] ?: 0
        val brewery = breweries[breweryIndex]
        val vatState = vars[brewery.vatVarbit]

        val recipe = ales.firstOrNull { it.vatOffset + 1 == vatState }
        if (recipe == null) {
            mes("The vat isn't ready to transfer yet.")
            return
        }
        if (vars[brewery.barrelVarbit] != 0) {
            mes("The barrel is still in use. Collect the beer first.")
            return
        }

        vars[brewery.vatVarbit] = 0
        vars[brewery.barrelVarbit] = 2
        mes("You turn the valve and the brew transfers to the barrel to ferment.")

        weakQueue("queue.brewing_ferment", 50, BrewFermentTask(ales.indexOf(recipe), breweryIndex))
    }

    private fun ProtectedAccess.ferment(task: BrewFermentTask) {
        val brewery = breweries[task.breweryIndex]
        val recipe = ales.getOrNull(task.recipeIndex) ?: return

        if (vars[brewery.barrelVarbit] != 2) return

        val mature = Random.nextInt(100) < 5
        vars[brewery.barrelVarbit] = if (mature) recipe.barrelOffset + 1 else recipe.barrelOffset
    }

    private fun ProtectedAccess.collectBeer(baseLocId: Int) {
        val brewery = getBreweryFromBarrel(baseLocId)
        val state = vars[brewery.barrelVarbit]

        if (state == 0) {
            mes("The barrel is empty.")
            return
        }
        if (state == 2) {
            mes("The barrel is still fermenting. Come back later.")
            return
        }

        val normal = ales.firstOrNull { it.barrelOffset == state }
        val mature = ales.firstOrNull { it.barrelOffset + 1 == state }
        val recipe = normal ?: mature

        if (recipe == null) {
            mes("The barrel is empty.")
            return
        }
        if (!inv.contains("obj.beer_glass")) {
            mes("You need a beer glass to collect the ale.")
            return
        }

        val result = if (mature != null) recipe.matureResult.internalName else recipe.result.internalName

        invDel(inv, "obj.beer_glass", 1)
        invAdd(inv, result, 1)
        statAdvance("stat.cooking", recipe.xp.toDouble())
        vars[brewery.barrelVarbit] = 0
        mes("You collect the ale from the barrel.")
    }

    data class BrewFermentTask(
        val recipeIndex: Int,
        val breweryIndex: Int,
    )
}
