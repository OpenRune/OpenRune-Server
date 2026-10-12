package org.rsmod.content.areas.city.burthorpe.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseAgilityLvl
import org.rsmod.api.player.stat.baseThievingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class RoguesDen @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.roguesden_trader") { startDialogue(it.npc) { martinThwait(it.npc) } }
        onOpNpc3("npc.roguesden_trader") { tradeMartin(it.npc) }
        onOpNpc1("npc.roguesden_mazemaster") { startDialogue(it.npc) { brianORichard() } }
        onOpNpc1("npc.rooftops_grace") { startDialogue(it.npc) { grace() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Martin_Thwait
    // TODO(content): [system:treasure_trails] challenge scroll
    private suspend fun Dialogue.martinThwait(npc: Npc) {
        chatNpc(
            shifty,
            "You know it's sometimes funny how things work out, I lose some gold but find an " +
                "item, or I lose an item and find some gold... no-one ever knows what's gone " +
                "where ya know.",
        )
        val option =
            choice3(
                "Yeah I know what you mean, found anything recently?",
                1,
                "Can you tell me about your cape?",
                2,
                "Okay... I'll be going now.",
                3,
            )
        when (option) {
            1 -> lostAndFound(npc)
            2 -> {
                chatPlayer(quiz, "Can you tell me about your cape?")
                chatNpc(
                    happy,
                    "Certainly! Skillcapes are a symbol of achievement. Only people who have " +
                        "mastered a skill and reached level 99 can get their hands on them and " +
                        "gain the benefits they carry.",
                )
                chatNpc(
                    happy,
                    "The Cape of Thieving provides a nice boost to your chances of pickpocketing " +
                        "when worn.",
                )
                val again =
                    choice2(
                        "Have you found anything recently?",
                        true,
                        "No thank you.",
                        false,
                        title = "Is there anything else I can help you with?",
                    )
                if (again) {
                    lostAndFound(npc)
                } else {
                    chatPlayer(neutral, "No thank you.")
                }
            }
            3 -> chatPlayer(neutral, "Okay... I'll be going now.")
        }
    }

    private suspend fun ProtectedAccess.tradeMartin(npc: Npc) =
        startDialogue(npc) { lostAndFound(npc) }

    // TODO(content): [system:skillcapes] inv.roguesden_shop_skillcape_trimmed when the player has
    //  more than one level 99
    private suspend fun Dialogue.lostAndFound(npc: Npc) {
        val thieving = player.baseThievingLvl
        if (thieving < ROGUES_DEN_LEVEL) {
            chatNpc(
                neutral,
                "Sorry, mate. Train up your Thieving skill to at least 50 and I might be able to " +
                    "help you out.",
            )
            return
        }
        if (thieving < MAX_LEVEL && player.baseAgilityLvl < ROGUES_DEN_LEVEL) {
            chatNpc(
                neutral,
                "Sorry, mate. Train up your Agility skill to at least 50 and I might be able to " +
                    "help you out.",
            )
            return
        }
        val inv = if (thieving >= MAX_LEVEL) SKILLCAPE_SHOP else SHOP
        shops.open(player, npc, "Martin Thwait's Lost and Found.", inv)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brian_O%27Richard (before maze access)
    // TODO(content): [system:rogues_den] maze access flag, the "Hi again" greeting and the maze
    private suspend fun Dialogue.brianORichard() {
        chatNpc(happy, "Hi there, looking for a challenge are you?")
        while (true) {
            val option =
                choice3(
                    "Yes actually, what've you got?",
                    1,
                    "What is this place?",
                    2,
                    "No thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    brianChallenge()
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "Ah welcome to my humble home, well actually it belongs to mummsie but " +
                            "she's getting on a bit so I look after the place for her.",
                    )
                    chatNpc(quiz, "So are you interested in a challenge?")
                }
                3 -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.brianChallenge() {
        chatPlayer(quiz, "Yes actually, what've you got?")
        if (player.baseThievingLvl < ROGUES_DEN_LEVEL) {
            chatNpc(
                sad,
                "Shame, I don't think I have anything for you. Train up your Thieving skill to " +
                    "at least 50 and I might be able to help you out.",
            )
            return
        }
        if (player.baseAgilityLvl < ROGUES_DEN_LEVEL) {
            chatNpc(
                sad,
                "Shame, I don't think I have anything for you. Train up your Agility skill to at " +
                    "least 50 and I might be able to help you out.",
            )
            return
        }
        chatNpc(
            happy,
            "Aha, I have the perfect thing for you! See if you can get to the centre of my maze, " +
                "the further you get the greater the rewards. There's even some special prizes " +
                "if you make it right to the end.",
        )
        while (true) {
            val option =
                choice3(
                    "Ok that sounds good!",
                    1,
                    "What is this place?",
                    2,
                    "Actually I think I'll pass thanks.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Ok that sounds good!")
                    chatNpc(
                        happy,
                        "Great! When you enter the maze, I'll give you a jewel - it'll allow you " +
                            "to get out of the maze at any time. However that's all you're " +
                            "allowed to take in with you, no cheating!",
                    )
                    chatNpc(
                        neutral,
                        "Oh one last thing, if you happen to see my harmonica I'd really like to " +
                            "have it back.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "Ah welcome to my humble home, well actually it belongs to mummsie but " +
                            "she's getting on a bit so I look after the place for her.",
                    )
                    chatNpc(quiz, "So are you interested in a challenge?")
                }
                3 -> {
                    chatPlayer(neutral, "Actually I think I'll pass thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Grace
    // TODO(content): [system:graceful_shop] "Can I see what you're selling?" and Trade open
    //  Grace's Graceful Clothing
    // TODO(content): [system:agility_lap_counter] View Laps and Toggle Counter options
    private suspend fun Dialogue.grace() {
        chatNpc(happy, "What can I do for you?")
        val option =
            choice5(
                "I don't know. What can you do for me?",
                1,
                "Can I see what you're selling?",
                2,
                "Can I see how many laps I've completed?",
                3,
                "Can I toggle my lap counter?",
                4,
                "I'm alright, thanks.",
                5,
                title = GRACE_MENU,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "I don't know. What can you do for me?")
                chatNpc(
                    happy,
                    "A good question indeed! I'm selling special clothing for Agility " +
                        "enthusiasts.",
                )
                chatNpc(
                    neutral,
                    "Sometimes, when you're exploring the hidden rooftops of our cities, you'll " +
                        "find one of my Marks.",
                )
                chatNpc(
                    quiz,
                    "Once you've got enough Marks, I'll exchange them for my Graceful clothing. " +
                        "So, does that interest you?",
                )
                val see =
                    choice2(
                        "Can I see what you're selling?",
                        true,
                        "I'm alright, thanks.",
                        false,
                        title = GRACE_MENU,
                    )
                if (see) {
                    chatPlayer(quiz, "Can I see what you're selling?")
                }
            }
            2 -> chatPlayer(quiz, "Can I see what you're selling?")
            3 -> chatPlayer(quiz, "Can I see how many laps I've completed?")
            4 -> chatPlayer(quiz, "Can I toggle my lap counter?")
            5 -> chatPlayer(neutral, "I'm alright, thanks.")
        }
    }

    private companion object {
        const val ROGUES_DEN_LEVEL = 50
        const val MAX_LEVEL = 99
        const val SHOP = "inv.roguesden_shop"
        const val SKILLCAPE_SHOP = "inv.roguesden_shop_skillcape"
        const val GRACE_MENU = "What would you like to say?"
    }
}
