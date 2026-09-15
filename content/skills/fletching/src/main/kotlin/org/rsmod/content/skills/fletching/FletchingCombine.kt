package org.rsmod.content.skills.fletching

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.player.stat.slayerLvl
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

class FletchingCombine @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in FletchingRecipes.combining) {
            onOpHeldU(recipe.first, recipe.second) { combine(recipe) }
        }
        onPlayerQueueWithArgs<CombineTask>(QUEUE_COMBINE) { combineTick(it.args) }
    }

    private suspend fun ProtectedAccess.combine(recipe: CombineRecipe) {
        if (player.fletchingLvl < recipe.level) {
            mes("You need a Fletching level of ${recipe.level} to do that.")
            return
        }

        if (player.slayerLvl < recipe.slayerLevel) {
            mes("You need a Slayer level of ${recipe.slayerLevel} to do that.")
            return
        }

        if (available(recipe) <= 0) {
            return
        }

        val entry =
            SkillMultiEntry(
                internal = recipe.output,
                materials = listOf(Material(recipe.first), Material(recipe.second)),
            )
        val config =
            SkillMultiConfig(
                actionType = SkillingActionType.MAKE,
                verb = "make",
                entries = listOf(entry),
            )

        openSkillMulti(config) { selection ->
            weakQueue(QUEUE_COMBINE, 1, CombineTask(recipe, selection.amount, 0))
        }
    }

    private fun ProtectedAccess.combineTick(task: CombineTask) {
        val recipe = task.recipe
        if (player.fletchingLvl < recipe.level || player.slayerLvl < recipe.slayerLevel) {
            resetAnim()
            return
        }

        val remaining = task.amount - task.made
        val batch = minOf(recipe.setSize, remaining, available(recipe))
        if (batch <= 0) {
            resetAnim()
            return
        }

        if (invDel(inv, recipe.first, batch, recipe.second, batch).failure) {
            resetAnim()
            return
        }

        if (invAdd(inv, recipe.output, batch).failure) {
            invAdd(inv, recipe.first, batch)
            invAdd(inv, recipe.second, batch)
            mes("You don't have enough inventory space to do that.")
            resetAnim()
            return
        }

        anim(recipe.anim)
        statAdvance(STAT_FLETCHING, recipe.xp * batch * xpMods.get(player, STAT_FLETCHING))
        spam("You make ${objDisplayName(recipe.output, batch)}.")

        val total = task.made + batch
        if (total < task.amount) {
            weakQueue(QUEUE_COMBINE, recipe.ticks, task.copy(made = total))
        } else {
            resetAnim()
        }
    }

    private fun ProtectedAccess.available(recipe: CombineRecipe): Int =
        minOf(inv.count(recipe.first), inv.count(recipe.second))

    private data class CombineTask(val recipe: CombineRecipe, val amount: Int, val made: Int)
}
