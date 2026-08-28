package org.rsmod.content.skills.fishing.scripts

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KylieMinnow : PluginScript() {

    override fun ScriptContext.startup() {
        for (npc in listOf(KYLIE_UNLOCKED, KYLIE_LOCKED, KYLIE_MULTI)) {
            onOpNpc1(npc) { openTrade(it.npc) }
            onOpNpc3(npc) { openTrade(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.openTrade(npc: Npc) {
        startDialogue(npc) { trade() }
    }

    private suspend fun Dialogue.trade() {
        chatNpc(
            happy,
            "Have you got any minnows for trade? I have plenty of nice, fresh sharks waiting for ya!",
        )
        val option = choice2("Yes, let's trade.", 1, "No thanks.", 2)
        if (option != 1) {
            chatPlayer(neutral, "No thanks.")
            return
        }

        val minnows = access.invTotal(access.inv, MINNOW)
        if (minnows < 40) {
            chatNpc(
                sad,
                "You'll be needing at least 40 minnows to trade for a shark! " +
                    "Come back and see me when you have some more!",
            )
            return
        }
        if (access.inv.isFull()) {
            chatNpc(neutral, "I can't trade you any sharks while you don't have any space for them!")
            return
        }

        chatNpc(happy, "I can give you a shark for every 40 minnows that you give me. How many sharks would you like?")
        val maxSharks = minnows / 40
        val requested = access.countDialog("How many sharks? (max $maxSharks)").coerceIn(0, maxSharks)
        if (requested <= 0) return

        var given = 0
        while (given < requested && access.invTotal(access.inv, MINNOW) >= 40) {
            access.invDel(access.inv, MINNOW, 40)
            val result = access.invAdd(access.inv, RAW_SHARK, 1)
            if (result.failure) {
                access.invAdd(access.inv, MINNOW, 40)
                break
            }
            given++
        }

        if (given > 0) {
            chatNpc(happy, "There you go! Enjoy your shark${if (given == 1) "" else "s"}!")
        } else {
            chatNpc(neutral, "I can't trade you any sharks while you don't have any space for them!")
        }
    }

    private companion object {
        private const val KYLIE_UNLOCKED = "npc.minnow_fisherman_unlocked"
        private const val KYLIE_LOCKED = "npc.minnow_fisherman_locked"
        private const val KYLIE_MULTI = "npc.minnow_fisherman_multi"
        private const val MINNOW = "obj.minnow"
        private const val RAW_SHARK = "obj.raw_shark"
    }
}
