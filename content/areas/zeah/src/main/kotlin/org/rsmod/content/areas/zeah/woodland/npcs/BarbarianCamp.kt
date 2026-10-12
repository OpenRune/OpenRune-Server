package org.rsmod.content.areas.zeah.woodland.npcs

import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// https://oldschool.runescape.wiki/w/Transcript:Barbarian_(Barbarian_Camp)
class BarbarianCamp : PluginScript() {
    override fun ScriptContext.startup() {
        for (barbarian in BARBARIANS) {
            onOpNpc1(barbarian) {
                val npc = it.npc
                startDialogue(npc) {
                    chatNpc(angry, TAUNTS[access.random.of(TAUNTS.size)])
                    npc.queueCombatRetaliate(player)
                }
            }
        }
    }

    private companion object {
        val BARBARIANS = (1..5).map { "npc.akd_barbarian_$it" }

        val TAUNTS =
            listOf(
                "What do you want?",
                "You look funny!",
                "Grrr!",
                "Ah, you've come for fight!",
                "Wanna fight?",
            )
    }
}
