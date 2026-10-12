package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.areas.city.rellekka.brotherOrSister
import org.rsmod.content.areas.city.rellekka.fremennikGreeting
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_fremennik_trials] each council member's trial task
// TODO(content): [skill:thieving] pickpocketing Agnar, Inga and Pontak; before the Trials
//  they say "You stay away from me outerlander!" and the player is too suspicious to steal
class FremennikCouncil @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_man") { startDialogue(it.npc) { mustTalkToBrundt() } }
        onOpNpc1("npc.viking_woman_indoors") { startDialogue(it.npc) { mustTalkToBrundt() } }
        onOpNpc1("npc.viking_man5") { startDialogue(it.npc) { mustTalkToBrundt() } }
        onOpNpc1("npc.viking_fisherman1") { startDialogue(it.npc) { fisherman() } }
        onOpNpc1("npc.viking_reveller_3") { startDialogue(it.npc) { manni() } }
        onOpNpc1("npc.viking_olaf") { startDialogue(it.npc) { olaf() } }
        onOpNpc1("npc.viking_peer") { startDialogue(it.npc) { peer() } }
        onOpNpc3("npc.viking_peer") { startDialogue(it.npc) { peerDeposit() } }
        onOpNpc1("npc.viking_sigli") { startDialogue(it.npc) { sigli() } }
        onOpNpc1("npc.viking_hallifred") { startDialogue(it.npc) { swensen() } }
        onOpNpc1("npc.viking_thorvald") { startDialogue(it.npc) { thorvald() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Agnar
    // https://oldschool.runescape.wiki/w/Transcript:Inga
    // https://oldschool.runescape.wiki/w/Transcript:Pontak
    private suspend fun Dialogue.mustTalkToBrundt() {
        if (isFremennik()) {
            fremennikGreeting()
            return
        }
        chatNpc(angry, "I cannot speak to you outerlander! Talk to Brundt, the Chieftain!")
    }

    private fun Dialogue.isFremennik(): Boolean =
        QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)

    // https://oldschool.runescape.wiki/w/Transcript:Fisherman_(Rellekka)
    private suspend fun Dialogue.fisherman() {
        chatPlayer(happy, "Hello there.")
        if (isFremennik()) {
            chatNpc(
                happy,
                "Welcome back ${brotherOrSister()} ${fremennikName()}. It has been too long " +
                    "since last we spoke! The fish are really biting today!",
            )
            return
        }
        chatNpc(
            angry,
            "Don't talk to me outerlander. Anything you have to say to me, you can say to the " +
                "Chieftain. Goodbye.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Manni_the_Reveller
    private suspend fun Dialogue.manni() {
        if (isFremennik()) {
            chatPlayer(happy, "Howdy!")
            chatNpc(happy, "Hey! It's ${fremennikName()}! Let me buy you a drink!")
            access.invAddOrDrop(objRepo, "obj.beer")
            chatNpc(happy, "There ya go! Anyone who can drink like you earns my respect!")
            return
        }
        chatPlayer(happy, "Hello there!")
        chatNpc(
            neutral,
            "Do not think me rude outerlander, but our customs forbid me talking to you. All " +
                "contact with outerlanders must be vetted by our chieftain, Brundt.",
        )
        chatPlayer(quiz, "Where is this Brundt?")
        chatNpc(neutral, "He is standing just over there. He will speak for the tribe.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Olaf_the_Bard
    private suspend fun Dialogue.olaf() {
        if (isFremennik()) {
            fremennikOlaf()
            return
        }
        chatPlayer(quiz, "Hello there. So you're a bard?")
        chatNpc(
            neutral,
            "I am afraid I cannot speak to outerlanders. Besides, I am busy composing an epic.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Peer_the_Seer
    // TODO(content): [quest:the_fremennik_trials] trial task
    // TODO(content): [system:fremennik_armour] skeletal armour crafting after the Trials
    // TODO(content): [quest:desert_treasure_ii] combining ring icons with ancient vestiges
    private suspend fun Dialogue.peer() {
        if (isFremennik()) {
            fremennikPeer()
            return
        }
        if (SKELETAL_PIECES.none { it in player.inv }) {
            chatNpc(neutral, "I have no interest in talking to you just now outerlander.")
            return
        }
        val armour =
            choice2(
                "Wallasalki Bone Armour",
                true,
                "The Fremennik Trials",
                false,
                title = "What would you like to ask about?",
            )
        if (!armour) {
            chatNpc(neutral, "I have no interest in talking to you just now outerlander.")
            return
        }
        chatPlayer(
            quiz,
            "Hello there. I have some wallasalki bone fragments, and I understand you may be " +
                "able to use them to make some armour for me...?",
        )
        chatNpc(
            neutral,
            "I see great things in your future outerlander, but I will not go against the " +
                "wishes of the tribe, and will not make armour for an outerlander.",
        )
        chatNpc(
            neutral,
            "You may try and gain a dispensation from Chieftain Brundt, but I suspect you will " +
                "be unsuccessful...",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Peer_the_Seer
    // TODO(content): [diary:fremennik] open the deposit box after the easy Fremennik diary
    private suspend fun Dialogue.peerDeposit() {
        chatNpc(
            neutral,
            "I cannot do that for you until you have completed the easy tier of the Fremennik " +
                "achievement diary.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sigli_the_Huntsman
    // TODO(content): [quest:the_fremennik_trials] trial task
    // TODO(content): [system:fremennik_armour] spined armour crafting after the Trials, when the
    //  player has spined dagannoth hides
    private suspend fun Dialogue.sigli() {
        if (isFremennik()) {
            chatPlayer(happy, "Hello again Sigli.")
            chatNpc(neutral, "Hello again to you ${fremennikName()}. How goes the hunt?")
            chatPlayer(
                confused,
                "The hunt? I've already done your task! That's why I'm a Fremennik now!",
            )
            chatNpc(
                neutral,
                "You think so do you? The Draugen is always out there, just as I and my hunters " +
                    "are always on its trail to keep it at bay.",
            )
            chatNpc(neutral, "The hunt will never end until I do.")
            return
        }
        if (SPINED_HIDES.none { it in player.inv }) {
            sigliRefusal()
            return
        }
        val armour =
            choice2(
                "Dagannoth Spined Armour",
                true,
                "The Fremennik Trials",
                false,
                title = "What would you like to ask about?",
            )
        if (!armour) {
            sigliRefusal()
            return
        }
        chatPlayer(
            quiz,
            "Hello there. I have some spined dagannoth hides, and believe you might be able to " +
                "make an armour of them for me...?",
        )
        chatNpc(
            neutral,
            "My apologies outerlander, but we are forbidden by decree to communicate with those " +
                "not of our tribe, and this includes making armour for you.",
        )
        chatNpc(
            neutral,
            "I suggest you speak to Brundt, our chieftain. He resides in the longhall and speaks " +
                "for us all.",
        )
    }

    private suspend fun Dialogue.sigliRefusal() {
        chatNpc(
            neutral,
            "I do not speak to outerlanders. If you have anything of import to say, go and " +
                "speak to the Chieftain.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Swensen_the_Navigator
    private suspend fun Dialogue.swensen() {
        chatPlayer(happy, "Hello!")
        if (isFremennik()) {
            chatNpc(
                happy,
                "Greetings to you ${brotherOrSister()} ${fremennikName()}! How fare you today?",
            )
            chatPlayer(happy, "I am fine thanks Swensen. How are you doing?")
            chatNpc(happy, "I am fine too!")
            return
        }
        chatNpc(
            neutral,
            "Hello outerlander. This is my home, I would be grateful if you would leave.",
        )
        chatPlayer(sad, "Oh. Okay.")
        chatNpc(
            neutral,
            "I am sorry outerlander, I will not offer you hospitality until my Chieftain has " +
                "vouched for your honesty. This is our way.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Thorvald_the_Warrior
    private suspend fun Dialogue.thorvald() {
        if (isFremennik()) {
            chatPlayer(happy, "Howdy Thorvald!")
            chatNpc(
                neutral,
                "And greetings to you too. It is good to see new blood entering the Fremennik; " +
                    "we gain our strength by bringing new warriors into the tribe.",
            )
            return
        }
        chatNpc(angry, "Leave me be, outerlander. I have nothing to say to the likes of you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Olaf_the_Bard (after the Trials)
    // TODO(content): [system:music_cape] the Music cape and hood after "About the Music Cape...",
    //  which depend on whether every song is unlocked
    // TODO(content): [quest:the_fremennik_isles] "Do you know any great stories about Basilisk
    //  Jaws?" while holding a basilisk jaw, which unlocks Neitiznot faceguards
    private suspend fun Dialogue.fremennikOlaf() {
        chatNpc(
            happy,
            "Hello again to you ${fremennikName()}. Us bards should stick together, what?",
        )
        val cape =
            choice2("About the Music Cape...", true, "Can I learn to play my lyre again?", false)
        if (cape) {
            chatPlayer(quiz, "About the Music Cape...")
            return
        }
        chatPlayer(quiz, "I was wondering... Can I learn to play my lyre again?")
        chatNpc(
            neutral,
            "Well, that is an interesting question. Let me let you into a little secret. If you " +
                "make another offering to Fossegrimen you will learn a secret melody.",
        )
        chatPlayer(quiz, "What kind of melody?")
        chatNpc(
            neutral,
            "It is the song of Rellekka. When you play it, it will bring you back to this town " +
                "wherever you are in this world.",
        )
        chatNpc(
            neutral,
            "We often go adventuring with bards for precisely this reason; No matter where we " +
                "have ended up we can return safe and sound to home.",
        )
        chatPlayer(happy, "Thanks Olaf!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Peer_the_Seer (after the Trials)
    // TODO(content): [diary:fremennik] "Can you bank my stuff?" opening the deposit box
    private suspend fun Dialogue.fremennikPeer() {
        chatPlayer(happy, "Hello Peer.")
        chatNpc(
            happy,
            "Greetings to you ${brotherOrSister()} ${fremennikName()}! What brings you to see me " +
                "again?",
        )
        val future = choice2("Can you tell my future?", true, "Nothing really.", false)
        if (!future) {
            chatPlayer(happy, "Nothing really, I just stopped by to say hello.")
            chatNpc(happy, "Well, hello to you too!")
            return
        }
        chatPlayer(
            quiz,
            "I was wondering if you could give me a reading on my future...?",
        )
        chatNpc(
            neutral,
            "Ah, you would like a prediction? I do not see that that would be so difficult... " +
                "wait a moment...",
        )
        // TODO(content): [verify:in_game] how long the dialogue box disappears before the reading
        delay(PREDICTION_DELAY)
        val random = access.random
        val prediction =
            when (random.of(6)) {
                0 ->
                    "The enemy called ${random.pick(LUCKY_ENEMIES)} is your lucky totem this day."
                1 ->
                    "The stars tell me that you should use a ${random.pick(LUCKY_WEAPONS)} in " +
                        "combat today."
                2 ->
                    "The place called ${random.pick(LUCKY_PLACES)} will be worth your time to " +
                        "visit."
                3 -> "You would be wise to train the skill ${random.pick(LUCKY_SKILLS)} today."
                4 -> "The colour ${random.pick(LUCKY_COLOURS)} will bring you luck this day."
                else -> "You will find luck today with the number ${random.pick(LUCKY_NUMBERS)}."
            }
        chatNpc(neutral, "Here is your prediction: $prediction")
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
        const val PREDICTION_DELAY = 3

        val LUCKY_ENEMIES =
            listOf(
                "fire giant",
                "ghost",
                "goblin",
                "green dragon",
                "hobgoblin",
                "lesser demon",
                "moss giant",
                "ogre",
                "skeleton",
                "zombie",
            )
        val LUCKY_WEAPONS =
            listOf(
                "battleaxe",
                "crossbow",
                "dagger",
                "javelin",
                "long sword",
                "mace",
                "scimitar",
                "short sword",
                "spear",
                "warhammer",
            )
        val LUCKY_PLACES =
            listOf(
                "Al Kharid",
                "Ardougne",
                "Burthorpe",
                "Canifis",
                "Falador",
                "Karamja",
                "Varrock",
                "The Wilderness",
                "Yanille",
            )
        val LUCKY_SKILLS =
            listOf(
                "Agility",
                "Cooking",
                "Crafting",
                "Fishing",
                "Fletching",
                "Herblore",
                "Mining",
                "Runecrafting",
                "Smithing",
                "Thieving",
            )
        val LUCKY_COLOURS =
            listOf(
                "black",
                "blue",
                "brown",
                "cyan",
                "green",
                "pink",
                "purple",
                "red",
                "white",
                "yellow",
            )
        val LUCKY_NUMBERS =
            listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
        val SKELETAL_PIECES =
            listOf(
                "obj.dagganoth_mage_head_part",
                "obj.dagganoth_mage_body_part",
                "obj.dagganoth_mage_leg_part",
            )

        val SPINED_HIDES =
            listOf(
                "obj.dagganoth_range_head_part",
                "obj.dagganoth_range_body_part",
                "obj.dagganoth_range_leg_part",
            )
    }
}
