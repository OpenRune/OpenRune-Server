package org.rsmod.content.areas.misc.outskirts.digsite.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DigsiteGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.digworkman2") {
            startDialogue(it.npc) {
                // https://oldschool.runescape.wiki/w/Transcript:Doug_Deeping
                if (QuestRequirements.hasCompleted(player, DIG_SITE)) {
                    chatNpc(happy, "Well, hello again. Congratulations on your finds.")
                }
                // TODO(content): [quest:the_dig_site] Doug's dialogue during the quest
                // (https://oldschool.runescape.wiki/w/Transcript:The_Dig_Site)
            }
        }
    }

    private companion object {
        const val DIG_SITE = "quest_digsite"
    }
}
