package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.willowOutside(quest: BelowIceMountain) {
    when {
        quest.stage(player) == BimStage.NOT_STARTED -> startWillowQuest(quest)
        quest.stage(player) < BimStage.ONE_RECRUITED -> recruitingWillow(quest)
        quest.stage(player) < BimStage.ALL_RECRUITED -> recruitingWillow(quest)
        else -> chatNpc(happy, "Hurry up! It's not exactly exciting standing out here all day.")
    }
}

private suspend fun Dialogue.startWillowQuest(quest: BelowIceMountain) {
    chatPlayer(neutral, "Hello there. What are you doing all the way out here?")
    if (quest.questPoints(player) < REQUIRED_QUEST_POINTS) {
        chatNpc(neutral, "Nothing you need to worry about.")
        mesbox("You do not meet all of the requirements to start the Below Ice Mountain quest.")
        return
    }

    chatNpc(neutral, "Just... looking around.")
    chatPlayer(quiz, "Oh, did you lose something?")
    chatNpc(neutral, "No... I found something. A door.")
    chatPlayer(quiz, "A door? I see doors all the time. What makes this one so special?")
    chatNpc(
        neutral,
        "It's buried beneath a tonne of rocks. Could be the entrance to some ancient ruins...",
    )
    chatPlayer(happy, "Wow! Ancient ruins? Are you an archaeologist or something?")
    chatNpc(confused, "Archaeologist? Me? Hmm...")
    chatNpc(
        happy,
        "Of course I am! I'm the greatest archaeologist of our time. The name's Willow! " +
            "Have you not heard of me?",
    )
    chatPlayer(neutral, "I'm afraid I haven't. Are you looking for a way into the ruins?")
    chatNpc(neutral, "You bet I am, but I don't think there's any other way in.")
    chatPlayer(quiz, "Oh dear. Do you have another plan to get in?")
    chatNpc(
        worried,
        "Potentially, but I won't be able to do it on my own. Nor can I leave the area. If " +
            "I'm not here to guard the entrance, some other archaeologist might come along " +
            "and steal the glory from me!",
    )
    chatNpc(happy, "Maybe you could give me a hand? There could be untold riches inside the ruins!")

    val start =
        choice2(
            "Yes.",
            true,
            "No.",
            false,
            title = "Start the Below Ice Mountain quest?",
        )
    if (!start) {
        chatPlayer(neutral, "Seems like you're in a bit of a pickle. Have fun with that.")
        return
    }

    quest.setStage(access, BimStage.STARTED)
    chatPlayer(
        happy,
        "A chance to uncover ancient ruins doesn't happen every day! Of course I'll help! " +
            "I'm ${player.displayName} by the way. How do you plan to get in?",
    )
    chatNpc(
        neutral,
        "The entrance is covered in rocks and rubble - I don't think mining them will work.",
    )
    chatNpc(
        neutral,
        "We'll need something stronger. Who knows what could be inside the ruins. There " +
            "could be traps, or even monsters!",
    )
    chatNpc(neutral, "No, I think we'll need to get my old crew...")
    chatNpc(
        neutral,
        "...I mean, excavation team together. But you'll need to convince them to help us. " +
            "We didn't part on the best of terms.",
    )
    chatPlayer(quiz, "What happened?")
    quest.setStage(access, BimStage.HEARD_OF_CREW)
    chatNpc(neutral, "We had a falling out after a, uhm... dig, went wrong.")
    chatPlayer(quiz, "Where can I find your old team, and how will they help?")
    chatNpc(neutral, "To get into the ruins we'll need to blow up all the rocks blocking the entrance.")
    chatPlayer(
        worried,
        "That doesn't sound too safe. What if we damage the ruins? Surely it's better to go " +
            "slower and preserve as much as we can?",
    )
    chatNpc(
        neutral,
        "No! We need to get in there as soon as possible! Burntof the dwarf will do it. " +
            "He's a master with explosives.",
    )
    chatNpc(
        neutral,
        "From what I can remember, he's a fan of his drink. His favourite pub being the " +
            "Rising Sun Inn, in Falador - I'd look there first.",
    )
    chatNpc(
        neutral,
        "After we've blown the rocks away, we'll need someone to move all the rubble. " +
            "Someone strong.",
    )
    chatPlayer(happy, "I could do it! I'm quite strong myself.")
    val strong = if (access.isBodyTypeA()) "strongman" else "strongwoman"
    chatNpc(
        laugh,
        "Ha ha ha! I don't think so. We need a real $strong - someone with muscles. An old " +
            "friend of mine, Checkal, should be able to do all the heavy lifting.",
    )
    chatPlayer(neutral, "Oh okay... Where can I find him?")
    chatNpc(
        neutral,
        "Hmm... We had quite a rough falling out when we last spoke, but I heard he's " +
            "hanging around Barbarian Village.",
    )
    chatNpc(
        neutral,
        "Lastly, if my hunch is correct, this place will be covered in traps. We'll need my " +
            "pal Marley to disarm them. He's the best thief I know. Been doing it for years.",
    )
    chatNpc(
        neutral,
        "I have no idea where he is these days. But his cousin Charlie in Varrock may know. " +
            "You should talk to him. He's normally somewhere near the Blue Moon Inn.",
    )
    chatPlayer(
        quiz,
        "You have a rather interesting group of friends... but if they get us into the ruins, " +
            "I'm all for it.",
    )
    quest.setStage(access, BimStage.RECRUITING)
    chatNpc(
        happy,
        "Good luck. Once you've recruited one of the team, I'll meet them at the entrance. " +
            "It's on the west side of the mountain.",
    )
}

