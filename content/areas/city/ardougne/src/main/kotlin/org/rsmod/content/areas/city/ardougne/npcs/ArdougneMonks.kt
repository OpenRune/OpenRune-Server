package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Monk (Ardougne Monastery)
// TODO(content): [quest:monks_friend] "*hic* What a party! Wow!" after the quest
class ArdougneMonks : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.monk_ardougne") { it.npc.say("Peace brother") }
    }
}
