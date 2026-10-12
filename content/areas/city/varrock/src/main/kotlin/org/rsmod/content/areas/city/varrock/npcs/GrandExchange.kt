package org.rsmod.content.areas.city.varrock.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GrandExchange : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ge_clerk_1") { startDialogue(it.npc) { clerk() } }
        onOpNpc1("npc.ge_clerk_2") { startDialogue(it.npc) { clerk() } }
        onOpNpc1("npc.ge_clerk_3") { startDialogue(it.npc) { clerk() } }
        onOpNpc1("npc.ge_clerk_4") { startDialogue(it.npc) { clerk() } }
        onOpNpc1("npc.ge_boss") { startDialogue(it.npc) { brugsenBursen() } }
        onOpNpc1("npc.ge_expert_herbs") { startDialogue(it.npc) { bobBarter() } }
        onOpNpc1("npc.ge_expert_runes") { startDialogue(it.npc) { murkyMatt() } }
        onOpNpc1("npc.ge_expert_ores") { startDialogue(it.npc) { faridMorrisane() } }
        onOpNpc1("npc.ge_expert_combat") { startDialogue(it.npc) { hofuthand() } }
        onOpNpc1("npc.ge_expert_logs") { startDialogue(it.npc) { reloboBlinyo() } }
        onOpNpc1("npc.tob_spectator_misthalin") { startDialogue(it.npc) { abigaila() } }
        onOpNpc1("npc.clan_clerk") { startDialogue(it.npc) { clanClerk() } }
        onOpNpc1("npc.bond_james_bond") { startDialogue(it.npc) { james() } }
        onOpNpc1("npc.bond_james_visible") { startDialogue(it.npc) { james() } }
        onOpNpc1("npc.boardgames_barmaid3") { startDialogue(it.npc) { meredith() } }
        onOpNpc1("npc.game_zone_recruiter_saradomin") {
            startDialogue(it.npc) { saradoministRecruiter() }
        }
        onOpNpc1("npc.game_zone_recruiter_zamorak") {
            startDialogue(it.npc) { zamorakianRecruiter() }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grand_Exchange_Clerk
    // TODO(content): [system:grand_exchange] Exchange, History and Sets ops
    // TODO(content): [system:adventure_paths] Combat path voucher reward (2500 coins)
    // TODO(content): [system:grand_exchange] Exchange collection reminders on login toggle
    private suspend fun Dialogue.clerk() {
        chatNpc(
            happy,
            "Welcome to the Grand Exchange. Would you like to trade now, or exchange item sets?",
        )
        val option =
            choice4(
                "How do I use the Grand Exchange?",
                1,
                "I'd like to set up trade offers please.",
                2,
                "Can you help me with item sets?",
                3,
                "I'm fine, thanks.",
                4,
            )
        when (option) {
            1 -> {
                clerkHowToUse()
                when (
                    choice3(
                        "I'd like to set up trade offers please.",
                        2,
                        "Can you help me with item sets?",
                        3,
                        "I'm fine, thanks.",
                        4,
                    )
                ) {
                    2 -> clerkTradeOffers()
                    3 -> clerkItemSets()
                    4 -> chatPlayer(happy, "I'm fine, thanks.")
                }
            }
            2 -> clerkTradeOffers()
            3 -> clerkItemSets()
            4 -> chatPlayer(happy, "I'm fine, thanks.")
        }
    }

    private suspend fun Dialogue.clerkHowToUse() {
        chatPlayer(quiz, "How do I use the Grand Exchange?")
        chatNpc(
            neutral,
            "My colleague and I can let you set up trade offers. You can offer to Sell items or " +
                "Buy items.",
        )
        chatNpc(
            neutral,
            "When you want to sell something, you give us the items and tell us how much money " +
                "you want for them.",
        )
        chatNpc(
            neutral,
            "We'll look for someone who wants to buy those items at your price, and we'll " +
                "perform the trade. You can then collect the cash here, or at any bank.",
        )
        chatNpc(
            neutral,
            "When you want to buy something, you tell us what you want, and give us the cash " +
                "you're willing to spend on it.",
        )
        chatNpc(
            neutral,
            "We'll look for someone who's selling those items at your price, and we'll perform " +
                "the trade. You can then collect the items here, or at any bank, along with any " +
                "left-over cash.",
        )
        chatNpc(
            neutral,
            "Sometimes it takes a while to find a matching trade offer. If you change your mind, " +
                "we'll let you cancel your trade offer, and we'll return your unused items and " +
                "cash.",
        )
        chatNpc(
            happy,
            "That's all the essential information you need to get started. Would you like to " +
                "trade now, or exchange item sets?",
        )
    }

    // TODO(content): [system:grand_exchange] open the Grand Exchange interface
    private suspend fun Dialogue.clerkTradeOffers() {
        chatPlayer(happy, "I'd like to set up trade offers please.")
    }

    // TODO(content): [system:grand_exchange] open the Item Sets interface
    private suspend fun Dialogue.clerkItemSets() {
        chatPlayer(quiz, "Can you help me with item sets?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brugsen_Bursen
    private suspend fun Dialogue.brugsenBursen() {
        chatNpc(happy, "Hello, and welcome to the Grand Exchange! How can I help you?")
        val option =
            choice3(
                "What is this place?",
                1,
                "Can you tell me the price of an item?",
                2,
                "I'm fine, thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "This is the Grand Exchange, the best trading spot in the land! People come " +
                        "from all across the country to buy and sell every item under the sun.",
                )
                chatNpc(
                    neutral,
                    "When you want to buy or sell an item, talk to the clerks in the middle. " +
                        "They'll tell you how to set up a trade offer.",
                )
                chatNpc(quiz, "Now, is there something I can do for you?")
                when (
                    choice2("Can you tell me the price of an item?", 2, "I'm fine, thanks.", 3)
                ) {
                    2 -> brugsenPriceCheck()
                    3 -> chatPlayer(happy, "I'm fine, thanks.")
                }
            }
            2 -> brugsenPriceCheck()
            3 -> chatPlayer(happy, "I'm fine, thanks.")
        }
    }

    // TODO(content): [system:grand_exchange] open the price checker, then "Thank you."
    private suspend fun Dialogue.brugsenPriceCheck() {
        chatPlayer(quiz, "Can you tell me the price of an item?")
        chatNpc(quiz, "What had you in mind?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bob_Barter_(herbs) (members world)
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.bobBarter() {
        chatNpc(
            happy,
            "Hello, chum, fancy buyin' some designer jewellery? They've come all the way from " +
                "Ardougne! Most pukka!",
        )
        chatPlayer(neutral, "Erm, no. I'm all set, thanks.")
        chatNpc(
            happy,
            "Okay, chum, so what can I do for you? I can tell you the very latest herb & potion " +
                "prices, or perhaps I could help you decant your potions.",
        )
        val option =
            choice4(
                "Who are you?",
                1,
                "Can you show me the prices for herbs and potions?",
                2,
                "Can you decant things for me?",
                3,
                "I'll leave you to it.",
                4,
            )
        if (option == 1) {
            chatPlayer(quiz, "Who are you?")
            chatNpc(happy, "Why, I'm Bob! Your friendly seller of smashin' goods!")
            chatPlayer(quiz, "So what do you have to sell?")
            chatNpc(
                neutral,
                "Oh, not much at the moment. Cuz, ya know, business being so well and cushie.",
            )
            chatPlayer(neutral, "You don't really look like you're being so successful.")
            chatNpc(
                laugh,
                "You plonka! It's all a show, innit! If I let people knows I'm in good business " +
                    "they'll want a share of the moolah!",
            )
            chatPlayer(neutral, "You conveniently have a response for everything.")
            chatNpc(happy, "That's the Ardougne way, my friend.")
            when (
                choice3(
                    "Can you show me the prices for herbs and potions?",
                    2,
                    "Can you decant things for me?",
                    3,
                    "I'll leave you to it.",
                    4,
                )
            ) {
                2 -> bobBarterPrices()
                3 -> bobBarterDecant()
            }
            return
        }
        when (option) {
            2 -> bobBarterPrices()
            3 -> bobBarterDecant()
        }
    }

    // TODO(content): [system:grand_exchange] open the Herbs and Potions price guide
    private suspend fun Dialogue.bobBarterPrices() {
        chatPlayer(quiz, "Can you show me the prices for herbs and potions?")
    }

    // TODO(content): [system:potion_decanting] open the decanting interface (also the Decant op)
    private suspend fun Dialogue.bobBarterDecant() {
        chatPlayer(quiz, "Can you decant things for me?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Murky_Matt_(runes) (members world)
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.murkyMatt() {
        chatNpc(
            happy,
            "Arrr, what is it that ye be wantin? I can tell ye all about the prices of runes, or " +
                "perhaps I could combine the charges on yer teleport jewellery.",
        )
        val option =
            choice5(
                "What's a pirate doing here?",
                1,
                "Tell me about the prices of runes.",
                2,
                "Combine the charges on my teleport jewellery, please.",
                3,
                "I hear you deal in rings of forging?",
                4,
                "I got to go, erm, swab some decks! Yarr!",
                5,
            )
        if (option == 1) {
            chatPlayer(quiz, "What's a pirate doing here?")
            chatNpc(angry, "By my sea-blistered skin, I could ask the same of you!")
            chatPlayer(confused, "But... I'm not a pirate?")
            chatNpc(
                quiz,
                "No? Then what's that smell? The smell o' someone spent too long at sea without " +
                    "a bath!",
            )
            chatPlayer(neutral, "I think that's probably you.")
            chatNpc(
                laugh,
                "Har har har! We've got a stern landlubber 'ere! Well, let me tell ye, I'm here " +
                    "for the Grand Exchange! Gonna cash in me loot!",
            )
            chatPlayer(
                quiz,
                "Don't you just want to sell it in a shop or trade it to someone specific?",
            )
            chatNpc(
                happy,
                "By my wave-battered bones! Not when I can sell to the whole world from this " +
                    "very spot!",
            )
            if (QuestRequirements.hasCompleted(player, LUNAR_DIPLOMACY)) {
                chatPlayer(
                    angry,
                    "You pirates are nothing but trouble! Why, once I travelled to Lunar Isle " +
                        "with a bunch of your type, and spent days sailing around in circles!",
                )
                chatNpc(happy, "Then ye must know me brother! Murky Pat!")
                chatPlayer(confused, "Hmmm. Not so sure I remember him.")
                val type = if (access.isBodyTypeA()) "guy" else "gal"
                chatNpc(
                    neutral,
                    "Well, 'e be on that ship for sure. And I remember 'im tellin' me about some " +
                        "$type like ye, getting all mixed up with curses and cabin boys.",
                )
                chatPlayer(happy, "Yes! That was me!")
                chatNpc(laugh, "Ye sure be a different character.")
            }
            when (
                choice3(
                    "Tell me about the prices of runes.",
                    2,
                    "Combine the charges on my teleport jewellery, please.",
                    3,
                    "I got to go, erm, swab some decks! Yarr!",
                    5,
                )
            ) {
                2 -> murkyMattPrices()
                3 -> murkyMattCombine()
                5 -> murkyMattSwabDecks()
            }
            return
        }
        when (option) {
            2 -> murkyMattPrices()
            3 -> murkyMattCombine()
            4 -> murkyMattForging()
            5 -> murkyMattSwabDecks()
        }
    }

    // TODO(content): [system:grand_exchange] open the Runes price guide
    private suspend fun Dialogue.murkyMattPrices() {
        chatPlayer(quiz, "Tell me about the prices of runes.")
    }

    // TODO(content): [system:jewellery_decanting] combine teleport jewellery charges (Decant op)
    private suspend fun Dialogue.murkyMattCombine() {
        chatPlayer(quiz, "Combine the charges on my teleport jewellery, please.")
    }

    // TODO(content): [system:ring_of_forging_enchanting] ruby ring checks, 250gp per ring, count
    private suspend fun Dialogue.murkyMattForging() {
        chatPlayer(quiz, "I hear you deal in rings of forging?")
    }

    private suspend fun Dialogue.murkyMattSwabDecks() {
        chatPlayer(happy, "I got to go, erm, swab some decks! Yarr!")
        chatNpc(
            angry,
            "Defer your speech right there! Quit this derogatory and somewhat narrow-minded " +
                "allusion that all folks of sea voyage are only concerned with washing the " +
                "decks, looking after parrots and drinking rum. I'll have ye",
        )
        chatNpc(angry, "know there is much more to a pirate than meets the eye.")
        chatPlayer(happy, "Aye-aye, captain!")
        chatNpc(angry, "...")
        chatPlayer(laugh, "Oh, come on! Lighten up!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farid_Morrisane_(ores_and_bars)
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.faridMorrisane() {
        chatPlayer(happy, "Hello, little boy.")
        chatNpc(
            angry,
            "I would prefer it if you didn't speak to me in such a manner. I'll have you know " +
                "I'm an accomplished merchant.",
        )
        val option =
            choice3(
                "Calm down, junior.",
                1,
                "Can you show me the prices of ores and bars?",
                2,
                "I best go and speak with someone more my height.",
                3,
            )
        if (option == 1) {
            chatPlayer(happy, "Calm down, junior.")
            chatNpc(angry, "Don't tell me to calm down! And don't call me 'junior'.")
            chatNpc(
                angry,
                "I'll have you know I am Farid Morrisane, son of Ali Morrisane, the world's " +
                    "greatest merchant!",
            )
            chatPlayer(quiz, "Then why are you here and not him?")
            chatNpc(
                neutral,
                "My dad has given me the responsibility of expanding our business here.",
            )
            chatPlayer(
                laugh,
                "And you're up to the task? What a grown up boy you are! Mummy and daddy must be " +
                    "very pleased!",
            )
            val address = if (access.isBodyTypeA()) "mate" else "darling"
            chatNpc(
                angry,
                "Look, $address - I may be young, I may be short, but I'm a respected merchant " +
                    "around here and don't have time to deal with simpletons like you.",
            )
            when (
                choice2(
                    "Can you show me the prices of ores and bars?",
                    2,
                    "I best go and speak with someone more my height.",
                    3,
                )
            ) {
                2 -> faridPrices()
                3 -> faridLeave()
            }
            return
        }
        when (option) {
            2 -> faridPrices()
            3 -> faridLeave()
        }
    }

    // TODO(content): [system:grand_exchange] open the Ores and Bars price guide
    private suspend fun Dialogue.faridPrices() {
        chatPlayer(quiz, "Can you show me the prices of ores and bars?")
    }

    private suspend fun Dialogue.faridLeave() {
        chatPlayer(neutral, "I best go and speak with someone more my height.")
        val title = if (access.isBodyTypeA()) "mister" else "miss"
        chatNpc(neutral, "Then I shall not stop you, $title. I've too much work to do.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hofuthand_(weapons_and_armour)
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.hofuthand() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            confused,
            "What? Oh, hello. I was deep in thought. Did you want me to show you the prices of " +
                "weapons and armour?",
        )
        val option =
            choice4(
                "You seem a bit flustered.",
                1,
                "Yes, show me the prices of weapons and armour.",
                2,
                "Can you tell me about the bank's auto-charging services?",
                3,
                "I'll leave you alone.",
                4,
            )
        if (option == 1) {
            chatPlayer(quiz, "You seem a bit flustered.")
            chatNpc(
                neutral,
                "Sorry, I'm just deep in thought. I'm waiting for many deals to complete today.",
            )
            chatPlayer(quiz, "What sort of things are you selling?")
            chatNpc(
                happy,
                "Good old weapons and armour! My people - dwarves, you understand - are hoping I " +
                    "can trade with humans.",
            )
            chatPlayer(happy, "It looks like you've come to the right place for that.")
            chatNpc(happy, "I have indeed, my friend. Now, can I help you?")
            when (
                choice2(
                    "Yes, show me the prices of weapons and armour.",
                    2,
                    "I'll leave you alone.",
                    4,
                )
            ) {
                2 -> hofuthandPrices()
                4 -> hofuthandLeave()
            }
            return
        }
        when (option) {
            2 -> hofuthandPrices()
            3 -> hofuthandAutoCharging()
            4 -> hofuthandLeave()
        }
    }

    // TODO(content): [system:grand_exchange] open the Weapons and Armour price guide
    private suspend fun Dialogue.hofuthandPrices() {
        chatPlayer(quiz, "Yes, show me the prices of weapons and armour.")
    }

    // TODO(content): [system:bank_auto_charging] "Set a global minimum charge threshold?" menu
    private suspend fun Dialogue.hofuthandAutoCharging() {
        chatPlayer(quiz, "Can you tell me about the bank's auto-charging services?")
        chatNpc(
            neutral,
            "Hm? Oh, yes. If you don't want to charge your items yourself, you can have the bank " +
                "attendant do it for you on withdrawal.",
        )
        chatPlayer(quiz, "How do they know how many charges to add?")
        chatNpc(
            neutral,
            "You can tell them how many you would like for each item. Or if you'd like, I can " +
                "let them know a minimum threshold for all items.",
        )
    }

    private suspend fun Dialogue.hofuthandLeave() {
        chatPlayer(neutral, "I'll leave you alone.")
        chatNpc(neutral, "Thank you, I have much on my mind.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Relobo_Blinyo_(logs)
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.reloboBlinyo() {
        chatNpc(happy, "Hello, my friend. Would you like me to show you the prices of logs?")
        val option =
            choice3(
                "You look like you've travelled a fair distance.",
                1,
                "Okay, show me the prices of logs.",
                2,
                "Sorry, I need to be making tracks.",
                3,
            )
        if (option == 1) {
            chatPlayer(quiz, "You look like you've travelled a fair distance.")
            chatNpc(quiz, "What gave me away?")
            chatPlayer(
                neutral,
                "I don't mean to be rude, but the clothing and hair, for starters.",
            )
            chatNpc(
                happy,
                "Ah, yes. I'm from Shilo Village on Karamja, it's a style I've had since I was " +
                    "little.",
            )
            chatPlayer(quiz, "Then tell me, why are you so far from home?")
            chatNpc(
                happy,
                "This Grand Exchange! Isn't it marvellous! I've never seen anything like it in " +
                    "my life.",
            )
            chatNpc(
                neutral,
                "My people were saddened when I chose to leave home and travel here, but I hope " +
                    "to make some serious profit, then return to my tribe.",
            )
            chatPlayer(quiz, "So, what are you selling?")
            chatNpc(
                happy,
                "Logs! Of all kinds! That's my plan, at least. Living in the jungle as we do, my " +
                    "people understand exotic wood very well indeed.",
            )
            when (
                choice2(
                    "Okay, show me the prices of logs.",
                    2,
                    "Sorry, I need to be making tracks.",
                    3,
                )
            ) {
                2 -> reloboPrices()
                3 -> reloboLeave()
            }
            return
        }
        when (option) {
            2 -> reloboPrices()
            3 -> reloboLeave()
        }
    }

    // TODO(content): [system:grand_exchange] open the Logs price guide
    private suspend fun Dialogue.reloboPrices() {
        chatPlayer(happy, "Okay, show me the prices of logs.")
    }

    private suspend fun Dialogue.reloboLeave() {
        chatPlayer(neutral, "Sorry, I need to be making tracks.")
        chatNpc(happy, "Okay, nice talking to you!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Abigaila
    // TODO(content): [boss:theatre_of_blood] Scry op and the scrying ball spectating options
    private suspend fun Dialogue.abigaila() {
        chatNpc(happy, "What can I help you with, dear?")
        while (true) {
            val option =
                choice3(
                    "Who are you and what are you doing?",
                    1,
                    "I'd like to look in your scrying ball.",
                    2,
                    "I'll leave you be.",
                    3,
                )
            when (option) {
                1 -> abigailaStory()
                2 -> {
                    chatPlayer(quiz, "I'd like to look in your scrying ball.")
                    return
                }
                3 -> {
                    chatPlayer(neutral, "I'll leave you be.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.abigailaStory() {
        chatPlayer(quiz, "Who are you and what are you doing?")
        chatNpc(
            sad,
            "I'm Abigaila, from Morytania. It's a terrible place, run by the vampyres. They " +
                "visit the towns and collect everyone's blood for food!",
        )
        chatNpc(
            sad,
            "I tried for years to escape, with my son, Suro, to make a better life for ourselves " +
                "in Misthalin. I scraped together enough money to bribe a boatman to get us out.",
        )
        chatNpc(
            sad,
            "The journey was tough. Some men in pink hoods attacked our boat, shouting at us to " +
                "go back where we belonged, and to keep out of their country. In the struggle, " +
                "the boat overturned.",
        )
        chatNpc(
            sad,
            "It was dark, and I couldn't find Suro in the water. I swam as deep as I could, " +
                "until my lungs screamed, but I couldn't find him. He wasn't good at swimming.",
        )
        chatNpc(
            sad,
            "Eventually I got myself to the shore. The sun was rising, and I found Suro there. " +
                "On the shore, where the water had dumped his... body.",
        )
        chatNpc(sad, "*sob*")
        chatNpc(
            neutral,
            "I've done alright in Misthalin, I guess. I know a bit of magic, and the local " +
                "gentry pay me well for my scrying skills. I'm good at turning a profit on Grand " +
                "Exchange trades too.",
        )
        chatNpc(
            neutral,
            "Anyway, I'm able to rent a room in Lumbridge, and to buy food. I'm not being a " +
                "burden on anyone, and there are no vampyres coming for my blood.",
        )
        chatNpc(happy, "I am free!")
        chatNpc(
            neutral,
            "But I haven't forgotten my people in the east. There's one vampyre there who runs a " +
                "place she calls the Theatre of Blood, challenging humans to face monsters in " +
                "her arena.",
        )
        chatNpc(
            sad,
            "My husband went there, trying to win freedom from the blood-gatherers by surviving " +
                "her arena, but he never returned. No-one does.",
        )
        chatNpc(
            neutral,
            "So now I pass my spare time scrying on the Theatre of Blood, watching other " +
                "adventurers there. You can have a look, if you like.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Clerk_(Clan_Hub)
    // TODO(content): [verify:in_game] option menus and where each branch returns (flattened wiki transcript)
    private suspend fun Dialogue.clanClerk() {
        chatNpc(happy, "Good day.")
        var menu = CLAN_MENU_INITIAL
        while (true) {
            val option =
                when (menu) {
                    CLAN_MENU_INITIAL ->
                        choice4(
                            "Who are you?",
                            1,
                            "What is a clan?",
                            2,
                            "I'm in a clan. Can you give me anything?",
                            3,
                            "Goodbye.",
                            4,
                        )
                    CLAN_MENU_WHO ->
                        choice4(
                            "What do you do here?",
                            5,
                            "What is a clan?",
                            2,
                            "I'm in a clan. Can you give me anything?",
                            3,
                            "Goodbye.",
                            4,
                        )
                    else ->
                        choice5(
                            "How do I join a clan?",
                            6,
                            "What can a clan offer me?",
                            7,
                            "What is a Clan Hall?",
                            8,
                            "I'm in a clan. Can you give me anything?",
                            3,
                            "Goodbye.",
                            4,
                        )
                }
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "My name is Clerk. I work on gathering people together to form what we " +
                            "call clans.",
                    )
                    menu = CLAN_MENU_WHO
                }
                2 -> {
                    clanClerkWhatIsAClan()
                    menu = CLAN_MENU_CLAN
                }
                3 -> {
                    if (!clanClerkItems()) {
                        return
                    }
                    menu = CLAN_MENU_INITIAL
                }
                4 -> {
                    chatPlayer(happy, "Goodbye.")
                    chatNpc(happy, "Goodbye. Come back any time.")
                    return
                }
                5 -> {
                    clanClerkWhatDoYouDo()
                    menu = CLAN_MENU_WHO
                }
                6 -> clanClerkHowToJoin()
                7 -> clanClerkOffer()
                8 -> {
                    clanClerkClanHall()
                    menu = CLAN_MENU_INITIAL
                }
            }
        }
    }

    private suspend fun Dialogue.clanClerkWhatIsAClan() {
        chatPlayer(quiz, "What is a clan?")
        chatNpc(
            neutral,
            "Ah, clans. Where do I begin? Clans are groups of people gathered together for " +
                "numerous reasons. Usually they have shared interests, like activities they " +
                "enjoy together.",
        )
        chatNpc(
            neutral,
            "These activities could include improving certain skills, battling the same " +
                "monsters, or even battling other clans!",
        )
        chatNpc(
            neutral,
            "Other clans form together because they just enjoy the company of other like-minded " +
                "people. The clan community can be a great resource if you should ever need help " +
                "with anything.",
        )
    }

    private suspend fun Dialogue.clanClerkHowToJoin() {
        chatPlayer(quiz, "How do I join a clan?")
        chatNpc(
            neutral,
            "You can either form your own clan with your friends, or apply to an already " +
                "existing clan.",
        )
        chatNpc(
            neutral,
            "Should you choose to form your own clan you will need four friends to join you, and " +
                "it is recommended that you have spent a good amount of time in this world " +
                "first. Leadership is not for everyone.",
        )
        chatNpc(
            happy,
            "However, fret not. There are plenty of interesting and exciting clans out there! " +
                "Check out the clan recruitment board to see which clans are currently accepting " +
                "applications.",
        )
    }

    private suspend fun Dialogue.clanClerkOffer() {
        chatPlayer(quiz, "What can a clan offer me?")
        chatNpc(
            neutral,
            "Clans offer you the ability to have an official gathering of friends or like-minded " +
                "individuals in a hierarchical structure.",
        )
        chatNpc(
            neutral,
            "In other words, you and your friends get an exclusive chat channel and a Clan Hall, " +
                "as well as the ability to assign ranks to members.",
        )
        chatNpc(
            neutral,
            "Ranks can be assigned from custom pre-sets, which then determine what permissions " +
                "that person has.",
        )
        chatNpc(
            neutral,
            "For example: One person may have the rank to kick players from the chat, while " +
                "another has the rank to access the Clan Coffer.",
        )
        chatPlayer(quiz, "What is the Clan Coffer?")
        chatNpc(
            neutral,
            "The Clan Coffer is a shared coin store, used by members of the clan. Highly ranked " +
                "members can assign who has permission to deposit and withdraw coins.",
        )
    }

    private suspend fun Dialogue.clanClerkClanHall() {
        chatPlayer(quiz, "What is a Clan Hall?")
        chatNpc(
            neutral,
            "A Clan Hall is a private area only accessible for the people with the permission to " +
                "enter. High ranked members of the clan can restrict it to only allow certain " +
                "ranks in, or open it up to even allow guests.",
        )
        chatPlayer(quiz, "Why would I need a Clan Hall? Can't I get a house instead?")
        chatNpc(
            happy,
            "The beauty of it is that you can have both. You can keep your house for private " +
                "guests and your own personal use, but when you wish to see larger groups then " +
                "the Clan Hall may be more fitting.",
        )
    }

    private suspend fun Dialogue.clanClerkWhatDoYouDo() {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "My mission is to administer clans; I help form them and can tell you anything you " +
                "need to know about them.",
        )
        chatPlayer(quiz, "Sure, but why are you here specifically?")
        chatNpc(
            neutral,
            "Why does anyone go anywhere? As a gatherer of people, I go where people gather. The " +
                "Grand Exchange is known to be a place where people come together. What better " +
                "place to bring them even closer?",
        )
        chatPlayer(neutral, "I suppose this usually is a crowded place.")
        chatNpc(
            happy,
            "Very much so. I was lucky to find this charming corner to set up the clan hub.",
        )
        chatPlayer(quiz, "What's the clan hub?")
        chatNpc(
            neutral,
            "The clan hub is what I call this area, my little corner of the Grand Exchange. This " +
                "is where every clan is born, as well as the best place to look for new recruits.",
        )
        chatNpc(neutral, "If you're in a clan you can also access your Clan Hall from here.")
    }

    // TODO(content): [system:clans] clan cloak and vexillum interface (also the Items op)
    private suspend fun Dialogue.clanClerkItems(): Boolean {
        chatPlayer(quiz, "I'm in a clan. Can you give me anything?")
        if (player.inv.isFull()) {
            chatNpc(
                neutral,
                "I've got cloaks that let you show your clan's colours, and vexilla to help you " +
                    "recruit more people into your clan, but you don't have space to take any.",
            )
            chatPlayer(neutral, "I suppose I'd better free up some space.")
            chatNpc(
                happy,
                "Yes, you should! Now, did you want to ask me anything else while you're here?",
            )
            return true
        }
        chatNpc(
            happy,
            "Certainly. I've got cloaks that let you show your clan's colours, and vexilla to " +
                "help you recruit more people into your clan.",
        )
        return false
    }

    // https://oldschool.runescape.wiki/w/Transcript:James
    // TODO(content): [system:bonds] bond redemption and the bond pouch
    private suspend fun Dialogue.james() {
        chatPlayer(quiz, "Hey, who are you?")
        chatNpc(
            happy,
            "I'm James. It's my job to tell you all there is to know about Old School Bonds.",
        )
        while (true) {
            val option =
                choice4(
                    "What is an Old School Bond?",
                    1,
                    "How do I buy an Old School Bond in game?",
                    2,
                    "How do I turn an Old School Bond into membership?",
                    3,
                    "More options...",
                    4,
                )
            when (option) {
                1 -> {
                    chatNpc(
                        neutral,
                        "An Old School Bond will allow you to buy membership for your account " +
                            "using in game gold pieces.",
                    )
                    chatNpc(
                        happy,
                        "My sources have also informed me that due to popular demand, they can " +
                            "now be redeemed for a free name change! Handy!",
                    )
                }
                2 ->
                    chatNpc(
                        neutral,
                        "You can get an Old School Bond from other players or from the Grand " +
                            "Exchange.",
                    )
                3 -> chatNpc(neutral, "Simply click the bond and choose the redeem option.")
                4 -> jamesMoreOptions()
            }
        }
    }

    private suspend fun Dialogue.jamesMoreOptions() {
        val option =
            choice3(
                "How much membership do I get from an Old School bond?",
                1,
                "Can I use an Old School Bond to pay for anything else?",
                2,
                "How do I buy an Old School Bond to give or sell to another player?",
                3,
            )
        when (option) {
            1 -> chatNpc(neutral, "You get 14 days membership from an Old School Bond.")
            2 ->
                chatNpc(
                    neutral,
                    "At the moment Old School Bonds can only be redeemed for membership and for " +
                        "display name changes.",
                )
            3 ->
                chatNpc(
                    neutral,
                    "Open the Bond Pouch from the Controls Settings and follow the instructions " +
                        "to buy a bond. You can then give the bond to another player or sell it " +
                        "on the Grand Exchange.",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meredith
    // TODO(content): [system:board_games] challenging players and the game tables
    private suspend fun Dialogue.meredith() {
        chatNpc(happy, "Welcome to the Games Zone!")
        while (true) {
            val option =
                choice4(
                    "How do I play board games?",
                    1,
                    "What games can I play?",
                    2,
                    "Can I buy a drink please?",
                    3,
                    "Thanks!",
                    4,
                )
            when (option) {
                1 -> meredithHowToPlay()
                2 -> meredithGames()
                3 -> {
                    chatPlayer(quiz, "Can I buy a drink please?")
                    chatNpc(neutral, "I'm afraid I don't sell drinks.")
                    return
                }
                4 -> {
                    chatPlayer(happy, "Thanks!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.meredithHowToPlay() {
        chatPlayer(quiz, "How do I play board games?")
        chatNpc(
            neutral,
            "You can challenge someone to a game anywhere in the Games Zone (except on a PvP or " +
                "Deadman world) by using the right click option.",
        )
        chatNpc(
            neutral,
            "Choose the type of game and then choose the options you want such as time per move.",
        )
        chatNpc(
            neutral,
            "If your opponent accepts the challenge you will be taken to one of the tables " +
                "around where you will play your game of choice.",
        )
        chatNpc(
            happy,
            "Once you have finished your game you will come back here where you can challenge " +
                "again! We also have tables in a few pubs around the world!",
        )
    }

    private suspend fun Dialogue.meredithGames() {
        chatPlayer(quiz, "What games can I play?")
        chatNpc(happy, "Currently we offer Draughts, Runelink, Runesquares, and Runeversi.")
        val option =
            choice4("Draughts?", 1, "Runelink?", 2, "Runesquares?", 3, "Runeversi?", 4)
        when (option) {
            1 -> {
                chatPlayer(quiz, "Draughts?")
                chatNpc(
                    neutral,
                    "Draughts uses standard rules, apart from: a draw is declared if no piece " +
                        "has been taken or promoted for forty moves.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Runelink?")
                chatNpc(neutral, "Yup, you have to get four runes in row.")
            }
            3 -> {
                chatPlayer(quiz, "Runesquares?")
                chatNpc(
                    neutral,
                    "Yes, you take it in turns to add a line with the goal of making squares. " +
                        "Everytime you make a square you take another turn.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Runeversi?")
                chatNpc(
                    neutral,
                    "Yep, the aim is to have more of your runes on the board than your opponent. " +
                        "You can take your opponents pieces by trapping them between your own.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Saradominist_recruiter
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.saradoministRecruiter() {
        chatPlayer(quiz, "Hey there, what's this all about?")
        chatNpc(happy, "Our Lord Saradomin needs your help!")
        chatPlayer(bored, "Oh great, what is it this time?")
        chatNpc(
            worried,
            "You're needed at Castle Wars, our forces are weakening and we need you as " +
                "reinforcements.",
        )
        when (choice2("Yes.", 1, "No.", 2)) {
            1 -> {
                chatPlayer(happy, "Sure, I'll play some Castle Wars.")
                chatNpc(happy, "Praise Saradomin, I'll send you there right away!")
                // TODO(content): [system:castle_wars] item checks and the teleport to the lobby
            }
            2 -> {
                chatPlayer(
                    neutral,
                    "Hmm, I'm alright actually, I don't want to play Castle Wars right now.",
                )
                chatNpc(worried, "I urge you to reconsider, before it's too late.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Zamorakian_recruiter
    // TODO(content): [system:npc_overhead] random overhead shouts
    private suspend fun Dialogue.zamorakianRecruiter() {
        chatPlayer(quiz, "Hey there, what's with all the shouting.")
        chatNpc(
            angry,
            "Our all-powerful Lord Zamorak is looking for the strongest of champions, you hardly " +
                "look like you fit that criteria.",
        )
        chatPlayer(angry, "I am big and strong, what does he need new champions for?")
        chatNpc(
            angry,
            "The glorious battlefield of Castle Wars, where we can smite down the weak followers " +
                "of Saradomin needs new blood.",
        )
        when (choice2("Yes.", 1, "No.", 2, title = "Join Castle Wars?")) {
            1 -> {
                chatPlayer(happy, "That sounds neat, I'll be a champion for Zamorak.")
                chatNpc(angry, "Bring death to Saradomin in his great name.")
                // TODO(content): [system:castle_wars] item checks and the teleport to the lobby
            }
            2 -> {
                chatPlayer(neutral, "He can find a champion somewhere else, I'll pass.")
                chatNpc(angry, "Fool, you'll regret this one day.")
            }
        }
    }

    private companion object {
        const val LUNAR_DIPLOMACY = "quest_lunardiplomacy"
        const val CLAN_MENU_INITIAL = 0
        const val CLAN_MENU_WHO = 1
        const val CLAN_MENU_CLAN = 2
    }
}
