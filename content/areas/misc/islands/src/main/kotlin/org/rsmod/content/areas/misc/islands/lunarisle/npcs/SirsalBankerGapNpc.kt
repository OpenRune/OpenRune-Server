package org.rsmod.content.areas.misc.islands.lunarisle.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class SirsalBankerGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SIRSAL_BANKER) { startDialogue(it.npc) { sirsalBanker() } }
        onOpNpc3(SIRSAL_BANKER) { tryOpenBank() }
        onOpNpc4(SIRSAL_BANKER) { openCollectionBox() }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sirsal_Banker
    // TODO(content): [verify:in_game] whether the Bank and Collect ops also need the seal
    private suspend fun Dialogue.sirsalBanker() {
        if (!access.hasSealOfPassage()) {
            chatPlayer(happy, "Hi, I...")
            chatNpc(angry, "What are you doing here, Fremennik?!")
            chatPlayer(worried, "I have a Seal of pass...")
            chatNpc(angry, "No You do not! Begone!")
            return
        }
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
            1 -> {
                chatPlayer(quiz, "I'd like to access my bank account, please.")
                access.tryOpenBank()
            }
            2 -> {
                chatPlayer(quiz, "I'd like to check my PIN settings.")
                access.ifOpenMainModal("interface.bankpin_settings")
            }
            3 -> {
                chatPlayer(quiz, "I'd like to collect items.")
                access.openCollectionBox()
            }
            else -> {
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
        const val SIRSAL_BANKER = "npc.lunar_moonclan_monk_man"
    }
}
