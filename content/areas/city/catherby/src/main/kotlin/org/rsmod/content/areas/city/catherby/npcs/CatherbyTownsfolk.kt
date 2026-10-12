package org.rsmod.content.areas.city.catherby.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CatherbyTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.caleb_fitzharmon_1op") { startDialogue(it.npc) { caleb() } }
        onOpNpc1("npc.caleb_fitzharmon_2ops") { startDialogue(it.npc) { caleb() } }
        onOpNpc1("npc.current_affairs_councillor") { startDialogue(it.npc) { catherine() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Caleb (before Family Crest)
    // TODO(content): [quest:family_crest] quest dialogue and gauntlet changing after the quest
    // TODO(content): [quest:family_pest] cooking gauntlets after Family Pest
    private suspend fun Dialogue.caleb() {
        chatNpc(confused, "Who are you? What are you after?")
        val cook =
            choice2(
                "Nothing, I will be on my way.",
                false,
                "I see you are a chef, will you cook me anything?",
                true,
            )
        if (!cook) {
            chatPlayer(neutral, "Nothing, I will be on my way.")
            return
        }
        chatPlayer(quiz, "I see you are a chef... Could you cook me something?")
        chatNpc(
            neutral,
            "I would, but I am very busy. I am trying to increase my reknown as one of the " +
                "worlds' leading chefs by preparing a special and unique fish salad.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Councillor_Catherine
    // TODO(content): [quest:current_affairs] quest dialogue
    private suspend fun Dialogue.catherine() {
        chatNpc(
            neutral,
            "Welcome to the Catherby Council Office. Your views are important to us.",
        )
        chatPlayer(neutral, "That's good to hear...")
        chatNpc(
            neutral,
            "Unfortunately, all of our operatives are too busy to help you today. Please try " +
                "again soon.",
        )
        chatPlayer(bored, "Delightful...")
    }
}
