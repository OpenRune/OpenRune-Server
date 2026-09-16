package org.rsmod.content.areas.city.draynor

import org.rsmod.api.script.onApNpc2
import org.rsmod.api.script.onOpNpc2
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorTrees : PluginScript() {
    override fun ScriptContext.startup() {
        // Tree swipes must not make auto-retaliation fight the scenery.
        for (tree in listOf("npc.nasty_tree", "npc.nasty_tree_unchoppable", "npc.nasty_tree_choppable")) {
            onOpNpc2(tree) {}
            onApNpc2(tree) {}
        }
    }
}
