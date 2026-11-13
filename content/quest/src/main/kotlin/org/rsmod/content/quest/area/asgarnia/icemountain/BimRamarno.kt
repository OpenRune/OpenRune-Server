package org.rsmod.content.quest.area.asgarnia.icemountain

import org.rsmod.api.player.dialogue.Dialogue

internal suspend fun Dialogue.ramarnoEntrance(quest: BelowIceMountain) {
    chatPlayer(neutral, "Wow. I honestly didn't expect to see a dwarf in these ruins.")
    chatNpc(shocked, "I can't believe it...")
    chatPlayer(neutral, "Yeah, exactly.")
    chatNpc(sad, "It's been so long since I've heard another person's voice.")
    chatNpc(quiz, "How did you find me?")
    chatPlayer(neutral, "To be honest, it was all a bit of an accident.")
    chatNpc(quiz, "What do you mean?")
    chatPlayer(
        neutral,
        "There was a gang of... well, I thought were archaeologists, but it turned out they " +
            "were grave robbers.",
    )
    chatPlayer(
        neutral,
        "I helped them clear the entrance to these ruins and we worked our way inside, only to " +
            "wake up a monster! They ran off and left me to defeat it.",
    )
    chatNpc(neutral, "That wasn't a monster...")
    chatPlayer(quiz, "Well it looked menacing enough!")
    chatNpc(neutral, "That was one of the guardians of this once great city.")
    chatPlayer(quiz, "City?")
    chatNpc(happy, "Yes! Camdozaal, home of the Imcando Dwarves.")
    chatNpc(
        neutral,
        "These halls once bustled with life, but that was a long time ago. I'm the only one " +
            "left now. The name's Ramarno.",
    )
    chatPlayer(
        happy,
        "Well Ramarno, I'm ${player.displayName}, and you have me at a loss for words! I'd " +
            "have never even imagined there would be a whole city down here.",
    )
    chatNpc(neutral, "Not much of a city anymore, I suppose.")
    chatPlayer(
        sad,
        "I guess I didn't help with that. Sorry about breaking into your home and killing your " +
            "guardian.",
    )
    chatNpc(
        neutral,
        "We have plenty more. No real harm done. That said, I do think I might have a way for " +
            "you to make up for it. There'll likely be something in it for you as well.",
    )
    chatNpc(neutral, "My workshop is just north of here. Meet me there and we'll talk further.")
    player.bimRamarnoIntro = BimRamarno.DEPARTED
    mesbox("Ramarno departs for his workshop.")
}

internal suspend fun Dialogue.ramarnoWorkshop(quest: BelowIceMountain) {
    chatPlayer(neutral, "Hello again.")
    chatNpc(happy, "Ah, ${player.displayName}. You made it.")
    chatPlayer(quiz, "So, tell me about this proposal of yours.")
    chatNpc(
        neutral,
        "Camdozaal is a shadow of what it once was. We Imcando Dwarves were the finest " +
            "artisans in the world, but now I am all that remains. Today, you'll only find " +
            "echoes of our once great city.",
    )
    chatNpc(
        happy,
        "But that which was once great can still be great again! We Imcando Dwarves know that " +
            "better than most. There is hope for the future of Camdozaal. But I need help.",
    )
    chatPlayer(quiz, "What do you need?")
    chatNpc(
        neutral,
        "We have a Sacred Forge. You will find none other like it in all the world. Some of " +
            "the greatest weapons of legend started life here.",
    )
    chatNpc(
        neutral,
        "The Forge could be the key to Camdozaal's future, but it's going out. If it does, I " +
            "fear it will never be lit again. I can no longer gather the fuel it needs. But " +
            "perhaps you can?",
    )
    chatPlayer(quiz, "Fuel?")
    chatNpc(
        neutral,
        "Barronite. You'll only find it in these ruins. If you can gather some for me, I can " +
            "use it to keep the Forge lit. In return, I'll let you take what you want from our " +
            "Great Vault.",
    )
    chatNpc(
        neutral,
        "I no longer have use for anything in the Vault. Besides, material possessions mean " +
            "nothing compared to the fire of the Sacred Forge.",
    )
    chatPlayer(
        quiz,
        "So I find barronite in the ruins and bring it to you? Doesn't sound too difficult. " +
            "Where should I start?",
    )
    chatNpc(
        neutral,
        "Anywhere you like. The ruins are filled with barronite, so anything you do here should " +
            "yield some. If you need more information on specific things you can do, just let " +
            "me know.",
    )
    player.bimRamarnoIntro = BimRamarno.WORKSHOP
}

internal suspend fun Dialogue.ramarnoItem() {
    chatPlayer(neutral, "Ramarno still looks shocked. Perhaps you should talk to him first.")
}
