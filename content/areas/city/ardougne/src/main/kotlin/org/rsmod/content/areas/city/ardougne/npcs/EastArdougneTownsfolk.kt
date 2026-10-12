package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EastArdougneTownsfolk : PluginScript() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Plague_City (starting the quest)
    // TODO(content): [quest:plague_city] start offer, quest stages and after-quest dialogue
    // TODO(content): [quest:song_of_the_elves] after-quest dialogue
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.edmond() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Alrena (before Plague City)
    // TODO(content): [quest:plague_city] quest stages and after-quest dialogue
    // TODO(content): [quest:song_of_the_elves] after-quest dialogue and replacement tabard
    private suspend fun Dialogue.alrena() {
        chatPlayer(happy, "Hello Madam.")
        chatNpc(neutral, "Oh, hello there.")
        chatPlayer(quiz, "Are you ok?")
        chatNpc(sad, "Not too bad... I've just got some troubles on my mind...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Biohazard (getting started)
    // TODO(content): [quest:plague_city] Elena is only home after Plague City
    // TODO(content): [quest:biohazard] start offer, quest stages and after-quest dialogue
    private suspend fun Dialogue.elena() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Councillor_Halgrive (before Sheep Herder)
    // TODO(content): [quest:sheep_herder] start offer, quest stages and after-quest dialogue
    // TODO(content): [quest:song_of_the_elves] mourner outfit and after-quest dialogue
    private suspend fun Dialogue.halgrive() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Doctor_Orbon (before Song of the Elves)
    // TODO(content): [quest:sheep_herder] protective clothing
    // TODO(content): [quest:song_of_the_elves] mourner outfit and after-quest dialogue
    private suspend fun Dialogue.doctorOrbon() {
        chatPlayer(happy, "Hello.")
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

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_Brumty (before Sheep Herder)
    // TODO(content): [quest:sheep_herder] after-quest dialogue
    // TODO(content): [quest:song_of_the_elves] after-quest dialogue
    private suspend fun Dialogue.farmerBrumty() {
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
    // TODO(content): [quest:plague_city] after-quest dialogue
    private suspend fun Dialogue.mournerWall() {
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
        plagueQuestions()
    }

    private suspend fun Dialogue.plagueQuestions() {
        val option =
            choice3(
                "What brought the plague to Ardougne?",
                1,
                "What are the symptoms of the plague?",
                2,
                "Okay then, see you around.",
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
                chatPlayer(neutral, "Okay then, see you around.")
                chatNpc(neutral, "Maybe...")
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
    // TODO(content): [quest:regicide] after-quest dialogue
    // TODO(content): [quest:biohazard] quest stages
    private suspend fun Dialogue.kingLathas() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Jimmy_Dazzler (before Ratcatchers)
    // TODO(content): [quest:ratcatchers] quest stages and after-quest dialogue
    private suspend fun Dialogue.jimmyDazzler() {
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
}
