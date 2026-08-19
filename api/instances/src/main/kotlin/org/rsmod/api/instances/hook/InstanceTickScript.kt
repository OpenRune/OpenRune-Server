package org.rsmod.api.instances.hook

import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.script.onEvent
import org.rsmod.game.MapClock
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class InstanceTickScript
@Inject
constructor(private val worldClock: MapClock, private val manager: InstanceManager) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<GameLifecycle.LateCycle> { manager.tickReclaim(worldClock.cycle) }
    }
}
