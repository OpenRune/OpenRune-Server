package org.rsmod.api.inv

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.NpcTDefaultEvents
import org.rsmod.api.player.interact.NpcUInteractions
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpcT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NpcUOpScript @Inject private constructor(private val interactions: NpcUInteractions) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpcT("component.inventory:items") { opNpcU(it) }
    }

    private suspend fun ProtectedAccess.opNpcU(op: NpcTDefaultEvents.Op) {
        val objType =
            op.objType?.let { id ->
                ServerCacheManager.getItems().values.firstOrNull { it.id == id.id }
            } ?: return resendSlot(inv, 0)

        interactions.interactOp(this, op.npc, inv, op.comsub, op.npcType, objType)
    }
}
