package org.rsmod.api.stats.plugin.xp

import jakarta.inject.Inject
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class XpEventsScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        PlayerSkillXP.eventBus = eventBus
    }
}
