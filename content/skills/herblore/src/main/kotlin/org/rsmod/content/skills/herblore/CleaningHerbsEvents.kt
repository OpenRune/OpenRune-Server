package org.rsmod.content.skills.herblore

import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
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

        val result = player.invTransaction(inv) {
            val inventory = select(inv)
            delete(inventory, row.input.id, 1)
            add(inventory, row.output.id, 1)
        }
        if (result.failure) return

        if (row.xp > 0) {
            statAdvance("stat.herblore", row.xp / 10.0)
        }

        if (inv.contains(row.input.internalName)) {
            weakQueue("queue.herblore_clean", 2, task)
        }
    }

    private data class CleanHerbTask(val row: HerbloreCleaningRow)
}
