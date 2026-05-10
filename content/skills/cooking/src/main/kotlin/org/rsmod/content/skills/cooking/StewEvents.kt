package org.rsmod.content.skills.cooking

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class StewEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU("obj.bowl_water", "obj.potato") { addPotato() }
        onOpHeldU("obj.stew1", "obj.cooked_meat") { addMeat("obj.cooked_meat") }
        onOpHeldU("obj.stew1", "obj.cooked_chicken") { addMeat("obj.cooked_chicken") }
    }

    private fun ProtectedAccess.addPotato() {
        invDel(inv, "obj.bowl_water", 1)
        invDel(inv, "obj.potato", 1)
        invAdd(inv, "obj.stew1", 1)
        mes("You add the potato to the bowl of water.")
    }

    private fun ProtectedAccess.addMeat(meat: String) {
        invDel(inv, "obj.stew1", 1)
        invDel(inv, meat, 1)
        invAdd(inv, "obj.uncooked_stew", 1)
        mes("You add the meat to the stew.")
    }
}
