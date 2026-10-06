package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Book
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.HadleyNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.MetHudon
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Hadley, the tourist guide in the information centre south of the falls. */
class Hadley @Inject constructor(private val waterfall: WaterfallQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(HadleyNpc) { startDialogue(it.npc) { hadley() } }
    }

    private suspend fun Dialogue.hadley() {
        val onTheTrail = waterfall.stage(player) >= MetHudon
        chatPlayer(happy, "Hello there.")
        if (onTheTrail && player.inv.contains(Book)) {
            chatNpc(
                happy,
                "I hope you're enjoying your visit. That book you're carrying is full of " +
                    "useful information, so do give it a read.",
            )
        } else {
            welcome()
        }
        if (onTheTrail) treasureTopics() else touristTopics()
    }

    private suspend fun Dialogue.welcome() {
        chatNpc(
            happy,
            "Welcome, welcome! I'm Hadley, the local tourist guide. If you have any " +
                "questions, just ask. This valley has some of the finest unspoilt countryside " +
                "in Gielinor.",
        )
        chatNpc(happy, "Visitors travel for miles to fish our lakes and stroll across our hills.")
        chatPlayer(neutral, "It is rather pretty.")
        chatNpc(
            happy,
            "Pretty? Breathtaking, more like! Have you seen Baxtorian Falls yet? They're " +
                "named after the elf king who lies buried beneath them.",
        )
    }

    private suspend fun Dialogue.touristTopics() {
        while (true) {
            when (
                choice4(
                    "What happened to the elven king?",
                    1,
                    "Where else is worth visiting around here?",
                    2,
                    "I don't like nature, it gives me a rash!",
                    3,
                    "Thanks, goodbye.",
                    4,
                )
            ) {
                1 -> baxtorianStory(mentionBook = false)
                2 -> {
                    chatPlayer(quiz, "Where else is worth visiting around here?")
                    chatNpc(
                        worried,
                        "There's plenty of wildlife, though I'm afraid most of it is rather " +
                            "dangerous. And please don't feed the goblins.",
                    )
                    chatPlayer(neutral, "Right.")
                    glarialMonument()
                }
                3 -> {
                    chatPlayer(angry, "I don't like nature, it gives me a rash!")
                    chatNpc(confused, "Oh, don't be ridiculous.")
                }
                else -> {
                    goodbye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.treasureTopics() {
        while (true) {
            when (
                choice4(
                    "Can you tell me what happened to the elven king?",
                    1,
                    "Where else is worth visiting around here?",
                    2,
                    "Is there treasure under the waterfall?",
                    3,
                    "Thanks, goodbye.",
                    4,
                )
            ) {
                1 -> baxtorianStory(mentionBook = true)
                2 -> {
                    chatPlayer(quiz, "Where else is worth visiting around here?")
                    glarialMonument()
                    chatPlayer(quiz, "Who was Glarial?")
                    chatNpc(
                        sad,
                        "Baxtorian's wife, and the only other person who could enter the " +
                            "waterfall. She was queen back when elves lived in these lands.",
                    )
                    chatNpc(
                        sad,
                        "She was taken while Baxtorian was away at war. Her body was eventually " +
                            "recovered and brought home to be laid to rest.",
                    )
                    chatPlayer(sad, "How sad.")
                    chatNpc(
                        neutral,
                        "It is. There's a book about Baxtorian and Glarial upstairs, if you'd " +
                            "like to read more.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Is there treasure under the waterfall?")
                    chatNpc(
                        laugh,
                        "Ha! Another treasure hunter. If there is, nobody has ever reached it. " +
                            "People have been searching that river for years without finding a " +
                            "thing.",
                    )
                }
                else -> {
                    goodbye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.baxtorianStory(mentionBook: Boolean) {
        chatPlayer(quiz, "What happened to the elven king?")
        chatNpc(
            sad,
            "Baxtorian? He died a very long time ago. It's a sad tale. He left his kingdom to " +
                "drive back an invasion, and came home to find his wife Glarial had been taken " +
                "by the enemy.",
        )
        chatNpc(
            sad,
            "It broke him. After years of searching he withdrew from the world and shut " +
                "himself inside the hidden home he had built for Glarial beneath the waterfall. " +
                "Nobody has managed to get in since.",
        )
        if (mentionBook) {
            chatNpc(
                neutral,
                "Anyway, we keep a book about him upstairs if you'd like to know more.",
            )
        }
    }

    private suspend fun Dialogue.glarialMonument() {
        chatNpc(
            happy,
            "There's a lovely picnic spot on the hill to the north-east, beside a monument to " +
                "the elven queen Glarial. It's very pretty up there.",
        )
    }

    private suspend fun Dialogue.goodbye() {
        chatPlayer(happy, "Thanks, goodbye.")
        chatNpc(happy, "Enjoy your stay.")
    }
}
