package org.rsmod.content.areas.misc.islands.mosleharmless.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Mac (without a max cape)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:total_level] "S'me cape." branch, the cape's perks and buying the max
//  cape for 2,376,000 coins at total level 2376
// TODO(content): [system:max_cape] selling a max cape back for 80% and replacement hoods
class Mac : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mac") { startDialogue(it.npc) { mac() } }
    }

    private suspend fun Dialogue.mac() {
        chatPlayer(happy, "Hello.")
        mesbox("The man glances at you and grunts something unintelligible.")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "What do you have in your sack?",
                    2,
                    "Why are you so dirty?",
                    3,
                    "Bye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(angry, "Mac. What's it to you?")
                    chatPlayer(neutral, "Only trying to be friendly.")
                }
                2 -> {
                    chatPlayer(quiz, "What do you have in your sack?")
                    chatNpc(angry, "None of yer bis'ness.")
                    chatPlayer(angry, "How rude! I was only being curious.")
                }
                3 -> {
                    chatPlayer(quiz, "Why are you so dirty?")
                    chatNpc(neutral, "Bath XP waste.")
                }
                else -> {
                    chatPlayer(neutral, "Bye.")
                    return
                }
            }
        }
    }
}
