package org.rsmod.content.skills.herblore

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreCleaningRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CleaningHerbsEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreDefinitions.cleaningHerbs.forEach { row ->
            onOpHeld1(row.grimyHerb) { startCleanHerb(row) }
        }
        onPlayerQueueWithArgs<CleanHerbTask>("queue.herblore_clean") { processCleanHerbTick(it.args) }
    }

    private suspend fun ProtectedAccess.startCleanHerb(row: HerbloreCleaningRow) {
        if (player.herbloreLvl < row.level) {
            mesbox("You need a Herblore level of ${row.level} to clean this herb.")
            return
        }

        if (!inv.contains(row.grimyHerb.internalName)) {
            return
        }

        if (inv.freeSpace() < 1 && !inv.contains(row.cleanHerb.internalName)) {
            mes("You don't have enough inventory space.")
            return
        }

        weakQueue("queue.herblore_clean", 1, CleanHerbTask(row))
    }

    private suspend fun ProtectedAccess.processCleanHerbTick(task: CleanHerbTask) {
        val row = task.row

        if (player.herbloreLvl < row.level) {
            mesbox("You need a Herblore level of ${row.level} to clean this herb.")
            return
        }

        if (
            !inv.contains(row.grimyHerb.internalName) ||
            (inv.freeSpace() < 1 && !inv.contains(row.cleanHerb.internalName))
        ) {
            return
        }

        if (invDel(inv, row.grimyHerb.internalName, 1).failure) {
            return
        }

        if (invAdd(inv, row.cleanHerb.internalName, 1).failure) {
            invAdd(inv, row.grimyHerb.internalName, 1)
            mes("You don't have enough inventory space.")
            return
        }

        if (row.xp > 0) {
            statAdvance("stat.herblore", row.xp.toDouble())
        }

        if (
            inv.contains(row.grimyHerb.internalName) &&
            (inv.freeSpace() >= 1 || inv.contains(row.cleanHerb.internalName))
        ) {
            weakQueue("queue.herblore_clean", 2, task)
        }
    }

    private data class CleanHerbTask(val row: HerbloreCleaningRow)

}
