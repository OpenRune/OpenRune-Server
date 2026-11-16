package org.rsmod.content.skills.woodcutting.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentMixedLocU
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WoodcuttingEasterEgg : PluginScript() {
    override fun ScriptContext.startup() {
        for (herring in HERRINGS) {
            onOpContentMixedLocU("content.tree", herring) { treeHerring() }
            for (loc in REDWOOD_TREES) {
                onOpLocU(loc, herring) { redwoodTreeHerring() }
            }
            onOpLocU("loc.blisterwood_tree", herring) { blisterwoodTreeHerring() }
            for (loc in GRAND_TREE_DOORS) {
                onOpLocU(loc, herring) { grandTreeHerring() }
            }
        }
    }

    private fun ProtectedAccess.treeHerring() {
        mes("This is not the mightiest tree in the forest.")
    }

    private fun ProtectedAccess.redwoodTreeHerring() {
        mes("This is not the mightiest tree in the forest; it is fairly mighty though.")
    }

    private fun ProtectedAccess.blisterwoodTreeHerring() {
        mes("This is indeed a mighty tree. But alas, it is not the mightiest tree in the forest.")
    }

    private fun ProtectedAccess.grandTreeHerring() {
        mes("It can't be done!")
    }

    private companion object {
        val HERRINGS = listOf("obj.herring", "obj.raw_herring")
        val REDWOOD_TREES = listOf("loc.redwoodtree_l", "loc.redwoodtree_r")
        val GRAND_TREE_DOORS = listOf("loc.treedoorl", "loc.treedoorr")
    }
}
