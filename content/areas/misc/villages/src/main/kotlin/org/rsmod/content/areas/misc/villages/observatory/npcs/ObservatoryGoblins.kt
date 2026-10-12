package org.rsmod.content.areas.misc.villages.observatory.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ObservatoryGoblins : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.qip_obs_cooking_goblin") { startDialogue(it.npc) { greasycheeks() } }
        onOpNpc1("npc.qip_obs_drunken_goblin") { startDialogue(it.npc) { smellytoes() } }
        onOpNpc1("npc.qip_obs_goblin_kid") { startDialogue(it.npc) { clothears() } }
        onOpNpc1("npc.qip_obs_goblin_setting_a_fire") { startDialogue(it.npc) { creakyknees() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Greasycheeks
    private suspend fun Dialogue.greasycheeks() {
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "Shush! I'm concentrating.")
        chatPlayer(worried, "Oh, sorry.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Smellytoes
    private suspend fun Dialogue.smellytoes() {
        chatPlayer(happy, "Hi there.")
        chatNpc(drunk, "Hey, ids me matesh!")
        chatPlayer(confused, "Sorry, have we met?")
        chatNpc(drunk, "Yeah! you wazsh wiv me in dat pub overy by hill!")
        chatPlayer(confused, "I have no idea what you're going on about.")
        chatNpc(drunk, "Glad yeeash remembers.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Clothears
    private suspend fun Dialogue.clothears() {
        mesbox("The little goblin appear to be fast asleep, and yet still fishing...")
        chatPlayer(neutral, "I think I'd rather not wake him.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Creakyknees
    private suspend fun Dialogue.creakyknees() {
        chatPlayer(quiz, "Where did you get that lens?")
        chatNpc(neutral, "From that strange metal thing up on the hill.")
        chatPlayer(angry, "You should give that back!")
        chatNpc(quiz, "Even if it's cracked?")
        chatPlayer(neutral, "Ah, well, I suppose it's of no use. But, still.")
    }
}
