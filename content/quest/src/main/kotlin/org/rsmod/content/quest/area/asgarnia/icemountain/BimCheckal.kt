package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.checkalBarbarian(quest: BelowIceMountain) {
    val checkal = player.bimCheckal
    when {
        checkal >= BimCheckal.RECRUITED -> checkalPostquest(quest)
        checkal >= BimCheckal.FLEX_PROMPT -> checkalFlexReminder()
        checkal >= BimCheckal.TRAINED -> checkalTrained(quest)
        checkal >= BimCheckal.SENT_TO_ATLAS -> checkalReminder(quest)
        else -> checkalRecruit(quest)
    }
}

private suspend fun Dialogue.checkalRecruit(quest: BelowIceMountain) {
    val title = if (access.isBodyTypeA()) "sir" else "madam"
    val lad = if (access.isBodyTypeA()) "lad" else "lass"
    val strongman = if (access.isBodyTypeA()) "strongman" else "strongwoman"

    chatPlayer(neutral, "Hello there. I'm ${player.displayName} and I'm looking for a man named Checkal.")
    chatNpc(neutral, "That would be me $title. What can I do for you?")
    chatPlayer(neutral, "I was told to come find you. Willow has found a new digsi...")
    chatNpc(angry, "Stop right there $title. You can tell Willow she can do the heavy lifting herself.")
    chatPlayer(quiz, "And why is that?")
    chatNpc(
        angry,
        "Willow does not appreciate the amount of work that goes into building my physique. " +
            "I would not help her lift a shrimp.",
    )
    chatNpc(
        angry,
        "I take great pride in these muscles and it hurts me to the core to hear such insults!",
    )
    chatPlayer(
        neutral,
        "But we need your help! She's found a new archaeological digsite and needs you to " +
            "help clear the entrance.",
    )
    chatNpc(quiz, "Archaeological digsite? Why is Willow interested in starting a digsite?")
    chatPlayer(
        happy,
        "She said there could be some ancient ruins, even an untouched hoard of gold and treasure!",
    )
    chatNpc(sad, "Ah that makes sense. *Sighs* You poor $lad...")
    chatPlayer(quiz, "What do you mean?")
    chatNpc(neutral, "Nothing. Look, I will help you. But first you must prove your strength to me.")

    if (access.statBase("stat.strength") >= 99) {
        chatPlayer(
            happy,
            "Well Checkal, I don't mean to brag, but there are few people in Gielinor " +
                "stronger than me.",
        )
        chatNpc(quiz, "Hmm... I will see about that.")
        chatNpc(neutral, "Yes... you do have tremendous muscle mass.")
        chatNpc(neutral, "Your legs are strong as steel, too.")
        chatNpc(neutral, "Well ${player.displayName}, I have to say you are the definition of strength.")
        chatPlayer(happy, "I do try.")
        chatNpc(
            neutral,
            "But a $strongman doesn't stop when they reach the top. They always push themselves.",
        )
    } else {
        chatPlayer(quiz, "How can I do that?")
    }

    player.bimCheckal = BimCheckal.SENT_TO_ATLAS
    chatNpc(
        neutral,
        "I would ask you to train with my old friend and trainer, Atlas, one of the strongest " +
            "barbarians I know. You can find him in the hall north of here.",
    )
    chatNpc(
        neutral,
        "If you can survive his rigorous training program, I shall offer you my help.",
    )
    chatPlayer(quiz, "How do you know he'll train me?")
    chatNpc(neutral, "Tell him I sent you and he will train you.")
    chatPlayer(neutral, "Alright then, I'll go speak to Atlas. Farewell.")
}

private suspend fun Dialogue.checkalReminder(quest: BelowIceMountain) {
    chatPlayer(quiz, "I seem to have forgotten where Atlas is. Can you remind me?")
    chatNpc(neutral, "Atlas can be found in the hall to the north of us.")
    chatPlayer(neutral, "Thanks.")
    chatNpc(happy, "No problem at all.")
}

