package org.rsmod.api.mechanics.toxins

import org.rsmod.api.mechanics.toxins.impl.NpcPoison
import org.rsmod.api.script.onNpcTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class NpcPoisonTimerScript : PluginScript() {
    override fun ScriptContext.startup() {
        onNpcTimer("timer.npc_poison") { NpcPoison.onPoisonTimerTick(npc) }
    }
}
