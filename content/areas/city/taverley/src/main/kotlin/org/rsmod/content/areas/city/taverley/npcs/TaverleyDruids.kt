package org.rsmod.content.areas.city.taverley.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TaverleyDruids @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kaqemeex") { startDialogue(it.npc) { kaqemeex() } }
        onOpNpc1("npc.sanfew") { startDialogue(it.npc) { sanfew() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kaqemeex
    // TODO(content): [system:skillcapes] trimmed cape when the player has more than one level 99
    private suspend fun Dialogue.kaqemeex() {
        if (QuestRequirements.hasCompleted(player, DRUIDIC_RITUAL)) {
            kaqemeexAfterQuest()
            return
        }
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

    private suspend fun Dialogue.kaqemeexAfterQuest() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello again. How is the Herblore going?")
        val master = player.statBase("stat.herblore") >= MAX_LEVEL
        val ownsCape = playerContainsObj(HERBLORE_CAPE) || playerContainsObj(HERBLORE_CAPE_TRIMMED)
        val options =
            buildList {
                add("Very well, thank you." to 1)
                if (!master) add("I need more practice at it..." to 2)
                add("Can you explain the fundamentals again?" to 3)
                if (master) {
                    add("May I buy a Herblore skillcape, please" to 5)
                    if (ownsCape && !playerContainsObj(HERBLORE_HOOD)) {
                        add("Can I get a hood for my skillcape, please?" to 6)
                    }
                } else {
                    add("What must I do to wear a Herblore skillcape?" to 4)
                }
            }
        when (choiceOf(options)) {
            1 -> {
                chatPlayer(happy, "Very well, thank you.")
                chatNpc(happy, "That is good to hear.")
            }
            2 -> {
                chatPlayer(sad, "I need more practice at it...")
                chatNpc(neutral, "Persistence is key to success.")
            }
            3 -> kaqemeexFundamentals()
            4 -> {
                chatPlayer(quiz, "What must I do to wear a Herblore skillcape?")
                chatNpc(
                    neutral,
                    "To earn the right to wear any skillcape you need to have mastered that " +
                        "skill to the highest level possible and it is no different for Herblore.",
                )
                chatNpc(
                    neutral,
                    "With the cape equipped the Nardah Herbalist will create unfinished potions " +
                        "for you and you may search the cape for a pestle and mortar. When you " +
                        "have achieved a level of 99, come back and talk to me again.",
                )
            }
            5 -> kaqemeexBuyCape()
            6 -> kaqemeexHood()
        }
    }

    private suspend fun Dialogue.kaqemeexFundamentals() {
        chatPlayer(quiz, "Can you explain the fundamentals again?")
        chatNpc(neutral, "Indeed I will...")
        chatNpc(
            neutral,
            "Herblore is the skill of working with herbs and other ingredients, to make useful " +
                "potions and poison.",
        )
        chatNpc(
            neutral,
            "First you will need a vial, which can be found or made with the crafting skill.",
        )
        chatNpc(neutral, "Then you must gather the herbs needed to make the potion you want.")
        chatNpc(
            neutral,
            "You must fill your vial with water and add the ingredients you need. There are " +
                "normally 2 ingredients to each type of potion.",
        )
        chatNpc(neutral, "Bear in mind, you must first clean each herb before you can use it.")
        chatNpc(
            neutral,
            "You may also have to grind some ingredients before you can use them. You will need " +
                "a pestle and mortar in order to do this.",
        )
        chatNpc(
            neutral,
            "Herbs can be found on the ground, and are also dropped by some monsters when you " +
                "kill them.",
        )
        chatNpc(
            neutral,
            "Let's try an example Attack potion: The first ingredient is Guam leaf; the next is " +
                "Eye of Newt.",
        )
        chatNpc(
            neutral,
            "Mix these in your water-filled vial, and you will produce an Attack potion. Drink " +
                "this potion to increase your Attack level.",
        )
        chatNpc(
            neutral,
            "Different potions also require different Herblore levels before you can make them.",
        )
        chatNpc(happy, "Good luck with your Herblore practices, and a good day to you.")
        chatPlayer(happy, "Thanks for your help.")
    }

    private suspend fun Dialogue.kaqemeexBuyCape() {
        chatPlayer(quiz, "May I buy a Herblore skillcape, please?")
        if (access.inv.count(COINS) < CAPE_PRICE) {
            chatNpc(
                neutral,
                "Most certainly, but I must ask for a donation of 99,000 coins to cover the " +
                    "expense of the cape.",
            )
            return
        }
        if (access.inv.freeSpace() < 2) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood; it's part of " +
                    "a skill promotion deal - buy one get one free, you know. So you'll need to " +
                    "free up some inventory space before I can sell you one.",
            )
            return
        }
        chatNpc(
            happy,
            "Most certainly; the Nardah Herbalist will recognize this cape and create " +
                "unfinished potions for you and it may be searched for a pestle and mortar.",
        )
        chatNpc(
            neutral,
            "It has been a pleasure to watch you grow as a herbalist. I am privileged to have " +
                "been instrumental in your learning, but I must ask for a donation of 99,000 " +
                "coins to cover the expense of the cape.",
        )
        val pay =
            choice2(
                "I'm afraid that's too much money for me.",
                false,
                "Okay, here's 99,000 coins.",
                true,
            )
        if (!pay) {
            chatPlayer(sad, "I'm afraid that's too much money for me.")
            chatNpc(
                neutral,
                "Not at all; there are many other adventurers who would love the opportunity to " +
                    "purchase such a prestigious item. You can find me here if you change your " +
                    "mind.",
            )
            return
        }
        chatPlayer(happy, "Okay, here's 99,000 coins.")
        if (!player.invTakeFee(CAPE_PRICE)) {
            return
        }
        player.invAddOrDrop(objRepo, HERBLORE_CAPE)
        player.invAddOrDrop(objRepo, HERBLORE_HOOD)
        chatNpc(happy, "Good luck to you, ${player.displayName}.")
    }

    private suspend fun Dialogue.kaqemeexHood() {
        chatPlayer(quiz, "Can I get a hood for my skillcape, please?")
        if (access.inv.freeSpace() < 1) {
            chatNpc(
                neutral,
                "You don't appear to have enough space in your inventory, come back and see me " +
                    "when you do.",
            )
            return
        }
        chatNpc(happy, "Most certainly, and free of charge!")
        player.invAddOrDrop(objRepo, HERBLORE_HOOD)
        objbox(HERBLORE_HOOD, "Kaqemeex hands you another hood for your skillcape.")
    }

    private suspend fun Dialogue.choiceOf(options: List<Pair<String, Int>>): Int =
        if (options.size == 3) {
            choice3(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
            )
        } else {
            choice4(
                options[0].first,
                options[0].second,
                options[1].first,
                options[1].second,
                options[2].first,
                options[2].second,
                options[3].first,
                options[3].second,
            )
        }

    private suspend fun Dialogue.kaqemeexLeave() {
        chatPlayer(neutral, "Well, I'll be on my way now.")
        chatNpc(neutral, "Goodbye adventurer. I feel we shall meet again.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sanfew
    private suspend fun Dialogue.sanfew() {
        chatNpc(neutral, "What can I do for you young 'un?")
        if (!QuestRequirements.hasCompleted(player, DRUIDIC_RITUAL)) {
            sanfewBeforeQuest()
            return
        }
        val serum =
            QuestRequirements.isOnQuest(player, ZOGRE_FLESH_EATERS) ||
                QuestRequirements.hasCompleted(player, ZOGRE_FLESH_EATERS)
        val option =
            if (serum) {
                choice3(
                    "Ask about Sanfew Serum.",
                    1,
                    "Ask general questions.",
                    2,
                    "Actually, I don't need to speak to you.",
                    3,
                )
            } else {
                choice2("Ask general questions.", 2, "Actually, I don't need to speak to you.", 3)
            }
        when (option) {
            1 -> sanfewSerum()
            2 -> sanfewGeneral()
            3 -> sanfewNoNeed()
        }
    }

    private suspend fun Dialogue.sanfewBeforeQuest() {
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
            2 -> sanfewNoNeed()
        }
    }

    private suspend fun Dialogue.sanfewNoNeed() {
        chatPlayer(neutral, "Actually, I don't need to speak to you.")
        chatNpc(neutral, "Well, we all make mistakes sometimes.")
    }

    private suspend fun Dialogue.sanfewSerum() {
        chatPlayer(
            quiz,
            "Hey there, can you tell me something about this Sanfew Serum, like how do I make " +
                "it, and what does it do?",
        )
        chatNpc(
            happy,
            "Certainly. It was after all, my creation, and mine alone. If anyone should know how " +
                "to make it, it should be me!",
        )
        chatPlayer(laugh, "Can't fault that logic!")
        chatNpc(
            neutral,
            "Ok, essentially this potion is a mixture of a cure poison, cure disease and restore " +
                "stats all rolled into one! Very handy for the adventurers with an eye on " +
                "conserving inventory space.",
        )
        chatPlayer(quiz, "Ok, I'm sold... How do you make it?")
        chatNpc(
            neutral,
            "Well, it's very easy really. You take a standard super stat restore potion. Add " +
                "some ground unicorn horn. Then, add some snake weed from the island of " +
                "Karamja. Finally, add some nail beast nails. You can",
        )
        chatNpc(
            neutral,
            "only get these from the area of Morytania, specifically, if you go deep in the " +
                "Mort Myre swamp.",
        )
        chatPlayer(quiz, "How do you mean, deep into the Mort Myre swamp?")
        chatNpc(
            neutral,
            "Well, most adventurers avoid the deep Mort Myre swamp, whereas the Paterdomus trek " +
                "and the Burgh de Rott trek take you through the most interesting areas. These " +
                "area's often allow the quickest, but potentially",
        )
        chatNpc(
            neutral,
            "more dangerous routes through Mort Myre. This is where you're likely to encounter a " +
                "Nail beast.",
        )
        chatPlayer(quiz, "Are they very dangerous?")
        chatNpc(
            neutral,
            "They can be pretty dangerous. They'll defend for a long time, then attack suddenly " +
                "and seemingly without warning, landing three viscious attacks at once. This " +
                "can be pretty lethal. However, the nail beasts themselves",
        )
        chatNpc(
            neutral,
            "are curiously wounded from the Sanfew Serum itself...very bizzare, that a potion " +
                "made from their claws should have such a pronounced effect on them.",
        )
        chatPlayer(happy, "Ok, that's very helpful. Many thanks for your assistance.")
    }

    // TODO(content): [verify:in_game] the wiki lists only the work question under "Ask general
    //  questions." before Eadgar's Ruse; this adds "Actually, I don't need to speak to you."
    private suspend fun Dialogue.sanfewGeneral() {
        if (!QuestRequirements.hasCompleted(player, EADGARS_RUSE)) {
            val work =
                choice2(
                    "Have you any more work for me, to help reclaim the circle?",
                    true,
                    "Actually, I don't need to speak to you.",
                    false,
                )
            if (work) sanfewEadgarsRuse() else sanfewNoNeed()
            return
        }
        val goutweed = access.inv.count(GOUTWEED) > 0
        val option =
            choice3(
                if (goutweed) {
                    "I have some more goutweed for you."
                } else {
                    "Did you say you needed more goutweed?"
                },
                1,
                "Have you any more work for me, to help reclaim the circle?",
                2,
                "Actually, I don't need to speak to you.",
                3,
            )
        when (option) {
            1 -> {
                if (goutweed) {
                    chatPlayer(happy, "I have some more goutweed for you.")
                    // TODO(content): [quest:eadgars_ruse] goutweed exchange for a herb from the
                    //  herb drop table (https://oldschool.runescape.wiki/w/Goutweed)
                    return
                }
                chatPlayer(quiz, "Did you say you needed more goutweed?")
                chatNpc(
                    neutral,
                    "I don't need any more goutweed for the ritual, but it's still quite " +
                        "difficult for me to get. If you ever come across some, bring it to me; " +
                        "I'll exchange it for some other herbs.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Have you any more work for me to help reclaim the stone circle?")
                chatNpc(
                    neutral,
                    "Well, not right now I don't think young 'un. In fact, I need to make some " +
                        "more preparations myself for the ritual. Rest assured, if I need any " +
                        "more help I will ask you again.",
                )
            }
            3 -> sanfewNoNeed()
        }
    }

    // TODO(content): [quest:eadgars_ruse] "Start the Eadgar's Ruse quest?" offer, start and
    //  in-progress dialogue
    private suspend fun Dialogue.sanfewEadgarsRuse() {
        chatPlayer(quiz, "Have you any more work for me, to help reclaim the circle?")
        if (!QuestRequirements.hasCompleted(player, TROLL_STRONGHOLD)) {
            chatNpc(neutral, "Not just yet, young 'un.")
            mesbox("You do not meet all of the requirements to start the Eadgar's Ruse quest.")
            return
        }
        if (player.stat("stat.herblore") < EADGARS_RUSE_HERBLORE) {
            chatNpc(
                neutral,
                "I'm afraid you are not yet learned enough in the art of Herblore to be able to " +
                    "help me, adventurer...",
            )
            mesbox("You do not meet all of the requirements to start the Eadgar's Ruse quest.")
            return
        }
        chatPlayer(quiz, "Have you any more work for me to help reclaim the stone circle?")
        chatNpc(
            neutral,
            "Ah, you've come just in time. I need a certain herb for the next part of the " +
                "purification ritual...",
        )
        chatNpc(
            neutral,
            "It used to be quite common, but nowadays only the trolls know where to find it. " +
                "They use it in their cooking, you see. It's a very prized ingredient.",
        )
        chatPlayer(quiz, "And what exactly do you want me to do?")
        chatNpc(
            neutral,
            "Journey to the north into the land of the trolls, and find the secret of the herb " +
                "the trolls call 'goutweed'.",
        )
        chatNpc(
            neutral,
            "My friend Eadgar lives in the area, and he may be able to help you. Bring some " +
                "goutweed back and I will teach you something useful.",
        )
    }

    private companion object {
        const val DRUIDIC_RITUAL = "quest_druidicritual"
        const val EADGARS_RUSE = "quest_eadgarsruse"
        const val TROLL_STRONGHOLD = "quest_trollstronghold"
        const val ZOGRE_FLESH_EATERS = "quest_zogreflesheaters"

        const val MAX_LEVEL = 99
        const val CAPE_PRICE = 99_000
        const val COINS = "obj.coins"
        const val HERBLORE_CAPE = "obj.skillcape_herblore"
        const val HERBLORE_CAPE_TRIMMED = "obj.skillcape_herblore_trimmed"
        const val HERBLORE_HOOD = "obj.skillcape_herblore_hood"

        // https://oldschool.runescape.wiki/w/Eadgar%27s_Ruse: 31 Herblore
        const val EADGARS_RUSE_HERBLORE = 31
        const val GOUTWEED = "obj.eadgar_goutweed_herb"
    }
}
