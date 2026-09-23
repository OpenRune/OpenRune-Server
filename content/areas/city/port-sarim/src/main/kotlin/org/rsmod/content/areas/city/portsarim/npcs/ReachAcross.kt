package org.rsmod.content.areas.city.portsarim.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.ScriptContext

private const val ACROSS_RANGE = 2

internal fun ScriptContext.onTalkAcross(npc: String, action: suspend ProtectedAccess.(Npc) -> Unit) {
    onOpNpc1(npc) { action(it.npc) }
    onApNpc1(npc) { reachAcross(it.npc, action) }
}

internal fun ScriptContext.onTradeAcross(npc: String, action: suspend ProtectedAccess.(Npc) -> Unit) {
    onOpNpc3(npc) { action(it.npc) }
    onApNpc3(npc) { reachAcross(it.npc, action) }
}

private suspend fun ProtectedAccess.reachAcross(
    npc: Npc,
    action: suspend ProtectedAccess.(Npc) -> Unit,
) {
    if (isWithinDistance(npc, ACROSS_RANGE)) {
        action(npc)
        return
    }
    apRange(ACROSS_RANGE)
}
