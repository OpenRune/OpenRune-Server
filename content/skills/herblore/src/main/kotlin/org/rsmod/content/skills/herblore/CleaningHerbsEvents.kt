package org.rsmod.content.skills.herblore

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

        if (inv.freeSpace() < 1 && !inv.contains(row.output.internalName)) {
            mes("You don't have enough inventory space.")
            return
        }

        weakQueue("queue.herblore_clean", 1, CleanHerbTask(row))
    }

    private suspend fun ProtectedAccess.processCleanHerbTick(task: CleanHerbTask) {
        val row = task.row

        if (!meetsStatReqs(row.statReq)) {
            return
        }

        if (
            !inv.contains(row.input.internalName) ||
            (inv.freeSpace() < 1 && !inv.contains(row.output.internalName))
        ) {
            return
        }

        if (invDel(inv, row.input.internalName, 1).failure) {
            return
        }

        if (invAdd(inv, row.output.internalName, 1).failure) {
            invAdd(inv, row.input.internalName, 1)
            mes("You don't have enough inventory space.")
            return
        }

        if (row.xp > 0) {
            statAdvance("stat.herblore", row.xp.toDouble())
        }

        if (
            inv.contains(row.input.internalName) &&
            (inv.freeSpace() >= 1 || inv.contains(row.output.internalName))
        ) {
            weakQueue("queue.herblore_clean", 2, task)
        }
    }

    private data class CleanHerbTask(val row: HerbloreCleaningRow)

}
