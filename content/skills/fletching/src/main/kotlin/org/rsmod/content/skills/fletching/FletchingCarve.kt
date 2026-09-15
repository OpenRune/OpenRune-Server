package org.rsmod.content.skills.fletching

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FletchingCarve @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in FletchingRecipes.carving) {
            onOpHeldU(recipe.tool, recipe.input) { carve(recipe) }
        }
        onPlayerQueueWithArgs<CarveTask>(QUEUE_CARVE) { carveTick(it.args) }
    }

    private suspend fun ProtectedAccess.carve(recipe: CarveRecipe) {
        val unlocked = recipe.products.filter { player.fletchingLvl >= it.level }
        if (unlocked.isEmpty()) {
            val required = recipe.products.minOf(FletchProduct::level)
            mes("You need a Fletching level of $required to do that.")
            return
        }

        if (!inv.contains(recipe.input)) {
            return
        }

        val entries = unlocked.map { SkillMultiEntry(it.output, listOf(Material(recipe.input))) }
        val config =
            SkillMultiConfig(
                actionType = SkillingActionType.CUT,
                verb = "make",
                entries = entries,
            )

        openSkillMulti(config) { selection ->
            val product = unlocked.first { it.output == selection.entry.internal }
            weakQueue(QUEUE_CARVE, 1, CarveTask(recipe, product, selection.amount, 0))
        }
    }

    private fun ProtectedAccess.carveTick(task: CarveTask) {
        val product = task.product
        if (player.fletchingLvl < product.level || !inv.contains(task.recipe.input)) {
            resetAnim()
            return
        }

        if (invDel(inv, task.recipe.input, 1).failure) {
            resetAnim()
            return
        }

        if (invAdd(inv, product.output, product.count).failure) {
            invAdd(inv, task.recipe.input, 1)
            mes("You don't have enough inventory space to do that.")
            resetAnim()
            return
        }

        anim(task.recipe.anim)
        statAdvance(STAT_FLETCHING, product.xp * xpMods.get(player, STAT_FLETCHING))
        spam("${task.recipe.message} ${objDisplayName(product.output, product.count)}.")

        val total = task.made + 1
        if (total < task.amount) {
            weakQueue(QUEUE_CARVE, task.recipe.ticks, task.copy(made = total))
        } else {
            resetAnim()
        }
    }

    private data class CarveTask(
        val recipe: CarveRecipe,
        val product: FletchProduct,
        val amount: Int,
        val made: Int,
    )
}
