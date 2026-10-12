package org.rsmod.content.areas.city.rimmington.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RimmingtonTownsfolkScript
@Inject
constructor(private val objRepo: ObjRepository, private val npcRepo: NpcRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hw18_girl") { startDialogue(it.npc) { angel() } }
        onOpNpc1("npc.hw18_boy") { startDialogue(it.npc) { henja() } }
        onOpNpc1("npc.hetty") { startDialogue(it.npc) { hetty() } }
        onOpNpc1("npc.chemist") { startDialogue(it.npc) { chemist() } }
        onOpNpc1(ANJA) { startDialogue(it.npc) { anja(it.npc) } }
        onOpNpc1(HENGEL) { startDialogue(it.npc) { hengel(it.npc) } }
        onOpNpc1("npc.artist1") { startDialogue(it.npc) { daVinci() } }
        onOpNpc1("npc.gambler1") { startDialogue(it.npc) { chancy() } }
        onOpNpc1("npc.drunk1") { startDialogue(it.npc) { hops() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Angel
    // TODO(content): [event:halloween_2018] the dialogue after the event
    private suspend fun Dialogue.angel() {
        chatNpc(shifty, "We don't talk to strangers.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Henja
    // TODO(content): [event:halloween_2018] the dialogue after the event
    private suspend fun Dialogue.henja() {
        chatNpc(shifty, "Sorry, we don't talk to strangers.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hetty
    // https://oldschool.runescape.wiki/w/Transcript:Witch's_Potion
    // TODO(content): [quest:witchs_potion] "Start the Witch's Potion quest?" and the quest stages
    private suspend fun Dialogue.hetty() {
        if (QuestRequirements.hasCompleted(player, WITCHS_POTION)) {
            chatNpc(happy, "How's your magic coming along?")
            chatPlayer(happy, "I'm practicing and slowly getting better.")
            chatNpc(happy, "Good, good.")
            return
        }
        chatNpc(quiz, "What could you want with an old woman like me?")
        val quest =
            choice2("I am in search of a quest.", true, "I've heard that you are a witch.", false)
        if (!quest) {
            chatPlayer(quiz, "I've heard that you are a witch.")
            chatNpc(neutral, "Yes it does seem to be getting fairly common knowledge.")
            chatNpc(
                worried,
                "I fear I may be getting a visit from the witch hunters of Falador before long.",
            )
            if (!choice2("I am in search of a quest.", true, "Goodbye.", false)) {
                chatPlayer(neutral, "Goodbye.")
                return
            }
        }
        chatPlayer(quiz, "I am in search of a quest.")
        chatNpc(neutral, "Hmmm... Maybe I can think of something for you.")
        chatNpc(quiz, "Would you like to become more proficient in the dark arts?")
        if (choice2("Yes.", true, "No.", false, title = "Start the Witch's Potion quest?")) {
            return
        }
        chatPlayer(neutral, "No, I have my principles and honour.")
        chatNpc(neutral, "Suit yourself, but you're missing out.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chemist
    // TODO(content): [quest:biohazard] the quest dialogue
    // TODO(content): [system:treasure_trails] easy clue step
    private suspend fun Dialogue.chemist() {
        val topic =
            choice3(
                "Yes.",
                ChemistTopic.Lamps,
                "No.",
                ChemistTopic.Hello,
                "No, I'm more interested in impling jars.",
                ChemistTopic.Implings,
                title = "Do you want to talk about lamps?",
            )
        when (topic) {
            ChemistTopic.Lamps -> lampOil()
            ChemistTopic.Hello -> {
                chatPlayer(happy, "Hello.")
                chatNpc(happy, "Oh.. hello, how's it going?")
                chatPlayer(happy, "Good thanks.")
                chatNpc(neutral, "Good to hear, sorry but I have a few things to do right now.")
                chatPlayer(neutral, "Well I'd better let you get on then.")
            }
            ChemistTopic.Implings -> implingJars()
        }
    }

    private suspend fun Dialogue.lampOil() {
        chatPlayer(quiz, "Hi, I need fuel for a lamp.")
        chatNpc(happy, "Hello there, the fuel you need is lamp oil, do you need help making it?")
        if (!choice2("Yes please.", true, "No thanks.", false)) {
            chatPlayer(neutral, "No thanks.")
            return
        }
        chatPlayer(happy, "Yes please.")
        chatNpc(
            neutral,
            "It's really quite simple.  You use the small still in here.  It's all set up, so " +
                "there's no fiddling around with dials...",
        )
        chatNpc(
            neutral,
            "Just put ordinary swamp tar in, and then use a lantern or lamp to get the oil out.",
        )
        chatPlayer(happy, "Thanks.")
    }

    private suspend fun Dialogue.implingJars() {
        chatPlayer(quiz, "I have a slightly odd question.")
        chatNpc(happy, "Jolly good, the odder the better. I like oddities.")
        chatPlayer(
            quiz,
            "Do you know how I might distil a mix of anchovy oil and flowers so that it forms a " +
                "layer on the inside of a butterfly jar?",
        )
        chatNpc(
            quiz,
            "That is an odd question. I commend you for it. Why would you want to do that?",
        )
        chatPlayer(
            neutral,
            "Apparently, if I can make a jar like this it will be useful for storing implings in.",
        )
        chatNpc(
            quiz,
            "So, do you have any of this fish-and-flower flavoured oil and a butterfly jar then?",
        )
        if (IMP_REPELLENT in player.inv && BUTTERFLY_JAR in player.inv) {
            chatPlayer(happy, "As it happens, I do.")
            chatNpc(
                neutral,
                "Well then. Hmmm. My lamp oil still may be able to do what you want. Give it a " +
                    "go and see if it works.",
            )
            chatPlayer(happy, "Thanks!")
            return
        }
        chatPlayer(neutral, "Actually, no.")
        chatNpc(
            neutral,
            "If you go and get them then I may be able to help you. I'm better at coming up with " +
                "answers if the questions are in my hands.",
        )
        while (true) {
            val topic =
                choice3(
                    "So how do you make anchovy oil?",
                    ImplingTopic.AnchovyOil,
                    "Do you have a sieve I can use?",
                    ImplingTopic.Sieve,
                    "I'd better go and get the repellent.",
                    ImplingTopic.Leave,
                    title = "Is there anything else you want to ask?",
                )
            when (topic) {
                ImplingTopic.AnchovyOil -> {
                    chatPlayer(quiz, "So, how do you make anchovy oil?")
                    chatNpc(
                        neutral,
                        "Anchovies are pretty oily fish. I'd have thought you could just grind " +
                            "them up and sieve out the bits. You'd probably want to remove any " +
                            "water first - Cooking should do that pretty well.",
                    )
                }
                ImplingTopic.Sieve -> return sieve()
                ImplingTopic.Leave -> {
                    chatPlayer(neutral, "I'd better go and get the repellent.")
                    chatPlayer(neutral, "I think so.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.sieve() {
        chatPlayer(quiz, "Do you have a sieve I can use?")
        if (SIEVE in player.inv) {
            chatNpc(
                confused,
                "Errm, yes. But you already have one. Two sieves is a bit excessive, don't you " +
                    "think?",
            )
            return
        }
        if (SIEVE in access.bank) {
            chatNpc(quiz, "Are you sure you don't already have one?")
            objbox(SIEVE, "You already have a sieve that will do the job in your bank.")
            return
        }
        player.invAddOrDrop(objRepo, SIEVE)
        chatNpc(
            neutral,
            "Errm, yes. Here, have this one. It's only been used for sieving dead rats out of " +
                "sewer water.",
        )
        chatPlayer(confused, "Err, why? Actually, on second thoughts I don't want to know.")
        chatNpc(neutral, "Well, it should be ideally suited to your task.")
        chatPlayer(neutral, "Fair enough.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anja
    private suspend fun Dialogue.anja(npc: Npc) {
        chatPlayer(happy, "Hello.")
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(quiz, "Hello $title. What are you doing in my house?")
        when (householderOptions()) {
            1 -> {
                chatPlayer(neutral, "I'm just wandering around.")
                chatNpc(quiz, "Oh dear, are you lost?")
                if (choice2("Yes, I'm lost.", true, "No, I know where I am.", false)) {
                    chatPlayer(sad, "Yes, I'm lost.")
                    chatNpc(
                        neutral,
                        "Okay, just walk north-east when you leave this house, and soon you'll " +
                            "reach the big city of Falador.",
                    )
                    chatPlayer(happy, "Thanks a lot.")
                    return
                }
                chatPlayer(neutral, "No, I know where I am.")
                chatNpc(
                    angry,
                    "Oh? Well, would you mind wandering somewhere else? This is my house.",
                )
                chatPlayer(angry, "Meh!")
            }
            2 -> beg()
            else -> scream(npc)
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hengel
    private suspend fun Dialogue.hengel(npc: Npc) {
        chatPlayer(happy, "Hello.")
        chatNpc(quiz, "What are you doing here?")
        when (householderOptions()) {
            1 -> {
                chatPlayer(neutral, "I'm just wandering around.")
                chatNpc(quiz, "You do realise you're wandering around in my house?")
                chatPlayer(neutral, "Yep.")
                chatNpc(angry, "Well please get out!")
                chatPlayer(angry, "Sheesh, keep your wig on!")
            }
            2 -> {
                chatPlayer(happy, "I was hoping you'd give me some free stuff.")
                chatNpc(angry, "No, I jolly well wouldn't! Get out of my house!")
                chatPlayer(angry, "Meanie!")
            }
            else -> scream(npc)
        }
    }

    private suspend fun Dialogue.householderOptions(): Int =
        choice3(
            "I'm just wandering around.",
            1,
            "I was hoping you'd give me some free stuff.",
            2,
            "I've come to kill you.",
            3,
        )

    private fun scream(npc: Npc) {
        val partnerType = if (npc.isType(ANJA)) HENGEL else ANJA
        val partner =
            npcRepo.findAll(ZoneKey.from(npc.coords), zoneRadius = 1).firstOrNull {
                it.isType(partnerType)
            }
        for (resident in listOfNotNull(npc, partner)) {
            resident.say(if (resident.isType(ANJA)) "Eeeek!" else "Aaaarrgh!")
        }
    }

    private suspend fun Dialogue.beg() {
        chatPlayer(happy, "I was hoping you'd give me some free stuff.")
        val reluctance =
            listOf(
                "Do you REALLY need it?",
                "Er...",
                "I don't have much on me...",
                "I don't know...",
            )
        chatNpc(confused, reluctance[access.random.of(reluctance.size)])
        val pleading =
            listOf(
                "<col=800000>(beg beg beg beg beg beg)</col>",
                "I promise I'll stop bothering you!",
                "Pleeease!",
                "Pwetty pleathe wiv thugar on top!",
            )
        chatPlayer(sad, pleading[access.random.of(pleading.size)])
        chatNpc(neutral, "Oh, alright. Here you go.")
        player.invAddOrDrop(objRepo, "obj.coins", access.random.of(1, 3))
    }

    // https://oldschool.runescape.wiki/w/Transcript:Da_Vinci
    // TODO(content): [quest:biohazard] the quest dialogue and moving to Varrock after it
    private suspend fun Dialogue.daVinci() {
        chatNpc(
            angry,
            "Bah! A great artist such as myself should not have to suffer the HUMILIATION of " +
                "spending time where the likes of you wander everywhere!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chancy (Rimmington, members world)
    // TODO(content): [quest:biohazard] the quest dialogue and moving to Varrock after it
    private suspend fun Dialogue.chancy() {
        chatPlayer(happy, "Hello! Playing solitaire?")
        chatNpc(
            shifty,
            "Hush - I'm trying to perfect the art of dealing off the bottom of the deck.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Biohazard (Hops in Rimmington)
    // TODO(content): [quest:biohazard] the quest dialogue and moving to Varrock after it
    private suspend fun Dialogue.hops() {
        chatNpc(drunk, "Hops don't wanna talk now.")
    }

    private enum class ChemistTopic {
        Lamps,
        Hello,
        Implings,
    }

    private enum class ImplingTopic {
        AnchovyOil,
        Sieve,
        Leave,
    }

    private companion object {
        const val ANJA = "npc.rimmington_anja"
        const val HENGEL = "npc.rimmington_hengel"
        const val WITCHS_POTION = "quest_witchspotion"
        const val IMP_REPELLENT = "obj.ii_imp_repellent"
        const val BUTTERFLY_JAR = "obj.butterfly_jar"
        const val SIEVE = "obj.mourning_sieve"
    }
}
