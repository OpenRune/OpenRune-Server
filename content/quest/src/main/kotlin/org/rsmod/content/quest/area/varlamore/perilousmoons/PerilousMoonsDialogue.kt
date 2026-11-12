package org.rsmod.content.quest.area.varlamore.perilousmoons

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.content.quest.manager.menu

internal suspend fun Dialogue.gateRefusal() {
    attala(angry, "What do you think you're doing, human?")
    chatPlayer(shifty, "Er... Admiring these lovely gates?")
    attala(angry, "You can admire them from back here!")
}

internal suspend fun Dialogue.gateArgument() {
    zuma(
        neutral,
        "It is my right and duty as a follower of Ranul to document that which has been found " +
            "beneath your city.",
    )
    attala(
        angry,
        "Your beliefs do not grant you a right to anything in Cam Torum, not to document " +
            "things, and especially not to get yourself killed.",
    )
    attala(
        angry,
        "I don't want to give Fortis any excuses to come knocking on our door to investigate " +
            "your untimely and idiotic demise.",
    )
    zuma(neutral, "I won't take no for an answer. I am bound by my divine oath.")
    attala(
        angry,
        "Oh yes? And will your divine oath feed you in there? Light your way? Keep you safe " +
            "from cave-ins and monsters?",
    )
    chatPlayer(neutral, "I don't know about cave-ins, but I can handle monsters.")
}

internal suspend fun Dialogue.gateTurnedAway() {
    attala(
        angry,
        "Sorry, but I'm busy with a different arrogant human. You'll have to wait in line.",
    )
}

internal suspend fun Dialogue.guardReportOpening() {
    zuma(happy, "Ranul provides...")
    attala(
        angry,
        "No! We've more than enough humans in Cam Torum with this blasted Fortis mandated dig! " +
            "I don't care how many deals it secured, it's not worth all this trouble!",
    )
    guard(neutral, "Advisor Attala...")
    attala(angry, "What?!")
}

internal suspend fun Dialogue.guardReport() {
    guard(
        neutral,
        "Apologies, but I was sent to tell you that one of the creatures from the ruins managed " +
            "to bypass our defenses.",
    )
    guard(neutral, "It escaped through the mountain's outflow spring, to the north east.")
    attala(
        angry,
        "The one that leads to the aqueduct?! We'll have Fortis guards barging into the city if " +
            "they catch wind...",
    )
    chatPlayer(happy, "It sounds like you need help.")
    attala(angry, "Cam Torum does not need help, much less that of any humans!")
    attala(
        neutral,
        "Wake the off-duty guards and put them to work. I'll ensure they're paid handsomely.",
    )
    guard(neutral, "I'm afraid we already have.")
    attala(angry, "Argh! Get back to your post!")
}

internal suspend fun Dialogue.attalaOffer() {
    attala(
        angry,
        "Alright, fine! Human, you look like you've seen a fight or two. Deal with the escaped " +
            "creature by the mountain outflow spring.",
    )
    chatPlayer(quiz, "Hang on, what's in it for me?")
    attala(angry, "What do you want?!")
}

internal suspend fun Dialogue.naguaReminder() {
    attala(quiz, "Have you killed that creature yet?")
    chatPlayer(neutral, "Working on it.")
    attala(
        angry,
        "It's up by the mountain outflow spring, north east of here. Get to it before it makes " +
            "its way to the aqueduct!",
    )
}

internal suspend fun Dialogue.naguaReport() {
    val name = player.displayName
    chatPlayer(quiz, "I've taken care of the... What was that thing anyway?")
    attala(
        neutral,
        "A spirit of sorts. Something far older than anything your people have bothered to " +
            "remember. In any case, a deal is a deal. You may enter the city of Cam Torum.",
    )
    zuma(quiz, "What about me?")
    attala(angry, "Are you still here?!")
    zuma(angry, "Yes, and I'm not going anywhere!")
    attala(angry, "Fine! If it'll make you stop pestering me, you can join...")
    chatPlayer(neutral, "$name.")
    attala(angry, "You can join $name.")
    attala(
        angry,
        "$name, he's your responsibility now. If either of you cause any trouble, you're both " +
            "out!",
    )
    chatPlayer(quiz, "Now, hang on...")
    zuma(happy, "Thank you, $name. I'm Zuma, by the way. I'll meet you inside.")
}

internal suspend fun Dialogue.buildersPointToJess() {
    builder1(
        happy,
        "Thanks for that, mate. You should speak to Jess inside. She'll be in the north of the " +
            "city. She could use a problem solver like yourself.",
    )
}

internal suspend fun Dialogue.buildersAfterZumaLeaves() {
    buildersPointToJess()
    chatPlayer(quiz, "Who's Jess?")
    builder2(
        neutral,
        "She's the archaeologist in charge of the dig into these ruins they've found. " +
            "Discovering the mysteries of the past and that.",
    )
}

internal suspend fun Dialogue.buildersHeadInside() {
    builder1(
        happy,
        "You should head inside, mate. Remember, Jess will be in the north of the city. She " +
            "could use a problem solver like yourself.",
    )
}

private enum class BuilderTopic {
    GoingOn,
    Philosophers,
    Goodbye,
}

internal suspend fun Dialogue.buildersAtGate() {
    builder1(
        neutral,
        "It's a bit rude of 'em to take up the whole bridge with their conversation, isn't it, " +
            "mate?",
    )
    builder2(neutral, "Aye, mate, but wouldn't it be just as rude for us to interrupt?")
    builder1(
        neutral,
        "A good point well made, mate. Who are we to decide that the dig is more impor'ant " +
            "than a personal disagreement between two people?",
    )
    builder2(
        neutral,
        "You're not wrong, mate. We often place matters of industry above those of people, and " +
            "it's chippin' away at our ability to relate to each other on a human level.",
    )
    while (true) {
        val topic =
            menu(
                "What's going on here?" to BuilderTopic.GoingOn,
                "Are you two philosophers?" to BuilderTopic.Philosophers,
                "Goodbye." to BuilderTopic.Goodbye,
            )
        when (topic) {
            BuilderTopic.GoingOn -> {
                chatPlayer(quiz, "What's going on here?")
                builder1(
                    neutral,
                    "We're part of a dig into some ruins beneath Cam Torum, but the way's " +
                        "blocked by these folks.",
                )
                chatPlayer(quiz, "Have you tried asking them to move?")
                builder2(
                    neutral,
                    "We're considering the moral implications of doing exactly that, mate.",
                )
            }
            BuilderTopic.Philosophers -> {
                chatPlayer(quiz, "Are you two philosophers?")
                builder1(neutral, "No mate, builders.")
                builder2(
                    neutral,
                    "But thinking, talking, discussing, it's what makes us human, mate.",
                )
                chatPlayer(neutral, "It's not just humans that do those things.")
                builder2(
                    neutral,
                    "Fair play, mate. I guess we're reflecting more on the condition of " +
                        "sen'ience in general then.",
                )
                chatPlayer(confused, "...")
            }
            BuilderTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                builder2(happy, "Have a good one, mate.")
                return
            }
        }
    }
}

