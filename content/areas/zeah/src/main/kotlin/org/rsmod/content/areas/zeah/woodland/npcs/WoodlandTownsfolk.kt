package org.rsmod.content.areas.zeah.woodland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WoodlandTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kw_red_guy") { startDialogue(it.npc) { reginald() } }
        onOpNpc1("npc.kw_yellow_guy") { startDialogue(it.npc) { royJr() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reginald
    private suspend fun Dialogue.reginald() {
        when (access.random.of(3)) {
            0 -> {
                chatNpc(bored, "That sounds really boring.")
                chatPlayer(confused, "What does?")
                chatNpc(bored, "Whatever you plan on saying.")
                chatPlayer(sad, "Oh.")
            }
            1 -> {
                chatNpc(happy, "I'll one day be the very best.")
                chatPlayer(quiz, "Oh yeah?")
                chatNpc(happy, "That no was ever was.")
                chatPlayer(confused, "...Uh huh.")
                chatNpc(happy, "To catch th-")
                chatPlayer(neutral, "I'm just going to stop you right there...")
                chatNpc(neutral, "That's probably for the best.")
            }
            else -> {
                chatNpc(quiz, "Who are you?")
                chatPlayer(neutral, "I'm... Me.")
                chatNpc(happy, "Hi me.")
                chatPlayer(happy, "Why, hello you!")
                chatNpc(confused, "...I'm not you.")
                chatPlayer(quiz, "Who are you?")
                chatNpc(neutral, "Me.")
                chatPlayer(happy, "I'm me too!")
                chatNpc(neutral, "But you're not the real you.")
                chatNpc(shocked, "Woah.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Roy_jr.
    private suspend fun Dialogue.royJr() {
        when (access.random.of(3)) {
            0 -> {
                chatNpc(happy, "Hi, I'm Roy jr!")
                chatPlayer(quiz, "Is that Roy over there then?")
                chatNpc(happy, "Nope, that's Red, we're on an adventure!")
                chatPlayer(quiz, "Oh really? Where are you heading?")
                chatNpc(happy, "To the west, to explore the Kebos Lowlands.")
                chatPlayer(worried, "I've heard it's dangerous over there...")
                chatNpc(happy, "Well, my dad has gone that way, he's a knight!")
                chatPlayer(neutral, "I hope you find him, young one. I best be off.")
            }
            1 -> {
                chatNpc(confused, "Something's different.")
                chatPlayer(neutral, "Nope, seems pretty much the same to me.")
                chatNpc(confused, "Hmm...")
            }
            else -> {
                chatNpc(happy, "My favourite colour is green!")
                chatPlayer(laugh, "Great! How uncreative!")
                chatNpc(happy, "It's the colour of my little duck friend.")
                chatPlayer(neutral, "Indeed it is.")
            }
        }
    }
}
