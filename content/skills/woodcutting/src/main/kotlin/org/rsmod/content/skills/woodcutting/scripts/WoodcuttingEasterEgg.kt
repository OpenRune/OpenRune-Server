package org.rsmod.content.skills.woodcutting.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentMixedLocU
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WoodcuttingEasterEgg : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentMixedLocU("content.tree", "obj.herring") { treeHerring() }
        onOpContentMixedLocU("content.tree", "obj.raw_herring") { treeHerring() }
        onOpLocU("loc.redwoodtree_l", "obj.herring") { redwoodTreeHerring() }
        onOpLocU("loc.redwoodtree_r", "obj.raw_herring") { redwoodTreeHerring() }
    }

    private fun ProtectedAccess.treeHerring() {
        mes("This is not the mightiest tree in the forest.")
    }

    private fun ProtectedAccess.redwoodTreeHerring() {
        mes("This is not the mightiest tree in the forest; it is fairly mighty though.")
    }
}
