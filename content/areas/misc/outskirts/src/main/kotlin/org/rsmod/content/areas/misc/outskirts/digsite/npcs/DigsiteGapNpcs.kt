package org.rsmod.content.areas.misc.outskirts.digsite.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Doug_Deeping
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_dig_site] Doug's dig is reached through the quest, so the
// after-quest line is the default
class DigsiteGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.digworkman2") {
            startDialogue(it.npc) {
                chatNpc(happy, "Well, hello again. Congratulations on your finds.")
            }
        }
    }
}
