package org.rsmod.content.skills.cooking

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PizzaAssemblyEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU("obj.knife", "obj.pineapple") { slicePineapple() }

        onOpHeldU("obj.pizza_base", "obj.tomato") { addTomato() }
        onOpHeldU("obj.incomplete_pizza", "obj.cheese") { addCheese() }

        onOpHeldU("obj.plain_pizza", "obj.anchovies") { addTopping("obj.anchovies", "obj.anchovie_pizza") }
        onOpHeldU("obj.plain_pizza", "obj.cooked_meat") { addTopping("obj.cooked_meat", "obj.meat_pizza") }
        onOpHeldU("obj.plain_pizza", "obj.cooked_chicken") { addTopping("obj.cooked_chicken", "obj.meat_pizza") }
        onOpHeldU("obj.plain_pizza", "obj.pineapple_chunks") { addTopping("obj.pineapple_chunks", "obj.pineapple_pizza") }
        onOpHeldU("obj.plain_pizza", "obj.pineapple_ring") { addTopping("obj.pineapple_ring", "obj.pineapple_pizza") }
    }

    private suspend fun ProtectedAccess.slicePineapple() {
        openSkillMulti(SkillMultiConfig(
            verb = "cut",
            entries = listOf(
                SkillMultiEntry("obj.pineapple_chunks", listOf(Material("obj.pineapple"))),
                SkillMultiEntry("obj.pineapple_ring", listOf(Material("obj.pineapple"))),
            ),
        )) { selection ->
            repeat(selection.amount) {
                if (!inv.contains("obj.pineapple")) return@repeat
                invDel(inv, "obj.pineapple", 1)
                invAdd(inv, selection.entry.internal, 1)
            }
        }
    }

    private fun ProtectedAccess.addTomato() {
        invDel(inv, "obj.pizza_base", 1)
        invDel(inv, "obj.tomato", 1)
        invAdd(inv, "obj.incomplete_pizza", 1)
        mes("You add the tomato to the pizza base.")
    }

    private fun ProtectedAccess.addCheese() {
        invDel(inv, "obj.incomplete_pizza", 1)
        invDel(inv, "obj.cheese", 1)
        invAdd(inv, "obj.uncooked_pizza", 1)
        mes("You add the cheese to the pizza.")
    }

    private fun ProtectedAccess.addTopping(topping: String, result: String) {
        invDel(inv, "obj.plain_pizza", 1)
        invDel(inv, topping, 1)
        invAdd(inv, result, 1)
        mes("You add the topping to the pizza.")
    }
}
