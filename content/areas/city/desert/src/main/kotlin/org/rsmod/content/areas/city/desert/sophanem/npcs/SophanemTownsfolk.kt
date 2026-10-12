package org.rsmod.content.areas.city.desert.sophanem.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:icthlarins_little_helper] Sophanem is entered during the quest; every
//  townsperson below uses the after-quest dialogue, the quest and pre-quest variants are missing
// TODO(content): [system:acne_potion] potion used on the Sophanites
class SophanemTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ics_little_embalmer") { startDialogue(it.npc) { embalmer() } }
        onOpNpc1("npc.ics_little_linen2") { startDialogue(it.npc) { siamun() } }
        onOpNpc1("npc.ics_little_carpenter") { startDialogue(it.npc) { carpenter() } }
        onOpNpc1("npc.ics_little_priest") { startDialogue(it.npc) { templePriest() } }
        onOpNpc1("npc.ics_little_priestdoorman") { startDialogue(it.npc) { gatePriest() } }
        onOpNpc1("npc.ics_little_priestdoorman_alt") { startDialogue(it.npc) { southEastPriest() } }
        onOpNpc1("npc.ics_little_worker1") { startDialogue(it.npc) { worker() } }
        onOpNpc1("npc.ics_little_worker2") { startDialogue(it.npc) { worker() } }
        onOpNpc1("npc.ics_little_hipriest_town") { startDialogue(it.npc) { highPriest() } }
        onOpNpc1("npc.ics_little_sphinx") { startDialogue(it.npc) { sphinx() } }
        onOpNpc1("npc.neferti_the_camel") { startDialogue(it.npc) { neferti() } }
        onOpNpc1("npc.contact_jex") { startDialogue(it.npc) { jex() } }
        onOpNpc1("npc.ntk_tarik") { startDialogue(it.npc) { tarik() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Embalmer
    // TODO(content): [quest:contact] "Could I buy something?" and the Trade option opening
    //  "The Spice Is Right" after the quest
    private suspend fun Dialogue.embalmer() {
        chatPlayer(happy, "Hi.")
        chatNpc(neutral, "Hello again. Do you need something?")
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "Well I'm afraid I can't stop to talk right now. Lots to do.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Siamun
    private suspend fun Dialogue.siamun() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Do you need something?")
        chatPlayer(neutral, "No.")
        chatNpc(neutral, "Well I have lots to do, so good day.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Carpenter
    // TODO(content): [quest:contact] balm dialogue after the quest
    // TODO(content): [quest:beneath_cursed_sands] dialogue after the quest
    private suspend fun Dialogue.carpenter() {
        chatPlayer(quiz, "Hey there. How're you holding up?")
        chatNpc(neutral, "Not too bad. Still, I hope we can get rid of these spots soon.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Priest_(Sophanem)
    private suspend fun Dialogue.templePriest() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Greetings. May the blessings of Icthlarin rest on your tired shoulders and set you " +
                "at ease during these strange times.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Priest_(Sophanem)
    // TODO(content): [verify:in_game] which doorman spawn is the priest outside the south-eastern
    //  gate
    private suspend fun Dialogue.gatePriest() {
        chatNpc(happy, "Good day, adventurer.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Priest_(Sophanem)
    private suspend fun Dialogue.southEastPriest() {
        chatNpc(happy, "Good day, adventurer.")
        if (!choice2("What's that place to the south?", true, "Good day.", false)) {
            chatPlayer(happy, "Good day.")
            return
        }
        chatPlayer(quiz, "What's that place to the south?")
        chatNpc(
            neutral,
            "That's the old necropolis. It's been abandoned for a long time. In fact, you " +
                "couldn't even access most of it until fairly recently.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(neutral, "It was mostly buried under the sand until a storm came in from the south.")
        chatPlayer(
            neutral,
            "Interesting... I bet the place is a hotspot for grave robbers.",
        )
        chatNpc(
            neutral,
            "Not as many as you'd think. There's a lot of superstition about the place. People " +
                "say that even the sand is marked by death.",
        )
        chatPlayer(quiz, "You sound like you don't believe them.")
        chatNpc(
            neutral,
            "Not really. The sand is probably that colour due to the amount of salt in it. Still, " +
                "if it keeps people from robbing the dead, that's no bad thing.",
        )
        chatPlayer(happy, "I see. Thanks for the information.")
        chatNpc(happy, "Any time, adventurer.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Worker_(Sophanem)
    // TODO(content): [quest:contact] back-to-work dialogue after the quest
    private suspend fun Dialogue.worker() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hey there!")
        chatPlayer(
            confused,
            "You seem rather happy for a busy labourer with a bad case of spots.",
        )
        chatNpc(happy, "Of course! These spots are the best thing since log rollers!")
        chatPlayer(confused, "I really don't follow.")
        chatNpc(
            happy,
            "Well thanks to these spots, the city has been under quarantine. That means there's " +
                "no goods coming in or out. No goods means we don't have any work to do!",
        )
        chatPlayer(quiz, "But aren't the spots intolerable?")
        chatNpc(
            happy,
            "I would take a dermatological irritation over back-breaking, mind-numbing labour any " +
                "day!",
        )
        chatPlayer(confused, "Right... Okay then...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Contact!
    // TODO(content): [quest:contact] requirement check, start offer and the rest of the quest;
    //  https://oldschool.runescape.wiki/w/Transcript:High_Priest_(Sophanem) after it
    private suspend fun Dialogue.highPriest() {
        chatNpc(neutral, "Good day.")
        chatPlayer(happy, "Hello there. How goes things?")
        chatNpc(
            neutral,
            "Better now that we've sorted out that business with Klenter. Still, we're no closer " +
                "to dealing with these plagues.",
        )
        chatNpc(happy, "However, we do have one bit of good news in that area, thanks to you.")
        chatPlayer(confused, "Thanks to me?")
        chatNpc(
            happy,
            "Yes. You've shown us that the pox we're all infected with can't be contagious. If it " +
                "was, you'd be covered in spots by now.",
        )
        chatPlayer(quiz, "That's a very good point. Does this mean you can reopen the city?")
        chatNpc(
            happy,
            "It does indeed. The decision to lock things down was made very quickly, so we were " +
                "forced to trap many of our citizens outside the walls. We'll be able to let them " +
                "all back in now.",
        )
        chatPlayer(happy, "That's wonderful news!")
        chatNpc(worried, "It is, but there is a problem.")
        chatPlayer(quiz, "What is it?")
        chatNpc(
            sad,
            "When the plague first started, Menaphos, our neighbouring city, closed their gates as " +
                "well. Many of our citizens became trapped there when they did.",
        )
        chatPlayer(
            quiz,
            "But if there's no risk of them becoming infected, can't they open up again as well?",
        )
        chatNpc(
            sad,
            "You'd think so, but they believe that there is still a danger. It seems they're even " +
                "willing to kill if it helps keep their city isolated.",
        )
        chatPlayer(neutral, "Hmm... Perhaps I could help?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sphinx
    // TODO(content): [system:cat_followers] dialogue with a cat follower or a cat in the inventory
    // TODO(content): [quest:icthlarins_little_helper] cat ears dialogue
    private suspend fun Dialogue.sphinx() {
        chatPlayer(happy, "Good day.")
        chatNpc(
            neutral,
            "I have no interest in speaking with you. You should bring your cat to me again. The " +
                "two of us have much to discuss.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Neferti_the_Camel
    // TODO(content): [quest:enakhras_lament] camulet dialogue and ugthanki dung
    private suspend fun Dialogue.neferti() {
        val option =
            choice3(
                "Ask the camel about its dung.",
                1,
                "Say something unpleasant.",
                2,
                "Neither - I'm a polite person.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    quiz,
                    "I'm sorry to bother you, but could you spare me a little dung?",
                )
                mesbox("The camel didn't seem to appreciate that question.")
            }
            2 -> {
                val insult =
                    access.random.pick(
                        "Mmm... looks like that camel would make a nice kebab.",
                        "If I go near that camel, it'll probably bite my hand off.",
                        "I wonder if that camel has fleas...",
                    )
                chatPlayer(neutral, insult)
                val reaction =
                    access.random.pick(
                        "The camel tries to stamp on your foot, but you pull it back quickly.",
                        "The camel turns its head and glares at you.",
                        "The camel spits at you, and you jump back hurriedly.",
                    )
                access.mes(reaction)
            }
            3 -> return
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jex
    // TODO(content): [quest:contact] quest dialogue and the cellar ladder
    private suspend fun Dialogue.jex() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello there. Welcome to the Temple of the Lesser Gods.")
        val learn =
            choice2(
                "Could you tell me more about the lesser gods?",
                true,
                "I'd better get going.",
                false,
            )
        if (!learn) {
            chatPlayer(neutral, "I'd better get going.")
            return
        }
        chatPlayer(quiz, "Could you tell me more about the lesser gods?")
        chatNpc(
            neutral,
            "Of course. The lesser gods, or avatars as some call them, were born from the dreams " +
                "of Tumeken. They are Apmeken, Het, Crondis and Scabaras.",
        )
        while (true) {
            val god =
                choice5(
                    "Could you tell me more about Apmeken?",
                    1,
                    "Could you tell me more about Het?",
                    2,
                    "Could you tell me more about Crondis?",
                    3,
                    "Could you tell me more about Scabaras?",
                    4,
                    "Interesting. Thanks for the information.",
                    5,
                )
            when (god) {
                1 -> apmeken()
                2 -> het()
                3 -> crondis()
                4 -> scabaras()
                5 -> {
                    chatPlayer(happy, "Interesting. Thanks for the information.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.apmeken() {
        chatPlayer(quiz, "Could you tell me more about Apmeken?")
        chatNpc(
            happy,
            "Apmeken is the goddess of companionship. She is known to be the most mischievous, " +
                "playful and unpredictable of the minor deities. I'd say she's my personal " +
                "favourite.",
        )
        chatPlayer(shifty, "So I shouldn't trust you then?")
        chatNpc(
            neutral,
            "Apmeken can appeal to the most erudite scholar or the most barefaced liar. I'd like " +
                "to be considered amongst the former category.",
        )
        chatPlayer(quiz, "I see. What does she look like?")
        chatNpc(
            neutral,
            "She has the head of a monkey, or sometimes an ape. It changes depending upon what " +
                "nature she is displaying. For example, the orangutan represents wisdom, while " +
                "the baboon represents mischief.",
        )
    }

    private suspend fun Dialogue.het() {
        chatPlayer(quiz, "Could you tell me more about Het?")
        chatNpc(
            neutral,
            "Het is the least strange of our deities to foreign eyes, for he looks just like us. I " +
                "suspect this is why his shrines are particularly targeted by thieves like that " +
                "infamous Templeton.",
        )
        chatPlayer(quiz, "What does he represent?")
        chatNpc(
            neutral,
            "Het is the god of health, both in mind and body. He is quite popular amongst soldiers " +
                "as a result, especially up in Al Kharid.",
        )
        chatPlayer(quiz, "Just soldiers?")
        chatNpc(
            neutral,
            "Not at all! Like I said, the wellbeing of the mind is also key to Het. Many great " +
                "orators also look to him for guidance. I imagine his teachings help with some of " +
                "their strenuous speeches.",
        )
        chatPlayer(quiz, "How did his followers react to the plagues here?")
        chatNpc(
            neutral,
            "With many arguments. They really couldn't decide whether it was a punishment from " +
                "Het, or an affront to him. I stayed out of that debate.",
        )
    }

    private suspend fun Dialogue.crondis() {
        chatPlayer(quiz, "Could you tell me more about Crondis?")
        chatNpc(neutral, "Crondis is a ferocious lady with the head of a mighty crocodile.")
        chatPlayer(laugh, "That must be awkward when buying helmets.")
        chatNpc(laugh, "Luckily for her, I don't think deities have much need for them.")
        chatPlayer(quiz, "So what is she the goddess of?")
        chatNpc(neutral, "Resourcefulness. A word which seems to be open to large interpretation.")
        chatPlayer(quiz, "How so?")
        chatNpc(
            neutral,
            "Most consider her to be a teacher of modesty. They say we should be resourceful by " +
                "only taking and using that which we absolutely need.",
        )
        chatPlayer(quiz, "But there's others with a different interpretation?")
        chatNpc(
            neutral,
            "Some believe she is actually the goddess of pleasure. They claim that the idea of " +
                "modesty came from a misinterpretation, and that to be resourceful is to take all " +
                "that you can.",
        )
        chatPlayer(confused, "That doesn't sound right.")
        chatNpc(
            neutral,
            "No. It is a more recent take that I suspect was born in Menaphos. There are many in " +
                "that city with more wealth than they'll ever need. Modesty is not an ideal " +
                "they'll ever embody.",
        )
    }

    private suspend fun Dialogue.scabaras() {
        chatPlayer(quiz, "Could you tell me more about Scabaras?")
        chatNpc(
            neutral,
            "There are many opinions out there on Scabaras, but general knowledge isn't always the " +
                "most accurate.",
        )
        chatPlayer(quiz, "Well what are the absolute facts then?")
        chatNpc(
            neutral,
            "Scabaras is the god of isolation. He takes on the appearance of a man with the head " +
                "of a scarab. Both he and his followers were driven into hiding long ago.",
        )
        chatPlayer(quiz, "And?")
        chatNpc(
            neutral,
            "And that's it. You asked for the absolute facts. Anything beyond that is just " +
                "speculation.",
        )
        chatPlayer(
            confused,
            "But there must be more. The High Priest told me that Scabaras tried to conquer these " +
                "lands.",
        )
        chatNpc(
            neutral,
            "So it is said, but the written accounts that exist contain many inconsistencies when " +
                "it comes to what role Scabaras actually played. Of course, most would not know " +
                "that.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(
            shifty,
            "Many of the writings are only available to us priests. That does of course include " +
                "the new High Priest, but perhaps he is yet to read them. They are definitely... " +
                "illuminating.",
        )
        chatPlayer(neutral, "Interesting...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tarik
    // TODO(content): [system:met_flags] "Hello again." on later visits
    // TODO(content): [system:pyramid_plunder] reply after the player has played Pyramid Plunder,
    //  and artefacts or the sceptre used on Tarik
    private suspend fun Dialogue.tarik() {
        chatNpc(sad, "Ouch!")
        chatPlayer(worried, "Are you alright?")
        chatNpc(neutral, "I'm fine, I'm fine. Just a scratch.")
        chatPlayer(quiz, "If you're sure. Who are you?")
        chatNpc(neutral, "Me? I'm Tarik.")
        chatPlayer(quiz, "That hat you're wearing doesn't look like it comes from around here?")
        chatNpc(
            neutral,
            "It was a present. My employer thought it would suit me. I'm not sure it does, though " +
                "it does keep the sun off very well.",
        )
        chatPlayer(quiz, "Who is this employer?")
        chatNpc(
            neutral,
            "Simon Templeton. He's an archaeologist. I worked as an assistant for him, though I " +
                "haven't seen him since the plague business started, so I work for myself now.",
        )
        chatPlayer(quiz, "I see. So what have you been up to?")
        chatNpc(neutral, "Simon suggested that there might be riches to be found in this pyramid.")
        chatNpc(sad, "All I've found so far are injuries though.")
        chatPlayer(quiz, "Interesting... What can you tell me about this pyramid?")
        chatNpc(neutral, "Well, I'm not sure. It's called Jalsavrah apparently. It's a strange place.")
        chatPlayer(quiz, "How so?")
        chatNpc(
            neutral,
            "First of all, there's something odd about the doors. There's four of them, but three " +
                "of them lead to an empty tomb.",
        )
        chatNpc(
            neutral,
            "The other one is guarded by something, and whatever it is, it doesn't like visitors.",
        )
        chatNpc(
            confused,
            "The thing is, the doors seem to change around sometimes. It's all quite confusing.",
        )
        chatPlayer(quiz, "What's this thing guarding one of them.")
        chatNpc(
            neutral,
            "I think it might be a mummy. It's not very friendly, but it does allow you to enter " +
                "some of the rooms in the tomb. They are dangerous though.",
        )
        chatPlayer(happy, "Well I'm not afraid of a little danger.")
        chatNpc(
            worried,
            "Are you sure? The place is full of poisonous snakes and scarabs along with some " +
                "rather nasty mummies as well. You could die in there.",
        )
        chatNpc(
            neutral,
            "I managed to get into one room, but the next room was harder. My lockpicking skills " +
                "weren't good enough. Maybe I should have taken a lockpick.",
        )
        chatPlayer(happy, "It all sounds like fun to me. So, is there anything valuable in there?")
        chatNpc(
            neutral,
            "There are lots of artefacts that you should be able to sell to a legitimate trader...",
        )
        chatPlayer(shifty, "Like your friend Simon Templeton?")
        chatNpc(shifty, "Of course. He's as 'legitimate' as you can get.")
        chatPlayer(quiz, "Is there anything else of value in there?")
        chatNpc(neutral, "I've heard a rumour that there is an ancient magic sceptre somewhere inside.")
        chatPlayer(happy, "Ah, now you've piqued my interest. What do you know about this sceptre?")
        chatNpc(
            neutral,
            "Not a lot. It's apparently made of gold and covered in jewels. It's supposed to have " +
                "some magical powers as well.",
        )
        chatPlayer(happy, "Magical powers? This sounds good. What are they?")
        chatNpc(happy, "I'll let you know when I find it!")
        chatPlayer(laugh, "Not if I find it first!")
        chatNpc(
            angry,
            "Hey! if you find it, I deserve a share of the profits! You wouldn't have known it " +
                "existed without my help.",
        )
        chatPlayer(shifty, "I'll think about it...")
    }
}
