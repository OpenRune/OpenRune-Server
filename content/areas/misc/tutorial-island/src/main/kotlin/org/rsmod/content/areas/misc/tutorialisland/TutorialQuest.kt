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
        if (stage >= TutorialStage.QUEST_HOUSE_ENTERED) {
            strike("I arrived on Tutorial Island and met the Gielinor Guide, who")
            strike("welcomed me on my journey to becoming an adventurer.")
        }
        if (stage < TutorialStage.QUEST_HOUSE_ENTERED) {
            line(
                "I met Brynna, the Survival Expert. She taught me how to catch shrimp " +
                    "with a fishing net, and then how to cook those shrimp on a fire " +
                    "made from logs I had cut myself.",
            )
        } else {
            strike("I met Brynna, the Survival Expert. She taught me how to catch")
            strike("shrimp with a fishing net, and then how to cook those shrimp")
            strike("on a fire made from logs I had cut myself.")
        }
        if (stage < TutorialStage.QUEST_HOUSE_ENTERED) {
            line(
                "Lev, the Master Chef, explained proper cooking techniques to me. With " +
                    "his guidance I made a loaf of bread from flour and water.",
            )
        } else {
            strike("Lev, the Master Chef, explained proper cooking techniques to")
            strike("me. With his guidance I made a loaf of bread from flour and")
            strike("water.")
        }
        if (stage < TutorialStage.QUEST_HOUSE_ENTERED) {
            line(
                "The Quest Guide is teaching me about the quest list and quest journals. " +
                    "The journals are useful tools for tracking my progress through quests " +
                    "in Gielinor, and I can keep reminders here of what I have to do next.",
            )
        } else {
            line("The Quest Guide is teaching me about the quest list and quest journals.")
            line("I should speak to the Quest Guide again to continue.")
        }
        if (stage >= TutorialStage.MINING_GUIDE_TALKED) {
            strike("The Mining Instructor taught me how to mine tin and copper and")
            strike("smith them into a bronze dagger.")
        }
        if (stage >= TutorialStage.COMBAT_GUIDE_TALKED) {
            strike("The Combat Instructor taught me how to fight with melee and ranged")
            strike("weapons.")
        }
        if (stage >= TutorialStage.BANK_OPENED) {
            strike("I visited the bank and learned how to store my items.")
        }
        if (stage >= TutorialStage.PRAYER_GUIDE_TALKED) {
            strike("Brother Brace taught me about Prayer.")
        }
        if (stage >= TutorialStage.MAGIC_GUIDE_TALKED) {
            strike("The Magic Instructor taught me how to cast Wind Strike.")
        }
    }

    override fun completedLog(player: ProtectedAccess): String = completionJournal(player) {
        line("I arrived on Tutorial Island and met the Gielinor Guide, who welcomed me on my journey to becoming an adventurer.")
        line("I met Brynna, the Survival Expert. She taught me how to catch shrimp with a fishing net, and then how to cook those shrimp on a fire made from logs I had cut myself.")
        line("Lev, the Master Chef, explained proper cooking techniques to me. With his guidance I made a loaf of bread from flour and water.")
        line("The Quest Guide taught me about the quest list and quest journals.")
        line("The Mining Instructor taught me how to mine tin and copper and smith them into a bronze dagger.")
        line("The Combat Instructor taught me how to fight with melee and ranged weapons.")
        line("I visited the bank and learned how to store my items.")
        line("Brother Brace taught me about Prayer.")
        line("The Magic Instructor taught me how to cast Wind Strike.")
        line("I completed the tutorial and can now travel to the mainland.")
    }
}
