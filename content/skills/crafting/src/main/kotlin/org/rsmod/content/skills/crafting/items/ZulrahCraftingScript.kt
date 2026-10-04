package org.rsmod.content.skills.crafting.items

import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.Material
import org.rsmod.content.skills.crafting.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ZulrahCraftingScript : PluginScript() {
    override fun ScriptContext.startup() {
        val blowpipe = CraftingProduct(
            section = CraftingSection.COMBINING,
            output = "obj.toxic_blowpipe",
            inputs = listOf(Material("obj.blowpipe_fang", 1)),
            level = 1, xp = 0.0, ticks = listOf(1), tools = listOf("obj.chisel"),
            extraReqs = listOf(CraftingStatReq("stat.fletching", "Fletching", 78)),
            extraXp = listOf(CraftingStatXp("stat.fletching", 120.0)),
            actionName = "create a toxic blowpipe",
            successMessage = "You carve the fang into a toxic blowpipe.",
        )
        CraftingRecipes.register(blowpipe)
        registerHeldCrafting(blowpipe)
        for (suffix in listOf("_i", "_orn", "_i_orn")) {
            val product = CraftingProduct(
                section = CraftingSection.COMBINING,
                output = "obj.toxic_tots${suffix.replace("_orn", "")}_uncharged${if (suffix.endsWith("_orn")) "_orn" else ""}",
                inputs = listOf(Material("obj.magic_fang", 1), Material("obj.tots${suffix.replace("_orn", "")}_uncharged${if (suffix.endsWith("_orn")) "_orn" else ""}", 1)),
                level = 59, xp = 0.0, ticks = listOf(1), tools = listOf("obj.chisel"),
                actionName = "create a trident of the swamp",
            )
            CraftingRecipes.register(product)
            onOpHeldU(product.inputs[1].internal, "obj.magic_fang") { craftInstantly(product) }
        }
    }
}
