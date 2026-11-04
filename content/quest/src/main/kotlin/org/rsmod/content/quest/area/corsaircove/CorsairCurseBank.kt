package org.rsmod.content.quest.area.corsaircove

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onApNpc4
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.content.interfaces.bank.tryOpenBank
import org.rsmod.content.quest.manager.Quest
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

internal class CorsairCurseBank(private val quest: Quest) {

    fun ScriptContext.register() {
        for (npc in Yusuf) {
            onApNpc1(npc) { apTalkToBanker(it.npc) }
            onApNpc3(npc) { apOpenBank(it.npc) }
            onApNpc4(npc) { apOpenCollectionBox(it.npc) }

            onOpNpc1(npc) { startDialogue(it.npc) { yusuf() } }
            onOpNpc3(npc) { openBank(it.npc) }
            onOpNpc4(npc) { openCollectionBox(it.npc) }
        }
    }

    private suspend fun Dialogue.yusuf() {
        if (quest.getQuestStage(player) < CorsairCurseStage.Complete) {
            chatNpc(neutral, BankClosed)
            return
        }

        chatNpc(quiz, "I've agreed to let you use the bank. What do you want today?")

        while (true) {
            val option =
                choice5(
                    "This is an odd-looking bank.",
                    1,
                    "I'd like to access my bank account, please.",
                    2,
                    "I'd like to check my PIN settings.",
                    3,
                    "I'd like to collect items.",
                    4,
                    "I want nothing.",
                    5,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "This is an odd-looking bank.")
                    chatNpc(
                        neutral,
                        "It's an odd-looking town. Those Corsairs are basically no better than " +
                            "pirates, you know, and the adventurers they bring in are worse. " +
                            "That's why I keep a cannon for protection.",
                    )
                    chatNpc(quiz, "Anyway, did you want something?")
                }
                2 -> {
                    access.tryOpenBank()
                    return
                }
                3 -> {
                    access.ifOpenMainModal("interface.bankpin_settings")
                    return
                }
                4 -> {
                    access.ifOpenMainModal("interface.ge_collect")
                    return
                }
                else -> {
                    chatPlayer(neutral, "I want nothing.")
                    chatNpc(neutral, "Then you shall get it.")
                    return
                }
            }
        }
    }

    private suspend fun ProtectedAccess.apOpenCollectionBox(npc: Npc) {
        if (isWithinApRange(npc, distance = 2)) {
            openCollectionBox(npc)
        }
    }

    private suspend fun ProtectedAccess.apOpenBank(npc: Npc) {
        if (isWithinApRange(npc, distance = 2)) {
            openBank(npc)
        }
    }

    private suspend fun ProtectedAccess.apTalkToBanker(npc: Npc) {
        if (isWithinApRange(npc, distance = 2)) {
            startDialogue(npc) { yusuf() }
        }
    }

    private suspend fun ProtectedAccess.openBank(npc: Npc) {
        if (quest.getQuestStage(player) < CorsairCurseStage.Complete) {
            startDialogue(npc) { chatNpc(neutral, BankClosed) }
            return
        }
        tryOpenBank()
    }

    private suspend fun ProtectedAccess.openCollectionBox(npc: Npc) {
        if (quest.getQuestStage(player) < CorsairCurseStage.Complete) {
            startDialogue(npc) { chatNpc(neutral, BankClosed) }
            return
        }
        ifOpenMainModal("interface.ge_collect")
    }

    private companion object {
        private val Yusuf =
            listOf(
                "npc.corscurs_banker_1op",
                "npc.corscurs_banker_allops",
                "npc.corscurs_banker_multi",
            )

        private const val BankClosed = "Bank's closed. Go away."
    }
}
