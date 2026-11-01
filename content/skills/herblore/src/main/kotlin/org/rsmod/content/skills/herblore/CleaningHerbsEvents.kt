package org.rsmod.content.skills.herblore

import org.rsmod.api.player.events.skilling.SkillingInput
import org.rsmod.api.player.events.skilling.SkillingProduct
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.skilling.SkillingAwardResult
import org.rsmod.api.player.skilling.awardSkillingProduct
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CleaningHerbsEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.cleaningHerbs.forEach { row ->
            onOpHeld1(row.input) { startCleanHerb(row) }
        }
        onPlayerQueueWithArgs<CleanHerbTask>("queue.herblore_clean") { processCleanHerbTick(it.args) }
    }

    private suspend fun ProtectedAccess.startCleanHerb(row: HerbloreCleaningRow) {
        if (!meetsStatReqs(row.statReq)) {
            return
        }

        if (!inv.contains(row.input.internalName)) {
            return
        }

        weakQueue("queue.herblore_clean", 1, CleanHerbTask(row))
    }

    private suspend fun ProtectedAccess.processCleanHerbTick(task: CleanHerbTask) {
        val row = task.row

        if (!meetsStatReqs(row.statReq)) {
            return
        }

        val product =
            SkillingProduct(
                player = player,
                skill = "stat.herblore",
                item = row.output.internalName,
                experience = row.xp / 10.0,
                source = SkillingProductSource.HerbCleaning(row),
                inputs = listOf(SkillingInput(row.input.internalName)),
            )
        when (awardSkillingProduct(product)) {
            SkillingAwardResult.Success -> Unit
            SkillingAwardResult.InventoryFull -> {
                mes("You don't have enough inventory space.")
                return
            }
            SkillingAwardResult.Cancelled -> return
        }

        if (inv.contains(row.input.internalName)) {
            weakQueue("queue.herblore_clean", 2, task)
        }
    }

    private data class CleanHerbTask(val row: HerbloreCleaningRow)
}
