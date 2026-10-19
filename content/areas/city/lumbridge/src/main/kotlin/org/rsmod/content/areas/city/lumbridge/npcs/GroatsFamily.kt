package org.rsmod.content.areas.city.lumbridge.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.lumbridge.idesofmilk.IdesOfMilkQuest
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GroatsFamily @Inject constructor(private val idesOfMilk: IdesOfMilkQuest) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.favour_seth_groats") { startDialogue(it.npc) { sethGroats() } }
        onOpNpc1("npc.gillie_the_milkmaid") { startDialogue(it.npc) { gillieGroats() } }
    }

    private suspend fun Dialogue.sethGroats() {
        if (with(idesOfMilk) { sethDialogue() }) {
            return
        }
        chatNpc(happy, "M'arnin'....going to milk me cowsies!")
    }

    // TODO(content): [verify:in_game] the lost cowbell amulet option and what grants it
    private suspend fun Dialogue.gillieGroats() {
        if (with(idesOfMilk) { gillieDialogue() }) {
            return
        }
        chatNpc(happy, "Hello, I'm Gillie the Milkmaid. What can I do for you?")
        val choice =
            if (idesOfMilk.canAskGillieAboutCows(player)) {
                choice4(
                    "Who are you?",
                    1,
                    "Can you tell me how to milk a cow?",
                    2,
                    "Can you share what makes your cows so productive?",
                    4,
                    "I'm fine, thanks.",
                    3,
                )
            } else {
                choice3(
                    "Who are you?",
                    1,
                    "Can you tell me how to milk a cow?",
                    2,
                    "I'm fine, thanks.",
                    3,
                )
            }
        when (choice) {
            4 -> with(idesOfMilk) { gillieAboutCows() }
            1 -> {
                chatNpc(
                    happy,
                    "My name is Gillie Groats. My father is a farmer and I milk the cows for him.",
                )
                chatPlayer(quiz, "Do you have any buckets of milk spare?")
                chatNpc(
                    neutral,
                    "I'm afraid not. We need all of our milk to sell to market, but you can milk " +
                        "the cow yourself if you need the milk.",
                )
                chatPlayer(happy, "Thanks.")
            }
            2 -> {
                chatPlayer(quiz, "So how do you get milk from a cow then?")
                chatNpc(
                    happy,
                    "It's very easy. First you need an empty bucket to hold the milk.",
                )
                chatNpc(
                    happy,
                    "Then find a dairy cow to milk - you can't milk just any cow.",
                )
                chatPlayer(quiz, "How do I find a dairy cow?")
                chatNpc(
                    happy,
                    "They are easy to spot - they are dark brown and white, unlike beef cows, " +
                        "which are light brown and white. We also tether them to a post to stop " +
                        "them wandering around all over the place.",
                )
                chatNpc(happy, "There are a couple very near, in this field.")
                chatNpc(
                    happy,
                    "Then just milk the cow and your bucket will fill with tasty, nutritious milk.",
                )
            }
            else -> chatPlayer(neutral, "I'm fine, thanks.")
        }
    }
}
