package org.rsmod.content.skills.firemaking

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.table.LightSourcesRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LightSourceEvents : PluginScript() {

    override fun ScriptContext.startup() {
        LightSourcesRow.all().forEach { row ->
            onOpHeldU("obj.tinderbox", row.unlit) { light(row) }
            onOpHeld2(row.lit) { extinguish(row) }
        }
    }

    private fun ProtectedAccess.light(source: LightSourcesRow) {
        if (player.firemakingLvl < source.level) {
            mes("You need a Firemaking level of ${source.level} to light this.")
            return
        }

        if (invReplace(inv, source.unlit.internalName, 1, source.lit.internalName).success) {
            mes("You light the ${source.unlit.name}.")
        }
    }

    private fun ProtectedAccess.extinguish(source: LightSourcesRow) {
        if (invReplace(inv, source.lit.internalName, 1, source.unlit.internalName).success) {
            mes("You extinguish the light source.")
        }
    }
}
