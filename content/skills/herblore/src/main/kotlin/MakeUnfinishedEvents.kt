package org.rsmod.content.skills.herblore

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerbloreHerbsRow
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.skillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MakeUnfinishedEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerbloreHerbsRow.all().forEach { herb ->
            onOpHeldU("obj.vial_water", herb.clean) { startMakeUnfinished(herb) }
        }
        onPlayerQueueWithArgs("queue.herblore_unfinished") { processMakeUnfinished(it.args) }
    }

    private suspend fun ProtectedAccess.startMakeUnfinished(herb: HerbloreHerbsRow) {
        val config = skillMulti {
            entry(herb.unfinished.internalName) {
                material("obj.vial_water")
                material(herb.clean.internalName)
            }
        }
        openSkillMulti(config) { selection ->
            weakQueue("queue.herblore_unfinished", 3, UnfinishedTask(herb, selection.amount))
        }
    }

    private fun ProtectedAccess.processMakeUnfinished(task: UnfinishedTask) {
        val herb = task.herb
        if (!inv.contains("obj.vial_water") || !inv.contains(herb.clean.internalName)) {
            return
        }
        invDel(inv, "obj.vial_water", 1)
        invDel(inv, herb.clean.internalName, 1)
        invAdd(inv, herb.unfinished.internalName)
        anim("seq.human_herbing_vial")
        player.mes("You put the ${herb.clean.name} into the vial of water.")
        val remaining = task.remaining - 1
        if (remaining > 0 &&
            inv.contains("obj.vial_water") &&
            inv.contains(herb.clean.internalName)
        ) {
            weakQueue("queue.herblore_unfinished", 3, task.copy(remaining = remaining))
        }
    }

    private data class UnfinishedTask(
        val herb: HerbloreHerbsRow,
        val remaining: Int,
    )
}