private enum class AttalaCityTopic {
    Unhappy,
    Defence,
    Humans,
    Goodbye,
}

internal suspend fun Dialogue.attalaInCity() {
    attala(neutral, "Hello again.")
    chatPlayer(quiz, "What are you doing here?")
    attala(
        angry,
        "Well with the situation outside dealt with, I now need to keep an eye on this " +
            "expedition. Any mistakes made in these ruins could bring the ground out from " +
            "beneath our city!",
    )
    chatPlayer(quiz, "Is it really that dangerous in there?")
    attala(
        neutral,
        "It might be if you don't know what you're dealing with. Your people often assume they " +
            "do, which generally makes things worse.",
    )
    while (true) {
        val topic =
            menu(
                "The rest of your people don't seem very happy I'm here." to
                    AttalaCityTopic.Unhappy,
                "Will the city's defense be okay without you?" to AttalaCityTopic.Defence,
                "I take it you don't get on well with the humans?" to AttalaCityTopic.Humans,
                "Goodbye." to AttalaCityTopic.Goodbye,
            )
        when (topic) {
            AttalaCityTopic.Unhappy -> {
                chatPlayer(sad, "The rest of your people don't seem very happy I'm here.")
                attala(angry, "The rest?! I'm not happy you're here either.")
                chatPlayer(
                    angry,
                    "I helped you with the aqueduct. You could afford to show a little bit of " +
                        "gratitude.",
                )
                attala(
                    angry,
                    "You'll have my gratitude when you finish your business in Cam Torum and " +
                        "leave.",
                )
            }
            AttalaCityTopic.Defence -> {
                chatPlayer(quiz, "Will the city's defense be okay without you?")
                attala(
                    neutral,
                    "As I say, the situation has been dealt with for now. Besides, the only " +
                        "reason that situation existed in the first place is because of these " +
                        "ruins.",
                )
                attala(
                    neutral,
                    "At this point a smart person would call off this dig entirely, but as that " +
                        "doesn't seem set to happen, I'd say it's in our best interests we keep " +
                        "an eye on things.",
                )
            }
            AttalaCityTopic.Humans -> {
                chatPlayer(quiz, "I take it you don't get on well with the humans?")
                attala(
                    neutral,
                    "It's not exactly that. You have your virtues, but you have short sight and " +
                        "even shorter memories, which can be very dangerous.",
                )
                chatPlayer(quiz, "Why not share what you know with us then?")
                attala(
                    neutral,
                    "I suspect some of your people wouldn't be too pleased if we did. The truth " +
                        "isn't something that everyone desires.",
                )
                chatPlayer(happy, "Well I'm always open to a bit of truth myself.")
                attala(angry, "Hmph! I doubt that.")
            }
            AttalaCityTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                return
            }
        }
    }
}

private enum class ZumaCityTopic {
    Heard,
    Expecting,
    Dwarves,
    Goodbye,
}

internal suspend fun Dialogue.zumaInCity() {
    val name = player.displayName
    zuma(happy, "Thank you for your help back there, $name. Dwarves can be very stubborn.")
    chatPlayer(neutral, "So can you, apparently.")
    zuma(neutral, "It is not stubbornness on my part. It is duty that drives me.")
    chatPlayer(quiz, "Duty to what?")
    zuma(
        neutral,
        "I am a nelta of Ranul and I am bound by his tenets. One of them is to ensure that " +
            "knowledge is protected and respected. These ruins they've found could contain all " +
            "manner of things! I must find out!",
    )
    while (true) {
        val topic =
            menu(
                "How did you hear about the ruins?" to ZumaCityTopic.Heard,
                "What are you expecting to find down there?" to ZumaCityTopic.Expecting,
                "I take it you don't get on well with the dwarves?" to ZumaCityTopic.Dwarves,
                "Goodbye." to ZumaCityTopic.Goodbye,
            )
        when (topic) {
            ZumaCityTopic.Heard -> {
                chatPlayer(quiz, "How did you hear about these ruins?")
                zuma(shocked, "The cave-in that revealed them shook the foundations of the Teomat!")
                zuma(neutral, "The dwarves tried to hide them from us, but we eventually found out.")
            }
            ZumaCityTopic.Expecting -> {
                chatPlayer(quiz, "What are you expecting to find down there?")
                zuma(
                    happy,
                    "I don't know exactly yet, but knowledge of some kind! Maybe even some " +
                        "undiscovered history regarding Ralos and Ranul!",
                )
            }
            ZumaCityTopic.Dwarves -> {
                chatPlayer(quiz, "I take it you don't get on well with the dwarves?")
                zuma(
                    angry,
                    "They are arrogant! They think they know it all, and worst of all, they are " +
                        "non-believers!",
                )
                chatPlayer(neutral, "Technically I'm a non-believer.")
                zuma(
                    neutral,
                    "But that is because you are ignorant of the truth, not because you " +
                        "outright reject it. Would you like me to tell you of the ways of Ranul?",
                )
                chatPlayer(shifty, "Maybe some other time.")
            }
            ZumaCityTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                zuma(neutral, "Timoiva.")
                return
            }
        }
    }
}

private enum class JessCityTopic {
    Help,
    Dwarves,
    Goodbye,
}

