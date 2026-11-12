package org.rsmod.content.areas.zeah.region

import org.rsmod.api.script.onArea
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KourendVisitScript : PluginScript() {
    override fun ScriptContext.startup() {
        onArea("area.great_kourend") { vars["varbit.zeah_playerhasvisited"] = 1 }
    }
}
