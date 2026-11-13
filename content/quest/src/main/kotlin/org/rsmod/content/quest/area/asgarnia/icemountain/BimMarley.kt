package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.marleyEdgeville(quest: BelowIceMountain) {
    when {
        player.bimMarley >= BimMarley.RECRUITED -> marleyRecruited(quest)
        hasSandwich() -> marleyHandIn(quest)
        player.bimMarley >= BimMarley.SANDWICH_REQUESTED -> marleyWaiting(quest)
        else -> marleyRequest(quest)
    }
}

private suspend fun Dialogue.marleyRequest(quest: BelowIceMountain) {
    val title = if (access.isBodyTypeA()) "sir" else "madam"
    chatPlayer(neutral, "Hey there. Would you happen to be Marley?")
    chatNpc(
        neutral,
        "I could be. Depends on who's asking? If you're with the Varrock City Guard, I had " +
            "nothing to do with the robbery at the museum. I don't even know what I'd do with " +
            "some Wizard robes.",
    )
    chatPlayer(confused, "Uhm, no? What robbery? Wizard robes?")
    chatNpc(shifty, "Robbery? Sorry, $title, first I've heard of a robbery.")
    chatPlayer(
        neutral,
        "Look, I'm not here with the city guard, Willow sent me to find you. She needs your help.",
    )
    chatNpc(confused, "Willow? Hmm... I'm afraid I don't recall a Willow.")
    chatPlayer(quiz, "Are you sure? She said you were old pals.")
    chatNpc(neutral, "Nope, I'm sure. Only Willow I know of is a type of tree.")
    chatNpc(
        neutral,
        "But, then again, my memory is a bit hazy, due to how hungry I am.",
    )
    chatPlayer(quiz, "You're too hungry... to remember your friend?")
    chatNpc(neutral, "Yep... been a while since I had a nice hot meal.")
    chatPlayer(
        neutral,
        "*Sighs*... would you like me to get you something to eat, to help jog your memory?",
    )
    chatNpc(happy, "Oh what a kind offer! I couldn't say no.")
    chatPlayer(neutral, "Fine, I'll be back in a...")
    chatNpc(
        worried,
        "Woah woah woah, hold your horses there friend! I can't just eat anything. I have " +
            "very specific dietary requirements.",
    )
    chatPlayer(neutral, "...")
    chatNpc(
        neutral,
        "Don't be like that! I don't want nothing fancy, just a Steak sandwich from the Blue " +
            "Moon Inn, in Varrock.",
    )
    chatPlayer(quiz, "Why don't you go and get one then?")
    player.bimMarley = BimMarley.SANDWICH_REQUESTED
    chatNpc(
        neutral,
        "Slight problem there, friend. I've been barred and the chef won't make me another " +
            "one. Something about stealing from punters. I can't remember...",
    )
    chatPlayer(
        neutral,
        "*Sighs*... Fine. If I go get you this sandwich, will you help Willow?",
    )
    chatNpc(happy, "Of course! Oh, by the way, don't bother with the bartender. Just talk to " +
        "the chef. Trust me.")
    chatPlayer(neutral, "Alright. I'll be back momentarily with your sandwich.")
}

private suspend fun Dialogue.marleyWaiting(quest: BelowIceMountain) {
    chatNpc(neutral, "Have you got the sandwich yet?")
    chatPlayer(neutral, "I'm still working on it. Where did you say you got them from?")
    chatNpc(happy, "The Blue Moon Inn, in Varrock. They have the best cook around!")
    chatPlayer(neutral, "I'll go speak to them, then.")
}

private suspend fun Dialogue.marleyHandIn(quest: BelowIceMountain) {
    val title = if (access.isBodyTypeA()) "sir" else "madam"
    chatPlayer(happy, "Hey Marley. I've managed to get your Steak sandwich.")
    chatNpc(happy, "Ah, just in time for my afternoon snack. Pass it over.")
    access.invDel(access.inv, "obj.bim_steak_sandwich", 1)
    objbox("obj.bim_steak_sandwich", "You give the steak sandwich to Marley.")
    player.bimMarley = BimMarley.SANDWICH_GIVEN
    chatNpc(happy, "Aaah... not quite as good as the usual, but it hit the spot.")
    chatPlayer(quiz, "Are you finally done?")
    chatNpc(
        happy,
        "Yes $title I am. I'll honour my side of the deal and head off to find Willow.",
    )
    chatPlayer(neutral, "Finally! You can find her on the west side of Ice Mountain.")
    chatNpc(happy, "Excellent! I'll be on my way then.")
    player.bimMarley = BimMarley.RECRUITED
    quest.markOneRecruited(player)
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
            "Marley is certainly an interesting character. Hopefully the next team member is " +
                "more respectable.",
        )
    }
}

private suspend fun Dialogue.marleyRecruited(quest: BelowIceMountain) {
    if (quest.stage(player) >= BimStage.COMPLETE) {
        chatPlayer(neutral, "Hello again Marley.")
        chatNpc(happy, "Ah, my new favourite chef. Have you got another sandwich for me?")
        chatPlayer(angry, "No!")
        chatNpc(worried, "Woah! Alright there, calm down. Was only a joke.")
        chatPlayer(neutral, "I don't find being your errand boy very funny.")
        chatNpc(neutral, "You need to lighten up a little, my friend.")
        return
    }
    chatPlayer(neutral, "Hello again Marley.")
    chatNpc(happy, "Ah, my new favourite chef. Have you got another sandwich for me?")
    chatPlayer(angry, "No!")
    chatNpc(worried, "Woah! Alright there, calm down. Was only a joke.")
    chatPlayer(neutral, "I don't find being your errand boy very funny.")
    chatNpc(neutral, "You need to lighten up a little, my friend.")
}

internal suspend fun Dialogue.marleyEntrance(quest: BelowIceMountain) {
    chatPlayer(neutral, "Hello again Marley.")
    chatNpc(happy, "Ah, my new favourite chef. Have you got another sandwich for me?")
    chatPlayer(angry, "No!")
    chatNpc(worried, "Woah! Alright there, calm down. Was only a joke.")
    chatPlayer(neutral, "I don't find being your errand boy very funny.")
    chatNpc(neutral, "You need to lighten up a little, my friend.")
}

internal suspend fun Dialogue.marleyPostquest(quest: BelowIceMountain) = marleyRecruited(quest)

private fun Dialogue.hasSandwich(): Boolean =
    access.inv.count("obj.bim_steak_sandwich") > 0
