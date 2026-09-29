package org.rsmod.api.death.plugin

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NpcDeathScript @Inject constructor(private val death: NpcDeath) : PluginScript() {
    override fun ScriptContext.startup() {
        onNpcQueue("queue.death") { death.deathWithDrops(this) }
    }
}
