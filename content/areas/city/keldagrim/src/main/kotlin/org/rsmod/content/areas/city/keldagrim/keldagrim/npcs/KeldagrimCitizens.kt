package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KeldagrimCitizens @Inject constructor(private val objRepo: ObjRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.aluft_gnome_inventor") { startDialogue(it.npc) { manglethorp() } }
        onOpNpc1("npc.dwarf_city_drunken_dwarf") { startDialogue(it.npc) { drunkenDwarf() } }
        onOpNpc1("npc.dwarfrock_engineer1") { startDialogue(it.npc) { dwarvenEngineer() } }
        onOpNpc1("npc.vc_the_beast") { startDialogue(it.npc) { theBeast() } }
        onOpNpc1("npc.vc_smokin_joe") { startDialogue(it.npc) { smokinJoe() } }
        onOpNpc1("npc.dwarf_city_boatman_city") { startDialogue(it.npc) { cityBoatman() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Professor_Manglethorp
    // TODO(content): [system:gnome_restaurant] delivery dialogue
    private suspend fun Dialogue.manglethorp() {
        val title = if (access.isBodyTypeA()) "man" else "woman"
        chatNpc(
            neutral,
            "Fascinating, fascinating. It's remarkable what these young dwarves have managed " +
                "to create. Oh, hello young $title.",
        )
        chatPlayer(quiz, "Hello, what's a gnome like you doing in a place like this?")
        chatNpc(
            happy,
            "Ah, well I have been sent by King to talk with the dwarves here and find out more " +
                "about their recent advances in technology.",
        )
        chatPlayer(quiz, "Found anything interesting?")
        chatNpc(
            happy,
            "The rideable carts are a very handy way to get to mining sites, and the Blast " +
                "Furnace is a magnificent beast of a machine.",
        )
        chatPlayer(happy, "I'll have a look, thanks.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Drunken_Dwarf_(Keldagrim)
    // TODO(content): [system:met_flags] later visits open with "What are you doing here anyway?"
    // TODO(content): [system:treasure_trails] hard clue puzzle box dialogue
    private suspend fun Dialogue.drunkenDwarf() {
        chatPlayer(quiz, "Hello there! Are you alright?")
        chatNpc(drunk, "Of courshe! Why why why *hic* why shouldn't I be?")
        chatPlayer(
            confused,
            "Hey, you look vaguely familiar... haven't I seen you somewhere before?",
        )
        chatNpc(drunk, "That'sh not likely... I've been here for eh...")
        chatNpc(drunk, "I've been here for...")
        chatNpc(drunk, "Yearsh!")
        chatNpc(
            drunk,
            "I've been looking after thish houshe for my... my coushin? Or wash it my brother? " +
                "A family member anyway.",
        )
        chatPlayer(quiz, "And where is this... family member of yours?")
        chatNpc(
            drunk,
            "All over, all over... he shaish he went off to wander all over Gielinor. Don't " +
                "remember why.",
        )
        chatPlayer(neutral, "Hmm... I wonder, I may have met him...")
        chatNpc(drunk, "He comesh back sometimesh... saysh he needsh more kebabsh.")
        chatNpc(
            drunk,
            "Or ish that jusht a dream? Sometimesh I dream kebabsh invade Keldi... Keldu... " +
                "Keldashomething.",
        )
        chatNpc(
            drunk,
            "When you eatsh them, they take over your mind, they do! An army of mindlesh kebab " +
                "eating dwarvesh!",
        )
        chatPlayer(worried, "Er, yes, well, I think I should be off now.")
        chatNpc(drunk, "THE KEBABSH WILL COME FOR YOU!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dwarven_Engineer
    private suspend fun Dialogue.dwarvenEngineer() {
        chatNpc(angry, "I wish you'd just leave me alone.")
        chatPlayer(quiz, "Why, what's wrong?")
        chatNpc(sad, "I was a great engineer once. Still am actually.")
        chatNpc(sad, "They just don't let me do the work I do best anymore.")
        chatPlayer(quiz, "What do you do then?")
        chatNpc(sad, "I'm an assistant in this shop. I get all the lousy jobs too.")
        chatPlayer(sad, "Well, I'm sorry to hear that.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Beast
    private suspend fun Dialogue.theBeast() {
        chatPlayer(quiz, "Hello, The Beast! Why do they call you that?")
        chatNpc(neutral, "...")
        chatPlayer(worried, "Oh, I see.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Smokin%27_Joe
    private suspend fun Dialogue.smokinJoe() {
        if (QuestRequirements.hasCompleted(player, RATCATCHERS)) {
            smokinJoeAfterQuest()
            return
        }
        chatPlayer(happy, "Hello!")
        chatPlayer(quiz, "Hello?")
        chatPlayer(angry, "Hey!")
        mesbox("Smokin' Joe ignores you.")
    }

    // TODO(content): [verify:in_game] the top-level options (the transcript lists three)
    private suspend fun Dialogue.smokinJoeAfterQuest() {
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
            1 -> whereToFind()
            2 -> morePits()
            3 -> {
                chatPlayer(quiz, "Can you tell me where I can find the rat pits in Keldagrim?")
                chatNpc(
                    neutral,
                    "Down the stairs into the basement of the 'Laughing Miner'. A grand " +
                        "waterin' hole it is. Just look for The Beast, you'll find it.",
                )
            }
        }
    }

    private suspend fun Dialogue.whereToFind() {
        val option =
            choice5(
                "Grimesquit and Phingspet",
                1,
                "Jimmy Dazzler",
                2,
                "Hooknosed Jack",
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
                    "Ah, the sisters Grime, as strange a pair as you're ever likely to meet.",
                )
                chatNpc(neutral, "You'll find them in the sewers of Varrock.")
            }
            2 -> {
                chatPlayer(quiz, "Can you tell me where I could find Jimmy Dazzler?")
                chatNpc(neutral, "Ah, young Jimmy, he must be a made man by now.")
                chatNpc(neutral, "I remember when he was a lad...")
                chatPlayer(quiz, "Do you remember where to find him, too?")
                chatNpc(angry, "Hrmph! His old waterin' hole was the Flying Horse in Ardougne.")
            }
            3 -> {
                chatPlayer(quiz, "Can you tell me where I could find Hooknosed Jack?")
                chatNpc(neutral, "That old brawler? Is he still knockin' about?")
                chatNpc(
                    neutral,
                    "Well, your best bet would be somewhere on the east side of Varrock. It's " +
                        "rough around there though, be careful.",
                )
            }
            4 -> {
                chatPlayer(quiz, "Can you tell me where I could find 'The Face'?")
                chatNpc(neutral, "You'll find her in Port Sarim.")
            }
            5 -> {
                chatPlayer(quiz, "Can you tell me where I could find Felkrash?")
                chatNpc(
                    neutral,
                    "Ah, the boss herself. You'll probably find her in the Port Sarim rat pits.",
                )
            }
        }
    }

    private suspend fun Dialogue.morePits() {
        chatPlayer(quiz, "Can you tell me more about the pits?")
        if (RATPITS_MANUAL !in player.inv) {
            chatNpc(neutral, "Here take this, you might find it useful.")
            player.invAddOrDrop(objRepo, RATPITS_MANUAL)
            objbox(RATPITS_MANUAL, "You are handed the rat pits manual.")
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
            if (option == 1) pitPointers() else pitBenefits()
            chatPlayer(happy, "Thanks for your help.")
            chatNpc(neutral, "Do you have any other questions about the pits?")
            if (!choice2("I'd like to ask you...", true, "No thanks", false)) {
                chatPlayer(neutral, "No thanks.")
                return
            }
        }
    }

    private suspend fun Dialogue.pitPointers() {
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
            val veryDefensive =
                choice2(
                    "I think I would adopt a very defensive strategy.",
                    true,
                    "I think I would adopt a less defensive strategy.",
                    false,
                )
            if (veryDefensive) {
                chatPlayer(neutral, "I think I would adopt a very defensive strategy")
                chatNpc(
                    neutral,
                    "In that case you should tell your cat: 'Just take care of yourself, cat.'",
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
        chatPlayer(
            neutral,
            "I am willing to risk my cat, if it increases my chances of victory.",
        )
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
        val veryAggressive =
            choice2(
                "I think I would adopt a very aggressive strategy.",
                true,
                "I think I would adopt a less aggressive strategy.",
                false,
            )
        if (veryAggressive) {
            chatPlayer(neutral, "I think I would adopt a very aggressive strategy.")
            chatNpc(neutral, "In that case you should tell your cat: 'Go berserk!'")
        } else {
            chatPlayer(neutral, "I think I would adopt a less aggressive strategy.")
            chatNpc(neutral, "In that case you should tell your cat: 'No guts, no glory!'")
        }
    }

    private suspend fun Dialogue.pitBenefits() {
        chatPlayer(quiz, "What benefits does my cat get from training in the pits?")
        chatNpc(neutral, "It depends on what kind of cat you have.")
        chatNpc(
            neutral,
            "In the Keldagrim pits, your cat will grow wiser by gaining more experience.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dwarven_Boatman (Keldagrim)
    private suspend fun Dialogue.cityBoatman() {
        chatNpc(quiz, "Want me to take you back to the mines?")
        val choice = choice2("Yes, please take me.", 1, "What, on your ship! No thanks!", 2)
        if (choice == 1) {
            chatPlayer(happy, "Yes, please take me.")
            // TODO(content): [system:keldagrim_boat_travel] sail to the Keldagrim entrance mines
            return
        }
        chatPlayer(
            shocked,
            "What, on your ship? No thanks! I don't want to crash into any more things!",
        )
        chatNpc(neutral, "Hey now, it was only a slight accident!")
        if (QuestRequirements.hasCompleted(player, GIANT_DWARF)) {
            chatPlayer(
                angry,
                "A slight accident?? Have you any idea how much time I spent rebuilding that " +
                    "statue?",
            )
        } else {
            chatPlayer(
                angry,
                "A slight accident?? Thanks to you I have to rebuild this statue you so " +
                    "carelessly knocked over!",
            )
        }
        chatNpc(neutral, "Calm down, calm down! You got what you paid for the trip, didn't you?")
    }

    private companion object {
        const val RATCATCHERS = "quest_ratcatchers"
        const val GIANT_DWARF = "quest_giantdwarf"
        const val RATPITS_MANUAL = "obj.ratpits_manual"
    }
}
