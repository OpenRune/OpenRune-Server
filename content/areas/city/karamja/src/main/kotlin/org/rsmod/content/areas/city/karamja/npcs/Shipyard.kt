package org.rsmod.content.areas.city.karamja.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Shipyard : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shipyardworker1") { startDialogue(it.npc) { worker() } }
        onOpNpc1("npc.shipyardworker2") { startDialogue(it.npc) { worker() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shipyard_worker
    private suspend fun Dialogue.worker() {
        when (access.random.of(15)) {
            0 -> {
                chatPlayer(happy, "Hello.")
                chatPlayer(happy, "Quite a few ships you're building!")
                chatNpc(happy, "This is just the start! The completed fleet will be awesome!")
            }
            1 -> {
                chatPlayer(happy, "Hello.")
                chatNpc(angry, "No time to talk we've a fleet to build!")
            }
            2 -> {
                chatPlayer(happy, "Hello.")
                chatNpc(happy, "Hello matey!")
                chatPlayer(quiz, "How are you?")
                chatNpc(sad, "Tired!")
                chatPlayer(neutral, "You shouldn't work so hard!")
            }
            3 -> {
                chatPlayer(happy, "Hello.")
                chatPlayer(happy, "Quite an impressive set up!")
                chatNpc(
                    neutral,
                    "It needs to be. There's no other way to build a fleet of this size!",
                )
            }
            4 -> {
                chatPlayer(happy, "Hello.")
                chatNpc(quiz, "Hello there. I haven't seen you before.")
                chatPlayer(happy, "I'm new!")
                chatNpc(neutral, "Well it's hard work, but the pay is good.")
            }
            5 -> {
                chatPlayer(happy, "Hello!")
                chatPlayer(quiz, "How are you?")
                chatNpc(angry, "Too busy to waste time gossiping!")
                chatPlayer(angry, "Touchy!")
            }
            6 -> {
                chatPlayer(happy, "Hello.")
                chatPlayer(neutral, "Looks like hard work.")
                chatNpc(happy, "I like to keep busy.")
            }
            7 -> {
                chatPlayer(happy, "Hello!")
                chatPlayer(quiz, "So where are you sailing?")
                chatNpc(confused, "What do you mean?")
                chatPlayer(laugh, "Don't worry, just kidding!")
            }
            8 -> {
                chatPlayer(happy, "Hello!")
                chatNpc(quiz, "Hello there, are you too lazy to work as well?")
                chatPlayer(neutral, "Something like that.")
                chatNpc(happy, "I'm just sun bathing!")
            }
            9 -> {
                chatPlayer(happy, "Hello!")
                chatNpc(angry, "No time to talk we've a fleet to build!")
            }
            10 -> {
                chatPlayer(happy, "Hello!")
                chatPlayer(quiz, "What are you building?")
                chatNpc(confused, "Are you serious?")
                chatPlayer(laugh, "Of course not! You're obviously building a boat.")
            }
            11 -> {
                chatPlayer(happy, "Hello!")
                chatNpc(angry, "I'm getting tired of this!")
                chatPlayer(confused, "What?")
                chatNpc(angry, "Breaking my back for pennies! It's just not on!")
            }
            12 -> {
                chatPlayer(happy, "Hello!")
                chatNpc(angry, "What do you want?")
                chatPlayer(angry, "Is that any way to talk to your new superior?")
                chatNpc(worried, "Oh, I'm sorry, I didn't realise!")
            }
            13 -> {
                chatPlayer(happy, "Hello!")
                chatNpc(shocked, "Ouch!")
                chatPlayer(worried, "What's wrong?!")
                chatNpc(sad, "I cut my finger!")
                chatNpc(sad, "Do you have a bandage?")
                chatPlayer(sad, "I'm afraid not.")
                chatNpc(neutral, "That's ok, I'll use my shirt.")
            }
            else -> {
                chatPlayer(happy, "Hello!")
                chatNpc(quiz, "Can I help you?")
                chatPlayer(neutral, "I'm just looking around.")
                chatNpc(
                    neutral,
                    "Well there's plenty of work to be done, so if you don't mind...",
                )
                chatNpc(neutral, "Of course. Sorry to have disturbed you.")
            }
        }
    }
}
