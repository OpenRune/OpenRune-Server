package org.rsmod.content.areas.city.desert.carpet.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CarpetMonkey : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.magic_carpet_monkey") { startDialogue(it.npc) { monkey() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Monkey_(magic_carpet)
    private suspend fun Dialogue.monkey() {
        chatPlayer(happy, "Who's a cute little monkey?")
        val understands =
            MONKEYSPEAK_AMULET in player.worn ||
                QuestRequirements.hasCompleted(player, MONKEY_MADNESS_II)
        if (!understands) {
            chatNpc(happy, "Ukkuk oook! Eeek aka, ahh aka gonk.")
            return
        }
        chatNpc(angry, "Who's an ugly human? Give me a banana!")
        chatPlayer(confused, "What's up with you?")
        chatNpc(angry, "Stupid human! Give monkey a banana!")
        if (BANANA !in player.inv) {
            chatPlayer(sad, "Sorry monkey, I don't have any bananas.")
            chatNpc(angry, "Aghhh. You're a rubbish human, get monkey a banana now.")
            return
        }
        val option =
            choice2("Give the monkey a banana.", 1, "Don't give it a banana.", 2)
        if (option == 2) {
            chatPlayer(neutral, "I'll not give you a banana until you learn manners.")
            return
        }
        if (access.invDel(player.inv, BANANA).failure) {
            return
        }
        chatNpc(laugh, "Ha ha! Smelly human gave monkey a banana.")
        chatPlayer(
            angry,
            "Wow you're one nasty piece of work. Have you ever heard of gratitude?",
        )
        chatNpc(laugh, "Hey baldy, give monkey another banana!")
        chatPlayer(
            angry,
            "Just because I'm not covered in fur doesn't make me bald, you cheeky monkey.",
        )
        chatNpc(angry, "Monkey wants another banana now! Give me, give me!")
        chatPlayer(angry, "Look I've had it with you, you little degenerate.")
    }

    private companion object {
        const val MONKEYSPEAK_AMULET = "obj.mm_amulet_of_monkey_speak"
        const val BANANA = "obj.banana"
        const val MONKEY_MADNESS_II = "quest_monkeymadness2"
    }
}
