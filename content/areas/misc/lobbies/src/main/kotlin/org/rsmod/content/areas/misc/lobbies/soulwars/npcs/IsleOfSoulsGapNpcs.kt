package org.rsmod.content.areas.misc.lobbies.soulwars.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class IsleOfSoulsGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sw_adventurer") { startDialogue(it.npc) { garl() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garl
    private suspend fun Dialogue.garl() {
        chatNpc(happy, "Hello there. Don't see many people around here. The name's Garl.")
        chatPlayer(quiz, "Hey. What are you doing here?")
        chatNpc(
            neutral,
            "Exploring. Until that Nomad chap opened things up, this island was abandoned. Who " +
                "knows what could be hiding here.",
        )
        chatPlayer(quiz, "Found anything good so far?")
        chatNpc(
            neutral,
            "Well there's a dungeon on the east side of the island. I haven't gone too far in " +
                "though. There's also a tower to the north west, near my camp.",
        )
        chatNpc(
            neutral,
            "There's no shortage of resources either. Though there's not really any way to " +
                "process them. I tried to build a spinning wheel and a pottery wheel myself, but " +
                "they broke.",
        )
        chatPlayer(
            happy,
            "You could really live off the land here. I can think of worse places to do so.",
        )
        chatNpc(neutral, "Indeed. It's all very peaceful. And yet...")
        chatPlayer(quiz, "What is it?")
        chatNpc(
            worried,
            "I don't know. Something just seems amiss. There's something sinister about this " +
                "place. I just don't really know what.",
        )
        chatPlayer(neutral, "I'm sure it's nothing.")
        chatNpc(neutral, "You're probably right. Anyway, I'll let you get on. All the best.")
    }
}
