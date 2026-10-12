package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class HosidiusTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_historian") { startDialogue(it.npc) { jonathan() } }
        onOpNpc1("npc.hosidius_private_gardener") { startDialogue(it.npc) { farmerHayfield() } }
        onOpNpc1("npc.hosidius_mess_jon") { startDialogue(it.npc) { jon() } }
        onOpNpc1("npc.hosidius_cleric") { startDialogue(it.npc) { fatherJean() } }
        onOpNpc1("npc.hosidius_chief_farmer") { startDialogue(it.npc) { dale() } }
        onOpNpc1("npc.hosidius_citykitchen_chef") { startDialogue(it.npc) { ramocean() } }
        onOpNpc1("npc.hosidius_servery_chef") { startDialogue(it.npc) { ewesey() } }
        onOpNpc1("npc.hosidius_vintner") { startDialogue(it.npc) { gallow() } }
        onOpNpc1("npc.pmod_town_crier_zeah") { startDialogue(it.npc) { townCrier() } }
        onOpNpc1("npc.con_contractor_hosidius") { startDialogue(it.npc) { angelo() } }
        onOpNpc1("npc.con_contract_client_9") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_10") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_11") { startDialogue(it.npc) { mahoganyClient() } }
        for (storyteller in GOLDEN_FIELD_STORYTELLERS) {
            onOpNpc1(storyteller) { startDialogue(it.npc) { drunkenStoryteller() } }
        }
        for (listener in GOLDEN_FIELD_LISTENERS) {
            onOpNpc1(listener) { startDialogue(it.npc) { drunkenListener() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jonathan
    private suspend fun Dialogue.jonathan() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello, lovely day we're having.")
        chatPlayer(neutral, "Same as every day really, the weather is very consistent around here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_Hayfield
    private suspend fun Dialogue.farmerHayfield() {
        mesbox("Farmer Hayfield stares at one of his flower beds.")
        chatNpc(quiz, "Are you alive?")
        chatPlayer(confused, "Me?")
        mesbox("Farmer Hayfield continues to stare at the flower bed.")
        chatNpc(quiz, "Tell me! How does it feel to be alive?")
        chatPlayer(confused, "Huh, what a strange guy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jon
    private suspend fun Dialogue.jon() {
        chatNpc(sad, "This dish turned out horrendously.")
        chatPlayer(quiz, "How come?")
        chatNpc(sad, "It... tastes like nothing...")
        mesbox("Jon shows you an empty bowl.")
        chatPlayer(confused, "Isn't that bowl empty?")
        chatNpc(confused, "So nothing tastes the same cause nothing is never there?")
        chatPlayer(neutral, "I'll be going...")
        chatNpc(neutral, "That's probably for the best.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Father_Jean
    // TODO(content): [system:treasure_trails] easy emote clue reward
    private suspend fun Dialogue.fatherJean() {
        chatNpc(
            neutral,
            "Sorry, there will be no sermon today, but you may offer a prayer to Saradomin " +
                "yourself.",
        )
        val option =
            choice2(
                "Do the people here mostly worship Saradomin?",
                true,
                "That's nice.",
                false,
            )
        if (!option) {
            chatPlayer(neutral, "That's nice.")
            return
        }
        chatPlayer(quiz, "Do the people here mostly worship Saradomin?")
        chatNpc(
            happy,
            "Of course! Even before our forefathers founded Kourend, under King Rada I, they " +
                "remembered the tumult of the times when the armies of the gods laid waste to " +
                "the lands.",
        )
        chatNpc(
            neutral,
            "We thank Saradomin for the end of that violent Age, and pray that he will continue " +
                "to defend us from the evil other gods.",
        )
        chatPlayer(neutral, "Okay, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dale
    private suspend fun Dialogue.dale() {
        chatNpc(
            neutral,
            "Greetings, traveller. I'd love to stop and talk, but farming is a busy line of " +
                "work. Perhaps another time?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ramocean
    private suspend fun Dialogue.ramocean() {
        chatNpc(angry, "What do you want? I am busy supervising!")
        while (true) {
            val option =
                choice4(
                    "What is this place?",
                    1,
                    "Can I cook here?",
                    2,
                    "Why don't you do some work yourself?",
                    3,
                    "I'll leave you alone.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        neutral,
                        "This is the Hosidius Kitchen. Here, under my expert eye, we prepare the " +
                            "food that feeds most of the population.",
                    )
                    chatNpc(
                        neutral,
                        "Much of the income of Hosidius derives from trade in cooked food, so " +
                            "our work is vital.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Can I cook here?")
                    chatNpc(
                        neutral,
                        "Yes, you are most welcome to cook here. You may find you burn food less " +
                            "often under my supervision.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Why don't you do some work yourself?")
                    chatNpc(
                        angry,
                        "Excuse me? My cooking level is higher than you could imagine - I have " +
                            "already done my share of the work. Now, with my expertise, I am " +
                            "guiding future generations of chefs.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you alone.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ewesey
    // TODO(content): [system:hosidius_mess] serving, ingredient and eating warnings
    // TODO(content): [system:treasure_trails] master clue torn scroll part
    private suspend fun Dialogue.ewesey() {
        chatNpc(quiz, "Are you a cook? I need more staff.")
        while (true) {
            val option =
                choice3(
                    "What's going on here?",
                    1,
                    "Yeah, I can do some cooking.",
                    2,
                    "No, I'm off.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's going on here?")
                    chatNpc(
                        neutral,
                        "This is the Hosidius Mess. It was set up to help feed the soldiers from " +
                            "the Shayzien Encampment. Much better than anything they can cook " +
                            "over there.",
                    )
                    chatPlayer(quiz, "How come it's so far from Hosidius?")
                    chatNpc(
                        neutral,
                        "The solders do enough hard work, so it seemed only right not to make " +
                            "them walk all the way across Kourend for a bit of food.",
                    )
                    chatNpc(neutral, "Anyway, enough chatting. Are you a cook?")
                }
                2 -> {
                    val level = player.statBase("stat.cooking")
                    if (level < MESS_COOKING_LEVEL) {
                        chatPlayer(
                            neutral,
                            "Yeah, I can do some cooking. Well, my level's $level, anyway.",
                        )
                        chatNpc(
                            laugh,
                            "Hah! You'll need a level of 20 to cook anything for these soldiers. " +
                                "They're picky about what they eat.",
                        )
                        return
                    }
                    chatPlayer(happy, "Yeah, I can do some cooking.")
                    // TODO(content): [verify:in_game] the favour-met reply is assumed since Kourend
                    //  favour was removed
                    chatNpc(
                        happy,
                        "Good. The soldiers like stew, meat pies and pineapple pizza. You'll " +
                            "find ingredients in the cupboards.",
                    )
                    chatNpc(
                        neutral,
                        "When you've made something, put it on the buffet tables, and you'll " +
                            "gain experience based on how much the soldiers appreciate them.",
                    )
                    return
                }
                else -> {
                    chatPlayer(neutral, "No, I'm off.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gallow
    // TODO(content): [quest:a_kingdom_divided] after-quest "What do you do?" reply
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.gallow() {
        chatNpc(happy, "Welcome to the vinery. Can I help you?")
        while (true) {
            val option =
                choice4(
                    "How do I farm here?",
                    1,
                    "What do you do?",
                    2,
                    "Why are you called Gallow?",
                    3,
                    "I'm fine, thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "How do I farm here?")
                    chatNpc(
                        neutral,
                        "Let's see... First, you'll need to treat the soil with saltpetre. Dig " +
                            "it into the patch with a trowel. The minerals are good for the " +
                            "fruit.",
                    )
                    chatNpc(
                        neutral,
                        "Then plant a grape seed, and just let it grow. I'll look after it, so " +
                            "it won't get diseased.",
                    )
                    chatNpc(
                        neutral,
                        "When your grapes are ready, pick them and clear the patch so you can " +
                            "grow a new vine.",
                    )
                    chatNpc(
                        quiz,
                        "Does that seem clear, or would you like help with something else?",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What do you do?")
                    chatNpc(
                        neutral,
                        "I'm the vintner. I helped Lord Hosidius to develop this land as a " +
                            "vinery, and now it's complete he's put me in charge of production " +
                            "while he works on a new project.",
                    )
                    chatNpc(
                        neutral,
                        "His son, young Pandur, works here too. Pandur checks the wine I " +
                            "produce. But if you've got any questions about the farming, don't " +
                            "ask Pandur. Ask me instead. He checks the wine too much.",
                    )
                    chatNpc(quiz, "So can I help you with anything?")
                }
                3 -> {
                    chatPlayer(quiz, "Why are you called Gallow?")
                    chatNpc(
                        neutral,
                        "It's short for 'Gallows'. When I was young, a boy made some improper " +
                            "suggestions to me, so I grabbed him by his neck and held him in the " +
                            "air until he learnt some manners.",
                    )
                    chatNpc(
                        happy,
                        "Now, dear, do you need help with the vinery? I'm enjoying the chat, but " +
                            "I have work to do.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'm fine, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Town_Crier
    private suspend fun Dialogue.townCrier() {
        chatNpc(happy, "Hello citizen!")
        if (choice2("What do you do around here?", true, "See you later.", false)) {
            chatPlayer(quiz, "What do you do around here?")
            chatNpc(
                happy,
                "I'm a Town Crier. It's my job to let people know of any recent news. After all, " +
                    "there's all sorts of things happening around here.",
            )
            chatPlayer(neutral, "I see. See you later.")
        } else {
            chatPlayer(neutral, "See you later.")
        }
        chatNpc(happy, "Until next time.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Angelo
    // TODO(content): [system:mahogany_homes] contracts after joining the company
    private suspend fun Dialogue.angelo() {
        chatNpc(happy, "Hey, how can I help you?")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            happy,
            "This is one of the branches for Mahogany Homes, Gielinor's first construction " +
                "company.",
        )
        chatPlayer(happy, "Oh wow, how can I get involved?")
        chatNpc(
            happy,
            "If you go see Amy in the Falador branch, she'll fill you in. Her office is just " +
                "east of the Estate Agent in Falador.",
        )
        chatPlayer(happy, "Nice, thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barbara
    // https://oldschool.runescape.wiki/w/Transcript:Leela_(Mahogany_Homes)
    // https://oldschool.runescape.wiki/w/Transcript:Mariah
    // TODO(content): [system:mahogany_homes] contract requests and the after-contract cup of tea
    private suspend fun Dialogue.mahoganyClient() {
        chatNpc(neutral, "Please excuse me, I'm rather busy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Farmer_(The_Golden_Field)
    // TODO(content): [system:npc_overhead] storyteller and listener overhead lines
    private suspend fun Dialogue.drunkenStoryteller() {
        chatNpc(
            drunk,
            "Exchuse me... I am trying to tell my verrry good friends 'bout the biggest bull I " +
                "ever raised.",
        )
    }

    private suspend fun Dialogue.drunkenListener() {
        chatNpc(drunk, "Exchuse me... I am lishning to my good friend.")
    }

    private companion object {
        const val MESS_COOKING_LEVEL = 20
        val GOLDEN_FIELD_STORYTELLERS =
            listOf("npc.hosidius_drunken_farmer_boss_1", "npc.hosidius_drunken_farmer_boss_2")
        val GOLDEN_FIELD_LISTENERS =
            listOf(
                "npc.hosidius_drunken_farmer_1",
                "npc.hosidius_drunken_farmer_2",
                "npc.hosidius_drunken_farmer_3",
                "npc.hosidius_drunken_farmer_4",
            )
    }
}