internal suspend fun Dialogue.jessamineInCity() {
    val name = player.displayName
    if (player.pmoonStage >= PmoonStage.RUINS_OPEN) {
        chatPlayer(neutral, "Hello again.")
        jessamine(
            happy,
            "Nilsal! When you're ready, meet me inside the ruins. We need help setting up some " +
                "base camps for further exploration.",
        )
    } else {
        chatPlayer(neutral, "Hello.")
        jessamine(
            happy,
            "Nilsal! The name's Jessamine, though most people call me Jess. You must be $name. " +
                "I hear you sorted out the situation holding back the arrival of our supplies. " +
                "Thanks for that.",
        )
        chatPlayer(quiz, "Don't mention it. So what brings you here?")
        jessamine(
            happy,
            "A cave-in revealed some ruins beneath the city! I'm here to lead the dig and see " +
                "what we can discover.",
        )
        chatPlayer(
            neutral,
            "Ancient ruins? There seems to be a lot of those popping up lately.",
        )
        jessamine(
            happy,
            "Gielinor has been around for a long time. We're really only just starting to " +
                "scratch its surface!",
        )
    }
    while (true) {
        val options = buildList {
            if (player.pmoonStage < PmoonStage.RUINS_OPEN) {
                add("Can I be of any help with the dig?" to JessCityTopic.Help)
            }
            add("How come the dwarves let you in?" to JessCityTopic.Dwarves)
            add("Goodbye." to JessCityTopic.Goodbye)
        }
        when (menu(options)) {
            JessCityTopic.Help -> {
                chatPlayer(quiz, "Can I be of any help with the dig?")
                jessamine(
                    confused,
                    "Well it seems our expedition has already gained a couple of new recruits " +
                        "somehow, but what I really need is someone more proactive who can fend " +
                        "for themselves. You seem perfect for the job!",
                )
                player.pmoonStage = PmoonStage.RUINS_OPEN
                jessamine(
                    neutral,
                    "Meet me inside the ruins. We need help setting up some base camps for " +
                        "further exploration.",
                )
                return
            }
            JessCityTopic.Dwarves -> aboutTheDwarves()
            JessCityTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                jessamine(happy, "See you later.")
                return
            }
        }
    }
}

private suspend fun Dialogue.aboutTheDwarves() {
    chatPlayer(quiz, "How come the dwarves let you in?")
    jessamine(
        neutral,
        "I work at the museum in Fortis. The dwarves have a begrudging alliance with the " +
            "capital, and so they begrudgingly let me in.",
    )
    jessamine(
        neutral,
        "They demanded agreement to some absurdly expensive export deals in exchange, and even " +
            "exclusive rights to some imports from the mainland.",
    )
    jessamine(neutral, "It took weeks of negotiation, and they still don't seem entirely satisfied.")
    chatPlayer(quiz, "I take it you don't get on too well?")
    jessamine(
        sad,
        "No. It's a shame. They would be a great source of historical knowledge, if they were " +
            "just more willing to share it.",
    )
}

internal suspend fun Dialogue.ruinsRefusal() {
    jessamine(angry, "Excuse me! I don't believe I've given you permission to enter the dig.")
}

internal suspend fun Dialogue.attalaAtMural() {
    val name = player.displayName
    attala(worried, "Concerning...")
    chatPlayer(neutral, "What have you found?")
    attala(
        worried,
        "This is an old place, $name. Older even than much of what we dwarves remember.",
    )
    chatPlayer(happy, "That's exciting.")
    attala(angry, "No, it's dangerous is what it is.")
    player.pmoonMurals = 1
    attala(angry, "Now where is that fool priest?")
}

internal suspend fun Dialogue.zumaAtMural() {
    zuma(shocked, "Incredible...")
    chatPlayer(quiz, "What have you found?")
    zuma(
        shocked,
        "This is sacred ground. This is most obviously a shrine built by emissaries of Ranul.",
    )
    attala(
        laugh,
        "You see a picture of a creature eating the sun and instead of a warning, you see " +
            "evidence of your god? You really are beyond saving.",
    )
    zuma(
        angry,
        "Enough! This mural may depict a scene straight from our holy scriptures! Show some " +
            "respect!",
    )
    chatPlayer(
        neutral,
        "Alright, calm down, both of you. Let's go see what the actual archaeologist has to say.",
    )
    player.pmoonMurals = 2
}

internal suspend fun Dialogue.jessamineAtMural(knowsTheLanguage: Boolean) {
    jessamine(confused, "Strange...")
    chatPlayer(quiz, "What have you found?")
    jessamine(
        neutral,
        "This mural has an inscription beneath it, written in the language of the Old Ones. I " +
            "think I'm getting there, but I'm going to need more time to decipher it.",
    )
    attala(
        angry,
        "We don't need to be deciphering inscriptions to know that we shouldn't be here. I mean " +
            "look at that... whatever that floating thing is. This place is dangerous!",
    )
    jessamine(neutral, "All the more reason to find out more about it then.")
    if (knowsTheLanguage) {
        chatPlayer(
            quiz,
            "Could I help with the translations? I actually have a bit of experience with this " +
                "language.",
        )
        jessamine(
            happy,
            "That's very kind of you to offer, but if it's okay with you, I actually have a " +
                "different task I could use your help with.",
        )
    } else {
        chatPlayer(quiz, "Do you need me to do anything in the meantime?")
    }
    jessamine(
        happy,
        "The surrounding caverns are quite large. If you could take some building supplies and " +
            "set up some camps in each of them, it would make further exploration a lot easier.",
    )
    player.pmoonStage = PmoonStage.BUILD_CAMPS
    chatPlayer(neutral, "I'll get right to it.")
}

internal suspend fun Dialogue.campsReminderJessamine() {
    jessamine(quiz, "Have you finished setting up the camps?")
    chatPlayer(neutral, "Not quite.")
    jessamine(
        neutral,
        "Let me know when you have. In the meantime, I've got to keep working on this " +
            "inscription, whilst also making sure these two don't kill each other...",
    )
    passionatePair()
}

internal suspend fun Dialogue.campsReminderPair() {
    attala(angry, "How can you make those claims so confidently?")
    zuma(angry, "It's in the holy scriptures! As I was saying...")
    jessamine(sad, "Can't get a word in edgeways with these two.")
    passionatePair()
}

private suspend fun Dialogue.passionatePair() {
    chatPlayer(shifty, "They're... er... Very passionate.")
    chatPlayer(quiz, "Speaking of which, what about you? Why a life of archaeology?")
    jessamine(
        happy,
        "I've always loved it. When I was a child, I had a friend in Fortis who I'd collect old " +
            "trinkets with.",
    )
    chatPlayer(neutral, "I see. I guess it was a natural next step then?")
    jessamine(
        happy,
        "Indeed. The employees at the museum already knew me well by the time I applied. I've " +
            "spent a lot of my life browsing it.",
    )
    chatPlayer(happy, "You should visit the museum in Varrock some time. There's a lot to see!")
    jessamine(
        happy,
        "I've been meaning to! Thing is, I couldn't turn down this opportunity when it came up.",
    )
    chatPlayer(neutral, "Understandable. Right, I should get back to those camps.")
}

private enum class InscriptionTopic {
    Inscription,
    WhatNow,
    West,
    East,
    North,
    Goodbye,
}

