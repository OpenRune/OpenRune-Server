package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestClose
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.plugin.scripts.ScriptContext

internal class TutorialQuest :
    QuestScript(
        questKey = "quest_learningtheropes",
        questVarp = "varp.tutorial",
        rewards =
            rewards {
                extra("Access to the Mainland!")
            },
        completedQuestItemDisplay = ItemRewardDisplay(""),
        closeDialogue = QuestClose.Never,
        closeScroll = QuestClose.Never,
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
        guideLine(stage)
        survivalLine(stage)
        chefLine(stage)
        questGuideLine(stage)
        miningLines(stage)
        combatLines(stage)
        bankLine(stage)
        prayerLine(stage)
        magicLine(stage)
        if (stage >= TutorialStage.COMPLETE) {
            line("I completed the tutorial and can now travel to the mainland.")
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I arrived on Tutorial Island and met the Gielinor Guide, who welcomed me on my journey to becoming an adventurer.")
        line("I met Brynna, the Survival Expert. She taught me how to catch shrimp with a fishing net, and then how to cook those shrimp on a fire made from logs I had cut myself.")
        line("Lev, the Master Chef, explained proper cooking techniques to me. With his guidance I made a loaf of bread from flour and water.")
        line("The Quest Guide taught me about my quest list and quest journals.")
        line("The Mining Instructor taught me how to mine tin and copper and smith them into a bronze dagger.")
        line("The Combat Instructor taught me how to fight with melee and ranged weapons.")
        line("I visited the bank and learned how to store my items.")
        line("Brother Brace taught me about Prayer.")
        line("The Magic Instructor taught me how to cast Wind Strike.")
        line("I completed the tutorial and can now travel to the mainland.")
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.guideLine(stage: Int) {
        val text =
            "I arrived on Tutorial Island and met the Gielinor Guide, who welcomed me on my " +
                "journey to becoming an adventurer."
        if (stage >= TutorialStage.GUIDE_DONE) strike(text) else line(text)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.survivalLine(stage: Int) {
        val text =
            "I met Brynna, the Survival Expert. She taught me how to catch shrimp with a " +
                "fishing net, and then how to cook those shrimp on a fire made from logs I had " +
                "cut myself."
        if (stage >= TutorialStage.SHRIMP_COOKED) strike(text) else line(text)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.chefLine(stage: Int) {
        val text =
            "Lev, the Master Chef, explained proper cooking techniques to me. With his guidance " +
                "I made a loaf of bread from flour and water."
        if (stage >= TutorialStage.BREAD_BAKED) strike(text) else line(text)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.questGuideLine(stage: Int) {
        if (stage < TutorialStage.QUEST_GUIDE_DONE) {
            line(
                "The <col=800000>Quest Guide<col=000080> is teaching me about the quest list " +
                    "and quest journals. The journals are useful tools for tracking my " +
                    "progress through quests in Gielinor, and I can keep reminders here of " +
                    "what I have to do next. Right now I should speak to the " +
                    "<col=800000>Quest Guide<col=000080> again!",
            )
        } else {
            strike(
                "The Quest Guide taught me about my quest list and quest journals. These are " +
                    "a useful tool for tracking my progress through quests in Gielinor.",
            )
        }
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.miningLines(stage: Int) {
        if (stage < TutorialStage.LADDER_DOWN) {
            return
        }
        val mined =
            "I then met Dezzick, the Mining Instructor. He handed me a pickaxe and tasked me " +
                "with mining some copper and tin ores in order to smelt a bronze bar in the " +
                "furnace, which I managed to successfully complete!"
        if (stage >= TutorialStage.BAR_SMELTED) strike(mined) else line(mined)
        val dagger =
            "Dezzick told me to make a bronze dagger with the bronze bar I had smelted. To that " +
                "end, he handed me a hammer and pointed me to the anvils."
        if (stage >= TutorialStage.DAGGER_SMITHED) strike(dagger) else line(dagger)
        val moving =
            "Now that I've learned how to mine and smith, it's time to move on. I should go " +
                "through the nearby gate to find my next instructor."
        if (stage >= TutorialStage.COMBAT_GATE) strike(moving) else line(moving)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.combatLines(stage: Int) {
        if (stage < TutorialStage.COMBAT_GATE) {
            return
        }
        val melee =
            "I met Vannaka, the Combat Instructor. He taught me the basics of melee combat, and " +
                "I killed a giant rat with a bronze sword."
        if (stage >= TutorialStage.RAT_KILLED_MELEE) strike(melee) else line(melee)
        val ranged =
            "Vannaka then gave me a shortbow and arrows, and I killed another giant rat from " +
                "outside the pit."
        if (stage >= TutorialStage.COMBAT_GUIDE_DONE) strike(ranged) else line(ranged)
        val done =
            "Now that I've learned the basics of combat, it's time to move on. I should climb " +
                "the ladder to find my next instructor."
        if (stage >= TutorialStage.LADDER_UP) strike(done) else line(done)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.bankLine(stage: Int) {
        if (stage < TutorialStage.LADDER_UP) {
            return
        }
        val text =
            "I visited the Bank of Gielinor and learned how to store my items. I also learned " +
                "about poll booths and account management."
        if (stage >= TutorialStage.ACCOUNT_GUIDE_DONE) strike(text) else line(text)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.prayerLine(stage: Int) {
        if (stage < TutorialStage.CHAPEL_ARRIVED) {
            return
        }
        val text =
            "Brother Brace taught me about Prayer, including how to activate prayers and how " +
                "to bury bones for Prayer experience."
        if (stage >= TutorialStage.PRAYER_GUIDE_DONE) strike(text) else line(text)
    }

    private fun org.rsmod.content.quest.manager.QuestJournalBuilder.magicLine(stage: Int) {
        if (stage < TutorialStage.MAGIC_GUIDE_ARRIVED) {
            return
        }
        val text =
            "The Magic Instructor taught me how to cast Wind Strike. All spells require runes, " +
                "and I can find them in my magic interface."
        if (stage >= TutorialStage.WIND_STRIKE_CAST) strike(text) else line(text)
    }
}
