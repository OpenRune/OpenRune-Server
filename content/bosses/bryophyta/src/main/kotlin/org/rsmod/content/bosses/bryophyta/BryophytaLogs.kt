package org.rsmod.content.bosses.bryophyta

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BryophytaLogs @Inject constructor() : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc1(LOG_WITH_AXE) { takeBronzeAxe() }
    }

    private fun ProtectedAccess.takeBronzeAxe() {
        if (invAdd(player.inv, BRONZE_AXE, 1).failure) {
            mes("You don't have enough inventory space.")
            return
        }
        mes("You take a bronze axe from the logs.", ChatType.Spam)
    }

    private companion object {
        private const val LOG_WITH_AXE = "loc.gb_log_withaxe"
        private const val BRONZE_AXE = "obj.bronze_axe"
    }
}
