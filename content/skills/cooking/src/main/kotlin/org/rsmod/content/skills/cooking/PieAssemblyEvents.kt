package org.rsmod.content.skills.cooking

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PieAssemblyEvents : PluginScript() {

    override fun ScriptContext.startup() {
        onOpHeldU("obj.pastry_dough", "obj.piedish") { makePieShell() }

        // Simple pies
        onOpHeldU("obj.pie_shell", "obj.redberries") { fillSimplePie("obj.redberries", "obj.uncooked_redberry_pie") }
        onOpHeldU("obj.pie_shell", "obj.cooked_meat") { fillSimplePie("obj.cooked_meat", "obj.uncooked_meat_pie") }
        onOpHeldU("obj.pie_shell", "obj.cooked_chicken") { fillSimplePie("obj.cooked_chicken", "obj.uncooked_meat_pie") }
        onOpHeldU("obj.pie_shell", "obj.cooking_apple") { fillSimplePie("obj.cooking_apple", "obj.uncooked_apple_pie") }
        onOpHeldU("obj.pie_shell", "obj.golovanova_top") { fillSimplePie("obj.golovanova_top", "obj.uncooked_botanical_pie") }
        onOpHeldU("obj.pie_shell", "obj.fossil_sulliuscep_cap") { fillSimplePie("obj.fossil_sulliuscep_cap", "obj.uncooked_mushroom_pie") }
        onOpHeldU("obj.pie_shell", "obj.dragonfruit") { fillSimplePie("obj.dragonfruit", "obj.uncooked_dragonfruit_pie") }

        // Mud pie
        onOpHeldU("obj.pie_shell", "obj.bucket_compost") { fillSimplePie("obj.bucket_compost", "obj.uncooked_mud_pie") }

        // Garden pie
        onOpHeldU("obj.pie_shell", "obj.tomato") { fillSimplePie("obj.tomato", "obj.unfinished_garden_pie_1") }
        onOpHeldU("obj.unfinished_garden_pie_1", "obj.onion") { fillComplex("obj.unfinished_garden_pie_1", "obj.onion", "obj.unfinished_garden_pie_2") }
        onOpHeldU("obj.unfinished_garden_pie_2", "obj.cabbage") { fillComplex("obj.unfinished_garden_pie_2", "obj.cabbage", "obj.uncooked_garden_pie") }

        // Fish pie
        onOpHeldU("obj.pie_shell", "obj.trout") { fillSimplePie("obj.trout", "obj.unfinished_fish_pie_1") }
        onOpHeldU("obj.unfinished_fish_pie_1", "obj.cod") { fillComplex("obj.unfinished_fish_pie_1", "obj.cod", "obj.unfinished_fish_pie_2") }
        onOpHeldU("obj.unfinished_fish_pie_2", "obj.potato") { fillComplex("obj.unfinished_fish_pie_2", "obj.potato", "obj.uncooked_fish_pie") }

        // Admiral pie
        onOpHeldU("obj.pie_shell", "obj.salmon") { fillSimplePie("obj.salmon", "obj.unfinished_admiral_pie_1") }
        onOpHeldU("obj.unfinished_admiral_pie_1", "obj.tuna") { fillComplex("obj.unfinished_admiral_pie_1", "obj.tuna", "obj.unfinished_admiral_pie_2") }
        onOpHeldU("obj.unfinished_admiral_pie_2", "obj.potato") { fillComplex("obj.unfinished_admiral_pie_2", "obj.potato", "obj.uncooked_admiral_pie") }

        // Wild pie
        onOpHeldU("obj.pie_shell", "obj.raw_bear_meat") { fillSimplePie("obj.raw_bear_meat", "obj.unfinished_wild_pie_1") }
        onOpHeldU("obj.unfinished_wild_pie_1", "obj.raw_chompy") { fillComplex("obj.unfinished_wild_pie_1", "obj.raw_chompy", "obj.unfinished_wild_pie_2") }
        onOpHeldU("obj.unfinished_wild_pie_2", "obj.raw_rabbit") { fillComplex("obj.unfinished_wild_pie_2", "obj.raw_rabbit", "obj.uncooked_wild_pie") }

        // Summer pie
        onOpHeldU("obj.pie_shell", "obj.strawberry") { fillSimplePie("obj.strawberry", "obj.unfinished_summer_pie_1") }
        onOpHeldU("obj.unfinished_summer_pie_1", "obj.watermelon") { fillComplex("obj.unfinished_summer_pie_1", "obj.watermelon", "obj.unfinished_summer_pie_2") }
        onOpHeldU("obj.unfinished_summer_pie_2", "obj.cooking_apple") { fillComplex("obj.unfinished_summer_pie_2", "obj.cooking_apple", "obj.uncooked_summer_pie") }
    }

    private fun ProtectedAccess.makePieShell() {
        invDel(inv, "obj.pastry_dough", 1)
        invDel(inv, "obj.piedish", 1)
        invAdd(inv, "obj.pie_shell", 1)
        mes("You line the pie dish with pastry dough.")
    }

    private fun ProtectedAccess.fillSimplePie(filling: String, result: String) {
        invDel(inv, "obj.pie_shell", 1)
        invDel(inv, filling, 1)
        invAdd(inv, result, 1)
        mes("You add the filling to the pie.")
    }

    private fun ProtectedAccess.fillComplex(base: String, filling: String, result: String) {
        invDel(inv, base, 1)
        invDel(inv, filling, 1)
        invAdd(inv, result, 1)
        mes("You add the filling to the pie.")
    }
}
