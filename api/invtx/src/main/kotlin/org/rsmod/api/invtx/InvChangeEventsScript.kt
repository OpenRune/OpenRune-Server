package org.rsmod.api.invtx

import jakarta.inject.Inject
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class InvChangeEventsScript @Inject constructor(private val eventBus: EventBus) :
    PluginScript() {
    override fun ScriptContext.startup() {
        cachedEventBus = eventBus
    }
}
