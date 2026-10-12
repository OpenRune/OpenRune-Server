package org.rsmod.content.areas.zeah.kourend

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.manager.QuestRequirements

// https://oldschool.runescape.wiki/w/Transcript:Richard_(Hosidius) (the same for Bast, Fuggy,
//  Oswald, Sheryl and Rassain)
internal suspend fun Dialogue.kourendRumour() {
    val next = RUMOURS.firstOrNull { (quest, _) -> notStarted(quest) }
    chatNpc(neutral, next?.second ?: "No, it hasn't been very busy lately.")
}

private fun Dialogue.notStarted(quest: String): Boolean =
    !QuestRequirements.hasCompleted(player, quest) && !QuestRequirements.isOnQuest(player, quest)

private val RUMOURS =
    listOf(
        "quest_clientofkourend" to
            "Apparently Veos is looking for some help with something. He hangs around the docks " +
                "of Port Piscarilius.",
        "quest_queenofthieves" to
            "They say there's trouble in Port Piscarilius. Something about gangs uniting. Might " +
                "be worth looking into.",
        "quest_depthsofdespair" to
            "I hear that Lord Hosidius has misplaced one of his sons. The Hosidius family might " +
                "pay a lot to see him returned safely.",
        "quest_taleoftherighteous" to
            "Phileas Rimor has supposedly been looking for some assistance with something in " +
                "Shayzien. Maybe worth investigating.",
        "quest_forsakentower" to
            "I hear that the rulers of Lovakengj have some work available. You'll find them in " +
                "the Lovakengj Assembly.",
        "quest_ascentofarceuus" to
            "Apparently there's been some commotion in the middle of Arceuus. Might be worth " +
                "checking out.",
        "quest_kingdomdivided" to
            "That protest outside Kourend Castle is starting to attract some attention. Could be " +
                "worth taking a look.",
    )
