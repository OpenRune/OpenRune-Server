package org.rsmod.content.areas.city.rellekka.miscellania.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Sailor
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:ship_travel] sailing between Rellekka and Miscellania, and the
// Miscellania (npc.viking_sailor) and Rellekka (npc.misc_sailor) quick-travel ops
class MiscellaniaSailors : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_sailor") { startDialogue(it.npc) { sailor() } }
        onOpNpc1("npc.misc_sailor") { startDialogue(it.npc) { sailor() } }
    }

    private suspend fun Dialogue.sailor() {
        if (QuestRequirements.hasNotCompleted(player, FREMENNIK_TRIALS)) {
            chatPlayer(happy, "Hiya.")
            chatNpc(
                angry,
                "Don't talk to me outerlander. I need to fix this longboat. Go talk to the " +
                    "chieftain.",
            )
            return
        }
        chatPlayer(quiz, "Hello. Can I get a ride on your ship?")
        val kin = if (access.isBodyTypeA()) "brother" else "sister"
        chatNpc(
            happy,
            "Hello again, $kin ${fremennikName()}. If you're ready to jump aboard, we're all " +
                "ready to set sail with the tide!",
        )
        when (choice2("Let's go!", 1, "Actually, no.", 2)) {
            1 -> chatPlayer(happy, "Let's go!")
            2 -> {
                chatPlayer(neutral, "Actually, no.")
                chatNpc(neutral, "Okay. Suit yourself.")
            }
        }
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
    }
}
