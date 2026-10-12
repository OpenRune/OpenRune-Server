package org.rsmod.content.areas.zeah.kourend.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:item_on_npc] Kharedst's memoirs dialogue for Alyssa and the grave digger
class KourendGapNpcs
@Inject
constructor(private val shops: Shops, private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.arcquest_maid") { startDialogue(it.npc) { alyssa() } }
        onOpNpc1(BAKER) { startDialogue(it.npc) { baker(it.npc) } }
        onOpNpc3(BAKER) { player.openBakersStall(it.npc) }
        onOpNpc1("npc.gem_merchant") { startDialogue(it.npc) { gemMerchant() } }
        onOpNpc1(SILK_MERCHANT) { startDialogue(it.npc) { silkMerchant() } }
        onOpNpc1("npc.cata_adventurer1") { startDialogue(it.npc) { kineer() } }
        onOpNpc1("npc.cata_wanderer") { startDialogue(it.npc) { lillia() } }
        onOpNpc1("npc.dt2_lovakengj_historian") { startDialogue(it.npc) { barus() } }
        onOpNpc1("npc.lovakengj_assistant_vis") { startDialogue(it.npc) { jorra() } }
        onOpNpc1("npc.shayzien_bosstent_engineer") { startDialogue(it.npc) { kilian() } }
        onOpNpc1("npc.shayzien_bosstent_logistics") { startDialogue(it.npc) { babacus() } }
        onOpNpc1("npc.shayzien_bosstent_strategist") { startDialogue(it.npc) { salara() } }
        onOpNpc1("npc.shayzien_crime_controller") { startDialogue(it.npc) { ginea() } }
        onOpNpc1("npc.shayzien_nurse1") { startDialogue(it.npc) { shayzienNurse() } }
        onOpNpc1("npc.shayzien_nurse2") { startDialogue(it.npc) { shayzienNurse() } }
        onOpNpc1("npc.shayzien_nurse_info") { startDialogue(it.npc) { shayzienNurse() } }
        onOpNpc1("npc.shayzien_pub_patron_1") { startDialogue(it.npc) { shayzienFarmer() } }
        onOpNpc1("npc.shayzienquest_gravedigger") { startDialogue(it.npc) { graveDigger() } }
        onOpNpc1("npc.arceuus_darkwizard_reading") { startDialogue(it.npc) { darkWizard() } }
    }

    private fun Player.openBakersStall(npc: Npc) {
        shops.open(this, npc, "Kourend Castle Baker's Stall.", "inv.bakery3")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Alyssa
    private suspend fun Dialogue.alyssa() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            angry,
            "Hello, if you've something important to tell me, be out with it! I'm a busy woman.",
        )
        val option = choice3("Just making small talk.", 1, "Who are you?", 2, "Bye.", 3)
        when (option) {
            1 -> {
                chatPlayer(happy, "Just making small talk.")
                chatNpc(
                    neutral,
                    "How very nice of you. However, I'm sure there are less busy people who'd " +
                        "love to chat.",
                )
                chatPlayer(sad, "Oh, okay.")
                chatNpc(happy, "Chin up love and go adventure, that's what you lot do right?")
                chatPlayer(happy, "I suppose we do, bye!")
            }
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    angry,
                    "I'm Alyssa the housekeeper and this castle needs constant attention! I've " +
                        "enough trouble with the guards messing up my carpets without you adding " +
                        "to the problem.",
                )
                chatPlayer(
                    quiz,
                    "Hmm, yeah sorry about that. Speaking of carpets, ever thought how strange " +
                        "that there are carpets beneath the stairs?",
                )
                chatNpc(
                    angry,
                    "It's cut to fit! If I had a gold piece for everytime I was asked that " +
                        "question!",
                )
                chatPlayer(shifty, "Sure... Cut to fit... Not an oversight at all.")
                chatNpc(angry, "Alright, believe what you like. I've a castle to clean.")
            }
            3 -> chatPlayer(neutral, "Bye.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Baker
    // TODO(content): [skill:thieving] overhead lines after stealing from the stall
    private suspend fun Dialogue.baker(npc: Npc) {
        val title = if (access.isBodyTypeA()) "Sir" else "Madame"
        chatNpc(
            happy,
            "Good day $title. Would you like some nice freshly baked bread? Or perhaps a nice " +
                "piece of cake?",
        )
        if (choice2("Let's see what you have.", true, "No thank you.", false)) {
            player.openBakersStall(npc)
            return
        }
        chatPlayer(neutral, "No thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gem_merchant_(Kourend_Castle)
    // TODO(content): [system:shops] no server inv data for the Kourend Castle Gem Stall
    //  (inv.kourendgemstall; also no Trade op binding)
    // TODO(content): [skill:thieving] overhead lines after stealing from the stall
    private suspend fun Dialogue.gemMerchant() {
        chatNpc(happy, "Here, look at my lovely gems.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Silk_merchant
    // TODO(content): [skill:thieving] overhead lines and refusing silk after a theft
    private suspend fun Dialogue.silkMerchant() {
        if (count(SILK) > 0) {
            haggle()
            return
        }
        chatNpc(neutral, "I buy silk. If you ever want to sell any silk, bring it here.")
        val noted = count(NOTED_SILK)
        if (noted == 0) {
            return
        }
        chatPlayer(quiz, "I've got some silk here. Will you buy it?")
        chatNpc(confused, "Silk? You're not carrying any silk.")
        chatPlayer(neutral, "Yes I am.")
        chatNpc(
            neutral,
            "No, you're carrying $noted bits of paper that say they can be exchanged for silk at " +
                "a bank or general store. I'm not buying those. Fetch me some real silk, then " +
                "we'll trade.",
        )
        chatPlayer(sad, "Oh, alright.")
    }

    private suspend fun Dialogue.haggle() {
        chatPlayer(happy, "Hello. I have some fine silk from far away to sell to you.")
        chatNpc(
            neutral,
            "Ah I may be interested in that. What sort of price were you looking at per piece " +
                "of silk?",
        )
        when (choice4("20 coins.", 20, "80 coins.", 80, "120 coins.", 120, "200 coins.", 200)) {
            20 -> {
                chatPlayer(neutral, "20 coins.")
                chatNpc(happy, "Ok suits me.")
                sellSilk(20)
            }
            80 -> {
                chatPlayer(neutral, "80 coins.")
                chatNpc(shocked, "80 coins? That's a bit steep! How about 40 coins?")
                val price =
                    choice3(
                        "Ok, 40 sounds good.",
                        40,
                        "50, and that's my final price.",
                        50,
                        "No, that is not enough.",
                        0,
                    )
                when (price) {
                    40 -> {
                        chatPlayer(happy, "Ok, 40 sounds good.")
                        sellSilk(40)
                    }
                    50 -> {
                        chatPlayer(neutral, "50, and that's my final price.")
                        chatNpc(neutral, "Done.")
                        sellSilk(50)
                    }
                    else -> chatPlayer(angry, "No, that is not enough.")
                }
            }
            120 -> {
                chatPlayer(neutral, "120 coins.")
                chatNpc(
                    neutral,
                    "You'll never get that much for it. I'll be generous and give you 50 for it.",
                )
                val price =
                    choice3(
                        "Ok, I guess 50 will do.",
                        50,
                        "I'll give it to you for 60.",
                        60,
                        "No, that is not enough.",
                        0,
                    )
                when (price) {
                    50 -> {
                        chatPlayer(neutral, "Ok, I guess 50 will do.")
                        sellSilk(50)
                    }
                    60 -> {
                        chatPlayer(neutral, "I'll give it to you for 60.")
                        chatNpc(
                            neutral,
                            "You drive a hard bargain, but I guess that will have to do.",
                        )
                        sellSilk(60)
                    }
                    else -> chatPlayer(angry, "No, that is not enough.")
                }
            }
            200 -> {
                chatPlayer(neutral, "200 coins.")
                chatNpc(
                    angry,
                    "Don't be ridiculous! That is far too much. You insult me with that price.",
                )
            }
        }
    }

    private fun Dialogue.sellSilk(price: Int) {
        val silk = count(SILK)
        if (silk == 0) {
            return
        }
        val taken = player.invDel(player.inv, SILK, count = silk, ignoreVirtualStorage = true)
        if (!taken.success) {
            return
        }
        player.invAddOrDrop(objRepo, COINS, silk * price)
    }

    private fun Dialogue.count(obj: String): Int = player.inv.physicalCount(obj)

    // https://oldschool.runescape.wiki/w/Transcript:Kineer
    private suspend fun Dialogue.kineer() {
        chatNpc(confused, "It was the bewk that told me ta come here.")
        chatPlayer(quiz, "What book?")
        chatNpc(confused, "Circles within trees it is. And hopping too.")
        chatPlayer(worried, "Are you ok?")
        chatNpc(confused, "I'll find the light, that will see me through.")
        chatPlayer(confused, "Oookay then, I'll just go explore then.")
        chatNpc(confused, "Jelly shards in the dark.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lillia
    private suspend fun Dialogue.lillia() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Greetings. No I'm not.")
        chatPlayer(quiz, "Are you lost?")
        chatNpc(neutral, "Possibly.")
        chatPlayer(confused, "Hang on, did you just answer my question before I asked it?")
        chatNpc(shifty, "Absolutely not!")
        chatPlayer(confused, "Are you a little... mad?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barus
    private suspend fun Dialogue.barus() {
        chatNpc(happy, "Hello again! Lovely day for it, isn't it?")
        chatPlayer(neutral, "I suppose it is.")
        chatNpc(happy, "Nothing like a good day in Lovakengj, that's what I always say!")
        chatPlayer(neutral, "Well I hope you keep enjoying it.")
        chatNpc(happy, "I'm sure I will!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jorra
    private suspend fun Dialogue.jorra() {
        chatNpc(neutral, "Can't stop to talk human. Lady Lovakengj might need me.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:General_Kilian
    private suspend fun Dialogue.kilian() {
        chatNpc(neutral, "What brings you here?")
        val curious =
            choice2(
                "Curiosity. What are you doing?",
                true,
                "Just wandering around, really.",
                false,
            )
        if (curious) {
            chatPlayer(quiz, "Curiosity. What are you doing?")
            chatNpc(
                neutral,
                "I'm just leafing through some designs, waiting for the others to decide what's " +
                    "needed. It's taking them a while, but there's sure to be some engineering " +
                    "involved in whatever they decide.",
            )
            chatPlayer(neutral, "Okay, thanks.")
            return
        }
        chatPlayer(neutral, "Just wandering around, really.")
        chatNpc(
            angry,
            "You do realise this is a military installation, right? Tourists are generally not " +
                "welcome.",
        )
        chatPlayer(worried, "Oh, right.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:General_Babacus
    private suspend fun Dialogue.babacus() {
        chatNpc(angry, "Please do not disturb our deliberations.")
        if (!choice2("What are you deliberating?", true, "Oh, sorry.", false)) {
            chatPlayer(worried, "Oh, sorry.")
            return
        }
        chatPlayer(quiz, "What are you deliberating?")
        chatNpc(
            neutral,
            "Lord Shayzien wishes us to draw up contingency plans for various scenarios in the " +
                "ongoing war against the lizardmen.",
        )
        chatNpc(
            neutral,
            "If they ever succeed in cutting us off from Lovakengj, our supply lines will be " +
                "severely constrained, and I must work on alternative plans.",
        )
        chatNpc(angry, "So please do not disturb us further.")
        chatPlayer(neutral, "I'll leave you to it.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:General_Salara
    private suspend fun Dialogue.salara() {
        chatNpc(angry, "As you can see, we are busy. What do you want?")
        if (choice2("What is all this paper for?", true, "I don't want anything.", false)) {
            chatPlayer(quiz, "What is all this paper for?")
            chatNpc(
                neutral,
                "This paper, as you call it, contains strategies for use in the war against the " +
                    "lizardmen. The details are a military secret, so do not ask any more " +
                    "questions.",
            )
            chatPlayer(neutral, "Fair enough.")
            return
        }
        chatPlayer(neutral, "I don't want anything.")
        chatNpc(angry, "Then be off with you!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Ginea (initial dialogue)
    // TODO(content): [system:met_flags] subsequent dialogue after offering to help
    // TODO(content): [system:shayzien_gang_meetings] meeting reports and intelligence hand-ins
    private suspend fun Dialogue.ginea() {
        chatNpc(neutral, "I'm busy here. Do you need something?")
        val ask =
            choice2(
                "Please tell me about what you're doing.",
                true,
                "You sound busy. I'll be off.",
                false,
            )
        if (!ask) {
            chatPlayer(neutral, "You sound busy. I'll be off.")
            return
        }
        chatPlayer(quiz, "Please tell me about what you're doing.")
        chatNpc(
            angry,
            "Gangs. It's the gangs from the slums in the east. Those ruffians are always " +
                "hatching schemes, whether it's smuggling or extortion or even plain street " +
                "violence.",
        )
        chatNpc(
            neutral,
            "I'm in charge of the investigations unit. Sometimes an informant will let us know " +
                "when a gang is meeting. Then we can send in the troops and get them.",
        )
        if (!choice2("Can I help?", true, "You sound busy. I'll be off.", false)) {
            chatPlayer(neutral, "You sound busy. I'll be off.")
            return
        }
        chatPlayer(quiz, "Can I help?")
        chatNpc(
            happy,
            "That would be very welcome. Information about gang meetings is written on the " +
                "notice board when it's received. When you see a meeting announced there, go and " +
                "deal with 'em.",
        )
        chatNpc(neutral, "If they drop any documents, bring them to me for an extra reward.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nurse_Wooned
    // https://oldschool.runescape.wiki/w/Transcript:Nurse_Inn%27Juree
    // https://oldschool.runescape.wiki/w/Transcript:Nurse_Boubou
    // TODO(content): [system:treasure_trails] elite challenge scroll from Nurse Wooned
    private suspend fun Dialogue.shayzienNurse() {
        chatNpc(
            worried,
            access.random.pick(
                "Patients, patients everywhere! We need to get these soldiers up and fighting " +
                    "again!",
                "I don't have time to chat, I have patients to attend to.",
                "I can't take much more of this. As soon as we help one patient, another comes in!",
            ),
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_(Shayzien)
    private suspend fun Dialogue.shayzienFarmer() {
        chatNpc(happy, "Hello stranger.")
        chatPlayer(
            quiz,
            "You look like a farmer. Don't see many farmers around here.",
        )
        chatNpc(
            neutral,
            "Aye, that you don't. I'm from Hosidius, y'see. I just come here to drink.",
        )
        chatPlayer(
            quiz,
            "You travel all the way out to Shayzien just for a few beers? Don't you have a pub " +
                "back home?",
        )
        chatNpc(neutral, "Indeed we do... But I can't stand the place for more than a few minutes.")
        chatNpc(
            angry,
            "Have you ever been in there? Such a rowdy crowd... people shouting across the bar " +
                "about 'Farming this', 'Farming that'... can barely hear myself think half the " +
                "time.",
        )
        chatNpc(
            neutral,
            "No, no... Can't be doin' with it. That's why I make the trip out here for a nice " +
                "quiet one.",
        )
        chatPlayer(happy, "Well I'd better leave you to it then!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grave_digger
    private suspend fun Dialogue.graveDigger() {
        if (GHOSTSPEAK.any { player.worn.count(it) > 0 }) {
            chatPlayer(quiz, "How's it going?")
            chatNpc(sad, "I'm dead.")
            chatPlayer(neutral, "Maybe you should try looking on the bright side of...")
            chatNpc(confused, "...Of?")
            chatPlayer(worried, "Oh, yeah. Nevermind.")
            chatNpc(neutral, "Ok.")
            return
        }
        chatNpc(neutral, "Wooo wooo wooooo!")
        val option =
            choice3(
                "Sorry, I don't speak ghost.",
                1,
                "Mmmm, should I be writing this down?",
                2,
                "I'll come back later.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(sad, "Sorry, I don't speak ghost.")
                chatNpc(neutral, "Woo woo?")
                chatPlayer(confused, "Nope, still don't understand you.")
                chatNpc(angry, "WOOOOOOOOO!")
                chatPlayer(neutral, "Never mind.")
            }
            2 -> {
                chatPlayer(quiz, "Mmmm, should I be writing this down?")
                chatNpc(neutral, "Woo wooo. Woooooooooooooooooo!")
            }
            3 -> {
                chatPlayer(neutral, "I'll come back later.")
                chatNpc(
                    neutral,
                    "Wooooooo woo! Wooooo woo wooooo woowoowoo woo Woo wooo. Wooooo woo woo? " +
                        "Woooooooooooooooooo!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dark_wizard_(Great_Kourend) (Arceuus library)
    private suspend fun Dialogue.darkWizard() {
        if (access.random.of(2) == 0) {
            chatPlayer(quiz, "What are you doing here?")
            chatNpc(
                neutral,
                "I'm working on combining the dark teleportation magic of these lands into " +
                    "physical form.",
            )
            chatPlayer(quiz, "Kind of like teleportation tablets?")
            chatNpc(neutral, "Yes, but not made quite the same way.")
            chatPlayer(quiz, "Can I do that?")
            chatNpc(
                neutral,
                "If you must. That lectern over there contains the book I've been working on.",
            )
            return
        }
        chatPlayer(quiz, "What are you reading?")
        chatNpc(
            angry,
            "I am seeking to understand more about the dark powers present in these lands. Now " +
                "leave me be, there is much to be learned.",
        )
    }

    private companion object {
        const val BAKER = "npc.baker_merchant"
        const val SILK_MERCHANT = "npc.silk_merchant"
        const val SILK = "obj.silk"
        const val NOTED_SILK = "obj.cert_silk"
        const val COINS = "obj.coins"

        val GHOSTSPEAK = listOf("obj.amulet_of_ghostspeak", "obj.amulet_of_ghostspeak_enchanted")
    }
}
