package org.rsmod.content.skills.construction

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.table.construction.ConstructionDispenserRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Larders, shelves and tool racks hand out items; barrels and sinks fill what is used on them. */
class PohDispensers : PluginScript() {
    override fun ScriptContext.startup() {
        for (row in ConstructionDispenserRow.all()) {
            val loc = RSCM.getReverseMapping(RSCMType.LOC, row.loc.id)
            val take = row.take.map { RSCM.getReverseMapping(RSCMType.OBJ, it.id) }
            if (take.isNotEmpty()) {
                onOpLoc1(loc) { take(take) }
            }
            for ((fromType, intoType) in row.fill.chunked(2)) {
                val from = RSCM.getReverseMapping(RSCMType.OBJ, fromType.id)
                val into = RSCM.getReverseMapping(RSCMType.OBJ, intoType.id)
                onOpLocU(loc, from) { fill(from, into) }
            }
        }
    }

    private suspend fun ProtectedAccess.take(objs: List<String>) {
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        val obj =
            if (objs.size == 1) {
                objs.single()
            } else {
                objs.getOrNull(menu("Take what?", hotkeys = true, objs.map(::name))) ?: return
            }
        invAdd(inv, obj, 1)
    }

    private fun ProtectedAccess.fill(from: String, into: String) {
        if (invReplace(inv, from, 1, into).success) {
            spam("You fill the ${name(from).lowercase()}.")
        }
    }

    private fun name(obj: String): String =
        ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj
}
