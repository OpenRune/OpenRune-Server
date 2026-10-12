package org.rsmod.content.areas.zeah.lovakengj.npcs

import dev.openrune.types.MesAnimType
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LovakengjTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.lovakengj_assistant") { startDialogue(it.npc) { jorra() } }
        onOpNpc1("npc.vulcana_lovakengj") { startDialogue(it.npc) { ladyVulcana() } }
        onOpNpc1("npc.lovakengj_charcoal_mother") { startDialogue(it.npc) { moggy() } }
        onOpNpc1("npc.lovakengj_blast_oreguard") { startDialogue(it.npc) { operator() } }
        onOpNpc1("npc.lovakengj_blast_hopperguard") { startDialogue(it.npc) { operator() } }
        onOpNpc1("npc.lova_tinker") { startDialogue(it.npc) { oldDwarf() } }
        onOpNpc1("npc.lovakengj_drunkard_boss") { startDialogue(it.npc) { drunkenStoryteller() } }
        for (listener in DEEPER_LODE_LISTENERS) {
            onOpNpc1(listener) { startDialogue(it.npc) { drunkenListener() } }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jorra
    private suspend fun Dialogue.jorra() {
        chatNpc(neutral, "Can't stop to talk human. Lady Lovakengj might need me.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lady_Vulcana_Lovakengj
    // TODO(content): [quest:the_forsaken_tower] start offer for players who meet the
    //  requirements
    // TODO(content): [quest:a_kingdom_divided] "Apparently you need a reward or something? Here you
    //  go." for players who lost the memoir page
    private suspend fun Dialogue.ladyVulcana() {
        if (QuestRequirements.hasCompleted(player, FORSAKEN_TOWER)) {
            ladyVulcanaAfterQuest()
            return
        }
        unkar(neutral, "...and he says that it's of the greatest urgency.")
        chatNpc(
            angry,
            "Greatest urgency? I don't like urgency. This is quite simply not on.",
        )
        chatPlayer(happy, "Hiya!")
        chatNpc(confused, "Who is this human, Councillor Unkar? What are they doing here?")
        unkar(neutral, "I'm afraid I don't know.")
        chatNpc(neutral, "Oh. Could you make them go away?")
        unkar(
            neutral,
            "Sorry human. We're currently engaging in important official business. You'll need " +
                "to come back another time.",
        )
    }

    private suspend fun Dialogue.ladyVulcanaAfterQuest() {
        chatNpc(happy, "Oh look, it's the human. Hello human.")
        while (true) {
            val option =
                choice4(
                    "Tell me about yourself.",
                    1,
                    "Tell me about Lovakengj.",
                    2,
                    "Tell me about your plans.",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(confused, "Myself? Oh.")
                    chatNpc(
                        neutral,
                        "Well I'm Vulcana Lovakengj, the head of Lovakengj House. I rule the " +
                            "city of Lovakengj.",
                    )
                    chatPlayer(happy, "Cool.")
                    chatPlayer(neutral, "...")
                    chatPlayer(quiz, "Anything else?")
                    chatNpc(confused, "Oh... No I don't think so.")
                    chatPlayer(confused, "Huh, fair enough.")
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about Lovakengj.")
                    chatNpc(confused, "Lovakengj?")
                    chatNpc(happy, "Yes! I know a lot about Lovakengj!")
                    chatNpc(
                        neutral,
                        "Lovakengj is a city of dwarves. It's the only non-human city in Kourend.",
                    )
                    chatNpc(
                        neutral,
                        "Lovakengj is very important you know. The resources mined here are used " +
                            "throughout the kingdom. Weapons, armour, tools, buildings...",
                    )
                    chatNpc(neutral, "All sorts of things come out of Lovakengj.")
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about your plans.")
                    chatNpc(confused, "My plans? Hmm let me see now...")
                    chatNpc(happy, "Ah, I know. I'll have a sherry.")
                    chatNpc(neutral, "Jorra! Bring me a sherry.")
                    chatNpcSpecific("Jorra", JORRA, neutral, "Yes Lady Lovakengj.")
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "Goodbye human.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.unkar(mesanim: MesAnimType, text: String) {
        chatNpcSpecific("Councillor Unkar", UNKAR, mesanim, text)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Moggy
    private suspend fun Dialogue.moggy() {
        val young = if (access.isBodyTypeA()) "man" else "lady"
        chatNpc(happy, "Why, what brings you to my house, young $young?")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "Have you got anything to trade?",
                    2,
                    "I'm just wandering around.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "Me? Well, let's see... I live here. So I suppose I'm your host, as long " +
                            "as you're in my house.",
                    )
                    chatNpc(
                        happy,
                        "I used to work down at the beach, running the charcoal furnaces, but " +
                            "I've handed all that business to my son now. He's a good boy.",
                    )
                    chatNpc(
                        neutral,
                        "Anyway, retiring isn't too bad. There's plenty of warmth from the lava " +
                            "pits, and my boy helps me carry my shopping from Munty's shop. I'll " +
                            "be alright.",
                    )
                    chatNpc(
                        quiz,
                        "But you've never told me what you're doing in my house, young $young. " +
                            "Don't think I've forgotten!",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Have you got anything to trade?")
                    val seller = if (access.isBodyTypeA()) "salesman" else "saleswoman"
                    chatNpc(
                        worried,
                        "Oh, are you a door-to-door $seller? My boy said I shouldn't let people " +
                            "like you into the house, and to call the guards if you bothered me.",
                    )
                    chatPlayer(neutral, "That won't be necessary.")
                    chatNpc(quiz, "So why are you in my house anyway?")
                }
                else -> {
                    chatPlayer(neutral, "I'm just wandering around.")
                    chatNpc(neutral, "Be a dear and just wander off now.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Operator
    // TODO(content): [system:blast_mine] collecting cleaned ore
    private suspend fun Dialogue.operator() {
        chatNpc(neutral, "How can I help you, human?")
        var option =
            choice5(
                "Have you any ore for me?",
                1,
                "How do I mine in this place?",
                2,
                "Who are you?",
                3,
                "What is this machine?",
                4,
                "I'll be off.",
                5,
            )
        while (true) {
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Have you any ore for me?")
                    chatNpc(neutral, "None of the ore here is yours, sorry.")
                    return
                }
                2 -> {
                    howToMine()
                    option =
                        choice3("Who are you?", 3, "What is this machine?", 4, "I'll be off.", 5)
                }
                3 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        neutral,
                        "I work here, in the blast mine. It's a valuable source of minerals for " +
                            "the kingdom, so our work is very important.",
                    )
                    option =
                        choice3(
                            "How do I mine in this place?",
                            2,
                            "What is this machine?",
                            4,
                            "I'll be off.",
                            5,
                        )
                }
                4 -> {
                    chatPlayer(quiz, "What is this machine?")
                    chatNpc(
                        neutral,
                        "The ore from the blast mines is unstable, and needs washing quickly " +
                            "before it disintegrates, so the Lovakengj engineers invented this " +
                            "machine to clean it.",
                    )
                    chatNpc(
                        happy,
                        "The design's a secret; even if someone tried to copy it, they'd never " +
                            "get it quite right. I bet their machine would break down all the " +
                            "time.",
                    )
                    option =
                        choice3(
                            "How do I mine in this place?",
                            2,
                            "Who are you?",
                            3,
                            "I'll be off.",
                            5,
                        )
                }
                else -> {
                    chatPlayer(neutral, "I'll be off.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.howToMine() {
        chatPlayer(quiz, "How do I mine in this place?")
        chatNpc(neutral, "Okay, first you need dynamite. That's got three ingredients...")
        objbox(
            "obj.lovakengj_sulphur",
            "Firstly, you need volcanic sulphur. It comes from Toothy's mine, west of here.",
        )
        objbox(
            "obj.juniper_charcoal",
            "Secondly, you need juniper charcoal. Smoggy's got some charcoal furnaces on the " +
                "beach in the Hosidius area, near the juniper woods.",
        )
        objbox(
            "obj.hosidius_saltpetre",
            "Thirdly, you need saltpetre. There are some natural deposits in the Hosidius " +
                "woodlands, near the juniper trees.",
        )
        doubleobjbox(
            "obj.pot_empty",
            "obj.ball_of_wool",
            "Mix the volcanic sulphur, the juniper charcoal and the saltpetre in a pot to make " +
                "dynamite. Finally add a string as a fuse.",
        )
        chatNpc(
            neutral,
            "Once you've got dynamite, use a chisel to excavate a cavity in the hard rock walls, " +
                "so you can put the dynamite in, then light the fuse.",
        )
        chatNpc(
            neutral,
            "The dynamite blows ore out of the hard rock. It's very unstable, so you don't have " +
                "long to get it cleaned before it disintegrates.",
        )
        chatNpc(
            neutral,
            "Just deposit it into the sack upstream, and my colleague will get it cleaned " +
                "safely. You can collect your cleaned ore from me.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Old_dwarf
    // TODO(content): [quest:a_kingdom_divided] Kharedst's memoirs page conversation and items
    //  used on him
    private suspend fun Dialogue.oldDwarf() {
        chatPlayer(happy, "Alright?")
        chatNpc(sad, "I've been better...")
        while (true) {
            when (choice3("What happened to your foot?", 1, "Who are you?", 2, "Bye.", 3)) {
                1 -> foot()
                2 -> tinker()
                else -> {
                    chatPlayer(neutral, "Bye.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.foot() {
        chatPlayer(quiz, "What happened to your foot?")
        chatNpc(
            neutral,
            "I was minding my own business in that abandoned shack for the night and suddenly I " +
                "could hear...",
        )
        chatPlayer(shocked, "A g-g-g-ghost?!")
        chatNpc(angry, "No... Don't interrupt!")
        chatNpc(confused, "Where was I?")
        chatNpc(neutral, "That was it... a faint sound... wind chimes...")
        chatPlayer(confused, "Please don't tell me you were attacked by the wind.")
        chatNpc(angry, "What did I say about interruptions?")
        chatPlayer(sad, "Sorry.")
        chatNpc(
            worried,
            "The chiming grew louder and started to sound like the knocking of bones. I was in " +
                "the bed hiding under the moth-eaten sheet - ignorance is bliss, y'know? - and " +
                "suddenly the sheet was pulled off of me and there was",
        )
        chatNpc(worried, "a-")
        chatPlayer(shocked, "A skeleton!")
        chatNpc(
            angry,
            "I SAID NO INTER- Oh yes, that's correct... Skeletons. Two of the blighters.",
        )
        chatPlayer(happy, "Well, you seem to have got away in one piece!")
        chatNpc(angry, "One piece?! Do you not-")
        chatNpc(verymad, "THE BONEY BLIGHTER BIT MY FOOT!")
        chatPlayer(
            neutral,
            "Sorry... You didn't leave anything behind did you? I'm a brave adventurer! I'm sure " +
                "I can pry whatever it is from their malicious mitts.",
        )
        chatNpc(neutral, "A few pots, but without my tools they won'y do me any good.")
    }

    private suspend fun Dialogue.tinker() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(neutral, "Me? Just a tinker with nothing to trade.")
        chatPlayer(quiz, "What happened to your wares?")
        chatNpc(
            angry,
            "A tinker doesn't get the respect that they used to... Gah! A thief took my bag in " +
                "the night, they took everything.",
        )
        chatPlayer(
            sad,
            "Sorry to hear you've had some bad luck of late, did you lose much?",
        )
        chatNpc(
            neutral,
            "Bits and pieces, nothing too important I s'pose... An old pan flute, a dirty book " +
                "and a charred skull.",
        )
        chatPlayer(neutral, "Hmm.")
        chatNpc(
            sad,
            "That's all in the past now, anyway... I'm afraid I can't help you.",
        )
        chatPlayer(neutral, "That's alright, I need no help.")
        chatNpc(quiz, "Didn't someone send you for the pots?")
        chatPlayer(neutral, "Nope, just exploring...")
        chatNpc(neutral, "Well, if anyone asks... I'm dead!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Miner_(The_Deeper_Lode)
    // TODO(content): [system:npc_overhead] storyteller and listener overhead lines
    private suspend fun Dialogue.drunkenStoryteller() {
        chatNpc(
            drunk,
            "Exchuse me... I am trying to tell my verrry good friends 'bout my uncle's bushy " +
                "beard.",
        )
    }

    private suspend fun Dialogue.drunkenListener() {
        chatNpc(drunk, "Exchuse me... I am lishning to my good friend.")
    }

    private companion object {
        const val UNKAR = "npc.councillor_unkar"
        const val JORRA = "npc.lovakengj_assistant"
        const val FORSAKEN_TOWER = "quest_forsakentower"
        val DEEPER_LODE_LISTENERS =
            listOf(
                "npc.lovakengj_drunkard_sub_1",
                "npc.lovakengj_drunkard_sub_2",
                "npc.lovakengj_drunkard_sub_3",
            )
    }
}
