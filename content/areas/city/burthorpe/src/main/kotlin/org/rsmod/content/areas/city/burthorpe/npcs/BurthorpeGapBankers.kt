package org.rsmod.content.areas.city.burthorpe.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:bank_space_shop] the "buy more bank slots" option of both bankers
class BurthorpeGapBankers : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(EMERALD_BENEDICT) { startDialogue(it.npc) { emeraldBenedict() } }
        onOpNpc3(EMERALD_BENEDICT) { tryOpenBank() }
        onOpNpc4(EMERALD_BENEDICT) { openCollectionBox() }
        onOpNpc1(JADE) { startDialogue(it.npc) { jade() } }
        onOpNpc3(JADE) { tryOpenBank() }
        onOpNpc4(JADE) { openCollectionBox() }
    }

    private fun ProtectedAccess.openCollectionBox() {
        ifOpenMainModal("interface.ge_collect")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Emerald_Benedict
    private suspend fun Dialogue.emeraldBenedict() {
        chatNpc(shifty, "Got anything you don't want to lose?")
        val option =
            choice4(
                "Yes actually, can you help?",
                1,
                "Yes, but can you show me my PIN settings?",
                2,
                "Yes, but I'd like to collect items now.",
                3,
                "Yes thanks, and I'll keep hold of it too.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Yes actually, can you help?")
                access.tryOpenBank()
            }
            2 -> {
                chatPlayer(quiz, "Yes, but can you show me my PIN settings?")
                access.ifOpenMainModal("interface.bankpin_settings")
            }
            3 -> {
                chatPlayer(quiz, "Yes, but I'd like to collect items now.")
                access.openCollectionBox()
            }
            else -> chatPlayer(neutral, "Yes thanks, and I'll keep hold of it too.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jade_(Warriors'_Guild)
    private suspend fun Dialogue.jade() {
        chatNpc(happy, "Greetings warrior, how may I help you?")
        val option =
            choice4(
                "I'd like to access my bank account, please.",
                1,
                "I'd like to check my PIN settings.",
                2,
                "I'd like to collect items.",
                3,
                "How long have you worked here?",
                4,
            )
        when (option) {
            1 -> access.tryOpenBank()
            2 -> access.ifOpenMainModal("interface.bankpin_settings")
            3 -> access.openCollectionBox()
            else -> {
                chatPlayer(quiz, "How long have you worked here?")
                chatNpc(happy, "Oh ever since the Guild opened. I like it here.")
                chatPlayer(quiz, "Why's that?")
                chatNpc(
                    happy,
                    "Well.. what with all these warriors around, there's not much chance of my " +
                        "bank being robbed is there!",
                )
            }
        }
    }

    private companion object {
        const val EMERALD_BENEDICT = "npc.roguesden_banker"
        const val JADE = "npc.warguild_banker"
    }
}
