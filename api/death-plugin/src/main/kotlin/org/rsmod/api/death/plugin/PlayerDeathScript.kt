package org.rsmod.api.death.plugin

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerDeath
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlayerDeathScript @Inject constructor(private val death: PlayerDeath) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueue("queue.death") { death.death(this) }
    }
}
