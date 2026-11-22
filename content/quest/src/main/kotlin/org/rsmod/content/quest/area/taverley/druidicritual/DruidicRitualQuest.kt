package org.rsmod.content.quest.area.taverley.druidicritual

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.table.QuestRow
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.menu
import org.rsmod.content.quest.manager.rewards
import org.rsmod.content.quest.manager.startQuestPrompt
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class DruidicRitualQuest @Inject constructor() :
    QuestScript(
        "quest_druidicritual",
        "varp.druidquest",
        rewards {
            xp("stat.herblore", HERBLORE_XP)
            extra("Access to the Herblore skill")
        },
        ItemRewardDisplay(REWARD_DISPLAY, zoom = 230),
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "Druidic Ritual end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpNpc1(KAQEMEEX) { startDialogue(it.npc) { kaqemeex() } }
        onOpNpc1(SANFEW) { startDialogue(it.npc) { sanfew() } }
        onOpLocU(CAULDRON) { dipInCauldron(it.objType, it.invSlot) }
    }

    override fun subTitle(): String =
        "speaking to <col=800000>Kaqemeex<col=000080> who is at the <col=800000>Druids " +
            "Circle<col=000080> just <col=800000>north<col=000080> of " +
            "<col=800000>Taverley<col=000080>."

    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            strike("Kaqemeex and the druids are preparing a ceremony to")
            strike("purify the Varrock stone circle. I told Kaqemeex I would")
            strike("help them.")
            line("")
            when (stage(player.player)) {
                STAGE_STARTED ->
                    line("I should speak to <red>Sanfew</red> in the village to the <red>south</red>.")
                STAGE_MEATS -> {
                    line("<red>Sanfew</red> told me for the ritual they would need me to place")
                    line("<red>raw bear meat</red>, <red>raw chicken</red>, <red>raw rat meat</red>, and <red>raw beef</red> in")
                    line("the <red>Cauldron of Thunder</red> in the <red>dungeon south</red> of <red>Taverley</red>.")
                }
                else -> {
                    strike("The ceremony required various meats being placed in the")
                    strike("Cauldron of Thunder. I did this and gave them to Sanfew.")
                    line("")
                    line("I should speak to <red>Kaqemeex</red> again and claim my <red>reward</red>.")
                }
            }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            strike("Kaqemeex and the druids are preparing a ceremony to")
            strike("purify the Varrock stone circle. I told Kaqemeex I would")
            strike("help them.")
            strike("The ceremony required various meats being placed in the")
            strike("Cauldron of Thunder. I did this and gave them to Sanfew.")
            strike("Kaqemeex then taught me the basics of the Herblore skill.")
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    private suspend fun Dialogue.kaqemeex() {
        chatPlayer(neutral, "Hello there.")
        when (stage(player)) {
            0 -> kaqemeexIntro()
            STAGE_STARTED,
            STAGE_MEATS -> {
                chatNpc(
                    neutral,
                    "Hello again, adventurer. You will need to speak to my fellow druid Sanfew in " +
                        "the village south of here to continue in your quest.",
                )
                chatPlayer(neutral, "Okay, thanks.")
            }
            STAGE_MEATS_GIVEN -> teachHerblore()
            else -> kaqemeexAfterQuest()
        }
    }

    private suspend fun Dialogue.kaqemeexIntro() {
        chatNpc(neutral, "What brings you to our holy monument?")
        when (
            menu(
                "Who are you?" to KaqemeexTopic.WhoAreYou,
                SEARCH_QUEST to KaqemeexTopic.Quest,
                "Did you build this?" to KaqemeexTopic.Built,
            )
        ) {
            KaqemeexTopic.WhoAreYou -> whoAreYou()
            KaqemeexTopic.Quest -> searchForQuest()
            KaqemeexTopic.Built -> didYouBuildThis()
            else -> Unit
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "We are the druids of Guthix. We worship our god at our famous stone circles. You " +
                "will find them located throughout these lands.",
        )
        when (
            menu(
                DARK_WIZARDS to KaqemeexTopic.DarkWizards,
                "So what's so good about Guthix?" to KaqemeexTopic.Guthix,
                LEAVE to KaqemeexTopic.Leave,
            )
        ) {
            KaqemeexTopic.DarkWizards -> darkWizards()
            KaqemeexTopic.Guthix -> {
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
            else -> leave()
        }
    }

    private suspend fun Dialogue.didYouBuildThis() {
        chatPlayer(quiz, "Did you build this?")
        chatNpc(
            neutral,
            "What, personally? No, of course I didn't. However, our forefathers did. The first " +
                "Druids of Guthix built many stone circles across these lands over eight " +
                "hundred years ago.",
        )
        chatNpc(
            sad,
            "Unfortunately we only know of two remaining, and of those only one is usable by " +
                "us anymore.",
        )
        when (
            menu(
                DARK_WIZARDS to KaqemeexTopic.DarkWizards,
                "I'm in search of a quest" to KaqemeexTopic.Quest,
                LEAVE to KaqemeexTopic.Leave,
            )
        ) {
            KaqemeexTopic.DarkWizards -> darkWizards()
            KaqemeexTopic.Quest -> searchForQuest()
            else -> leave()
        }
    }

    private suspend fun Dialogue.leave() {
        chatPlayer(neutral, LEAVE)
        chatNpc(neutral, "Goodbye adventurer. I feel we shall meet again.")
    }

    private suspend fun Dialogue.darkWizards() {
        chatPlayer(quiz, DARK_WIZARDS)
        offerQuest()
    }

    private suspend fun Dialogue.searchForQuest() {
        chatPlayer(quiz, SEARCH_QUEST)
        chatNpc(
            neutral,
            "Hmm. I think I may have a worthwhile quest for you actually. I don't know if you are " +
                "familiar with the stone circle south of Varrock or not, but...",
        )
        offerQuest()
    }

    private suspend fun Dialogue.offerQuest() {
        chatNpc(
            angry,
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
        val recommended = QuestRow.getRow(quest.rowID).recommendedCombat ?: 0
        if (player.combatLevel < recommended) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $recommended."
            )
        }
        if (!startQuestPrompt(quest)) {
            chatPlayer(neutral, "No, that doesn't sound very interesting.")
            chatNpc(
                neutral,
                "I will not try and change your mind adventurer. Some day when you have matured " +
                    "you may reconsider your position. We will wait until then.",
            )
            return
        }
        if (stage(player) == 0) {
            quest.setQuestStage(access, STAGE_STARTED)
        }
        chatPlayer(happy, "Okay, I will try and help.")
        chatNpc(
            happy,
            "Excellent. Go to the village south of this place and speak to my fellow druid, " +
                "Sanfew, who is working on the purification ritual. He knows what is required " +
                "to complete it.",
        )
        chatPlayer(neutral, "Will do.")
    }

    private suspend fun Dialogue.teachHerblore() {
        chatNpc(
            happy,
            "I have word from Sanfew that you have been very helpful in assisting him with his " +
                "preparations for the purification ritual. As promised I will now teach you the " +
                "ancient arts of Herblore.",
        )
        herbloreFundamentals()
        quest.completeQuest(access)
    }

    private suspend fun Dialogue.herbloreFundamentals() {
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
    }

    private suspend fun Dialogue.kaqemeexAfterQuest() {
        chatNpc(neutral, "Hello again. How is the Herblore going?")
        val mastered = player.statBase(HERBLORE) >= DruidicRitualItems.MAX_LEVEL
        val options = buildList {
            add("Very well, thank you." to KaqemeexTopic.VeryWell)
            if (!mastered) add("I need more practice at it..." to KaqemeexTopic.Practice)
            add(FUNDAMENTALS to KaqemeexTopic.Fundamentals)
            if (mastered) {
                add("May I buy a Herblore skillcape, please?" to KaqemeexTopic.BuyCape)
                if (DruidicRitualItems.ownsCape(player) && !DruidicRitualItems.ownsHood(player)) {
                    add("Can I get a hood for my skillcape, please?" to KaqemeexTopic.Hood)
                }
            } else {
                add("What must I do to wear a Herblore skillcape?" to KaqemeexTopic.CapeInfo)
            }
        }
        when (menu(options)) {
            KaqemeexTopic.VeryWell -> {
                chatPlayer(neutral, "Very well, thank you.")
                chatNpc(neutral, "That is good to hear.")
            }
            KaqemeexTopic.Practice -> {
                chatPlayer(sad, "I need more practice at it...")
                chatNpc(neutral, "Persistence is key to success.")
            }
            KaqemeexTopic.Fundamentals -> {
                chatPlayer(confused, FUNDAMENTALS)
                chatNpc(neutral, "Indeed I will...")
                herbloreFundamentals()
                chatNpc(happy, "Good luck with your Herblore practices, and a good day to you.")
                chatPlayer(neutral, "Thanks for your help.")
            }
            KaqemeexTopic.CapeInfo -> {
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
            KaqemeexTopic.BuyCape -> buyCape()
            KaqemeexTopic.Hood -> hood()
            else -> Unit
        }
    }

    private suspend fun Dialogue.buyCape() {
        chatPlayer(quiz, "May I buy a Herblore skillcape, please?")
        if (player.inv.count(DruidicRitualItems.COINS) < DruidicRitualItems.CAPE_PRICE) {
            chatNpc(
                neutral,
                "Most certainly, but I must ask for a donation of 99,000 coins to cover the " +
                    "expense of the cape.",
            )
            return
        }
        if (player.inv.freeSpace() < CAPE_SLOTS) {
            chatNpc(neutral, NO_SPACE_FOR_CAPE)
            return
        }
        chatNpc(
            happy,
            "Most certainly; the Nardah Herbalist will recognize this cape and create unfinished " +
                "potions for you and it may be searched for a pestle and mortar.",
        )
        chatNpc(
            neutral,
            "It has been a pleasure to watch you grow as a herbalist. I am privileged to have " +
                "been instrumental in your learning, but I must ask for a donation of 99,000 " +
                "coins to cover the expense of the cape.",
        )
        val pay =
            choice2("I'm afraid that's too much money for me.", false, "Okay, here's 99,000 coins.", true)
        if (!pay) {
            chatPlayer(sad, "I'm afraid that's too much money for me.")
            chatNpc(
                neutral,
                "Not at all; there are many other adventurers who would love the opportunity to " +
                    "purchase such a prestigious item. You can find me here if you change your mind.",
            )
            return
        }
        chatPlayer(happy, "Okay, here's 99,000 coins.")
        if (!DruidicRitualItems.buyCape(player)) {
            chatNpc(neutral, NO_SPACE_FOR_CAPE)
            return
        }
        chatNpc(happy, "Good luck to you, ${player.displayName}.")
    }

    private suspend fun Dialogue.hood() {
        chatPlayer(quiz, "Can I get a hood for my skillcape, please?")
        if (!DruidicRitualItems.giveHood(player)) {
            chatNpc(
                neutral,
                "You don't appear to have enough space in your inventory, come back and see me " +
                    "when you do.",
            )
            return
        }
        chatNpc(happy, "Most certainly, and free of charge!")
        objbox(DruidicRitualItems.HOOD, "Kaqemeex hands you another hood for your skillcape.")
    }

    private suspend fun Dialogue.sanfew() {
        if (stage(player) == STAGE_MEATS) {
            sanfewWantsMeats()
            return
        }
        chatNpc(quiz, "What can I do for you young 'un?")
        when (stage(player)) {
            0 -> sanfewBeforeQuest()
            STAGE_STARTED -> sanfewGivesTask()
            STAGE_MEATS_GIVEN -> sendToKaqemeex()
            else -> sanfewAfterQuest()
        }
    }

    private suspend fun Dialogue.sanfewBeforeQuest() {
        val herblore =
            menu(
                "I've heard you druids might be able to teach me herblore." to true,
                NO_NEED to false,
            )
        if (!herblore) {
            noNeedToSpeak()
            return
        }
        chatPlayer(quiz, "So... I've heard you druids might be able to teach me herblore...")
        chatNpc(
            neutral,
            "Herblore eh? You're probably best off talking to Kaqemeex about that; he's the best " +
                "herblore teacher we currently have. I believe at the moment he's at out stone " +
                "circle just North of here.",
        )
        chatPlayer(neutral, "Thanks.")
    }

    private suspend fun Dialogue.sanfewGivesTask() {
        val help = menu(SENT_TO_HELP to true, NO_NEED to false)
        if (!help) {
            noNeedToSpeak()
            return
        }
        chatPlayer(
            neutral,
            "I've been sent to assist you with the ritual to purify the Varrockian stone circle.",
        )
        if (stage(player) == STAGE_STARTED) {
            quest.setQuestStage(access, STAGE_MEATS)
        }
        chatNpc(
            neutral,
            "Well, what I'm struggling with right now is the meats needed for the potion to " +
                "honour Guthix. I need the raw meat of four different animals for it, but not " +
                "just any old meats will do.",
        )
        chatNpc(
            neutral,
            "Each meat has to be dipped individually into the Cauldron of Thunder for it to work " +
                "correctly.",
        )
        cauldronMenu()
    }

    private suspend fun Dialogue.cauldronMenu() {
        val where = menu("Where can I find this cauldron?" to true, DO_THAT to false)
        if (!where) {
            chatPlayer(neutral, DO_THAT)
            chatNpc(happy, "Well thank you very much!")
            return
        }
        chatPlayer(quiz, "Where can I find this cauldron?")
        chatNpc(
            neutral,
            "It is located somewhere in the mysterious underground halls which are located " +
                "somewhere in the woods just South of here. They are too dangerous for me to go " +
                "myself however.",
        )
    }

    private suspend fun Dialogue.sanfewWantsMeats() {
        chatNpc(quiz, "Did you bring me the required ingredients for the potion?")
        if (!DruidicRitualItems.hasAllEnchanted(player)) {
            chatPlayer(sad, "No, not yet...")
            chatNpc(neutral, "Well, let me know when you do young 'un.")
            val remind =
                menu("What was I meant to be doing again?" to true, "I'll get on with it." to false)
            if (!remind) {
                chatPlayer(neutral, "I'll get on with it.")
                chatNpc(happy, "Good, good.")
                return
            }
            chatPlayer(confused, "What was I meant to be doing again?")
            chatNpc(
                laugh,
                "Trouble with your memory eh young 'un? I need the raw meats of four different " +
                    "animals that have been dipped into the Cauldron of Thunder so I can make " +
                    "my potion to honour Guthix.",
            )
            chatPlayer(happy, "Ooooh yeah, I remember.")
            cauldronMenu()
            return
        }
        chatPlayer(happy, "Yes, I have all four now!")
        if (!handInMeats(access)) {
            return
        }
        chatNpc(
            happy,
            "Thank you so much adventurer! These meats will allow our potion to honour Guthix to " +
                "be completed, and bring one step closer to reclaiming our stone circle!",
        )
        sendToKaqemeex()
    }

    private suspend fun Dialogue.sendToKaqemeex() {
        chatNpc(
            happy,
            "Now go and talk to Kaqemeex and he will introduce you to the wonderful world of " +
                "herblore and potion making!",
        )
    }

    private suspend fun Dialogue.sanfewAfterQuest() {
        val work = menu(MORE_WORK to true, NO_NEED to false)
        if (!work) {
            noNeedToSpeak()
            return
        }
        chatPlayer(quiz, MORE_WORK)
        chatNpc(neutral, "Not just yet, young 'un.")
        mesbox("You do not meet all of the requirements to start the Eadgar's Ruse quest.")
    }

    private suspend fun Dialogue.noNeedToSpeak() {
        chatPlayer(neutral, "Actually, I don't need to speak to you.")
        chatNpc(neutral, "Well, we all make mistakes sometimes.")
    }

    fun handInMeats(access: ProtectedAccess): Boolean {
        if (stage(access.player) != STAGE_MEATS || !DruidicRitualItems.handIn(access.player)) {
            return false
        }
        quest.setQuestStage(access, STAGE_MEATS_GIVEN)
        return true
    }

    private suspend fun ProtectedAccess.dipInCauldron(raw: ItemServerType, slot: Int) {
        arriveDelay()
        if (stage(player) != STAGE_MEATS || DruidicRitualItems.enchantedFor(raw) == null) {
            mes(NOTHING_INTERESTING)
            return
        }
        anim(DIP_SEQ)
        if (DruidicRitualItems.dip(player, raw, slot)) {
            mes("You dip the ${raw.name.lowercase()} in the cauldron.")
        }
    }

    private enum class KaqemeexTopic {
        WhoAreYou,
        Quest,
        Built,
        DarkWizards,
        Guthix,
        Leave,
        VeryWell,
        Practice,
        Fundamentals,
        CapeInfo,
        BuyCape,
        Hood,
    }

    companion object {
        const val STAGE_STARTED = 1
        const val STAGE_MEATS = 2
        const val STAGE_MEATS_GIVEN = 3
        const val STAGE_COMPLETE = 4

        const val KAQEMEEX = "npc.kaqemeex"
        const val SANFEW = "npc.sanfew"
        const val CAULDRON = "loc.cauldron_of_thunder"

        private const val HERBLORE = "stat.herblore"
        private const val HERBLORE_XP = 250.0
        private const val REWARD_DISPLAY = "obj.guam_leaf"
        private const val DIP_SEQ = "seq.human_pickuptable"
        private const val CAPE_SLOTS = 2

        private const val SEARCH_QUEST = "I'm in search of a quest."
        private const val DARK_WIZARDS = "What about the stone circle full of dark wizards?"
        private const val LEAVE = "Well, I'll be on my way now."
        private const val FUNDAMENTALS = "Can you explain the fundamentals again?"
        private const val SENT_TO_HELP = "I've been sent to help purify the Varrock stone circle."
        private const val DO_THAT = "Ok, I'll do that then."
        private const val NO_NEED = "Actually, I don't need to speak to you."
        private const val MORE_WORK = "Have you any more work for me, to help reclaim the circle?"
        private const val NOTHING_INTERESTING = "Nothing interesting happens."
        private const val NO_SPACE_FOR_CAPE =
            "Unfortunately all Skillcapes are only available with a free hood; it's part of a " +
                "skill promotion deal - buy one get one free, you know. So you'll need to free " +
                "up some inventory space before I can sell you one."
    }
}
