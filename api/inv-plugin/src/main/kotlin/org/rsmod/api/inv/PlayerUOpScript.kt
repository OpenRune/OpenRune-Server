package org.rsmod.api.inv

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.events.interact.PlayerTEvents
import org.rsmod.api.player.interact.PlayerUInteractions
import org.rsmod.api.player.output.UpdateInventory.resendSlot
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpPlayerT
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlayerUOpScript @Inject constructor(private val interactions: PlayerUInteractions) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpPlayerT(components.inv_items) { opPlayerU(it) }
    }

    private suspend fun ProtectedAccess.opPlayerU(op: PlayerTEvents.Op) {
        val objType =
            op.objType?.let { id ->
                ServerCacheManager.getItems().values.firstOrNull { it.id == id.id }
            } ?: return resendSlot(inv, 0)

        interactions.interactOp(this, op.target, inv, op.comsub, objType)
    }
}
