package org.rsmod.api.mechanics.burn

import jakarta.inject.Inject
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class BurnTimerScript @Inject constructor(private val burn: Burn) : PluginScript() {
    override fun ScriptContext.startup() {
        onNpcTimer(Burn.NPC_TIMER) { burn.tick(npc) }
        onPlayerTimer(Burn.PLAYER_TIMER) { burn.tick(player) }
    }
}
