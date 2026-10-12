package org.rsmod.content.areas.city.desert.nardah.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Nardah_Banker
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NardahBanker : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BANKER) { startDialogue(it.npc) { banker() } }
        onOpNpc3(BANKER) { tryOpenBank() }
        onOpNpc4(BANKER) { openCollectionBox() }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    private suspend fun Dialogue.banker() {
        chatNpc(neutral, "Good day, how may I help you?")
        val option =
            choice4(
                "I'd like to access my bank account, please.",
                1,
                "I'd like to check my PIN settings.",
                2,
                "I'd like to collect items.",
                3,
                "What is this place?",
                4,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> access.ifOpenMainModal("interface.bankpin_settings")
            3 -> access.openCollectionBox()
            4 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    neutral,
                    "This is a branch of the Bank of Gielinor. We have branches in many towns.",
                )
                chatPlayer(quiz, "And what do you do?")
                chatNpc(
                    neutral,
                    "We will look after your items and money for you. Leave your valuables with " +
                        "us if you want to keep them safe.",
                )
            }
        }
    }

    private companion object {
        const val BANKER = "npc.elid_banker"
    }
}
