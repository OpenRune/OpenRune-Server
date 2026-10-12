package org.rsmod.content.slayer.guardians

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.content.slayer.core.SlayerTaskManager
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SlayerAreaGuardians : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wyvern_cave_pieve") { startDialogue(it.npc) { pieve() } }
        onOpNpc1("npc.karam_dungeon_metaldragonguardian") { startDialogue(it.npc) { hieve() } }
        onOpNpc1("npc.smoke_devil_guardian") { startDialogue(it.npc) { breive() } }
        onOpNpc1("npc.fossil_slayerdungeonguard") { startDialogue(it.npc) { weve() } }
        onOpNpc1("npc.slayer_kurask_troll") { startDialogue(it.npc) { jelly() } }
        onOpNpc1("npc.slayer_kraken_cove_lieve") { startDialogue(it.npc) { lieve() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pieve
    private suspend fun Dialogue.pieve() {
        chatNpc(happy, "Welcome to my private little wyvern area.")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "Why is this wyvern area private?",
                    2,
                    "So are you a Slayer Master?",
                    3,
                    "Thanks, goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm Pieve. My big brother Steve made this training area, but he's gone " +
                            "off to be a Slayer Master to replace our cousin Nieve, so this " +
                            "area's mine now.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Why is this wyvern area private?")
                    chatNpc(
                        neutral,
                        "My brother, Steve, carved the steps to get up here. He was jealous of " +
                            "our cousin, Nieve, because she'd got a huge training cave where she " +
                            "could boss people around. So he made this place.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "So are you a Slayer Master?")
                    chatNpc(
                        neutral,
                        "No, I'm not. My brother Steve is a Slayer Master. He took over the job " +
                            "from our cousin Nieve. All he has to do is tell people to kill " +
                            "stuff - he says it's easy!",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Thanks, goodbye.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hieve
    private suspend fun Dialogue.hieve() {
        chatNpc(quiz, "Hey, down there! What is it?")
        while (true) {
            val option =
                choice3(
                    "Why are you here?",
                    1,
                    "Can I kill the dragons in here?",
                    2,
                    "I'll be off.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Why are you here?")
                    chatNpc(
                        happy,
                        "For the dragons, of course! I've always wanted to slay dragons. I've " +
                            "found they're a bit tough, though, even with all this kit I've got, " +
                            "but I'll slay them one day.",
                    )
                    if (QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)) {
                        chatNpc(
                            angry,
                            "My little brother became a Slayer Master recently. That job should " +
                                "have been mine - he's just a little brat! But one day I'll be a " +
                                "mighty dragon-slayer, and they'll give me a job like that too.",
                        )
                    } else {
                        chatNpc(
                            neutral,
                            "My little cousin is a Slayer Master, lucky girl. But one day I'll " +
                                "be a mighty dragon-slayer, and they'll give me a job like that " +
                                "too.",
                        )
                    }
                }
                2 -> {
                    chatPlayer(quiz, "Can I kill the dragons in here?")
                    val task = SlayerTaskManager.getCurrentSlayerTask(access)
                    if (task != null && task.id in METAL_DRAGON_TASKS) {
                        chatNpc(happy, "Yes, certainly, since you've been assigned to do so.")
                        return
                    }
                    chatNpc(
                        neutral,
                        "No, you haven't got an assignment to slay these dragons. Go and train " +
                            "somewhere else.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'll be off.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Breive
    private suspend fun Dialogue.breive() {
        chatNpc(neutral, "*cough* whatcha want? *hack*")
        when (
            choice3(
                "What is this place?",
                1,
                "Are you alright?",
                2,
                "I think I'll leave you alone.",
                3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    neutral,
                    "Smoke dungeon. *wheeze* Got smoke devils in it. For killing. *splutter*",
                )
                val relative =
                    if (QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)) {
                        "My nephew Steve"
                    } else {
                        "My niece Nieve"
                    }
                chatNpc(
                    neutral,
                    "$relative *hack hack* is one of those Slayer Masters. You know, they " +
                        "tell people what to go kill. *cough* I look after this cave for people " +
                        "on assignments like that.",
                )
                breive()
            }
            2 -> breiveAlright()
            3 -> breiveLeave()
        }
    }

    private suspend fun Dialogue.breiveAlright() {
        chatPlayer(quiz, "Are you alright?")
        chatNpc(
            angry,
            "Of course I'm *wheeze* alright. I'm fed up with you young folk asking *cough cough* " +
                "if I'm alright just because I like a bit of smoke *splutter* in my tubes.",
        )
        chatNpc(
            angry,
            "Don't you come lecturing me on *cough* air quality, young man - I've breathed smoke " +
                "all my life, man and boy and *euuurgh* and there's no *ghhuk* scientific " +
                "evidence that it's harmful.",
        )
        when (choice2("Are you alright?", 1, "I think I'll leave you alone.", 2)) {
            1 -> breiveAlright()
            2 -> breiveLeave()
        }
    }

    private suspend fun Dialogue.breiveLeave() {
        chatPlayer(neutral, "I think I'll leave you alone.")
        chatNpc(neutral, "*cough cough cough ghhuk*")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Weve
    private suspend fun Dialogue.weve() {
        chatPlayer(quiz, "Hello, what's a little boy like you doing down here?")
        chatNpc(
            angry,
            "I ain't no little boy. Me dad, Steve, said I was big enough to take care of these " +
                "'ere wyverns, as the pal- palint-palintililiolog- those blokes from the museum " +
                "are too scared of them.",
        )
        while (true) {
            val option =
                choice4(
                    "Calm down, I was just worried.",
                    1,
                    "Why are you here?",
                    2,
                    "Who are you?",
                    3,
                    "Ok, bye.",
                    4,
                )
            when (option) {
                1 ->
                    chatNpc(
                        neutral,
                        "Yeah, whatever. I know how to look after myself, my entire family are " +
                            "slayer guardians or something like that.",
                    )
                2 -> {
                    chatNpc(neutral, "To guard the wyverns of course.")
                    chatPlayer(quiz, "Guard them? From what?")
                    chatNpc(angry, "From people like you who just want to kill 'em.")
                    chatPlayer(confused, "But... that's what they're for?")
                    chatNpc(
                        neutral,
                        "They have lives too. They were created for a purpose. But whatever, you " +
                            "can only kill these ones if the Slayer Masters say you can.",
                    )
                }
                3 -> {
                    chatNpc(happy, "I'm Weve, son of Steve.")
                    chatPlayer(confused, "That's... an interesting name.")
                    chatNpc(
                        happy,
                        "Well, it's kinda a family tradition you might say. Plus I'm good at " +
                            "looking after wyverns and fast enough to weave between their legs " +
                            "and not get hurt.",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Ok, bye.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jelly_(mountain_troll)
    private suspend fun Dialogue.jelly() {
        chatNpc(happy, "Welcome to Jelly's Kurask cave.")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "Why is this area private?",
                    2,
                    "What kind of a name is Jelly?",
                    3,
                    "Thanks, goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(happy, "I am Jelly.")
                    chatNpc(
                        happy,
                        "When Jelly was just a baby troll, parents left him in Slayer cave... " +
                            "But Jelly is strong, Jelly learn to survive in the cave, and one " +
                            "day Jelly will become great Slayer master!",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Why is this area private?")
                    chatNpc(
                        neutral,
                        "When the Kurasks finded new treasures, many humans come to the Slayer " +
                            "cave to hunt them... No Kurasks left for Jelly to train on, so " +
                            "Jelly makes a private area just for training. These be Jelly's very " +
                            "own Kurask!",
                    )
                    chatNpc(
                        neutral,
                        "Not just any human gets to train up here, you has to be on a Kurask " +
                            "Slayer task.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What kind of a name is Jelly?")
                    chatNpc(
                        sad,
                        "Jelly grew up in this cave, there not much choice around for a hungry " +
                            "troll...",
                    )
                }
                else -> {
                    chatPlayer(neutral, "Thanks, goodbye.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lieve_McCracken
    private suspend fun Dialogue.lieve() {
        chatNpc(
            drunk,
            "*hic* G'day! Do ya... ... well, do ya need any help with yer trident? *buuurrrp*",
        )
        var option =
            choice4(
                "Bit early to be drinking, isn't it?",
                1,
                "Who are you?",
                2,
                "So, what's in the cave?",
                3,
                "Let's talk about tridents.",
                4,
            )
        while (true) {
            when (option) {
                1 -> {
                    lieveDrinking()
                    option = lieveMenu()
                }
                2 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        drunk,
                        "What!? Why I'm the infamous pirate Lieve McCracken! You've not heard of " +
                            "me?",
                    )
                    chatPlayer(neutral, "Hmm, can't say I have.")
                    chatNpc(
                        drunk,
                        "I'm wanted in several *hic* kingdoms - murder, armed robbery and even " +
                            "*hic* flag desecration!",
                    )
                    chatPlayer(quiz, "Shouldn't you be in hiding?")
                    chatNpc(drunk, "I'm sorry, but *hic* do ya see anyone here?")
                    chatPlayer(
                        quiz,
                        "I guess you could call this hiding. If you're some famous pirate, " +
                            "where's you ship?",
                    )
                    chatNpc(
                        drunk,
                        "*hic* My last expedition didn't end too well for my boat... or crew " +
                            "*hic*.",
                    )
                    option =
                        choice4(
                            "So, what's in the cave?",
                            3,
                            "Bit early to be drinking, isn't it?",
                            1,
                            "Let's talk about tridents.",
                            4,
                            "I'll be, heh, leaving you now...",
                            5,
                        )
                }
                3 -> {
                    chatPlayer(quiz, "So, what's in the cave?")
                    chatNpc(
                        drunk,
                        "The fruits o' my labour... and some waterfiends, those were a surprise " +
                            "*hic* to be sure, but a welcome one.",
                    )
                    chatPlayer(confused, "Umm, fruits?")
                    chatNpc(
                        drunk,
                        "Oh, not *hic* actual fruits, silly! A relative o' mine wanted a cave " +
                            "full o' Krakens, so here *hic* I am.",
                    )
                    chatPlayer(quiz, "And why are you so drunk?")
                    chatNpc(
                        sad,
                        "Well, the Krakens destroyed m' ship, killed m' crew, the rum it's... " +
                            "all I 'ave left.",
                    )
                    chatPlayer(quiz, "Sorry about that, so... Can I kill some Krakens?")
                    chatNpc(
                        drunk,
                        "If you've been assigned some by a slayer master, I don't see *hic* why " +
                            "not.",
                    )
                    chatPlayer(quiz, "And if I havn't been?")
                    chatNpc(drunk, "Then ya can leave m' Krakens *hic* alone.")
                    chatPlayer(neutral, "Umm... ok then.")
                    option =
                        choice4(
                            "Who are you?",
                            2,
                            "Bit early to be drinking, isn't it?",
                            1,
                            "Let's talk about tridents.",
                            4,
                            "I'll be, heh, leaving you now...",
                            5,
                        )
                }
                4 -> {
                    lieveTridents()
                    option =
                        choice3(
                            "So, what's in the cave?",
                            3,
                            "Who are you?",
                            2,
                            "I'll be, heh, leaving you now...",
                            5,
                        )
                }
                else -> {
                    chatPlayer(neutral, "I'll be, heh, leaving you now...")
                    chatNpc(drunk, "Wow, so original.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.lieveMenu(): Int =
        choice5(
            "Bit early to be drinking, isn't it?",
            1,
            "Who are you?",
            2,
            "So, what's in the cave?",
            3,
            "Let's talk about tridents.",
            4,
            "I'll be, heh, leaving you now...",
            5,
        )

    private suspend fun Dialogue.lieveDrinking() {
        chatPlayer(quiz, "Bit early to be drinking, isn't it?")
        chatNpc(drunk, "Why?! What time *hic* is it?")
        chatPlayer(confused, "Hmm, I've no idea.")
        chatNpc(drunk, "I can drink to that!")
    }

    private suspend fun Dialogue.lieveTridents() {
        chatPlayer(neutral, "Let's talk about tridents.")
        val stored = vars[TENTACLES_STORED]
        val intro =
            "If ya brings me a trident, I can enhance it for ya. It'll store 20,000 charges, not " +
                "just 2,500."
        when {
            stored == 0 -> {
                chatNpc(drunk, "$intro But I wants payin' fer the job.")
                chatNpc(
                    drunk,
                    "Give me 10 kraken tentacles first. I'll hold onto 'em for ya. Then ya can " +
                        "bring me a trident to enhance, an' I'll get to work for ya.",
                )
            }
            stored == 1 -> {
                chatNpc(
                    drunk,
                    "$intro Ya brought me one tentacle so far, but I'll want 10 of 'em to do the " +
                        "job.",
                )
                chatNpc(
                    drunk,
                    "Just pass me 9 more tentacles, then give me yer trident an' I'll get to " +
                        "work for ya.",
                )
            }
            stored == 9 -> {
                chatNpc(
                    drunk,
                    "$intro Ya brought me 9 tentacles so far, but I'll want 10 of 'em to do the " +
                        "job.",
                )
                chatNpc(
                    drunk,
                    "Just pass me one more tentacle, then give me yer trident an' I'll get to " +
                        "work for ya.",
                )
            }
            stored < TENTACLES_NEEDED -> {
                chatNpc(
                    drunk,
                    "$intro Ya brought me $stored tentacles so far, but I'll want 10 of 'em to " +
                        "do the job.",
                )
                chatNpc(
                    drunk,
                    "Just pass me ${TENTACLES_NEEDED - stored} more tentacles, then give me yer " +
                        "trident an' I'll get to work for ya.",
                )
            }
            else -> {
                chatNpc(
                    drunk,
                    "$intro Ya brought me $stored tentacles; I'll use up 10 of 'em to do the job.",
                )
                chatNpc(drunk, "Just pass me yer trident an' I'll get to work for ya.")
            }
        }
    }

    private companion object {
        const val TENTACLES_STORED = "varbit.kraken_cove_tentaclesstored"
        const val TENTACLES_NEEDED = 10
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"

        // Bronze, iron and steel dragons in `dbtable.slayer_task`.
        val METAL_DRAGON_TASKS = setOf(58, 59, 60)
    }
}
