package org.rsmod.content.other.pets

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.events.interact.NpcUDefaultEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc2
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.script.onOpNpcU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

fun petOpIndex(npc: String, op: String): Int? {
    val type = ServerCacheManager.getNpc(npc.asRSCM(RSCMType.NPC)) ?: error("Npc not found: $npc")
    return (0 until 5).firstOrNull { type.actions.getOpOrNull(it).equals(op, ignoreCase = true) }?.plus(1)
}

fun ScriptContext.onPetOp(npc: String, op: String, action: suspend ProtectedAccess.(Npc) -> Unit) {
    val faceThenRun: suspend ProtectedAccess.(Npc) -> Unit = { target ->
        player.faceNpc(target)
        action(target)
    }
    when (petOpIndex(npc, op) ?: error("Npc $npc has no '$op' op")) {
        1 -> onOpNpc1(npc) { faceThenRun(it.npc) }
        2 -> onOpNpc2(npc) { faceThenRun(it.npc) }
        3 -> onOpNpc3(npc) { faceThenRun(it.npc) }
        4 -> onOpNpc4(npc) { faceThenRun(it.npc) }
        5 -> onOpNpc5(npc) { faceThenRun(it.npc) }
    }
}

/** Item-on-pet equivalent of [onPetOp]; turns the player towards the pet before the handler runs. */
fun ScriptContext.onPetOpU(npc: String, action: suspend ProtectedAccess.(NpcUDefaultEvents.OpType) -> Unit) {
    onOpNpcU(npc) {
        player.faceNpc(it.npc)
        action(it)
    }
}

/** Binds [op] only when the npc type actually declares it, for ops that vary between forms. */
fun ScriptContext.onPetOpIfPresent(npc: String, op: String, action: suspend ProtectedAccess.(Npc) -> Unit) {
    if (petOpIndex(npc, op) != null) {
        onPetOp(npc, op, action)
    }
}
