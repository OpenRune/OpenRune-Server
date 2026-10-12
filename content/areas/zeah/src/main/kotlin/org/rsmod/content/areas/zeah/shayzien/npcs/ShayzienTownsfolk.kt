package org.rsmod.content.areas.zeah.shayzien.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ShayzienTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shayzien_historian") { startDialogue(it.npc) { amelia() } }
        onOpNpc1("npc.shayzien_recruitment_captain") { startDialogue(it.npc) { captainBruce() } }
        onOpNpc1("npc.zeah_general_central") { startDialogue(it.npc) { captainRachelle() } }
        onOpNpc1("npc.zeah_raids_captain") { startDialogue(it.npc) { captainSdiar() } }
        onOpNpc1("npc.shayzien_commissioner") { startDialogue(it.npc) { commissionerAnwar() } }
        onOpNpc1("npc.shayzien_pub_patron_4") { startDialogue(it.npc) { drunkenSoldier() } }
        onOpNpc1("npc.shayzien_pub_patron_2") { startDialogue(it.npc) { pubSoldier() } }
        onOpNpc1("npc.shayzien_pub_patron_3") { startDialogue(it.npc) { pubSoldier() } }
        onOpNpc1("npc.shayzien_outofplace_soldier") { startDialogue(it.npc) { tony() } }
        onOpNpc1("npc.osten") { startDialogue(it.npc) { osten() } }
        onOpNpc1("npc.shayzien_eager_sergeant") { startDialogue(it.npc) { ricardo() } }
        onOpNpc1("npc.shayzien_gardener") { startDialogue(it.npc) { elijah() } }
        onOpNpc1("npc.shayzien_church_father") { startDialogue(it.npc) { fatherEysseric() } }
        onOpNpc1("npc.shayzien_church_woman") { startDialogue(it.npc) { mia() } }
        onOpNpc1("npc.shayzien_oldman") { startDialogue(it.npc) { oldMan() } }
        onOpNpc1("npc.shayzien_forester") { startDialogue(it.npc) { jessie() } }
        onOpNpc1("npc.shayzien_dwarf") { startDialogue(it.npc) { kaston() } }
        onOpNpc1("npc.giants_den_natural_historian") { startDialogue(it.npc) { farolt() } }
        onOpNpc1("npc.phileas_rimor") { startDialogue(it.npc) { phileasRimor() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Amelia
    // TODO(content): [quest:dragon_slayer_ii] Camorra and the Shayzien Crypts
    private suspend fun Dialogue.amelia() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Hello. Do you need something?")
        chatPlayer(happy, "Nope. Just thought I'd say hi.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Bruce
    // TODO(content): [system:treasure_trails] master clue light box and puzzle box
    private suspend fun Dialogue.captainBruce() {
        chatNpc(
            happy,
            "Hello there, I'm Captain Bruce. Do you want to serve in the Shayzien Army and " +
                "defend our kingdom?",
        )
        if (!choice2("Yes, I'd love to join.", true, "No, I'm not interested.", false)) {
            chatPlayer(neutral, "No, I'm not interested.")
            chatNpc(neutral, "That's alright. Come back if you change your mind.")
            return
        }
        chatPlayer(happy, "Yes, I'd love to join.")
        chatNpc(happy, "Wonderful! To get started, you need to answer a few questions.")
        chatNpc(quiz, "Are you a resident of Kourend?")
        chatPlayer(confused, "Errrr... Not really.")
        chatNpc(sad, "Ah. Then you cannot join I'm afraid.")
        chatPlayer(quiz, "How come?")
        chatNpc(
            neutral,
            "It's in the rules I'm afraid. Only citizens of Kourend are allowed to join the " +
                "Shayzien Army.",
        )
        chatPlayer(quiz, "Is that not too restrictive?")
        chatNpc(
            neutral,
            "Well at one time only citizens of Shayzien could join the army. It wasn't until the " +
                "reign of King Byrne I that people from other parts of the kingdom were allowed " +
                "to enlist.",
        )
        chatNpc(
            neutral,
            "Of course, many people in Kourend still like to stick to tradition. After all, the " +
                "five cities of Kourend have always had very defined roles within the kingdom.",
        )
        chatPlayer(quiz, "Interesting. So most people in the army are still from Shayzien?")
        chatNpc(
            neutral,
            "Indeed. While the Shayzien Army is by far the largest military force in Kourend, " +
                "the other cities still have some form of militia, apart from Arceuus of course.",
        )
        chatNpc(
            neutral,
            "Even if someone from elsewhere in the kingdom wants to become a soldier, they're " +
                "far more likely to join their local city force than come to us.",
        )
        chatNpc(
            happy,
            "Still, times are slowly changing. Every year we see more people from outside " +
                "Shayzien enlist.",
        )
        chatPlayer(neutral, "I see. Thanks for the information.")
        chatNpc(happy, "My pleasure.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Rachelle
    // TODO(content): [verify:in_game] the favour-met reply is assumed since Kourend favour was
    //  removed
    private suspend fun Dialogue.captainRachelle() {
        chatNpc(angry, "Attention! Front and centre!")
        chatPlayer(shocked, "Yes ma'am!")
        val title = if (access.isBodyTypeA()) "sir" else "lady"
        chatNpc(neutral, "I have to ask you to get back from here $title.")
        chatPlayer(quiz, "Why? What's going on here?")
        chatNpc(
            neutral,
            "This is the Lizardman Canyon. The lizardmen from the west are desperately trying to " +
                "invade Great Kourend.",
        )
        chatNpc(
            neutral,
            "We were able to hold them back, so they've started tunnelling under the kingdom and " +
                "into this canyon to bypass our defences.",
        )
        chatNpc(
            neutral,
            "Luckily, we were able to work out what they were doing and move to stop them. So " +
                "far we've managed to keep them trapped within the canyon, but we need to " +
                "constantly hold them at bay.",
        )
        chatPlayer(
            happy,
            "I'm a very strong adventurer. Would I be able to help out in the battle?",
        )
        chatNpc(happy, "You may. Head into the canyon and take the fight to those monsters!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Sdiar
    private suspend fun Dialogue.captainSdiar() {
        chatPlayer(quiz, "What's going on here?")
        chatNpc(
            neutral,
            "Beyond this wall you'll find the Kebos Lowlands. Most of the region is lizardman " +
                "territory. We're here to keep them from entering Kourend.",
        )
        chatNpc(
            angry,
            "We used to keep this passage properly barricaded shut, but then the fancy " +
                "historians from the Arceuus Library wanted to go do some research out there.",
        )
        chatNpc(
            angry,
            "Soon the farmers joined them, and before you knew it, everyone wanted to go " +
                "explore. Some of them have even started living out there.",
        )
        chatNpc(
            angry,
            "I can see it all ending badly, but that's the problem with intellectuals - they " +
                "always think they know better.",
        )
        chatNpc(
            neutral,
            "Anyway, maybe they'll all get themselves killed. Then we can put the barricade back " +
                "up.",
        )
        chatNpc(
            angry,
            "If you go out there yourself, give the lizardmen a kicking from me. They've killed " +
                "too many of my soldiers.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Commissioner_Anwar
    private suspend fun Dialogue.commissionerAnwar() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Good day to you.")
        var option = choice3("Who are you?", 1, "What is this place?", 2, "See you later.", 3)
        while (true) {
            when (option) {
                1 -> {
                    anwarWhoAreYou()
                    option =
                        choice3(
                            "What's the Shayzien Constabulary?",
                            4,
                            "What is this place?",
                            2,
                            "See you later.",
                            3,
                        )
                }
                2 -> {
                    anwarThisPlace()
                    option =
                        choice3("Who are you?", 1, "What is this place?", 2, "See you later.", 3)
                }
                4 -> {
                    anwarConstabulary()
                    return
                }
                else -> {
                    chatPlayer(neutral, "See you later.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.anwarWhoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "I'm Commissioner Anwar. I'm in charge of the Shayzien Constabulary. With Lord " +
                "Shayzien currently busy with the war efforts, I'm also assisting him with his " +
                "duties here in the city.",
        )
    }

    private suspend fun Dialogue.anwarConstabulary() {
        chatPlayer(quiz, "What's the Shayzien Constabulary?")
        chatNpc(
            neutral,
            "The Shayzien Army is split into six regiments plus the Shayzien Constabulary. While " +
                "the six regiments are jointly responsible for protecting Kourend from external " +
                "threats, we deal with issues closer to home.",
        )
        chatPlayer(quiz, "What are the six regiments?")
        chatNpc(
            neutral,
            "The Intelligence Corps, the Medical Corps, the Engineering Corps, the Logistics " +
                "Corps, the Rangers and of course, the Infantry. Each regiment is led by a " +
                "general who reports directly to Lord Shayzien.",
        )
        chatPlayer(
            quiz,
            "It feels like there must be a bit of overlap between the Constabulary and the " +
                "regiments, especially the Intelligence Corps.",
        )
        chatNpc(
            neutral,
            "On paper we're totally separate from the regiments. However, in reality you are " +
                "correct. The distinction between internal and external threats grows murkier by " +
                "the day.",
        )
        chatNpc(
            neutral,
            "More and more we find ourselves working alongside the Intelligence Corps and the " +
                "other regiments. In particular, they've been supporting us in combating the " +
                "rising gang threat in Port Piscarilius.",
        )
    }

    private suspend fun Dialogue.anwarThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            neutral,
            "This is the Shayzien Administration. The city of Shayzien is run from here.",
        )
        chatPlayer(neutral, "It seems very quiet.")
        chatNpc(
            neutral,
            "A lot of focus right now is on the war with the lizardmen. Most senior members of " +
                "the Shayzien Army, including Lord Shayzien himself, are working out of the " +
                "Shayzien Encampment instead.",
        )
        chatPlayer(quiz, "So the Shayzien Army are also responsible for running the city?")
        chatNpc(
            neutral,
            "Indeed. There's more of a separation between the city of Shayzien and the Shayzien " +
                "Army now than there used to be, but this will likely always be a military " +
                "settlement.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Drunken_soldier
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.drunkenSoldier() {
        chatNpc(drunk, "'Ow yer doin', pal?")
        chatPlayer(quiz, "Just passing through, thanks. Are you feeling okay?")
        chatNpc(drunk, "N... Never been better!")
        chatPlayer(
            worried,
            "Just making sure... You seem a little unsturdy on your feet there.",
        )
        chatNpc(
            drunk,
            "I like you, mate. We've really c-*hic*-connected. You're like a brother to me.",
        )
        chatNpc(
            drunk,
            "You see any of those lizards 'round here, you give me a shout. I'll show 'em. Smash " +
                "'em.",
        )
        chatPlayer(worried, "I'm going to go now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Soldier_(The_Cloak_and_Stagger)
    private suspend fun Dialogue.pubSoldier() {
        chatPlayer(happy, "Not often you see a couple of soldiers out for a drink.")
        chatNpc(
            happy,
            "We've just finished our first duties in the Lizardman Canyon. A few drinks at the " +
                "Cloak and Stagger to celebrate is always traditional.",
        )
        chatPlayer(happy, "Sounds like you earned it. Enjoy!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:New_Recruit_Tony
    // TODO(content): [system:treasure_trails] master clue light box and puzzle box
    private suspend fun Dialogue.tony() {
        chatNpc(angry, "No, no, no! I won't get back to training. Leave me alone!")
        chatNpc(
            worried,
            "Oh, you're not the drill sergeant. Please don't tell her I'm hiding over here.",
        )
        val why =
            choice2(
                "Why are you hiding?",
                true,
                "I'm going to have to tell the sergeant about this.",
                false,
            )
        if (why) {
            chatPlayer(quiz, "Why are you hiding?")
            chatNpc(
                sad,
                "When I was growing up, all I ever wanted to be was a fisherman. I wanted to go " +
                    "and live in Port Piscarilius and sail the vast oceans.",
            )
            chatNpc(
                sad,
                "My parents though... they hate the people of Piscarilius. They said I had to " +
                    "become a soldier and bring honour to Shayzien.",
            )
            chatNpc(
                sad,
                "I just can't handle it though. The training is too intense, and my heart's just " +
                    "not in it. That's why I'm out here. I'm not going back until the drill " +
                    "sergeant finishes her session.",
            )
            chatPlayer(
                happy,
                "That's awful! If I were you, I wouldn't let my dreams stay as dreams. You " +
                    "should go to Port Piscarilius and be the fisherman you always wanted to be!",
            )
            chatPlayer(
                laugh,
                "Besides, otherwise you're probably going to be waiting an awfully long time for " +
                    "that drill sergeant to be done...",
            )
            return
        }
        chatPlayer(angry, "I'm going to have to tell the sergeant about this.")
        chatPlayer(
            angry,
            "All the other soldiers are out there doing their best to train so that they're " +
                "ready to protect and serve all of Great Kourend! Yet here you are, just " +
                "wandering around.",
        )
        chatNpc(
            worried,
            "Please, please! I'll do anything. Please don't tell the sergeant! I'll be the " +
                "laughing stock of the whole kingdom.",
        )
        chatPlayer(shifty, "Anything? Hmm... how about 2,147,483,647 coins?")
        chatNpc(
            worried,
            "I don't have that much! I'll give you all I can for you to keep quiet. How is one " +
                "gold coin?",
        )
        chatPlayer(
            laugh,
            "Ha! I was just kidding. You can keep your money and I'll keep your secret to " +
                "myself. See you around.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Osten
    // TODO(content): [system:graceful_recolour] recolouring graceful pieces used on him
    private suspend fun Dialogue.osten() {
        chatNpc(
            happy,
            "Welcome, traveller. Are you looking to represent one of the houses of Great Kourend?",
        )
        val option =
            choice3(
                "Tell me more.",
                1,
                "Actually, I just wanted to buy some clothes.",
                2,
                "Not right now.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Tell me more.")
                chatNpc(
                    neutral,
                    "As you are probably aware, Great Kourend is a vast kingdom. Many " +
                        "adventurers choose to wear special clothing which allows them to " +
                        "explore swiftly and with elegance.",
                )
                chatNpc(
                    neutral,
                    "As official tailor for Kourend, I can recolour this clothing, allowing " +
                        "adventurers to represent the city to which they are most loyal.",
                )
                chatNpc(
                    happy,
                    "If you are interested, I would be more than happy to offer my tailoring " +
                        "services in exchange for Marks of Grace.",
                )
                val recolour =
                    choice2(
                        "I'd like to recolour my graceful clothing.",
                        true,
                        "Not right now.",
                        false,
                    )
                if (recolour) {
                    chatPlayer(happy, "I'd like to recolour my graceful clothing.")
                    chatNpc(
                        happy,
                        "Fantastic, simply pass me the items of clothing you would like " +
                            "recoloured, and we can get started.",
                    )
                } else {
                    chatPlayer(neutral, "Not right now.")
                }
            }
            2 -> {
                chatPlayer(neutral, "Actually, I just wanted to buy some clothes.")
                chatNpc(
                    happy,
                    "Oh, certainly. My assistant over there, Robyn, will be happy to help you.",
                )
            }
            else -> chatPlayer(neutral, "Not right now.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sergeant_Ricardo
    private suspend fun Dialogue.ricardo() {
        chatNpc(happy, "Ah, hello! Isn't it a wonderful day?")
        chatPlayer(quiz, "You seem happy.")
        chatNpc(
            happy,
            "And why wouldn't I be? I'm part of the best fighting force in the world! The " +
                "Shayzien Army!",
        )
        chatPlayer(quiz, "The best?")
        chatNpc(happy, "Of course!")
        chatPlayer(quiz, "So the Shayzien Army has no weaknesses at all?")
        chatNpc(
            neutral,
            "Well... I suppose we don't really have any magical capabilities. That's more an " +
                "Arceuus thing. Though of course, they're mostly pacifists. Apparently they " +
                "didn't even properly fight in the War of the Five Houses.",
        )
        chatNpc(
            laugh,
            "Still, I guess 'War of the Four Houses while House Arceuus watched' isn't as catchy " +
                "a name.",
        )
        chatPlayer(quiz, "Right... But apart from magic, the Shayzien Army has no weaknesses?")
        chatNpc(
            neutral,
            "Well... we are just an army. We don't have a navy, so can't really deal with any " +
                "threats at sea. Lady Piscarilius is meant to be in charge of the Kourend Navy. " +
                "Problem is, it doesn't actually exist.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(
            neutral,
            "Lady Piscarilius has little interest in that sort of thing. Lord Shayzien wants to " +
                "establish a Shayzien Navy instead, but there's a lot of pushback from the rest " +
                "of the kingdom.",
        )
        chatNpc(
            neutral,
            "They worry about Shayzien having too much power. It does mean we'll have big " +
                "problems against any enemy navy, but on their heads be it I suppose.",
        )
        chatNpc(
            happy,
            "Anyway, apart from those minor issues, the Shayzien Army has absolutely no " +
                "weaknesses!",
        )
        chatPlayer(neutral, "If you say so. See you later.")
        chatNpc(happy, "Goodbye!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elijah
    private suspend fun Dialogue.elijah() {
        chatPlayer(quiz, "Hello. Can you give me any farming advice?")
        chatNpc(angry, "Can't you see I'm busy?")
        chatPlayer(
            neutral,
            "Oh, sorry. I thought you might be able to help me with farming like other gardeners.",
        )
        chatNpc(
            angry,
            "Well, I'm not the type of gardener you think I am. Just leave me alone please.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Father_Eysseric
    private suspend fun Dialogue.fatherEysseric() {
        chatPlayer(happy, "I'm looking for a quest!")
        chatNpc(neutral, "In this holy place I can only offer you calm and peace.")
        chatPlayer(
            happy,
            "Calm and Peace? Sounds like the quest for me! How should I start?",
        )
        chatNpc(
            neutral,
            "Yes, calm and peace. Just stay here with me and you will feel it eventually.",
        )
        chatPlayer(confused, "Oh... well... thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mia
    private suspend fun Dialogue.mia() {
        chatPlayer(quiz, "Hello, you look worried.")
        chatNpc(
            worried,
            "My husband is serving near the Lizardman Canyon. I'm worried that something might " +
                "happen to him.",
        )
        chatNpc(
            sad,
            "And I lost my job last week. I need to find another one before my savings run out.",
        )
        chatPlayer(sad, "Oh, that is bad. I'm sorry to hear that.")
        chatNpc(
            sad,
            "Well, it's a difficult time for a lot of people I guess. The war with the lizardmen " +
                "never ends.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Old_man_(Shayzien)
    private suspend fun Dialogue.oldMan() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Hello traveller, do you know what this is?")
        chatPlayer(neutral, "It looks like a monument.")
        chatNpc(
            neutral,
            "Yes. I served in the military for 30 years. This monument always reminds me of my " +
                "days in the army.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jessie
    private suspend fun Dialogue.jessie() {
        chatNpc(happy, "Hello friend.")
        chatPlayer(quiz, "Hello, what are you doing here?")
        chatNpc(happy, "My name is Jessie. I help look after the forest.")
        chatPlayer(happy, "Nice to meet you. You must know this area very well then.")
        chatNpc(
            neutral,
            "I do. There're all sorts of interesting creatures around here. Be careful, some of " +
                "them are not very friendly. A lot of travellers have become their victims.",
        )
        chatPlayer(neutral, "I see. Thanks for the warning.")
        chatNpc(happy, "No problem! Safe travels my friend.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kaston
    private suspend fun Dialogue.kaston() {
        chatPlayer(happy, "Hello, you must be a traveller.")
        chatNpc(happy, "Well guessed, adventurer.")
        while (true) {
            val option =
                choice3(
                    "Are you from Lovakengj?",
                    1,
                    "How do you feel about this place?",
                    2,
                    "See you later.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Are you from Lovakengj?")
                    chatNpc(
                        happy,
                        "You are wrong this time, adventurer. I'm actually from Ice Mountain.",
                    )
                    chatNpc(
                        neutral,
                        "I always wanted to visit Lovakengj to see the Blast Mine. This year I " +
                            "finally saved up enough to travel here.",
                    )
                    chatNpc(
                        sad,
                        "However, the war with the lizardmen means Lovakengj is dangerous to get " +
                            "to from here. I'll need to go the long way round.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What do you think about this place?")
                    chatNpc(
                        neutral,
                        "To be honest, Shayzien is a bit boring for visitors. It's all about war " +
                            "and military things.",
                    )
                    chatNpc(
                        happy,
                        "The only place I would recommend is the Cloak and Stagger. You'll find " +
                            "some pretty good drinks there.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "See you later.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farolt
    private suspend fun Dialogue.farolt() {
        chatNpc(happy, "Hello there!")
        chatPlayer(happy, "Greetings. It's nice to see another adventurer here.")
        chatNpc(
            neutral,
            "Adventurer? No. I'm a natural historian from Varrock. I'm working on a project to " +
                "research the creatures of Kourend.",
        )
        chatNpc(
            happy,
            "Don't you think this place is so fascinating? It looks like this cave is a major " +
                "habitat for the giants.",
        )
        chatPlayer(
            quiz,
            "Yeah, there are many different giants here for sure. How did you manage to get this " +
                "far? Those giants are quite hostile to visitors.",
        )
        chatNpc(laugh, "Well, I just ran past them as fast as I could.")
        chatNpc(neutral, "Anyway, I'd better get back to my research.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Phileas_Rimor (before Tale of the Righteous)
    // TODO(content): [quest:tale_of_the_righteous] start offer for players who meet the
    //  requirements and the quest dialogue
    private suspend fun Dialogue.phileasRimor() {
        chatPlayer(happy, "Good day.")
        chatNpc(happy, "Hello there. Not very often I get visitors. What brings you here?")
        val option =
            choice3("Who are you?", 1, "Do you need help with anything?", 2, "Nothing.", 3)
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "The name's Phileas Rimor. I was once a soldier in the Shayzien Army but now " +
                        "I'm retired.",
                )
                phileasHelp()
            }
            2 -> phileasHelp()
            else -> {
                chatPlayer(neutral, "Nothing.")
                chatNpc(neutral, "Oh, okay then.")
            }
        }
    }

    private suspend fun Dialogue.phileasHelp() {
        chatPlayer(quiz, "Do you need help with anything?")
        chatNpc(happy, "I'm fine thank you. Thanks for asking though.")
        mesbox("You do not meet all of the requirements to start the Tale of the Righteous quest.")
    }
}
