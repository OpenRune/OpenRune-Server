package org.rsmod.content.areas.misc.lobbies.pestcontrol

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.rsmod.api.enums.PestControlEnums.pest_control_refund_points
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PestControlRefunds : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.pest_squire_refunds") {
            startDialogue(it.npc) { introduction() }
        }
        onOpNpcU("npc.pest_squire_refunds") {
            if (inv[it.invSlot]?.id == it.objType.id) {
                offerPestControlRefund(it.npc, it.invSlot)
            }
        }
    }

    private suspend fun Dialogue.introduction() {
        chatNpc(
            shifty,
            "Psst! Got any Void kit you don't want? Spare robes, spare helms, that kind of thing?",
        )
        if (!choice2("What if I do?", true, "No, I don't.", false)) {
            chatPlayer(neutral, "No, I don't.")
            return
        }
        chatPlayer(quiz, "What if I do?")
        chatNpc(
            shifty,
            "Might be able to take it off your hands for you. Show me what you've got, " +
                "and I'll see if I can get you some Commendation Points for it.",
        )
        for (slot in access.inv.indices.reversed()) {
            val item = access.inv[slot] ?: continue
            val type = ServerCacheManager.getItem(item.id) ?: continue
            if (pest_control_refund_points[type] == null) continue
            objbox(item, "You show the squire some of your Void kit.")
            if (access.inv[slot] !== item) return
            access.offerPestControlRefund(checkNotNull(npc), slot)
            return
        }
    }
}

internal suspend fun ProtectedAccess.offerPestControlRefund(npc: Npc, slot: Int) {
    val item = inv[slot] ?: return
    val type = ServerCacheManager.getItem(item.id) ?: return
    val value = pest_control_refund_points[type]
    startDialogue(npc) {
        if (value == null) {
            chatNpc(CONFUSED, "That's not really the sort of stuff I want.")
            return@startDialogue
        }
        val points = player.pestControlPoints
        val refund = minOf(value, (MAX_POINTS - points).coerceAtLeast(0))
        if (refund < value) {
            // UNVERIFIED: Capped-offer chathead mesanim (wiki transcript).
            chatNpc(
                neutral,
                "Just so you know, you've got a lot of Commendation points already, " +
                    "so I can't pay you my usual rate.",
            )
        }
        objbox(item, 400, "${type.name}:<br>The squire offers you $refund Commendation points.")
        if (!choice2("Accept", true, "Decline", false, "${type.name}: Exchange for $refund points?")) {
            return@startDialogue
        }
        val balance = player.pestControlPoints
        if (balance !in 0..MAX_POINTS || refund > MAX_POINTS - balance) return@startDialogue
        if (inv[slot] !== item) return@startDialogue
        if (player.invDel(inv, item.id, count = 1, slot = slot, strict = true).failure) {
            return@startDialogue
        }
        player.pestControlPoints = balance + refund
        chatNpc(happy, "Deal!")
    }
}

private const val MAX_POINTS = 4000
private val CONFUSED by lazy {
    val animation = checkNotNull(ServerCacheManager.getMesAnim("mesanim.confused".asRSCM()))
    animation.copy(len2 = animation.len1, len3 = animation.len1, len4 = animation.len1)
}
