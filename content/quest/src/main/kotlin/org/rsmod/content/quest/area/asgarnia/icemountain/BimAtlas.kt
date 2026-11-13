package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.atlas(quest: BelowIceMountain, workout: BimWorkout) {
    val checkal = player.bimCheckal
    when {
        checkal in BimCheckal.SENT_TO_ATLAS until BimCheckal.TRAINING -> atlasTrainingOffer(quest, workout)
        checkal in BimCheckal.TRAINED until BimCheckal.FLEX_PROMPT -> atlasAfterTraining()
        checkal >= BimCheckal.TRAINING && checkal < BimCheckal.TRAINED -> atlasBusy()
        quest.stage(player) >= BimStage.COMPLETE -> atlasPaidWorkout(quest, workout)
        else -> atlasBusy()
    }
}

private suspend fun Dialogue.atlasBusy() {
    val title = if (access.isBodyTypeA()) "man" else "woman"
    chatNpc(neutral, "What do you want, little $title?")
    chatPlayer(neutral, "Nothing. Just though I'd try to strike up a conversation.")
    chatNpc(neutral, "Not now little one, I'm busy.")
}

private suspend fun Dialogue.atlasTrainingOffer(quest: BelowIceMountain, workout: BimWorkout) {
    val title = if (access.isBodyTypeA()) "man" else "woman"
    chatNpc(neutral, "What do you want, little $title?")
    chatPlayer(
        neutral,
        "I've just come to talk to you. Checkal said you could train me. He said you're the " +
            "strongest barbarian he knows.",
    )
    chatNpc(quiz, "Oh did he now? What makes you think I will train you?")
    chatPlayer(worried, "I... Um...")
    chatNpc(neutral, "Come on then! Out with it already!")
    chatPlayer(worried, "Well uh, he said he wouldn't...")
    chatNpc(happy, "I'm just pulling your leg! Any friend of Checkal is a friend of mine.")
    chatPlayer(neutral, "Please don't scare me like that.")
    chatNpc(
        happy,
        "It's a force of habit. It comes with the barbarian lifestyle. But to apologise, I'll " +
            "give you a free lesson!",
    )
    chatPlayer(quiz, "Oh really? How much would this usually cost?")
    chatNpc(neutral, "Around 25,000 coins per session.")
    chatPlayer(worried, "That's a lot of money!")
    chatNpc(
        neutral,
        "No price is too much for a good workout. Are you ready to start your training session?",
    )
    val yes = choice2("Yes.", true, "No.", false)
    if (!yes) {
        chatPlayer(neutral, "Not right now, I am a bit busy.")
        chatNpc(neutral, "No problem. Come back when you are ready.")
        return
    }
    player.bimCheckal = BimCheckal.TRAINING
    workout.play(access)
}

private suspend fun Dialogue.atlasAfterTraining() {
    chatNpc(
        neutral,
        "My work is done. Return to Checkal and show him your new-found muscles.",
    )
}

private suspend fun Dialogue.atlasPaidWorkout(quest: BelowIceMountain, workout: BimWorkout) {
    val count = player.bimWorkoutCount
    if (count == 0) {
        chatNpc(happy, "Well done, little one. You've proven your worth.")
        chatPlayer(quiz, "Can I do it again?")
        chatNpc(confused, "What? Why?")
        chatPlayer(happy, "It was fun, I'd like to do it again.")
        chatNpc(
            neutral,
            "You're the first person to ask me that ${player.displayName}. I suppose I could " +
                "train you again, but I will have to charge you the full 25,000 coins this time.",
        )
    } else if (count == 1) {
        chatNpc(happy, "Back again I see, here for another workout?")
        chatPlayer(happy, "Yes please.")
        chatNpc(
            neutral,
            "You must take your training very seriously ${player.displayName}. This is the " +
                "second time you'll have done this workout and paid full price!",
        )
    } else {
        chatNpc(happy, "Back again I see, here for another workout?")
        chatPlayer(happy, "Yes please.")
        chatNpc(
            neutral,
            "You take your training very seriously ${player.displayName}. You know you've done " +
                "this workout $count times, are you sure you want to do it again?",
        )
    }

    val pay =
        choice2(
            "Yes",
            true,
            "No",
            false,
            title = "Pay Atlas 25,000 coins to train again?",
        )
    if (!pay) {
        return
    }
    chatPlayer(happy, "Let's do this, I'm ready.")
    if (access.inv.count("obj.coins") < WORKOUT_PRICE) {
        chatNpc(
            neutral,
            "Hold it there little one, you don't have enough money to pay for the lesson.",
        )
        chatPlayer(worried, "Oh, sorry I was too excited...")
        chatNpc(neutral, "No worries friend, come back when you can afford the session.")
        return
    }
    val transaction =
        access.player.invTransaction(access.inv, autoCommit = false) {
            delete(select(access.inv), "obj.coins".asRSCM(RSCMType.OBJ), WORKOUT_PRICE)
        }
    if (transaction.failure) {
        return
    }
    transaction.commitAll()
    chatNpc(happy, "It's not going to be any easier my friend!")
    player.bimWorkoutCount = count + 1
    workout.play(access)
}

private const val WORKOUT_PRICE = 25_000
