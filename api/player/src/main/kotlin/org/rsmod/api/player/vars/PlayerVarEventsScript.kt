package org.rsmod.api.player.vars

import jakarta.inject.Inject
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlayerVarEventsScript @Inject constructor(private val eventBus: EventBus) :
    PluginScript() {
    override fun ScriptContext.startup() {
        PlayerVarWatch.eventBus = eventBus
    }
}
