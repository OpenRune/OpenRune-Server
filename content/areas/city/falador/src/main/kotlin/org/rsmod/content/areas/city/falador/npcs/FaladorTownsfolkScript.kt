package org.rsmod.content.areas.city.falador.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.Constants
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Talk-to dialogue for Falador's non-shop NPCs, taken from the wiki's standard transcripts. */
class FaladorTownsfolkScript @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.rd_teleporter_guy") { startDialogue(it.npc) { sirTiffy() } }
        onOpNpc1("npc.partyroom_pete") { startDialogue(it.npc) { partyPete(it.npc) } }
        onOpNpc1("npc.sir_vyvin") { startDialogue(it.npc) { sirVyvin() } }
        onOpNpc1("npc.white_knight_diary") { startDialogue(it.npc) { sirRebral() } }
        onOpNpc1("npc.wyson") { startDialogue(it.npc) { wyson() } }
        onOpNpc1("npc.pmod_town_crier_falador") { startDialogue(it.npc) { townCrier() } }
        onOpNpc1("npc.aluft_gnome_diplomat_falador") { startDialogue(it.npc) { spanfipple() } }
        onOpNpc1("npc.falador_man4") { startDialogue(it.npc) { norman() } }
        onOpNpc1("npc.falador_man1") { startDialogue(it.npc) { drunkenMan() } }
        onOpNpc1("npc.falador_woman") { startDialogue(it.npc) { cecilia() } }
        onOpNpc1("npc.falador_workman_fat") { startDialogue(it.npc) { workman() } }
        onOpNpc1("npc.falador_workman_young") { startDialogue(it.npc) { apprenticeWorkman() } }
        for (client in MAHOGANY_HOMES_CLIENTS) {
            onOpNpc1(client) { startDialogue(it.npc) { mahoganyHomesClient() } }
        }
    }

    private suspend fun Dialogue.sirTiffy() {
        val greeting =
            if (player.appearance.bodyType == Constants.bodytype_a) "What ho, sir." else "What ho, milady."
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "$greeting Spiffing day for a walk in the park, what?")
        chatPlayer(confused, "...spiffing?")
        chatNpc(
            laugh,
            "Absolutely, top-hole! Well, can't stay and chat all day, dontchaknow! Ta-ta for now!",
        )
        chatPlayer(confused, "Erm... goodbye.")
    }

    private suspend fun Dialogue.partyPete(npc: Npc) {
        chatNpc(happy, "Hi! I'm Party Pete. Welcome to the Party Room!")
        when (
            choice5(
                "So what's this room for?",
                1,
                "What's the big lever over there for?",
                2,
                "What's the gold chest for?",
                3,
                "Have you always been here in Falador?",
                4,
                "I wanna party!",
                5,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "So what's this room for?")
                chatNpc(happy, "This room is for partying the night away!")
                chatPlayer(quiz, "How do you have a party in Gielinor?")
                chatNpc(happy, "Get a few mates round, get the beers in and have fun!")
                chatNpc(happy, "Some players organise parties so keep an eye open!")
                chatPlayer(laugh, "Woop! Thanks Pete!")
            }
            2 -> {
                chatPlayer(quiz, "What's the big lever over there for?")
                chatNpc(happy, "Simple. With the lever you can do some fun stuff.")
                chatPlayer(quiz, "What kind of stuff?")
                chatNpc(
                    happy,
                    "A balloon drop costs 1000 gold. For this you get 200 balloons dropped across the " +
                        "whole of the party room. You can then have fun popping the balloons! If there " +
                        "are items in the Party Drop Chest they will be inside",
                )
                chatNpc(
                    happy,
                    "the balloons! For 500 gold you can summon the Party Room Knights who will dance " +
                        "for your delight.",
                )
                chatNpc(laugh, "Their singing isn't a delight though!")
            }
            3 -> {
                chatPlayer(quiz, "What's the gold chest for?")
                chatNpc(
                    happy,
                    "Any items that are in the chest will be dropped inside the balloons when you pull " +
                        "the lever!",
                )
                chatPlayer(happy, "Cool! Sounds like a fun way to do a drop party!")
                chatNpc(happy, "Exactly!")
                chatNpc(
                    neutral,
                    "A word of warning though. Any items that you put into the chest can't be taken " +
                        "out again and it costs 1000 gold pieces for each balloon drop.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Have you always been here in Falador?")
                chatNpc(
                    neutral,
                    "We used to be in Seers' Village, far to the west, but we had to move - the seers " +
                        "were complaining about the noise level, and the knights of Camelot got it into " +
                        "their heads that the Party Room knights were making fun of",
                )
                chatNpc(neutral, "them.")
                chatNpc(
                    happy,
                    "We're doing well here, though. The people of Falador are happy we're here, and " +
                        "we've hardly ever had the White Knights telling us to keep the noise down.",
                )
                chatNpc(laugh, "We're going to turn Falador into the party capital of Gielinor!")
            }
            else -> {
                chatPlayer(happy, "I wanna party!")
                chatNpc(happy, "I've won the Dance Trophy at the Kandarin Ball three years in a trot!")
                chatPlayer(happy, "Show me your moves Pete!")
                npc.anim(SEQ_DANCE)
                access.delay(DANCE_CYCLES)
                npc.anim(SEQ_CHEER)
                access.delay(CHEER_CYCLES)
                npc.anim(SEQ_BOW)
            }
        }
    }

    private suspend fun Dialogue.sirVyvin() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Greetings traveller.")
        when (
            choice3(
                "Do you have anything to trade?",
                1,
                "Why are there so many knights in this city?",
                2,
                "Can I just distract you for a minute?",
                3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Do you have anything to trade?")
                chatNpc(neutral, "No, I'm sorry.")
            }
            2 -> {
                chatPlayer(quiz, "Why are there so many knights in this city?")
                chatNpc(
                    neutral,
                    "We are the White Knights of Falador. We are the most powerful order of knights in " +
                        "the land. We are helping the king Vallance rule the kingdom as he is getting old " +
                        "and tired.",
                )
            }
            else -> {
                chatPlayer(
                    happy,
                    "Can I just talk to you very slowly for a few minutes, while I distract you, so " +
                        "that my friend over there can do something while you're busy being distracted " +
                        "by me?",
                )
                chatNpc(confused, "... ...what?")
                chatNpc(
                    confused,
                    "I'm... not sure what you're asking me... you want to join the White Knights?",
                )
                chatPlayer(happy, "Nope. I'm just trying to distract you.")
                chatNpc(confused, "... ...you are very odd.")
                chatPlayer(happy, "So can I distract you some more?")
                chatNpc(confused, "... ...I don't think I want to talk to you anymore.")
                chatPlayer(happy, "Ok. My work here is done. 'Bye!")
            }
        }
    }

    private suspend fun Dialogue.sirRebral() {
        chatNpc(happy, "Hello!")
        while (true) {
            when (
                choice3(
                    "Who are you?",
                    1,
                    "I have a question about my Achievement Diary",
                    2,
                    "Bye!",
                    3,
                )
            ) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm Sir Rebral of the White Knights and taskmaster for the Falador Achievement " +
                            "Diary.",
                    )
                    chatPlayer(quiz, "What is the Achievement Diary?")
                    explainDiary()
                }
                2 -> {
                    chatPlayer(quiz, "I have a question about my Achievement diary.")
                    if (!diaryQuestions()) {
                        rebralGoodbye()
                        return
                    }
                }
                else -> {
                    rebralGoodbye()
                    return
                }
            }
        }
    }

    /** @return `false` if the player chose to say goodbye. */
    private suspend fun Dialogue.diaryQuestions(): Boolean {
        when (
            choice4(
                "What is the Achievement Diary?",
                1,
                "What are the rewards?",
                2,
                "How do I claim the rewards?",
                3,
                "Bye!",
                4,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What is the Achievement Diary?")
                explainDiary()
            }
            2 -> diaryRewards()
            3 -> {
                chatPlayer(quiz, "How do I claim the rewards?")
                chatNpc(
                    happy,
                    "Just complete the tasks in the Falador area so they're ticked off, then come and " +
                        "speak to me for your rewards.",
                )
            }
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.explainDiary() {
        chatNpc(
            happy,
            "It's a diary that helps you keep track of particular achievements. In Falador and the " +
                "surrounding area it can help you discover some quite useful things. Eventually, with " +
                "enough exploration, the inhabitants will",
        )
        chatNpc(happy, "reward you.")
    }

    private suspend fun Dialogue.diaryRewards() {
        chatNpc(
            happy,
            "Well, there are four different shields of Falador, which match up with the four levels " +
                "of difficulty. Each has the same rewards as the previous level and some additional " +
                "benefits too... which tier of rewards would you like to",
        )
        chatNpc(happy, "know more about?")
        val tier =
            choice4(
                "Easy Rewards.",
                "Easy",
                "Medium Rewards.",
                "Medium",
                "Hard Rewards.",
                "Hard",
                "Elite Rewards.",
                "Elite",
            )
        chatPlayer(happy, "Tell me more about the $tier rewards please!")
        when (tier) {
            "Easy" ->
                chatNpc(
                    happy,
                    "If you complete all of the easy tasks in Falador, the shield can restore one " +
                        "quarter of your prayer points once per day and you can access a shortcut to the " +
                        "Chaos temple from Burthorpe.",
                )
            "Medium" -> {
                chatNpc(
                    happy,
                    "In addition to the easy rewards, the shield can restore half of your prayer " +
                        "points once per day and provides a 10% experience boost at Falador farm. You'll " +
                        "get an increased chance of finding a clue scroll from a Guard",
                )
                chatNpc(happy, "and can access a shortcut within the Motherlode Mine")
            }
            "Hard" -> {
                chatNpc(
                    happy,
                    "In addition to the easy and medium benefits, the shield will restore all of your " +
                        "prayer points once per day, you can access a bank deposit box at the crafting " +
                        "guild and a shortcut to the Heroes' Guild fountain. The Giant",
                )
                chatNpc(happy, "mole becomes much easier to locate and it will drop noted skins and claws.")
            }
            else -> {
                chatNpc(
                    happy,
                    "In addition to the previous tiers of rewards, the tree patch in Falador park will " +
                        "never become diseased and the shield will restore all of your prayer points twice " +
                        "per day in addition to an increased chance at higher level",
                )
                chatNpc(happy, "ores from cleaning Pay-dirt.")
            }
        }
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.rebralGoodbye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "See you later.")
    }

    private suspend fun Dialogue.wyson() {
        chatNpc(
            neutral,
            "I'm the head gardener around here. If you're looking for woad leaves, or if you need " +
                "help with owt, I'm yer man.",
        )
        when (
            choice3(
                "Yes please, I need woad leaves.",
                1,
                "How about ME helping YOU instead?",
                2,
                "Sorry, but I'm not interested.",
                3,
            )
        ) {
            1 -> buyWoadLeaves()
            2 -> wysonMole()
            else -> {
                chatPlayer(neutral, "Sorry, but I'm not interested.")
                chatNpc(neutral, "Fair enough.")
            }
        }
    }

    private suspend fun Dialogue.buyWoadLeaves() {
        chatPlayer(happy, "Yes please, I need woad leaves.")
        chatNpc(quiz, "How much are you willing to pay?")
        val offer =
            choice4(
                "How about 5 coins?",
                5,
                "How about 10 coins?",
                10,
                "How about 15 coins?",
                15,
                "How about 20 coins?",
                20,
            )
        chatPlayer(quiz, "How about $offer coins?")
        val leaves =
            when (offer) {
                15 -> {
                    chatNpc(neutral, "Mmmm... okay, that sounds fair.")
                    1
                }
                20 -> {
                    chatNpc(happy, "Okay, that's more than fair.")
                    2
                }
                else -> {
                    chatNpc(
                        neutral,
                        "No no, that's far too little. Woad leaves are hard to get. I used to have " +
                            "plenty but someone kept stealing them off me.",
                    )
                    return
                }
            }
        if (!player.invTakeFee(offer)) {
            chatPlayer(sad, "I don't have enough coins to buy the leaves. I'll come back later.")
            return
        }
        player.invAddOrDrop(objRepo, "obj.woadleaf", count = leaves)
        if (leaves == 2) {
            chatNpc(happy, "Here, have two, you're a generous person.")
            chatPlayer(happy, "Thanks.")
        } else {
            chatPlayer(happy, "Thanks.")
            chatNpc(happy, "I'll be around if you have any more gardening needs.")
        }
    }

    private suspend fun Dialogue.wysonMole() {
        chatPlayer(happy, "How about ME helping YOU instead?")
        chatNpc(
            happy,
            "That's a nice thing to say. I do need a hand, now you mention it. You see, there's some " +
                "stupid mole digging up my lovely garden.",
        )
        chatPlayer(quiz, "A mole? Surely you've dealt with moles in the past?")
        chatNpc(
            worried,
            "Ah, well this is no ordinary mole! He's a big'un for sure. Ya see... I'm always relied " +
                "upon to make the most of this 'ere garden - the faster and bigger I can grow plants the " +
                "better!",
        )
        chatNpc(
            worried,
            "In my quest for perfection I looked into 'Malignius-Mortifer's-Super-Ultra-Flora-Growth-" +
                "Potion'. It worked well on my plants, no doubt about it! But it had the same effect on a " +
                "nearby mole. Ya can imagine the",
        )
        chatNpc(
            worried,
            "havoc he causes to my patches of sunflowers! Why, if any of the other gardeners knew " +
                "about this mole, I'd be looking for a new job in no time!",
        )
        chatPlayer(neutral, "I see. What do you need me to do?")
        chatNpc(
            neutral,
            "If ya are willing maybe yer wouldn't mind killing it for me? Take a spade and use it to " +
                "shake up them mole hills. Be careful though, he really is big!",
        )
        chatPlayer(quiz, "Is there anything in this for me?")
        chatNpc(
            neutral,
            "Well, if yer gets any mole skin or mole claws off 'un, I'd trade 'em for bird nests if " +
                "ye brings 'em here to me.",
        )
        chatPlayer(neutral, "Right, I'll bear it in mind.")
    }

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

    private suspend fun Dialogue.spanfipple() {
        chatNpc(neutral, "It's all very bright round here, isn't it?")
        chatPlayer(neutral, "Well, it is the White Knights' Castle.")
        chatNpc(
            neutral,
            "I think it would all look better in a nice dark green. At least then I wouldn't be " +
                "squinting all the time.",
        )
        chatPlayer(
            neutral,
            "Yes, but then they'd have to become the Dark Green Knights. Doesn't really have the same " +
                "ring to it.",
        )
        chatNpc(angry, "Bah, humans have no sense of style...")
    }

    private suspend fun Dialogue.norman() {
        chatPlayer(happy, "Hello.")
        chatNpc(worried, "Quickly, tell me - is it still there?")
        chatPlayer(confused, "Is what still where?")
        chatNpc(
            worried,
            "The THING, the THING! It was just outside my house! Has it gone away yet? Or is it still " +
                "lurking out there, waiting for me to go outside?",
        )
        chatPlayer(
            neutral,
            "I didn't see any THING out there, just a couple of guards. What did it look like?",
        )
        chatNpc(
            worried,
            "Ohhhh, it was HORRIBLE! It was an enormous THING, with TEETH and EYES and... and... and " +
                "THINGS!",
        )
        chatPlayer(confused, "Um... would you care to be more specific?")
        chatNpc(worried, "I can't. I only saw it in the dark.")
        chatPlayer(confused, "You only saw this THING in the dark?")
        chatNpc(
            worried,
            "I was sleeping peacefully one night, when suddenly I woke up and saw it through the " +
                "window. It was LOOKING at me! I haven't dared go out since then. It's had me trapped in " +
                "here for days! I've packed my",
        )
        chatNpc(
            worried,
            "bags so I can escape, but the THING's still out there waiting for me to come out! If I " +
                "have to stay in here much longer I'll go mad! MAD!! MAD!!!",
        )
        chatPlayer(
            neutral,
            "There's no THING outside. Just come out and get some fresh air before you go funny in the " +
                "head.",
        )
        chatNpc(shocked, "You want me to go outside?")
        chatPlayer(neutral, "I think you might just have dreamed about the THING.")
        chatNpc(shocked, "You want me to believe that it's not real?")
        chatPlayer(neutral, "Please come outside!")
        chatNpc(
            angry,
            "No! No! I know what you are! You're in league with the THING! It keeps sending people in " +
                "here to trick me into going outside! They keep stealing from me too! GO AWAY!",
        )
        chatPlayer(shocked, "I'm not trying to trick you!")
        chatNpc(angry, "Get thee gone, trickster!")
        chatPlayer(bored, "Sheesh...")
    }

    private suspend fun Dialogue.drunkenMan() {
        chatPlayer(happy, "Hello.")
        chatNpc(drunk, "... whassup?")
        chatPlayer(quiz, "Are you alright?")
        chatNpc(drunk, "... see... two of you... why there two of you?")
        chatPlayer(neutral, "There's only one of me, friend.")
        chatNpc(drunk, "... no, two of you... you can't count... ... maybe you drunk too much...")
        chatPlayer(neutral, "Whatever you say, friend.")
        chatNpc(drunk, "... giant hairy cabbages...")
    }

    private suspend fun Dialogue.cecilia() {
        chatNpc(
            happy,
            "Greetings! Have you come to gaze in rapture at the natural beauty of Falador's parkland?",
        )
        chatPlayer(neutral, "Um, yes, very nice. Lots of... trees and stuff.")
        chatNpc(laugh, "Trees! I do so love trees! And flowers! And squirrels!")
        chatPlayer(worried, "Sorry, I have a strange urge to be somewhere else.")
        chatNpc(happy, "Come back to me soon and we can talk again about trees!")
        chatPlayer(bored, "...")
    }

    private suspend fun Dialogue.workman() {
        chatPlayer(happy, "Hiya.")
        chatNpc(angry, "What do you want? I've got work to do!")
        chatPlayer(quiz, "Can you teach me anything?")
        chatNpc(
            angry,
            "No - I've got one lousy apprentice already, and that's quite enough hassle! Go away!",
        )
    }

    private suspend fun Dialogue.apprenticeWorkman() {
        chatPlayer(happy, "Hiya.")
        chatNpc(
            neutral,
            "Sorry, I haven't got time to chat. We've only just finished a collossal order of furniture " +
                "for the Varrock area, and already there's more work coming in.",
        )
        chatPlayer(quiz, "Varrock?")
        chatNpc(neutral, "Yeah, the Council's had it redecorated.")
        chatNpcSpecific(
            "Workman",
            "npc.falador_workman_fat",
            angry,
            "- Oi - stop gabbing and get that chair finished!",
        )
        chatNpc(worried, "You'd better let me get on with my work.")
        chatPlayer(neutral, "Ok, bye.")
    }

    private suspend fun Dialogue.mahoganyHomesClient() {
        chatNpc(neutral, "Please excuse me, I'm rather busy.")
    }

    private companion object {
        const val SEQ_DANCE = "seq.emote_dance"
        const val SEQ_CHEER = "seq.emote_cheer"
        const val SEQ_BOW = "seq.emote_bow"
        const val DANCE_CYCLES = 5
        const val CHEER_CYCLES = 3

        val MAHOGANY_HOMES_CLIENTS =
            listOf("npc.con_contract_client_4", "npc.con_contract_client_5")
    }
}
