package org.rsmod.content.skills.mining.scripts

import jakarta.inject.Inject
import org.rsmod.api.script.onGameStartup
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CalcifiedMineScript @Inject constructor(private val mine: CalcifiedMine) : PluginScript() {
    override fun ScriptContext.startup() {
        onGameStartup { mine.start() }
    }
}
