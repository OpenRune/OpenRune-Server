package org.rsmod.content.skills.prayer.ecto

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EctoBonemealEvents : PluginScript() {
    override fun ScriptContext.startup() {
        ECTO_RECIPES.distinctBy { it.bonemeal }.forEach { recipe ->
            onOpHeld1(recipe.bonemeal) {
                emptyBonemealPot(recipe.bonemeal)
            }
        }
        onOpHeld1("obj.bucket_ectoplasm") { emptySlimeBucket() }
    }

    private fun ProtectedAccess.emptyBonemealPot(bonemeal: String) {
        if (invDel(inv, bonemeal, 1).failure) {
            return
        }
        if (invAdd(inv, "obj.pot_empty", 1).failure) {
            invAdd(inv, bonemeal, 1)
            mes("You don't have enough inventory space.")
            return
        }
        mes("You empty the pot of bonemeal.")
    }

    private fun ProtectedAccess.emptySlimeBucket() {
        if (invDel(inv, "obj.bucket_ectoplasm", 1).failure) {
            return
        }
        if (invAdd(inv, "obj.bucket_empty", 1).failure) {
            invAdd(inv, "obj.bucket_ectoplasm", 1)
            mes("You don't have enough inventory space.")
            return
        }
        mes("You empty the bucket.")
    }
}
