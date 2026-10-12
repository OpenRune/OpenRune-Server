package org.rsmod.content.areas.city.varrock.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseThievingLvl
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarrockPalace @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.myq3_aeonisig_roalds_advisor") { startDialogue(it.npc) { aeonisig() } }
        onOpNpc1("npc.aluft_gnome_diplomat_varrock") { startDialogue(it.npc) { ferrnook() } }
        onOpNpc1("npc.fai_varrock_guard_trainer") { startDialogue(it.npc) { captain() } }
        onOpNpc1("npc.fai_varrock_guard_trainer02") { startDialogue(it.npc) { captain() } }
        onOpNpc1("npc.captain_rovin") { startDialogue(it.npc) { captainRovin() } }
        onOpNpc1("npc.queen_ellamaria") { startDialogue(it.npc) { queenEllamaria() } }
        onOpNpc1("npc.king_roald") { startDialogue(it.npc) { kingRoald() } }
        onOpNpc1("npc.reldo") { startDialogue(it.npc) { reldo() } }
        onOpNpc1("npc.sir_prysin") { startDialogue(it.npc) { sirPrysin() } }
        onOpNpc1("npc.surok_surok") { startDialogue(it.npc) { surokMagis() } }
        onOpNpc1("npc.wgs_surok") { startDialogue(it.npc) { imprisonedSurok() } }
        onOpNpc1("npc.surok_annajones") { startDialogue(it.npc) { annaJones() } }
        onOpNpc1("npc.farming_gardener_tree_3_02") { startDialogue(it.npc) { treznor() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aeonisig_Raispher
    private suspend fun Dialogue.aeonisig() {
        chatNpc(
            neutral,
            "Please only talk to the King if it's important. He has a heavy burden to bear with " +
                "the running of his kingdom.",
        )
        var asked = false
        while (true) {
            val option =
                choice5(
                    "Who are you?",
                    1,
                    "What do you do here?",
                    2,
                    "Where did you come from?",
                    3,
                    "How did you come to be an advisor to King Roald?",
                    4,
                    if (asked) "I'd best be off." else "Fair enough.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "Apologies! Allow me to introduce myself. My name is Aeonisig Raispher, " +
                            "special advisor to King Roald on spiritual matters.",
                    )
                    chatPlayer(
                        confused,
                        "Special advisor on spiritual matters? What does that mean?",
                    )
                    chatNpc(
                        neutral,
                        "It means that some decisions the King has to make might have " +
                            "unforeseen repercussions on Misthalin's spiritual sensibilities. My " +
                            "duty is to ensure that Saradominist ideals are not stomped underfoot.",
                    )
                    chatPlayer(neutral, "I see...")
                }
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        neutral,
                        "My main function is to ensure that King Roald is apprised of all " +
                            "options, especially when it comes to any potential impacts on the " +
                            "righteous followers of Saradomin.",
                    )
                    chatPlayer(
                        confused,
                        "But surely the King should be able to make his own decisions on what's " +
                            "best for Misthalin?",
                    )
                    chatNpc(
                        laugh,
                        "What an interesting perspective you have! Totally unworkable of course, " +
                            "but interesting nonetheless.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Where did you come from?")
                    chatNpc(
                        neutral,
                        "I took my religious and combat training in several parts of the known " +
                            "world. I've also fought despicable beasts in the Wilderness in " +
                            "Saradomin's name.",
                    )
                    chatNpc(
                        happy,
                        "Needless to say, I have great experience in the ways of the world and " +
                            "am an invaluable aid to the King's decision-making process.",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "How did you come to be an advisor to King Roald?")
                    chatNpc(
                        neutral,
                        "The King of Misthalin, like any great leader, always requires the best " +
                            "advice and the best advisors.",
                    )
                    chatNpc(
                        neutral,
                        "He very often summons occasional advisors to help him in certain " +
                            "situations, but it was felt by the Church of Saradomin that a full " +
                            "time advisor on religious matters was needed.",
                    )
                    chatPlayer(
                        quiz,
                        "How come he doesn't have an advisor for any other religious " +
                            "denominations?",
                    )
                    chatNpc(
                        angry,
                        "Other religious denominations? The ruling of a kingdom is no place for " +
                            "pagan cults and their intolerable beliefs! We do things properly " +
                            "here, thank you very much.",
                    )
                }
                5 -> {
                    chatPlayer(neutral, if (asked) "I'd best be off." else "Fair enough.")
                    return
                }
            }
            asked = true
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ambassador_Ferrnook
    // TODO(content): [system:gnome_restaurant] food delivery
    private suspend fun Dialogue.ferrnook() {
        chatPlayer(quiz, "Hello Ambassador. Are you here visiting King Roald?")
        chatNpc(neutral, "Well, in theory, but he always seems to be busy.")
        chatPlayer(confused, "You don't seem that upset by that, though...")
        chatNpc(
            happy,
            "Oh no, I like travelling, and if you become a diplomat patience is a vital skill.",
        )
        chatPlayer(happy, "I'll try to remember that.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain
    // TODO(content): [system:npc_overhead] random shouts at the trainee guards
    private suspend fun Dialogue.captain() {
        chatNpc(angry, "What do you want, civilian?")
        val option =
            choice3(
                "What are you doing?",
                1,
                "Can I have some armour like yours?",
                2,
                "I want nothing.",
                3,
                title = SAY,
            )
        when (option) {
            1 -> captainTraining()
            2 -> captainArmour()
            3 -> {
                chatPlayer(neutral, "I want nothing.")
                chatNpc(angry, "Lucky you. Go away.")
            }
        }
    }

    private suspend fun Dialogue.captainTraining() {
        chatPlayer(quiz, "What are you doing?")
        chatNpc(
            angry,
            "What does it look like I'm doing? I'm training this useless bunch of recruits!",
        )
        val fit =
            choice2(
                "Oh yes, they look very fit.",
                true,
                "Oh yes, they look very weak.",
                false,
                title = SAY,
            )
        if (fit) {
            chatPlayer(happy, "Oh yes, they look very fit.")
            chatNpc(
                angry,
                "They're nothing but a useless bunch of weaklings who won't survive an hour " +
                    "once they're out on patrol.",
            )
        } else {
            chatPlayer(neutral, "Oh yes, they look very weak.")
            chatNpc(angry, "Show some respect when you talk about my men, civilian!")
        }
        val armour =
            choice2(
                "Can I have some armour like yours?",
                true,
                if (fit) "Oh yes, they look very weak." else "Oh yes, they look very fit.",
                false,
                title = SAY,
            )
        if (armour) {
            captainArmour()
            return
        }
        val earlier = if (fit) "fit" else "weak"
        chatNpc(
            angry,
            "Have you lost your mind? Earlier you said they looked $earlier! Just go away and " +
                "leave me alone.",
        )
    }

    private suspend fun Dialogue.captainArmour() {
        chatPlayer(quiz, "Can I have some armour like yours?")
        chatNpc(neutral, "Only guards of Varrock can wear this armour.")
        chatPlayer(happy, "Well, I could become a guard of Varrock.")
        // TODO(content): [verify:in_game] the transcript only covers 31+ Thieving; the lower-level path is missing
        if (player.baseThievingLvl >= 31) {
            chatNpc(
                angry,
                "Oh yeah? I see you've got a Thieving level of ${player.baseThievingLvl}. That " +
                    "must give you quite a long criminal record.",
            )
            chatPlayer(shifty, "I could repent of my evil ways if you like.")
        }
        chatNpc(neutral, "So you really want to be a guard, huh?")
        chatPlayer(happy, "Yes, that's right.")
        chatNpc(
            quiz,
            "Does it worry you that the life expectancy of a guard is about 30 seconds?",
        )
        val forget =
            choice2(
                "No, that doesn't worry me.",
                false,
                "Actually, forget it.",
                true,
                title = SAY,
            )
        if (forget) {
            chatPlayer(worried, "Actually, forget it.")
            chatNpc(angry, "Go away, coward.")
        } else {
            chatPlayer(happy, "No, that doesn't worry me.")
            chatNpc(angry, "Bah! You just failed the intelligence test. Go away, civilian.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Captain_Rovin
    // TODO(content): [quest:demon_slayer] "There's a demon who wants to invade this city." option
    private suspend fun Dialogue.captainRovin() {
        chatNpc(
            angry,
            "What are you doing up here? Only the palace guards are allowed up here.",
        )
        val option =
            choice3(
                "I am one of the palace guards.",
                1,
                "What about the King?",
                2,
                "Yes I know, but this is important.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "I am one of the palace guards.")
                chatNpc(angry, "No, you're not! I know all the palace guards.")
                val recruit =
                    choice2(
                        "I'm a new recruit.",
                        true,
                        "I've had extensive plastic surgery.",
                        false,
                    )
                if (recruit) {
                    chatPlayer(neutral, "I'm a new recruit.")
                    chatNpc(
                        angry,
                        "I interview all the new recruits. I'd know if you were one of them.",
                    )
                    chatPlayer(sad, "That blows that story out of the water then.")
                    chatNpc(angry, "Get out of my sight.")
                } else {
                    chatPlayer(neutral, "I've had extensive plastic surgery.")
                    chatNpc(
                        confused,
                        "What sort of surgery is that? I've never heard of it. Besides, you " +
                            "look reasonably healthy.",
                    )
                    chatNpc(angry, "Why is this relevant anyway? You still shouldn't be here.")
                }
            }
            2 -> {
                chatPlayer(quiz, "What about the King? Surely you'd let him up here.")
                chatNpc(
                    neutral,
                    "Well, yes, I suppose we'd let him up. He doesn't generally want to come up " +
                        "here, but if he did want to, he could.",
                )
                chatNpc(angry, "Anyway, you're not the King either. So get out of my sight.")
            }
            3 -> {
                chatPlayer(neutral, "Yes, I know, but this is important.")
                chatNpc(neutral, "Ok, I'm listening. Tell me what's so important.")
                val forgot =
                    choice2(
                        "Erm I forgot.",
                        true,
                        "The castle has just received its ale delivery.",
                        false,
                    )
                if (forgot) {
                    chatPlayer(confused, "Erm I forgot.")
                    chatNpc(angry, "Well it can't be that important then.")
                    chatPlayer(quiz, "How do you know?")
                    chatNpc(angry, "Just go away.")
                } else {
                    chatPlayer(happy, "The castle has just received its ale delivery.")
                    chatNpc(
                        neutral,
                        "Now that is important. However I'm the wrong person to speak to about " +
                            "it. Go talk to the kitchen staff.",
                    )
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garden_of_Tranquillity
    // https://oldschool.runescape.wiki/w/Transcript:Queen_Ellamaria (after the quest)
    // TODO(content): [quest:garden_of_tranquillity] start offer when the requirements are met
    //  and the quest dialogue
    private suspend fun Dialogue.queenEllamaria() {
        if (QuestRequirements.hasCompleted(player, GARDEN_OF_TRANQUILLITY)) {
            chatPlayer(quiz, "Can I use this garden for my own farming?")
            chatNpc(
                angry,
                "What, you want to dig up the plants that took one so long to grow? Don't be " +
                    "ridiculous, you silly little fool.",
            )
            chatPlayer(sad, "All right then, sorry I asked.")
            chatNpc(
                angry,
                "Impertinent peasant! Oh, if you promise to leave one alone then feel free to " +
                    "pick fruit from the White Tree. One is told that they are a great natural " +
                    "energy restorative.",
            )
            return
        }
        chatPlayer(
            quiz,
            "What's going on here? I see a lot of farming patches with nothing growing in them.",
        )
        chatNpc(
            happy,
            "No, one has just had them installed. One has had the most marvellous idea to bring " +
                "renewed happiness to one's own dearest husband.",
        )
        chatPlayer(confused, "One? I'm not sure I understand you-")
        chatNpc(
            angry,
            "Oh dear - the common classes, oh how they fill one with intolerable levels of " +
                "exasperation, I swear to be most true.",
        )
        chatPlayer(
            neutral,
            "Ri-ight ... if you say so, my lady. I'll be off then, if you don't mind-",
        )
        chatNpc(angry, "Yes, be off with you, before I call the guards.")
        chatPlayer(neutral, "No problem, sorry to have bothered you.")
        chatNpc(shocked, "Peasant - WAIT!!!")
        chatPlayer(quiz, "Are you speaking to me?")
        chatNpc(
            angry,
            "Who else would I be speaking to, you funny little fool? Of course you.",
        )
        chatPlayer(
            neutral,
            "Well for a moment there I thought there was a dog around here somewhere...",
        )
        chatNpc(
            neutral,
            "Oh yes, sarcasm - how amusing. No, no, peasant - I have a question to ask of you, " +
                "and you are required to answer.",
        )
        chatPlayer(neutral, "Well, seeing as you put it so nicely...")
        chatNpc(
            quiz,
            "Do you know of anyone who might have some degree of horticultural experience?",
        )
        chatPlayer(neutral, "Not really.")
        chatNpc(angry, "Well, hurry off then. You are of no use to me.")
        mesbox(
            "You do not meet all of the requirements to start the Garden of Tranquillity quest.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Roald (initial dialogue)
    // TODO(content): [system:met_flags] repeat-visit greeting needs a met flag (no King Roald varbit in the cache)
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.kingRoald() {
        if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            kingRoaldAfterBloodMoonRises()
            return
        }
        if (QuestRequirements.hasCompleted(player, PRIEST_IN_PERIL)) {
            kingRoaldAfterPriestInPeril()
            return
        }
        chatPlayer(happy, "Greetings, your majesty.")
        chatNpc(neutral, "Do you have anything of importance to say?")
        chatPlayer(neutral, "...Not really.")
        chatNpc(
            angry,
            "You will have to excuse me, then. I am very busy as I have a kingdom to run!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Roald (after Priest in Peril)
    private suspend fun Dialogue.kingRoaldAfterPriestInPeril() {
        chatNpc(neutral, "Yes, citizen. Do you need something?")
        if (choice2("About that job you gave me...", 1, "Not really.", 2) == 2) {
            chatPlayer(neutral, "Not really.")
            chatNpc(
                angry,
                "You will have to excuse me then. I am very busy as I have a kingdom to run!",
            )
            return
        }
        chatPlayer(neutral, "About that job you gave me...")
        chatNpc(
            happy,
            "Ah, yes, we recieved word from Drezel. I hear the Salve is safe once more. Very " +
                "good work!",
        )
        chatPlayer(
            quiz,
            "Thank you. Will you be sending some soldiers over to sort out those Zamorakians?",
        )
        chatNpc(
            neutral,
            "Yes, yes, we'll make sure they're dealt with. Might take a little while, mind you. " +
                "Lots of priorities to balance around here. I'm sure we'll have them gone before " +
                "the end of the year though.",
        )
        chatPlayer(neutral, "Right...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:King_Roald (after The Blood Moon Rises)
    private suspend fun Dialogue.kingRoaldAfterBloodMoonRises() {
        chatNpc(neutral, "Yes, citizen. Do you need something?")
        if (choice2("How are things with Morytania?", 1, "Not really.", 2) == 2) {
            chatPlayer(neutral, "...Not really.")
            chatNpc(
                angry,
                "You will have to excuse me, then. I am very busy as I have a kingdom to run!",
            )
            return
        }
        chatPlayer(quiz, "How are things with Morytania?")
        chatNpc(
            happy,
            "Couldn't be better! After all that fearmongering from you, citizen, it turned out " +
                "they actually wanted to be friends! Good job you're not the King.",
        )
        chatPlayer(confused, "I don't think that's quite...")
        chatNpc(happy, "Yes, I agree! It's very good indeed.")
        chatNpcSpecific(
            "Aeonisig Raispher",
            AEONISIG,
            worried,
            "I do still feel that we should reconsider this new alliance, my lord. Vampyres are " +
                "beasts of Zamorak!",
        )
        chatNpc(
            happy,
            "Well, you were very worried about those Guthixian edicts, Aeonisig. Surely with " +
                "this new alliance, we've managed to achieve the perfect balance those " +
                "Guthixians always wanted!",
        )
        chatNpcSpecific(
            "Aeonisig Raispher",
            AEONISIG,
            worried,
            "Yes, but the Church of Saradomin...",
        )
        chatNpc(
            happy,
            "Oh, don't worry yourself, Aeonisig. It's not like we'll be inviting any of them " +
                "over any time soon. No, this new arrangement is perfect. We can be all be " +
                "friends, but just from a safe and healthy distance!",
        )
        chatNpc(neutral, "Anyway, what was it you wanted, citizen?")
        chatPlayer(neutral, "You know, I think I'll actually be on my way...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reldo
    // TODO(content): [quest:the_lost_tribe] brooch dialogue
    // TODO(content): [quest:a_tail_of_two_cats] Robert the Strong dialogue
    // TODO(content): [quest:dragon_slayer_ii] Tristan Corvo and the census records
    // TODO(content): [system:wise_old_man_tasks] delivering the Wise Old Man's message
    // TODO(content): [system:treasure_trails] beginner hot and cold clue
    private suspend fun Dialogue.reldo() {
        val soaStarted =
            !QuestRequirements.hasNotCompleted(player, SHIELD_OF_ARRAV) ||
                QuestRequirements.isOnQuest(player, SHIELD_OF_ARRAV)
        if (vars[RELDO_MET] == 0 && !soaStarted) {
            vars[RELDO_MET] = 1
            chatNpc(neutral, "Hello, stranger.")
            chatPlayer(
                happy,
                "Hello there! The name's ${player.displayName}. Who are you?",
            )
            chatNpc(happy, "My name is Reldo. I'm the palace librarian.")
            chatPlayer(happy, "Ah, that explains why you're in the library then.")
            chatNpc(
                happy,
                "Well, truth be told, I would probably be here even if it wasn't where I " +
                    "worked. I very much like reading, not to mention sharing what I learn with " +
                    "all who are interested.",
            )
            chatPlayer(happy, "Sounds like a very noble goal!")
        } else {
            chatNpc(happy, "Hello there, ${player.displayName}.")
        }
        val imcando =
            QuestRequirements.isOnQuest(player, KNIGHTS_SWORD) ||
                QuestRequirements.isOnQuest(player, GIANT_DWARF)
        val option =
            menu(
                buildList {
                    if (!soaStarted) add("I'm in search of a quest." to 1)
                    if (imcando) add("What do you know about the Imcando dwarves?" to 4)
                    add("Do you have anything to trade?" to 2)
                    add("I'd better get going." to 3)
                }
            )
        when (option) {
            4 -> reldoImcando()
            1 -> {
                chatPlayer(quiz, "I'm in search of a quest.")
                chatNpc(neutral, "Hmm... I don't believe there are any here.")
                chatNpc(neutral, "Let me think actually...")
                chatNpc(
                    happy,
                    "Ah, yes. I think I have something, if you're definitely interested?",
                )
                // TODO(content): [quest:shield_of_arrav] start offer and the combat level warning
            }
            2 -> {
                chatPlayer(quiz, "Do you have anything to trade?")
                chatNpc(happy, "Only knowledge.")
                chatPlayer(quiz, "How much do you want for that then?")
                chatNpc(laugh, "No, sorry, that was just my little joke. I'm not the trading type.")
                chatPlayer(sad, "Ah well.")
            }
            3 -> {
                chatPlayer(neutral, "I'd better get going.")
                chatNpc(happy, "Until next time.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Reldo
    // (during The Knight's Sword or The Giant Dwarf)
    private suspend fun Dialogue.reldoImcando() {
        chatPlayer(quiz, "What do you know about the Imcando dwarves?")
        chatNpc(neutral, "The Imcando dwarves, you say? Let's see...")
        chatNpc(
            neutral,
            "Ah, yes... For many hundreds of years, they were the world's most skilled smiths. " +
                "They used secret smithing knowledge passed down from generation to generation.",
        )
        chatNpc(
            sad,
            "Unfortunately, about a century ago, the once thriving group was wiped out during " +
                "the barbarian invasions of that time.",
        )
        chatNpc(
            neutral,
            "I believe a few of them survived, but with the bulk of their population destroyed, " +
                "their numbers dwindled even further. If any are left now, they'll no doubt keep " +
                "to themselves.",
        )
        chatNpc(
            neutral,
            "However, I have heard tales of their love for redberry pie. Perhaps one of those " +
                "could convince one to reveal their true heritage.",
        )
        chatPlayer(confused, "So I just go around offering random dwarves redberry pie?")
        chatNpc(
            neutral,
            "Well, not totally random. Apparently some of the surviving Imcando made a home for " +
                "themselves near the cliffs on the Asgarnian southern peninsula. That would be a " +
                "good place to start.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Demon_Slayer (Sir Prysin)
    // https://oldschool.runescape.wiki/w/Transcript:Sir_Prysin (after Demon Slayer)
    // TODO(content): [quest:demon_slayer] "Aris said I should come and talk to you."
    // TODO(content): [system:treasure_trails] hard clue
    private suspend fun Dialogue.sirPrysin() {
        if (QuestRequirements.hasCompleted(player, DEMON_SLAYER)) {
            sirPrysinAfterQuest()
            return
        }
        chatNpc(neutral, "Hello, who are you?")
        val mighty =
            choice2(
                "I am a mighty adventurer. Who are you?",
                true,
                "I'm not sure, I was hoping you could tell me.",
                false,
            )
        if (mighty) {
            chatPlayer(happy, "I am a mighty adventurer, who are you?")
            chatNpc(happy, "I am Sir Prysin. A bold and famous knight of the realm.")
        } else {
            chatPlayer(confused, "I was hoping you could tell me.")
            chatNpc(neutral, "Well I've never met you before.")
        }
    }

    private suspend fun Dialogue.sirPrysinAfterQuest() {
        chatNpc(happy, "Hello. I've heard you stopped the demon, well done.")
        val ownsSilverlight =
            SILVERLIGHTS.any { it in player.inv || it in player.worn || it in access.bank }
        if (ownsSilverlight) {
            silverlightThanks()
            return
        }
        val lost =
            choice2(
                "Yes, that's right.",
                false,
                "Yes, although I'm afraid I've lost Silverlight.",
                true,
            )
        if (!lost) {
            silverlightThanks()
            return
        }
        chatPlayer(sad, "Yes, although I'm afraid I've lost Silverlight.")
        chatNpc(
            angry,
            "Yes, news of your carelessness is almost as widespread as knowledge of your " +
                "victory. Fortunately for you, Silverlight has come back into my possession.",
        )
        // https://oldschool.runescape.wiki/w/Darklight: reclaimed for 1,000 coins after
        // Shadow of the Storm.
        if (!QuestRequirements.hasCompleted(player, SHADOW_OF_THE_STORM)) {
            // TODO(content): [quest:shadow_of_the_storm] reclaiming Silverlight for 500 coins
            //  before the quest (missing from the transcript)
            return
        }
        val borrow =
            choice2(
                "Phew, that's a relief.",
                false,
                "Is there any chance of me borrowing it again?",
                true,
            )
        if (!borrow) {
            chatPlayer(happy, "Phew, that's a relief.")
            return
        }
        chatPlayer(quiz, "Is there any chance of me borrowing it again?")
        chatNpc(
            angry,
            "I'm not going to give it away that easily again, it's far too important to be " +
                "treated so disrespectfully. Especially now its powers have increased after your " +
                "fight with Agrith-Naar!",
        )
        chatNpc(
            neutral,
            "If you wish to make use of Silverlight again, it will cost you 1000 gold pieces. " +
                "Maybe that will encourage you to look after it.",
        )
        val pay = choice2("No way, it's not worth that much.", false, "Ok, I'll pay.", true)
        if (!pay) {
            chatPlayer(angry, "No way, it's not worth that much.")
            return
        }
        chatPlayer(neutral, "Ok, I'll pay.")
        if (player.inv.count(COINS) < DARKLIGHT_FEE) {
            chatPlayer(sad, "But I don't have that much money.")
            return
        }
        if (!buyDarklight()) {
            chatNpc(
                angry,
                "You don't even have enough free space to carry it. Stop wasing my time!",
            )
            return
        }
        chatNpc(happy, "May you make good use of it.")
    }

    private suspend fun Dialogue.silverlightThanks() {
        chatPlayer(happy, "Yes, that's right.")
        chatNpc(happy, "A good job well done then.")
        chatPlayer(happy, "Thank you.")
    }

    private fun Dialogue.buyDarklight(): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = DARKLIGHT_FEE
                }
                insert {
                    into = inv
                    obj = DARKLIGHT.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    // https://oldschool.runescape.wiki/w/Transcript:Surok_Magis (after While Guthix Sleeps)
    private suspend fun Dialogue.imprisonedSurok() {
        chatPlayer(happy, "How are you enjoying your cell, Surok?")
        chatNpc(angry, "You let me out of here! This isn't over...")
        chatPlayer(happy, "It's nice to see a little justice around here!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Surok_Magis
    // TODO(content): [system:met_flags] repeat-visit dialogue ("Ummm...") needs a met flag
    // TODO(content): [quest:what_lies_below] quest dialogue
    private suspend fun Dialogue.surokMagis() {
        chatPlayer(neutral, "Excuse me...")
        chatNpc(
            angry,
            "What do you want? ...Oh, wait. I know! You're probably just like all the others, " +
                "aren't you? After some fancy spell or potion from me, I bet!",
        )
        chatPlayer(
            confused,
            "What? No! At least, I don't think so. What sort of spells do you have?",
        )
        chatNpc(
            laugh,
            "Hah! I knew it! I expect you want my Aphro-Dizzy-Yak spell! Want someone to fall " +
                "madly in love with you, eh?",
        )
        chatPlayer(
            worried,
            "That spell sounds very interesting, but I didn't mean to disturb you!",
        )
        chatNpc(
            neutral,
            "Well, I see that you do have some manners. I'm glad to see that you use them.",
        )
        chatNpc(
            neutral,
            "Now, if it's all the same, I am very busy at the moment. Unless you want something " +
                "specific, please come back another time.",
        )
        chatPlayer(happy, "Yes, of course. Goodbye!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anna_Jones (before What Lies Below)
    // TODO(content): [quest:what_lies_below] statue excavation and the lines once the tunnel is
    //  open (varbit.surok_tunnelcheck)
    private suspend fun Dialogue.annaJones() {
        if (QuestRequirements.hasCompleted(player, WHAT_LIES_BELOW)) {
            annaJonesAfterQuest()
            return
        }
        chatNpc(neutral, "Yes? Can I help you?")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "What are you doing here?",
                    2,
                    "Who does this statue represent?",
                    3,
                    "Okay, I'd better go.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        angry,
                        "Well, now. Do you always go around asking about people like that? It's " +
                            "very rude, you know.",
                    )
                    chatPlayer(worried, "Sorry! I didn't mean to pry!")
                    chatNpc(
                        happy,
                        "That's alright. My name is Louisiana Jones, although most people call " +
                            "me Anna. I'm an archaeologist.",
                    )
                    chatPlayer(quiz, "Oh. Do you work for the Varrock Museum?")
                    chatNpc(
                        laugh,
                        "Hah! No. I used to, but now I prefer to work freelance for independent " +
                            "employers.",
                    )
                    chatPlayer(neutral, "I see.")
                }
                2 -> {
                    chatPlayer(quiz, "What are you doing here?")
                    chatNpc(neutral, "I'm investigating something for someone.")
                    chatPlayer(confused, "That doesn't really explain anything!")
                    chatNpc(neutral, "I never said it would.")
                    chatPlayer(quiz, "So what are you investigating?")
                    chatNpc(neutral, "I'm afraid I can't say.")
                    chatPlayer(quiz, "Okay, so who are you investigating it for?")
                    chatNpc(neutral, "I'm afraid I can't say that either.")
                    chatPlayer(quiz, "Okay, so is there anything you CAN say?")
                    chatNpc(
                        happy,
                        "I can say that I can't say anything about what you want me to say!",
                    )
                    chatPlayer(confused, "Okay, I think you've said enough!")
                }
                3 -> {
                    chatPlayer(quiz, "Who does this statue represent?")
                    chatNpc(
                        happy,
                        "That, my dear, is the statue of the great god Saradomin himself. Stand " +
                            "and admire in awe, for you are in the presence of greatness!",
                    )
                    chatPlayer(shifty, "...Yay Zamorak...!")
                    chatNpc(shocked, "WHAT?!?!")
                    chatPlayer(worried, "Uh...I said I've lost my anorak!")
                    chatNpc(neutral, "I see. I'm afraid I haven't seen it.")
                }
                4 -> {
                    chatPlayer(neutral, "Okay, I'd better go.")
                    chatNpc(happy, "Okay, then!")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Anna_Jones (after What Lies Below)
    private suspend fun Dialogue.annaJonesAfterQuest() {
        chatNpc(
            happy,
            "Ah. You must be ${player.displayName}, right? I have a bronze pickaxe here for you!",
        )
        // TODO(content): [verify:in_game] whether she hands over a pickaxe on every visit, and
        //  full-inventory behaviour (drops it)
        player.invAddOrDrop(objRepo, BRONZE_PICKAXE)
        chatPlayer(happy, "Thanks very much!")
        chatNpc(
            neutral,
            "My employer, Surok Magis, sent word to me that you may come to use the tunnel. You " +
                "will need something to help you get in there. The pickaxe I've given to you " +
                "should help.",
        )
        chatPlayer(neutral, "Uh, thanks.")
        chatNpc(happy, "Okay, then. The tunnel awaits...")
        while (true) {
            val option =
                choice5(
                    "What tunnel?",
                    1,
                    "Sorry, who are you?",
                    2,
                    "Who does this statue represent?",
                    3,
                    "What are you doing here?",
                    4,
                    "Okay, I'd better go.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What tunnel?")
                    chatNpc(
                        neutral,
                        "Why, the Chaos Tunnel of course! I imagine Surok will have told you of " +
                            "it.",
                    )
                    chatPlayer(quiz, "Oh, I see. Well, what can you tell me about it?")
                    chatNpc(
                        neutral,
                        "Well, story and rumour has it that the Dagon'hai built a tunnel here " +
                            "under the statue of Saradomin that would allow them to visit the " +
                            "Chaos Altar without having to go through the Wilderness.",
                    )
                    chatPlayer(happy, "That's extremely handy!")
                    chatNpc(
                        sad,
                        "Yes, it is. Or, at least, it would be. Unfortunately, I haven't been " +
                            "able to get in there so far.",
                    )
                    chatPlayer(quiz, "Why not?")
                    chatNpc(
                        neutral,
                        "The tunnel entrance needs to be cleared before anyone can get in there. " +
                            "However, my Mining skill isn't high enough at all to do it myself. " +
                            "Perhaps you could have a go?",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Sorry, who are you?")
                    chatNpc(
                        happy,
                        "My name is Louisiana Jones, although most people call me Anna. I'm an " +
                            "archaeologist.",
                    )
                    chatPlayer(happy, "Pleased to meet you.")
                    chatNpc(happy, "Likewise!")
                }
                3 -> {
                    chatPlayer(quiz, "Who does this statue represent?")
                    chatNpc(
                        happy,
                        "That, my dear, is the statue of the great god Saradomin himself. Stand " +
                            "and admire in awe for you are in the presence of greatness!",
                    )
                    chatPlayer(happy, "Consider me awed!")
                    chatNpc(happy, "Indeed!")
                }
                4 -> {
                    chatPlayer(quiz, "What are you doing here?")
                    chatNpc(
                        neutral,
                        "Well story and rumour has it that the Dagon'hai built a tunnel under " +
                            "this statue of Saradomin that would allow them to visit the Chaos " +
                            "Altar without having to go through the Wilderness.",
                    )
                    chatPlayer(happy, "That's extremely handy!")
                    chatNpc(
                        sad,
                        "Yes, it is. Or, at least, it would be. I came to excavate the tunnel " +
                            "but, unfortunately,  I haven't been able to get in there so far.",
                    )
                    chatPlayer(quiz, "Why not?")
                    chatNpc(
                        neutral,
                        "The tunnel entrance needs to be cleared before anyone can get in there. " +
                            "However, my Mining skill just isn't what it used to be. Perhaps you " +
                            "could have a go?",
                    )
                    chatPlayer(neutral, "Maybe. I might do.")
                }
                5 -> {
                    chatPlayer(neutral, "Okay, I'd better go.")
                    chatNpc(happy, "Okay, then!")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Treznor
    private suspend fun Dialogue.treznor() {
        while (true) {
            val option =
                choice4(
                    "Would you look after my crops for me?",
                    1,
                    "Can you give me any farming advice?",
                    2,
                    "Can you sell me something?",
                    3,
                    "I'll come back another time.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Would you look after my crops for me?")
                    // TODO(content): [skill:farming] tree patch protection payment
                    chatNpc(
                        neutral,
                        "You don't have a sapling planted in that patch. Plant one and I might " +
                            "agree to look after it for you.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Can you give me any farming advice?")
                    treznorAdvice()
                }
                3 -> {
                    chatPlayer(quiz, "Can you sell me something?")
                    chatNpc(
                        neutral,
                        "That depends on whether I have it to sell. What is it that you're " +
                            "looking for?",
                    )
                    // TODO(content): [skill:farming] Farming Supplies interface
                    return
                }
                4 -> {
                    chatPlayer(neutral, "I'll come back another time.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.treznorAdvice() {
        when (access.random.of(17)) {
            0 ->
                chatNpc(
                    neutral,
                    "You can buy all the farming tools from farming shops, which can be found " +
                        "close to the allotments.",
                )
            1 ->
                chatNpc(
                    neutral,
                    "Applying compost to a patch will not only reduce the chance that your " +
                        "crops will get diseased, but you will also grow more crops to harvest.",
                )
            2 ->
                chatNpc(
                    neutral,
                    "Supercompost is far better than normal compost, but more expensive to " +
                        "make. You need to rot the right type of item; show me an item, and " +
                        "I'll tell you if it's super-compostable or not.",
                )
            3 ->
                chatNpc(
                    neutral,
                    "Don't just throw away your weeds after you've raked a patch - put them in " +
                        "a compost bin and make some compost.",
                )
            4 ->
                chatNpc(
                    neutral,
                    "The only way to cure a bush or tree of disease is to prune away the " +
                        "diseased leaves with a pair of secateurs. For all other crops I would " +
                        "just apply some plant-cure.",
                )
            5 ->
                chatNpc(
                    neutral,
                    "Vegetables, hops and flowers need constant watering - if you ignore my " +
                        "advice, you will sooner or later find yourself in possession of a dead " +
                        "farming patch.",
                )
            6 ->
                chatNpc(
                    neutral,
                    "There is a special patch for growing Belladonna - I believe that it is " +
                        "somewhere near Draynor Manor, where the ground is a tad 'unblessed'.",
                )
            7 ->
                chatNpc(
                    neutral,
                    "Bittercap mushrooms can only be grown in a special patch in Morytania, " +
                        "near the Mort Myre swamp. There the ground is especially dank and " +
                        "suited to growing poisonous fungii.",
                )
            8 ->
                chatNpc(
                    neutral,
                    "You can put up to five tomatoes, strawberries, apples, bananas or oranges " +
                        "into a fruit basket, although you can't have a mix in the same basket.",
                )
            9 ->
                chatNpc(
                    neutral,
                    "You can put up to ten potatoes, cabbages or onions in vegetable sacks, " +
                        "although you can't have a mix in the same sack.",
                )
            10 ->
                chatNpc(
                    neutral,
                    "If you want to make your own sacks and baskets you'll need to use the loom " +
                        "that's near the Farming shop in Falador. If you're good enough at " +
                        "crafting, that is.",
                )
            11 ->
                chatNpc(
                    neutral,
                    "Tree seeds must be grown in a plantpot of soil into a sapling, and then " +
                        "transferred to a tree patch to continue growing to adulthood.",
                )
            12 ->
                chatNpc(
                    neutral,
                    "You can fill plantpots with soil from any empty patch, if you have a " +
                        "gardening trowel.",
                )
            13 ->
                chatNpc(
                    neutral,
                    "You don't have to buy all your plantpots you know, you can make them " +
                        "yourself on a pottery wheel. If you're good enough at crafting, that is.",
                )
            14 ->
                chatNpc(
                    neutral,
                    "If you want to grow fruit trees you could try a few places: Catherby, " +
                        "Brimhaven, Gnome Stronghold and the Farming Guild all have fruit tree " +
                        "patches.",
                )
            15 ->
                chatNpc(
                    neutral,
                    "Hops are good for brewing ales. I believe there's a brewery up in " +
                        "Keldagrim somewhere, and I've heard rumours that a place called " +
                        "Phasmatys used to be good for that type of thing. 'Fore they all died, " +
                        "of course.",
                )
            else -> {
                chatNpc(
                    neutral,
                    "There are many Farming areas out there. I know of Elstan's area near " +
                        "Falador, Dantaera's area near Catherby, Kragen's area near Ardougne, " +
                        "Lyra's area in Morytania, Marisi's area in Hosidius and Alan's area in",
                )
                chatNpc(
                    neutral,
                    "the Farming Guild. That said, I'm sure there's plenty more that I don't " +
                        "know about.",
                )
            }
        }
    }

    private companion object {
        const val RELDO_MET = "varbit.reldo_met"
        const val AEONISIG = "npc.myq3_aeonisig_roalds_advisor"
        const val GARDEN_OF_TRANQUILLITY = "quest_gardenoftranquillity"
        const val PRIEST_IN_PERIL = "quest_priestinperil"
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
        const val SHIELD_OF_ARRAV = "quest_shieldofarrav"
        const val KNIGHTS_SWORD = "quest_knightssword"
        const val GIANT_DWARF = "quest_giantdwarf"
        const val DEMON_SLAYER = "quest_demonslayer"
        const val SHADOW_OF_THE_STORM = "quest_shadowofthestorm"
        const val WHAT_LIES_BELOW = "quest_whatliesbelow"
        const val COINS = "obj.coins"
        const val DARKLIGHT = "obj.darklight"
        const val DARKLIGHT_FEE = 1_000
        const val BRONZE_PICKAXE = "obj.bronze_pickaxe"
        val SILVERLIGHTS =
            listOf(
                "obj.silverlight",
                "obj.agrith_silverlight_dyed",
                "obj.darklight",
                "obj.arclight",
                "obj.arclight_inactive",
                "obj.emberlight",
            )
        const val SAY = "What would you like to say?"
    }
}
