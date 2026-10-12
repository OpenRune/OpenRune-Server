package org.rsmod.content.areas.city.taverley.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TaverleyDruids : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kaqemeex") { startDialogue(it.npc) { kaqemeex() } }
        onOpNpc1("npc.sanfew") { startDialogue(it.npc) { sanfew() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kaqemeex (before Druidic Ritual)
    // TODO(content): [quest:druidic_ritual] after-quest Herblore talk and the Herblore skillcape
    private suspend fun Dialogue.kaqemeex() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "What brings you to our holy monument?")
        val option =
            choice3(
                "Who are you?",
                1,
                "I'm in search of a quest.",
                2,
                "Did you build this?",
                3,
            )
        when (option) {
            1 -> kaqemeexWhoAreYou()
            2 -> kaqemeexQuest()
            3 -> kaqemeexBuilt()
        }
    }

    private suspend fun Dialogue.kaqemeexWhoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "We are the druids of Guthix. We worship our god at our famous stone circles. You " +
                "will find them located throughout these lands.",
        )
        val option =
            choice3(
                "What about the stone circle full of dark wizards?",
                1,
                "So what's so good about Guthix?",
                2,
                "Well, I'll be on my way now.",
                3,
            )
        when (option) {
            1 -> kaqemeexDarkWizards()
            2 -> {
                chatPlayer(quiz, "So what's so good about Guthix?")
                chatNpc(
                    neutral,
                    "Guthix is the oldest and most powerful god in Gielinor. His existence is " +
                        "vital to this world. He is the god of balance, and nature; he is also a " +
                        "very part of this world.",
                )
                chatNpc(
                    neutral,
                    "He exists in the trees, and the flowers, the water and the rocks. He is " +
                        "everywhere. His purpose is to ensure balance in everything in this " +
                        "world, and as such we worship him.",
                )
                chatPlayer(bored, "He sounds kind of boring...")
                chatNpc(
                    neutral,
                    "Some day when your mind achieves enlightenment you will see the true beauty " +
                        "of his power.",
                )
            }
            3 -> kaqemeexLeave()
        }
    }

    private suspend fun Dialogue.kaqemeexBuilt() {
        chatPlayer(quiz, "Did you build this?")
        chatNpc(
            neutral,
            "What, personally? No, of course I didn't. However, our forefathers did. The first " +
                "Druids of Guthix built many stone circles across these lands over eight " +
                "hundred years ago.",
        )
        chatNpc(
            sad,
            "Unfortunately we only know of two remaining, and of those only one is usable by us " +
                "anymore.",
        )
        val option =
            choice3(
                "What about the stone circle full of dark wizards?",
                1,
                "I'm in search of a quest.",
                2,
                "Well, I'll be on my way now.",
                3,
            )
        when (option) {
            1 -> kaqemeexDarkWizards()
            2 -> kaqemeexQuest()
            3 -> kaqemeexLeave()
        }
    }

    private suspend fun Dialogue.kaqemeexDarkWizards() {
        chatPlayer(quiz, "What about the stone circle full of dark wizards?")
        kaqemeexQuestOffer()
    }

    private suspend fun Dialogue.kaqemeexQuest() {
        chatPlayer(quiz, "I'm in search of a quest.")
        chatNpc(
            neutral,
            "Hmm. I think I may have a worthwhile quest for you actually. I don't know if you " +
                "are familiar with the stone circle south of Varrock or not, but...",
        )
        kaqemeexQuestOffer()
    }

    // TODO(content): [quest:druidic_ritual] "Start the Druidic Ritual quest?" offer and start
    private suspend fun Dialogue.kaqemeexQuestOffer() {
        chatNpc(
            sad,
            "That used to be OUR stone circle. Unfortunately, many many years ago, dark wizards " +
                "cast a wicked spell upon it so that they could corrupt its power for their own " +
                "evil ends.",
        )
        chatNpc(
            neutral,
            "When they cursed the rocks for their rituals they made them useless to us and our " +
                "magics. We require a brave adventurer to go on a quest for us to help purify " +
                "the circle of Varrock.",
        )
    }

    private suspend fun Dialogue.kaqemeexLeave() {
        chatPlayer(neutral, "Well, I'll be on my way now.")
        chatNpc(neutral, "Goodbye adventurer. I feel we shall meet again.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sanfew (before Druidic Ritual)
    // TODO(content): [quest:druidic_ritual] after-quest general questions
    // TODO(content): [quest:eadgars_ruse] quest start and goutweed exchange
    // TODO(content): [quest:zogre_flesh_eaters] "Ask about Sanfew Serum."
    private suspend fun Dialogue.sanfew() {
        chatNpc(neutral, "What can I do for you young 'un?")
        val option =
            choice2(
                "I've heard you druids might be able to teach me herblore.",
                1,
                "Actually, I don't need to speak to you.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    quiz,
                    "So... I've heard you druids might be able to teach me herblore...",
                )
                chatNpc(
                    neutral,
                    "Herblore eh? You're probably best off talking to Kaqemeex about that; he's " +
                        "the best herblore teacher we currently have. I believe at the moment " +
                        "he's at out stone circle just North of here.",
                )
                chatPlayer(happy, "Thanks.")
            }
            2 -> {
                chatPlayer(neutral, "Actually, I don't need to speak to you.")
                chatNpc(neutral, "Well, we all make mistakes sometimes.")
            }
        }
    }
}