/** Returns `true` once the talismans are due to be handed over. */
internal suspend fun Dialogue.campsReport(): Boolean {
    if (player.pmoonStage == PmoonStage.CAMPS_BUILT) {
        chatPlayer(neutral, "I've finished setting up the camps.")
        jessamine(
            happy,
            "Thanks. I'm all done deciphering the inscription. Some of it has worn away, so " +
                "we're missing bits, but between it, the murals and the big floating monolith, " +
                "we've already found some pretty exciting stuff!",
        )
    }
    while (true) {
        val read = player.pmoonStage >= PmoonStage.INSCRIPTION_READ
        val options = buildList {
            if (read) {
                add("What do we do now?" to InscriptionTopic.WhatNow)
            } else {
                add("What does the inscription say?" to InscriptionTopic.Inscription)
            }
            add("What does the western mural depict?" to InscriptionTopic.West)
            add("What does the eastern mural depict?" to InscriptionTopic.East)
            add("What does the northern mural depict?" to InscriptionTopic.North)
            add("Goodbye." to InscriptionTopic.Goodbye)
        }
        when (menu(options)) {
            InscriptionTopic.Inscription -> inscription()
            InscriptionTopic.WhatNow -> {
                whatNow()
                return true
            }
            InscriptionTopic.West -> westernMural()
            InscriptionTopic.East -> easternMural()
            InscriptionTopic.North -> northernMural()
            InscriptionTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                return false
            }
        }
    }
}

private suspend fun Dialogue.inscription() {
    chatPlayer(neutral, "What does the inscription say?")
    jessamine(
        neutral,
        "'Here in Neypotzli, by ??? of earth and streams, have these emissaries of ??? been " +
            "sealed, ??? ??? ???.'",
    )
    zuma(shocked, "Emissaries of Ranul! We must work to free them at once.")
    attala(
        angry,
        "First they built the place, now they're sealed in it. These leaping assumptions are " +
            "going to get us all killed!",
    )
    chatPlayer(laugh, "Well I've only ever had good experiences opening ancient vaults.")
    jessamine(
        neutral,
        "Attala is right to be cautious. These murals don't look all too friendly...",
    )
    zuma(
        happy,
        "But these emissaries may have ancient knowledge lost to the ages! We must investigate " +
            "further.",
    )
    jessamine(
        quiz,
        "These ruins are directly beneath the Teomat... I suppose it's possible there may be " +
            "some links to Ralos and Ranul.",
    )
    attala(angry, "I will not allow you to put Cam Torum in danger just because you're curious!")
    chatPlayer(
        confused,
        "I'm not sure if these disagreements are really getting us anywhere...",
    )
    player.pmoonStage = PmoonStage.INSCRIPTION_READ
    attala(confused, "You make a good point.")
}

private suspend fun Dialogue.whatNow() {
    val name = player.displayName
    if (player.pmoonStage < PmoonStage.TALISMANS_SHOWN) {
        chatPlayer(quiz, "What do we do now?")
        jessamine(
            quiz,
            "I think it would be unwise to go and start freeing emissaries that we know nothing " +
                "about.",
        )
        attala(angry, "Agreed!")
        jessamine(
            quiz,
            "But we should still try and learn more about them. They may pose a risk to Cam " +
                "Torum.",
        )
        attala(neutral, "Hmph! Very well.")
        jessamine(
            quiz,
            "We know they were sealed by the power of earth and streams. This magic presumably " +
                "originates from somewhere within these ruins. I also wonder if these talismans " +
                "I found might be connected...",
        )
        mesbox("Jessamine shows you an earth and a water talisman.")
        chatPlayer(confused, "Interesting...")
        player.pmoonStage = PmoonStage.TALISMANS_SHOWN
        attala(
            neutral,
            "Hmm... Nahta will know what to do with these. $name, take them to her in Cam " +
                "Torum's magic shop. Tell her I sent you.",
        )
    }
}

private suspend fun Dialogue.westernMural() {
    chatPlayer(quiz, "What does the western mural depict?")
    attala(angry, "It's raining blood! It is clearly a depiction of death!")
    jessamine(
        neutral,
        "It's not clear from the mural whether the creature depicted is calling the rain or " +
            "lamenting its arrival.",
    )
    zuma(happy, "It's an emissary of the blood moon! A bringer of rain and bountiful harvests!")
    jessamine(
        neutral,
        "Blood moons don't usually bring good harvests, and I don't see any thriving crops " +
            "depicted in the mural.",
    )
    chatPlayer(quiz, "So what do you make of it?")
    jessamine(
        neutral,
        "The blood moon is nothing more than a cosmic occurrence. We need more context to come " +
            "to any conclusions.",
    )
}

private enum class XimouaTopic {
    What,
    MoveOn,
}

private suspend fun Dialogue.easternMural() {
    chatPlayer(quiz, "What does the eastern mural depict?")
    zuma(happy, "An emissary of the eclipsed moon! A herald of Ximoua!")
    attala(angry, "Ximoua? It is eating the sun, you fool! If it's a herald of anything, it's death!")
    jessamine(neutral, "Given what is taught about Ximoua, that isn't too surprising.")
    val topic =
        menu(
            "What is Ximoua again?" to XimouaTopic.What,
            "Maybe we should focus on something else." to XimouaTopic.MoveOn,
        )
    when (topic) {
        XimouaTopic.What -> {
            chatPlayer(quiz, "What is Ximoua again?")
            zuma(
                happy,
                "An eclipse that marks the end of a cycle! The destruction of the world and its " +
                    "rebirth!",
            )
            chatPlayer(shifty, "Ah yes, I remember now. Very pleasant.")
            zuma(
                happy,
                "Indeed, it is an event we all look forward to. It is said that when it " +
                    "happens, Ralos and Ranul unite all life as one! Can you imagine such an " +
                    "honour?",
            )
        }
        XimouaTopic.MoveOn -> chatPlayer(neutral, "Maybe we should focus on something else.")
    }
}

private suspend fun Dialogue.northernMural() {
    chatPlayer(quiz, "What does the northern mural depict?")
    attala(quiz, "A creature snapping a mountain in half. Care to explain this one, Zuma?")
    zuma(shifty, "It's an emissary of the blue moon! Clearly... er... bringing an end to the winter?")
    chatPlayer(quiz, "Any thoughts on this one, Jess?")
    jessamine(
        neutral,
        "Blue moons are exceedingly rare... They're only seen occasionally during volcanic " +
            "eruptions. It's possible that this cracking mountain signals the appearance of a " +
            "volcano.",
    )
}

