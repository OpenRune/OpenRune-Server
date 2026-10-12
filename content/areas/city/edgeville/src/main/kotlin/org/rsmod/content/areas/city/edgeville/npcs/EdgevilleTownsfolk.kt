package org.rsmod.content.areas.city.edgeville.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class EdgevilleTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.oziach") { startDialogue(it.npc) { oziach() } }
        onOpNpc3("npc.oziach") { startDialogue(it.npc) { oziachTrade() } }
        onOpNpc1("npc.hundred_dave_mum") { startDialogue(it.npc) { doris() } }
        onOpNpc1("npc.canoeing_hari") { startDialogue(it.npc) { hari() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oziach (before Dragon Slayer I)
    // TODO(content): [quest:dragon_slayer_i] rune platebody shop, Melzar's Maze key and DFS options
    private suspend fun Dialogue.oziach() {
        chatNpc(happy, "Aye, 'tis a fair day my friend.")
        when (choice2("I'm not your friend.", 1, "Yes, it's a very nice day.", 2)) {
            1 -> {
                chatPlayer(angry, "I'm not your friend.")
                chatNpc(
                    angry,
                    "I'm surprised if you're anyone's friend with those kind of manners.",
                )
            }
            2 -> {
                chatPlayer(happy, "Yes, it's a very nice day.")
                chatNpc(neutral, "Aye, may the gods walk by yer side. Now leave me alone.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Oziach (trying to trade)
    // TODO(content): [quest:dragon_slayer_i] open inv.runeplateshop after the quest
    private suspend fun Dialogue.oziachTrade() {
        chatNpc(angry, "I ain't got nothing to sell ye, adventurer. Leave me be!")
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
        const val LOG_CANOE_LEVEL = 12
        const val DUGOUT_LEVEL = 27
        const val STABLE_DUGOUT_LEVEL = 42
        const val WAKA_LEVEL = 57
    }
}
