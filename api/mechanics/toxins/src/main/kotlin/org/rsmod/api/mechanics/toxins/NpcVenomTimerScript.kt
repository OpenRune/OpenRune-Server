package org.rsmod.api.mechanics.toxins

import org.rsmod.api.mechanics.toxins.impl.NpcVenom
import org.rsmod.api.script.onNpcTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NpcVenomTimerScript : PluginScript() {
    override fun ScriptContext.startup() {
        onNpcTimer(NpcVenom.TIMER) { NpcVenom.tick(npc) }
    }
}
