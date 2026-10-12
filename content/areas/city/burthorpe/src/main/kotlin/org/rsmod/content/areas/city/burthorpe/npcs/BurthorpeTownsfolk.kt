package org.rsmod.content.areas.city.burthorpe.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BurthorpeTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        for (villager in VILLAGERS) {
            onOpNpc1(villager) { startDialogue(it.npc) { villager() } }
        }
        onOpNpc1("npc.death_woman_indoors1") { startDialogue(it.npc) { hild() } }
        onOpNpc1("npc.bernald") { startDialogue(it.npc) { bernald() } }
        onOpNpc1("npc.death_cook") { startDialogue(it.npc) { eadburg() } }
        onOpNpc1("npc.death_servant1") { startDialogue(it.npc) { servant() } }
        onOpNpc1("npc.death_smithy") { startDialogue(it.npc) { dunstan() } }
        onOpNpc1("npc.twocats_unferth") { startDialogue(it.npc) { unferth() } }
        onOpNpc1("npc.death_ig_solider_wander") { startDialogue(it.npc) { soldier() } }
        for (guard in GUARDS) {
            onOpNpc1(guard) { player.mes("The guard won't talk whilst on duty.") }
        }
        for (archer in ARCHERS) {
            onOpNpc1(archer) { player.mes("The archer won't talk whilst on duty.") }
        }
        for (sergeant in SERGEANTS) {
            onOpNpc1(sergeant) { player.mes("The Sergeant is busy training the soldiers.") }
        }
        for (soldier in TRAINING_SOLDIERS) {
            onOpNpc1(soldier) { player.mes("The soldier is busy training.") }
        }
        for (soldier in SITTING_SOLDIERS) {
            onOpNpc1(soldier) { player.mes("The soldier is busy eating.") }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Breoca
    private suspend fun Dialogue.villager() {
        chatPlayer(happy, "Hi!")
        if (QuestRequirements.hasCompleted(player, DEATH_PLATEAU)) {
            chatNpc(happy, VILLAGER_LINES_AFTER[access.random.of(VILLAGER_LINES_AFTER.size)])
            return
        }
        val line = access.random.of(VILLAGER_LINES.size)
        chatNpc(neutral, VILLAGER_LINES[line])
        if (line == WELCOME_LINE) {
            chatPlayer(happy, "Thanks!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hild
    private suspend fun Dialogue.hild() {
        val lostAmulet =
            QuestRequirements.hasCompleted(player, A_TAIL_OF_TWO_CATS) &&
                !playerContainsObj(CATSPEAK_AMULET_E)
        if (!lostAmulet) {
            villager()
            return
        }
        chatPlayer(sad, "I've lost my enchanted Amulet of Catspeak.")
        chatNpc(
            neutral,
            "This can be rectified, simply bring me another Amulet of Catspeak and five death " +
                "runes and I will enchant it for you.",
        )
        if (access.inv.count(DEATH_RUNE) < ENCHANT_DEATH_RUNES) {
            chatPlayer(sad, "I don't have the five death runes on me.")
            chatNpc(neutral, "Come back to me when you have them.")
            return
        }
        if (access.inv.count(CATSPEAK_AMULET) < 1) {
            chatPlayer(neutral, "I have the runes but don't have the amulet on me.")
            chatNpc(neutral, "Bring me the amulet and I will enchant it for you.")
            return
        }
        chatPlayer(happy, "I have the death runes with me now.")
        chatNpc(neutral, "Good. Give me the amulet so that I may perform the enchantment.")
        if (!swap(DEATH_RUNE, ENCHANT_DEATH_RUNES, CATSPEAK_AMULET, CATSPEAK_AMULET_E)) {
            return
        }
        chatNpc(
            neutral,
            "Using the enchanted amulet is easy; open up the amulet and you'll see a cat's face, " +
                "use the whiskers to rotate the cats nose. You'll see the nose move and the new " +
                "direction highlighted. When the nose points in the",
        )
        chatNpc(
            neutral,
            "direction that Bob is in, the eyes will light up and the mouth will open. You'll " +
                "need to head in that direction and then check the direction again. If you are " +
                "very close to Bob then the eyes and mouth will animate no",
        )
        chatNpc(
            neutral,
            "matter which way the nose is pointing. That's all there is to it, good luck!",
        )
        chatPlayer(happy, "Thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bernald
    // TODO(content): [quest:garden_of_tranquillity] in-progress dialogue
    private suspend fun Dialogue.bernald() {
        if (QuestRequirements.hasCompleted(player, GARDEN_OF_TRANQUILLITY)) {
            chatPlayer(quiz, "How are your grapes coming along?")
            chatNpc(happy, "Marvellously, thanks to your help, ${player.displayName}!")
            return
        }
        chatNpc(quiz, "Do you know anything about grapevine diseases?")
        chatPlayer(neutral, "No, I'm afraid I don't.")
        chatNpc(
            sad,
            "Oh that's a shame. I hope I find someone soon, otherwise I could lose all this " +
                "year's crops.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Eadburg
    private suspend fun Dialogue.eadburg() {
        chatPlayer(quiz, "What's cooking?")
        chatNpc(neutral, "The stew for the servant's main meal.")
        chatPlayer(neutral, "Oh... Fair enough. See you later.")
        chatNpc(neutral, "Bye.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Servant_(Burthorpe)
    private suspend fun Dialogue.servant() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hi!")
        chatNpc(worried, "Look, I'd better not talk. I'll get in trouble.")
        chatNpc(
            neutral,
            "If you want someone to show you round the castle ask Eohric, the Head Servant.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dunstan
    // https://oldschool.runescape.wiki/w/Transcript:Death_Plateau (greeting before the quest)
    // TODO(content): [verify:in_game] the wiki has no menu before Death Plateau; the options
    //  are the "Talk about something else." ones that need no quest
    // TODO(content): [verify:in_game] the wiki gives no greeting before the "Talk about a
    //  quest." menu; this keeps the Death Plateau greeting in front of it
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.dunstan() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hi! How can I help?")
        if (QuestRequirements.hasCompleted(player, TROLL_ROMANCE)) {
            val quest = choice2("Talk about a quest.", true, "Talk about something else.", false)
            if (quest) {
                dunstanQuestTalk()
                return
            }
        }
        dunstanOtherTalk()
    }

    private suspend fun Dialogue.dunstanQuestTalk() {
        chatNpc(happy, "Well, if it isn't our very own sledder! What can I do for you today?")
        while (true) {
            val options =
                buildList {
                    if (canBuyLawTalisman()) add("Can I have another law talisman?" to 1)
                    add("Can you put some spikes on my Climbing boots?" to 2)
                    add("Can you make me another sled?" to 3)
                    add("What do I need for the sled wax again?" to 4)
                    add("Nothing, thanks." to 5)
                }
            when (choiceOf(options)) {
                1 -> {
                    dunstanLawTalisman()
                    return
                }
                2 -> dunstanSpikes()
                3 -> {
                    dunstanSled()
                    return
                }
                4 -> {
                    dunstanSledWax()
                    return
                }
                5 -> {
                    chatPlayer(neutral, "Nothing, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.dunstanOtherTalk() {
        while (true) {
            val deathPlateau = QuestRequirements.hasCompleted(player, DEATH_PLATEAU)
            val options =
                buildList {
                    if (
                        QuestRequirements.hasCompleted(player, TROLL_ROMANCE) &&
                            canBuyLawTalisman()
                    ) {
                        add("Can I have another law talisman?" to 1)
                    }
                    if (deathPlateau) {
                        add("Can you put some spikes on my Climbing boots?" to 2)
                        add("How is your son getting on?" to 3)
                    }
                    add("Is it OK if I use your anvil?" to 4)
                    add("Nothing, thanks." to 5)
                }
            when (choiceOf(options)) {
                1 -> {
                    dunstanLawTalisman()
                    return
                }
                2 -> dunstanSpikes()
                3 -> dunstanSon()
                4 -> {
                    chatPlayer(quiz, "Is it OK if I use your anvil?")
                    chatNpc(quiz, "So you're a smithy are you?")
                    chatPlayer(neutral, "I dabble.")
                    chatNpc(happy, "A fellow smith is welcome to use my anvil!")
                    chatPlayer(happy, "Thanks!")
                    chatNpc(neutral, "Anything else before I get on with my work?")
                }
                5 -> {
                    chatPlayer(neutral, "Nothing, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.dunstanSon() {
        chatPlayer(quiz, "How is your son getting on?")
        if (QuestRequirements.hasCompleted(player, TROLL_STRONGHOLD)) {
            chatNpc(
                happy,
                "He is getting on fine! He has just been promoted to Sergeant! I'm really proud " +
                    "of him!",
            )
            chatPlayer(happy, "I'm happy for you!")
        } else {
            chatNpc(
                sad,
                "He was captured by those blasted trolls! I don't know what to do. Even the " +
                    "imperial guard are too afraid to go rescue him.",
            )
            chatPlayer(quiz, "What happened?")
            chatNpc(neutral, "Talk to Denulth, he can tell you all about it.")
        }
        chatNpc(neutral, "Anything else before I get on with my work?")
    }

    private fun Dialogue.canBuyLawTalisman(): Boolean {
        val bank = player.invMap[BANK]?.count(LAW_TALISMAN) ?: 0
        return access.inv.count(LAW_TALISMAN) + bank < LAW_TALISMAN_LIMIT
    }

    private suspend fun Dialogue.dunstanLawTalisman() {
        chatPlayer(quiz, "Can I have another law talisman?")
        chatNpc(
            neutral,
            "Well, I've got one here you can have for 1,000 coins if you'd like it?",
        )
        if (!choice2("Yes, I'll take it.", true, "No thanks...", false)) {
            chatPlayer(neutral, "No thanks...")
            return
        }
        chatPlayer(happy, "Yes, I'll take it.")
        if (access.inv.count(COINS) < LAW_TALISMAN_PRICE) {
            chatNpc(
                neutral,
                "Looks like you don't have enough money to afford it just now, come back when " +
                    "you do.",
            )
            return
        }
        if (access.inv.freeSpace() < 1) {
            chatNpc(
                neutral,
                "Looks like you don't have any room to carry it just now, come back when you do.",
            )
            return
        }
        if (!swap(COINS, LAW_TALISMAN_PRICE, null, LAW_TALISMAN)) {
            return
        }
        mesbox("You hand over the coins... and receive a law talisman.")
        chatPlayer(happy, "Thanks.")
    }

    // TODO(content): [verify:in_game] the wiki gives both spiked boots messages twice; this
    //  shows each once
    private suspend fun Dialogue.dunstanSpikes() {
        chatPlayer(quiz, "Can you put some spikes on my Climbing boots?")
        chatNpc(happy, "For you, no problem.")
        chatNpc(
            neutral,
            "Do you realise that you can only use the Climbing boots right now? The Spiked boots " +
                "can only be used in the Icelands but no one's been able to get there for years!",
        )
        if (!choice2("Yes, but I still want them.", true, "Oh OK, I'll leave them thanks.", false)) {
            chatPlayer(neutral, "Oh OK, I'll leave them thanks.")
        } else {
            chatPlayer(neutral, "Yes, but I still want them.")
            if (access.inv.count(CLIMBING_BOOTS) < 1) {
                chatPlayer(sad, "I don't have them on me.")
            } else if (access.inv.count(IRON_BAR) < 1) {
                chatNpc(neutral, "Sorry, I'll need an Iron bar to make the spikes.")
            } else if (swap(IRON_BAR, 1, CLIMBING_BOOTS, SPIKED_BOOTS)) {
                mesbox("You give Dunstan an Iron bar and the climbing boots.")
                mesbox("Dunstan has given you the Spiked boots.")
            }
        }
        chatNpc(neutral, "Anything else before I get on with my work?")
    }

    // TODO(content): [quest:troll_romance] making the sled from the materials (the wiki only
    //  gives the dialogue without them)
    private suspend fun Dialogue.dunstanSled() {
        chatPlayer(quiz, "Can you make me another sled?")
        chatNpc(
            quiz,
            "Yeah sure, have you got the maple or yew logs, an iron bar and some rope?",
        )
        val logs = access.inv.count(MAPLE_LOGS) > 0 || access.inv.count(YEW_LOGS) > 0
        val materials = logs && access.inv.count(IRON_BAR) > 0 && access.inv.count(ROPE) > 0
        if (!materials) {
            chatPlayer(sad, "No, I haven't got everything yet.")
            chatNpc(
                neutral,
                "Well, unfortunately I can't make the sled without all the materials.",
            )
        }
    }

    private suspend fun Dialogue.dunstanSledWax() {
        chatPlayer(quiz, "What do I need for the sled wax again?")
        chatNpc(neutral, "You will need some wax and some swamp tar.")
        var asked = false
        while (true) {
            val options =
                buildList {
                    add("Where can I get wax from?" to 1)
                    add("Where can I get swamp tar from?" to 2)
                    add("What else do I need?" to 3)
                    if (asked) add("See you!" to 4)
                }
            when (choiceOf(options)) {
                1 -> {
                    chatPlayer(quiz, "Where can I get wax from?")
                    chatNpc(
                        neutral,
                        "I don't know. I'm a smith! But maybe you should look for some bees.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Where can I get swamp tar from?")
                    chatNpc(neutral, "The swamp of course! It's south of Lumbridge.")
                }
                3 -> {
                    chatPlayer(quiz, "What else do I need?")
                    chatNpc(neutral, "You will also need a cake tin to store the wax in.")
                }
                4 -> {
                    chatPlayer(happy, "See you!")
                    chatNpc(happy, "Thank you, come again.")
                    return
                }
            }
            asked = true
        }
    }

    private fun Dialogue.swap(take: String, count: Int, alsoTake: String?, give: String): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = take.asRSCM(RSCMType.OBJ)
                    strictCount = count
                }
                if (alsoTake != null) {
                    delete {
                        from = inv
                        obj = alsoTake.asRSCM(RSCMType.OBJ)
                        strictCount = 1
                    }
                }
                insert {
                    into = inv
                    obj = give.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private suspend fun Dialogue.choiceOf(options: List<Pair<String, Int>>): Int =
        when (options.size) {
            2 -> choice2(options[0].first, options[0].second, options[1].first, options[1].second)
            3 ->
                choice3(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                )
            4 ->
                choice4(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                    options[3].first,
                    options[3].second,
                )
            else ->
                choice5(
                    options[0].first,
                    options[0].second,
                    options[1].first,
                    options[1].second,
                    options[2].first,
                    options[2].second,
                    options[3].first,
                    options[3].second,
                    options[4].first,
                    options[4].second,
                )
        }

    // https://oldschool.runescape.wiki/w/Transcript:Unferth
    // https://oldschool.runescape.wiki/w/Transcript:A_Tail_of_Two_Cats (without the requirements)
    // TODO(content): [quest:a_tail_of_two_cats] quest start, in-progress, and after-quest
    //  dialogue before Dragon Slayer II (not in the transcript)
    private suspend fun Dialogue.unferth() {
        if (QuestRequirements.hasCompleted(player, DRAGON_SLAYER_II)) {
            chatPlayer(neutral, "Hi Unferth. How are you doing?")
            chatNpc(sad, "It's just not the same without Bob around.")
            chatPlayer(sad, "I'm so sorry Unferth.")
            chatNpc(
                sad,
                "Gertrude asked me if I'd like one of her new kittens. I don't think I'm ready " +
                    "for that yet.",
            )
            chatPlayer(neutral, "Give it time. Things will get better, I promise.")
            chatNpc(neutral, "Thanks ${player.displayName}.")
            return
        }
        chatNpc(sad, "Hello.")
        chatPlayer(quiz, "What's wrong?")
        chatNpc(sad, "It's fine. Nothing for you to worry about.")
        mesbox("You do not meet all of the requirements to start the A Tail of Two Cats quest.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Soldier_(Burthorpe)
    private suspend fun Dialogue.soldier() {
        chatPlayer(happy, "Hi!")
        chatNpc(neutral, SOLDIER_LATIN[access.random.of(SOLDIER_LATIN.size)])
        when (access.random.of(SOLDIER_REPLIES)) {
            0 -> chatPlayer(confused, "What?!")
            1 -> chatPlayer(confused, "Huh?!")
            2 -> chatPlayer(confused, "Er...")
            3 -> chatPlayer(neutral, "OK...")
            else -> {
                chatPlayer(angry, "Are you insulting me in Latin?")
                chatNpc(laugh, "Yes!")
                chatPlayer(angry, "Hmm...")
            }
        }
    }

    private companion object {
        const val DEATH_PLATEAU = "quest_deathplateau"
        const val TROLL_STRONGHOLD = "quest_trollstronghold"
        const val TROLL_ROMANCE = "quest_trollromance"
        const val A_TAIL_OF_TWO_CATS = "quest_tailoftwocats"
        const val GARDEN_OF_TRANQUILLITY = "quest_gardenoftranquillity"
        const val DRAGON_SLAYER_II = "quest_dragonslayer2"

        const val COINS = "obj.coins"
        const val DEATH_RUNE = "obj.deathrune"
        const val CATSPEAK_AMULET = "obj.ics_little_amulet_of_catspeak"
        const val CATSPEAK_AMULET_E = "obj.twocats_amuletofcatspeak"
        const val CLIMBING_BOOTS = "obj.death_climbingboots"
        const val SPIKED_BOOTS = "obj.death_spikedboots"
        const val IRON_BAR = "obj.iron_bar"
        const val LAW_TALISMAN = "obj.law_talisman"
        const val MAPLE_LOGS = "obj.maple_logs"
        const val YEW_LOGS = "obj.yew_logs"
        const val ROPE = "obj.rope"
        const val BANK = "inv.bank"

        // https://oldschool.runescape.wiki/w/Transcript:Hild
        const val ENCHANT_DEATH_RUNES = 5

        // https://oldschool.runescape.wiki/w/Transcript:Dunstan
        const val LAW_TALISMAN_PRICE = 1_000
        const val LAW_TALISMAN_LIMIT = 5

        const val WELCOME_LINE = 4
        const val SOLDIER_REPLIES = 5

        val VILLAGERS =
            listOf(
                "npc.death_man_outdoors1",
                "npc.death_woman_outdoors2",
                "npc.death_woman_outdoors1",
                "npc.death_man_outdoors2",
                "npc.death_man_indoors2",
            )

        val GUARDS = listOf("npc.death_guard1", "npc.death_guard2")

        // TODO(content): [quest:death_plateau] the north-western tower archer thanks the player
        //  after the equipment room is unlocked
        val ARCHERS =
            listOf("npc.death_archer1", "npc.death_archer2", "npc.death_archer_trapped")

        // TODO(content): [verify:in_game] the wiki gives the Sergeant and training soldier
        //  lines without saying whether they are game messages or message boxes
        val SERGEANTS = listOf("npc.death_ig_sergeant_drilling", "npc.death_ig_sergeant_training")

        val TRAINING_SOLDIERS =
            listOf("npc.death_ig_solider_drilling", "npc.death_ig_solider_training")

        val SITTING_SOLDIERS =
            listOf(
                "npc.death_ig_solider_sitting1",
                "npc.death_ig_solider_sitting2",
                "npc.death_ig_solider_sitting3",
            )

        val VILLAGER_LINES =
            listOf(
                "Let me at 'em!",
                "Trolls? Schmolls!",
                "The trolls are coming!",
                "The trolls took my baby son!",
                "Welcome to Burthorpe!",
                "Trolls!",
                "Hello stranger.",
                "Go away!",
                "Run!",
                "Hi!",
                "The Imperial Guard can no longer protect us!",
                "The White Knights will soon have control!",
            )

        val VILLAGER_LINES_AFTER =
            listOf(
                "I heard about what you did, thank you!",
                "The White Knights are still going to take over.",
                "I hear the Imperial Guard are preparing an attack!",
                "Thank you so much!",
                "Surely we are safe now!",
            )

        val SOLDIER_LATIN =
            listOf(
                "Mihi ignosce. Cum homine de cane debeo congredi.",
                "Errare humanum est.",
                "Die dulci freure.",
                "Carpe Diem!",
                "Te audire non possum. Musa sapientum fixa est in aure.",
                "Furnulum pani nolo.",
                "Fac ut gaudeam.",
                "Utinam barbari spatium proprium tuum invadant!",
                "Quantum materiae materietur marmota monax si marmota monax materiam possit " +
                    "materiari?",
                "Sona si Latine loqueris.",
                "Raptus Regaliter",
                "Nemo dat quod non habet.",
                "Ne auderis delere orbem rigidum meum!",
                "Da mihi sis bubulae frustum assae, solana tuberosa in modo gallico fricta, ac " +
                    "quassum lactatum coagulatum crassum.",
                "Cogito ergo sum.",
                "Vacca foeda.",
                "Di! Ecce hora! Uxor mea me necabit!",
                "Latine loqui coactus sum.",
                "Cave ne ante ullas catapultas ambules.",
                "Fac ut vivas!",
                "Noli me vocare, ego te vocabo.",
                "Meliora cogito.",
                "Braccae tuae aperiuntur.",
                "Vescere bracis meis.",
                "Corripe cervisiam!",
            )
    }
}
