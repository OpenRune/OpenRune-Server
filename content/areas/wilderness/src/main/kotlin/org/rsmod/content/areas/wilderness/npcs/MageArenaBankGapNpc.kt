package org.rsmod.content.areas.wilderness.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MageArenaBankGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.arenamage1") { startDialogue(it.npc) { kolodion() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kolodion (after Mage Arena I)
    // TODO(content): [verify:in_game] the after-Mage Arena I dialogue is used since the bank is
    //  reached after the miniquest
    // TODO(content): [system:god_spell_casts] Kolodion's progress lines for each god spell after
    //  "How am I doing so far?" and the mastered-spells check
    // TODO(content): [miniquest:mage_arena_ii] the 75 Magic check and the challenge once every god
    //  spell is mastered
    private suspend fun Dialogue.kolodion() {
        chatPlayer(happy, "Hello, Kolodion.")
        chatNpc(happy, "Hey there, how are you? Are you enjoying the bloodshed?")
        val option =
            choice3(
                "I think I've had enough for now.",
                1,
                "How can I use my new spells outside of the arena?",
                2,
                "Are there any more challenges available?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I think I've had enough for now.")
                chatNpc(neutral, "A shame. You're a good battle mage. I hope to see you soon.")
            }
            2 -> {
                chatPlayer(quiz, "How can I use my new spells outside of the arena?")
                chatNpc(
                    neutral,
                    "Experience, my friend, experience. Once you've used the spell enough times " +
                        "in the arena, you'll be able to use them in the rest of Gielinor.",
                )
                chatPlayer(happy, "Good stuff.")
                chatNpc(laugh, "Not so good for the citizens; they won't stand a chance.")
                chatPlayer(quiz, "How am I doing so far?")
            }
            else -> {
                chatPlayer(quiz, "Are there any more challenges available?")
                chatNpc(
                    neutral,
                    "You still need to learn how to cast all god spells outside of the arena.",
                )
                chatNpc(
                    neutral,
                    "I've made a note of the spells you still need to learn in your journal.",
                )
            }
        }
    }
}
