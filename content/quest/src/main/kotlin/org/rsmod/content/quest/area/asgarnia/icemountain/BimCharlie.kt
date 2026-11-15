package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.charlieTramp(quest: BelowIceMountain) {
    chatNpc(neutral, "Spare some change guv?")
    if (quest.stage(player) >= BimStage.RECRUITING && player.bimMarley < BimMarley.RECRUITED) {
        val choice =
            choice5(
                "I'm looking for a man named Marley. Have you seen him?",
                1,
                "Who are you?",
                2,
                "Sorry, I haven't got any.",
                3,
                "Ok. Here you go.",
                4,
                "Is there anything down this alleyway?",
                5,
            )
        when (choice) {
            1 -> marleyQuestion()
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "Charles. Charles E. Trampin' at your service. Now, about that change you " +
                        "were going to give me...",
                )
            }
            3 -> {
                chatPlayer(sad, "Sorry, I haven't got any.")
                chatNpc(neutral, "Thanks anyway.")
            }
            4 -> giveCoin()
            else -> alleyway()
        }
        return
    }

    when (
        choice5(
            "Who are you?",
            1,
            "Sorry, I haven't got any.",
            2,
            "Go get a job!",
            3,
            "Ok. Here you go.",
            4,
            "Is there anything down this alleyway?",
            5,
        )
    ) {
        1 -> {
            chatPlayer(quiz, "Who are you?")
            chatNpc(
                neutral,
                "Charles. Charles E. Trampin' at your service. Now, about that change you were " +
                    "going to give me...",
            )
        }
        2 -> {
            chatPlayer(sad, "Sorry, I haven't got any.")
            chatNpc(neutral, "Thanks anyway.")
        }
        3 -> {
            chatPlayer(angry, "Go get a job!")
            chatNpc(angry, "You startin? I hope your nose falls off!")
        }
        4 -> giveCoin()
        else -> alleyway()
    }
}

private suspend fun Dialogue.marleyQuestion() {
    chatPlayer(quiz, "I'm looking for a man named Marley. Have you seen him?")
    chatNpc(neutral, "Ah Marley, my cousin. He's always getting himself into trouble!")
    chatPlayer(quiz, "Oh really? Do you know where I could find him?")
    chatNpc(
        neutral,
        "Hmm... it's been a while since I've seen him. Last I heard he got caught in the middle " +
            "of a robbery and had to flee Varrock.",
    )
    chatNpc(
        neutral,
        "Something to do with trying to steal a set of Wizard robes from the museum.",
    )
    chatPlayer(neutral, "Great, so I'm dealing with a criminal!")
    chatNpc(
        neutral,
        "Afraid so. Marley is no stranger to the authorities... Knowing him, he's probably " +
            "hanging out in Edgeville ruins till the attention dies down. I'd try there.",
    )
    chatPlayer(happy, "Perfect. I'll go look for him.")
}

private suspend fun Dialogue.giveCoin() {
    if (access.inv.count("obj.coins") < 1) {
        chatPlayer(sad, "Oh wait. I haven't got any, sorry.")
        chatNpc(neutral, "Thanks anyway.")
        return
    }
    chatPlayer(happy, "Ok. Here you go.")
    access.invDel(access.inv, "obj.coins", 1)
    mesbox("You give Charlie a coin.")
    chatNpc(happy, "Hey, thanks a lot!")
    val hint = choice2("No problem.", true, "Don't I get some sort of quest hint or something?", false)
    if (hint) {
        chatPlayer(neutral, "No problem.")
    } else {
        chatPlayer(quiz, "So...don't I get some sort of quest hint or something now?")
        chatNpc(neutral, "Huh? What do you mean? That wasn't why I asked you for money.")
        chatNpc(sad, "I just need to eat...")
    }
}

private suspend fun Dialogue.alleyway() {
    chatPlayer(quiz, "Is there anything down this alleyway?")
    chatNpc(neutral, "Funny you should mention that...there is actually.")
    chatNpc(
        neutral,
        "The ruthless and notorious criminal gang known as the Black Arm Gang have their " +
            "headquarters down there.",
    )
    val thanks = choice2("Thanks for the warning!", true, "Do you think they would let me join?", false)
    if (thanks) {
        chatPlayer(neutral, "Thanks for the warning!")
        chatNpc(neutral, "Don't worry about it.")
        chatPlayer(quiz, "Do you think they would let me join?")
    } else {
        chatPlayer(quiz, "Do you think they would let me join?")
    }
    chatNpc(
        neutral,
        "You never know. You'll find a lady down there called Katrine. Speak to her.",
    )
    chatNpc(neutral, "But don't upset her, she's pretty dangerous.")
}
