package org.rsmod.content.skills.cooking

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.SkillMultiConfig
import org.rsmod.content.skills.SkillMultiEntry
import org.rsmod.content.skills.openSkillMulti
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DoughMakingEvents : PluginScript() {

    private val waterSources = listOf(
        "obj.jug_water" to "obj.jug_empty",
        "obj.bucket_water" to "obj.bucket_empty",
        "obj.bowl_water" to "obj.bowl_empty",
    )

    override fun ScriptContext.startup() {
        waterSources.forEach { (water, emptyContainer) ->
            onOpHeldU("obj.pot_flour", water) { chooseDough(water, emptyContainer) }
        }
    }

    private suspend fun ProtectedAccess.chooseDough(water: String, emptyContainer: String) {
        val materials = listOf(Material("obj.pot_flour"), Material(water))
        openSkillMulti(SkillMultiConfig(
            verb = "make",
            entries = listOf(
                SkillMultiEntry("obj.bread_dough", materials),
                SkillMultiEntry("obj.pastry_dough", materials),
                SkillMultiEntry("obj.pizza_base", materials),
            ),
        )) { selection ->
            val dough = selection.entry.internal
            val amount = selection.amount
            repeat(amount) {
                if (!inv.contains("obj.pot_flour") || !inv.contains(water)) return@repeat
                invDel(inv, "obj.pot_flour", 1)
                invDel(inv, water, 1)
                invAdd(inv, dough, 1)
                invAdd(inv, "obj.pot_empty", 1)
                invAdd(inv, emptyContainer, 1)
            }
            val name = selection.entry.item.name
            mes("You mix the flour and water to make some $name.")
        }
    }
}
