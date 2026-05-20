package org.rsmod.content.skills.herblore

import org.rsmod.api.config.Constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreCrushingRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CrushingEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.crushingRecipes.forEach { recipe ->
            onOpHeldU("obj.pestle_and_mortar", recipe.item.internalName) { crushItem(recipe) }
        }
        onPlayerQueueWithArgs<CrushTask>("queue.herblore_crush") { processCrushTick(it.args) }
    }

    private fun ProtectedAccess.crushItem(recipe: HerbloreCrushingRow) {
        if (player.herbloreLvl < recipe.level) {
            mes("You need a Herblore level of ${recipe.level} to crush this item.")
            return
        }

        if (!inv.contains("obj.pestle_and_mortar") || !inv.contains(recipe.item.internalName)) {
            mes(Constants.dm_default)
            return
        }

        if (inv.freeSpace() < 1 && !inv.contains(recipe.crushedItem.internalName)) {
            mes("You don't have enough inventory space.")
            return
        }

        weakQueue("queue.herblore_crush", 1, CrushTask(recipe))
    }

    private suspend fun ProtectedAccess.processCrushTick(task: CrushTask) {
        val recipe = task.recipe

        if (player.herbloreLvl < recipe.level) {
            resetAnim()
            return
        }

        if (
            !inv.contains("obj.pestle_and_mortar") ||
            !inv.contains(recipe.item.internalName) ||
            (inv.freeSpace() < 1 && !inv.contains(recipe.crushedItem.internalName))
        ) {
            resetAnim()
            return
        }

        anim("seq.human_herbing_grind")

        if (invDel(inv, recipe.item.internalName, 1).failure) {
            resetAnim()
            return
        }

        if (invAdd(inv, recipe.crushedItem.internalName, 1).failure) {
            invAdd(inv, recipe.item.internalName, 1)
            mes("You don't have enough inventory space.")
            resetAnim()
            return
        }

        if (recipe.xp > 0) {
            statAdvance("stat.herblore", recipe.xp.toDouble())
        }

        val itemName = recipe.item.name.lowercase()
        mes("You crush the $itemName.")

        if (inv.contains(recipe.item.internalName)) {
            weakQueue("queue.herblore_crush", 3, CrushTask(recipe))
        } else {
            resetAnim()
        }
    }

    private data class CrushTask(val recipe: HerbloreCrushingRow)

}