internal suspend fun Dialogue.makeSpaceForTalismans() {
    attala(neutral, "Come back to me when you have space and I'll give you the talismans.")
}

private enum class JessRuinsTopic {
    Mediating,
    Assigned,
    Goodbye,
}

internal suspend fun Dialogue.jessamineWaiting() {
    val name = player.displayName
    when {
        player.pmoonStage == PmoonStage.TALISMANS -> {
            jessamine(quiz, "Do you have the enchanted talismans yet?")
            chatPlayer(neutral, "Still working on it.")
            jessamine(neutral, "Let Attala know if you need any help.")
        }
        player.pmoonStage == PmoonStage.ENCHANTED ||
            (player.pmoonStage == PmoonStage.INFUSED && !player.pmoonBriefed) -> {
            jessamine(quiz, "Do you have the enchanted talismans yet?")
            chatPlayer(
                happy,
                "Yes. I managed to get the dwarves to cooperate for just long enough to make them.",
            )
            jessamine(happy, "That's no small feat! You should speak to Attala about it.")
        }
        else -> {
            jessamine(happy, "Nilsal, $name.")
            chatPlayer(happy, "Nilsal, Jess.")
        }
    }
    while (true) {
        val topic =
            menu(
                "Good job mediating those two." to JessRuinsTopic.Mediating,
                "So how did you end up getting assigned to these ruins?" to JessRuinsTopic.Assigned,
                "Goodbye." to JessRuinsTopic.Goodbye,
            )
        when (topic) {
            JessRuinsTopic.Mediating -> {
                chatPlayer(happy, "Good job mediating those two.")
                jessamine(
                    neutral,
                    "Thanks. It does feel a bit like I'm walking a tightrope over a scorpion " +
                        "nest at times.",
                )
                chatPlayer(laugh, "Attala might be worse than a pit of scorpions!")
                attala(quiz, "Did I hear my name?")
                jessamine(shifty, "Yes! We were just saying how helpful you've been!")
                attala(neutral, "Hmph.")
                jessamine(shifty, "Quite!")
            }
            JessRuinsTopic.Assigned -> {
                chatPlayer(quiz, "So how did you end up getting assigned to these ruins?")
                jessamine(
                    neutral,
                    "The museum suspected there might be an Old One connection. It's one of my " +
                        "specialties, so exploring these kinds of places is what I do.",
                )
            }
            JessRuinsTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                return
            }
        }
    }
}

private enum class ZumaRuinsTopic {
    Devout,
    Expected,
    Goodbye,
}

internal suspend fun Dialogue.zumaWaiting() {
    val name = player.displayName
    when {
        player.pmoonStage == PmoonStage.TALISMANS -> {
            zuma(quiz, "Any word on the enchanted talismans?")
            chatPlayer(neutral, "Still working on it.")
            zuma(neutral, "I'm afraid you'll need to talk to Attala if you need any help.")
        }
        player.pmoonStage == PmoonStage.ENCHANTED ||
            (player.pmoonStage == PmoonStage.INFUSED && !player.pmoonBriefed) -> {
            zuma(quiz, "Any word on the enchanted talismans?")
            chatPlayer(
                neutral,
                "Yes. The dwarves of Cam Torum are quite skilled, so it was relatively quick.",
            )
            zuma(angry, "Hmph! All the skill in the world couldn't make up for their attitude.")
        }
        else -> {
            zuma(quiz, "How goes the search, $name?")
            chatPlayer(neutral, "Not too bad, thank you.")
        }
    }
    while (true) {
        val topic =
            menu(
                "What lead you to be such a devout follower of Ranul?" to ZumaRuinsTopic.Devout,
                "So is this place what you were expecting?" to ZumaRuinsTopic.Expected,
                "Goodbye." to ZumaRuinsTopic.Goodbye,
            )
        when (topic) {
            ZumaRuinsTopic.Devout -> {
                chatPlayer(quiz, "What lead you to be such a devout follower of Ranul?")
                zuma(neutral, "Ralos appeals to many who wish to live fast-paced lives.")
                zuma(
                    neutral,
                    "I do see that appeal, but there is a calmness and peace in Ranul that calls " +
                        "to me.",
                )
                chatPlayer(quiz, "Doesn't Ranul govern over death? That doesn't put you off?")
                zuma(
                    neutral,
                    "Making peace with death is one of the things that attracted me to Ranul's " +
                        "teachings.",
                )
                zuma(happy, "That, and all the time I get to spend in nice and quiet libraries.")
            }
            ZumaRuinsTopic.Expected -> {
                chatPlayer(quiz, "So is this place what you were expecting?")
                zuma(
                    happy,
                    "Far more so! As much as Attala refuses to believe it, I think these ruins " +
                        "are connected to Ranul.",
                )
                chatPlayer(quiz, "Is it because of all the moon symbology?")
                zuma(
                    neutral,
                    "That's part of it, but there's also that thing at the centre of this room. " +
                        "Some of the markings remind me of something I once read about.",
                )
                chatPlayer(
                    quiz,
                    "Interesting, if a bit vague. Care to elaborate on what it is you read about?",
                )
                zuma(shifty, "I can't really. I'm still not done putting my theory together.")
                chatPlayer(neutral, "Well I look forward to hearing more once you've done so.")
            }
            ZumaRuinsTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                zuma(neutral, "Timoiva.")
                return
            }
        }
    }
}

internal suspend fun Dialogue.attalaSendsToNahta(lostTalismans: Boolean) {
    attala(neutral, "Take the talismans to Nahta in Cam Torum's magic shop. Tell her I sent you.")
    if (lostTalismans) {
        chatPlayer(quiz, "What if I lost them?")
        attala(
            angry,
            "Then you'll need to find some more yourself, and take better care with them once " +
                "you have!",
        )
    }
}

internal suspend fun Dialogue.attalaSendsBackToCity() {
    attala(
        angry,
        "You're meant to be in Cam Torum sorting those talismans out. Return here once you " +
            "have done so.",
    )
}

private enum class AttalaRuinsTopic {
    WellKnown,
    Friends,
    Goodbye,
}

