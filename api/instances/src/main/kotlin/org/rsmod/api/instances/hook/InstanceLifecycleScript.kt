package org.rsmod.api.instances.hook

import jakarta.inject.Inject
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.game.MapClock
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

internal class InstanceLifecycleScript
@Inject
constructor(
    private val manager: InstanceManager,
    private val worldClock: MapClock,
    private val collision: CollisionFlagMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogout { manager.handleLogout(player, worldClock.cycle) }
    }
}
