package org.rsmod.content.travel.skillcapes

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.hook.ItemTeleportCaster
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldSubOp
import org.rsmod.api.script.onOpWorn3
import org.rsmod.api.script.onOpWorn4
import org.rsmod.api.script.onOpWorn5
import org.rsmod.api.script.onOpWornSubOp
import org.rsmod.api.table.SkillcapeTeleportDestinationsRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SkillcapeTeleportScript @Inject constructor(private val caster: ItemTeleportCaster) :
    PluginScript() {
    private val byCape: Map<String, List<SkillcapeTeleportDestinationsRow>> =
        SkillcapeTeleportDestinationsRow.all()
            .flatMap { row -> row.capes.map { RSCM.getReverseMapping(RSCMType.OBJ, it.id) to row } }
            .groupBy({ it.first }, { it.second })

    override fun ScriptContext.startup() {
        for ((cape, rows) in byCape) {
            for (row in rows) {
                val heldOp = row.heldOp
                if (heldOp != null && row.heldSubop == null) {
                    bindHeldOp(cape, heldOp, row)
                }
                val wornOp = row.wornOp
                if (wornOp != null && row.wornSubop == null) {
                    bindWornOp(cape, wornOp, row)
                }
            }
            val heldSubMenu = rows.filter { it.heldSubop != null }
            if (heldSubMenu.isNotEmpty()) {
                onOpHeldSubOp(cape) { event ->
                    heldSubMenu
                        .firstOrNull { it.heldOp == event.op.slot && it.heldSubop == event.subop }
                        ?.let { teleport(it) }
                }
            }
            val wornSubMenu = rows.filter { it.wornSubop != null }
            if (wornSubMenu.isNotEmpty()) {
                onOpWornSubOp(cape) { event ->
                    wornSubMenu
                        .firstOrNull { it.wornOp == event.op && it.wornSubop == event.subop }
                        ?.let { teleport(it) }
                }
            }
        }
    }

    private fun ScriptContext.bindHeldOp(
        cape: String,
        op: Int,
        row: SkillcapeTeleportDestinationsRow,
    ) {
        when (op) {
            3 -> onOpHeld3(cape) { teleport(row) }
            4 -> onOpHeld4(cape) { teleport(row) }
            else -> error("Unsupported held op $op for $cape")
        }
    }

    private fun ScriptContext.bindWornOp(
        cape: String,
        op: Int,
        row: SkillcapeTeleportDestinationsRow,
    ) {
        when (op) {
            3 -> onOpWorn3(cape) { teleport(row) }
            4 -> onOpWorn4(cape) { teleport(row) }
            5 -> onOpWorn5(cape) { teleport(row) }
            else -> error("Unsupported worn op $op for $cape")
        }
    }

    private suspend fun ProtectedAccess.teleport(row: SkillcapeTeleportDestinationsRow) {
        if (actionDelay > mapClock) {
            return
        }
        val stat = row.stat
        if (stat != null && statBase(RSCM.getReverseMapping(RSCMType.STAT, stat.id)) < MAX_LEVEL) {
            mes("You need to have a level of $MAX_LEVEL in that skill.")
            return
        }
        with(caster) { cast(TeleportType.Standard, row.coords) }
    }

    private companion object {
        private const val MAX_LEVEL = 99
    }
}