private suspend fun Dialogue.checkalTrained(quest: BelowIceMountain) {
    chatNpc(neutral, "So have you completed your training session with Atlas?")
    chatPlayer(neutral, "Just about, but it was a real struggle.")
    chatNpc(
        happy,
        "It always is, but I can see you are standing taller and looking stronger already.",
    )
    chatNpc(
        neutral,
        "Now, show me your muscles and I will judge if you're ready to receive my help.",
    )
    player.bimCheckal = BimCheckal.FLEX_PROMPT
    mesbox("Show your strength by <col=ef1020>flexing</col> your muscles.")
}

private suspend fun Dialogue.checkalFlexReminder() {
    chatPlayer(quiz, "What did you want me to do again?")
    chatNpc(neutral, "I want you to prove your strength by showing me your muscles!")
    mesbox("Show your strength by <col=ef1020>flexing</col> your muscles.")
}

internal suspend fun Dialogue.checkalPostFlex(quest: BelowIceMountain) {
    val title = if (access.isBodyTypeA()) "man" else "lady"
    chatNpc(
        happy,
        "My goodness, those muscles are exquisite. I will definitely help a $title who " +
            "shows that much dedication to their physique.",
    )
    chatPlayer(
        happy,
        "Fantastic! Willow said she'd meet you by the entrance, on the west side of Ice Mountain.",
    )
    chatNpc(neutral, "Thank you my friend, I will meet you there.")
    recruitCrewFade {
        player.bimCheckal = BimCheckal.RECRUITED
        quest.markOneRecruited(player)
    }
    if (quest.hasAllRecruits(player)) {
        quest.markAllRecruited(player)
        chatPlayer(
            neutral,
            "Well, that's everyone. I guess I should head off the entrance on the west side of " +
                "Ice Mountain.",
        )
    } else {
        chatPlayer(
            neutral,
            "What an exhausting workout. I hope the others aren't as much effort to recruit.",
        )
    }
}

internal suspend fun Dialogue.checkalEntrance(quest: BelowIceMountain) {
    val strongman = if (access.isBodyTypeA()) "strongman" else "strongwoman"
    chatPlayer(quiz, "Do you think you'll be able to handle all these rocks?")
    chatNpc(
        happy,
        "Should not be a problem. This challenge is easy for a $strongman.",
    )
    chatPlayer(neutral, "I wish I could help...")
    chatNpc(
        neutral,
        "Do not worry my friend. If you stay dedicated and keep working everyday, you'll be " +
            "able to achieve anything you dream of.",
    )
    chatPlayer(happy, "Thanks Checkal, I will.")
    chatNpc(
        neutral,
        "One last thing - be careful when we go inside. You never know what will happen.",
    )
}

internal suspend fun Dialogue.checkalPostquest(quest: BelowIceMountain) {
    chatNpc(happy, "${player.displayName}, I am so glad to see you are safe.")
    chatPlayer(quiz, "Hello, Checkal. Why didn't you tell me about Willow?")
    chatPlayer(sad, "I thought we were friends...")
    chatNpc(
        sad,
        "I am sorry ${player.displayName}. I do respect you and your love for the muscle. " +
            "But sadly I needed the money. These muscles don't pay the bills on their own.",
    )
    chatPlayer(neutral, "Oh, I understand...")
    chatNpc(
        happy,
        "Please do not let this incident divert you from your path of becoming a strongman! " +
            "I truly believe in you.",
    )
}

internal suspend fun Dialogue.checkalAfterFlex(quest: BelowIceMountain) = checkalPostFlex(quest)

internal suspend fun Dialogue.recruitCrewFade(recruit: () -> Unit) {
    access.fadeOverlay(0, FADE_CLEAR, 0, FADE_BLACK, RECRUIT_FADE_DURATION)
    access.delay(RECRUIT_FADE_TICKS)
    recruit()
    access.fadeOverlay(0, FADE_BLACK, 0, FADE_CLEAR, RECRUIT_FADE_DURATION)
    access.delay(RECRUIT_FADE_TICKS)
    access.closeFadeOverlayNow()
}

private const val RECRUIT_FADE_DURATION = 50

private const val RECRUIT_FADE_TICKS = 2

private const val FADE_CLEAR = 255

private const val FADE_BLACK = 0
