package org.rsmod.content.areas.city.desert.kharid.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KharidDesertGapNpcs
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_gnome_explorer_safari") { startDialogue(it.npc) { wingstone() } }
        onOpNpc1(SIMON) { startDialogue(it.npc) { simon() } }
        onOpNpc1("npc.clanwars_doctor") { startDialogue(it.npc) { drFord() } }
        onOpNpc1("npc.feud_arabian_guard_coward") { startDialogue(it.npc) { cowardlyBandit() } }
        onOpNpc1("npc.dt_bandit_shopkeeper") { startDialogue(it.npc) { banditShopkeeper(it.npc) } }
        onOpNpc3("npc.dt_bandit_shopkeeper") { player.openBanditBargains(it.npc) }
        onOpNpc1("npc.uncerter_bandit") { startDialogue(it.npc) { tiles() } }
        onOpNpc1("npc.kalphite_oldman") { startDialogue(it.npc) { weirdOldMan() } }
        onOpNpc1("npc.oasis_knight_morgiana") { startDialogue(it.npc) { morgiana() } }
        onOpNpc1("npc.oasis_mage_cassim") { startDialogue(it.npc) { cassim() } }
        onOpNpc1("npc.oasis_tea_ali") { startDialogue(it.npc) { aliTheTeaSeller() } }
        onOpNpc1("npc.tourtrap_qip_cart_camel") { startDialogue(it.npc) { cartCamel() } }
        onOpNpc1("npc.ugthanki") { startDialogue(it.npc) { ugthanki() } }
        onOpNpc1("npc.capt_siad") { startDialogue(it.npc) { captainSiad() } }
        onOpNpc1("npc.capt_siad_vis") { startDialogue(it.npc) { captainSiad() } }
        onOpNpc1("npc.tourtrap_qip_mining_slave_female_1") { it.npc.say(SLAVE_LINE) }
        onOpNpc1("npc.tourtrap_qip_mining_slave_female_2") { it.npc.say(SLAVE_LINE) }
    }

    private fun Player.openBanditBargains(npc: Npc) {
        shops.open(this, npc, "Bandit Bargains", "inv.dt_bandit_shop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wingstone
    // TODO(content): [system:gnome_restaurant] delivery dialogue and tips
    // TODO(content): [system:treasure_trails] master clue puzzle box
    private suspend fun Dialogue.wingstone() {
        val mintCake = player.inv.count(MINT_CAKE) > 0
        val seedPod = player.inv.count(SEED_POD) > 0
        val option =
            when {
                mintCake && seedPod ->
                    choice3(
                        "It's a bit hot and dry round here for a gnome isn't it?",
                        1,
                        "What does this mint cake do?",
                        2,
                        "What does this seed pod do?",
                        3,
                    )
                mintCake ->
                    choice2(
                        "It's a bit hot and dry round here for a gnome isn't it?",
                        1,
                        "What does this mint cake do?",
                        2,
                    )
                seedPod ->
                    choice2(
                        "It's a bit hot and dry round here for a gnome isn't it?",
                        1,
                        "What does this seed pod do?",
                        3,
                    )
                else -> 1
            }
        when (option) {
            1 -> {
                chatPlayer(
                    quiz,
                    "It's a bit hot and dry round here for a gnome isn't it? I thought you " +
                        "gnomes liked your home comforts.",
                )
                chatNpc(
                    neutral,
                    "Certainly I miss home cooking, but gnomes are also fundamentally curious " +
                        "people. I'm here exploring the furthest reaches of the world for the " +
                        "King.",
                )
                chatPlayer(quiz, "Found anything interesting?")
                chatNpc(
                    neutral,
                    "Sophanem is a curious place, if you can get into the blighted place. There " +
                        "are several pyramids round here as well. Fascinating architecture, I've " +
                        "heard stories that some of them contain treasure tombs and mummies.",
                )
                chatNpc(
                    neutral,
                    "Cats seem to have considerable importance round here, they certainly seem " +
                        "tied into their religion. The Gods round here seem very alien to me - " +
                        "I've never heard of them before.",
                )
                chatPlayer(happy, "Thanks.")
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

    // https://oldschool.runescape.wiki/w/Transcript:Simon_Templeton
    // TODO(content): [system:met_flags] subsequent greeting and the greeting after a first sale
    //  (the player is treated as new unless they carry a pyramid top)
    // TODO(content): [system:pyramid_plunder] buying Pyramid Plunder artefacts and the sceptre
    private suspend fun Dialogue.simon() {
        val tops = player.inv.count(PYRAMID_TOP)
        if (tops == 0) {
            simonIntroduction()
            return
        }
        chatPlayer(happy, "Hello, Simon.")
        chatNpc(happy, "G'day, ${player.displayName}.")
        chatPlayer(quiz, "How's it going?")
        chatNpc(happy, "Not bad, mate, not bad at all. Got any artefacts for me?")
        val sell =
            if (tops == 1) {
                choice2("Sell it.", true, "Keep it.", false)
            } else {
                choice2("Sell them.", true, "Keep them.", false)
            }
        if (!sell) {
            if (tops == 1) {
                chatPlayer(neutral, "I have one, but I want to keep it for now.")
            } else {
                chatPlayer(neutral, "I have some, but I want to keep them for now.")
            }
            chatNpc(neutral, "Careful, mate, people might come looking for a thing like that!")
            chatPlayer(neutral, "Thanks for the advice, but I'll hang onto it for now.")
            chatNpc(happy, "Bye, cobber.")
            return
        }
        val sold = sellPyramidTops()
        if (sold == 0) {
            return
        }
        objbox(
            PYRAMID_TOP,
            "You hand over the artefact(s) and Simon hands you ${sold * TOP_PRICE} coins.",
        )
        chatNpc(
            happy,
            "Ripper! Thanks a bundle, mate! Thanks to you I can fulfill me contract. You're a " +
                "true blue! The boss will be pleased.",
        )
        chatPlayer(
            quiz,
            "Glad I was able to help... but who is your boss? I thought you worked for the " +
                "museum?",
        )
        chatNpc(
            shifty,
            "Mind your own bizzo, mate. But if you get any more, you know where I am!",
        )
    }

    private fun Dialogue.sellPyramidTops(): Int {
        val tops = player.inv.count(PYRAMID_TOP)
        if (tops == 0) {
            return 0
        }
        val taken =
            player.invDel(player.inv, PYRAMID_TOP, count = tops, ignoreVirtualStorage = true)
        if (!taken.success) {
            return 0
        }
        player.invAddOrDrop(objRepo, COINS, tops * TOP_PRICE)
        return tops
    }

    private suspend fun Dialogue.simonIntroduction() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "G'day, mate. I'm Simon - Simon Templeton. Who are you?")
        chatPlayer(
            quiz,
            "I'm ${player.displayName}. What are you doing all the way out here?",
        )
        chatNpc(
            neutral,
            "I've been contracted to retrieve artefacts from the top of this magnificent pyramid.",
        )
        chatPlayer(quiz, "So, why are you down here and not up there then?")
        chatNpc(
            sad,
            "Well, it's me back - an old injury I picked up in Sophanem has come back to haunt " +
                "me. I was working for the Museum of Varrock, and the gold I had been given " +
                "wasn't enough to pay for the carpet rides, let",
        )
        chatNpc(
            sad,
            "alone any decent equipment. But that was a long time ago... I can't even lift a " +
                "chisel right now. There's no way I can climb all the way up there.",
        )
        chatPlayer(quiz, "How on earth did you get down the cliff face, then?")
        chatNpc(
            sad,
            "That's what set off the injury. I was fine before I scrabbled down that pesky " +
                "slope. I think it will be a while before I can climb back up again; I must be " +
                "getting old.",
        )
        chatNpc(
            happy,
            "Hang on a second... You look like you're pretty agile and up for a challenge. How " +
                "about you retrieve it for me?",
        )
        chatNpc(
            happy,
            "I'll tell you what, I'll give you 10000 coins for every artefact you get for me.",
        )
        chatPlayer(neutral, "I'll see what I can do.")
        chatNpc(happy, "Good on ya, mate!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dr_Ford
    private suspend fun Dialogue.drFord() {
        chatNpc(angry, "What, what do you want?")
        chatPlayer(quiz, "Why are you out here on your own?")
        chatNpc(
            neutral,
            "I was drawn here... I use to be a traveling doctor, until I arrived in Al Kharid.",
        )
        chatNpc(
            neutral,
            "Then I heard it call to me... I immediately gave up my profession and set up here.",
        )
        chatNpc(
            happy,
            "I'm happy here... the voices tell me I'm happy here. I can continue my work.",
        )
        chatPlayer(confused, "Uhh... what voices? There is no one else here.")
        chatNpc(
            angry,
            "Of course there isn't, the hole you idiot, the hole speaks to me and I listen... It " +
                "says I will do great things, only if I listen...",
        )
        chatPlayer(worried, "Well... alright then, I think I'm just going to go and leave then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cowardly_Bandit
    private suspend fun Dialogue.cowardlyBandit() {
        if (QuestRequirements.hasCompleted(player, THE_FEUD)) {
            chatNpc(
                angry,
                "You dog! You killed our champion and for that I hope you die a slow and painful " +
                    "death.",
            )
            chatPlayer(angry, "What was that? Are you looking for a fight?")
            chatNpc(
                worried,
                "Me? No, no, I was just, er, complimenting your, that is - don't kill me, please!",
            )
            chatPlayer(neutral, "I wouldn't waste my time.")
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "What do you want?")
        chatPlayer(
            neutral,
            "Nothing really, I'm new to town and wanted to get to know the locals.",
        )
        chatNpc(
            angry,
            "I'm too busy for chitchat. Come back if you have some business to discuss with us.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bandit_shopkeeper
    private suspend fun Dialogue.banditShopkeeper(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Stuff for sale. You buying?")
        if (choice2("Yes", true, "No", false)) {
            player.openBanditBargains(npc)
            return
        }
        chatNpc(neutral, "No? 'Bye then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tiles
    // TODO(content): [system:note_exchange] exchanging banknotes used on Tiles for 5 coins each
    private suspend fun Dialogue.tiles() {
        chatNpc(
            neutral,
            "Hello. Do you wish me to exchange banknotes for you? I charge only 5 coins for each " +
                "banknote.",
        )
        while (true) {
            when (choice3("Yes please.", 1, "Who are you?", 2, "No thanks.", 3)) {
                1 -> {
                    chatPlayer(happy, "Yes please.")
                    chatNpc(neutral, "Hand me the banknotes you wish me to exchange.")
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "I am Tiles. I can exchange banknotes for real items. Do you want any " +
                            "banknotes converted?",
                    )
                }
                else -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Weird_Old_Man
    // TODO(content): [system:treasure_trails] hard challenge scroll
    private suspend fun Dialogue.weirdOldMan() {
        when (access.random.of(4)) {
            0 -> {
                chatNpc(madlaugh, "She likes to be tickled hehehehe.")
                chatPlayer(confused, "Okay...")
            }
            1 -> chatNpc(neutral, "No thanks, I don't want any.")
            2 -> {
                chatNpc(shifty, "Pst, wanna hear a secret?")
                if (choice2("Sure!", true, "No thanks.", false)) {
                    chatPlayer(happy, "Sure!")
                    chatNpc(shocked, "They've got six legs... SIX!")
                } else {
                    chatPlayer(neutral, "No thanks.")
                }
            }
            else -> {
                while (true) {
                    chatNpc(shocked, "AAAAAAAAARRRRRRGGGGGHHHHHHHH!")
                    val ask =
                        choice2("What's wrong?", true, "I'll leave you to it then...", false)
                    if (!ask) {
                        chatPlayer(confused, "I'll leave you to it then...")
                        return
                    }
                    chatPlayer(quiz, "What's wrong?")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Morgiana
    // TODO(content): [system:met_flags] "Ah, hello again." greeting and the tea option after
    //  talking to Ali the Tea Seller
    private suspend fun Dialogue.morgiana() {
        chatNpc(neutral, "Afternoon. I'm Morgiana, and by all means, let yourself in...")
        chatPlayer(shifty, "Oh, hey! The door was open...")
        chatNpc(neutral, "... Yes?")
        val armour =
            choice2("That armour looks mighty fancy!", true, "I'd best be off.", false)
        if (!armour) {
            chatPlayer(neutral, "I'd best be off.")
            chatNpc(neutral, "Quite. Perhaps I'll see you around...")
            return
        }
        chatPlayer(happy, "That armour looks mighty fancy!")
        chatNpc(happy, "Indeed! A family heirloom of mine, passed down from my father.")
        chatPlayer(quiz, "Oh? Are you from a family of famous knights?")
        chatNpc(sad, "You could say that...")
        chatPlayer(quiz, "What's wrong?")
        chatNpc(
            sad,
            "Oh, nothing... it's just... old memories. I'm getting old, as you can probably see. " +
                "My family have all died to war. This is meant to be my retirement.",
        )
        chatPlayer(
            quiz,
            "Retirement? Then why still wear the armour? Especially with how hot it is here.",
        )
        chatNpc(
            neutral,
            "I find it's best to be prepared in this region. Bandits regularly visit this fine " +
                "oasis, and they ask fewer questions when they see me in armour.",
        )
        chatPlayer(neutral, "I see.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cassim
    // TODO(content): [system:met_flags] "Welcome back." greeting and the tea option after
    //  talking to Ali the Tea Seller
    private suspend fun Dialogue.cassim() {
        chatPlayer(happy, "Hello!")
        chatNpc(happy, "Greetings! I'm Cassim. Welcome to my home.")
        val wine = choice2("That's a big barrel of wine!", true, "I'd best be off.", false)
        if (!wine) {
            chatPlayer(neutral, "I'd best be off.")
            chatNpc(neutral, "May the flames of Zamorak light your path.")
            chatPlayer(confused, "Er... yeah...")
            return
        }
        chatPlayer(happy, "That's a big barrel of wine!")
        chatNpc(happy, "Isn't it just! I'm a bit of a connoisseur.")
        chatPlayer(quiz, "Do you make it yourself?")
        chatNpc(
            neutral,
            "I've dabbled, but this is all imported. I have a connection in the far west who has " +
                "the best vineyards.",
        )
        chatPlayer(quiz, "In the west?")
        chatNpc(
            happy,
            "Yes! A fine Hosidius grape, strong hints of plum and blackberry, very sweet.",
        )
        chatPlayer(happy, "Sounds lovely!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ali_the_Tea_Seller
    // TODO(content): [system:met_flags] "Well you never actually made me that tea..." greeting
    private suspend fun Dialogue.aliTheTeaSeller() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Welcome! I'm Ali, and this is my tea shop! Is there anything I can do for you today?",
        )
        chatPlayer(quiz, "Hmm... Could I get some tea?")
        chatNpc(
            happy,
            "Of course! I'll get the water boiling at once. Is there anything else I can do for " +
                "you while you wait?",
        )
        while (true) {
            val option =
                choice3(
                    "How's the tea coming along?",
                    1,
                    "How's business here? Seems quite out the way.",
                    2,
                    "I'd best be heading off.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "How's the tea coming along?")
                    chatNpc(happy, "Oh! Yes, the water is boiling. Just give me a minute!")
                    chatPlayer(sad, "I'm so thirsty.")
                    chatNpc(neutral, "I know, the desert is forever hot. Thirsty work here!")
                }
                2 -> {
                    chatPlayer(quiz, "How's business here? Seems quite out the way.")
                    chatNpc(worried, "Hmm...")
                    chatPlayer(quiz, "Not good?")
                    chatNpc(
                        worried,
                        "It's not that. It's just that I've been having issues with a few " +
                            "thieves lately. Four to be precise!",
                    )
                    chatPlayer(shocked, "They've been stealing your tea?")
                    chatNpc(
                        sad,
                        "There have been a few break-ins. A recent shipment from Karamja has " +
                            "been taken! That was some of the finest stuff I've ever managed to " +
                            "get my hands on.",
                    )
                    chatPlayer(
                        angry,
                        "That's awful. If I see any shifty characters, I'll be sure to teach " +
                            "them a lesson.",
                    )
                    chatNpc(
                        laugh,
                        "Ha! Very courageous, adventurer! These thieves are big and brutish. I " +
                            "wouldn't start something you may not be able to finish.",
                    )
                    chatPlayer(happy, "I'm pretty tough. I think I can handle them.")
                    chatNpc(neutral, "If you say so.")
                }
                else -> {
                    chatPlayer(neutral, "I'd best be heading off.")
                    chatNpc(shocked, "You're leaving? But the tea is almost done!")
                    chatPlayer(neutral, "I've been waiting quite a while. Perhaps another time.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cart_Camel
    private suspend fun Dialogue.cartCamel() {
        val index = access.random.of(CART_CAMEL_INSULTS.size)
        chatPlayer(shifty, CART_CAMEL_INSULTS[index])
        if (!playerContainsObj(CAMULET)) {
            return
        }
        chatNpcSpecific(
            CAMEL_TITLE,
            "npc.tourtrap_qip_cart_camel",
            angry,
            CART_CAMEL_REPLIES[index],
        )
        chatPlayer(shocked, "Oooh, sorry, didn't realise you could hear me?")
        chatNpcSpecific(
            CAMEL_TITLE,
            "npc.tourtrap_qip_cart_camel",
            angry,
            "Well I can, so watch what you say in future.",
        )
        chatPlayer(happy, "Righto!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ugthanki
    private suspend fun Dialogue.ugthanki() {
        chatPlayer(
            shifty,
            access.random.pick(
                "Mmm... looks like that camel would make a nice kebab.",
                "If I go near that camel, it'll probably bite my hand off.",
                "I wonder if that camel has fleas...",
            ),
        )
        if (player.worn.count(CAMULET) > 0) {
            chatNpc(
                angry,
                access.random.pick(
                    "Come over here, I'll bite your leg off!",
                    "Prepare to die, you two-legged combat wrangler!",
                    "Your mother was a jackal, and your father smelt of cactuses!",
                ),
            )
            return
        }
        access.mes(
            access.random.pick(
                "The camel tries to stamp on your foot, but you pull it back quickly.",
                "The camel turns its head and glares at you.",
                "The camel spits at you, and you jump back hurriedly.",
            )
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Siad (after The Tourist Trap)
    // TODO(content): [quest:pandemonium] recruiting Captain Siad as a Sailing crew member
    private suspend fun Dialogue.captainSiad() {
        if (choice2("How is the camp holding up?", true, "Goodbye.", false)) {
            chatPlayer(quiz, "How is the camp holding up?")
            chatNpc(
                sad,
                "The whole place is falling apart, to be honest. I'd jump at the chance to shut " +
                    "it all down for good.",
            )
            return
        }
        chatPlayer(neutral, "Goodbye.")
        chatNpc(neutral, "Until next time.")
    }

    private companion object {
        const val SIMON = "npc.agility_pyramid_simon"
        const val THE_FEUD = "quest_feud"
        const val PYRAMID_TOP = "obj.agility_pyramid_gold_pyramid"
        const val COINS = "obj.coins"
        const val TOP_PRICE = 10000
        const val MINT_CAKE = "obj.aluft_gnome_mint_cake"
        const val SEED_POD = "obj.aluft_seed_pod"
        const val CAMULET = "obj.camulet"
        const val CAMEL_TITLE = "Camel"
        const val SLAVE_LINE = "Hey leave me alone, can't you see that I'm busy?"

        val CART_CAMEL_INSULTS =
            listOf(
                "Mmm... looks like that camel would make a nice kebab.",
                "That camel looks vicious, it'll probably eat me if I go too near!",
                "I wonder if I can catch something from that camel?",
            )

        val CART_CAMEL_REPLIES =
            listOf(
                "You and whose army mate? You and whose army?",
                "Well, I have to say, I am pretty peckish, but thankfully, I don't eat humans... " +
                    "often.",
                "You'll catch a stiff kick if you talk about me like that again!",
            )
    }
}
