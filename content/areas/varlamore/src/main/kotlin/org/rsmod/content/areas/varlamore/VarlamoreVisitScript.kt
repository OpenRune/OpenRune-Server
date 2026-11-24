package org.rsmod.content.areas.varlamore

import org.rsmod.api.script.onArea
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VarlamoreVisitScript : PluginScript() {
    override fun ScriptContext.startup() {
        onArea("area.varlamore") { vars["varbit.varlamore_visited"] = 1 }
    }
}
