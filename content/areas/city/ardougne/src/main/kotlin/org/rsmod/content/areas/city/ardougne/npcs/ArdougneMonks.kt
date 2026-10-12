package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Monk#Ardougne_Monastery
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ArdougneMonks : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.monk_ardougne") {
            if (QuestRequirements.hasCompleted(player, MONKS_FRIEND)) {
                startDialogue(it.npc) { chatNpc(drunk, "*hic* What a party! Wow!") }
            } else {
                it.npc.say("Peace brother")
            }
        }
    }

    private companion object {
        const val MONKS_FRIEND = "quest_monksfriend"
    }
}
