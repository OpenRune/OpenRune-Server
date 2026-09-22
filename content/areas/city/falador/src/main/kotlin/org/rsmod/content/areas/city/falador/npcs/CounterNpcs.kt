package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

private const val COUNTER_RANGE = 2

/**
 * A counter a player can be served across. When the player clicks from elsewhere in [room], they
 * walk to the tile in front of the counter facing the npc instead of pathing around the building.
 */
internal class ServiceCounter(
    private val room: Pair<CoordGrid, CoordGrid>,
    private val customerX: Int,
    private val counterZ: IntRange,
) {
    fun servingTile(access: ProtectedAccess, npc: Npc): CoordGrid? {
        val npcBehindCounter = npc.coords.x < customerX - 1
        if (!npcBehindCounter || !access.isWithinArea(room.first, room.second)) {
            return null
        }
        return CoordGrid(customerX, npc.coords.z.coerceIn(counterZ), npc.coords.level)
    }
}

internal fun ScriptContext.onCounterTalk(
    npc: String,
    counter: ServiceCounter? = null,
    action: suspend ProtectedAccess.(Npc) -> Unit,
) {
    onOpNpc1(npc) { action(it.npc) }
    onApNpc1(npc) { approachCounter(it.npc, counter, action) }
}

internal fun ScriptContext.onCounterTrade(
    npc: String,
    counter: ServiceCounter? = null,
    action: suspend ProtectedAccess.(Npc) -> Unit,
) {
    onOpNpc3(npc) { action(it.npc) }
    onApNpc3(npc) { approachCounter(it.npc, counter, action) }
}

private suspend fun ProtectedAccess.approachCounter(
    npc: Npc,
    counter: ServiceCounter?,
    action: suspend ProtectedAccess.(Npc) -> Unit,
) {
    if (isWithinDistance(npc, COUNTER_RANGE)) {
        action(npc)
        return
    }
    val tile = counter?.servingTile(this, npc)
    if (tile == null) {
        apRange(COUNTER_RANGE)
        return
    }
    walk(coords)
    delay(1)
    playerMove(tile)
    action(npc)
}
