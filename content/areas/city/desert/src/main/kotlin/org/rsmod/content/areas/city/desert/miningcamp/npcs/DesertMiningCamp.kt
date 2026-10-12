package org.rsmod.content.areas.city.desert.miningcamp.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Male_slave
class DesertMiningCamp : PluginScript() {
    override fun ScriptContext.startup() {
        for (slave in SLAVES) {
            onOpNpc1(slave) { it.npc.say("Hey leave me alone, can't you see that I'm busy?") }
        }
    }

    private companion object {
        val SLAVES =
            listOf(
                "npc.mining_slave_male",
                "npc.tourtrap_qip_slave_male_1",
                "npc.tourtrap_qip_mining_slave_male_2",
            )
    }
}
