package org.rsmod.content.areas.city.edgeville.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseSmithingLvl
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EdgevilleTownsfolk @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.oziach") { startDialogue(it.npc) { oziach(it.npc) } }
        onOpNpc3("npc.oziach") { oziachTrade(it.npc) }
        onOpNpc1("npc.hundred_dave_mum") { startDialogue(it.npc) { doris() } }
        onOpNpc1("npc.canoeing_hari") { startDialogue(it.npc) { hari() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oziach (before Dragon Slayer I)
    // TODO(content): [quest:dragon_slayer_i] quest dialogue while on the quest
    private suspend fun Dialogue.oziach(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, DRAGON_SLAYER_I)) {
            oziachAfterQuest(npc)
            return
        }
        chatNpc(happy, "Aye, 'tis a fair day my friend.")
        when (choice2("I'm not your friend.", 1, "Yes, it's a very nice day.", 2)) {
            1 -> oziachNotYourFriend()
            2 -> oziachNiceDay()
        }
    }

    private suspend fun Dialogue.oziachNotYourFriend() {
        chatPlayer(angry, "I'm not your friend.")
        chatNpc(angry, "I'm surprised if you're anyone's friend with those kind of manners.")
    }

    private suspend fun Dialogue.oziachNiceDay() {
        chatPlayer(happy, "Yes, it's a very nice day.")
        chatNpc(neutral, "Aye, may the gods walk by yer side. Now leave me alone.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oziach (after Dragon Slayer I)
    // TODO(content): [system:skeletal_visage] dragonfire ward dialogue (not in the transcript)
    private suspend fun Dialogue.oziachAfterQuest(npc: Npc) {
        chatNpc(happy, "Aye, 'tis a fair day, my mighty dragon-slaying friend.")
        val hasVisage = player.inv.contains(DRACONIC_VISAGE)
        val hasShield =
            player.inv.contains(ANTI_DRAGON_SHIELD) || player.worn.contains(ANTI_DRAGON_SHIELD)
        if (!hasVisage && hasShield) {
            chatNpc(
                neutral,
                "I see ye've got one of them anti-dragon shields there. I've got an idea about " +
                    "them things.",
            )
        }
        val extra =
            when {
                hasVisage -> "Look at this visage."
                hasShield -> "What's your idea about anti-dragon shields?"
                else -> null
            }
        val option =
            if (extra == null) {
                choice4(
                    "Can I buy a rune platebody now, please?",
                    1,
                    "I'm not your friend.",
                    2,
                    "Yes, it's a very nice day.",
                    3,
                    "Can I have another key to Melzar's Maze?",
                    4,
                )
            } else {
                choice5(
                    "Can I buy a rune platebody now, please?",
                    1,
                    "I'm not your friend.",
                    2,
                    "Yes, it's a very nice day.",
                    3,
                    "Can I have another key to Melzar's Maze?",
                    4,
                    extra,
                    5,
                )
            }
        when (option) {
            1 -> oziachBuyPlatebody(npc)
            2 -> oziachNotYourFriend()
            3 -> oziachNiceDay()
            4 -> {
                chatPlayer(quiz, "Can I have another key to Melzar's Maze?")
                chatNpc(
                    neutral,
                    "It's the Guildmaster in the Champions' Guild who hands those keys out now. " +
                        "Go talk to him. No need to bother me if you don't need armour.",
                )
            }
            // TODO(content): [verify:in_game] whether a worn anti-dragon shield counts for forging
            5 -> {
                if (hasVisage) {
                    oziachVisage(player.inv.contains(ANTI_DRAGON_SHIELD))
                } else {
                    oziachShieldIdea(npc)
                }
            }
        }
    }

    private suspend fun Dialogue.oziachBuyPlatebody(npc: Npc) {
        chatPlayer(quiz, "Can I buy a rune platebody now please?")
        openOziachShop(npc)
    }

    private fun Dialogue.openOziachShop(npc: Npc) {
        shops.open(player, npc, "Oziach's Armour", "inv.runeplateshop")
    }

    private suspend fun Dialogue.oziachShieldIdea(npc: Npc) {
        chatPlayer(quiz, "What's your idea about anti-dragon shields?")
        chatNpc(
            neutral,
            "See, when ye uses them to fight a dragon, they absorb the fire. The shield must be " +
                "storing the dragonfire, and just letting it radiate away.",
        )
        chatNpc(neutral, "Now, if there was only a way to get the dragonfire out again...")
        while (true) {
            val use =
                choice2(
                    "What use would that be?",
                    1,
                    "Can I buy a rune platebody now, please?",
                    2,
                )
            if (use == 2) {
                oziachBuyPlatebody(npc)
                return
            }
            chatPlayer(quiz, "What use would that be?")
            chatNpc(
                happy,
                "Why, ain't it obvious? Ye could project blasts of dragonfire from yer shield!",
            )
            chatNpc(neutral, "Ye know, I think there might be a way.")
            val way = choice2("What way?", 1, "Can I buy a rune platebody now, please?", 2)
            if (way == 2) {
                oziachBuyPlatebody(npc)
                return
            }
            chatPlayer(quiz, "What way?")
            chatNpc(
                neutral,
                "I've heard legends of a thing called a 'draconic visage'. It was made by the " +
                    "dragonkin themselves apparently.",
            )
            chatNpc(
                neutral,
                "This 'draconic visage' had the power to channel dragonfire. Now, if I could " +
                    "attach the visage to the front of this shield...",
            )
            val next =
                choice3(
                    "What use would that be?",
                    1,
                    "I will look for this draconic visage!",
                    2,
                    "Can I buy a rune platebody now, please?",
                    3,
                )
            when (next) {
                1 -> continue
                2 -> {
                    chatPlayer(happy, "I will look for this draconic visage!")
                    chatNpc(
                        neutral,
                        "Good luck wi' that. They're mighty rare, so yer quest will take some " +
                            "time. If any monsters have them, it'll be the dragons.",
                    )
                }
                3 -> oziachBuyPlatebody(npc)
            }
            return
        }
    }

    private suspend fun Dialogue.oziachVisage(hasShield: Boolean) {
        chatPlayer(happy, "Look at this visage.")
        chatNpc(happy, "Amazin'! Ye can almost feel it pulsing with draconic power!")
        if (!hasShield) {
            chatNpc(
                neutral,
                "Now, if ye bring me this and the anti-dragonbreath shield, I reckon I could " +
                    "combine them into something pretty special!",
            )
            return
        }
        chatNpc(
            neutral,
            "Now, if ye want me to, I could attach this to yer anti-dragonbreath shield and make " +
                "something pretty special.",
        )
        chatNpc(neutral, "I'll charge 1,250,000 coins to construct it. What d'ye say?")
        while (true) {
            val option =
                choice3("Yes, please!", 1, "No, thanks.", 2, "That's a bit expensive!", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please!")
                    if (!forgeDragonfireShield()) {
                        // TODO(content): [verify:in_game] reply when the coins, visage or shield
                        //  are missing at the moment of forging (not in the transcript).
                        return
                    }
                    mesbox("Oziach skillfully forges the shield and visage into a new shield.")
                    chatNpc(
                        happy,
                        "There ye go. Now, the more dragonfire the shield absorbs, the more " +
                            "powerful it'll become.",
                    )
                    return
                }
                2 -> {
                    chatPlayer(neutral, "No, thanks.")
                    chatNpc(neutral, "Suit yerself.")
                    return
                }
                3 -> {
                    chatPlayer(worried, "That's a bit expensive!")
                    chatNpc(
                        neutral,
                        "Yes it is. But I'm a master smith so I reckon I can charge what I like. " +
                            "Unless ye've got level 90 Smithing too, which I doubt!",
                    )
                    if (player.baseSmithingLvl >= DFS_SMITHING_LEVEL) {
                        chatPlayer(happy, "Actually, I have got level 90 Smithing.")
                        chatNpc(neutral, "Oh! Well, I suppose ye don't need me for this then.")
                        return
                    }
                    chatPlayer(sad, "No, I don't have level 90 Smithing.")
                    chatNpc(
                        neutral,
                        "I thought not. I'm the master smith of Misthalin! So, shall I construct " +
                            "the shield for you?",
                    )
                }
            }
        }
    }

    private fun Dialogue.forgeDragonfireShield(): Boolean {
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = DFS_FEE
                }
                delete {
                    from = inv
                    obj = DRACONIC_VISAGE.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = inv
                    obj = ANTI_DRAGON_SHIELD.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                insert {
                    into = inv
                    obj = DRAGONFIRE_SHIELD.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oziach (trying to trade)
    // https://oldschool.runescape.wiki/w/Oziach_(shop) (needs Dragon Slayer I)
    private suspend fun ProtectedAccess.oziachTrade(npc: Npc) {
        if (QuestRequirements.hasCompleted(player, DRAGON_SLAYER_I)) {
            shops.open(player, npc, "Oziach's Armour", "inv.runeplateshop")
            return
        }
        startDialogue(npc) {
            chatNpc(angry, "I ain't got nothing to sell ye, adventurer. Leave me be!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Doris
    // TODO(content): [system:pets] "What's happened to my cat?" turns a following hellcat back
    // TODO(content): [system:treasure_trails] easy clue reward
    private suspend fun Dialogue.doris() {
        chatNpc(happy, "Hello again dearie. How are you doing?")
        val option =
            choice4(
                "Pretty good!",
                1,
                "Not too good actually",
                2,
                "What's it like living so close to the wilderness?",
                3,
                "How did Dave come to be evil?",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Pretty good!")
                chatNpc(happy, "That's good to hear.")
            }
            2 -> {
                chatPlayer(sad, "Not too good actually!")
                chatNpc(sad, "Oh well.")
            }
            3 -> {
                chatPlayer(quiz, "What's it like living so close to the wilderness?")
                chatNpc(
                    neutral,
                    "Oh, it's not all that bad. It was a bit scary at first but as long as I " +
                        "don't go past the warning signs I'm all right.",
                )
                chatNpc(
                    neutral,
                    "It's actually pretty quiet here. I used to live in Lumbridge and, let me " +
                        "tell you, that's the dangerous place for people like me.",
                )
                chatNpc(
                    worried,
                    "Thieves were picking people's pockets with impunity, and killing them on " +
                        "the streets in broad daylight!",
                )
                chatNpc(
                    angry,
                    "And there weren't even any guards to protect us! Not that the guards in the " +
                        "other cities do much good to protect people from what I've heard.",
                )
                chatNpc(
                    happy,
                    "I just thank goodness I haven't got an Attack option any more! I wouldn't " +
                        "last five minutes!",
                )
            }
            4 -> {
                chatPlayer(quiz, "How did Dave come to be evil?")
                chatNpc(
                    neutral,
                    "Oh, I don't know really. He's always been evil, I think. Maybe growing up " +
                        "so close to the wilderness has affected him in some way.",
                )
                chatNpc(
                    neutral,
                    "When he was little he used to want to go into Edgeville dungeon. You know " +
                        "how kids are, always wanting to be dungeon adventurers! But he didn't " +
                        "want to be a hero, he wanted to be a monster!",
                )
                chatNpc(
                    neutral,
                    "And then he spent all summer building a tree-house. Except it wasn't a " +
                        "tree-house, of course, it was his Tower of Fear!",
                )
                chatNpc(
                    sad,
                    "Of course I was hoping he'd have grown out of it by now and maybe even left " +
                        "home. But at least it keeps him happy.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hari
    // TODO(content): [system:canoe] Store-axe option and its dialogue
    private suspend fun Dialogue.hari() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello.")
        when (choice2("Who are you?", 1, "Can you teach me about Canoeing?", 2)) {
            1 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(happy, "My name is Hari.")
                chatPlayer(quiz, "And what are you doing here Hari?")
                chatNpc(
                    neutral,
                    "Like most people who come to Edgeville, I am here to seek adventure in the " +
                        "Wilderness.",
                )
                chatNpc(
                    neutral,
                    "I found a secret underground river that will take me quite a long way north.",
                )
                chatPlayer(quiz, "Underground river? Where does it come out?")
                chatNpc(neutral, "It comes out in a pond located deep in the Wilderness.")
                chatNpc(
                    neutral,
                    "I had to find a very special type of canoe to get me up the river though, " +
                        "would you like to know more?",
                )
                when (choice2("Yes", 1, "No", 2)) {
                    1 -> canoeLesson()
                    2 -> chatPlayer(neutral, "No thanks, not right now.")
                }
            }
            2 -> canoeLesson()
        }
    }

    private suspend fun Dialogue.canoeLesson() {
        chatPlayer(quiz, "Could you teach me about canoes?")
        val level = player.woodcuttingLvl
        if (level < LOG_CANOE_LEVEL) {
            chatNpc(neutral, "Well, you don't look like you have the skill to make a canoe.")
            chatNpc(neutral, "You need to have at least level 12 woodcutting.")
            chatNpc(
                neutral,
                "Once you are able to make a canoe it makes travel along the river much quicker!",
            )
            return
        }
        chatNpc(
            happy,
            "It's really quite simple. Just walk down to that tree on the bank and chop it down.",
        )
        chatNpc(
            happy,
            "When you have done that you can shape the log further with your axe to make a canoe.",
        )
        when {
            level < DUGOUT_LEVEL -> {
                chatNpc(
                    neutral,
                    "I can sense you're still a novice woodcutter, you will only be able to make " +
                        "a log canoe at present.",
                )
                chatPlayer(quiz, "Is that good?")
                chatNpc(
                    neutral,
                    "A log will take you one stop along the river. But you won't be able to " +
                        "travel into the Wilderness on it.",
                )
            }
            level < STABLE_DUGOUT_LEVEL -> {
                chatNpc(
                    neutral,
                    "You are an average woodcutter. You should be able to make a Dugout canoe " +
                        "quite easily. It will take you 2 stops along the river.",
                )
                chatPlayer(quiz, "Can I take a dugout canoe to reach the Wilderness?")
                chatNpc(worried, "You would never make it there alive.")
                chatPlayer(neutral, "Best not to try then.")
            }
            level < WAKA_LEVEL -> {
                chatNpc(
                    happy,
                    "You seem to be an accomplished woodcutter. You will easily be able to make " +
                        "a Stable Dugout",
                )
                chatNpc(
                    neutral,
                    "They are reliable enough to get you anywhere on this river, except to the " +
                        "Wilderness of course.",
                )
                chatNpc(neutral, "Only a Waka can take you there.")
                chatPlayer(quiz, "A Waka? What's that?")
                chatNpc(
                    neutral,
                    "Come and ask me when you have improved your skills as a woodcutter.",
                )
            }
            else -> {
                chatNpc(
                    happy,
                    "Your skills rival mine friend. You will certainly be able to build a Waka.",
                )
                chatPlayer(quiz, "A Waka? What's that?")
                chatNpc(
                    happy,
                    "A Waka is an invention of my people, it's an incredible strong and fast " +
                        "canoe and will carry you safely to any destination on the river.",
                )
                chatPlayer(quiz, "Any destination?")
                chatNpc(
                    neutral,
                    "Yes, you can take a waka north through the underground portion of this " +
                        "river.",
                )
                chatNpc(
                    worried,
                    "It will bring you out at a pond in the heart of the Wilderness. Be careful " +
                        "up there, many have lost more than their lives in that dark and twisted " +
                        "place.",
                )
            }
        }
    }

    private companion object {
        const val DRAGON_SLAYER_I = "quest_dragonslayer1"
        const val COINS = "obj.coins"
        const val ANTI_DRAGON_SHIELD = "obj.antidragonbreathshield"
        const val DRACONIC_VISAGE = "obj.dragonfire_visage"
        const val DRAGONFIRE_SHIELD = "obj.dragonfire_shield_uncharged"
        const val DFS_FEE = 1_250_000
        const val DFS_SMITHING_LEVEL = 90
        const val LOG_CANOE_LEVEL = 12
        const val DUGOUT_LEVEL = 27
        const val STABLE_DUGOUT_LEVEL = 42
        const val WAKA_LEVEL = 57
    }
}
