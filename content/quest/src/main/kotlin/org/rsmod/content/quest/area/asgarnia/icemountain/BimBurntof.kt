package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

private val DRINKS =
    listOf("obj.beer", "obj.asgarnian_ale", "obj.dwarven_stout", "obj.wizards_mind_bomb")

internal suspend fun Dialogue.burntof(quest: BelowIceMountain, rps: BimRps) {
    when {
        player.bimBurntof >= BimBurntof.RECRUITED -> burntofRecruited(quest)
        player.bimBurntof >= BimBurntof.RPS_ACCEPTED -> {
            chatNpc(neutral, "Ready to play rock paper scissorsh?")
            rps.play(this, quest)
        }
        player.bimBurntof >= BimBurntof.DRINK_GIVEN -> {
            chatNpc(happy, "Yesh I am *hic*. What can thish dwarf do for you?")
            burntofWillow(quest, rps)
        }
        player.bimBurntof >= BimBurntof.DRINK_REQUESTED -> burntofWantsDrink(quest, rps)
        else -> burntofFirstMeeting(quest, rps)
    }
}

private suspend fun Dialogue.burntofFirstMeeting(quest: BelowIceMountain, rps: BimRps) {
    chatNpc(drunk, "Buy ush a drink.")
    chatPlayer(quiz, "Are you Burntof?")
    chatNpc(drunk, "I might be, I might not. Buy ush a drink and find out *hic*.")
    val drink = takeDrink()
    if (drink == null) {
        chatPlayer(neutral, "Fine... I'll get you a beer.")
        chatNpc(drunk, "Thanksh a bunch.")
        player.bimBurntof = BimBurntof.DRINK_REQUESTED
        return
    }
    chatPlayer(happy, "It just so happens I've got a drink you can have.")
    chatNpc(drunk, "Giv' it 'ere then.")
    giveDrink(drink)
    chatPlayer(quiz, "So, are you Burntof?")
    player.bimBurntof = BimBurntof.DRINK_GIVEN
    burntofWillow(quest, rps)
}

private suspend fun Dialogue.burntofWantsDrink(quest: BelowIceMountain, rps: BimRps) {
    chatNpc(drunk, "Where'sh my beer?")
    val drink = takeDrink()
    if (drink == null) {
        chatPlayer(neutral, "I'll get right on it.")
        return
    }
    chatPlayer(neutral, "Here you go.")
    giveDrink(drink)
    chatPlayer(quiz, "So, are you Burntof?")
    player.bimBurntof = BimBurntof.DRINK_GIVEN
    burntofWillow(quest, rps)
}

private suspend fun Dialogue.burntofWillow(quest: BelowIceMountain, rps: BimRps) {
    chatPlayer(neutral, "Willow sent me. She needs your help with a job.")
    chatNpc(angry, "Willow? Now that'sh a name I drink to forget!")
    chatPlayer(quiz, "What do you mean?")
    chatNpc(
        drunk,
        "Willow alwaysh insishted on changing me waysh. I knows explosivesh. Just because I " +
            "lost a couple of me fingersh a few yearsh back doesn't mean I'll do it again *hic*.",
    )
    chatNpc(
        drunk,
        "I knew the exhact amount of dyn*hic*amite needed to blow that rich fool's shafe. " +
            "We woulda been shet for life!",
    )
    chatPlayer(quiz, "I thought you were a team of archaeologists?")
    chatNpc(drunk, "Archaeologishts? Shure, if that's what you want to call ush?!")
    chatPlayer(neutral, "Willow sent me to get you. She's found a new digsite and needs your " +
        "expert knowledge on explosives.")
    chatNpc(drunk, "Hmmm... *hic*")
    chatNpc(
        neutral,
        "Tell you what. If you beat me in a friendly game of rock paper s*hic*cissorsh, " +
            "I'll join the team.",
    )
    chatNpc(neutral, "*Whispers* We'll have the game upshtairs away from everyone.")
    player.bimBurntof = BimBurntof.RPS_ACCEPTED
    rps.play(this, quest)
}

private fun Dialogue.takeDrink(): String? {
    for (drink in DRINKS) {
        if (access.inv.count(drink) > 0) {
            access.invDel(access.inv, drink, 1)
            return drink
        }
    }
    return null
}

private suspend fun Dialogue.giveDrink(drink: String) {
    val text =
        when (drink) {
            "obj.dwarven_stout" ->
                "Ah! A ${if (access.isBodyTypeA()) "lad" else "lass"} of fhine taste *hic*. " +
                    "I love Dwarven shtout!"
            "obj.wizards_mind_bomb" ->
                "No messhing around with you! Only the strong stuff, I like it!"
            else -> "That'sh good shtuff."
        }
    objbox(drink, "You give Burntof the drink.")
    chatNpc(drunk, text)
}

private suspend fun Dialogue.burntofRecruited(quest: BelowIceMountain) {
    if (quest.stage(player) >= BimStage.COMPLETE) {
        chatPlayer(quiz, "You managed to make your way over to the ruins then?")
        chatNpc(
            drunk,
            "Yush, I did, why's that shuch a *hic* shurprise?",
        )
        chatPlayer(neutral, "I thought you would pass out on your way.")
        chatNpc(
            drunk,
            "Dwarshves have a very good shense of direction and we can *hic* hold our beer " +
                "very well.",
        )
        chatPlayer(quiz, "Are you sure you'll be able to handle the dynamite in your current state?")
        chatNpc(
            drunk,
            "Hah! I could blow thish rubble away with one hand and me eyesh closed.",
        )
        return
    }
    chatPlayer(quiz, "You managed to make your way over to the ruins then?")
    chatNpc(drunk, "Yush, I did, why's that shuch a *hic* shurprise?")
    chatPlayer(neutral, "I thought you would pass out on your way.")
    chatNpc(
        drunk,
        "Dwarshves have a very good shense of direction and we can *hic* hold our beer very well.",
    )
    chatPlayer(quiz, "Are you sure you'll be able to handle the dynamite in your current state?")
    chatNpc(drunk, "Hah! I could blow thish rubble away with one hand and me eyesh closed.")
}

internal suspend fun Dialogue.burntofEntrance(quest: BelowIceMountain) {
    chatPlayer(quiz, "You managed to make your way over to the ruins then?")
    chatNpc(drunk, "Yush, I did, why's that shuch a *hic* shurprise?")
    chatPlayer(neutral, "I thought you would pass out on your way.")
    chatNpc(
        drunk,
        "Dwarshves have a very good shense of direction and we can *hic* hold our beer very well.",
    )
    chatPlayer(quiz, "Are you sure you'll be able to handle the dynamite in your current state?")
    chatNpc(drunk, "Hah! I could blow thish rubble away with one hand and me eyesh closed.")
}

internal suspend fun Dialogue.burntofPostquest(quest: BelowIceMountain) =
    burntofRecruited(quest)
