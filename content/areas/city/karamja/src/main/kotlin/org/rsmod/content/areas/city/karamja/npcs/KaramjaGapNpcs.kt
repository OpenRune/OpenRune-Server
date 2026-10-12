package org.rsmod.content.areas.city.karamja.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KaramjaGapNpcs @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_gnome_explorer_jungle") { startDialogue(it.npc) { penwie() } }
        onOpNpc1("npc.captain_barnaby_karamja") { startDialogue(it.npc) { barnaby() } }
        onOpNpc1("npc.captain_shanks") { startDialogue(it.npc) { shanks() } }
        onOpNpc1("npc.jungleforester_f") { startDialogue(it.npc) { jungleForester() } }
        onOpNpc1("npc.jungleforester_m") { startDialogue(it.npc) { jungleForester() } }
        onOpNpc1("npc.mm_caranock") { startDialogue(it.npc) { caranock() } }
        onOpNpc1("npc.shilocartdriver") { startDialogue(it.npc) { vigroy() } }
        onOpNpc1("npc.trufitus") { startDialogue(it.npc) { trufitus() } }
        onOpNpc1("npc.uncerter_jiminua") { startDialogue(it.npc) { aisles() } }
        onOpNpc1(JIMINUA) { startDialogue(it.npc) { jiminua(it.npc) } }
        onOpNpc3(JIMINUA) { player.openJungleStore(it.npc) }
        onOpNpc1("npc.gub_musa_child_1") { startDialogue(it.npc) { child() } }
        onOpNpc1("npc.gub_musa_child_2") { startDialogue(it.npc) { child() } }
        onOpNpc1("npc.gub_musa_child_3") { startDialogue(it.npc) { child() } }
    }

    private fun Player.openJungleStore(npc: Npc) {
        shops.open(this, npc, "Jiminua's Jungle Store.", "inv.junglestore")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Penwie
    // TODO(content): [system:gnome_restaurant] delivery dialogue and tips
    private suspend fun Dialogue.penwie() {
        val mintCake = player.inv.count(MINT_CAKE) > 0
        val seedPod = player.inv.count(SEED_POD) > 0
        val option =
            when {
                mintCake && seedPod ->
                    choice3(
                        "Are you hunting animals then?",
                        1,
                        "What does this mint cake do?",
                        2,
                        "What does this seed pod do?",
                        3,
                    )
                mintCake ->
                    choice2("Are you hunting animals then?", 1, "What does this mint cake do?", 2)
                seedPod ->
                    choice2("Are you hunting animals then?", 1, "What does this seed pod do?", 3)
                else -> 1
            }
        when (option) {
            1 -> {
                chatPlayer(quiz, "Are you hunting animals, then?")
                chatNpc(neutral, "I'm exploring the region for the good of the gnome people.")
                chatPlayer(
                    quiz,
                    "You're not tracking down huge beasts to kill - you look equipped for it.",
                )
                chatNpc(
                    neutral,
                    "Well, if I find anything interesting I'll try and bring it back to the " +
                        "King. That might include animals, I suppose. I'd rather not kill it, " +
                        "though - most animals are more useful alive.",
                )
                chatPlayer(quiz, "How would you get it home, then?")
                chatNpc(happy, "Never underestimate the ingenuity of a gnome, friend.")
                chatPlayer(quiz, "Have you found anything else interesting around here?")
                chatNpc(
                    neutral,
                    "I've heard strange rumours about something the locals call the Broodoo. I " +
                        "haven't quite worked out what they are but they seem to be some sort of " +
                        "undead whose ghastly fate depends on the way they were killed.",
                )
                chatNpc(
                    neutral,
                    "I think they may be sacrifice victims - but I'm not sure why they are " +
                        "sacrificed. Either way the locals seem to be very scared of them.",
                )
                chatPlayer(happy, "That's interesting. Thanks.")
            }
            2 -> {
                chatPlayer(quiz, "What does this mint cake do?")
                chatNpc(
                    neutral,
                    "Gnomish mint cake is a great source of energy for the gnome traveller. It " +
                        "will give you that extra bit of zip when you are running out of steam.",
                )
                chatPlayer(happy, "Ooh, that might come in very handy. Do you have any spare?")
                chatNpc(neutral, "I need all the mint cake I have, I'm afraid.")
            }
            3 -> {
                chatPlayer(quiz, "What does this seed pod do?")
                chatNpc(
                    neutral,
                    "Ah, the seed pod of the Grand Tree is a fascinating spirit artefact. It is " +
                        "a very useful thing, though one must treat it with respect, since the " +
                        "Grand Tree has a connection with all of its parts, however distant they",
                )
                chatNpc(neutral, "may be. How do you think the Spirit Trees work?")
                chatNpc(
                    neutral,
                    "If you launch its seeds in a far flung location, you will be helping the " +
                        "tree survive, so the grand tree will help a gnome glider pilot find " +
                        "you, using the seed burst itself (which marks the sky) and a little " +
                        "magic, and bring you",
                )
                chatNpc(
                    neutral,
                    "back home as an act of gratitude. Budding farmers will also find their link " +
                        "with plants to be just a little bit better than before.",
                )
                chatNpc(
                    neutral,
                    "However, it takes time for a pilot to find you, and if you are in a real " +
                        "hurry, and I mean a real emergency, you can just squash the seed pod. " +
                        "The Grand Tree doesn't like this, but will pull you back",
                )
                chatNpc(
                    neutral,
                    "home straight away. Don't do this if you need to do anything with plants " +
                        "straight away, though - your fingers will be considerably less green " +
                        "for a while after using the seed pod this way, and the journey is not",
                )
                chatNpc(neutral, "very pleasant...")
                chatPlayer(happy, "Wow, that's very interesting. Thanks.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Barnaby (Brimhaven dock)
    // TODO(content): [system:ship_travel] taking 30 coins and sailing to Rimmington or Ardougne
    // TODO(content): [system:ship_travel] Ardougne and Rimmington quick-travel ops
    // TODO(content): [diary:karamja] free travel with Karamja gloves
    // TODO(content): [quest:ghosts_ahoy] free-travel "[Charm]" option with the ring of charos (a)
    private suspend fun Dialogue.barnaby() {
        chatNpc(
            happy,
            "Hello there. Would you like to travel somewhere? I can take you to Rimmington or " +
                "Ardougne for 30 coins.",
        )
        val destination =
            choice3(
                "I'd like to go to Rimmington.",
                "Rimmington",
                "I'd like to go to Ardougne.",
                "Ardougne",
                "I'm good thanks.",
                null,
            )
        if (destination == null) {
            chatPlayer(neutral, "I'm good thanks.")
            return
        }
        chatPlayer(happy, "I'd like to go to $destination.")
        if (access.invTotal(player.inv, COINS) < BARNABY_FARE) {
            chatNpc(neutral, "Come back when you've got 30 coins for me.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Shanks (after Shilo Village)
    // TODO(content): [system:ship_travel] taking the ticket and sailing to Khazard Port or Port
    //  Sarim
    // TODO(content): [verify:in_game] selling a ticket to players without one (the wiki gives a
    //  varying price of about 26-45 coins)
    private suspend fun Dialogue.shanks() {
        chatNpc(
            happy,
            "Hello there shipmate! I sail to Khazard Port and to Port Sarim. Where are you bound?",
        )
        if (player.inv.count(SHIP_TICKET) == 0) {
            return
        }
        chatNpc(happy, "Where would you like to go?")
        val option =
            choice3(
                "Khazard Port please.",
                1,
                "Port Sarim please.",
                2,
                "Nowhere just at the moment thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Khazard Port please.")
                chatNpc(
                    happy,
                    "Very well then me old shipmate, I'll just take your ticket and then we'll " +
                        "set sail.",
                )
            }
            2 -> {
                chatPlayer(happy, "Port Sarim please.")
                chatNpc(
                    happy,
                    "Very well then me old shipmate, I'll just take your ticket and then we'll " +
                        "set sail.",
                )
            }
            3 -> {
                chatPlayer(neutral, "Nowhere just at the moment thanks.")
                chatNpc(
                    neutral,
                    "Very well then me old shipmate, Just let me know if you change your mind.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jungle_forester
    // TODO(content): [system:item_on_npc] trading a cooked oomlie wrap for a bullroarer
    private suspend fun Dialogue.jungleForester() {
        chatNpc(happy, "Hello friend. You're a long way from Lumbridge!")
        var state =
            choice4(
                "How do I get into the Kharazi Jungle?",
                HOW,
                "What do you do here?",
                WHAT,
                "Have you seen any Kharazi tribespeople in the jungle?",
                SEEN,
                "I have a question about my Achievement Diary.",
                DIARY,
            )
        while (true) {
            state =
                when (state) {
                    HOW -> {
                        chatPlayer(quiz, "How do I get into the Kharazi Jungle?")
                        chatNpc(
                            worried,
                            "Well, I don't recommend just walking in! The last guy I saw try " +
                                "that barely escaped with his life.",
                        )
                        chatNpc(
                            laugh,
                            "Apparently he was on a mission to map the area. How foolish is that?",
                        )
                        choice5(
                            "Actually, I plan to map the jungle myself.",
                            MAP,
                            "Are you calling me foolish?",
                            FOOLISH,
                            "What do you do here?",
                            WHAT,
                            "Have you seen any Kharazi tribespeople in the jungle?",
                            SEEN,
                            "Okay, thanks.",
                            THANKS,
                        )
                    }
                    MAP -> {
                        chatPlayer(neutral, "Actually, I plan to map the jungle myself.")
                        chatNpc(
                            shocked,
                            "Oh. Oops. Sorry if I sounded rude, it just seems like a really bad " +
                                "idea.",
                        )
                        chatNpc(
                            neutral,
                            "I mean it's a noble calling. Lots of people around here could use a " +
                                "proper map to navigate with. But, you know... there's something " +
                                "not quite right about that jungle.",
                        )
                        chatNpc(neutral, "Best of luck though... Guthix knows, you'll need it.")
                        choice3(
                            "Do you have any other tips about the Kharazi Jungle?",
                            TIPS,
                            "Have you seen any Kharazi Tribesmen in the jungle?",
                            SEEN,
                            "Ok thanks.",
                            THANKS,
                        )
                    }
                    TIPS -> {
                        chatPlayer(quiz, "Do you have any other tips about the Kharazi Jungle?")
                        chatNpc(
                            neutral,
                            "Not really. Just be careful - there's a lot of weird stuff going on " +
                                "in there lately.",
                        )
                        return
                    }
                    FOOLISH -> {
                        chatPlayer(angry, "Are you calling me foolish?")
                        chatNpc(worried, "Er... maybe? I'll just be on my way...")
                        return
                    }
                    WHAT -> {
                        chatPlayer(quiz, "What do you do here?")
                        chatNpc(
                            neutral,
                            "I'm a forester, and I specialise in exotic woods. I haven't gone " +
                                "into the Kharazi myself, but I've found some interesting tree " +
                                "specimens out here.",
                        )
                        chatNpc(
                            happy,
                            "If you do find a safe path into the jungle, do come and let me " +
                                "know. I'd love to have a nose around in there.",
                        )
                        choice3(
                            "How do I get into the Kharazi Jungle?",
                            HOW,
                            "Have you seen any Kharazi tribespeople in the jungle?",
                            SEEN,
                            "Okay, thanks.",
                            THANKS,
                        )
                    }
                    SEEN -> {
                        chatPlayer(quiz, "Have you seen any Kharazi tribespeople in the jungle?")
                        chatNpc(
                            happy,
                            "Oh yes, I've met loads of them! Lovely lot. I haven't seen many " +
                                "about recently, though.",
                        )
                        chatNpc(
                            neutral,
                            "If you're looking to say hello, you'll need a Bullroarer. If you " +
                                "swing it above your head it makes a loud noise - that should " +
                                "attract someone's attention.",
                        )
                        choice3(
                            "Do you have a Bullroarer I could borrow?",
                            BORROW,
                            "How do I get into the Kharazi jungle?",
                            HOW,
                            "Ok thanks.",
                            THANKS,
                        )
                    }
                    BORROW -> {
                        chatPlayer(quiz, "Do you have a Bullroarer I could borrow?")
                        chatNpc(
                            sad,
                            "I wish I did! But I just have the one, and I've grown fond of it. " +
                                "Besides, I might need it if I ever get lost in the jungle.",
                        )
                        choice2("Will you trade something for it?", TRADE, "Okay, thanks.", THANKS)
                    }
                    TRADE -> {
                        chatPlayer(quiz, "Will you trade something for it?")
                        chatNpc(
                            neutral,
                            "Hmm, maybe. Tell you what, you show me something interesting and " +
                                "I'll see if it's worth my Bullroarer. How about that?",
                        )
                        chatPlayer(happy, "It's a deal!")
                        return
                    }
                    DIARY -> {
                        chatPlayer(quiz, "I have a question about my Achievement Diary.")
                        chatNpc(
                            neutral,
                            "Sorry, I don't do that kind of thing any more. You should go and " +
                                "talk to Pirate Jackie in Brimhaven instead.",
                        )
                        return
                    }
                    else -> {
                        chatPlayer(happy, "Okay, thanks.")
                        chatNpc(happy, "You're welcome! See you around...")
                        return
                    }
                }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:G.L.O._Caranock
    // TODO(content): [quest:monkey_madness_i] "Get out of my office!" after the quest
    // TODO(content): [system:gnome_restaurant] delivery dialogue and tips
    private suspend fun Dialogue.caranock() {
        mesbox("Caranock seems too busy to talk.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vigroy
    // TODO(content): [system:cart_travel] taking the fare and the cart ride to Brimhaven
    // TODO(content): [diary:karamja] free rides after the elite diary
    // TODO(content): [verify:in_game] fare rounding (wiki: about 5% of carried coins, 10 to 200)
    private suspend fun Dialogue.vigroy() {
        val fare = (access.invTotal(player.inv, COINS) / 20).coerceIn(10, 200)
        chatNpc(
            neutral,
            "I am offering a cart ride to Brimhaven, if you're interested. It will cost $fare " +
                "coins. Is that okay?",
        )
        val ride =
            choice2("Yes please, I'd like to go to Brimhaven.", true, "No thanks.", false)
        if (!ride) {
            chatPlayer(neutral, "No thanks.")
            chatNpc(neutral, "Okay, Bwana, let me know if you change your mind.")
            return
        }
        chatPlayer(happy, "Yes please, I'd like to go to Brimhaven.")
        if (access.invTotal(player.inv, COINS) < fare) {
            chatNpc(
                neutral,
                "Sorry, but it looks as if you don't have enough money. Come and see me when you " +
                    "have enough for the ride.",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Trufitus (before Jungle Potion)
    // TODO(content): [quest:jungle_potion] start offer, quest dialogue and after-quest lines
    // TODO(content): [quest:shilo_village] dialogue after Shilo Village
    // TODO(content): [system:item_on_npc] selling clean jungle herbs
    private suspend fun Dialogue.trufitus() {
        chatNpc(happy, "Greetings Bwana! I am Trufitus Shakaya of the Tai Bwo Wannai village.")
        chatNpc(happy, "Welcome to our humble village.")
        val option =
            choice3(
                "What does Bwana mean?",
                1,
                "Tai Bwo Wannai? What does that mean?",
                2,
                "It's a nice village, where is everyone?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What does Bwana mean?")
                chatNpc(
                    neutral,
                    "It means friend, and friends come in peace. I assume that you come in peace?",
                )
                val peace =
                    choice2(
                        "Yes, of course I do.",
                        true,
                        "What does a warrior like me know about peace?",
                        false,
                    )
                if (!peace) {
                    chatPlayer(angry, "What does a warrior like me know about peace?")
                    chatNpc(
                        neutral,
                        "When you grow weary of violence and seek a more enlightened path, " +
                            "please pay me a visit",
                    )
                    chatNpc(
                        neutral,
                        "as I may have a proposition for you. Now I need to attend to the plight " +
                            "of my people. Please excuse me...",
                    )
                    return
                }
                chatPlayer(happy, "Yes, of course I do.")
                chatNpc(happy, "Well, that is good news, as I may have a proposition for you.")
                val proposition =
                    choice2(
                        "A proposition eh? Sounds interesting!",
                        true,
                        "I am sorry, but I am very busy.",
                        false,
                    )
                if (!proposition) {
                    trufitusBusy("I am sorry, but I am very busy.")
                    return
                }
                chatPlayer(happy, "A proposition eh? Sounds interesting!")
                chatNpc(
                    neutral,
                    "I hoped you would think so. My people are afraid to stay in the village.",
                )
                chatNpc(
                    neutral,
                    "They have returned to the jungle and I need to commune with the gods",
                )
                chatNpc(
                    neutral,
                    "to see what fate befalls us. You can help me by collecting some herbs that " +
                        "I need.",
                )
                trufitusHelp(
                    "I am very sorry, but I don't have time for that.",
                    "I am very sorry, but I don't have any time for that.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Tai Bwo Wannai? What does that mean?")
                chatNpc(
                    neutral,
                    "It means 'small clearing in the jungle' but it is now the name of our " +
                        "village.",
                )
                val where =
                    choice2(
                        "It's a nice village, where is everyone?",
                        true,
                        "I am sorry, but I am very busy.",
                        false,
                    )
                if (where) {
                    trufitusVillage()
                } else {
                    trufitusBusy("I am sorry, but I am very busy.")
                }
            }
            3 -> trufitusVillage()
        }
    }

    private suspend fun Dialogue.trufitusVillage() {
        chatPlayer(quiz, "It's a nice village, where is everyone?")
        chatNpc(
            sad,
            "My people are afraid to stay in the village. They have returned to the jungle. I " +
                "need to commune with the gods to see what fate befalls us.",
        )
        chatNpc(neutral, "You may be able to help with this.")
        trufitusHelp("I am sorry, but I am very busy.", "I am sorry, but I am very busy.")
    }

    private suspend fun Dialogue.trufitusHelp(busyOption: String, busyLine: String) {
        if (!choice2("Me? How can I help?", true, busyOption, false)) {
            trufitusBusy(busyLine)
            return
        }
        chatPlayer(quiz, "Me? How can I help?")
        chatNpc(
            neutral,
            "I need to make a special brew! A potion that helps me to commune with the gods. For " +
                "this potion, I need very special herbs, that are only found in the deep jungle.",
        )
        chatNpc(
            neutral,
            "I can only guide you so far as the herbs are not easy to find. With some luck, you " +
                "will find each herb in turn and bring to me. I will give you details of where " +
                "to find the next herb.",
        )
        chatNpc(happy, "In return for this great favour I will give you training in Herblore.")
    }

    private suspend fun Dialogue.trufitusBusy(line: String) {
        chatPlayer(neutral, line)
        chatNpc(neutral, "Very well then, may your journeys bring you much joy.")
        chatNpc(neutral, "Maybe you will pass this way again and you then take up my proposal?")
        chatNpc(neutral, "But for now, fare thee well.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aisles
    // TODO(content): [system:note_exchange] exchanging banknotes used on Aisles for 5 coins each
    private suspend fun Dialogue.aisles() {
        chatNpc(
            neutral,
            "Hello. Do you wish me to exchange banknotes for you? I charge only 5 coins for each " +
                "banknote.",
        )
        var option = choice3("Yes please.", 1, "Who are you?", 2, "No thanks.", 3)
        if (option == 2) {
            chatPlayer(quiz, "Who are you?")
            chatNpc(
                neutral,
                "I am Aisles. I can exchange banknotes for real items. Do you want any banknotes " +
                    "converted?",
            )
            option = choice2("Yes please.", 1, "No thanks.", 3)
        }
        if (option == 1) {
            chatPlayer(happy, "Yes please.")
            chatNpc(neutral, "Hand me the banknotes you wish me to exchange.")
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jiminua
    private suspend fun Dialogue.jiminua(npc: Npc) {
        chatNpc(happy, "Welcome to the Jungle Store. Can I help you at all?")
        if (choice2("Yes please. What are you selling", true, "No thanks.", false)) {
            chatPlayer(quiz, "Yes please. What are you selling?")
            player.openJungleStore(npc)
            return
        }
        chatPlayer(neutral, "No thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jack_(Christmas_2013) (after the event)
    // https://oldschool.runescape.wiki/w/Transcript:Jill (after the event)
    // https://oldschool.runescape.wiki/w/Transcript:Jeff_(child) (after the event)
    private suspend fun Dialogue.child() {
        chatNpc(
            happy,
            "Thanks so much for saving us from those stinky gublinch. They were awful! I hope " +
                "Shanty gave you somefing cool for your efforts.",
        )
    }

    private companion object {
        const val JIMINUA = "npc.jiminua"
        const val COINS = "obj.coins"
        const val MINT_CAKE = "obj.aluft_gnome_mint_cake"
        const val SEED_POD = "obj.aluft_seed_pod"
        const val SHIP_TICKET = "obj.shiloshipticket"
        const val BARNABY_FARE = 30

        const val HOW = 1
        const val WHAT = 2
        const val SEEN = 3
        const val DIARY = 4
        const val THANKS = 5
        const val MAP = 6
        const val FOOLISH = 7
        const val TIPS = 8
        const val BORROW = 9
        const val TRADE = 10
    }
}
