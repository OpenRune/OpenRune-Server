package org.rsmod.content.raids.toa.raid.encounter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onAiTimer
import org.rsmod.api.script.onApNpc1
import org.rsmod.api.script.onApNpc3
import org.rsmod.api.script.onNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.Hit
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.ScriptContext

internal inline fun <reified R : ToaEncounter> ScriptContext.onRoomNpcHit(
    type: String,
    noinline action: R.(Npc, Hit) -> Unit,
) {
    onNpcHit(checkNotNull(ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)))) {
        val room = ToaRooms.of<R>(npc) ?: return@onNpcHit
        action(room, npc, hit)
    }
}

internal inline fun <reified R : ToaEncounter> ScriptContext.onRoomNpcQueue(
    type: String,
    queue: String,
    noinline action: R.(Npc) -> Unit,
) {
    onNpcQueue(checkNotNull(ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC))), queue) {
        val room = ToaRooms.of<R>(npc) ?: return@onNpcQueue
        action(room, npc)
    }
}

internal inline fun <reified R : ToaEncounter> ScriptContext.onRoomAiTimer(
    type: String,
    noinline action: R.(Npc) -> Unit,
) {
    onAiTimer(type) {
        val room = ToaRooms.of<R>(npc) ?: return@onAiTimer
        action(room, npc)
    }
}

internal inline fun <reified R : ToaEncounter> ScriptContext.onRoomApNpc1(
    type: String,
    noinline action: suspend ProtectedAccess.(R, Npc) -> Unit,
) {
    onApNpc1(type) {
        val room = ToaRooms.of<R>(it.npc) ?: return@onApNpc1
        action(this, room, it.npc)
    }
}

internal inline fun <reified R : ToaEncounter> ScriptContext.onRoomApNpc3(
    type: String,
    noinline action: suspend ProtectedAccess.(R, Npc) -> Unit,
) {
    onApNpc3(type) {
        val room = ToaRooms.of<R>(it.npc) ?: return@onApNpc3
        action(this, room, it.npc)
    }
}

internal inline fun <reified R : ToaEncounter> ScriptContext.onRoomOpLoc1(
    type: String,
    noinline action: suspend ProtectedAccess.(R, BoundLocInfo) -> Unit,
) {
    onOpLoc1(type) {
        val room = ToaRooms.of<R>(player) ?: return@onOpLoc1
        action(this, room, it.loc)
    }
}
