package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LumbridgeTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lost_tribe_sigmund") { startDialogue(it.npc) { sigmund() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sigmund (before Rune Mysteries)
    // TODO(content): [quest:the_lost_tribe] quest dialogue (afterwards he is in the H.A.M. Hideout)
    // TODO(content): [verify:in_game] the wiki only gives the before-Rune Mysteries greeting
    private suspend fun Dialogue.sigmund() {
        chatNpc(neutral, "Can I help you?")
        when (choice2("Do you have any quests for me?", 1, "Who are you?", 2)) {
            1 -> {
                chatPlayer(quiz, "Do you have any quests for me?")
                chatNpc(
                    neutral,
                    "I hear the Duke has a task for an adventurer. Otherwise, if you want to " +
                        "make yourself useful, there are always evil monsters to slay.",
                )
                chatPlayer(neutral, "Okay, I might just do that.")
            }
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "I'm the Duke's advisor.")
                chatPlayer(quiz, "Can you give me any advice then?")
                chatNpc(
                    neutral,
                    "I only advise the Duke. But if you want to make yourself useful, there are " +
                        "evil goblins to slay on the other side of the river.",
                )
            }
        }
    }
}