internal suspend fun Dialogue.attalaTalismanBriefing() {
    if (!player.pmoonBriefed) {
        chatPlayer(neutral, "I have the enchanted talismans.")
        attala(
            neutral,
            "Good. Now let's not waste any more time. In theory, the talismans should guide you " +
                "to the point in this place where these magics of earth and streams resonate " +
                "most strongly.",
        )
        player.pmoonBriefed = true
    } else {
        attala(quiz, "Have you managed to find the source of these earth and stream magics yet?")
        chatPlayer(neutral, "I'm still working on it.")
        attala(
            neutral,
            "Remember, the talismans should guide you to the point in this place where these " +
                "magics resonate most strongly.",
        )
    }
    attala(
        neutral,
        "Of course, we've not tried something like this before, so I imagine it will require " +
            "you to do some testing.",
    )
    while (true) {
        val topic =
            menu(
                "You seem to be well known by the other dwarves in Cam Torum." to
                    AttalaRuinsTopic.WellKnown,
                "Your friends weren't very happy to see me." to AttalaRuinsTopic.Friends,
                "Goodbye." to AttalaRuinsTopic.Goodbye,
            )
        when (topic) {
            AttalaRuinsTopic.WellKnown -> {
                chatPlayer(neutral, "You seem to be well known by the other dwarves in Cam Torum.")
                attala(
                    neutral,
                    "Of course. I am a key advisor to Highlord Katlo himself. Few dwarves hold " +
                        "as much sway as I do. Not just anyone can authorise the entry of a human.",
                )
                chatPlayer(neutral, "I suppose I should thank you for letting me in then.")
                attala(angry, "Hmph! Don't make me regret it.")
            }
            AttalaRuinsTopic.Friends -> {
                chatPlayer(sad, "Your friends weren't very happy to see me.")
                attala(
                    neutral,
                    "That doesn't surprise me. They're busy people with much less patience for " +
                        "humans than I have.",
                )
                chatPlayer(quiz, "You have patience for humans?")
                attala(angry, "Excuse me?")
                chatPlayer(shifty, "Nevermind.")
            }
            AttalaRuinsTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                return
            }
        }
    }
}

internal suspend fun Dialogue.eyatlalliDiscovered() {
    chatPlayer(shocked, "Argh!")
    eyatlalli(quiz, "Euat xita kemo? Euat iknil?")
    chatPlayer(confused, "Er... What?")
    eyatlalli(
        quiz,
        "A human? Interesting... In that case, allow me to use a language more familiar to you.",
    )
    chatPlayer(confused, "Thanks... I think. So who are you?")
    eyatlalli(neutral, "I am Eyatlalli, a nagua of earth and streams.")
    chatPlayer(confused, "Right... And what is this place?")
    eyatlalli(neutral, "Neypotzli. A prison.")
    chatPlayer(quiz, "A prison? For who? You?")
    eyatlalli(neutral, "For the Moons of Peril, and their long dead summoner.")
    chatPlayer(quiz, "Moons of Peril?")
    eyatlalli(neutral, "Yes. Powerful and terrible nagua summoned to wreak havoc.")
    chatPlayer(quiz, "Why would anyone want to do that?")
    eyatlalli(
        neutral,
        "In the face of inevitable death, a powerful shaman decided to lash out and inflict " +
            "death on others.",
    )
    eyatlalli(
        neutral,
        "Her fellows disagreed with her actions. They created this prison, and they summoned me " +
            "to watch over it.",
    )
}

internal suspend fun Dialogue.icosahedronStirs() {
    chatPlayer(shocked, "What was that?")
    player.pmoonStage = PmoonStage.EYATLALLI_GONE
    eyatlalli(neutral, "The icosahedron...")
}

/** The antechamber talk after Eyatlalli vanishes; resumes from the stage it was left at. */
internal suspend fun Dialogue.antechamberShaken() {
    val name = player.displayName
    if (player.pmoonStage < PmoonStage.ANTECHAMBER_SHAKEN) {
        player.pmoonStage = PmoonStage.ANTECHAMBER_SHAKEN
        jessamine(worried, "Argh!")
    }
    if (player.pmoonStage < PmoonStage.ZUMA_BLAMED) {
        jessamine(worried, "$name, what's going on?!")
        chatPlayer(confused, "I'm not sure!")
        attala(angry, "Hang on... is that...")
        eyatlalli(neutral, "I am Eyatlalli, a nagua of-")
        jessamine(angry, "Never mind the introductions, what was that shaking?!")
        eyatlalli(neutral, "The icosahedron has been disturbed.")
        chatPlayer(confused, "The icosa-who?")
        eyatlalli(
            neutral,
            "Icosahedron. An object of twenty facets, floating at the centre of this room.",
        )
        chatPlayer(quiz, "Did anybody touch that thing whilst I was gone?")
        jessamine(
            sad,
            "Zuma did something. We were discussing the murals and when we turned around, he " +
                "was gone!",
        )
        player.pmoonStage = PmoonStage.ZUMA_BLAMED
        attala(angry, "I knew I shouldn't have let that fool priest in!")
    }
    if (player.pmoonStage < PmoonStage.ZUMA_BLAMED_AGAIN) {
        chatPlayer(quiz, "What does the icosahedron do?")
        eyatlalli(
            neutral,
            "It is key to the magics that keep the Moons of Peril contained. It reshapes and " +
                "reweaves the magical seals as the Moons attempt to break them.",
        )
        chatPlayer(quiz, "Isn't it a good thing it moved then?")
        eyatlalli(
            neutral,
            "For it to have moved means that the seals must have needed reshaping. That has not " +
                "happened for decades.",
        )
        player.pmoonStage = PmoonStage.ZUMA_BLAMED_AGAIN
        jessamine(neutral, "Zuma must have done something!")
    }
    chatPlayer(quiz, "Can we fix it?")
    eyatlalli(
        neutral,
        "For centuries, the magic used to seal the Moons has been woven into Neypotzli and the " +
            "creatures that inhabit it.",
    )
    eyatlalli(
        neutral,
        "Naguanil is the magic of nature. A ritual involving the living parts of this place " +
            "could re-infuse the icosahedron.",
    )
    jessamine(neutral, "Living parts? As in the animals?")
    player.pmoonStage = PmoonStage.GATHER
    eyatlalli(
        neutral,
        "Yes. Bring me scales of a bream, the tail of a moss lizard and paste made of moonlight " +
            "grubs. I will begin preparing the rest of the ritual.",
    )
}

private enum class GatherTopic {
    Bream,
    Lizards,
    Grubs,
    Goodbye,
}

