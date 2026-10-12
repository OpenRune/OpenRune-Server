package org.rsmod.content.areas.misc.lobbies.pestcontrol.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class VoidBankSquireGapNpc : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BANK_SQUIRE) { startDialogue(it.npc) { bankSquire() } }
        onOpNpc3(BANK_SQUIRE) { tryOpenBank() }
        onOpNpc4(BANK_SQUIRE) { openCollectionBox() }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Squire_(Void_Knights_banker)
    // TODO(content): [system:bank_space_shop] "I'd like to buy more bank slots."
    private suspend fun Dialogue.bankSquire() {
        chatNpc(happy, "Good day, how may I help you?")
        val option =
            choice3(
                "I'd like to access my bank account, please.",
                1,
                "I'd like to check my PIN settings.",
                2,
                "I'd like to collect items.",
                3,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> access.ifOpenMainModal("interface.bankpin_settings")
            else -> access.openCollectionBox()
        }
    }

    private companion object {
        const val BANK_SQUIRE = "npc.pest_squire_bank_2"
    }
}
