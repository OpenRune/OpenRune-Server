package org.rsmod.content.areas.misc.multiway

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onArea
import org.rsmod.api.script.onAreaExit
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SinglesPlusAreaScript : PluginScript() {
    private var ProtectedAccess.singlesPlus by boolVarBit("varbit.singleway_plus_indicator")

    override fun ScriptContext.startup() {
        onArea("area.singles_plus") { singlesPlus = true }
        onAreaExit("area.singles_plus") { singlesPlus = false }
    }
}
