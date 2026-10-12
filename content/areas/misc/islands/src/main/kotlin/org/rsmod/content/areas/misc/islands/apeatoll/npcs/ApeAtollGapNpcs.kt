package org.rsmod.content.areas.misc.islands.apeatoll.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ApeAtollGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mm2_monkey_archer") { startDialogue(it.npc) { monkeyArcher() } }
        onOpNpc1("npc.mm2_lost") { startDialogue(it.npc) { kineer() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monkey_Archer
    private suspend fun Dialogue.monkeyArcher() {
        chatPlayer(quiz, "Hello?")
        chatNpc(neutral, "Sorry, I am far too busy to speak right now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kineer (Kruk's Dungeon)
    private suspend fun Dialogue.kineer() {
        chatNpc(worried, "I'm lost!")
        chatPlayer(worried, "I'm lost too!")
    }
}
