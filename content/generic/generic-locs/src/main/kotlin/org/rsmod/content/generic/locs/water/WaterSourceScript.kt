package org.rsmod.content.generic.locs.water

import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Containers that fill at any water source, and what each one becomes. */
private val CONTAINERS =
    mapOf(
        "obj.bucket_empty" to "obj.bucket_water",
        "obj.jug_empty" to "obj.jug_water",
        "obj.bowl_empty" to "obj.bowl_water",
        "obj.vial_empty" to "obj.vial_water",
    )

class WaterSourceScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLocU("content.water_source") {
            fill(it.objType.internalName, it.objType.name, it.type.name)
        }
    }

    private fun ProtectedAccess.fill(container: String, containerName: String, sourceName: String) {
        val filled = CONTAINERS[container] ?: return
        anim("seq.human_pickuptable_walkmerge")
        invDel(inv, container, 1)
        invAdd(inv, filled, 1)
        mes(
            "You fill the ${containerName.lowercase()} from the ${sourceName.lowercase()}.",
            ChatType.Spam,
        )
    }
}
