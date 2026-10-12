package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:hazeel_cult] quest stages and the after-quest dialogue for either side
//  (Ceril, Henryeta, Claus and the guard; the side is part of the quest progress)
class CarnilleanMansion : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.sir_ceril_carnillean") { startDialogue(it.npc) { ceril() } }
        onOpNpc1("npc.carnillean_wife") { startDialogue(it.npc) { henryeta() } }
        onOpNpc1("npc.philipe_carnillean") { startDialogue(it.npc) { philipe() } }
        onOpNpc1("npc.butler_jones_hazeel_cultist") { startDialogue(it.npc) { butlerJones() } }
        onOpNpc1("npc.claus_carnillean") { startDialogue(it.npc) { claus() } }
        onOpNpc1("npc.guard_carnillean") { startDialogue(it.npc) { guard() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hazeel_Cult (starting off)
    // TODO(content): [quest:hazeel_cult] start offer after "a modest cash reward!"
    private suspend fun Dialogue.ceril() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            angry,
            "Blooming, thieving cultists! Why don't they leave me alone? String them all up, " +
                "that's what I say!",
        )
        val option =
            choice3(
                "What's wrong?",
                1,
                "You probably deserve it.",
                2,
                "You seem uptight, I'll leave you alone.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What's wrong?")
                chatNpc(
                    angry,
                    "It's those blooming cultists from the south! They keep breaking into my " +
                        "house!",
                )
                chatPlayer(quiz, "Have they taken much?")
                chatNpc(
                    neutral,
                    "Well... no. They stole a suit of armour when they first broke in months " +
                        "ago, but they've been back four more times since then and taken nothing.",
                )
                chatPlayer(quiz, "I see. And you are?")
                chatNpc(
                    shocked,
                    "You don't know me? Why, I am Sir Ceril Carnillean! Mine is quite a famous " +
                        "bloodline, you know. We've played a vital role in the politics of " +
                        "Ardougne for many generations.",
                )
                chatPlayer(
                    happy,
                    "Well I'm ${player.displayName}. Maybe I can help you with these cultists?",
                )
                chatNpc(
                    neutral,
                    "Interesting... I suppose the authorities haven't been much help so far. " +
                        "Yes, very well. If you return the stolen armour, I will provide you " +
                        "with a modest cash reward!",
                )
            }
            2 -> {
                chatPlayer(angry, "You probably deserve it.")
                chatNpc(
                    angry,
                    "And who are you to judge me? You look like a peasant anyway. I'm wasting my " +
                        "time talking to you.",
                )
            }
            3 -> {
                chatPlayer(neutral, "You seem uptight. I'll leave you alone.")
                chatNpc(neutral, "Yes, I doubt you could help anyway.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Henryeta_Carnillean
    private suspend fun Dialogue.henryeta() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            neutral,
            "Oh, hello. If you wish to look around the Carnillean family home, please refrain " +
                "from touching anything with those grubby hands of yours.",
        )
        chatPlayer(confused, "Okay...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Philipe_Carnillean
    private suspend fun Dialogue.philipe() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "What have you brought me? I want some more toys!")
        chatPlayer(neutral, "I'm afraid I don't have any toys.")
        chatNpc(angry, "Toys! I want toys!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Butler_Jones
    private suspend fun Dialogue.butlerJones() {
        chatPlayer(happy, "Hello there.")
        if (QuestRequirements.hasCompleted(player, HAZEEL_CULT)) {
            chatNpc(
                happy,
                "It's an honour to be in your presence again, adventurer. I hope things are well?",
            )
            chatPlayer(happy, "Not bad, thanks. Yourself?")
            chatNpc(
                neutral,
                "Unfortunately, I am still forced to deal with this insufferable family. Many " +
                    "generations have passed, but they are still the enemy. As such, they must " +
                    "be kept a close eye on.",
            )
            chatNpc(
                neutral,
                "Still, I have no doubt that the time will soon come for me to leave this place. " +
                    "Our lord will certainly have need of me elsewhere once his current work is " +
                    "complete.",
            )
            chatPlayer(happy, "I see. Well good luck with it all.")
            return
        }
        chatNpc(happy, "Hello. How are you today?")
        chatPlayer(happy, "Good thank you, and yourself?")
        chatNpc(happy, "Very well, thank you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Claus_the_Chef
    private suspend fun Dialogue.claus() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            neutral,
            "Sorry, can't stop to chat! You would be amazed at how many meals this family gets " +
                "through daily!",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guard_(Carnillean)
    private suspend fun Dialogue.guard() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            neutral,
            "Hello there. I haven't seen you around here before. If you've come to admire the " +
                "Carnillean family home, I must warn you not to cause any trouble.",
        )
        chatNpc(
            neutral,
            "Due to recent criminal activities against such prominent Ardougnians, I have been " +
                "sent here on special guard duty to ensure the Carnilleans' security.",
        )
        chatNpc(
            angry,
            "Anyone caught interfering with the person or possessions of any Carnilleans will " +
                "be taken to jail so quickly that their feet won't even touch the floor!",
        )
        chatPlayer(neutral, "Well, I wasn't actually planning on doing anything like that...")
        chatNpc(neutral, "Glad to hear it.")
    }

    private companion object {
        const val HAZEEL_CULT = "quest_hazeelcult"
    }
}
