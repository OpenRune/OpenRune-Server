package org.rsmod.content.minigames.sepulchre

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.enums.HallowedEnums.hallowed_tool_limits
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc5
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.selectedQuantity by intVarBit("varbit.farming_tools_selectedquantity")

/**
 * The Mysterious Stranger's tool storage: the hallowed tools and tokens a player leaves with him,
 * kept in the `hallowed_storage_*` varbits up to the cache's limit for each. The quantity buttons
 * pick which of Remove/Store-1, -5, -X and -All the first op is, as the interface's scripts show.
 */
class SepulchreToolStorageScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc5(STRANGER) { openStorage() }
        for (tool in TOOLS) {
            onIfModalButton(tool.stored) { withdraw(tool, it.op) }
            onIfModalButton(tool.carried) { store(tool, it.op) }
        }
        onIfModalButton(WITHDRAW_ALL) { TOOLS.forEach { withdraw(it, Int.MAX_VALUE) } }
        onIfModalButton(STORE_ALL) { TOOLS.forEach { store(it, Int.MAX_VALUE) } }
        onIfModalButton(QUANTITY_1) { player.selectedQuantity = ONE }
        onIfModalButton(QUANTITY_5) { player.selectedQuantity = FIVE }
        onIfModalButton(QUANTITY_ALL) { player.selectedQuantity = ALL }
        onIfModalButton(QUANTITY_X) { player.selectedQuantity = X }
    }

    private fun ProtectedAccess.openStorage() {
        ifOpenMainSidePair(MAIN, SIDE)
        for (button in listOf(QUANTITY_1, QUANTITY_5, QUANTITY_ALL, QUANTITY_X)) {
            ifSetEvents(button, -1..-1, IfEvent.Op1)
        }
    }

    private suspend fun ProtectedAccess.withdraw(tool: Tool, op: IfButtonOp) {
        if (op == IfButtonOp.Op10) {
            mes(tool.type.examine)
            return
        }
        val requested = requested(tool, op) ?: return
        withdraw(tool, requested)
    }

    private suspend fun ProtectedAccess.store(tool: Tool, op: IfButtonOp) {
        if (op == IfButtonOp.Op10) {
            mes(tool.type.examine)
            return
        }
        val requested = requested(tool, op) ?: return
        store(tool, requested)
    }

    private fun ProtectedAccess.withdraw(tool: Tool, requested: Int) {
        val stored = vars[tool.varbit]
        val room = if (tool.type.isStackable) requested else inv.freeSpace()
        val count = minOf(requested, stored, room)
        if (count <= 0) {
            if (stored > 0) {
                mes("You don't have enough inventory space.")
            }
            return
        }
        if (invAdd(inv, tool.obj, count, ignoreVirtualStorage = true).failure) {
            return
        }
        vars[tool.varbit] = stored - count
    }

    private fun ProtectedAccess.store(tool: Tool, requested: Int) {
        val stored = vars[tool.varbit]
        val carried = inv.physicalCount(tool.obj)
        val count = minOf(requested, carried, tool.limit - stored)
        if (count <= 0) {
            return
        }
        if (invDel(inv, tool.obj, count, ignoreVirtualStorage = true).failure) {
            return
        }
        vars[tool.varbit] = stored + count
    }

    private suspend fun ProtectedAccess.requested(tool: Tool, op: IfButtonOp): Int? {
        if (tool.limit <= 1) {
            return if (op == IfButtonOp.Op1) 1 else null
        }
        val order = QUANTITY_ORDERS[player.selectedQuantity] ?: return null
        return when (order.getOrNull(op.ordinal)) {
            ONE -> 1
            FIVE -> 5
            ALL -> Int.MAX_VALUE
            X -> countDialog()
            else -> null
        }
    }

    private class Tool(
        val obj: String,
        val varbit: String,
        val stored: String,
        val carried: String,
    ) {
        val type: ItemServerType
            get() = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))

        val limit: Int
            get() = hallowed_tool_limits[type] ?: 0
    }

    private companion object {
        const val STRANGER = "npc.hallowed_lobby_npc_3op"
        const val MAIN = "interface.hallowed_tools"
        const val SIDE = "interface.hallowed_tool_side"
        const val WITHDRAW_ALL = "component.hallowed_tools:withdrawal_all"
        const val STORE_ALL = "component.hallowed_tool_side:store_all"
        const val QUANTITY_1 = "component.hallowed_tools:quantity_1"
        const val QUANTITY_5 = "component.hallowed_tools:quantity_5"
        const val QUANTITY_X = "component.hallowed_tools:quantity_x"
        const val QUANTITY_ALL = "component.hallowed_tools:quantity_all"
        const val ONE = 0
        const val FIVE = 1
        const val ALL = 2
        const val X = 3

        val QUANTITY_ORDERS =
            mapOf(
                ONE to listOf(ONE, FIVE, X, ALL),
                FIVE to listOf(FIVE, ONE, X, ALL),
                X to listOf(X, ONE, FIVE, ALL),
                ALL to listOf(ALL, ONE, FIVE, X),
            )

        val TOOLS =
            listOf(
                Tool(
                    "obj.hallowed_grapple",
                    "varbit.hallowed_storage_grapple",
                    "component.hallowed_tools:grapple",
                    "component.hallowed_tool_side:grapple",
                ),
                Tool(
                    "obj.hallowed_focus",
                    "varbit.hallowed_storage_focus",
                    "component.hallowed_tools:focus",
                    "component.hallowed_tool_side:focus",
                ),
                Tool(
                    "obj.hallowed_symbol",
                    "varbit.hallowed_storage_symbol",
                    "component.hallowed_tools:symbol",
                    "component.hallowed_tool_side:symbol",
                ),
                Tool(
                    "obj.hallowed_hammer",
                    "varbit.hallowed_storage_hammer",
                    "component.hallowed_tools:hammer",
                    "component.hallowed_tool_side:hammer",
                ),
                Tool(
                    "obj.hallowed_ring",
                    "varbit.hallowed_storage_ring",
                    "component.hallowed_tools:ring",
                    "component.hallowed_tool_side:ring",
                ),
                Tool(
                    "obj.hallowed_token",
                    "varbit.hallowed_storage_token",
                    "component.hallowed_tools:token",
                    "component.hallowed_tool_side:token",
                ),
            )
    }
}
