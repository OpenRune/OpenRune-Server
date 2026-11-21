package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestClose
import org.rsmod.content.quest.manager.QuestJournalBuilder
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

internal class TutorialQuest :
    QuestScript(
        questKey = "quest_learningtheropes",
        questVarp = "varp.tutorial",
        rewards = rewards { extra("Access to the Mainland!") },
        completedQuestItemDisplay = ItemRewardDisplay("", zoom = 400, x = 78, y = 128),
        stageInVarp = true,
        closeDialogue = QuestClose.Never,
        closeScroll = QuestClose.Never,
        completeJingleId = 153,
    ) {

    override fun ScriptContext.init() {
        check(quest.maxSteps == TutorialStage.COMPLETE || quest.maxSteps == TutorialStage.LEFT_ISLAND) {
            "Unexpected Learning the Ropes end state: ${quest.maxSteps}"
        }
    }

    override fun subTitle(): String =
        "talking to the <col=800000>Quest Guide</col> on <col=800000>Tutorial Island</col>."

    override fun questLog(player: ProtectedAccess): String = questJournal(player) {
        val stage = player.player.tutorialStage
        openingLines(stage)
        questGuideLines(stage)
        miningLines(stage)
        combatLines(stage)
        bankLines(stage)
        prayerLines(stage)
        magicLines(stage)
        if (stage >= TutorialStage.COMPLETE) {
            line("I completed the tutorial and can now travel to the mainland.")
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I arrived on Tutorial Island and met the Gielinor Guide, who")
        line("welcomed me on my journey to becoming an adventurer.")
        line("I met Brynna, the Survival Expert. She taught me how to catch")
        line("shrimp with a fishing net, and then how to cook those shrimp")
        line("on a fire made from logs I had cut myself.")
        line("Lev, the Master Chef, explained proper cooking techniques to")
        line("me. With his guidance I made a loaf of bread from flour and")
        line("water.")
        line("The Quest Guide taught me about my quest list and quest")
        line("journals. These are a useful tool for tracking my progress")
        line("through quests in Gielinor.")
        line("I then met Dezzick, the Mining Instructor. He handed me a")
        line("pickaxe and tasked me with mining some copper and tin ores")
        line("in order to smelt a bronze bar in the furnace, which I managed")
        line("to successfully complete!")
        line("Dezzick told me to make a bronze dagger with the bronze bar I")
        line("had smelted. To that end, he handed me a hammer and pointed")
        line("me to the anvils.")
        line("Vannaka taught me how to equip and unequip my weapons and")
        line("armour from my inventory in order to prepare me for combat.")
        line("I managed to kill a rat in Vannaka's cage.")
        line("Vannaka taught me the basics of melee and ranged combat,")
        line("giving me the weapons I needed to achieve that.")
        line("It would appear I can safely store my items with the Bank of")
        line("Gielinor, which will make them accessible from any branch and")
        line("means I don't have to carry them everywhere with me.")
        line("According to the Account Guide, I should be proactive with my")
        line("security to protect my account.")
        line("Brother Brace taught me about the Prayer skill. It can provide")
        line("a number of benefits to my regular activities, but costs")
        line("prayer points which can be restored at any altar or holy site. I")
        line("can train it by burying bones and scattering ashes.")
        line("Having met Terrova, I was instructed to cast Wind Strike on")
        line("one of his chickens. Fortunately, my magical prowess allowed")
        line("me to successfully achieve this.")
        afterCompletion("I can stay on Tutorial Island to learn more about Ironman")
        afterCompletion("mode, or use my Home Teleport to travel to the mainland.")
    }

    private fun QuestJournalBuilder.openingLines(stage: Int) {
        val guide =
            "I arrived on Tutorial Island and met the Gielinor Guide, who welcomed me on my " +
                "journey to becoming an adventurer."
        if (stage >= TutorialStage.GUIDE_DONE) strike(guide) else line(guide)
        val survival =
            "I met Brynna, the Survival Expert. She taught me how to catch shrimp with a " +
                "fishing net, and then how to cook those shrimp on a fire made from logs I had " +
                "cut myself."
        if (stage >= TutorialStage.SHRIMP_COOKED) strike(survival) else line(survival)
        val chef =
            "Lev, the Master Chef, explained proper cooking techniques to me. With his guidance " +
                "I made a loaf of bread from flour and water."
        if (stage >= TutorialStage.BREAD_BAKED) strike(chef) else line(chef)
    }

    private fun QuestJournalBuilder.questGuideLines(stage: Int) {
        when {
            stage < TutorialStage.BREAD_BAKED -> Unit
            stage < TutorialStage.QUEST_GUIDE_TALKED ->
                line(
                    "I should speak to the Quest Guide in the house to the north to learn about " +
                        "quests.",
                )
            stage < TutorialStage.QUEST_GUIDE_DONE ->
                line(
                    "The Quest Guide is teaching me about the quest list and quest journals. " +
                        "The journals are useful tools for tracking my progress through quests " +
                        "in Gielinor, and I can keep reminders here of what I have to do next. " +
                        "Right now I should speak to the Quest Guide again!",
                )
            else ->
                strike(
                    "The Quest Guide taught me about my quest list and quest journals. These are " +
                        "a useful tool for tracking my progress through quests in Gielinor.",
                )
        }
    }

    private fun QuestJournalBuilder.miningLines(stage: Int) {
        if (stage < TutorialStage.QUEST_GUIDE_DONE) {
            return
        }
        if (stage < TutorialStage.LADDER_DOWN) {
            line(
                "I need to go down the ladder in the Quest Guide's house to find my next tutor, " +
                    "Dezzick, the Mining Instructor.",
            )
            return
        }
        if (stage < TutorialStage.MINING_GUIDE_TALKED) {
            line("I need to speak to Dezzick, the Mining Instructor.")
            return
        }
        val mined =
            "I then met Dezzick, the Mining Instructor. He handed me a pickaxe and tasked me " +
                "with mining some copper and tin ores in order to smelt a bronze bar in the " +
                "furnace, which I managed to successfully complete!"
        if (stage >= TutorialStage.BAR_SMELTED) strike(mined) else line(mined)
        when (stage) {
            TutorialStage.COPPER_MINED_FIRST ->
                line("I have mined some copper ore. I just need some tin ore now.")
            TutorialStage.TIN_MINED ->
                line("I have mined some tin ore. I just need some copper ore now.")
            TutorialStage.COPPER_MINED ->
                line(
                    "I have mined some tin and copper ore. I should take them to the furnace " +
                        "and smelt a bronze bar.",
                )
            TutorialStage.BAR_SMELTED ->
                line("I have smelted a bronze bar. I should speak to Dezzick again.")
            else -> Unit
        }
        if (stage >= TutorialStage.MINING_GUIDE_BAR) {
            val dagger =
                "Dezzick told me to make a bronze dagger with the bronze bar I had smelted. To " +
                    "that end, he handed me a hammer and pointed me to the anvils."
            if (stage >= TutorialStage.DAGGER_SMITHED) strike(dagger) else line(dagger)
        }
        if (stage >= TutorialStage.DAGGER_SMITHED) {
            val moving =
                "Now that I've learned how to mine and smith, it's time to move on. I should go " +
                    "through the nearby gate to find my next instructor."
            if (stage >= TutorialStage.COMBAT_GATE) strike(moving) else line(moving)
        }
    }

    private fun QuestJournalBuilder.combatLines(stage: Int) {
        if (stage < TutorialStage.COMBAT_GATE) {
            return
        }
        if (stage < TutorialStage.DAGGER_EQUIP_TASK) {
            line("I should speak to Vannaka, the Combat Instructor, to learn about combat.")
            return
        }
        when {
            stage < TutorialStage.DAGGER_EQUIPPED ->
                line(
                    "Vannaka told me to wield my bronze dagger. I should open my worn inventory " +
                        "and equip it.",
                )
            stage < TutorialStage.COMBAT_GUIDE_SWORD ->
                line("I have equipped my bronze dagger. I should speak to Vannaka again.")
            stage < TutorialStage.COMBAT_TAB_OPENED ->
                line(
                    "Vannaka gave me a bronze sword and wooden shield. I should equip them and " +
                        "open the combat interface.",
                )
            stage < TutorialStage.ATTACK_RAT ->
                line(
                    "I have equipped my sword and shield and opened the combat interface. I " +
                        "should enter the rat cage through the gate.",
                )
            stage < TutorialStage.RAT_KILLED_MELEE ->
                line("I have entered the rat cage. I should attack a giant rat.")
            else ->
                strike(
                    "I met Vannaka, the Combat Instructor. He taught me the basics of melee " +
                        "combat, and I killed a giant rat with a bronze sword.",
                )
        }
        if (stage in TutorialStage.RAT_KILLED_MELEE until TutorialStage.RANGED_GIVEN) {
            line("I killed a giant rat with melee. I should speak to Vannaka again.")
        }
        if (stage >= TutorialStage.RANGED_GIVEN) {
            val ranged =
                "Vannaka gave me a shortbow and bronze arrows. I should equip them and kill " +
                    "another giant rat from outside the pit."
            if (stage >= TutorialStage.COMBAT_GUIDE_DONE) strike(ranged) else line(ranged)
        }
        if (stage >= TutorialStage.COMBAT_GUIDE_DONE) {
            val done =
                "Now that I have learned the basics of combat, it is time to move on. I should " +
                    "climb the ladder to find my next instructor."
            if (stage >= TutorialStage.LADDER_UP) strike(done) else line(done)
        }
    }

    private fun QuestJournalBuilder.bankLines(stage: Int) {
        if (stage < TutorialStage.LADDER_UP) {
            return
        }
        when {
            stage < TutorialStage.BANK_OPENED ->
                line(
                    "I should visit the Bank of Gielinor and open the bank booth to store my " +
                        "items.",
                )
            stage < TutorialStage.POLL_BOOTH ->
                line("I opened my bank. I should use the poll booth to learn about polls.")
            stage < TutorialStage.ACCOUNT_GUIDE_DONE ->
                line(
                    "I used the poll booth. I should speak to the Account Guide and open the " +
                        "Account Management interface.",
                )
            else ->
                strike(
                    "I visited the Bank of Gielinor and learned how to store my items. I also " +
                        "learned about poll booths and account management.",
                )
        }
    }

    private fun QuestJournalBuilder.prayerLines(stage: Int) {
        if (stage < TutorialStage.CHAPEL_ARRIVED) {
            return
        }
        if (stage < TutorialStage.PRAYER_GUIDE_DONE) {
            line("I should speak to Brother Brace and open my Prayer interface.")
            return
        }
        strike(
            "Brother Brace taught me about Prayer, including how to activate prayers and how " +
                "to bury bones for Prayer experience.",
        )
    }

    private fun QuestJournalBuilder.magicLines(stage: Int) {
        if (stage < TutorialStage.MAGIC_GUIDE_ARRIVED) {
            return
        }
        if (stage < TutorialStage.WIND_STRIKE_CAST) {
            line(
                "I should speak to the Magic Instructor, open my Magic interface, and cast Wind " +
                    "Strike on a chicken.",
            )
            return
        }
        strike(
            "The Magic Instructor taught me how to cast Wind Strike. All spells require runes, " +
                "and I can find them in my magic interface.",
        )
    }
}
