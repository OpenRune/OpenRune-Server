package org.rsmod.content.skills.cooking

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CakeEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU("obj.cake_tin", "obj.egg") { tryCake() }
        onOpHeldU("obj.cake_tin", "obj.bucket_milk") { tryCake() }
        onOpHeldU("obj.cake_tin", "obj.pot_flour") { tryCake() }

        onOpHeldU("obj.cake", "obj.chocolate_bar") { addChocolate() }
    }

    private fun ProtectedAccess.tryCake() {
        if (!inv.contains("obj.cake_tin") || !inv.contains("obj.egg") ||
            !inv.contains("obj.bucket_milk") || !inv.contains("obj.pot_flour")) {
            mes("You need a cake tin, an egg, a bucket of milk, and a pot of flour to make a cake.")
            return
        }

        invDel(inv, "obj.cake_tin", 1)
        invDel(inv, "obj.egg", 1)
        invDel(inv, "obj.bucket_milk", 1)
        invDel(inv, "obj.pot_flour", 1)
        invAdd(inv, "obj.uncooked_cake", 1)
        invAdd(inv, "obj.bucket_empty", 1)
        invAdd(inv, "obj.pot_empty", 1)
        mes("You mix the ingredients into a cake tin.")
    }

    private fun ProtectedAccess.addChocolate() {
        invDel(inv, "obj.cake", 1)
        invDel(inv, "obj.chocolate_bar", 1)
        invAdd(inv, "obj.chocolate_cake", 1)
        mes("You add the chocolate to the cake.")
    }
}
