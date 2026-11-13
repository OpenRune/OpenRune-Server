package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.blueMoonCook(quest: BelowIceMountain) {
    val recipe = quest.stage(player) >= BimStage.RECRUITING &&
        player.bimMarley in BimMarley.SANDWICH_REQUESTED until BimMarley.RECIPE_KNOWN
    val forgotten = quest.stage(player) >= BimStage.RECRUITING &&
        player.bimMarley in BimMarley.RECIPE_KNOWN until BimMarley.SANDWICH_GIVEN
    val hasSandwich = access.inv.count("obj.bim_steak_sandwich") > 0

    chatNpc(angry, "What do you want? I'm busy!")
    when {
        recipe -> {
            when (
                choice4(
                    "I was wondering if you'd be able to make me a Steak sandwich?",
                    1,
                    "Can you sell me any food?",
                    2,
                    "Can you give me any free food?",
                    3,
                    "I don't want anything from this horrible kitchen.",
                    4,
                )
            ) {
                1 -> steakSandwichRecipe(quest)
                2 -> sellFood()
                3 -> freeFood()
                else -> leaveKitchen()
            }
        }
        forgotten -> {
            when (
                choice4(
                    if (hasSandwich) {
                        "I've managed to make myself a Steak sandwich."
                    } else {
                        "I forgot the recipe to make a Steak sandwich."
                    },
                    1,
                    "Can you sell me any food?",
                    2,
                    "Can you give me any free food?",
                    3,
                    "I don't want anything from this horrible kitchen.",
                    4,
                )
            ) {
                1 -> if (hasSandwich) inspectSandwich() else forgottenRecipe()
                2 -> sellFood()
                3 -> freeFood()
                else -> leaveKitchen()
            }
        }
        else -> {
            when (
                choice3(
                    "Can you sell me any food?",
                    1,
                    "Can you give me any free food?",
                    2,
                    "I don't want anything from this horrible kitchen.",
                    3,
                )
            ) {
                1 -> sellFood()
                2 -> freeFood()
                else -> leaveKitchen()
            }
        }
    }
}

private suspend fun Dialogue.leaveKitchen() {
    chatPlayer(neutral, "I don't want anything from this horrible kitchen.")
    chatNpc(angry, "Suit yourself.")
}

private suspend fun Dialogue.steakSandwichRecipe(quest: BelowIceMountain) {
    chatPlayer(quiz, "I was wondering if you'd be able to make me a Steak sandwich?")
    chatNpc(
        neutral,
        "Hmm... we don't get many food orders in 'ere. Last person to order one of them was " +
            "barred for thievin'.",
    )
    chatPlayer(worried, "Oh... uhm... no. It's for me?")
    chatNpc(quiz, "Really?")
    chatPlayer(sad, "No... it's for Marley...")
    chatNpc(angry, "I knew it! I'm not making another meal for that thievin' git! Clear off.")
    chatPlayer(quiz, "At least tell me why you hate him so much?")
    chatNpc(
        neutral,
        "I know what it's like, not knowin' where your next meals going to come from, so I " +
            "let him eat 'ere for free.",
    )
    chatNpc(
        neutral,
        "The minute I turn my back, he's caught red-handed trying to pickpocket one of our " +
            "patrons. We gave him the boot there 'n' then.",
    )
    chatPlayer(
        neutral,
        "Well, fair enough, but it's just one sandwich. He's not even in Varrock anymore. " +
            "He's moved off to Edgeville.",
    )
    chatNpc(
        angry,
        "No chance mate, it's the principle. I won't make food for such a low class criminal...",
    )
    chatPlayer(
        worried,
        "Please, I really need Marley's help and he'll only offer it if I get him a Steak sandwich.",
    )
    chatNpc(
        neutral,
        "Well, I suppose if I give you the recipe, you can make it yourself.",
    )
    chatPlayer(happy, "Great! I can't imagine it's that hard to make.")
    chatNpc(angry, "Watch your mouth! Making a Steak sandwich is a delicate art!")
    chatPlayer(neutral, "Sorry! Of course it is. What's the recipe then?")
    chatNpc(
        neutral,
        "To start, you need to get some prime beef from a cow. The fields around Lumbridge are " +
            "a great source. Cook it up - medium rare is perfect. Don't cook it for too long " +
            "or you'll burn it...",
    )
    chatNpc(
        neutral,
        "Then, you take some bread. I recommend making it fresh, but to each their own. Slice " +
            "it in half and slide the beautifully cooked beef between the two halves...",
    )
    chatNpc(
        happy,
        "And hey presto, you've got yourself a delicious, perfectly made Steak sandwich.",
    )
    chatPlayer(quiz, "Wow... is that it? Some beef between a loaf of bread?")
    chatNpc(angry, "It's harder to make than it sounds! Now get out before I whack you one for " +
        "being so rude.")
    player.bimMarley = BimMarley.RECIPE_KNOWN
}

private suspend fun Dialogue.forgottenRecipe() {
    chatPlayer(quiz, "I forgot the recipe to make a Steak sandwich.")
    chatNpc(
        angry,
        "For Saradomin's sake! Take some bread, cut it in half and stick some cooked Steak " +
            "inside, done.",
    )
    chatPlayer(happy, "Oh yeah, thank you!")
}

private suspend fun Dialogue.inspectSandwich() {
    chatPlayer(quiz, "I've managed to make myself a Steak sandwich.")
    chatNpc(neutral, "Let me take a look at it.")
    objbox("obj.bim_steak_sandwich", "You show your Steak sandwich to the cook.")
    chatNpc(laugh, "Ha ha ha, you call this a sandwich!?")
    chatPlayer(worried, "Well, I tried my best...")
    chatNpc(neutral, "I'm sure you did. It'll be enough for Marley, anyway.")
}

private suspend fun Dialogue.sellFood() {
    chatPlayer(quiz, "Can you sell me any food?")
    chatNpc(neutral, "I've got some cabbage if you want it. One coin.")
    if (access.inv.count("obj.coins") < 1) {
        chatPlayer(sad, "I don't have any coins on me.")
        return
    }
    val buy = choice2("Yes please.", true, "No thanks.", false)
    if (!buy) {
        return
    }
    access.invDel(access.inv, "obj.coins", 1)
    access.invAdd(access.inv, "obj.cabbage", 1)
    objbox("obj.cabbage", "You buy a cabbage for 1 coin.")
}

private suspend fun Dialogue.freeFood() {
    chatPlayer(quiz, "Can you give me any free food?")
    chatNpc(angry, "No chance. I'm not running a charity.")
}
