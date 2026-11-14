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
    if (player.bimRamarnoIntro == BimRamarno.WORKSHOP) {
        ramarnoForge()
        return
    }
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

private suspend fun Dialogue.ramarnoForge() {
    chatPlayer(neutral, "Hello again.")
    chatNpc(
        neutral,
        "Ah, ${player.displayName}. I'm sure you have plenty of questions. I will do my best " +
            "to answer them.",
    )

    while (true) {
        val choice =
            choice4(
                "What can I do here?",
                1,
                "What happened to the other Imcando Dwarves?",
                2,
                "Can I use the Forge?",
                3,
                "I'll see you later.",
                4,
            )
        when (choice) {
            1 -> if (!whatCanIDoHere()) return
            2 -> otherImcandoDwarves()
            3 -> useForge()
            else -> return
        }
    }
}

private suspend fun Dialogue.whatCanIDoHere(): Boolean {
    chatPlayer(neutral, "What can I do here?")
    chatNpc(
        neutral,
        "As you can see, Camdozaal isn't the city it once was. That said, there are still a " +
            "few things around here that might interest you. There's the golem workshop, the " +
            "barronite mine and the offering altar.",
    )

    return when (
        choice5(
            "Tell me more about the golem workshop.",
            1,
            "Tell me more about the barronite mine.",
            2,
            "Tell me more about the offering altar.",
            3,
            "Let's talk about something else.",
            4,
            "I'll see you later.",
            5,
        )
    ) {
        1 -> {
            golemWorkshop()
            true
        }
        2 -> {
            barroniteMine()
            true
        }
        3 -> {
            offeringAltar()
            true
        }
        4 -> true
        else -> false
    }
}

private suspend fun Dialogue.golemWorkshop() {
    chatPlayer(neutral, "Tell me more about the golem workshop.")
    chatNpc(
        neutral,
        "You'll find it to the east. It was once the forefront of our technological " +
            "advances. Sadly, it was also one of the major factors that led to our doom.",
    )
    chatPlayer(neutral, "What were you doing there?")
    chatNpc(
        neutral,
        "Camdozaal was built upon the ruins of an older civilization. Little trace of them " +
            "remained by the time we arrived. However, when building the city, ancient golems " +
            "were discovered.",
    )
    chatNpc(
        neutral,
        "These golems were very crude. They were mostly made from clay, of all things. We " +
            "used our advanced Smithing knowledge to improve their form. Eventually, we were " +
            "able to get them working again.",
    )
    chatNpc(
        neutral,
        "We kept pushing with the golems. Many of them we imbued with magical energies. We " +
            "started with mind and body energy, but eventually moved on to chaos as well.",
    )
    chatNpc(
        neutral,
        "Alas, the influence of chaos had grave results. We lost control of many. Those that " +
            "became corrupt had to be locked away, but by then it was already too late.",
    )
    chatNpc(
        neutral,
        "The barbarians had learnt of our experimentation, and were determined to destroy " +
            "all traces of it.",
    )
    chatNpc(
        neutral,
        "Ironically, locking them away meant the golems were among the few things to survive " +
            "the barbarians. If you fancy a bit of combat, you might want to go take a look.",
    )
    chatNpc(
        neutral,
        "They're largely made of barronite now, so they're a good source of the stuff.",
    )
    chatNpc(
        neutral,
        "Their cores are also very interesting. Apparently, the dwarves of the golem " +
            "workshop once used old cores to craft runes. Maybe you could do the same.",
    )
}

private suspend fun Dialogue.barroniteMine() {
    chatPlayer(neutral, "Tell me more about the barronite mine.")
    chatNpc(
        neutral,
        "You'll find it to the north west. Unsurprisingly, it's the best place to find " +
            "barronite in the city.",
    )
    chatNpc(
        neutral,
        "At one time, our miners would work all through the day there to gather the " +
            "barronite needed to power the Sacred Forge. That was a long time ago.",
    )
    chatNpc(
        neutral,
        "If you're lucky, you might find other things while mining there. We were not the " +
            "first to build a home beneath this mountain. Sometimes traces of an older " +
            "civilization can be found.",
    )
    chatPlayer(quiz, "What kind of traces?")
    chatNpc(
        neutral,
        "You might find some ancient relics encased within barronite. If you do, you can use " +
            "the anvils here to crack open the deposits.",
    )
    chatNpc(
        neutral,
        "The relics are of no interest to me. Maybe some of your race would find them " +
            "interesting.",
    )
}

