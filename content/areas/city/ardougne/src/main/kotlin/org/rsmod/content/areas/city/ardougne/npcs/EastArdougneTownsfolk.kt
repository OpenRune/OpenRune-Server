package org.rsmod.content.areas.city.ardougne.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EastArdougneTownsfolk @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.edmond") { startDialogue(it.npc) { edmond() } }
        onOpNpc1("npc.alrena") { startDialogue(it.npc) { alrena() } }
        onOpNpc1("npc.elena2") { startDialogue(it.npc) { elena() } }
        onOpNpc1("npc.jerico") { startDialogue(it.npc) { jerico() } }
        onOpNpc1("npc.omart") { startDialogue(it.npc) { omart() } }
        onOpNpc1("npc.councillor_halgrive") { startDialogue(it.npc) { halgrive() } }
        onOpNpc1("npc.doctor_orbon") { startDialogue(it.npc) { doctorOrbon() } }
        onOpNpc1("npc.farmer_brumty") { startDialogue(it.npc) { farmerBrumty() } }
        onOpNpc1("npc.orbon_priest") { startDialogue(it.npc) { priest() } }
        onOpNpc1("npc.mournertwa") { startDialogue(it.npc) { mournerEast() } }
        onOpNpc1("npc.mournertwb") { startDialogue(it.npc) { mournerWall() } }
        onOpNpc1("npc.rpdt_employee") { startDialogue(it.npc) { gpdtEmployee() } }
        onOpNpc1("npc.horacio") { startDialogue(it.npc) { horacio() } }
        onOpNpc1("npc.kinglathas") { startDialogue(it.npc) { kingLathas() } }
        onOpNpc1("npc.aluft_gnome_diplomat_ardougne") { startDialogue(it.npc) { gimblewap() } }
        onOpNpc1("npc.vc_jimmy_dazzler") { startDialogue(it.npc) { jimmyDazzler() } }
        onOpNpc1("npc.pmod_town_crier_east_ardoyne") { startDialogue(it.npc) { townCrier() } }
        onOpNpc1("npc.con_contractor_ardougne") { startDialogue(it.npc) { ellie() } }
        onOpNpc1("npc.con_contract_client_6") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_8") { startDialogue(it.npc) { mahoganyClient() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Edmond
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.edmond() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(happy, "Hello there friend. I hope you're looking after Elena out there.")
            chatPlayer(laugh, "I think she might be the one looking after me.")
            chatNpc(laugh, "Ha. That's my Elena.")
            return
        }
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatPlayer(happy, "Hello there.")
            chatNpc(happy, "Ah hello. Thank you again for rescuing my daughter.")
            chatPlayer(happy, "No problem.")
            return
        }
        edmondBeforePlagueCity()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Plague_City (starting the quest)
    // TODO(content): [quest:plague_city] start offer and quest stages
    private suspend fun Dialogue.edmondBeforePlagueCity() {
        chatPlayer(happy, "Hello old man.")
        chatNpc(worried, "Sorry, I can't stop to talk...")
        chatPlayer(quiz, "Why, what's wrong?")
        chatNpc(sad, "I've got to find my daughter. I pray that she is still alive...")
        val ask =
            choice2(
                "What's happened to her?",
                true,
                "Well, good luck finding her.",
                false,
            )
        if (!ask) {
            chatPlayer(neutral, "Well, good luck finding her.")
            return
        }
        chatPlayer(quiz, "What's happened to her?")
        chatNpc(
            sad,
            "Elena's a healer. Three weeks ago she managed to cross the wall into West Ardougne. " +
                "No one's allowed to cross the wall in case they spread the plague.",
        )
        chatPlayer(confused, "Plague?")
        chatNpc(
            neutral,
            "Not that long ago, West Ardougne was hit by a deadly plague. They had the wall " +
                "built to try and keep it contained. No one is allowed to enter the city now " +
                "apart from the mourners.",
        )
        chatNpc(
            sad,
            "They say the plague is a horrible way to go... That's why Elena felt she had to go " +
                "help. She said she'd be gone for a few days but we've heard nothing since.",
        )
        chatPlayer(quiz, "Maybe I could help find her?")
        chatNpc(
            happy,
            "Really, would you? I've been working on a plan to get into West Ardougne, but I'm " +
                "too old and tired to carry it through. But you on the other hand, you should " +
                "have no problem.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alrena
    // TODO(content): [quest:plague_city] quest stages
    private suspend fun Dialogue.alrena() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            alrenaAfterSongOfTheElves()
            return
        }
        if (QuestRequirements.hasCompleted(player, MOURNINGS_END_II)) {
            chatNpc(sad, "They've taken her...")
            chatPlayer(quiz, "Who?")
            chatNpc(sad, "Elena! They came and took her away!")
            chatPlayer(shocked, "Oh no! Is there anything I can do?")
            chatNpc(sad, "I'm not sure... Speak to Edmond, he was there when they took her.")
            return
        }
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatNpc(
                happy,
                "Thank you for rescuing my daughter! Elena has told me of your bravery in " +
                    "entering a house that could have been plague infected. I can't thank you " +
                    "enough!",
            )
            return
        }
        chatPlayer(happy, "Hello Madam.")
        chatNpc(neutral, "Oh, hello there.")
        chatPlayer(quiz, "Are you ok?")
        chatNpc(sad, "Not too bad... I've just got some troubles on my mind...")
    }

    private suspend fun Dialogue.alrenaAfterSongOfTheElves() {
        chatNpc(happy, "Thank you so much for helping Elena. It means so much to Edmond and I.")
        val lostTabard = !player.ownsObj(KNIGHT_TABARD)
        val tabard =
            lostTabard &&
                choice2("I need another tabard.", true, "It's not a problem.", false)
        if (!tabard) {
            chatPlayer(happy, "It'a not a problem.")
            return
        }
        chatPlayer(neutral, "I need another tabard.")
        chatNpc(
            happy,
            "Not a problem. I still have enough materials to make another. Here you go.",
        )
        player.invAddOrDrop(objRepo, KNIGHT_TABARD)
        mesbox("Alrena gives you a Knight of Ardougne tabard.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elena
    // TODO(content): [quest:plague_city] Elena is only home after Plague City
    private suspend fun Dialogue.elena() {
        if (QuestRequirements.hasCompleted(player, MOURNINGS_END_I)) {
            elenaAfterMourningsEndI()
            return
        }
        elenaBeforeBiohazard()
    }

    private suspend fun Dialogue.elenaAfterMourningsEndI() {
        chatPlayer(happy, "Hey Elena.")
        chatNpc(happy, "Hey ${player.displayName}.")
        val option =
            choice3(
                "How's it going?",
                1,
                "Do you have another sieve? I lost the last one.",
                2,
                "See you later.",
                3,
            )
        when (option) {
            1 -> {
                chatNpc(
                    worried,
                    "Not bad. I won't lie though, the stuff you told me about elves and dark " +
                        "lords is all a bit frightening.",
                )
                chatPlayer(
                    neutral,
                    "I know. I'm working on it though, by the time I'm done we'll never have to " +
                        "worry about any of this again.",
                )
                chatNpc(worried, "I hope you're right. Be careful out there, I worry about you.")
                chatPlayer(happy, "I'll be fine Elena, don't you worry.")
            }
            2 -> {
                chatNpc(
                    happy,
                    "Well it just so happens that by freak accident, I had hundreds delivered " +
                        "the other day!",
                )
                player.invAddOrDrop(objRepo, SIEVE)
                mesbox("Elena hands you a large sieve.")
                chatPlayer(happy, "Thanks again.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Biohazard (getting started)
    // TODO(content): [quest:biohazard] start offer, quest stages and the dialogue between
    //  Biohazard and Mourning's End Part I
    private suspend fun Dialogue.elenaBeforeBiohazard() {
        chatPlayer(happy, "Good day to you, Elena.")
        chatNpc(happy, "You too, thanks for freeing me.")
        chatNpc(sad, "It's just a shame the mourners confiscated my equipment.")
        chatPlayer(quiz, "What did they take?")
        chatNpc(
            sad,
            "My distillator. I can't test any plague samples without it. They're holding it in " +
                "the Mourner Headquarters in West Ardougne.",
        )
        chatNpc(
            neutral,
            "I must somehow retrieve that distillator if I am to find a cure for this awful " +
                "affliction. Do you think you could help me?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jerico
    // TODO(content): [quest:biohazard] quest stages
    private suspend fun Dialogue.jerico() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "How's it going?")
        chatPlayer(happy, "Good thanks. Just passing by.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Omart
    // TODO(content): [quest:biohazard] quest stages
    private suspend fun Dialogue.omart() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Hello.")
        chatPlayer(quiz, "How are you?")
        chatNpc(neutral, "Fine thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Councillor_Halgrive
    // TODO(content): [quest:sheep_herder] start offer and quest stages
    private suspend fun Dialogue.halgrive() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatPlayer(happy, "Hello again.")
            chatNpc(happy, "Well hello again adventurer! How are you today?")
            chatPlayer(happy, "Fine thank you. And yourself?")
            chatNpc(
                neutral,
                "Not too bad, I've a lot to do though. King Lathas left plenty of things for us " +
                    "to clean up.",
            )
            return
        }
        if (QuestRequirements.hasCompleted(player, SHEEP_HERDER)) {
            halgriveAfterSheepHerder()
            return
        }
        chatPlayer(happy, "Hello. How are you?")
        chatNpc(sad, "I've been better.")
        if (!choice2("What's wrong?", true, "That's life for you.", false)) {
            chatPlayer(neutral, "That's life for you.")
            chatNpc(
                sad,
                "That's what concerns me... There might not be much life around here soon.",
            )
            return
        }
        chatPlayer(quiz, "What's wrong?")
        chatNpc(
            neutral,
            "You may or may not be aware, but a plague has spread across West Ardougne. Now, so " +
                "far, our efforts to contain it have been largely successful, for the most part.",
        )
        chatNpc(
            worried,
            "However, some sheep recently escaped from a farm near the city. When they were " +
                "found, we noticed that they were strangely discoloured, so we asked the " +
                "mourners to examine them.",
        )
        chatNpc(worried, "They believe that the sheep have become infected with the plague.")
        chatPlayer(quiz, "Are you sure it's the plague?")
        chatNpc(
            neutral,
            "Well the mourners seemed convinced and they're the experts. Even if it's not " +
                "though, the sheep are clearly diseased in some way.",
        )
        chatNpc(
            neutral,
            "As the councillor responsible for public health and safety here in East Ardougne, I " +
                "am in charge of dealing with this problem.",
        )
        chatNpc(
            neutral,
            "We need someone to herd these sheep into a safe enclosure, kill them quickly and " +
                "cleanly and then dispose of the remains hygienically in a special incinerator.",
        )
        chatNpc(
            sad,
            "Unfortunately, nobody wants to risk catching the plague, and I am unable to find " +
                "someone willing to undertake this mission for me.",
        )
    }

    private suspend fun Dialogue.halgriveAfterSheepHerder() {
        if (player.wearsMournerGear()) {
            chatPlayer(happy, "Hello.")
            chatNpc(
                quiz,
                "Hello, is there any news about how our sheep keep getting infected with the " +
                    "plague?",
            )
            chatPlayer(neutral, "Not that I know of, sorry.")
            chatNpc(
                neutral,
                "I'd sure like to know why we can't contain it. Please raise the issue next time " +
                    "you see the Head Mourner.",
            )
            chatPlayer(neutral, "I'll keep it in mind.")
            return
        }
        chatPlayer(happy, "Hello again sir.")
        chatNpc(happy, "Well hello again adventurer! How are you today?")
        chatPlayer(happy, "Fine thank you. And yourself?")
        chatNpc(
            neutral,
            "I'm okay. Sadly some more diseased sheep have appeared so I have them to deal with.",
        )
        chatPlayer(quiz, "Do you need me to help again?")
        chatNpc(
            neutral,
            "Thank you for the kind offer. You've done enough though. We'll find someone else to " +
                "deal with these ones.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Doctor_Orbon
    // TODO(content): [quest:sheep_herder] protective clothing
    private suspend fun Dialogue.doctorOrbon() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(neutral, "Hello there. Can't stop to talk I'm afraid, I'm very busy right now.")
            return
        }
        chatPlayer(happy, "Hello.")
        if (player.wearsMournerGear()) {
            chatNpc(
                quiz,
                "Good day, you have my next payment? I don't keep this charade up for fun you " +
                    "know.",
            )
            chatPlayer(neutral, "Sorry I don't.")
            chatNpc(
                neutral,
                "Well if you would mention it next time you see the Head Mourner, it would be " +
                    "appreciated.",
            )
            chatPlayer(neutral, "I'll keep it in mind.")
            return
        }
        chatNpc(
            quiz,
            "How do you feel? No heavy flu, shivers, nausea or loss of appetite?",
        )
        chatPlayer(confused, "Uh... no... I feel fine...")
        chatNpc(
            quiz,
            "How about nightmares then? Are you experiencing any problems with especially scary " +
                "nightmares?",
        )
        chatPlayer(confused, "No... not since I was young.")
        chatNpc(
            neutral,
            "Good, good, I had to make sure. This plague really has an incredibly fast contagion " +
                "rate. It spreads faster than the common cold!",
        )
        val tellMore = choice2("The plague? Tell me more.", true, "Okay, I'll be careful.", false)
        if (!tellMore) {
            chatPlayer(neutral, "Okay, I'll be careful.")
            chatNpc(
                neutral,
                "You do that adventurer. You do not want to become infected by the plague.",
            )
            return
        }
        chatPlayer(quiz, "The plague? Tell me more.")
        chatNpc(
            neutral,
            "It came from the west somewhere. It is extremely quick spreading, and utterly " +
                "deadly.",
        )
        chatPlayer(quiz, "What are the symptoms?")
        chatNpc(
            neutral,
            "It varies slightly from case to case, but usually abnormal nightmares and flu-like " +
                "symptoms, progressing as you succumb more. Eventually, it results in the " +
                "presence of a thick black tar-like substance secreted",
        )
        chatNpc(neutral, "from the nose and eyes.")
        chatNpc(sad, "Unfortunately if you reach this stage of infection then no one can save you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_Brumty
    private suspend fun Dialogue.farmerBrumty() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatPlayer(sad, "Hello there. Sorry about your sheep...")
            chatNpc(
                neutral,
                "That's okay, you are not to blame. I'm just glad the truth behind the plague " +
                    "was eventually revealed.",
            )
            return
        }
        if (QuestRequirements.hasCompleted(player, SHEEP_HERDER)) {
            chatPlayer(sad, "Hello there. Sorry about your sheep...")
            chatNpc(neutral, "That's okay, it had to be done for the sake of the town.")
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(
            sad,
            "I have all the bad luck. My sheep all run off somewhere, and then those mourners " +
                "tell me they're infected!",
        )
        chatPlayer(neutral, "Well, I hope things start to look up for you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Priest_(East_Ardougne)
    // TODO(content): [quest:song_of_the_elves] quest dialogue
    private suspend fun Dialogue.priest() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Peace be with you, friend.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mourner_(Plague_City) (East Ardougne)
    // TODO(content): [quest:plague_city] quest stages outside Edmond's house
    private suspend fun Dialogue.mournerEast() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Do you have a problem traveller?")
        chatPlayer(
            quiz,
            "No, I just wondered why you're wearing that outfit... Is it fancy dress?",
        )
        chatNpc(angry, "No! It's for protection.")
        chatPlayer(quiz, "Protection from what?")
        chatNpc(neutral, "The plague of course...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mourner_(Plague_City) (Ardougne Wall)
    private suspend fun Dialogue.mournerWall() {
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatPlayer(happy, "Hi.")
            chatNpc(neutral, "What are you up to?")
            chatPlayer(happy, "Just sight-seeing.")
            chatNpc(
                angry,
                "This is no place for sight-seeing. Don't you know there's been a plague outbreak?",
            )
            chatPlayer(neutral, "Yes, I had heard.")
            chatNpc(neutral, "Then I suggest you leave as soon as you can.")
            plagueQuestions(afterPlagueCity = true)
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Can I help you?")
        chatPlayer(quiz, "What are you doing?")
        chatNpc(
            neutral,
            "I'm guarding the border to West Ardougne. No one except we mourners can pass " +
                "through.",
        )
        chatPlayer(quiz, "Why?")
        chatNpc(neutral, "The plague of course. We can't risk cross contamination.")
        plagueQuestions(afterPlagueCity = false)
    }

    private suspend fun Dialogue.plagueQuestions(afterPlagueCity: Boolean) {
        val leave = if (afterPlagueCity) "Thanks for the advice." else "Okay then, see you around."
        val option =
            choice3(
                "What brought the plague to Ardougne?",
                1,
                "What are the symptoms of the plague?",
                2,
                leave,
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What brought the plague to Ardougne?")
                chatNpc(
                    neutral,
                    "It's all down to King Tyras of West Ardougne. It started came back from one " +
                        "of his visits to the lands west of here.",
                )
                chatNpc(
                    neutral,
                    "Some of his men must have unknowingly caught it out there and brought it " +
                        "back with them",
                )
                chatPlayer(quiz, "Does he know how bad the situation is now?")
                chatNpc(
                    angry,
                    "If he did he wouldn't care. I believe he wants his people to suffer, he's " +
                        "an evil man.",
                )
                chatPlayer(shocked, "Isn't that treason?")
                chatNpc(neutral, "He's not my king.")
            }
            2 -> {
                chatPlayer(quiz, "What are the symptoms of the plague?")
                chatNpc(
                    neutral,
                    "The first signs are typical flu symptoms. These tend to be followed by " +
                        "severe nightmares, horrifying hallucinations which drive many to " +
                        "madness.",
                )
                chatPlayer(worried, "Sounds nasty.")
                chatNpc(
                    neutral,
                    "It gets worse. Next the victim's blood changes into a thick black tar-like " +
                        "liquid, at this point they're past help.",
                )
                chatNpc(
                    neutral,
                    "Their skin is cold to the touch, the victim is now brain dead. Their body " +
                        "however lives on driven by the virus, roaming like a zombie, spreading " +
                        "itself further wherever possible.",
                )
                chatPlayer(worried, "I think I've heard enough.")
            }
            3 -> {
                chatPlayer(neutral, leave)
                if (!afterPlagueCity) {
                    chatNpc(neutral, "Maybe...")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:GPDT_employee
    // TODO(content): [quest:tribal_totem] delivery address labels
    private suspend fun Dialogue.gpdtEmployee() {
        chatNpc(happy, "Welcome to GPDT!")
        chatPlayer(happy, "Thank you very much.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Horacio
    // TODO(content): [system:treasure_trails] elite clue step
    private suspend fun Dialogue.horacio() {
        chatNpc(happy, "It's a fine day to be out in the garden isn't it?")
        if (choice2("Yes, it's very nice.", true, "So... who are you?", false)) {
            chatPlayer(happy, "Yes, it's very nice.")
            chatNpc(happy, "Days like these make me glad to be alive!")
            return
        }
        chatPlayer(quiz, "So... who are you?")
        chatNpc(
            happy,
            "My name is Horacio Dobson. I'm the gardener to Lord Handelmort. Take a look around " +
                "this beautiful garden, all of this is my handiwork.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Lathas
    // TODO(content): [quest:biohazard] quest stages
    private suspend fun Dialogue.kingLathas() {
        if (QuestRequirements.hasCompleted(player, REGICIDE)) {
            if (player.wearsMournerGear()) {
                chatPlayer(happy, "Good day.")
                chatNpc(
                    angry,
                    "What are you doing here? All Mourners should know that if they need to " +
                        "contact me, they need to use the proper channels!",
                )
                chatPlayer(sad, "Sorry, I'm a new recruit.")
                chatNpc(angry, "Well do not let it happen again.")
                return
            }
            chatPlayer(happy, "Good day my lord.")
            chatNpc(neutral, "Hello ${player.displayName}. I've got nothing for you right now.")
            return
        }
        chatNpc(angry, "Leave me citizen. I'm far too busy to talk.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ambassador_Gimblewap
    // TODO(content): [system:gnome_restaurant] food deliveries
    private suspend fun Dialogue.gimblewap() {
        chatNpc(angry, "I can't believe that I've been made to wait so long.")
        chatPlayer(quiz, "What are you waiting for?")
        chatNpc(angry, "I have been waiting for an audience with the King for days!")
        chatPlayer(neutral, "Well, he's clearly a busy man.")
        chatNpc(
            angry,
            "Bah! I think he's just trying to insult the gnome nation, by making me wait. The " +
                "cheek of these humans. Just because they're vertically challenged they think " +
                "they can ignore us.",
        )
        chatPlayer(neutral, "I'm not ignoring you.")
        chatNpc(neutral, "Hmm. I should be thankful for small mercies, I suppose.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jimmy_Dazzler
    // TODO(content): [quest:ratcatchers] quest stages
    private suspend fun Dialogue.jimmyDazzler() {
        if (QuestRequirements.hasCompleted(player, RATCATCHERS)) {
            jimmyAfterRatcatchers()
            return
        }
        chatNpc(shifty, "Who are you, and what do you want?")
        chatPlayer(
            neutral,
            "My name is ${player.displayName}, and I have a couple of questions for you.",
        )
        chatNpc(neutral, "No.")
        chatPlayer(confused, "What?")
        chatNpc(
            angry,
            "I don't know you, therefore I will not answer any questions. Now be gone from here, " +
                "you're drawing attention.",
        )
        chatPlayer(laugh, "As if those clothes you're wearing wouldn't do that already.")
    }

    private suspend fun Dialogue.jimmyAfterRatcatchers() {
        chatNpc(happy, "Ah, ${player.displayName}. Back for more intonation?")
        val option =
            choice4(
                "Can you tell me where I can find...",
                1,
                "Can you tell me more about the Rat Pits?",
                2,
                "Can you show me to the Rat Pits?",
                3,
                "How do you spell your name again?",
                4,
            )
        when (option) {
            1 -> jimmyWhereToFind()
            2 -> {
                chatPlayer(quiz, "Can you tell me more about the pits?")
                if (!player.ownsObj(RAT_PITS_MANUAL)) {
                    chatNpc(happy, "Here take this, you might find it useful.")
                    player.invAddOrDrop(objRepo, RAT_PITS_MANUAL)
                    objbox(RAT_PITS_MANUAL, "You are handed the rat pits manual.")
                }
                jimmyRatPitsQuestions()
            }
            3 -> {
                chatPlayer(quiz, "Can you show me to the Rat Pits?")
                chatNpc(neutral, "See that ladder there? That's the way in.")
                chatPlayer(happy, "Thanks.")
                chatNpc(happy, "Good luck!")
            }
            4 -> {
                chatPlayer(quiz, "How do you spell your name again?")
                chatNpc(neutral, "J-I-M-M-Y D-A-Z-Z-L-E-R.")
                chatNpc(quiz, "Why?")
                chatPlayer(quiz, "Ever thought about putting a '1' in it?")
                chatNpc(confused, "Why on Gielinor would I do that?")
                chatPlayer(happy, "Might add a bit of pizzazz, that's all.")
                chatNpc(neutral, "Pizzazz, you say? I'll think about it.")
            }
        }
    }

    private suspend fun Dialogue.jimmyWhereToFind() {
        val option =
            choice5(
                "Grimesquit and Phingspet",
                1,
                "Hooknosed Jack",
                2,
                "Smokin' Joe",
                3,
                "The Face",
                4,
                "Felkrash",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you tell me where I can find Phingspet and Grimesquit?")
                chatNpc(neutral, "They live in the sewers below Varrock.")
                chatNpc(neutral, "Please don't say hello from me.")
            }
            2 -> {
                chatPlayer(quiz, "Can you tell me where I can find Hooknosed Jack?")
                chatNpc(
                    neutral,
                    "He hangs around in a rather unfashionable part of Varrock. He likes a good " +
                        "scrap, does Jack.",
                )
            }
            3 -> {
                chatPlayer(quiz, "Can you tell me where I can find Smokin' Joe?")
                chatNpc(
                    neutral,
                    "If the old rascal's lungs haven't given up yet, you should find him in " +
                        "Keldagrim.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Can you tell me where I might find 'The Face'?")
                chatNpc(neutral, "Port Sarim is your best bet.")
            }
            5 -> {
                chatPlayer(quiz, "Can you tell me where I might find Felkrash?")
                chatNpc(quiz, "The Boss?")
                chatNpc(
                    neutral,
                    "You'll find her in Port Sarim, probably in the Rat Pits there.",
                )
                chatPlayer(quiz, "She's your boss?")
                chatNpc(neutral, "Well, sort of. She's the unofficial leader of the Ratcatchers.")
                chatNpc(neutral, "Nobody elected her or anything.")
                chatPlayer(happy, "Got it. She's the boss.")
            }
        }
    }

    private suspend fun Dialogue.jimmyRatPitsQuestions() {
        while (true) {
            chatNpc(quiz, "What do you want to know?")
            val pointers =
                choice2(
                    "Do you have any pointers for me and my cat?",
                    true,
                    "What benefits does my cat get from training in the pits?",
                    false,
                )
            if (pointers) {
                jimmyCatPointers()
            } else {
                chatPlayer(quiz, "What benefits does my cat get from training in the pits?")
                chatNpc(neutral, "It depends on what kind of cat you have.")
                chatNpc(
                    neutral,
                    "In the Ardougne pits, your kitten will grow up faster whether it wins or " +
                        "loses.",
                )
                chatPlayer(quiz, "What if it wins then?")
                chatNpc(
                    happy,
                    "It will gain more experience, it'll be happier, and it'll be fed, too.",
                )
            }
            chatPlayer(happy, "Thanks for your help.")
            chatNpc(quiz, "Do you have any other questions about the pits?")
            if (!choice2("I'd like to ask you...", true, "No thanks", false)) {
                chatPlayer(neutral, "No thanks.")
                return
            }
        }
    }

    private suspend fun Dialogue.jimmyCatPointers() {
        chatPlayer(quiz, "Do you have any pointers for me and my cat?")
        chatNpc(
            neutral,
            "You should wear an Amulet of Catspeak. It will allow you to organise tactics with " +
                "your cat.",
        )
        chatPlayer(quiz, "What would be the best tactic to adopt then?")
        chatNpc(
            neutral,
            "Well, that depends on your outlook, whether you're ready to risk your cat's life or " +
                "not and how defensive or aggressive you want your cat to be.",
        )
        val safe =
            choice2(
                "I will not risk my cat's life.",
                true,
                "I am willing to risk my cat if it improves my chances.",
                false,
            )
        if (safe) {
            chatPlayer(neutral, "I will not risk my cat's life.")
            chatNpc(neutral, "Well if that's the case then you're left with two choices.")
            chatNpc(
                neutral,
                "You can adopt an ultra defensive strategy, which increases your cat's hitpoints " +
                    "and defence but causes it to flee sooner.",
            )
            chatNpc(
                neutral,
                "Or you can adopt a less defensive strategy, which will allow your cat to stay " +
                    "in the pit for longer.",
            )
            val ultra =
                choice2(
                    "I think I would adopt an ultra defensive strategy.",
                    true,
                    "I think I would adopt a less defensive strategy.",
                    false,
                )
            if (ultra) {
                chatPlayer(neutral, "I think I would adopt a very defensive strategy")
                chatNpc(
                    neutral,
                    "In that case you should tell your cat: 'Just take care of yourself, cat.'",
                )
            } else {
                chatPlayer(neutral, "I think I would adopt a less defensive strategy.")
                chatNpc(
                    neutral,
                    "In that case you should tell your cat: 'Stay in for as long as you can.'",
                )
            }
            return
        }
        chatPlayer(neutral, "I am willing to risk my cat, if it increases my chances of victory.")
        chatNpc(neutral, "In that case you are left with two choices.")
        chatNpc(
            neutral,
            "You could go for all out attack which will boost your cat's attack and strength, " +
                "but weaken it.",
        )
        chatNpc(
            neutral,
            "Or, go for a less aggressive stance, which will increase your cat's strength and " +
                "attack to a lesser degree, but carries no defensive penalties.",
        )
        val veryAggressive =
            choice2(
                "I think I would adopt a very aggressive strategy.",
                true,
                "I think I would adopt a less aggressive strategy.",
                false,
            )
        if (veryAggressive) {
            chatPlayer(angry, "I think I would adopt a very aggressive strategy.")
            chatNpc(neutral, "In that case you should tell your cat: 'Go berserk!'")
        } else {
            chatPlayer(neutral, "I think I would adopt a less aggressive strategy.")
            chatNpc(neutral, "In that case you should tell your cat: 'No guts, no glory!'")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Town_Crier
    private suspend fun Dialogue.townCrier() {
        chatNpc(happy, "Hello citizen!")
        if (choice2("What do you do around here?", true, "See you later.", false)) {
            chatPlayer(quiz, "What do you do around here?")
            chatNpc(
                happy,
                "I'm a Town Crier. It's my job to let people know of any recent news. After all, " +
                    "there's all sorts of things happening around here.",
            )
            chatPlayer(neutral, "I see. See you later.")
        } else {
            chatPlayer(neutral, "See you later.")
        }
        chatNpc(happy, "Until next time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ellie
    // TODO(content): [system:mahogany_homes] contracts after joining the company
    private suspend fun Dialogue.ellie() {
        chatNpc(happy, "Hey, how can I help you?")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            happy,
            "This is one of the branches for Mahogany Homes, Gielinor's first construction " +
                "company.",
        )
        chatPlayer(happy, "Oh wow, how can I get involved?")
        chatNpc(
            happy,
            "If you go see Amy in the Falador branch, she'll fill you in. Her office is just " +
                "east of the Estate Agent in Falador.",
        )
        chatPlayer(happy, "Nice, thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jess
    // https://oldschool.runescape.wiki/w/Transcript:Noella
    // TODO(content): [system:mahogany_homes] contract requests and the after-contract cup of tea
    private suspend fun Dialogue.mahoganyClient() {
        chatNpc(neutral, "Please excuse me, I'm rather busy.")
    }

    private companion object {
        const val PLAGUE_CITY = "quest_plaguecity"
        const val SHEEP_HERDER = "quest_sheepherder"
        const val REGICIDE = "quest_regicide"
        const val RATCATCHERS = "quest_ratcatchers"
        const val MOURNINGS_END_I = "quest_mourningsendpart1"
        const val MOURNINGS_END_II = "quest_mourningsendpart2"
        const val SONG_OF_THE_ELVES = "quest_songoftheelves"

        const val KNIGHT_TABARD = "obj.ardougne_knight_tabard"
        const val SIEVE = "obj.mourning_sieve"
        const val RAT_PITS_MANUAL = "obj.ratpits_manual"
    }
}
