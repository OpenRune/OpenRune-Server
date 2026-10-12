package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] which inv Holoy's Trade opens (Hura's is inv.xbows_shop)
class WhiteWolfMountainStation : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.xbows_sales_dwarf_white_wolf_mt") { startDialogue(it.npc) { holoy() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Holoy
    private suspend fun Dialogue.holoy() {
        chatNpc(neutral, "Oh, hello.")
        chatPlayer(quiz, "Hello, what's in those boxes?")
        chatNpc(happy, "Crossbows, are you interested?")
        chatPlayer(quiz, "Maybe, are they any good?")
        chatNpc(angry, "Are they any good?! They're dwarven engineering at its best!")
        val choice =
            choice3(
                "How do I make one for myself?",
                1,
                "What about ammo?",
                2,
                "Thanks for telling me. Bye!",
                3,
            )
        when (choice) {
            1 -> makeOne()
            2 -> {
                chatPlayer(quiz, "What about ammo?")
                chatNpc(
                    neutral,
                    "You can smith yourself lots of different bolts, don't forget to flight them " +
                        "with feathers like you do arrows though. You can poison any untipped " +
                        "bolt but there's also the option of tipping them with gems then",
                )
                chatNpc(
                    neutral,
                    "enchanting them with runes. This can have some pretty powerful effects.",
                )
                chatPlayer(shocked, "Oh my poor bank, how will I store all those?!")
                chatNpc(
                    neutral,
                    "Find Hirko in Keldagrim, he also sells crossbow parts and I'm sure he has " +
                        "something you can use to store bolts in.",
                )
                chatPlayer(happy, "Thanks for the info.")
            }
            else -> bye()
        }
    }

    private suspend fun Dialogue.makeOne() {
        chatPlayer(quiz, "How do I make one for myself?")
        chatNpc(
            neutral,
            "Well, firstly you'll need to chop yourself some wood, then use a knife on the wood " +
                "to whittle out a nice crossbow stock like these here.",
        )
        chatPlayer(neutral, "Wood fletched into stock... check.")
        chatNpc(
            neutral,
            "Then get yourself some metal and a hammer and smith yourself some limbs for the " +
                "bow, mind that you use the right metals and woods though as some wood is too " +
                "light to use with some metal and vice versa.",
        )
        chatPlayer(quiz, "Which goes with which?")
        chatNpc(
            neutral,
            "Wood and Bronze as they're basic materials, Oak and Blurite, Willow and Iron, Steel " +
                "and Teak, Mithril and Maple, Adamantite and Mahogany and finally Runite and Yew.",
        )
        chatPlayer(quiz, "Ok, so I have my stock and a pair of limbs... what now?")
        chatNpc(
            neutral,
            "Simply take a hammer and smack the limbs firmly onto the stock. You'll then need a " +
                "string, only they're not the same as normal bows. You'll need to dry some large " +
                "animal's meat to get sinew, then spin that on a spinning",
        )
        chatNpc(
            neutral,
            "wheel, it's the only thing we've found to be strong enough for a crossbow.",
        )
        if (choice2("What about magic logs?", 1, "Thanks for telling me. Bye!", 2) == 2) {
            bye()
            return
        }
        chatPlayer(quiz, "What about magic logs?")
        chatNpc(
            neutral,
            "Well.. I don't rightly know... us dwarves don't work with magic, we prefer gold and " +
                "rock. Much more stable. I guess you could ask the humans at the rangers guild " +
                "to see if they can do something but I don't want",
        )
        chatNpc(neutral, "anything to do with it!")
        bye()
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Thanks for telling me. Bye!")
        chatNpc(happy, "Take care, straight shooting.")
    }
}
