package org.rsmod.content.skills.smithing

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.smithing.SmithingDragonForgeRow
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.SkillingActionType
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.smithing.util.SmithingUtils
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DragonForgeScript : PluginScript() {

    private val forgeRecipes = SmithingDragonForgeRow.all()
    private val recipesByOutput = forgeRecipes.associateBy { it.output.internalName }

    override fun ScriptContext.startup() {
        onOpLoc1("loc.ds2_ac_forge_anvil") { openDragonForge() }

        onPlayerQueueWithArgs<DragonForgeTask>("queue.smithing_dragon_forge") {
            processForgeTask(it.args)
        }
    }

    private suspend fun ProtectedAccess.openDragonForge() {
        val available = forgeRecipes.filter { player.smithingLvl >= it.statReq.first().t1 }
        if (available.isEmpty()) {
            return
        }

        openSkillMulti(
            SkillMultiConfig(
                verb = "make",
                actionType = SkillingActionType.SMELT,
                entries = available.map(::toEntry),
                maxCountProvider = { inventory, entry ->
                    recipesByOutput[entry.internal]
                        ?.let { maxSmeltCount(inventory, it) }
                        ?: 0
                },
            ),
        ) { selection ->
            val recipe = recipesByOutput[selection.entry.internal] ?: return@openSkillMulti
            startSmelting(recipe, selection.amount)
        }
    }

    private fun toEntry(recipe: SmithingDragonForgeRow): SkillMultiEntry =
        SkillMultiEntry(
            recipe.output.internalName,
            recipe.input.map { Material(it.internalName, recipe.inputAmount.first()) },
        )

    private suspend fun ProtectedAccess.startSmelting(
        recipe: SmithingDragonForgeRow,
        requestedAmount: Int,
    ) {
        val amount = minOf(requestedAmount, maxSmeltCount(inv, recipe))
        if (amount <= 0) {
            return
        }

        queueNext(recipe, amount, completed = 0)
    }

    private suspend fun ProtectedAccess.processForgeTask(task: DragonForgeTask) {
        if (!canSmelt(task.recipe)) {
            resetAnim()
            return
        }

        performSmelt(task.recipe)

        val completed = task.completed + 1
        if (completed >= task.amount || !canSmelt(task.recipe)) {
            return
        }


        queueNext(task.recipe, task.amount, completed)
    }

    private fun ProtectedAccess.queueNext(
        recipe: SmithingDragonForgeRow,
        amount: Int,
        completed: Int,
    ) {
        weakQueue(
            "queue.smithing_dragon_forge",
            if (completed == 0) 1 else 5,
            DragonForgeTask(recipe, amount, completed),
        )
    }

    private suspend fun ProtectedAccess.performSmelt(recipe: SmithingDragonForgeRow) {
        anim("seq.human_furnace")
        soundSynth("synth.furnace")
        delay(2)

        val removed =
            recipe.input.all { input ->
                invDel(inv, input.internalName, recipe.inputAmount.first()).success
            }

        if (!removed) {
            return
        }

        if (invAdd(inv, recipe.output.internalName, recipe.outputAmount).success) {
            statAdvance("stat.smithing", recipe.xp.toDouble())
            val inputName = SmithingUtils.itemName(recipe.input.first(), "ore")
            mes("You smelt the $inputName in the furnace.")
        }
    }

    private suspend fun ProtectedAccess.canSmelt(recipe: SmithingDragonForgeRow): Boolean {
        val missing =
            recipe.input.mapNotNull { input ->
                val need = recipe.inputAmount.first() - inv.count(input.internalName)
                if (need > 0) {
                    SmithingUtils.itemName(input) to need
                } else {
                    null
                }
            }

        if (missing.isNotEmpty()) {
            val missingText =
                missing.joinToString(", ") { (name, amount) ->
                    "${SmithingUtils.countLiteral(amount)} ${name.lowercase()}"
                }
            val outputName = SmithingUtils.itemName(recipe.output, "bar")
            mesbox("You need $missingText to make ${SmithingUtils.prefixAn(outputName)}.")
            return false
        }

        return SmithingUtils.requireSmithingLevel(
            this,
            recipe.statReq.first().t1,
            "smelt ${SmithingUtils.itemName(recipe.input.first(), "ore")}",
        )
    }

    private fun maxSmeltCount(inventory: Inventory, recipe: SmithingDragonForgeRow): Int =
        recipe.input.minOfOrNull { input ->
            inventory.count(input.internalName) / recipe.inputAmount.first()
        } ?: 0

    private data class DragonForgeTask(
        val recipe: SmithingDragonForgeRow,
        val amount: Int,
        val completed: Int,
    )

}