internal suspend fun Dialogue.gatherReminder(missing: String) {
    eyatlalli(
        neutral,
        "The Moons of Peril rage against their bindings. Holding them back is exhausting. " +
            "Please bring me the $missing from elsewhere in Neypotzli.",
    )
    while (true) {
        val topic =
            menu(
                "Where can I find bream?" to GatherTopic.Bream,
                "Where can I find moss lizards?" to GatherTopic.Lizards,
                "Where can I find moonlight grubs?" to GatherTopic.Grubs,
                "Goodbye." to GatherTopic.Goodbye,
            )
        when (topic) {
            GatherTopic.Bream -> {
                chatPlayer(quiz, "Where can I find bream?")
                eyatlalli(neutral, "They can be fished in the north east section of Neypotzli.")
            }
            GatherTopic.Lizards -> {
                chatPlayer(quiz, "Where can I find moss lizards?")
                eyatlalli(
                    neutral,
                    "They can be caught in the north west section of Neypotzli. They'll likely " +
                        "be hiding in the bushes.",
                )
            }
            GatherTopic.Grubs -> {
                chatPlayer(quiz, "Where can I find moonlight grubs?")
                eyatlalli(
                    neutral,
                    "They can be picked off of the saplings growing all around the north of " +
                        "Neypotzli.",
                )
            }
            GatherTopic.Goodbye -> {
                chatPlayer(neutral, "Goodbye.")
                return
            }
        }
    }
}

private enum class BriefingTopic {
    Goodbye,
    Blood,
    Blue,
    Eclipse,
    Other,
}

internal suspend fun Dialogue.moonsBriefing(doneOneSmallFavour: Boolean) {
    eyatlalli(
        neutral,
        "The ritual is in motion, but the wound to the seal will not close with the Moons still " +
            "raging against it. I must ask you for one more small favour.",
    )
    if (doneOneSmallFavour) {
        chatPlayer(sad, "Please, no...")
    }
    eyatlalli(
        neutral,
        "The Moons cannot be killed, but they can be distracted whilst I repair the seal. Enter " +
            "their chambers here in Neypotzli and engage them in combat.",
    )
    chatPlayer(
        worried,
        "Aren't these things like unstoppable forces of nature or something? Am I going to be " +
            "alright in there?",
    )
    eyatlalli(
        neutral,
        "Ordinarily, no. Their mere presence is enough to bring most creatures to an early " +
            "death. However, you have my help.",
    )
    if (player.pmoonStage < PmoonStage.FIGHT_MOONS) {
        player.pmoonStage = PmoonStage.FIGHT_MOONS
    }
    eyatlalli(
        neutral,
        "Whilst you fight them, look for my glyphs. They will protect and guide you. If the " +
            "Moons become too enraged, I will transport you out of their chambers.",
    )
    val topic =
        menu(
            "What should I expect from the Blood Moon?" to BriefingTopic.Blood,
            "What should I expect from the Blue Moon?" to BriefingTopic.Blue,
            "What should I expect from the Eclipse Moon?" to BriefingTopic.Eclipse,
            "Anything else I should know?" to BriefingTopic.Other,
            "Goodbye." to BriefingTopic.Goodbye,
        )
    when (topic) {
        BriefingTopic.Blood -> {
            chatPlayer(quiz, "What should I expect from the Blood Moon?")
            eyatlalli(
                neutral,
                "When it starts to rain blood, look to your feet. When the jaguars are " +
                    "summoned, prepare to leap back.",
            )
        }
        BriefingTopic.Blue -> {
            chatPlayer(quiz, "What should I expect from the Blue Moon?")
            eyatlalli(
                neutral,
                "Be prepared to fight without your weapon. Ensure the room's braziers remain " +
                    "lit, for the cold will restore the Moon's strength.",
            )
        }
        BriefingTopic.Eclipse -> {
            chatPlayer(quiz, "What should I expect from the Eclipse Moon?")
            eyatlalli(
                neutral,
                "Ensure it doesn't catch you with your back turned. Make sure to take cover " +
                    "when it fires its searing hot rays.",
            )
        }
        BriefingTopic.Other -> {
            chatPlayer(quiz, "Anything else I should know?")
            eyatlalli(
                neutral,
                "Any time the Moons of Peril inflict pain, they can heal themselves, freeze " +
                    "your joints, or blind your eyes. A defensive approach should be used " +
                    "against them.",
            )
        }
        BriefingTopic.Goodbye -> chatPlayer(neutral, "Goodbye.")
    }
}

internal suspend fun Dialogue.jessamineDilemma() {
    chatPlayer(neutral, "This is quite the dilemma.")
    jessamine(
        happy,
        "It is, although I'd be lying if I didn't admit that it is also fascinating.",
    )
    jessamine(happy, "Naguanil magic, ancient spirits of nature... This is the discovery of the century!")
    chatPlayer(neutral, "Let's make sure that Cam Torum is safe before we start celebrating.")
}

internal suspend fun Dialogue.attalaApology() {
    attala(sad, "I'm sorry...")
    chatPlayer(quiz, "What for?")
    attala(
        sad,
        "After berating you about keeping an eye on the priest, I failed to do the same myself. " +
            "I was caught up in the mystery.",
    )
    attala(laugh, "Perhaps there's a little human in me after all!")
    chatPlayer(neutral, "It's okay, we'll figure this out.")
}

internal suspend fun Dialogue.buildersInRuins() {
    builder1(happy, "G'day, mate.")
    when {
        player.pmoonStage < PmoonStage.CAMPS_BUILT -> {
            chatPlayer(
                quiz,
                "I'm surprised to see you both in such high spirits after walking past all " +
                    "those jeering dwarves.",
            )
            builder2(
                neutral,
                "Ah, well it's unfortunate, but no reason to be sad. It's them that ought to be " +
                    "sad.",
            )
            builder1(
                neutral,
                "Aye, mate. Livin' a life unable to connect with others because they look " +
                    "different must be miserable.",
            )
            builder2(
                happy,
                "We wish 'em the best. Hopefully their paths to personal growth are near and " +
                    "smooth.",
            )
            chatPlayer(confused, "...")
        }
        player.pmoonStage < PmoonStage.GATHER -> {
            builder2(happy, "Thanks for taking care of those base camps, mate.")
            chatPlayer(
                neutral,
                "No problem. I hope there's no hard feelings over Jessamine asking me to do a " +
                    "part of your jobs.",
            )
            builder1(
                neutral,
                "That's no worries, mate, I've no issue admitting you're better cut out for " +
                    "building camps in dangerous dungeons than I am.",
            )
            builder2(
                neutral,
                "We may not be mul'i-disciplined adventurers like you, but we don't need to be. " +
                    "The world needs more than just heroes, don't it?",
            )
            builder1(
                happy,
                "Aye, mate, I'm content and proud to just be a hard working, decent builder.",
            )
            builder2(
                neutral,
                "Besides, without us, building supplies wouldn't be there in the first place. " +
                    "Life's all about working together.",
            )
            chatPlayer(happy, "Glad to hear it. Keep up the good work then!")
            builder2(happy, "You too, mate.")
        }
        else -> {
            chatPlayer(
                quiz,
                "You seem remarkably calm considering everything that's going on.",
            )
            builder2(
                neutral,
                "Life's unpredictable, mate. No sense stressin' over what's out of your control.",
            )
            builder1(
                neutral,
                "It's not like we're not aware of the danger we're in, but worryin' about it " +
                    "ain't productive.",
            )
            chatPlayer(
                quiz,
                "Shouldn't you be getting out of here if you feel like you're in danger?",
            )
            builder2(
                neutral,
                "We may not be in total control of our surroundings, but we can still impact " +
                    "them positively through our actions. Like bein' here to support you.",
            )
            builder1(happy, "Even if that's just emotionally. We believe in you, mate.")
            chatPlayer(happy, "Thanks.")
            builder2(happy, "No worries, mate. Go get 'em.")
        }
    }
}

