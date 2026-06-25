package org.rsmod.api.instance

import jakarta.inject.Inject
import org.rsmod.api.player.hook.PlayerPostTickHook
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class InstanceScript
@Inject
constructor(private val manager: InstanceManager, private val instanceRepo: InstanceRepository) :
    PluginScript(), PlayerPostTickHook {
    override fun ScriptContext.startup() {
        onPlayerLogout { handleLogout() }
    }

    private fun SessionStateEvent.Logout.handleLogout() {
        val instance = manager.instanceOf(player) ?: return
        instance.fallbackPosition?.let { player.coords = it }
        instanceRepo.leave(instance, player)
    }

    override fun onPostTick(player: Player) {
        val instance = manager.instanceOf(player) ?: return
        if (player.regionUid != instance.region.uid) {
            instanceRepo.leave(instance, player)
        }
    }
}
