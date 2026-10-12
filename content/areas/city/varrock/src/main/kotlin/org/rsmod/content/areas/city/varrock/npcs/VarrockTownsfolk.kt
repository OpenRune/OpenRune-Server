package org.rsmod.content.areas.city.varrock.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VarrockTownsfolk @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.jonny_the_beard") { startDialogue(it.npc) { jonnyTheBeard() } }
        onOpNpc1("npc.vc_hooknosed_jack") { startDialogue(it.npc) { hooknosedJack() } }
        onOpNpc1("npc.varrock_granny_1") { startDialogue(it.npc) { iffie() } }
        onOpNpc1("npc.artist2") { startDialogue(it.npc) { daVinci() } }
        onOpNpc1("npc.gambler2") { startDialogue(it.npc) { chancy() } }
        onOpNpc1("npc.drunk2") { startDialogue(it.npc) { hops() } }
        onOpNpc1("npc.aris") { startDialogue(it.npc) { aris() } }
        onOpNpc1("npc.juliet") { startDialogue(it.npc) { juliet() } }
        onOpNpc1("npc.draul_leptoc") { startDialogue(it.npc) { draulLeptoc() } }
        onOpNpc1("npc.phillipa") { startDialogue(it.npc) { phillipa() } }
        onOpNpc1("npc.tailor_boutique") { startDialogue(it.npc) { suzie() } }
        onOpNpc1("npc.25th_host") { startDialogue(it.npc) { wizardGary() } }
        onOpNpc1("npc.pmod_town_crier_varrock") { startDialogue(it.npc) { townCrier() } }
        onOpNpc1("npc.johnathon_fitzharmon") { startDialogue(it.npc) { johnathon() } }
        onOpNpc1("npc.con_contractor_varrock") { startDialogue(it.npc) { marlo() } }
        onOpNpc1("npc.con_contract_client_1") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_2") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.con_contract_client_3") { startDialogue(it.npc) { mahoganyClient() } }
        onOpNpc1("npc.fai_varrock_jollyboar_chef") { startDialogue(it.npc) { jollyBoarCook() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jonny_the_Beard
    private suspend fun Dialogue.jonnyTheBeard() {
        chatNpc(angry, "Whatever you want, I'm not interested!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hooknosed_Jack
    // TODO(content): [quest:ratcatchers] "Step away from that manhole..." before the quest
    private suspend fun Dialogue.hooknosedJack() {
        if (QuestRequirements.hasCompleted(player, RATCATCHERS)) {
            hooknosedJackAfterQuest()
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "What do you want?")
        chatNpc(angry, "Actually, I've got no time for this. I don't want to talk to you.")
    }

    private suspend fun Dialogue.hooknosedJackAfterQuest() {
        val option =
            choice3(
                "Can you tell me where I would find ....",
                1,
                "Can you tell me more about the pits?",
                2,
                "Can you show me to the rat pits?",
                3,
            )
        when (option) {
            1 -> jackWhereToFind()
            2 -> jackPits()
            3 -> {
                chatPlayer(quiz, "Can you show me to the rat pits?")
                chatNpc(
                    angry,
                    "Didn't you hear me? They're through that manhole over there. You can't miss " +
                        "it!",
                )
            }
        }
    }

    private suspend fun Dialogue.jackWhereToFind() {
        val option =
            choice5(
                "Grimesquit and Phingspet",
                1,
                "Jimmy Dazzler",
                2,
                "Smokin' Joe",
                3,
                "The Face",
                4,
                "Felkrash",
                5,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you tell me where I could find Phingspet and Grimesquit?")
                chatNpc(
                    neutral,
                    "The sisters Grime? You'll find them down in the sewers here in Varrock.",
                )
            }
            2 -> {
                chatPlayer(quiz, "Can you tell me where I could find Jimmy Dazzler?")
                chatNpc(angry, "Pah! There's nothing else he can teach you, I'll tell you that.")
                chatPlayer(quiz, "But where would I find him?")
                chatNpc(neutral, "Probably cosying up to the nobles over in Ardougne.")
                chatPlayer(quiz, "What's your problem with Jimmy, anyway?")
                chatNpc(angry, "He reckons he's too good for the likes of us.")
                chatPlayer(quiz, "Are you sure? He spoke very highly of you.")
                chatNpc(neutral, "I'll believe it when I see it.")
                chatNpc(neutral, "Anyway. The Flying Horse, Ardougne. You'll find him there.")
            }
            3 -> {
                chatPlayer(quiz, "Can you tell me where I could find Smokin' Joe?")
                chatNpc(neutral, "He should be in Keldagrim, the city of the dwarves.")
                chatNpc(happy, "Tell him I said hello, will you? I've been meaning to catch up.")
            }
            4 -> {
                chatPlayer(quiz, "Can you tell me where I could find 'The Face'?")
                chatNpc(
                    neutral,
                    "She'll be in Port Sarim, scaring people away from the entrance to the pits.",
                )
            }
            5 -> {
                chatPlayer(quiz, "Can you tell me where I could find Felkrash?")
                chatNpc(
                    neutral,
                    "Her nibs? She'll be in the rat pits at Port Sarim. Good luck getting in " +
                        "there, though.",
                )
            }
        }
    }

    private suspend fun Dialogue.jackPits() {
        chatPlayer(quiz, "Can you tell me more about the pits?")
        if (RAT_PITS_MANUAL !in player.inv) {
            chatNpc(neutral, "Here take this, you might find it useful.")
            // TODO(content): [verify:in_game] full-inventory behaviour (drops the manual)
            player.invAddOrDrop(objRepo, RAT_PITS_MANUAL)
            objbox(RAT_PITS_MANUAL, "You are handed the rat pits manual.")
        }
        chatNpc(neutral, "What do you want to know?")
        while (true) {
            val option =
                choice2(
                    "Do you have any pointers for me and my cat?",
                    1,
                    "What benefits does my cat get from training in the pits?",
                    2,
                )
            if (option == 1) {
                jackCatPointers()
            } else {
                chatPlayer(quiz, "What benefits does my cat get from training in the pits?")
                chatNpc(neutral, "It depends on what kind of cat you have.")
                chatNpc(
                    neutral,
                    "In the Varrock pits, your cat will grow up faster by gaining more experience.",
                )
            }
            chatPlayer(happy, "Thanks for your help.")
            chatNpc(neutral, "Do you have any other questions about the pits?")
            if (choice2("I'd like to ask you...", 1, "No thanks", 2) == 2) {
                chatPlayer(neutral, "No thanks.")
                return
            }
        }
    }

    private suspend fun Dialogue.jackCatPointers() {
        chatPlayer(quiz, "Do you have any pointers for me and my cat?")
        chatNpc(
            neutral,
            "You should wear an Amulet of Catspeak. It will allow you to organise tactics with " +
                "your cat.",
        )
        chatPlayer(quiz, "What would be the best tactic to adopt then?")
        chatNpc(
            neutral,
            "Well, that depends on your outlook, whether you're ready to risk your cat's life or " +
                "not and how defensive or aggressive you want your cat to be.",
        )
        val risk =
            choice2(
                "I will not risk my cat's life.",
                false,
                "I am willing to risk my cat if it improves my chances.",
                true,
            )
        if (!risk) {
            chatPlayer(neutral, "I will not risk my cat's life.")
            chatNpc(neutral, "Well if that's the case then you're left with two choices.")
            chatNpc(
                neutral,
                "You can adopt an ultra defensive strategy, which increases your cat's hitpoints " +
                    "and defence but causes it to flee sooner.",
            )
            chatNpc(
                neutral,
                "Or you can adopt a less defensive strategy, which will allow your cat to stay " +
                    "in the pit for longer.",
            )
            val very =
                choice2(
                    "I think I would adopt a very defensive strategy.",
                    true,
                    "I think I would adopt a less defensive strategy.",
                    false,
                )
            if (very) {
                chatPlayer(neutral, "I think I would adopt a very defensive strategy")
                chatNpc(
                    neutral,
                    "In that case you should tell your cat: 'Just take care of yourself cat.'",
                )
            } else {
                chatPlayer(neutral, "I think I would adopt a less defensive strategy.")
                chatNpc(
                    neutral,
                    "In that case you should tell your cat: 'Stay in for as long as you can.'",
                )
            }
            return
        }
        chatPlayer(neutral, "I am willing to risk my cat, if it increases my chances of victory.")
        chatNpc(neutral, "In that case you are left with two choices.")
        chatNpc(
            neutral,
            "You could go for all out attack which will boost your cat's attack and strength, " +
                "but weaken it.",
        )
        chatNpc(
            neutral,
            "Or, go for a less aggressive stance, which will increase your cat's strength and " +
                "attack to a lesser degree, but carries no defensive penalties.",
        )
        val very =
            choice2(
                "I think I would adopt a very aggressive strategy.",
                true,
                "I think I would adopt a less aggressive strategy.",
                false,
            )
        if (very) {
            chatPlayer(neutral, "I think I would adopt a very aggressive strategy")
            chatNpc(neutral, "In that case you should tell your cat: 'Go berserk!'")
        } else {
            chatPlayer(neutral, "I think I would adopt a less aggressive strategy.")
            chatNpc(neutral, "In that case you should tell your cat: 'No guts, no glory!'")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Iffie (2021 Halloween event not completed)
    // TODO(content): [event:halloween_2021] "Hello Iffie, remember our adventure?"
    private suspend fun Dialogue.iffie() {
        chatPlayer(happy, "Hello Iffie!")
        chatNpc(
            neutral,
            "Sorry, dearie, if I stop to chat I'll lose count. Talk to my sister instead; she " +
                "likes to chat. You'll find her upstairs in the church.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Da_Vinci
    // TODO(content): [quest:biohazard] only in the Dancing Donkey after Biohazard
    private suspend fun Dialogue.daVinci() {
        chatNpc(
            angry,
            "Bah! A great artist such as myself should not have to suffer the HUMILIATION of " +
                "spending time where the likes of you wander everywhere!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chancy (Varrock, members world)
    // TODO(content): [quest:biohazard] only in the Dancing Donkey after Biohazard
    private suspend fun Dialogue.chancy() {
        chatPlayer(happy, "Good morning.")
        chatNpc(angry, "Leave me alone. I'm trying to find my gambling buddies!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hops_(Biohazard) (members world)
    // TODO(content): [quest:biohazard] only in the Dancing Donkey after Biohazard
    private suspend fun Dialogue.hops() {
        chatNpc(drunk, "Hops don't wanna talk now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Demon_Slayer (starting Demon Slayer)
    // https://oldschool.runescape.wiki/w/Transcript:Aris
    // (after Demon Slayer and Recipe for Disaster)
    // TODO(content): [quest:demon_slayer] fortune telling and start, and in-progress dialogue
    // TODO(content): [verify:in_game] which line wins once both quests are complete
    private suspend fun Dialogue.aris() {
        if (QuestRequirements.hasCompleted(player, RECIPE_FOR_DISASTER)) {
            chatPlayer(
                confused,
                "There's just one thing I don't understand about that whole time travel " +
                    "culinaromancer thing...",
            )
            chatNpc(quiz, "Oh yes? What was that?")
            chatPlayer(confused, "Everything!")
            chatNpc(
                neutral,
                "I wouldn't worry about it too much if I were you. The world is safe once more " +
                    "thanks to your assistance.",
            )
            chatNpc(neutral, "That's all that really matters.")
            chatPlayer(sad, "Yeah, I guess... I just wish things made sense is all...")
            return
        }
        if (QuestRequirements.hasCompleted(player, DEMON_SLAYER)) {
            arisAfterDemonSlayer()
            return
        }
        chatNpc(
            happy,
            "Hello young one. Cross my palm with silver and the future will be revealed to you.",
        )
        if ("obj.coins" !in player.inv) {
            chatPlayer(sad, "Oh dear. I don't have any money.")
        }
    }

    private suspend fun Dialogue.arisAfterDemonSlayer() {
        chatNpc(happy, "Greetings young one.")
        chatNpc(happy, "You're a hero now. That was a good bit of demonslaying.")
        val option =
            choice3(
                "How do you know I killed it?",
                1,
                "Thanks.",
                2,
                "Stop calling me that!",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "How do you know I killed it?")
                chatNpc(happy, "You forget. I'm good at knowing things.")
            }
            2 -> chatPlayer(happy, "Thanks.")
            3 -> {
                chatPlayer(angry, "Stop calling me that!")
                chatNpc(neutral, "In the scheme of things you are very young.")
                val next =
                    choice2(
                        "Ok but how old are you?",
                        1,
                        "Oh if it's in the scheme of things that's ok.",
                        2,
                    )
                if (next == 1) {
                    chatPlayer(quiz, "Ok, but how old are you?")
                    chatNpc(
                        neutral,
                        "Count the number of legs on the stools in the Blue Moon inn, and " +
                            "multiply that number by seven.",
                    )
                    chatPlayer(confused, "Er, yeah, whatever.")
                } else {
                    chatPlayer(happy, "Oh if it's in the scheme of things that's ok.")
                    chatNpc(happy, "You show wisdom for one so young.")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Juliet
    // TODO(content): [quest:romeo_and_juliet] quest dialogue
    private suspend fun Dialogue.juliet() {
        if (QuestRequirements.hasCompleted(player, ROMEO_AND_JULIET)) {
            chatNpc(sad, "I sat in that cold crypt for ages waiting for Romeo.")
            chatNpc(angry, "That useless fool never showed up.")
            chatNpc(angry, "And all I got was indigestion. I am done with men like him.")
            chatNpc(angry, "Now go away before I call my father!")
            return
        }
        chatNpc(sad, "Romeo, Romeo, wherefore art thou Romeo?")
        mesbox("She seems to be lost in thought.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Draul_Leptoc
    private suspend fun Dialogue.draulLeptoc() {
        if (QuestRequirements.hasCompleted(player, ROMEO_AND_JULIET)) {
            chatNpc(angry, "I suppose you're quite pleased with yourself now...")
            return
        }
        chatNpc(
            angry,
            "What are you doing in my house...why the impertinence...the sheer cheek...how dare " +
                "you violate my personal lodgings....",
        )
        chatPlayer(worried, "I..I was just looking around....")
        chatNpc(
            angry,
            "Well get out! Get out....this is my house....and don't go near my daughter " +
                "Juliet...she's grounded in her room to keep her away from that good for nothing " +
                "Romeo.",
        )
        chatPlayer(worried, "Yes....sir....")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Phillipa
    // TODO(content): [quest:romeo_and_juliet] quest dialogue
    private suspend fun Dialogue.phillipa() {
        if (QuestRequirements.hasCompleted(player, ROMEO_AND_JULIET)) {
            chatNpc(
                happy,
                "Hello, I seem to be getting on really well with that dashing lovely dovely, " +
                    "Romeo. Pity about poor Juliet - she missed out there I reckon!",
            )
            return
        }
        chatPlayer(quiz, "Hello, who are you?")
        chatNpc(
            happy,
            "Hi, I'm Phillipa, Juliet's cousin. I like to keep an eye on her, make sure that " +
                "dashing young Romeo doesn't just steal her away under our plain old noses!",
        )
        chatNpc(
            happy,
            "He'd do it, you know - he's ever so dashing, and cavalier, in a wet blanket sort of " +
                "way.",
        )
        chatPlayer(quiz, "Romeo? Where would I find him then?")
        chatNpc(
            happy,
            "Well, that's a good question! Who knows where his head's at most of the time? In " +
                "the clouds, most likely!",
        )
        chatNpc(
            happy,
            "But he's probably chasing the ladies who frequent Varrock market. He does like a " +
                "bit of kiss chase, so I've heard!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Suzie
    private suspend fun Dialogue.suzie() {
        chatPlayer(happy, "Hello!")
        chatNpc(silent, "...")
        chatPlayer(quiz, "Is everything okay?")
        chatNpc(shifty, "*mumble mumble*")
        chatPlayer(confused, "I didn't catch that.")
        chatNpc(silent, "...")
        chatPlayer(neutral, "Right... Well, have a nice day.")
        chatNpc(shifty, "*mumble*")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wizard_Gary
    // TODO(content): [event:25th_anniversary] dialogue after helping open the Realm of Memories
    private suspend fun Dialogue.wizardGary() {
        chatNpc(happy, "Hello. What brings you here?")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "If you're a wizard, are you doing anything magical?",
                    2,
                    "Bye, Gary.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm Gary, the wizard. I've not actually been qualified for very long, " +
                            "so I'm living here near the rune shop so I can keep practicing the " +
                            "magical arts.",
                    )
                    chatNpc(
                        neutral,
                        "I seem to be raising a cat, too, ever since some local lady persuaded " +
                            "me to buy one off her.",
                    )
                    val next =
                        choice3(
                            "Is the cat supposed to help you get runes too?",
                            1,
                            "If you're a wizard, are you doing anything magical?",
                            2,
                            "Bye, Gary.",
                            3,
                        )
                    when (next) {
                        1 -> {
                            chatPlayer(quiz, "Is the cat supposed to help you get runes too?")
                            chatNpc(
                                angry,
                                "No! I know there are places where people think it's normal to " +
                                    "swap cats for runes, but that will NOT happen to Sookie!",
                            )
                        }
                        2 -> wizardGaryMagic()
                        3 -> {
                            chatPlayer(neutral, "Bye, Gary.")
                            return
                        }
                    }
                }
                2 -> wizardGaryMagic()
                3 -> {
                    chatPlayer(neutral, "Bye, Gary.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.wizardGaryMagic() {
        chatPlayer(quiz, "If you're a wizard, are you doing anything magical?")
        chatNpc(
            happy,
            "Always! But I'm not looking for any assistance from passing adventurers, if that's " +
                "what you were thinking. Maybe another time.",
        )
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

    // https://oldschool.runescape.wiki/w/Transcript:Johnathon
    // TODO(content): [quest:family_pest] chaos gauntlets after Family Pest
    private suspend fun Dialogue.johnathon() {
        if (!QuestRequirements.hasCompleted(player, FAMILY_CREST)) {
            chatNpc(sad, "I am so very tired... Leave me be... to rest...")
            return
        }
        chatNpc(happy, "Hello again. My family now considers you a hero.")
        chatPlayer(happy, "Maybe because... I am.")
        chatNpc(neutral, "Yes well... Is there anything I can help you with?")
        if (choice2("Can you change my gauntlets for me?", 1, "No thanks.", 2) == 2) {
            chatPlayer(neutral, "No thanks.")
            return
        }
        chatPlayer(quiz, "Can you change my gauntlets for me?")
        // https://oldschool.runescape.wiki/w/Chaos_gauntlets: steel gauntlets are enchanted for
        // free, gauntlets that already carry an enchantment cost 25,000 coins.
        if (STEEL_GAUNTLETS in player.inv) {
            // TODO(content): [verify:in_game] steel gauntlets dialogue (not in the transcript)
            if (enchantChaosGauntlets(STEEL_GAUNTLETS, fee = 0)) {
                mesbox(GAUNTLET_CHANT)
            }
            return
        }
        val enchanted = ENCHANTED_GAUNTLETS.firstOrNull { it in player.inv }
        if (enchanted == null) {
            // TODO(content): [verify:in_game] the reply without changeable gauntlets
            return
        }
        chatNpc(neutral, "Yes certainly, though it will cost you 25,000 coins.")
        if (choice2("Great thanks!", 1, "No that's okay thanks.", 2) == 2) {
            chatPlayer(neutral, "No that's okay thanks.")
            return
        }
        chatPlayer(happy, "Great thanks!")
        if (enchantChaosGauntlets(enchanted, GAUNTLET_CHANGE_FEE)) {
            mesbox(GAUNTLET_CHANT)
        }
        // TODO(content): [verify:in_game] the reply when the player can't pay the fee
    }

    private fun Dialogue.enchantChaosGauntlets(gauntlets: String, fee: Int): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                if (fee > 0) {
                    delete {
                        from = inv
                        obj = "obj.coins".asRSCM(RSCMType.OBJ)
                        strictCount = fee
                    }
                }
                delete {
                    from = inv
                    obj = gauntlets.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inv
                    obj = CHAOS_GAUNTLETS.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    // https://oldschool.runescape.wiki/w/Transcript:Marlo
    // TODO(content): [system:mahogany_homes] contracts after joining the company
    private suspend fun Dialogue.marlo() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Bob_(Mahogany_Homes)
    // https://oldschool.runescape.wiki/w/Transcript:Jeff_(Mahogany_Homes)
    // https://oldschool.runescape.wiki/w/Transcript:Sarah_(Mahogany_Homes)
    // TODO(content): [system:mahogany_homes] contract requests and the after-contract cup of tea
    private suspend fun Dialogue.mahoganyClient() {
        chatNpc(neutral, "Please excuse me, I'm rather busy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cook_(Jolly_Boar_Inn)
    // TODO(content): [system:met_flags] "I distinctly remember..." greeting after asking to be cooked something
    private suspend fun Dialogue.jollyBoarCook() {
        chatNpc(angry, "What do you want? I'm busy!")
        val option =
            choice3(
                "Can you cook me something?",
                1,
                "Why do you work here?",
                2,
                "Can you tell me any good jokes?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Can you cook me something?")
                chatNpc(angry, "What? No! Go away.")
                chatPlayer(
                    confused,
                    "Well, you're meant to be a cook. Why can't you cook something for me?",
                )
                chatNpc(angry, "I don't want to, that's why! Now, leave me alone.")
                chatPlayer(sad, "You're not a very nice man.")
                chatNpc(angry, "Yet you keep talking to me!")
                chatPlayer(neutral, "Good point. I should stop that.")
                chatNpc(angry, "Yes, do us both a favour.")
            }
            2 -> {
                chatPlayer(quiz, "Why do you work here?")
                chatNpc(
                    angry,
                    "Why do you think I work in a disreputable inn filled with rough scoundrels " +
                        "on the very edge of the Wilderness? I work here because I need the job!",
                )
                chatPlayer(quiz, "Do you like it?")
                chatNpc(
                    sad,
                    "Like it? It's sucked the warmth and humanity out of me, and I've become a " +
                        "hollow shell holding nothing but anger, misery and gourmet recipes.",
                )
                chatPlayer(neutral, "Oh.")
                chatNpc(
                    angry,
                    "What did you expect to hear? I suppose you expected me to tell you an " +
                        "inspiring story about how working here can teach you to find hope in " +
                        "the darkest places, or how to see the best in everything?",
                )
                chatPlayer(neutral, "Maybe you should think about moving to a different inn.")
                chatNpc(
                    angry,
                    "Bah! I'm not letting it defeat me. Now, let me concentrate on my cooking.",
                )
                chatPlayer(neutral, "Okay.")
            }
            3 -> {
                chatPlayer(quiz, "Can you tell me any good jokes?")
                chatNpc(neutral, "Ohhh, if you insist...")
                chatNpc(happy, "What did the half-wit say to the cook?")
                chatPlayer(confused, "I don't know.")
                val pronoun = if (access.isBodyTypeA()) "He" else "She"
                chatNpc(laugh, "$pronoun said 'Can you tell me any good jokes?' Got ya! Hahahaha!")
                chatPlayer(bored, "Whatever.")
            }
        }
    }

    private companion object {
        const val RATCATCHERS = "quest_ratcatchers"
        const val ROMEO_AND_JULIET = "quest_romeoandjuliet"
        const val DEMON_SLAYER = "quest_demonslayer"
        const val RECIPE_FOR_DISASTER = "quest_recipefordisaster"
        const val FAMILY_CREST = "quest_familycrest"
        const val RAT_PITS_MANUAL = "obj.ratpits_manual"
        const val STEEL_GAUNTLETS = "obj.steel_gauntlets"
        const val CHAOS_GAUNTLETS = "obj.gauntlets_of_chaos"
        const val GAUNTLET_CHANGE_FEE = 25_000
        const val GAUNTLET_CHANT =
            "Johnathon takes your gauntlets from you, and begins a low chant over them. You see " +
                "them begin to glow and sparkle, before he returns them to you."
        val ENCHANTED_GAUNTLETS =
            listOf("obj.gauntlets_of_goldsmithing", "obj.gauntlets_of_cooking")
    }
}