private suspend fun Dialogue.recruitingWillow(quest: BelowIceMountain) {
    chatNpc(neutral, "Back again? What can I help you with?")
    while (true) {
        val choice =
            choice5(
                "Where can I find Marley?",
                1,
                "Where can I find Checkal?",
                2,
                "Where can I find Burntof?",
                3,
                "Where can I find the entrance to the ruins?",
                4,
                "Nothing, thanks.",
                5,
            )
        when (choice) {
            1 -> {
                chatPlayer(quiz, "Where can I find Marley?")
                chatNpc(
                    neutral,
                    "Marley is an elusive one, to be honest. I have no idea where to find him. " +
                        "You should try talk to his cousin Charlie near the Blue Moon Inn in " +
                        "Varrock. Maybe he will know.",
                )
                chatNpc(neutral, "Need help finding anyone else?")
            }
            2 -> {
                chatPlayer(quiz, "Where can I find Checkal?")
                chatNpc(
                    neutral,
                    "Last I heard Checkal was hanging around Barbarian Village. Maybe you " +
                        "should head there to find him?",
                )
                chatNpc(neutral, "Need help finding anyone else?")
            }
            3 -> {
                chatPlayer(quiz, "Where can I find Burntof?")
                chatNpc(neutral, "Burntof likes to hang out at the Rising Sun Inn located in Falador.")
                chatNpc(neutral, "Need help finding anyone else?")
            }
            4 -> {
                chatPlayer(quiz, "Where can I find the entrance to the ruins?")
                chatNpc(
                    neutral,
                    "On the western side of Ice Mountain. There's no way up on this side so " +
                        "you'll need to head east of here first and go around the dwarf colony " +
                        "to get there.",
                )
                chatPlayer(neutral, "Okay.")
            }
            else -> {
                chatPlayer(neutral, "Nothing, thanks.")
                chatNpc(
                    neutral,
                    "Hurry up! It's not exactly exciting standing out here all day.",
                )
                return
            }
        }
    }
}

internal suspend fun Dialogue.willowEntrance(quest: BelowIceMountain) {
    if (!quest.hasAllRecruits(player)) {
        chatNpc(neutral, "Have you recruited everyone yet? Hurry up, we haven't got all day!")
        return
    }
    chatPlayer(neutral, "That's everyone gathered. Interesting company for an archaeologist to keep...")
    chatNpc(neutral, "*Shrugs* How many archaeologists do you actually know?")
    chatPlayer(neutral, "Well I-")
    chatNpc(
        neutral,
        "Exactly. So who are you to judge? Anyway, we're all here and ready to break in... " +
            "Um, I mean excavate... the entrance.",
    )
    chatNpc(neutral, "Are you ready? Who knows what's inside the ruins?")
    val ready = choice2("Yes.", true, "No.", false)
    if (!ready) {
        chatPlayer(neutral, "I need a little bit more time to prepare.")
        chatNpc(neutral, "Hurry up then! We've waited long enough.")
        return
    }
    chatPlayer(neutral, "I'm ready as I'll ever be. Let's head in.")
    player.bimStage = BimStage.ENTRANCE_CUTSCENE
}

internal suspend fun Dialogue.willowPostquest(quest: BelowIceMountain) {
    chatPlayer(quiz, "So this is where you scampered off to after the monster awoke?")
    chatNpc(neutral, "Look ${player.displayName}, it was a simple misunderstanding, that's all...")
    chatPlayer(angry, "Oh really? You threatening to kill me was just a misunderstanding?")
    chatNpc(
        worried,
        "Things got a little heated down there, but we all made it out alive, right?",
    )
    chatPlayer(angry, "Barely... I had to defeat that monster on my own!")
    chatPlayer(quiz, "What are you doing out here, anyway?")
    chatNpc(
        neutral,
        "We thought you'd rat us out to the authorities, so we came out here to hide.",
    )
    chatNpc(
        neutral,
        "Plus, we heard there are some sealed tombs in the desert that haven't been touched " +
            "in hundreds of years...",
    )
    chatPlayer(neutral, "Ah, of course... scoping out your next grave to rob.")
}

private const val REQUIRED_QUEST_POINTS = 16
