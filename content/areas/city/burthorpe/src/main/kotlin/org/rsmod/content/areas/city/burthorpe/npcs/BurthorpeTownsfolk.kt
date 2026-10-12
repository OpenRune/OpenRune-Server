package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onOpNpc1
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

    // https://oldschool.runescape.wiki/w/Transcript:Breoca (before Death Plateau)
    // TODO(content): [quest:death_plateau] after-quest lines
    private suspend fun Dialogue.villager() {
        chatPlayer(happy, "Hi!")
        val line = access.random.of(VILLAGER_LINES.size)
        chatNpc(neutral, VILLAGER_LINES[line])
        if (line == WELCOME_LINE) {
            chatPlayer(happy, "Thanks!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hild (before Death Plateau)
    // TODO(content): [quest:a_tail_of_two_cats] enchanting a replacement Amulet of Catspeak
    private suspend fun Dialogue.hild() {
        villager()
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bernald (before Garden of Tranquillity)
    // TODO(content): [quest:garden_of_tranquillity] quest and after-quest dialogue
    private suspend fun Dialogue.bernald() {
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
    // TODO(content): [quest:death_plateau] climbing boot spikes and "How is your son getting on?"
    // TODO(content): [quest:troll_romance] sled, sled wax and law talisman for 1,000 coins
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.dunstan() {
        chatPlayer(happy, "Hi!")
        chatNpc(happy, "Hi! How can I help?")
        while (choice2("Is it OK if I use your anvil?", true, "Nothing, thanks.", false)) {
            chatPlayer(quiz, "Is it OK if I use your anvil?")
            chatNpc(quiz, "So you're a smithy are you?")
            chatPlayer(neutral, "I dabble.")
            chatNpc(happy, "A fellow smith is welcome to use my anvil!")
            chatPlayer(happy, "Thanks!")
            chatNpc(neutral, "Anything else before I get on with my work?")
        }
        chatPlayer(neutral, "Nothing, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:A_Tail_of_Two_Cats (without the requirements)
    // TODO(content): [quest:a_tail_of_two_cats] quest start, in-progress and after-quest dialogue
    // TODO(content): [quest:dragon_slayer_ii] after-quest dialogue about Bob
    private suspend fun Dialogue.unferth() {
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
