package org.rsmod.content.skills.cooking

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BurntPieEvents : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1("obj.burnt_pie") { emptyDish(it.slot) }
    }

    private fun ProtectedAccess.emptyDish(slot: Int) {
        val dish = checkNotNull(ServerCacheManager.getItem("obj.piedish".asRSCM(RSCMType.OBJ)))
        if (invReplaceSlot(inv, slot, 1, dish).failure) {
            return
        }
        mes("You remove the burnt pie from the pie dish.", ChatType.Spam)
    }
}
