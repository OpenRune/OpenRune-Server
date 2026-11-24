package org.rsmod.content.quest.mudfoot

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.quest.manager.Quest

private const val FOLLOWER_SPEAR = "npc.mba_mudfoot_follower_spear"

private suspend fun Dialogue.follower(anim: dev.openrune.types.MesAnimType, text: String) {
    chatNpcSpecific(MUDFOOT_TITLE, FOLLOWER_SPEAR, anim, text)
}

internal suspend fun ProtectedAccess.rangerDefeated(quest: Quest) {
    startDialogue {
        chatPlayer(quiz, "Right, that's the first one of those outlaws dealt with.")
        follower(
            neutral,
            "Yes, we do good work making them dead. Me do good work especially, but human " +
                "not bad either.",
        )
        chatPlayer(confused, "Thanks... So, what's next?")
        mageBriefing(quest)
    }
}

internal suspend fun Dialogue.mageBriefing(quest: Quest) {
    follower(
        neutral,
        "Other goblins tell me about next thief. They say she carry big stick that make air go " +
            "magic.",
    )
    chatPlayer(neutral, "So she's a mage. That means we'll want to use ranged against her.")
    follower(neutral, "Yes, that why me bring best ranged weapon of all: rocks.")
    chatPlayer(confused, "Rocks?")
    follower(neutral, "Me throw them hard. Harder me throw, more death happen.")
    chatPlayer(confused, "I see... I think I'll stick to a bow.")
    follower(
        neutral,
        "Yes, bow okay as well, but not as good as rocks. Maybe you use bow that first thief " +
            "had, or maybe guard man tell you how to get one.",
    )
    chatPlayer(quiz, "And once we're ready, where do we find this mage?")
    follower(neutral, "Other goblins say they see her by ruined building, west of shop.")
    if (quest.getQuestStage(player) < MudfootStage.MAGE_BRIEFED) {
        quest.setQuestStage(access, MudfootStage.MAGE_BRIEFED)
    }
    chatPlayer(quiz, "So directly south of here. In that case, shall we get going?")
    follower(neutral, "Okay. Me behind you.")
}

internal suspend fun ProtectedAccess.mageDefeated(quest: Quest) {
    startDialogue {
        follower(happy, "Another nasty thief go dead!")
        chatPlayer(confused, "She mentioned something called H.A.M.")
        follower(
            angry,
            "Humans Against Monsters! Nasty human group who think goblins monsters. We no " +
                "monsters... apart from maybe Dungface. He once kill chicken just because it lay " +
                "egg on his foot.",
        )
        chatPlayer(neutral, "I suppose this explains why they were trying to frame you.")
        follower(angry, "Me impossible to frame. Rattrap try it once, but he fail. Me too wriggly.")
        chatPlayer(confused, "What?")
        follower(
            angry,
            "Humans silly anyway. They no consider powerful hero like Mudfoot! Me on quest to " +
                "stop all the nasty humans.",
        )
        chatPlayer(
            quiz,
            "Well, on that note, just one left to go. What do you know about the last thief?",
        )
        warriorBriefing(quest)
    }
}

internal suspend fun Dialogue.warriorBriefing(quest: Quest) {
    follower(neutral, "Other goblins say this one great warrior, but not as great as me.")
    chatPlayer(neutral, "Meaning they're using melee, so we need to use magic.")
    follower(
        confused,
        "Magic? Me try that once. They say to cast spell you wave hands, but me just wave big " +
            "hammer instead. Work just as good.",
    )
    chatPlayer(quiz, "Are you sure?")
    follower(
        confused,
        "Yes. That why me stick with spear. This good for us though. Goblins say human do big " +
            "religion.",
    )
    chatPlayer(quiz, "Big religion?")
    follower(
        neutral,
        "He pray to his stupid god to help him. Means he protected. You do close up poke, he " +
            "pray to stop close up poke. You do far away throw, he pray to stop far away throw.",
    )
    follower(
        neutral,
        "But he stupid. He no have good god like goblins have Big High War God. His god have " +
            "short attention span and only listen to one prayer at time. If me use spear and you " +
            "use fancy magic, he can't stop both.",
    )
    chatPlayer(neutral, "I see. Sounds like we have a plan then.")
    follower(
        neutral,
        "Yes, best plan. You get yourself magic stick. Take one from dead magic woman, or maybe " +
            "guard man help. Then we kill last thief. Other goblins see him down in graveyard.",
    )
    if (quest.getQuestStage(player) < MudfootStage.WARRIOR_BRIEFED) {
        quest.setQuestStage(access, MudfootStage.WARRIOR_BRIEFED)
    }
    chatPlayer(quiz, "So south east of here. Shall we get going?")
    follower(neutral, "Okay. Me behind you.")
}

internal suspend fun ProtectedAccess.warriorDefeated(quest: Quest) {
    startDialogue {
        chatPlayer(happy, "That's the last of those thieves dealt with!")
        returnToHarlan(quest)
    }
}

internal suspend fun Dialogue.returnToHarlan(quest: Quest) {
    follower(
        neutral,
        "Stupid humans should never have crossed Mudfoot the good at quests. Guard man will want " +
            "to know about good job. He probably clap for us. Humans like clapping.",
    )
    if (quest.getQuestStage(player) < MudfootStage.RETURN_TO_HARLAN) {
        quest.setQuestStage(access, MudfootStage.RETURN_TO_HARLAN)
    }
    chatPlayer(quiz, "Shall we go tell him then?")
    follower(neutral, "Okay. Me behind you.")
}
