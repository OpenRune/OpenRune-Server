package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KeldagrimTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dwarf_city_dwarf_man11") { startDialogue(it.npc) { audmann() } }
        onOpNpc1("npc.dwarf_city_excentric_dwarf") { startDialogue(it.npc) { dromund() } }
        onOpNpc1(HAERA) { startDialogue(it.npc) { haera() } }
        onOpNpc1(HEGIR) { startDialogue(it.npc) { hegir() } }
        onOpNpc1("npc.dwarf_city_dwarf_man7") { startDialogue(it.npc) { myndill() } }
        onOpNpc1("npc.dwarf_city_gardener_dwarf") { startDialogue(it.npc) { rind() } }
        onOpNpc1("npc.dwarf_city_dwarf_man9") { startDialogue(it.npc) { tombar() } }
        onOpNpc1("npc.dwarf_city_dwarf_man2") { startDialogue(it.npc) { bentamir() } }
        onOpNpc1("npc.dwarf_city_dwarf_man5") { startDialogue(it.npc) { karl() } }
        onOpNpc1("npc.dwarf_city_dwarf_man4") { startDialogue(it.npc) { reinald() } }
        for (riki in RIKI) {
            onOpNpc1(riki) { startDialogue(it.npc) { riki() } }
        }
        onOpNpc1("npc.dwarf_city_shop_sculpture") { startDialogue(it.npc) { blasidar() } }
        onOpNpc1("npc.dwarf_city_rowdy_dwarf") { startDialogue(it.npc) { rowdyDwarf() } }
        onOpNpc1("npc.dwarf_city_old_dwarf3") { startDialogue(it.npc) { runvastr() } }
        onOpNpc1("npc.dwarf_city_dwarf_man1") { startDialogue(it.npc) { sune() } }
        onOpNpc1("npc.dwarf_city_dwarf_man3") { startDialogue(it.npc) { ulifed() } }
        onOpNpc1("npc.dwarf_city_dwarf_man10") { startDialogue(it.npc) { odmar() } }
        onOpNpc1("npc.dwarf_city_shop_ship_mechanic") { startDialogue(it.npc) { wemund() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Audmann
    private suspend fun Dialogue.audmann() {
        chatNpc(angry, "Oh, don't bother me human.")
        chatPlayer(confused, "Why not? What's wrong?")
        chatNpc(
            angry,
            "You are wrong, human. Your attire is outrageous. Your presence is obnoxious.",
        )
        chatPlayer(shocked, "What? What are you saying?")
        chatNpc(angry, "I'm saying you're in my way.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dromund
    private suspend fun Dialogue.dromund() {
        if (QuestRequirements.hasNotCompleted(player, GIANT_DWARF)) {
            // TODO(content): [quest:the_giant_dwarf] the stolen-boots line once the player has
            //  taken them during the quest
            chatNpc(angry, "Get out of my house!")
            chatPlayer(worried, "Alright, alright, I was just leaving!")
            return
        }
        chatNpc(
            sad,
            "Someone stole my beautiful boots... I had to buy some crummy replicas, the real " +
                "boots were one of a kind.",
        )
        chatPlayer(
            neutral,
            "Don't worry, if I find out who did it I'll take care of them.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Haera
    private suspend fun Dialogue.haera() {
        couple(HAERA, HAERA, angry, "What are you doing in our home?")
        chatPlayer(neutral, "I just came to ask you a question.")
        couple(HAERA, HAERA, angry, "Hegir, why did you let this human into our home?")
        couple(HAERA, HEGIR, confused, "I didn't let him in, he let himself in!")
        couple(HAERA, HAERA, angry, "Now what do you want, human? Speak up, speak up!")
        chatPlayer(worried, "I ehm, just wanted some information.")
        couple(
            HAERA,
            HAERA,
            angry,
            "Well go ask someone else instead of barging into people's homes!",
        )
        couple(HAERA, HEGIR, neutral, "There's no need to be rude to the human, Haera dear.")
        couple(HAERA, HAERA, verymad, "Now don't you dear me. I'll do whatever I please.")
        chatPlayer(worried, "I think this is my cue to leave...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hegir
    private suspend fun Dialogue.hegir() {
        chatPlayer(neutral, "Hello, can I ask you a question?")
        couple(HEGIR, HEGIR, happy, "Oh yes, young fellow? Yes, what do you want?")
        couple(HEGIR, HAERA, angry, "Who's that you're talking to Hegir?")
        couple(HEGIR, HEGIR, neutral, "Let me see, he appears to be a human of some sort.")
        couple(HEGIR, HEGIR, quiz, "What was your name again, human?")
        chatPlayer(neutral, "${player.displayName}.")
        couple(HEGIR, HEGIR, neutral, "It's ${player.displayName}!")
        couple(HEGIR, HAERA, angry, "Well ask him what he wants then!")
        couple(HEGIR, HEGIR, quiz, "What do you want, human?")
        chatPlayer(confused, "Errr... I don't know... A quest, maybe?")
        couple(HEGIR, HEGIR, neutral, "He says he's looking for his quest!")
        couple(HEGIR, HAERA, angry, "What makes him think he lost his quest in here?")
        couple(HEGIR, HEGIR, quiz, "What makes you think you lost your quest in here?")
        chatPlayer(confused, "No no, I mean, do you have a task of some sort for me?")
        couple(HEGIR, HEGIR, neutral, "Now he says he wants a task!")
        couple(
            HEGIR,
            HAERA,
            verymad,
            "Stop talking to that human and get back in here before I have a task for you!",
        )
        couple(HEGIR, HEGIR, sad, "No need to talk to me like that, Haera.")
        couple(HEGIR, HAERA, angry, "I'll talk to you in whatever way I like!")
        chatPlayer(worried, "I think I hear someone calling my name in the distance...")
    }

    private suspend fun Dialogue.couple(
        talkedTo: String,
        speaker: String,
        mesanim: MesAnimType,
        text: String,
    ) {
        if (speaker == talkedTo) {
            chatNpc(mesanim, text)
        } else {
            chatNpcSpecific(if (speaker == HAERA) "Haera" else "Hegir", speaker, mesanim, text)
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Myndill
    private suspend fun Dialogue.myndill() {
        chatNpc(neutral, "Hello there, human.")
        while (true) {
            val option =
                choice3(
                    "Do you have any news?",
                    1,
                    "Do you have any quests?",
                    2,
                    "See you later!",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Do you have any news?")
                    if (QuestRequirements.hasNotCompleted(player, GIANT_DWARF)) {
                        chatNpc(
                            quiz,
                            "Well, I heard some human knocked over the statue of King Alvis " +
                                "recently. You know anything about that?",
                        )
                        chatPlayer(
                            worried,
                            "Hey, it wasn't my fault! I was just travelling on the boat!",
                        )
                        continue
                    }
                    chatNpc(worried, "The Red Axe has departed the city recently. Ill omens.")
                    chatPlayer(quiz, "How is that? Do you know anything about this?")
                    chatNpc(
                        neutral,
                        "No, it's just that no company has ever willingly left the Consortium " +
                            "before.",
                    )
                    chatPlayer(quiz, "Companies have been forced out of the Consortium then?")
                    chatNpc(
                        neutral,
                        "Oh yes. It hasn't happened for a long while now, but sometimes a " +
                            "company goes bankrupt. Or the value of the company becomes so low " +
                            "that it's relegated to being a minor company.",
                    )
                    chatPlayer(happy, "I see, thank you.")
                }
                2 -> {
                    chatPlayer(quiz, "Do you have any quests?")
                    chatNpc(neutral, "Let me think for a moment...")
                    chatNpc(
                        neutral,
                        "No, I have nothing that I could possible want doing. Try the shops or " +
                            "the market instead.",
                    )
                }
                else -> {
                    chatPlayer(happy, "See you later!")
                    chatNpc(neutral, "Perhaps!")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rind_the_gardener
    private suspend fun Dialogue.rind() {
        chatNpc(happy, "Come to visit my garden then, have you, human?")
        chatPlayer(quiz, "This is your garden?")
        chatNpc(
            neutral,
            "No no, not quite. But I do look after it. It's the palace garden, you see. I keep " +
                "it tidy, keep it living.",
        )
        chatNpc(
            neutral,
            "It's hard to grow much on these hard rocks deep beneath these mountains, you know.",
        )
        chatPlayer(
            quiz,
            "I can imagine. Where do you get all your food from? Or even the soil for this " +
                "garden?",
        )
        chatNpc(
            neutral,
            "We can grow a little food underground, but most of it comes from the extensive " +
                "steamcart network that runs beneath the earth.",
        )
        chatNpc(
            neutral,
            "In the Era of Kings, many centuries ago, it was harder of course. Our steam " +
                "technology was not developed as far yet and getting fresh supplies of food " +
                "was always a problem.",
        )
        chatNpc(quiz, "Oh, I'm not boring you, am I?")
        when (choice3("Not at all!", 1, "A little, yes.", 2, "Not really, no.", 3)) {
            1 -> rindHistory()
            2 -> {
                chatPlayer(
                    bored,
                    "A little, yes. I don't really know what all this has to do with anything.",
                )
                chatNpc(
                    sad,
                    "I suppose it doesn't, really, it's just the mumblings of an old dwarf.",
                )
            }
            else -> {
                chatPlayer(neutral, "Not really, no.")
                chatNpc(neutral, "Oh, alright then.")
            }
        }
    }

    private suspend fun Dialogue.rindHistory() {
        chatPlayer(happy, "Not at all! Please, go on!")
        chatNpc(
            neutral,
            "Oh yes, as I was saying, it was difficult to make a decent living here. I'm afraid " +
                "this garden was rather neglected during that time.",
        )
        chatPlayer(
            quiz,
            "Why did you dwarves stay so deep under ground, then? Why not simply go above " +
                "ground?",
        )
        chatNpc(shocked, "What, with the wars raging up there?")
        chatNpc(
            neutral,
            "We dwarves went underground a long time ago, see. For a while the city of " +
                "Keldagrim was completely cut off from the rest of Gielinor simply waiting out " +
                "the wars of the humans and the gods.",
        )
        chatNpc(
            neutral,
            "This made it hard, though, to determine when the wars above the ground were over. " +
                "Perhaps Keldagrim had remained hidden for far too long.",
        )
        chatNpc(
            neutral,
            "Only when King Alvis heroically defended the city against an invasion of trolls " +
                "did we dare to venture out and see how much the world had changed.",
        )
        chatNpc(sad, "Of course, after that...")
        chatNpc(
            neutral,
            "Ah well, enough history lessons I should say. I must really get back to my " +
                "gardening.",
        )
        chatPlayer(happy, "Thanks for the info!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tombar
    private suspend fun Dialogue.tombar() {
        chatPlayer(quiz, "Say, aren't you a bit tall for a dwarf?")
        chatNpc(neutral, "Was there anything in particular you wanted?")
        val quest =
            choice2(
                "I'd like a quest please.",
                true,
                "No, I just like talking to strangers.",
                false,
            )
        if (quest) {
            chatPlayer(neutral, "I'd like a quest please.")
            chatNpc(
                neutral,
                "I have nothing to do for you, I'm afraid. Ask around town, though, there are " +
                    "always people who need some work done around here.",
            )
        } else {
            chatPlayer(happy, "No, I just like talking to strangers.")
            chatNpc(angry, "Well I don't.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bentamir
    private suspend fun Dialogue.bentamir() {
        chatNpc(angry, "Do you mind? You're in my home.")
        chatPlayer(laugh, "I'm sorry, should I have knocked?")
        chatNpc(angry, "Very funny, human.")
        chatNpc(
            neutral,
            "We can't all live in plush houses, you know. But that doesn't mean us mining " +
                "dwarves don't work hard.",
        )
        chatPlayer(quiz, "Where do you do your mining?")
        chatNpc(
            neutral,
            "Normally the coal mine to the north. We need a lot of coal to keep our steam " +
                "engines going, you know.",
        )
        chatPlayer(quiz, "Can I do a bit of mining there as well?")
        chatNpc(
            shifty,
            "I'm not sure, but as long as no one notices I don't think anyone is going to care.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Karl
    private suspend fun Dialogue.karl() {
        chatNpc(happy, "Hello there, human! Come in, come in!")
        chatPlayer(happy, "Hi, nice to see you!")
        chatNpc(quiz, "Tell me, what brings you here?")
        chatPlayer(
            neutral,
            "Well, first I came to this city just for the adventure and excitement, but then I " +
                "knocked over this statue, you see. Well, I didn't really knock it over myself, " +
                "I was on this boat.",
        )
        chatNpc(happy, "A boat! Fascinating, go on!")
        chatPlayer(
            neutral,
            "Yes, well, this boatman was a bit rubbish, or there was something wrong with the " +
                "ship, I don't know. And then I got arrested by the Black Guard.",
        )
        chatNpc(shocked, "Not the Black Guard!")
        chatPlayer(
            neutral,
            "Yes, and they took me to this Veldaban person, who pretended to be really angry " +
                "but actually he was really nice and he asked me for my help.",
        )
        if (QuestRequirements.hasCompleted(player, GIANT_DWARF)) {
            chatPlayer(
                happy,
                "And after a whole lot of running around, I finally managed to get the statue to " +
                    "be rebuilt.",
            )
        } else {
            chatPlayer(neutral, "So here I am, helping to rebuild the statue.")
        }
        chatNpc(happy, "Such an interesting tale!")
        chatNpc(quiz, "Now how did you come to be in Keldagrim again?")
        chatPlayer(bored, "Never mind.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reinald
    private suspend fun Dialogue.reinald() {
        chatNpc(neutral, "Welcome to my home. Is there anything you wanted?")
        chatPlayer(quiz, "Nothing in particular. Got any sightseeing tips?")
        chatNpc(neutral, "No, I'm just a factory worker. Don't do tours.")
        chatPlayer(confused, "This doesn't look like a factory.")
        chatNpc(neutral, "This is my home. I'm off-duty.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Riki_the_sculptor%27s_model
    private suspend fun Dialogue.riki() {
        if (QuestRequirements.hasNotCompleted(player, GIANT_DWARF)) {
            chatPlayer(happy, "Hello there, what are you doing here?")
            chatNpc(neutral, "I... am the sculptor's associate.")
            chatPlayer(quiz, "His associate? What does that mean?")
            chatPlayer(confused, "Are you going to say anything meaningful at all?")
            chatNpc(neutral, "I...-")
            chatPlayer(bored, "No, apparantly not.")
            return
        }
        chatPlayer(happy, "I'm glad I don't have to talk to you anymore!")
        chatNpc(neutral, "Hrm.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blasidar_the_sculptor (after The Giant Dwarf)
    private suspend fun Dialogue.blasidar() {
        chatNpc(happy, "The new statue looks beautiful, don't you agree?")
        chatPlayer(neutral, "Oh yes, quite.")
        chatNpc(happy, "My finest piece of work, without a doubt.")
        chatPlayer(quiz, "Say, I was wondering, did you do the statues down in the mines as well?")
        chatNpc(confused, "What, out with the trolls?")
        chatNpc(
            neutral,
            "If only! But no, they've been there for many thousands of years, actually. Been " +
                "there before dwarven recorded history.",
        )
        chatPlayer(confused, "No no, I'm sure they weren't there before!")
        chatNpc(laugh, "Of course they were, don't be silly!")
        chatPlayer(confused, "Alright... my mistake then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rowdy_dwarf
    // TODO(content): [system:keldagrim_tasks] item requests
    private suspend fun Dialogue.rowdyDwarf() {
        chatNpc(drunk, "Get OUT of my WAY!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Runvastr
    private suspend fun Dialogue.runvastr() {
        chatNpc(sad, "Oh, leave an old dwarf in peace will you?")
        chatPlayer(quiz, "Why, what's wrong?")
        chatNpc(
            sad,
            "Ninenty years down in the mines, and this is all I got out of it. All my life " +
                "wasted away.",
        )
        chatPlayer(neutral, "You've still got a nice house, don't you?")
        chatNpc(sad, "A crummy old house in East, that's all my life amounts to.")
        chatPlayer(neutral, "I'll leave you alone then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sune
    // TODO(content): [verify:in_game] "Sorry, I'm busy!" while he fetches Tati's errands
    private suspend fun Dialogue.sune() {
        chatNpc(bored, "Can you leave me alone please? I'm trying to get a bit of rest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ulifed
    private suspend fun Dialogue.ulifed() {
        chatNpc(quiz, "Say, aren't you the human who got arrested here the other day, by the boat?")
        chatPlayer(neutral, "Yes, but we cleared up the whole situation.")
        chatNpc(neutral, "Right, right.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Odmar (after The Giant Dwarf)
    private suspend fun Dialogue.odmar() {
        chatNpc(confused, "What to buy? What to buy?")
        chatNpc(neutral, "Oh, hello human.")
        chatPlayer(quiz, "Anything I can help you with?")
        chatNpc(neutral, "Only if you can help me make a suggestion on what to buy.")
        chatPlayer(happy, "I smell a quest!")
        chatNpc(neutral, "No, not really. I just need some advice on what to buy, that's all.")
        val option =
            choice5(
                "New clothes.",
                1,
                "A warhammer.",
                2,
                "A piece of rope.",
                3,
                "An oversized hat.",
                4,
                "Nothing.",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(
                    neutral,
                    "You should buy some new clothes. You can't go around town wearing what " +
                        "you're wearing.",
                )
                chatNpc(
                    neutral,
                    "You're right, this is a little drab. I shouldn't really go around " +
                        "Keldagrim-West wearing this, should I?",
                )
                chatNpc(
                    worried,
                    "Of course I still don't know what colour I should get. I don't want to " +
                        "accidentally offend the wrong company of course.",
                )
                chatPlayer(neutral, "Never mind.")
            }
            2 -> {
                chatPlayer(happy, "You should get yourself a big mighty warhammer.")
                chatNpc(
                    neutral,
                    "I don't think so. The warhammer shop is run by the Red Axe. I'd rather " +
                        "stay out of there.",
                )
                chatPlayer(quiz, "You don't like the Red Axe?")
                chatNpc(angry, "They're all a lot of bullies if you ask me.")
                chatPlayer(quiz, "But I thought they left the city?")
                chatNpc(
                    shifty,
                    "They may have, but I still think that shopkeeper is in league with them, " +
                        "banners or no.",
                )
            }
            3 -> {
                chatPlayer(neutral, "Get yourself a long piece of rope.")
                chatNpc(confused, "What would I do with a rope?")
                chatPlayer(
                    neutral,
                    "I don't know. I generally find a rope useful while adventuring.",
                )
                chatNpc(neutral, "I don't do a lot of adventuring to be honest.")
            }
            4 -> {
                chatPlayer(laugh, "An oversized hat. In a ridiculous colour.")
                chatNpc(
                    angry,
                    "No one is selling hats here, that's not a very helpful suggestion at all.",
                )
            }
            else -> {
                chatPlayer(neutral, "Nothing. You should buy absolutely nothing.")
                chatNpc(happy, "Now that's the best idea I've heard all day.")
                chatPlayer(
                    happy,
                    "I thought so! Now why don't you give me some of the money I saved you?",
                )
                chatNpc(
                    neutral,
                    "No, I don't think so. I think I'd rather buy something with my money after " +
                        "all.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wemund
    private suspend fun Dialogue.wemund() {
        chatNpc(
            happy,
            "What can I interest you in? Need any pipes? I make very sturdy pipes.",
        )
        chatPlayer(quiz, "No, not really... what kind of shop is this anyway?")
        chatNpc(happy, "Why, it's Wemund's Wrench Warehouse. Wemund, that's me.")
        chatNpc(
            neutral,
            "I don't really sell just wrenches, you know, I sell anything related to the " +
                "maintenance of dwarven steam machines.",
        )
        chatNpc(neutral, "Sometimes I do repair work as well. Or I send my assistant.")
        chatPlayer(quiz, "Who's he then?")
        chatNpc(
            neutral,
            "Oh, he's right over there... used to work in the mines to the south, poor lad. " +
                "Made a bit of a mess of things I believe.",
        )
        chatNpc(quiz, "So what'll it be then? Got any ship engines to repair?")
        chatPlayer(neutral, "Thanks, but I don't think your services are required.")
    }

    private companion object {
        const val GIANT_DWARF = "quest_giantdwarf"
        const val HAERA = "npc.dwarf_city_old_dwarf2"
        const val HEGIR = "npc.dwarf_city_old_dwarf1"

        val RIKI =
            listOf(
                "npc.dwarf_city_shop_sculpture_model",
                "npc.dwarf_city_shop_sculpture_model_boots",
                "npc.dwarf_city_shop_sculpture_model_torso",
                "npc.dwarf_city_shop_sculpture_model_axe",
                "npc.dwarf_city_shop_sculpture_model_boots_and_torso",
                "npc.dwarf_city_shop_sculpture_model_boots_and_axe",
                "npc.dwarf_city_shop_sculpture_model_torso_and_axe",
                "npc.dwarf_city_shop_sculpture_model_finished",
                "npc.dwarf_city_shop_sculpture_model_multi",
            )
    }
}
