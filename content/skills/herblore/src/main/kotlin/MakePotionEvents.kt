package org.rsmod.content.skills.herblore

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.herblore.HerblorePotionsRow
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.content.skills.skillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MakePotionEvents : PluginScript() {

    override fun ScriptContext.startup() {
        HerblorePotionsRow.all().forEach { potion ->
            onOpHeldU(potion.unfinished, potion.secondary) { startMake(potion) }
        }
        onPlayerQueueWithArgs("queue.herblore_make") { processMake(it.args) }
    }

    private suspend fun ProtectedAccess.startMake(potion: HerblorePotionsRow) {
        if (player.herbloreLvl < potion.level) {
            player.mes("You need a Herblore level of ${potion.level} to make this potion.")
            return
        }
        val config = skillMulti {
            entry(potion.result.internalName) {
                material(potion.unfinished.internalName)
                material(potion.secondary.internalName)
            }
        }
        openSkillMulti(config) { selection ->
            weakQueue("queue.herblore_make", 3, MakeTask(potion, selection.amount))
        }
    }

    private fun ProtectedAccess.processMake(task: MakeTask) {
        val potion = task.potion
        if (!inv.contains(potion.unfinished.internalName) || !inv.contains(potion.secondary.internalName)) {
            return
        }
        if (player.herbloreLvl < potion.level) {
            player.mes("You need a Herblore level of ${potion.level} to make this potion.")
            return
        }
        invDel(inv, potion.unfinished.internalName, 1)
        invDel(inv, potion.secondary.internalName, 1)
        invAdd(inv, potion.result.internalName)
        anim("seq.human_herbing_vial")
        statAdvance("stat.herblore", potion.xp / 10.0)
        player.mes("You mix the ${potion.secondary.name} into the potion.")
        val remaining = task.remaining - 1
        if (remaining > 0 &&
            inv.contains(potion.unfinished.internalName) &&
            inv.contains(potion.secondary.internalName)
        ) {
            weakQueue("queue.herblore_make", 3, task.copy(remaining = remaining))
        }
    }

    private data class MakeTask(
        val potion: HerblorePotionsRow,
        val remaining: Int,
    )
}
