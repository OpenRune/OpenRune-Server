package org.rsmod.content.quest.area.lumbridge.idesofmilk

import jakarta.inject.Inject
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.ironman.isAnyIronman
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.ItemRewardDisplay
import org.rsmod.content.quest.manager.QuestScript
import org.rsmod.content.quest.manager.rewards
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/The_Ides_of_Milk
// Dialogue, mesanims and stage changes: live capture, rev 241; branches it didn't take come
// from https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class IdesOfMilkQuest
@Inject
constructor(private val npcRepo: NpcRepository, private val random: GameRandom) :
    QuestScript(
        "quest_idesofmilk",
        "varp.cowquest_main",
        rewards {
            extra("Access to Brutus' Field")
            extra("Gillie's Cowbell Supply")
        },
        // live capture, rev 241
        ItemRewardDisplay(MILK_SAMPLE_2, zoom = 400),
        questVarbit = "varbit.cowquest",
    ) {

    private var Player.gillieInformation by boolVarBit("varbit.cowquest_gillie_information")
    private var Player.sethInformation by boolVarBit("varbit.cowquest_seth_information")
    private var Player.rewardReceived by boolVarBit("varbit.cowquest_reward")
    private var Player.lampUsed by boolVarBit("varbit.cowboss_reward_lamp")

    override fun ScriptContext.init() {
        check(quest.maxSteps == STAGE_COMPLETE) {
            "The Ides of Milk end state is ${quest.maxSteps} in the cache dbrow, " +
                "but the script completes at $STAGE_COMPLETE."
        }
        onOpNpc1(CASSIUS) { startDialogue(it.npc) { cassius() } }
        onOpLoc1(SETH_SHELF) { searchShelf() }
        onOpHeld1(MILK_SAMPLE_1) { drinkFirstSample() }
        onOpHeld1(MILK_SAMPLE_2) { drinkSecondSample() }
        onOpHeld1(LAMP) { rubLamp() }
    }

    override fun subTitle(): String =
        "talking to <col=800000>Cassius</col> south of the <col=800000>Lumbridge " +
            "Windmill</col>."

    // Journal text: live capture, rev 241.
    override fun questLog(player: ProtectedAccess) =
        questJournal(player) {
            val stage = stage(player.player)
            val paragraphs = JOURNAL_DONE.filterKeys { it <= stage }.values
            strike(JOURNAL_START)
            paragraphs.forEach { strike(it) }
            JOURNAL_CURRENT.entries.lastOrNull { it.key <= stage }?.let { line(it.value) }
        }

    override fun completedLog(player: ProtectedAccess): String =
        completionJournal(player) {
            line(JOURNAL_START)
            JOURNAL_DONE.values.forEach { line(it) }
        }

    fun stage(player: Player): Int = quest.getQuestStage(player)

    // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Starting_off
    private suspend fun Dialogue.cassius() {
        when (val stage = stage(player)) {
            0 -> cassiusStart()
            in STAGE_STARTED..STAGE_RESEARCH -> {
                chatPlayer(quiz, "What was it you needed me to do again?")
                chatNpc(
                    neutral,
                    "I need you to speak to Gillie and Seth Groats. We need to learn everything " +
                        "we can.",
                )
                chatNpc(
                    neutral,
                    "They live just across the River Lum. Head east from here and you should be " +
                        "able to find them.",
                )
            }
            STAGE_RESEARCHED -> cassiusBook()
            STAGE_BOOK_GIVEN,
            STAGE_FIRST_SAMPLE -> cassiusFirstSample()
            STAGE_FIRST_SAMPLE_DRUNK -> cassiusPermit()
            in STAGE_SECOND_SAMPLE until STAGE_GILLIE_DRANK -> cassiusAwaitingPermit()
            in STAGE_GILLIE_DRANK until STAGE_GILLIE_TOLD ->
                // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Talking_to_Cassius
                {
                    chatNpc(quiz, "Has the Duke given us our permit yet?")
                    chatPlayer(neutral, "It's become rather complicated, but I am working it out!")
                }
            STAGE_GILLIE_TOLD -> cassiusFinish()
            else -> {
                check(stage >= STAGE_COMPLETE)
                mesbox("Cassius makes a point of ignoring you.")
            }
        }
    }

    private suspend fun Dialogue.cassiusStart() {
        chatNpc(happy, "You!")
        chatPlayer(quiz, "Me?")
        chatNpc(happy, "Yes you!")
        chatPlayer(quiz, "Can I... help you?")
        chatNpc(quiz, "That depends. Do you like making money?")
        chatPlayer(neutral, "I'm not averse to making money.")
        chatNpc(quiz, "And do you enjoy... milk?")
        chatPlayer(quiz, "Milk?")
        chatNpc(neutral, "Milk.")
        // TODO(content): [system:speedrunning_worlds] "I suppose it's okay." on speedrunning worlds
        chatPlayer(neutral, "I can't say I've thought much about it.")
        chatNpc(quiz, "Would you like to think more about it?")
        if (player.combatLevel < RECOMMENDED_COMBAT) {
            mesbox(
                "Before starting this quest, be aware that your combat level is lower than the " +
                    "recommended level of $RECOMMENDED_COMBAT."
            )
        }
        val accept = choice2("Yes.", true, "No.", false, title = "Start The Ides of Milk quest?")
        if (!accept) {
            chatPlayer(neutral, "No, I'm not sure I would like that, thank you.")
            return
        }
        quest.setQuestStage(access, STAGE_STARTED)
        chatPlayer(quiz, "What am I going to get from thinking more about... milk?")
        chatNpc(happy, "Profit!")
        chatNpc(neutral, "Ten to fifteen percent, performance depending.")
        chatPlayer(quiz, "How are we going to be making that profit? And who even are you anyway?")
        chatNpc(shifty, "Ah, my name is Cassius. I got carried away.")
        chatNpc(shifty, "But, we're going to construct an empire.")
        chatPlayer(quiz, "Of milk?")
        quest.setQuestStage(access, STAGE_EMPIRE)
        chatNpc(shifty, "Of milk.")
        chatPlayer(quiz, "How do you intend to make an empire of milk?")
        chatNpc(neutral, "So, the first step is going to involve you doing a little... research.")
        chatPlayer(confused, "Right...")
        chatNpc(
            shifty,
            "You're going to cross the River Lum, find Gillie and Seth Groats, and learn about " +
                "animal husbandry.",
        )
        chatNpc(
            shifty,
            "Then... Then you're going to cross back to this side of the river, and tell me all " +
                "about how they've made their animals so productive.",
        )
        chatNpc(happy, "It's a perfect plan!")
        chatPlayer(
            quiz,
            "What use is it if I know everything and you don't? Couldn't I make my own... milk " +
                "empire... with that information?",
        )
        chatPlayer(neutral, "If anything I should be cutting you in at ten to fifteen percent.")
        chatNpc(neutral, "Hey, I'm the one supplying the high-quality milk here.")
        quest.setQuestStage(access, STAGE_RESEARCH)
        chatNpc(
            shifty,
            "Not to mention, my associate Brutus is already embedded in the Groats operation to " +
                "work a different angle. That will prove most lucrative.",
        )
        chatPlayer(confused, "Right... I guess I'll go and see the Groatses then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Returning_to_Cassius
    private suspend fun Dialogue.cassiusBook() {
        chatNpc(quiz, "So, how did you get on?")
        chatPlayer(
            happy,
            "I've learned a lot about how the Groats family care for their animals. They say the " +
                "secret is happiness!",
        )
        chatNpc(
            angry,
            "Happiness?! That can't be the only trick they use! Surely you found something else?",
        )
        if (player.inv.count(BOOK) == 0) {
            chatPlayer(
                neutral,
                "Now that you mention it, Seth did say he had a book that I... don't have on me " +
                    "right now.",
            )
            chatNpc(
                happy,
                "The legendary Groats book on milk cultivation? I'm most interested! Go fetch it!",
            )
            return
        }
        chatPlayer(
            neutral,
            "I have this book from Seth. He claims it's got his principles of animal husbandry in " +
                "it, if that's of any use?",
        )
        chatNpc(shifty, "Hand it here, quickly!")
        if (access.invDel(access.inv, BOOK, player.inv.count(BOOK)).failure) {
            return
        }
        quest.setQuestStage(access, STAGE_BOOK_GIVEN)
        objbox(BOOK, "Cassius snatches the book from your hands.")
        chatNpc(quiz, "Now, this is most curious.")
        chatNpc(
            neutral,
            "The idea that a happy animal will do what you want is frankly absurd. What if they " +
                "choose to not produce. Are you supposed to respect that?",
        )
        chatNpc(
            shifty,
            "However, the writings on the systems of taxation are fascinating. I couldn't imagine " +
                "Seth was smart enough to put this together.",
        )
        chatPlayer(quiz, "So, do you think this is enough to start our milk empire?")
        chatNpc(
            neutral,
            "No, no. Not quite yet. First of all, you haven't tried our milk yet, have you?",
        )
        chatPlayer(worried, "I don't think I have, no. You already have some milk?")
        chatNpc(neutral, "Don't look so worried. Here, have a try.")
        giveFirstSample("Cassius hands you a milk sample to try.")
    }

    private suspend fun Dialogue.cassiusFirstSample() {
        if (player.inv.count(MILK_SAMPLE_1) > 0) {
            chatNpc(quiz, "Have you given that sample a try yet?")
            return
        }
        // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Reclaiming_the_milk_sample
        chatPlayer(sad, "I've managed to lose that sample of milk...")
        if (player.inv.isFull()) {
            chatNpc(angry, "I'll give you some more once you've got some space.")
            return
        }
        giveFirstSample("Cassius glares as he hands you another sample of milk.")
    }

    private suspend fun Dialogue.giveFirstSample(text: String) {
        if (access.invAdd(access.inv, MILK_SAMPLE_1).failure) {
            return
        }
        quest.setQuestStage(access, STAGE_FIRST_SAMPLE)
        objbox(MILK_SAMPLE_1, text)
    }

    private suspend fun Dialogue.cassiusPermit() {
        chatNpc(quiz, "So, what do you think? The milk is fantastic, isn't it?")
        chatPlayer(shifty, "It wasn't what I was expecting, certainly.")
        chatNpc(
            happy,
            "We're going to change the lactic landscape! Before we do though, I have another task " +
                "for you.",
        )
        chatPlayer(angry, "Now what do you need?")
        chatNpc(neutral, "We need... a permit.")
        chatPlayer(quiz, "Shouldn't you have got a permit before making milk?")
        chatNpc(neutral, "The permit is to sell milk, not to make milk!")
        chatPlayer(quiz, "Fine, where are we going to get this permit?")
        chatNpc(
            neutral,
            "From the Duke of Lumbridge! Where else? You need to bring him our milk and I'm sure " +
                "he'll give us our permit without fuss.",
        )
        chatPlayer(sad, "Are you sure having the Duke drink our milk is a good idea?")
        chatNpc(
            shifty,
            "Of course! Soon the whole of Lumbridge will be drinking my, I mean our milk!",
        )
        if (access.invAdd(access.inv, MILK_SAMPLE_2).failure) {
            // UNVERIFIED: the full-inventory reply (the transcript's lost-sample wording)
            chatNpc(
                neutral,
                "It looks like you don't have space to carry this. You'll have to grab it from me " +
                    "when you have room.",
            )
            return
        }
        quest.setQuestStage(access, STAGE_SECOND_SAMPLE)
        objbox(MILK_SAMPLE_2, "Cassius hands you a milk sample.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Talking_to_Cassius_again
    private suspend fun Dialogue.cassiusAwaitingPermit() {
        chatNpc(quiz, "Has the Duke given us our permit yet?")
        if (player.inv.count(MILK_SAMPLE_2) > 0 || stage(player) < STAGE_SECOND_SAMPLE) {
            chatPlayer(neutral, "Not yet, no, but I am working on it!")
            return
        }
        chatPlayer(
            neutral,
            "Not yet, no. I could do with another sample of milk though, if you have one spare?",
        )
        chatNpc(neutral, "I should have one somewhere here. Just a moment.")
        if (player.inv.isFull()) {
            chatNpc(
                neutral,
                "It looks like you don't have space to carry this. You'll have to grab it from me " +
                    "when you have room.",
            )
            return
        }
        if (access.invAdd(access.inv, MILK_SAMPLE_2).failure) {
            return
        }
        objbox(MILK_SAMPLE_2, "Cassius hands you a milk sample.")
    }

    private suspend fun Dialogue.cassiusFinish() {
        chatPlayer(neutral, "Cassius, I don't think we'll be getting our business permit.")
        chatNpc(quiz, "Why? What's happened?")
        chatPlayer(sad, "Brutus is dead.")
        chatNpc(quiz, "Dead?")
        chatNpc(quiz, "How could this have happened?")
        chatPlayer(neutral, "So the Duke asked me to... and then Gillie said that...")
        chatNpc(angry, "You?!")
        chatNpc(angry, "Don't tell me you killed Brutus and ruined my- our, plan?")
        chatPlayer(
            neutral,
            "In my defence, I had no idea that was Brutus! You made it sound like he was a " +
                "business partner.",
        )
        chatPlayer(
            neutral,
            "Not to mention, apparently his aggression was ruining Seth's herd's milk. This " +
                "should be a good thing!",
        )
        chatNpc(angry, "Ruining the milk? The unique flavour was what I was trying to sell!")
        chatNpc(angry, "You should leave. I can't even look at you.")
        quest.setQuestStage(access, STAGE_COMPLETE)
    }

    fun canAskGillieAboutCows(player: Player): Boolean =
        stage(player) in STAGE_RESEARCH..STAGE_RESEARCHED && !player.gillieInformation

    suspend fun Dialogue.gillieAboutCows() {
        chatPlayer(quiz, "Can you share what makes your cows so productive?")
        chatNpc(happy, "Absolutely! The secret is happiness.")
        chatPlayer(quiz, "Happiness?")
        chatNpc(
            happy,
            "The cows make the best milk when they're happy, and to be honest, they make too much!",
        )
        chatNpc(
            happy,
            "I'm always glad that adventurers come by and help with milking them. I wouldn't be " +
                "able to keep up otherwise.",
        )
        chatPlayer(quiz, "That can't be everything though, can it?")
        chatNpc(
            neutral,
            "Come to think of it, I'm sure my father keeps a few tricks up his sleeve. Why don't " +
                "you go and ask him?",
        )
        val metSeth = player.sethInformation
        player.gillieInformation = true
        advanceResearch()
        if (!metSeth) {
            chatPlayer(quiz, "That would be Seth, wouldn't it?")
            chatNpc(
                happy,
                "It would be! I'm sure he'll be happy to share some information with you.",
            )
            chatPlayer(happy, "I'll go and speak to him now, thank you!")
            return
        }
        chatPlayer(
            happy,
            "I've already had a chance to talk to him. He did indeed have some tricks to share!",
        )
        chatNpc(happy, "That's good. You must be ready to care for your own cows at this point!")
        chatPlayer(
            happy,
            "If you think so, then I must be. Thank you for your help, I'll make sure that " +
                "knowledge is put to good use.",
        )
    }

    suspend fun Dialogue.gillieDialogue(): Boolean {
        when (stage(player)) {
            STAGE_DUKE_SPOKEN -> gillieSample()
            STAGE_GILLIE_ASKED -> {
                chatNpc(quiz, "Have you tried that milk sample yet?")
                chatPlayer(sad, "Not yet. I really don't want to, but I guess I must.")
            }
            STAGE_GILLIE_DRANK -> gillieAfterDrink()
            STAGE_BULL_TASK,
            STAGE_BULL_TASK + 1 -> {
                chatNpc(quiz, "How are you getting on with the bull?")
                chatPlayer(neutral, "I'm getting there! He looks quite tough though.")
            }
            STAGE_BRUTUS_KILLED -> gillieBrutusKilled()
            STAGE_GILLIE_TOLD -> {
                chatNpc(
                    happy,
                    "Thank you once again for handling Brutus, he was causing quite a problem for " +
                        "my herd.",
                )
                chatNpc(
                    neutral,
                    "And, if you haven't yet spoken to Cassius, you should probably let him know " +
                        "what's transpired.",
                )
            }
            STAGE_COMPLETE -> return gillieRewards()
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.gillieSample() {
        chatNpc(quiz, "Hiya, did you need something else from me?")
        chatPlayer(
            neutral,
            "I did. I've been trying to secure a business permit from the Duke, but he said that " +
                "as it is a milk-related business venture, I should speak to you instead.",
        )
        chatNpc(
            happy,
            "The Duke is a good man, directing you to me! I've been running the milk side of our " +
                "farm for a while now.",
        )
        chatNpc(quiz, "Were you thinking of being a competitor to us then?")
        if (player.inv.count(MILK_SAMPLE_2) == 0) {
            chatPlayer(neutral, "I can prove we wouldn't be if you could just try this sample of...")
            chatNpc(quiz, "Sample of?")
            chatPlayer(
                sad,
                "I seem to have misplaced my sample of milk. I'll be back when I have it.",
            )
            return
        }
        chatPlayer(
            neutral,
            "I think if you were to try this sample here you'd see we wouldn't be competing.",
        )
        chatNpc(quiz, "You want me to drink a vial of mysterious liquid?")
        chatPlayer(neutral, "Well, it's not mysterious, it's milk!")
        chatNpc(
            shifty,
            "No, I think you should drink it, I'll know the quality of it from your reaction.",
        )
        chatNpc(shifty, "It is just milk after all.")
        chatPlayer(quiz, "Would it convince you to help me with my permit?")
        quest.setQuestStage(access, STAGE_GILLIE_ASKED)
        chatNpc(happy, "It definitely won't hurt your chances.")
    }

    private suspend fun Dialogue.gillieAfterDrink() {
        chatNpc(sad, "That looked like a horrible experience.")
        chatPlayer(neutral, "It wasn't pleasant.")
        chatNpc(quiz, "Where did you even get that milk?")
        chatPlayer(
            neutral,
            "My business partner, Cassius. I don't know where he gets it from originally though.",
        )
        chatNpc(
            quiz,
            "Cassius... I don't recognise the name. He's not mentioned anything about its source?",
        )
        chatPlayer(
            neutral,
            "Nothing, but he apparently has enough of a source to start a business. I'm mostly " +
                "trying to be helpful.",
        )
        chatPlayer(
            quiz,
            "If the milk's bad, you're not going to recommend us for a permit, are you?",
        )
        chatNpc(neutral, "With how bad it looks, you wouldn't be any competition to us.")
        chatNpc(
            quiz,
            "However, I wonder if I'd be doing a public service by protecting anyone else from it.",
        )
        chatPlayer(quiz, "Is there anything more I could do to convince you?")
        chatNpc(
            neutral,
            "Now that you mention it, I've had an incredibly aggressive bull disrupting my herd " +
                "lately. I've got him caught in the pen over there.",
        )
        quest.setQuestStage(access, STAGE_BULL_TASK)
        chatNpc(
            quiz,
            "Perhaps you'd be interested in dealing with him? I'd owe you a fairly large favour.",
        )
        chatPlayer(quiz, "A roughly permit sized favour?")
        chatNpc(neutral, "Absolutely.")
    }

    private suspend fun Dialogue.gillieBrutusKilled() {
        chatNpc(
            happy,
            "You succeeded! I'll let the Duke know that you're all good to start a milk business.",
        )
        chatPlayer(sad, "I don't think that's going to be necessary anymore.")
        chatNpc(quiz, "Oh? What happened in that field?")
        chatPlayer(sad, "That bull was Cassius' other business partner, Brutus.")
        chatPlayer(
            neutral,
            "I'm sure he won't be feeling particularly friendly once I let him know.",
        )
        chatNpc(quiz, "That bull was one of the business partners?")
        chatNpc(
            quiz,
            "I wonder if his aggression is why Cassius' herd are producing such bad milk...",
        )
        chatPlayer(quiz, "I'm starting to think Cassius doesn't even have his own herd.")
        chatNpc(quiz, "You think he's been stealing milk?")
        chatPlayer(sad, "I'm not sure where else he would've been getting it from.")
        quest.setQuestStage(access, STAGE_GILLIE_TOLD)
        chatNpc(
            happy,
            "Why don't you go and talk to him about it! He may appreciate knowing the Brutus was " +
                "bad for business.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Post-quest
    // TODO(content): Gillie's replacement for a lost cowbell amulet (transcript wording not
    // captured; the wiki gives it as a short exchange).
    private suspend fun Dialogue.gillieRewards(): Boolean {
        if (!player.rewardReceived) {
            chatNpc(happy, "Oh, just the person I wanted to see! I have something for you.")
            if (player.inv.isFull()) {
                chatNpc(neutral, "Ah, your inventory is full. Can you make some room?")
                return true
            }
            chatNpc(
                happy,
                "This is for helping out with that bull. Our cows are much happier these days.",
            )
            // The empty amulet goes in the inventory, the objbox shows the full one (live capture,
            // rev 241).
            if (access.invAdd(access.inv, COWBELL_AMULET_EMPTY).failure) {
                return true
            }
            player.rewardReceived = true
            objbox(COWBELL_AMULET, "Gillie hands you an amulet with a bell on it.")
            chatNpc(happy, "One of our local milk enjoyers gave us some enchanted cow bells.")
            chatNpc(neutral, "They seem to be quite versatile, enjoy!")
            chatPlayer(happy, "Wow, thank you! I'll make sure to check it out.")
            if (player.inv.isFull()) {
                chatNpc(
                    neutral,
                    "I also have something else for you but your inventory is full. Can you make " +
                        "some room?",
                )
                return true
            }
            chatNpc(happy, "I also have this for you!")
            giveLamp()
            return true
        }
        if (!player.lampUsed && !hasLamp(player)) {
            chatNpc(happy, "I have this for you!")
            if (player.inv.isFull()) {
                chatNpc(neutral, "Ah, your inventory is full. Can you make some room?")
                return true
            }
            giveLamp()
            return true
        }
        return false
    }

    private suspend fun Dialogue.giveLamp() {
        if (access.invAdd(access.inv, LAMP).failure) {
            return
        }
        objbox(LAMP, "Gillie hands you a magical lamp.")
        chatPlayer(happy, "Oh, thank you!")
    }

    // TODO(content): [verify:in_game] whether the bank counts as having the lamp.
    private fun hasLamp(player: Player): Boolean = player.inv.count(LAMP) > 0

    suspend fun Dialogue.sethDialogue(): Boolean {
        if (stage(player) !in STAGE_RESEARCH..STAGE_RESEARCHED) {
            return false
        }
        chatNpc(quiz, "Well well, what can I do for you?")
        chatPlayer(
            quiz,
            "I was wondering if you could teach me a little about how you're so successful with " +
                "your animals.",
        )
        if (player.sethInformation) {
            chatNpc(neutral, "Mmm we already had this talk. Go read me book on the shelf over there.")
            if (player.inv.count(BOOK) == 0) {
                chatPlayer(neutral, "I'll go grab it, thanks.")
            } else {
                chatPlayer(
                    neutral,
                    "I've actually got it here, I'll take it away and give it a read somewhere a " +
                        "little more peaceful.",
                )
            }
            return true
        }
        chatNpc(quiz, "Lookin' to get into the business, eh?")
        chatPlayer(neutral, "Something along those lines.")
        chatNpc(happy, "Well, always happy to help another budding farmer out!")
        chatPlayer(quiz, "Great! So where do I start?")
        chatNpc(neutral, "Would always recommend me Groats Principles as yer basics.")
        chatPlayer(quiz, "Groats Principles?")
        chatNpc(happy, "Aye. First one is that happy animals makes happy farmers.")
        chatNpc(
            neutral,
            "Second of me principles is to never take more than you need. Greed'll stop you in " +
                "your tracks.",
        )
        chatNpc(
            shifty,
            "The last principle: never let the taxman know yer earnings. Money should be spent on " +
                "the animals!",
        )
        if (player.isAnyIronman) {
            chatPlayer(neutral, "I don't think I've ever paid any taxes.")
            chatNpc(happy, "You're ahead of the game then!")
        } else {
            chatPlayer(quiz, "You've found a way to get around the taxes?")
            chatNpc(shifty, "A way or two.")
        }
        player.sethInformation = true
        advanceResearch()
        chatNpc(
            neutral,
            "Anyway, feel free to grab me book on the topic from the shelf over there.",
        )
        chatPlayer(neutral, "Okay, thank you for the help.")
        return true
    }

    private fun Dialogue.advanceResearch() {
        if (player.gillieInformation && player.sethInformation && stage(player) == STAGE_RESEARCH) {
            quest.setQuestStage(access, STAGE_RESEARCHED)
        }
    }

    suspend fun Dialogue.dukeDialogue(): Boolean {
        when (stage(player)) {
            STAGE_SECOND_SAMPLE -> dukePermit()
            in STAGE_DUKE_SPOKEN until STAGE_GILLIE_DRANK -> {
                chatPlayer(quiz, "Hi, what did you want me to do for the permit again?")
                chatNpc(
                    neutral,
                    "Go speak to Gillie Groats. She seems to handle the milk business in " +
                        "Lumbridge. Maybe she'll get you set up under their permit.",
                )
                chatPlayer(neutral, "Thanks.")
            }
            else -> return false
        }
        return true
    }

    private suspend fun Dialogue.dukePermit() {
        chatPlayer(neutral, "Greetings. I come seeking a business permit.")
        chatNpc(quiz, "Oh wonderful! What sort of business are you looking to start?")
        chatPlayer(happy, "I'm looking to get into the milk trade!")
        chatNpc(
            quiz,
            "Milk? Well, the Groats family still have their permit, and I can't start stepping on " +
                "their toes.",
        )
        chatPlayer(neutral, "If you'd just try this milk...")
        chatNpc(
            neutral,
            "I really couldn't possibly give you a permit without their approval. This is really " +
                "their territory.",
        )
        quest.setQuestStage(access, STAGE_DUKE_SPOKEN)
        chatNpc(
            quiz,
            "Why not speak to Gillie Groats? She seems to handle the milk side of the business " +
                "now. Maybe she'll get you set up under their permit.",
        )
        chatPlayer(quiz, "You're the Duke. Couldn't you just grant a permit?")
        chatNpc(neutral, "No, no. It's really quite important you speak to Gillie about this.")
        chatPlayer(quiz, "Aren't you the highest power in this land?")
        chatNpc(
            shifty,
            "Don't go digging too deeply into that. Even I have some... commitments that I have " +
                "to keep to.",
        )
        chatPlayer(
            confused,
            "But you're the Duke, you shouldn't need the approval of a business entity to grant a " +
                "permit.",
        )
        chatPlayer(quiz, "Not to mention, can't they just refuse to have any competitors?")
        chatNpc(
            shifty,
            "Well absolutely, but I'm sure you'll find they're more than reasonable about things.",
        )
        chatNpc(sad, "Entirely. Completely reasonable.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Ides_of_Milk#Searching_the_shelf
    internal suspend fun ProtectedAccess.searchShelf() {
        arriveDelay()
        val bookTime =
            player.sethInformation &&
                stage(player) < STAGE_BOOK_GIVEN &&
                inv.count(BOOK) == 0
        if (!bookTime) {
            // UNVERIFIED: message box type for the random shelf finds (wiki transcript)
            mesbox(SHELF_FINDS[random.of(0, SHELF_FINDS.lastIndex)])
            return
        }
        if (inv.isFull()) {
            mesbox("You find Seth's book on the shelf, but don't have room to take it with you.")
            return
        }
        if (invAdd(inv, BOOK).failure) {
            return
        }
        // live capture, rev 241
        soundSynth("synth.pick2")
        objbox(BOOK, "You find Seth's book of principles on the shelf.")
    }

    internal suspend fun ProtectedAccess.drinkFirstSample() {
        if (stage(player) != STAGE_FIRST_SAMPLE) {
            // UNVERIFIED: drinking the first sample outside its stage
            mes("Nothing interesting happens.")
            return
        }
        if (invDel(inv, MILK_SAMPLE_1).failure) {
            return
        }
        quest.setQuestStage(this, STAGE_FIRST_SAMPLE_DRUNK)
        anim("seq.human_eat")
        soundSynth("synth.bucket_pour")
        startDialogue {
            // Live shows the first line with no continue, coughs and takes the hit three ticks
            // later without closing it, then shows the second line two ticks after that.
            access.chatPlayerNoContinue(sad, "Oh, that's not good milk.")
            access.delay(COUGH_DELAY)
            access.anim("seq.wgs_player_coughs")
            // https://oldschool.runescape.wiki/w/The_Ides_of_Milk: 1 poison damage, never lethal.
            val damage = if (player.hitpoints > 1) 1 else 0
            access.takeInstantHit(
                type = HitType.Typeless,
                damage = damage,
                hitmark = hitmark_groups.poison_damage,
            )
            // UNVERIFIED: always human_hit_4 (one capture; other hits pick one of four).
            access.soundSynth("synth.human_hit_4", delay = HIT_SOUND_DELAY)
            access.delay(SECOND_LINE_DELAY)
            chatPlayer(sad, "That's really not good milk.")
        }
    }

    private suspend fun ProtectedAccess.drinkSecondSample() {
        val stage = stage(player)
        if (stage < STAGE_GILLIE_ASKED) {
            startDialogue {
                chatPlayer(neutral, "I don't think I'm ready to drink another vial of this stuff.")
                chatPlayer(neutral, "I should find someone else to give it to.")
            }
            return
        }
        val gillie = gillieNearby()
        if (stage != STAGE_GILLIE_ASKED || gillie == null) {
            startDialogue { chatPlayer(neutral, "I shouldn't drink this unless I have Gillie nearby.") }
            return
        }
        if (invDel(inv, MILK_SAMPLE_2).failure) {
            return
        }
        quest.setQuestStage(this, STAGE_GILLIE_DRANK)
        anim("seq.human_eat")
        startDialogue(gillie) {
            chatPlayer(sad, "Oh, that's worse milk than before.")
            gillieAfterDrink()
        }
    }

    // UNVERIFIED: how close Gillie has to be (the wiki only says "next to Gillie")
    private fun ProtectedAccess.gillieNearby(): Npc? =
        npcRepo.findAll(ZoneKey.from(player.coords), zoneRadius = 1).firstOrNull {
            it.isType(GILLIE) && it.coords.chebyshevDistance(player.coords) <= GILLIE_RANGE
        }

    // live capture, rev 241
    internal suspend fun ProtectedAccess.rubLamp() {
        var chosen: Pair<String, String>? = null
        startDialogue { chosen = chooseCombatSkill() }
        val stat = chosen ?: return
        if (invDel(inv, LAMP).failure) {
            return
        }
        player.lampUsed = true
        statAdvance(stat.first, LAMP_XP)
        soundSynth("synth.found_gem")
        mesbox(
            "<col=0000ff>Your wish has been granted!</col> You have been awarded 1,000 " +
                "${stat.second} experience!"
        )
    }

    private suspend fun Dialogue.chooseCombatSkill(): Pair<String, String>? {
        val title = "Choose the skill you wish to gain XP in."
        var skill: Pair<String, String>? = null
        while (skill == null) {
            skill =
                when (
                    choice5(
                        "Attack.", 1, "Strength.", 2, "Defence.", 3, "Ranged.", 4,
                        "More options...", 5, title = title,
                    )
                ) {
                    1 -> "stat.attack" to "Attack"
                    2 -> "stat.strength" to "Strength"
                    3 -> "stat.defence" to "Defence"
                    4 -> "stat.ranged" to "Ranged"
                    else ->
                        when (
                            choice4(
                                "Magic.", 1, "Hitpoints.", 2, "Prayer", 3,
                                "Previous options...", 4, title = title,
                            )
                        ) {
                            1 -> "stat.magic" to "Magic"
                            2 -> "stat.hitpoints" to "Hitpoints"
                            3 -> "stat.prayer" to "Prayer"
                            else -> null
                        }
                }
        }
        val confirm =
            choice2(
                "Yes.", true, "No.", false,
                title = "Use the lamp to receive 1,000 ${skill.second} experience?",
            )
        return if (confirm) skill else null
    }

    companion object {
        private const val COUGH_DELAY = 3
        private const val HIT_SOUND_DELAY = 20
        private const val SECOND_LINE_DELAY = 2

        const val STAGE_STARTED = 1
        const val STAGE_EMPIRE = 2
        const val STAGE_RESEARCH = 3
        const val STAGE_RESEARCHED = 4
        const val STAGE_BOOK_GIVEN = 5
        const val STAGE_FIRST_SAMPLE = 6
        const val STAGE_FIRST_SAMPLE_DRUNK = 8
        const val STAGE_SECOND_SAMPLE = 10
        const val STAGE_DUKE_SPOKEN = 12
        const val STAGE_GILLIE_ASKED = 14
        const val STAGE_GILLIE_DRANK = 16
        const val STAGE_BULL_TASK = 18
        const val STAGE_BRUTUS_KILLED = 20
        const val STAGE_GILLIE_TOLD = 21
        const val STAGE_COMPLETE = 22

        const val CASSIUS = "npc.cowboss_farmer"
        const val GILLIE = "npc.gillie_the_milkmaid"
        const val SETH_SHELF = "loc.cowquest_seth_shelf"
        const val BOOK = "obj.cowquest_husbandry_book"
        const val MILK_SAMPLE_1 = "obj.cowquest_milk_sample_1"
        const val MILK_SAMPLE_2 = "obj.cowquest_milk_sample_2"
        const val COWBELL_AMULET = "obj.cowbell_amulet"
        const val COWBELL_AMULET_EMPTY = "obj.cowbell_amulet_empty"
        const val LAMP = "obj.cowboss_reward_lamp"

        private const val RECOMMENDED_COMBAT = 15
        private const val GILLIE_RANGE = 3
        private const val LAMP_XP = 1_000.0

        private val SHELF_FINDS =
            listOf(
                "You find a bucket on the shelf, as you'd expect a farmer to have.",
                "You find a loose egg on the shelf, and fortunately avoid knocking it to the floor.",
                "Hidden behind the piles of junk on the shelf, you find a small farmer's hat with " +
                    "Gillie's name written on it.",
            )

        private const val JOURNAL_START =
            "I have met an enigmatic man by the name of <red>Cassius</red> lurking around " +
                "Farmer Fred's farm in Lumbridge. Cassius has offered me the chance to share the " +
                "profits of his upcoming milk business, in exchange for a bit of help setting it up."

        private val JOURNAL_DONE =
            linkedMapOf(
                STAGE_BOOK_GIVEN to
                    "Having spoken to Seth and Gillie Groats, I found that they raise their " +
                    "animals following a set of principles that Seth keeps in a book. I took that " +
                    "book to Cassius as it may have been useful to his budding milk business.",
                STAGE_SECOND_SAMPLE to
                    "Having returned Seth's book to Cassius, he then offered me a sample of the " +
                    "milk we will be selling. It wasn't the most pleasant thing to drink, but I " +
                    "managed to finish it.",
                STAGE_DUKE_SPOKEN to
                    "The next step in getting our business set up was to get a business permit. " +
                    "To that end, Cassius sent me to the Duke of Lumbridge with another milk " +
                    "sample, aiming to convince him to let us begin trading.",
                STAGE_GILLIE_ASKED to
                    "I spoke to the Duke of Lumbridge about getting a business permit for " +
                    "Cassius' future milk empire, but unfortunately the Duke couldn't grant us " +
                    "one without us speaking to the Groats family first.",
                STAGE_BULL_TASK to
                    "I spoke to Gillie at the Duke of Lumbridge's request, and she asked me to " +
                    "drink the milk sample so that she could get a sense of the quality of the " +
                    "milk. It wasn't pleasant, but seemed to bring her around to my cause.",
                STAGE_BRUTUS_KILLED to
                    "Gillie sent me to deal with the aggressive bull that she had fenced in her " +
                    "field. It was a tough fight but I brought the beast down. Unfortunately it " +
                    "turned out the the bull was Brutus, who I believed to be Cassius business " +
                    "partner.",
                STAGE_COMPLETE to
                    "I have apologised to Cassius for killing Brutus, but unfortunately this may " +
                    "be a deathblow to his potential milk empire. It's unlikely he's going to " +
                    "want to see my face again.",
            )

        private val JOURNAL_CURRENT =
            linkedMapOf(
                STAGE_STARTED to
                    "I need to go and speak to <red>Gillie</red> and <red>Seth Groats</red> to " +
                    "research their animal husbandry techniques. Apparently Cassius already has " +
                    "an inside man in the Groats operation, <red>Brutus</red>.",
                STAGE_RESEARCHED to
                    "Having spoken to the Groats family, it seems they follow a number of " +
                    "principles with their animal care. Seth mentioned he had a book on a shelf " +
                    "in his house. <red>Cassius</red> might want to read it.",
                STAGE_FIRST_SAMPLE to
                    "<red>Cassius</red> has handed me a <red>Sample of Milk</red> to taste test. " +
                    "It doesn't look particularly special.",
                STAGE_FIRST_SAMPLE_DRUNK to
                    "I've tried Cassius's milk sample. I should let <red>him</red> know my " +
                    "thoughts.",
                STAGE_SECOND_SAMPLE to
                    "Cassius has asked me to take a second sample of milk to the <red>Duke of " +
                    "Lumbridge</red>, so that we can convince him to grant us our business permit.",
                STAGE_DUKE_SPOKEN to
                    "I have spoken to the Duke of Lumbridge, and he can't grant us our business " +
                    "permit without us first speaking to <red>Gillie Groats</red>.",
                STAGE_GILLIE_ASKED to
                    "Gillie seems willing to recommend us for a business permit, but first needs " +
                    "to see the quality of the milk sample that I have. She wants me to " +
                    "<red>drink it</red> so she can watch our reaction to it.",
                STAGE_BULL_TASK to
                    "<red>Gillie</red> does need another favour. There is a rather aggressive " +
                    "<red>bull</red> that she has penned in her field that she has asked me to " +
                    "deal with.",
                STAGE_BRUTUS_KILLED to
                    "I have killed Brutus, the aggressive bull. I should return to " +
                    "<red>Gillie</red> and ask her again about the permit.",
                STAGE_GILLIE_TOLD to
                    "Gillie has offered to grant me the business permit. Unfortunately it's no " +
                    "longer needed. I should return to <red>Cassius</red> to apologise for " +
                    "killing Brutus.",
            )
    }
}
