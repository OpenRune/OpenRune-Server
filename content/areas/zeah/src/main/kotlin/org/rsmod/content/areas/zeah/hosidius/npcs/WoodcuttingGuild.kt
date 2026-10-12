package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WoodcuttingGuild : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.wcguild_lars") { startDialogue(it.npc) { guildmasterLars() } }
        onOpNpc1("npc.wcguild_guard") { startDialogue(it.npc) { berry() } }
        onOpNpc1("npc.wcguild_forester") { startDialogue(it.npc) { forester() } }
        onOpNpc1("npc.wcguild_kai") { startDialogue(it.npc) { kai() } }
        onOpNpc1("npc.wcguild_cave_guard") { startDialogue(it.npc) { murfet() } }
        onOpNpc1("npc.wcguild_nesty") { startDialogue(it.npc) { nesty() } }
        onOpNpc1("npc.wcguild_axeseller") { startDialogue(it.npc) { perry() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guildmaster_Lars
    // TODO(content): [system:treasure_trails] master clue light box and puzzle box
    private suspend fun Dialogue.guildmasterLars() {
        chatNpc(happy, "Welcome to the woodcutting guild. How may I help you?")
        var option =
            choice3(
                "Tell me about the guild.",
                1,
                "Tell me about yourself.",
                2,
                "I'm just passing by.",
                3,
            )
        while (true) {
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the guild.")
                    chatNpc(
                        neutral,
                        "I founded the woodcutting guild when I retired from the Shayzien " +
                            "military. I spotted the ancient redwood trees in the hills many " +
                            "years ago from the Shayzien camp, and decided to construct the " +
                            "guild around them.",
                    )
                    chatNpc(neutral, "You can find them to the West, just follow the path.")
                    chatNpc(
                        neutral,
                        "However, if you're skilled in combat, or simply afraid of heights, the " +
                            "ent cavern may be more suited for you. The ents dwell deep beneath " +
                            "the ancient redwoods, and can be accessed through the cave to the " +
                            "North-West.",
                    )
                    chatNpc(quiz, "Is there anything else I can help you with?")
                    option = choice2("Tell me about yourself.", 2, "That's all, thanks.", 4)
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        neutral,
                        "My name is Lars, and I am the founder of the woodcutting guild. I " +
                            "originally grew up in a Shayzien camp, and was raised on the " +
                            "battlefield.",
                    )
                    chatNpc(
                        neutral,
                        "As I grew older, I found peace in Hosidius, and the joys of skilled " +
                            "labour. I turned in my battleaxe for a hatchet, and set up a guild " +
                            "for those also talented in the skill of woodcutting.",
                    )
                    chatNpc(quiz, "Is there anything else I can help you with?")
                    option = choice2("Tell me about the guild.", 1, "That's all, thanks.", 4)
                }
                3 -> {
                    chatPlayer(neutral, "I'm just passing by.")
                    chatNpc(happy, "Sure, let me know if there's anything I can do for you.")
                    return
                }
                else -> {
                    chatPlayer(neutral, "That's all, thanks.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Berry_(Woodcutting_Guild)
    // TODO(content): [system:npc_overhead] "Welcome to the Woodcutting Guild, adventurer." on entry
    private suspend fun Dialogue.berry() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            happy,
            "Good day, adventurer. If you have any questions, I'm sure Lars will be more than " +
                "happy to answer them. He's in the building to the south.",
        )
        chatNpc(
            happy,
            "Also don't forget to visit my sister's axe shop. She works in the same building as " +
                "Lars.",
        )
        chatPlayer(happy, "Thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Forester_(Woodcutting_Guild)
    private suspend fun Dialogue.forester() {
        when (access.random.of(3)) {
            0 -> chatNpc(happy, "Nice weather we're having today.")
            1 -> chatNpc(happy, "It's so peaceful here, don't you agree?")
            else ->
                chatNpc(
                    neutral,
                    "There's plenty of trees around, couldn't you go to another spot?",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kai
    private suspend fun Dialogue.kai() {
        chatPlayer(quiz, "Hello! Who are you?")
        chatNpc(
            happy,
            "Hi there, my name is Kai and I'm the keeper of this here ent habitat.",
        )
        if (!choice2("What exactly is an ent?", true, "That's nice, see you around.", false)) {
            chatPlayer(neutral, "That's nice, see you around.")
            return
        }
        chatPlayer(quiz, "What exactly is an ent?")
        chatNpc(
            happy,
            "Good question! Ents are ancient beings that resemble trees, we believe they draw " +
                "their energy directly from nature.",
        )
        if (!choice2("Where did they come from?", true, "That's nice, see you around.", false)) {
            chatPlayer(neutral, "That's nice, see you around.")
            return
        }
        chatPlayer(quiz, "Where did they come from?")
        chatNpc(
            neutral,
            "Another good question! We're not sure exactly, we just know they're a very old race.",
        )
        chatNpc(
            neutral,
            "I have heard of similar creatures roaming a desolate part of the eastern continent, " +
                "though.",
        )
        val fight =
            choice2(
                "Interesting... can I fight them?",
                true,
                "Thanks for the information, see you around.",
                false,
            )
        if (!fight) {
            chatPlayer(happy, "Thanks for the information, see you around.")
            return
        }
        chatPlayer(quiz, "Interesting... can I fight them?")
        chatNpc(
            happy,
            "Of course! There's no shortage of them residing within the dungeon, and they're a " +
                "fantastic source of wood.",
        )
        chatPlayer(happy, "Thanks!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Murfet
    private suspend fun Dialogue.murfet() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello, how can I help you?")
        if (!choice2("What's in the cave?", true, "I was just passing through.", false)) {
            chatPlayer(neutral, "I was just passing through.")
            return
        }
        chatPlayer(quiz, "What's in this cave?")
        chatNpc(
            neutral,
            "Creatures known as ents inhabit the depths of the hills, rumour has it they reside " +
                "there due to the presence of the ancient redwood trees on the hills.",
        )
        chatNpc(
            neutral,
            "If you're as skilled a fighter as you are a woodcutter, you may enter the caves. " +
                "You should find Kai once you are inside, he can give you some more information.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nesty
    private suspend fun Dialogue.nesty() {
        chatPlayer(quiz, "Hello! What's this then?")
        chatNpc(angry, "How can you not know? YOU MUST KNOW! It is of course the shrine!")
        chatPlayer(confused, "Okay... what is the purpose of said shrine?")
        chatNpc(angry, "Do you not listen? It is the shrine to almighty one! BWAAAAAAAK!")
        chatPlayer(worried, "You know, you're making me worried, are you okay?")
        chatNpc(madlaugh, "DO NOT defy him! Give him your eggs! So colourful! So beautiful!")
        chatPlayer(worried, "I'm going to leave you alone now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Perry
    // TODO(content): [system:shops] no server inv for Perry's Chop-chop Shop (Trade op)
    private suspend fun Dialogue.perry() {
        chatNpc(happy, "Hey, adventurer. Do you want to buy any axes?")
        while (true) {
            when (choice3("Who are you?", 1, "Okay, let's trade.", 2, "No, I'm fine.", 3)) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm Perry, and I sell axes here. Or sometimes I buy them, if someone's " +
                            "offering a good deal. Anyway, do you want to trade?",
                    )
                }
                2 -> {
                    chatPlayer(happy, "Okay, let's trade.")
                    // TODO(content): [system:shops] opens Perry's Chop-chop Shop
                    return
                }
                else -> {
                    chatPlayer(neutral, "No, I'm fine.")
                    return
                }
            }
        }
    }
}
