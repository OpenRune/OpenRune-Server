package org.rsmod.content.skills.runecrafting.essencepouch

import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.skills.runecrafting.essencepouch.CordeliaDialogue.talkToCordelia
import org.rsmod.content.skills.runecrafting.essencepouch.DarkMageDialogue.repairPouchesDirectly
import org.rsmod.content.skills.runecrafting.essencepouch.DarkMageDialogue.talkToDarkMage
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PouchRepairEvents : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.rcu_zammy_mage2") {
            talkToDarkMage(it.npc)
        }

        onOpNpc3("npc.rcu_zammy_mage2") {
            repairPouchesDirectly(it.npc)
        }

        listOf("npc.gotr_cordelia_1op", "npc.gotr_cordelia_2ops").forEach {
            onOpNpc1(it) {
                talkToCordelia(it.npc)
            }
        }

    }
}
