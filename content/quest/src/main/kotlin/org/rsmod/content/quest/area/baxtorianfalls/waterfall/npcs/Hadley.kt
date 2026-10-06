package org.rsmod.content.quest.area.baxtorianfalls.waterfall.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Book
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Complete
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.HadleyNpc
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.WaterfallQuest.Companion.Started
import org.rsmod.content.quest.area.baxtorianfalls.waterfall.heardOfTreasure
import org.rsmod.game.entity.player.Appearance
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Hadley, the tourist guide in the information centre south of the falls. */
class Hadley @Inject constructor(private val waterfall: WaterfallQuest) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(HadleyNpc) { startDialogue(it.npc) { hadley() } }
    }

    private suspend fun Dialogue.hadley() {
        val stage = waterfall.stage(player)
        val onTheTrail = stage in Started until Complete && player.heardOfTreasure
        chatPlayer(happy, "Hello there.")
        when {
            onTheTrail && player.inv.contains(Book) -> {
                chatNpc(
                    happy,
                    "I hope you're enjoying your stay, there should be lots of useful " +
                        "infomation in that book you've got. Make sure you give it a read.",
                )
            }
            onTheTrail -> {
                chatNpc(
                    happy,
                    "Are you on holiday? If so you've come to the right place. I'm Hadley the " +
                        "tourist guide, anything you need to know just ask me. We have some of " +
                        "the most unspoilt wildlife and scenery in Gielinor.",
                )
                chatNpc(
                    happy,
                    "People come from miles around to fish in the clear lakes or to wander the " +
                        "beautiful hillsides.",
                )
                prettyUnderstatement()
            }
            else -> {
                chatNpc(
                    happy,
                    "Well hello, come in, come in, my name's Hadley, I'm head of tourism here. " +
                        "There's some of the most unspoilt wildlife and scenery in Gielinor " +
                        "here. People come from miles around to fish in the clear lakes or to " +
                        "wander the",
                )
                chatNpc(happy, "beautiful hillsides.")
                prettyUnderstatement()
            }
        }
        if (onTheTrail) treasureTopics() else touristTopics()
    }

    private suspend fun Dialogue.prettyUnderstatement() {
        chatPlayer(neutral, "It is quite pretty.")
        val address = if (player.appearance.bodyType == Appearance.BODY_TYPE_A) "sir" else "lady"
        chatNpc(
            happy,
            "Surely pretty is an understatement, $address. Beautiful, amazing or possibly " +
                "life-changing would be more suitable wording. Have you seen Baxtorian Falls? " +
                "Named after the elven king who was buried beneath them.",
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
                1 -> {
                    chatPlayer(quiz, "What happened to the elven king?")
                    baxtorianStory()
                }
                2 -> {
                    chatPlayer(quiz, "Where else is worth visiting around here?")
                    chatNpc(
                        worried,
                        "Well, there's a wide variety of wildlife, although unfortunately most " +
                            "of it's quite dangerous. Please don't feed the goblins.",
                    )
                    chatPlayer(neutral, "Okay.")
                    glarialMonument()
                }
                3 -> {
                    chatPlayer(angry, "I don't like nature, it gives me a rash!")
                    chatNpc(confused, "That's just silly talk.")
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
                1 -> {
                    chatPlayer(quiz, "Can you tell me what happened to the elven king?")
                    baxtorianStory()
                    chatNpc(
                        neutral,
                        "Anyway, I believe we have a book on him upstairs if you want to learn " +
                            "more.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where else is worth visiting around here?")
                    glarialMonument()
                    chatPlayer(quiz, "Who was Glarial?")
                    chatNpc(
                        sad,
                        "Baxtorian's wife, the only other person who could also enter the " +
                            "waterfall apart from him. She was queen when this land was " +
                            "inhabited by elven kind.",
                    )
                    chatNpc(
                        sad,
                        "Glarial was kidnapped while Baxtorian was away, but they eventually " +
                            "recovered her body and brought her home to rest.",
                    )
                    chatPlayer(sad, "That's sad.")
                    chatNpc(
                        neutral,
                        "True, I believe we have a book on Baxtorian and Glarial upstairs if " +
                            "you want to learn more.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Is there treasure under the waterfall?")
                    chatNpc(
                        laugh,
                        "Ha ha... Another treasure hunter. Well if there is no one's been able " +
                            "to get to it. They've been searching that river for decades, all " +
                            "to no avail.",
                    )
                }
                else -> {
                    goodbye()
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.baxtorianStory() {
        chatNpc(
            sad,
            "Baxtorian? I guess he died a long long time ago, it's quite sad really. After " +
                "leaving his kingdom to deal with invading forces, Baxtorian returned to find " +
                "his wife Glarial had been captured by the enemy.",
        )
        chatNpc(
            sad,
            "This destroyed Baxtorian, after years of searching he became a recluse. He went " +
                "into the secret home he had made for Glarial under the waterfall and sealed " +
                "himself in. To this day, no one has managed to enter.",
        )
    }

    private suspend fun Dialogue.glarialMonument() {
        chatNpc(
            happy,
            "There is a lovely spot for a picnic on the hill to the north east, there's a " +
                "monument to the elven queen Glarial. It really is quite pretty.",
        )
    }

    private suspend fun Dialogue.goodbye() {
        chatPlayer(happy, "Thanks, goodbye.")
        chatNpc(happy, "Enjoy your visit.")
    }
}
