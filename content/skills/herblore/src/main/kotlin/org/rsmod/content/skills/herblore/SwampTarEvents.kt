package org.rsmod.content.skills.herblore

import org.rsmod.api.config.Constants
import org.rsmod.api.player.events.skilling.SkillingInput
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreSwampTarRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SwampTarEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.swampTars.forEach { recipe ->
            onOpHeldU(SWAMP_TAR, recipe.herb.internalName) { createSwampTar(recipe) }
        }
        onPlayerQueueWithArgs<SwampTarTask>("queue.herblore_swamp_tar") {
            processSwampTarTick(it.args)
        }
    }

    private suspend fun ProtectedAccess.createSwampTar(recipe: HerbloreSwampTarRow) {
        if (!meetsStatReqs(recipe.statReq)) {
            return
        }

        if (!inv.contains(recipe.herb.internalName) || inv.count(SWAMP_TAR) < SWAMP_TAR_PER_BATCH) {
            mes(Constants.dm_default)
            return
        }

        val hasFinishedTar = inv.contains(recipe.finishedTar.internalName)
        val neededSlots = if (hasFinishedTar) 0 else SWAMP_TAR_PER_BATCH
        if (inv.freeSpace() < neededSlots) {
            mes("You don't have enough inventory space to make this tar.")
            return
        }

        weakQueue("queue.herblore_swamp_tar", 1, SwampTarTask(recipe))
    }

    private suspend fun ProtectedAccess.processSwampTarTick(task: SwampTarTask) {
        val recipe = task.recipe

        if (!meetsStatReqs(recipe.statReq)) {
            resetAnim()
            return
        }

        if (
            !inv.contains(recipe.herb.internalName) ||
            inv.count(SWAMP_TAR) < SWAMP_TAR_PER_BATCH ||
            (inv.freeSpace() < SWAMP_TAR_PER_BATCH && !inv.contains(recipe.finishedTar.internalName))
        ) {
            resetAnim()
            return
        }

        anim("seq.human_herbing_vial")

        val product =
            SkillingProduct(
                player = player,
                skill = "stat.herblore",
                item = recipe.finishedTar.internalName,
                count = SWAMP_TAR_PER_BATCH,
                experience = recipe.xp.toDouble(),
                source = SkillingProductSource.SwampTar(recipe),
                inputs =
                    listOf(
                        SkillingInput(recipe.herb.internalName),
                        SkillingInput(SWAMP_TAR, SWAMP_TAR_PER_BATCH),
                    ),
            )
        when (awardSkillingProduct(product)) {
            SkillingAwardResult.Success -> Unit
            SkillingAwardResult.InventoryFull -> {
                mes("You don't have enough inventory space to make this tar.")
                resetAnim()
                return
            }
            SkillingAwardResult.Cancelled -> {
                resetAnim()
                return
            }
        }
        val herbName = recipe.herb.name.lowercase()
        mes("You add the $herbName to the swamp tar.")

        if (
            inv.contains(recipe.herb.internalName) &&
            inv.count(SWAMP_TAR) >= SWAMP_TAR_PER_BATCH
        ) {
            weakQueue("queue.herblore_swamp_tar", 3, SwampTarTask(recipe))
        } else {
            resetAnim()
        }
    }

    private data class SwampTarTask(val recipe: HerbloreSwampTarRow)

    companion object {
        private const val SWAMP_TAR = "obj.swamp_tar"
        private const val SWAMP_TAR_PER_BATCH = 15
    }
}
