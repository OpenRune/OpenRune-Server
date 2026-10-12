package org.rsmod.content.areas.city.seersvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Murder_Mystery
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:murder_mystery] the in-quest dialogue
class SinclairMansion : PluginScript() {
    override fun ScriptContext.startup() {
        for (resident in MEN) {
            onOpNpc1(resident) { startDialogue(it.npc) { family("He is ignoring you.") } }
        }
        for (resident in WOMEN) {
            onOpNpc1(resident) { startDialogue(it.npc) { family("She is ignoring you.") } }
        }
        onOpNpc1(MARY) { startDialogue(it.npc) { mary() } }
        onOpNpc1("npc.hobbes_the_butler") { startDialogue(it.npc) { hobbes() } }
        onOpNpc1("npc.louisa_the_cook") { startDialogue(it.npc) { louisa() } }
        onOpNpc1("npc.donovan_the_family_handyman") { startDialogue(it.npc) { donovan() } }
    }

    private fun Dialogue.murderSolved(): Boolean =
        QuestRequirements.hasCompleted(player, MURDER_MYSTERY)

    private suspend fun Dialogue.family(ignoring: String) {
        if (murderSolved()) {
            chatNpc(angry, "Apparently you aren't as stupid as you look.")
            return
        }
        mesbox(ignoring)
    }

    private suspend fun Dialogue.thanks() {
        chatNpc(happy, "Thank you for all your help in solving the murder.")
    }

    private suspend fun Dialogue.mary() {
        if (murderSolved()) return thanks()
        mesbox("She is ignoring you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hobbes
    private suspend fun Dialogue.hobbes() {
        if (murderSolved()) return thanks()
        chatNpc(angry, "This is private property! Please leave!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Louisa
    private suspend fun Dialogue.louisa() {
        if (murderSolved()) return thanks()
        chatNpc(sad, "I'm far too upset to talk to random people right now.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Donovan_the_Family_Handyman
    private suspend fun Dialogue.donovan() {
        if (murderSolved()) return thanks()
        chatNpc(angry, "I have no interest in talking to gawkers.")
    }

    private companion object {
        val MEN = listOf("npc.frank_sinclair", "npc.david_sinclair")
        val WOMEN =
            listOf(
                "npc.anna_sinclair",
                "npc.carol_sinclair",
                "npc.elizabeth_sinclair",
            )
        const val MARY = "npc.mary_the_maid"
        const val MURDER_MYSTERY = "quest_murdermystery"
    }
}
