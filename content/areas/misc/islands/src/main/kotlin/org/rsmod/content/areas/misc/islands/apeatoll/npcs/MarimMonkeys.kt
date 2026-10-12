package org.rsmod.content.areas.misc.islands.apeatoll.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:monkey_disguise] monkeys call the guards on a player in human form, and
// speak Monkey language to a player without an M'speak amulet
// TODO(content): [quest:monkey_madness_ii] humans are tolerated after the quest
class MarimMonkeys @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mm_aberab") { startDialogue(it.npc) { getOutOfMyWay() } }
        onOpNpc1("npc.mm_trefaji") { startDialogue(it.npc) { getOutOfMyWay() } }
        onOpNpc1("npc.mm_sleeping_monkey_guard") { startDialogue(it.npc) { sleepingMonkey() } }
        onOpNpc1("npc.mm_monkeys_aunt") { startDialogue(it.npc) { monkeysAunt() } }
        onOpNpc1("npc.mm_monkeys_uncle") { startDialogue(it.npc) { monkeysUncle() } }
        onOpNpc1("npc.mm_murowoi") { startDialogue(it.npc) { muruwoi() } }
        onOpNpc1("npc.mm_uwogo") { startDialogue(it.npc) { uwogo() } }
        onOpNpc1("npc.mm_denadu") { startDialogue(it.npc) { denadu() } }
        onOpNpc1("npc.mm_hafuba") { startDialogue(it.npc) { hafuba() } }
        onOpNpc1("npc.mm_lofu") { startDialogue(it.npc) { lofu() } }
        onOpNpc1("npc.mm_dugopul") { startDialogue(it.npc) { dugopul() } }
        onOpNpc1("npc.mm_monkey_child") { startDialogue(it.npc) { monkeyChild() } }
        onOpNpc1("npc.hundred_agility_monkey") { startDialogue(it.npc) { bobawu() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Aberab
    // https://oldschool.runescape.wiki/w/Transcript:Trefaji
    private suspend fun Dialogue.getOutOfMyWay() {
        chatNpc(angry, "Grr ... Get out of my way...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sleeping_Monkey
    private suspend fun Dialogue.sleepingMonkey() {
        mesbox("The monkey looks a little too sleepy to talk to.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Monkey%27s_Aunt
    private suspend fun Dialogue.monkeysAunt() {
        chatNpc(happy, "Hello! What's a monkey like you doing here?")
        chatPlayer(neutral, "I'm just passing through actually.")
        chatNpc(neutral, "Fair enough then.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:The_Monkey%27s_Uncle
    private suspend fun Dialogue.monkeysUncle() {
        chatNpc(angry, "What are you doing in my house?")
        chatPlayer(worried, "I'm just passing through!")
        chatNpc(angry, "Well I don't give a monkey's uncle!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Muruwoi
    private suspend fun Dialogue.muruwoi() {
        chatPlayer(happy, "Good day, Muruwoi.")
        chatNpc(happy, "Well done on retrieving the captive, visitor. Awowogei was impressed.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Uwogo
    private suspend fun Dialogue.uwogo() {
        chatPlayer(happy, "Good day, Uwogo.")
        chatNpc(angry, "Get away from me, you foul commoner!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Denadu
    private suspend fun Dialogue.denadu() {
        chatNpc(angry, "Get out of my way, you fool! Never again interrupt Denadu!")
        chatPlayer(sad, "I'm sorry...")
        chatNpc(
            angry,
            "Monkeys these days... you'll be acting like one of those awful Gnomes next...",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hafuba
    private suspend fun Dialogue.hafuba() {
        chatNpc(
            happy,
            "Welcome to the temple of Marimbo, child. In less busy times I would bless you, " +
                "but I have not the luxury of free time at the moment.",
        )
        chatNpc(happy, "However, feel free to stay here as long as you need.")
        chatPlayer(happy, "Thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lofu
    private suspend fun Dialogue.lofu() {
        chatNpc(angry, "Get out of my way, you fool! Can't you see I'm busy?")
        chatPlayer(sad, "I'm sorry...")
        chatNpc(
            angry,
            "Monkeys these days... you'll be acting like one of those awful Gnomes next...",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dugopul
    // TODO(content): [system:treasure_trails] master clue puzzle boxes
    private suspend fun Dialogue.dugopul() {
        chatNpc(neutral, "Not many monkeys come down here...")
        chatPlayer(neutral, "That's quite understandable really.")
        chatNpc(confused, "I don't really understand it at all. I've spent my life here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monkey_Child
    // TODO(content): [system:met_flags] "I'm good, uncle! I'd quite like my toy back now..." while
    // the player still has the borrowed talisman, and only lending it again after time has passed
    // TODO(content): [verify:in_game] the uncle's options appear without an opening line
    private suspend fun Dialogue.monkeyChild() {
        chatNpc(sad, "I'm feeling a bit better now...")
        chatPlayer(happy, "It's good to see you've cheered up then.")
        chatNpc(happy, "Yes - Aunty gave me a new toy!")
        if (choice2("That's really sweet of her!", true, "Wow - can I borrow it?", false)) {
            chatPlayer(happy, "That's really sweet of her!")
            return
        }
        chatPlayer(quiz, "Wow - can I borrow it?")
        chatNpc(neutral, "Only if you promise to give it back...")
        if (choice2("Ok, I promise!", true, "Actually, I don't really need it.", false)) {
            chatPlayer(happy, "Ok, I promise!")
            player.invAddOrDrop(objRepo, MONKEY_TALISMAN)
            objbox(MONKEY_TALISMAN, "The monkey child gives you some kind of talisman.")
        } else {
            chatPlayer(neutral, "Actually, I don't really need it.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bobawu (without a greegree)
    // TODO(content): [system:monkey_disguise] "Hey there, how do you do my fellow monkey?" with a
    // greegree equipped
    // TODO(content): [quest:monkey_madness_ii] the lap count, the Lap-count option and the monkey
    // transformation rewards
    private suspend fun Dialogue.bobawu() {
        chatPlayer(happy, "Hello, how are you doing?")
        chatNpc(happy, "I'm good thank you, how can I help you today?")
        val option =
            choice4(
                "What is this place?",
                1,
                "How many laps have I done?",
                2,
                "What can you offer me?",
                3,
                "Never mind.",
                4,
                title = "Select an Option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "This is our agility course, we use it to stay nimble and to keep our wits " +
                        "sharp. My name is Bobawu and I look after the course.",
                )
                chatNpc(
                    happy,
                    "As a friend and fellow monkey, you're free to use it anytime, but you'll " +
                        "need to be as nimble as a ninja first!",
                )
                chatPlayer(happy, "Oh great, thank you.")
                chatPlayer(quiz, "Are there any rewards you can offer me?")
                noRewards()
            }
            2 -> {
                chatPlayer(quiz, "Could you tell me how many laps I've completed?")
                noRewards()
            }
            3 -> {
                chatPlayer(quiz, "What can you offer me?")
                noRewards()
            }
            4 -> {
                chatPlayer(neutral, "On second thought, never mind.")
                chatNpc(happy, "That's okay my friend, come back anytime.")
            }
        }
    }

    private suspend fun Dialogue.noRewards() {
        chatNpc(
            neutral,
            "I can't offer you anything for now, so I don't see much reason to count your laps " +
                "either.",
        )
        mesbox(
            "You need to complete Monkey Madness 2 for Bobawu to offer rewards and to count your " +
                "lap total",
        )
    }

    private companion object {
        const val MONKEY_TALISMAN = "obj.mm_monkey_talisman"
    }
}