private suspend fun Dialogue.offeringAltar() {
    chatPlayer(neutral, "Tell me more about the offering altar.")
    chatNpc(
        neutral,
        "You'll find it to the south west. It's not something that ever interested me, but " +
            "most of my kind were followers of Saradomin - the god of order and wisdom.",
    )
    chatNpc(
        neutral,
        "Many of my kin would visit the altar to make offerings in his name.",
    )
    chatNpc(
        neutral,
        "First, they'd catch fish from the nearby reservoir. Next, they'd ritually prepare " +
            "them at tables by the altar. Finally, they'd use the altar to offer the fish.",
    )
    chatNpc(
        neutral,
        "There's plenty of barronite to be found within the reservoir, so it might be worth " +
            "checking out. Be warned though: worshipping gods never did my kind any good.",
    )
}

private suspend fun Dialogue.otherImcandoDwarves() {
    chatPlayer(neutral, "What happened to the other Imcando Dwarves?")
    chatNpc(
        neutral,
        "Ah... It's a tough tale to tell. Even though it's been many years since the last of " +
            "my kin died out, the memory is still fresh.",
    )
    chatPlayer(neutral, "You don't have to tell me if the memory hurts too much.")
    chatNpc(neutral, "No, I will. The legacy of my kin must live on.")
    chatNpc(
        neutral,
        "As you can see around you, Camdozaal was once a great city. Alive with the hum of " +
            "activity. We dug deep into the mountain and reaped the rewards.",
    )
    chatNpc(
        neutral,
        "Our methods would often differ from other dwarves. Whereas they just used " +
            "traditional smithing techniques, we would combine them with magic as well.",
    )
    chatNpc(
        neutral,
        "Alas, that was ultimately our undoing. We learned of a horde of barbarians " +
            "pillaging across the land, hunting down and killing anyone who dared use magic.",
    )
    chatNpc(
        neutral,
        "We underestimated them. We believed our weapons would be enough to keep us safe. We " +
            "were wrong.",
    )
    chatNpc(
        neutral,
        "The barbarians brought ruin upon Camdozaal. Most of my kind died as the city " +
            "collapsed around them. Only a few of us survived until the end. Now, only I remain.",
    )
    chatPlayer(
        neutral,
        "The world has changed a lot, Ramarno. Magic is practiced freely. Those barbarians " +
            "failed to wipe it from Gielinor.",
    )
    chatNpc(
        neutral,
        "I'm glad they didn't win, I suppose. But it doesn't change much for me. I am still " +
            "the last Imcando Dwarf.",
    )
    chatPlayer(
        neutral,
        "I'm sorry Ramarno. I will do my best to help ensure the legacy of your kin lives on.",
    )
    chatNpc(
        neutral,
        "Thank you, ${player.displayName}. That gives me some hope. If you have any more " +
            "questions, do not hesitate to ask.",
    )
}

private suspend fun Dialogue.useForge() {
    chatPlayer(neutral, "Can I use the Forge?")
    chatNpc(
        neutral,
        "I'm afraid not. The Sacred Forge is like none other in existence. Only an Imcando " +
            "Dwarf could use it correctly.",
    )
    chatPlayer(quiz, "What makes it so special?")
    chatNpc(
        neutral,
        "Truth be told, I don't fully know. It's far older than I am. It was supposedly built " +
            "in the earliest days of the city.",
    )
    chatNpc(
        neutral,
        "What I do know is that it's been imbued with powerful magic - some said that of the " +
            "gods themselves. The Forge powers the entire city, or what's left of it at least.",
    )
    chatNpc(
        neutral,
        "I've been doing my best to keep it lit over the years. It's not a job I'm capable " +
            "of any more though. That's why I'm hoping you can help.",
    )
    chatNpc(
        neutral,
        "Bring me any barronite you find. In return, I can channel enough power from the " +
            "Forge into the city or even open the Great Vault.",
    )
    chatNpc(neutral, "Is there anything else you wanted to talk about?")
}

internal suspend fun Dialogue.ramarnoItem() {
    chatPlayer(neutral, "Ramarno still looks shocked. Perhaps you should talk to him first.")
}
