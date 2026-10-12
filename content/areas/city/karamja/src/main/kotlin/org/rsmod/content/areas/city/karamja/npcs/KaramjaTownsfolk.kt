package org.rsmod.content.areas.city.karamja.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KaramjaTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.luthas") { startDialogue(it.npc) { luthas() } }
        onOpNpc1("npc.bas_rebecca") { startDialogue(it.npc) { rebecca() } }
        onOpNpc1("npc.garv") { startDialogue(it.npc) { garv() } }
        onOpNpc1("npc.grubor") { startDialogue(it.npc) { grubor() } }
        onOpNpc1("npc.kangai_mau") { startDialogue(it.npc) { kangaiMau() } }
        onOpNpc1("npc.tbwt_lubufu") { startDialogue(it.npc) { lubufu() } }
        onOpNpc1("npc.handsand_sandy0") { startDialogue(it.npc) { sandy() } }
        onOpNpc1("npc.handsand_sandy1") { startDialogue(it.npc) { sandy() } }
        onOpNpc1("npc.tribesman_doorman") { startDialogue(it.npc) { saniboch() } }
        onOpNpc1("npc.brimhavencartdriver") { startDialogue(it.npc) { hajedy() } }
        onOpNpc1("npc.shilofurnaceowner") { startDialogue(it.npc) { yohnus() } }
        onOpNpc1("npc.mosol_rei") { startDialogue(it.npc) { mosolRei() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Luthas
    // TODO(content): [quest:pirates_treasure] banana crate job, its progress and the payment
    // TODO(content): [system:treasure_trails] medium clue reward
    private suspend fun Dialogue.luthas() {
        chatNpc(happy, "Hello I'm Luthas, I run the banana plantation here.")
        val employment =
            choice2(
                "Could you offer me employment on your plantation?",
                true,
                "That customs officer is annoying isn't she?",
                false,
            )
        if (employment) {
            chatPlayer(quiz, "Could you offer me employment on your plantation?")
            chatNpc(
                happy,
                "Yes, I can sort something out. There's a crate ready to be loaded onto the ship.",
            )
            chatNpc(
                neutral,
                "You wouldn't believe the demand for bananas from Wydin's shop over in Port " +
                    "Sarim. I think this is the third crate I've shipped him this month..",
            )
            chatNpc(happy, "If you could fill it up with bananas, I'll pay you 30 gold.")
            return
        }
        chatPlayer(quiz, "That customs officer is annoying isn't she?")
        chatNpc(neutral, "Well I know her pretty well. She doesn't cause me any trouble any more.")
        chatNpc(
            neutral,
            "She doesn't even search my export crates any more. She knows they only contain " +
                "bananas.",
        )
        chatPlayer(quiz, "Really? How interesting. Whereabouts do you send those to?")
        chatNpc(
            neutral,
            "There is a little shop over in Port Sarim that buys them up by the crate. I believe " +
                "it is run by a man called Wydin.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rebecca (before Burial at Sea)
    private suspend fun Dialogue.rebecca() {
        chatPlayer(happy, "Hello there!")
        chatNpc(neutral, "Hello. Can I help you?")
        chatPlayer(quiz, "Just wondering what you're up to?")
        chatNpc(neutral, "Right now, not a lot. I'm just here for a drink or two.")
        chatPlayer(happy, "Well, in that case, enjoy!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garv
    // TODO(content): [quest:heroes_quest] quest dialogue
    private suspend fun Dialogue.garv() {
        chatNpc(neutral, "Hello. What do you want?")
        if (choice2("Can I go in there?", true, "I want for nothing!", false)) {
            chatPlayer(quiz, "Can I go in there?")
            chatNpc(angry, "No. In there is private.")
        } else {
            chatPlayer(happy, "I want for nothing!")
            chatNpc(neutral, "You're one of a very lucky few then.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grubor
    // TODO(content): [quest:heroes_quest] door dialogue and the Phoenix Gang password
    private suspend fun Dialogue.grubor() {
        chatPlayer(happy, "Hi.")
        chatNpc(neutral, "Hi. I'm a little busy right now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kangai_Mau
    // TODO(content): [system:treasure_trails] medium clue reward
    private suspend fun Dialogue.kangaiMau() {
        if (QuestRequirements.hasCompleted(player, TRIBAL_TOTEM)) {
            chatNpc(happy, "Many greetings esteemed thief.")
            chatPlayer(happy, "Hey.")
            return
        }
        // TODO(content): [quest:tribal_totem] quest start and quest dialogue
        chatNpc(happy, "Hello. I Kangai Mau of the Rantuki tribe.")
        val option =
            choice3(
                "And what are you doing in Brimhaven?",
                1,
                "I'm in search of adventure!",
                2,
                "Who are the Rantuki tribe?",
                3,
            )
        when (option) {
            1 -> kangaiMauInBrimhaven()
            2 -> {
                chatPlayer(happy, "I'm in search of adventure!")
                chatNpc(happy, "Adventure is something I may be able to give.")
                kangaiMauMission()
            }
            3 -> {
                chatPlayer(quiz, "Who are the Rantuki tribe?")
                chatNpc(
                    neutral,
                    "A proud and noble tribe of Karamja. But now we are few, as men come from " +
                        "across sea, steal our land, and settle on our hunting grounds.",
                )
                chatPlayer(quiz, "And what are you doing in Brimhaven?")
                kangaiMauInBrimhaven()
            }
        }
    }

    private suspend fun Dialogue.kangaiMauInBrimhaven() {
        chatNpc(
            neutral,
            "I looking for someone brave to go on important mission for me. Someone skilled in " +
                "thievery and sneaking about. I am told I can find such people in Brimhaven.",
        )
        chatPlayer(quiz, "Tell me of this mission.")
        kangaiMauMission()
    }

    private suspend fun Dialogue.kangaiMauMission() {
        chatNpc(
            neutral,
            "I need someone to go on a mission to the city of Ardougne. There you will find the " +
                "house of Lord Handelmort. In his house he has our tribal totem.",
        )
        chatNpc(neutral, "We need it back.")
        chatPlayer(quiz, "Why does he have it?")
        chatNpc(
            angry,
            "Lord Handelmort is an Ardougnese explorer which means he think he have the right to " +
                "come to my tribal home, steal our stuff and put in his private museum.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lubufu
    // TODO(content): [system:met_flags] "I said go away!" when spoken to again
    private suspend fun Dialogue.lubufu() {
        if (QuestRequirements.hasCompleted(player, TAI_BWO_WANNAI_TRIO)) {
            chatNpc(
                angry,
                "You deceived me you confoundering dolt! Because of you, my entire business is " +
                    "ruined! Get out of my sight!",
            )
            return
        }
        chatNpc(angry, "Watch where you're going, young whippersnapper!")
        val option =
            choice3(
                "I wasn't going anywhere...",
                1,
                "What's a whippersnapper?",
                2,
                "Who are you?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(confused, "I wasn't going anywhere...")
                chatNpc(angry, "Well then go away from here!")
            }
            2 -> {
                chatPlayer(quiz, "What's a whippersnapper?")
                chatNpc(angry, "It's a whip. Which snaps. Like me. Now leave!")
            }
            3 -> {
                chatNpc(
                    neutral,
                    "I am Lubufu - the only fisherman who knows the secret of the Karambwan!",
                )
                chatPlayer(quiz, "What's a Karambwan?")
                chatNpc(angry, "What a foolish question! Now leave!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sandy (before The Hand in the Sand)
    // TODO(content): [quest:the_hand_in_the_sand] quest dialogue
    private suspend fun Dialogue.sandy() {
        when (access.random.of(3)) {
            0 -> chatNpc(happy, "Nice day for sand isn't it?")
            1 -> chatNpc(happy, "Sand is yellow, Sand is grand, Sand puts money, In my hand!")
            else -> {
                chatNpc(
                    neutral,
                    "I'm Sandy of Sandy's Sand Corp, are you in the sand business?",
                )
                chatPlayer(confused, "Errrr... no.")
                chatNpc(angry, "Then go away!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Saniboch
    // TODO(content): [system:brimhaven_dungeon] entry fee, permanent access and the Pay option
    // TODO(content): [system:treasure_trails] hard puzzle box step
    private suspend fun Dialogue.saniboch() {
        chatNpc(happy, "Good day to you, Bwana.")
        while (true) {
            val option =
                choice4(
                    "Can I go through that door please?",
                    1,
                    "Where does this strange entrance lead?",
                    2,
                    "I'm impressed, that tree is growing on that shed.",
                    3,
                    "Good day to you too.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Can I go through that door please?")
                    chatNpc(
                        neutral,
                        "Most certainly, but I must charge you the sum of 875 coins first.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Where does this strange entrance lead?")
                    chatNpc(
                        neutral,
                        "To a huge fearsome dungeon, populated by giants and strange dogs. " +
                            "Adventurers come from all around to explore its depths.",
                    )
                    chatNpc(
                        neutral,
                        "I know not what lies deeper in myself, for my skills in agility and " +
                            "woodcutting are inadequate.",
                    )
                }
                3 -> {
                    chatPlayer(happy, "I'm impressed, that tree is growing on that shed.")
                    chatNpc(
                        neutral,
                        "My employer tells me it is an uncommon sort of tree called the " +
                            "Fyburglars tree.",
                    )
                }
                4 -> {
                    chatPlayer(happy, "Good day to you too.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hajedy
    // TODO(content): [diary:karamja] free rides after the elite diary
    private suspend fun Dialogue.hajedy() {
        if (!QuestRequirements.hasCompleted(player, SHILO_VILLAGE)) {
            chatNpc(
                neutral,
                "We used to run cart trips down to Shilo Village in south Karamja. Since the " +
                    "troubles we had we've had to stop them though, too many people got killed.",
            )
            return
        }
        hajedyCartRide()
    }

    // TODO(content): [system:cart_travel] taking the fare and the cart ride to Shilo Village
    // TODO(content): [verify:in_game] fare rounding (wiki: about 5% of carried coins, 10 to 200)
    private suspend fun Dialogue.hajedyCartRide() {
        val fare = (access.invTotal(player.inv, COINS) / 20).coerceIn(10, 200)
        chatNpc(
            neutral,
            "I am offering a cart ride to Shilo Village, if you're interested. It will cost " +
                "$fare coins. Is that okay?",
        )
        val ride =
            choice2("Yes please, I'd like to go to Shilo Village.", true, "No thanks.", false)
        if (!ride) {
            chatPlayer(neutral, "No thanks.")
            chatNpc(neutral, "Okay, Bwana, let me know if you change your mind.")
            return
        }
        chatPlayer(happy, "Yes please, I'd like to go to Shilo Village.")
        if (access.invTotal(player.inv, COINS) < fare) {
            chatNpc(
                neutral,
                "Sorry, but it looks as if you don't have enough money. Come and see me when you " +
                    "have enough for the ride.",
            )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Yohnus
    // TODO(content): [system:shilo_furnace_coffer] coffer menu (deposit, empty, cancel)
    // TODO(content): [diary:karamja] free furnace line after the elite diary
    private suspend fun Dialogue.yohnus() {
        if (!QuestRequirements.hasCompleted(player, SHILO_VILLAGE)) {
            chatNpc(sad, "I'm getting out of here Bwana, the place is infested with Zombies.")
            return
        }
        chatPlayer(neutral, "Hello.")
        chatNpc(
            neutral,
            "Sorry, but the smithery is closed. You can use the furnace for a fee, and I can " +
                "hold a coffer of gold so you don't need to pay each time.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mosol_Rei
    private suspend fun Dialogue.mosolRei() {
        if (QuestRequirements.hasCompleted(player, SHILO_VILLAGE)) {
            mosolReiAfterShilo()
            return
        }
        if (QuestRequirements.hasCompleted(player, JUNGLE_POTION)) {
            // TODO(content): [quest:shilo_village] quest start and quest dialogue
            return
        }
        chatNpc(
            neutral,
            "Sorry Bwana, I cannot help you at this time. Go and talk to Trufitus at Tai Bwo " +
                "Wannai, I believe he needs some help.",
        )
    }

    private suspend fun Dialogue.mosolReiAfterShilo() {
        chatPlayer(happy, "Greetings!")
        chatNpc(
            happy,
            "Greetings Bwana! We've removed the threat of Rashiliyia and though there are still " +
                "some random outbreaks of undead activity we are more than able to deal with it.",
        )
        chatNpc(happy, "You're welcome to enter the village now Bwana, shall I show you the way?")
        // TODO(content): [verify:in_game] "You can now enter Shilo Village." as the menu title
        val enter =
            choice2(
                "Yes, Ok, I'll go into the village!",
                true,
                "I think I'll see it some other time.",
                false,
                title = "You can now enter Shilo Village.",
            )
        if (!enter) {
            mesbox("You decide not to visit the village.")
            return
        }
        // TODO(content): [system:shilo_gate] Mosol leading the player through the gate into the
        //  village ("Mosol leads you
        //  into the village." / "Mosol leaves you by the gate and walks back out into the
        //  jungle.")
    }

    private companion object {
        const val COINS = "obj.coins"
        const val JUNGLE_POTION = "quest_junglepotion"
        const val SHILO_VILLAGE = "quest_shilovillage"
        const val TAI_BWO_WANNAI_TRIO = "quest_taibwowannaitrio"
        const val TRIBAL_TOTEM = "quest_tribaltotem"
    }
}
