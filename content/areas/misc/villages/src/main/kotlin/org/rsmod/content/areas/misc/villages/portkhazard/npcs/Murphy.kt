package org.rsmod.content.areas.misc.villages.portkhazard.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Murphy (first time speaking to Murphy)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:met_flags] "Hello again Murphy." subsequent conversations
// TODO(content): [system:treasure_trails] Murphy hands out a sextant on later visits
// TODO(content): [quest:recipe_for_disaster] "Go Diving." after Freeing Pirate Pete
class Murphy : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.murphy") { startDialogue(it.npc) { murphy() } }
    }

    private suspend fun Dialogue.murphy() {
        chatPlayer(happy, "Good day to you Sir.")
        chatNpc(happy, "Well hello my brave adventurer.")
        chatPlayer(quiz, "What are you up to?")
        chatNpc(happy, "Getting ready to go fishing of course. There's no time to waste!")
        chatNpc(
            neutral,
            "I've got all the supplies I need from the shop at the end of the pier. They sell " +
                "good rope, hammers and axes although their bailing buckets aren't too effective.",
        )
        while (true) {
            val option =
                choice3(
                    "What fish do you catch?",
                    1,
                    "Your boat doesn't look too safe.",
                    2,
                    "Could I help?",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What fish do you catch?")
                    chatNpc(
                        happy,
                        "I get all sorts, anything that lies on the sea bed, you never know what " +
                            "you're going to get until you pull up the net!",
                    )
                }
                2 -> {
                    chatPlayer(worried, "Your boat doesn't look too safe.")
                    chatNpc(neutral, "That's because it's not, the darn thing's full of holes.")
                    chatPlayer(quiz, "Oh, so I suppose you can't go out for a while?")
                    chatNpc(
                        laugh,
                        "Oh no, I don't let a few holes stop an experienced sailor like me. I " +
                            "could sail these seas in a barrel, I'll be going out soon enough.",
                    )
                }
                3 -> {
                    couldIHelp()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.couldIHelp() {
        chatPlayer(quiz, "Could I help?")
        chatNpc(
            happy,
            "Well of course you can! I'll warn you though, the seas are merciless and without " +
                "fishing experience you won't catch much.",
        )
        chatNpc(neutral, "You need a fishing level of 15 or above to catch any fish on the trawler.")
        chatNpc(neutral, "On occasions the net rips, so you'll need some rope to repair it.")
        chatNpc(
            neutral,
            "Repairing the net is difficult in the harsh conditions so you'll find it easier with " +
                "a higher crafting level.",
        )
        chatPlayer(confused, "Right...ok.")
        chatNpc(neutral, "There's also a slight problem with leaks.")
        chatPlayer(shocked, "Leaks?!")
        chatNpc(neutral, "Nothing some swamp paste won't fix...")
        chatPlayer(confused, "Swamp paste?")
        chatNpc(neutral, "Also, there's the small matter of the Kraken.")
        chatPlayer(shocked, "You have got to be kidding me?")
        chatNpc(
            neutral,
            "Occupational hazard. I usually whack it with an axe a few time and fix the damage " +
                "with hammer once it's gone.",
        )
        chatNpc(laugh, "Oh, and one more thing... I hope you're a good swimmer?")
        val option =
            choice3(
                "Actually, I think I'll leave it.",
                1,
                "I'll be fine, let's go.",
                2,
                "What's swamp paste?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(worried, "Actually, I think I'll leave it.")
                chatNpc(angry, "Bloomin' land lovers!!!")
            }
            2 -> {
                chatPlayer(happy, "I'll be fine, let's go.")
                // TODO(content): [system:fishing_trawler] trawler at sea and boarding the trawler
                chatNpc(
                    happy,
                    "Aye aye! Meet me on board the trawler. I just need to get a few things " +
                        "together.",
                )
            }
            3 -> {
                chatPlayer(quiz, "What's swamp paste?")
                chatNpc(neutral, "Swamp tar mixed with flour which is then heated over a fire.")
                chatPlayer(quiz, "Where can I find swamp tar?")
                chatNpc(
                    neutral,
                    "Unfortunately the only supply of swamp tar is in the swamps below Lumbridge.",
                )
            }
        }
    }
}