internal suspend fun Dialogue.jessamineFinish() {
    val name = player.displayName
    if (player.pmoonStage < PmoonStage.JESSAMINE_DONE) {
        player.pmoonStage = PmoonStage.JESSAMINE_DONE
        jessamine(neutral, "Did you do it?")
        chatPlayer(neutral, "I think so.")
        jessamine(
            happy,
            "Good work, $name. We made many significant discoveries today, even if it was a bit " +
                "precarious.",
        )
        chatPlayer(laugh, "Perilous, even.")
    }
    jessamine(
        happy,
        "I'll be sticking around here for some time I reckon. There's still so much to discover " +
            "in this place. See you around.",
    )
}

internal suspend fun Dialogue.zumaFinish() {
    zuma(sad, "I am a fool...")
    chatPlayer(neutral, "Well... yes, a bit.")
    builder1(
        happy,
        "You meant well. You thought you were helpin'. Can't fault you for that part, mate.",
    )
    builder2(
        happy,
        "You made a mistake. Coulda been any of us. Just gotta make sure we learn from our " +
            "mistakes is all.",
    )
    builder1(
        happy,
        "And if yer genuinely sorry, best you can do is work to be better, not mope around. So " +
            "keep your chin up, mate!",
    )
    zuma(neutral, "Thank you...")
    chatPlayer(quiz, "What did you do, anyway?")
    zuma(
        confused,
        "I saw sun and moon runes on that thing. Recalled something from Ranul's teachings, a " +
            "rite he once performed...",
    )
    zuma(
        neutral,
        "I tried to emulate it, and it worked. I thought I had freed his emissaries. But they " +
            "weren't that at all...",
    )
    if (player.pmoonStage == PmoonStage.JESSAMINE_DONE) {
        player.pmoonStage = PmoonStage.ZUMA_DONE
    }
    chatPlayer(
        quiz,
        "Interesting. Maybe there was some sort of connection between Ranul and this place " +
            "after all.",
    )
}

internal suspend fun Dialogue.zumaFinishAgain() {
    zuma(
        neutral,
        "I suppose I should think about heading back to the Teomat. I might not mention " +
            "everything that happened down here to the others...",
    )
    chatPlayer(neutral, "I guess that's understandable.")
    zuma(happy, "I suppose I still have plenty to share. Maybe I'll write a book about it!")
    chatPlayer(happy, "I'm sure it will be a great one. Until next time, Zuma.")
}

internal suspend fun Dialogue.buildersFinishAgain() {
    builder1(happy, "Well, all's well that ends well.")
    builder2(happy, "We never had any doubts! Always knew you'd get it sorted, mate.")
    chatPlayer(happy, "Thanks.")
    builder2(happy, "No worries, mate.")
}

internal suspend fun Dialogue.attalaFinish() {
    val name = player.displayName
    attala(quiz, "The nagua seems to have relaxed. I assume you were successful?")
    chatPlayer(neutral, "I think so.")
    attala(
        happy,
        "You have my gratitude, $name. You have helped to ensure Cam Torum's safety. You will " +
            "always be welcome back.",
    )
    chatPlayer(
        shifty,
        "Something tells me the rest of your people won't be too happy about that.",
    )
    attala(
        laugh,
        "Well it's not their choice, so they'll just have to suck it up. Who knows, maybe given " +
            "long enough, you might start to grow on them!",
    )
}

internal suspend fun Dialogue.eyatlalliFinish() {
    eyatlalli(
        neutral,
        "You have done well. The seal is repaired, but the Moons have seen a glimpse of " +
            "freedom. Even now they rage against their bindings. It is... taxing.",
    )
    chatPlayer(neutral, "Is there anything I can do to help?")
    eyatlalli(
        neutral,
        "You could. Distracting them again will afford me some rest. In return, I am willing to " +
            "repay you with trinkets from ancient times.",
    )
    chatPlayer(happy, "That sounds interesting.")
    eyatlalli(
        neutral,
        "Very well. Come and claim your rewards from the chamber to the south east. Should you " +
            "aid in containing them again, you can return there to receive more rewards.",
    )
    eyatlalli(neutral, "For now though, thank you for your help, iknil.")
}

internal suspend fun Dialogue.jessaminePostQuest() {
    val name = player.displayName
    jessamine(happy, "Nilsal, $name. How are things going?")
    chatPlayer(
        happy,
        "Pretty good, thanks. How about you? Made any more discoveries down here?",
    )
    jessamine(
        happy,
        "More than you can count! I've found so much new information on the Old Ones and " +
            "naguanil. The museum are hoping to include it all in some new exhibits soon!",
    )
    chatPlayer(
        happy,
        "Sounds awesome! I'll be sure to keep an eye out for them. On that note, I'll let you " +
            "get back to it.",
    )
}

internal suspend fun Dialogue.buildersPostQuest() {
    builder1(happy, "Good to see you again, mate.")
    chatPlayer(happy, "Good to see you both as well. How's things?")
    builder2(happy, "Never better, mate. Never better. This here ruin was the find of the century!")
    builder1(happy, "All credit to Jess, o'course, but we're glad to be doing our bit!")
    chatPlayer(happy, "Glad to hear it.")
}

internal suspend fun Dialogue.eclipseZumaApproaches() {
    zuma(happy, "I can't believe it... An emissary of my god, standing before me...")
    chatPlayer(angry, "Zuma, no! Step back!")
}

internal suspend fun Dialogue.eclipseZumaFlees() {
    chatPlayer(angry, "Get out of here!")
}
