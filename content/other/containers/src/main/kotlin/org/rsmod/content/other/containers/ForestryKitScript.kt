package org.rsmod.content.other.containers

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Accepted CS2 uses twenty fixed item rows, each with twelve dynamic children. */
internal class ForestryKitScript @Inject constructor(private val store: ContainerStore) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ForestryContainers.KIT) { if (inv[it.slot]?.id == ForestryContainers.KIT.asRSCM()) open(this) }
        onOpWorn2(ForestryContainers.KIT) { if (worn[it.slot]?.id == ForestryContainers.KIT.asRSCM()) open(this) }
        onIfOpen(MAIN) { if (store.owns(player, ForestryContainers.kit)) player.startInvTransmit(player.invMap.getOrPut(DISPLAY)) }
        onIfClose(MAIN) { player.invMap[DISPLAY]?.let { player.stopInvTransmit(it); it.fillNulls() } }
        onIfModalButton("component.forestry_kit_main:list") { withdraw(it) }
        onIfModalButton("component.forestry_kit_side:items") { deposit(it) }
        onIfModalButton("component.forestry_kit_main:fill") { if (owned()) { for (item in inv.filterNotNull().toList()) store.store(player, ForestryContainers.kit, item.id, item.count); refresh() } }
        onIfModalButton("component.forestry_kit_main:empty") { if (owned()) { for ((id, count) in store.contents(player, ForestryContainers.kit).toMap()) store.withdraw(player, ForestryContainers.kit, id.toInt(), count, inv); refresh() } }
        for ((button, quantity) in listOf("1" to 0, "5" to 1, "x" to 2, "all" to 3)) onIfModalButton("component.forestry_kit_main:$button") { if (owned()) VarPlayerIntMapSetter.set(player, QUANTITY, quantity) }
    }
    internal fun open(access: ProtectedAccess) = with(access) {
        if (!owned()) return@with
        refresh()
        ifOpenMainSidePair(MAIN, "interface.forestry_kit_side")
        ifSetEvents("component.forestry_kit_main:list", 0..239, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6, IfEvent.Op10)
        ifSetEvents("component.forestry_kit_side:items", inv.indices, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op10)
        for (button in listOf("fill", "empty", "1", "5", "x", "all")) ifSetEvents("component.forestry_kit_main:$button", -1..0, IfEvent.Op1)
    }
    private fun ProtectedAccess.owned(): Boolean = store.owns(player, ForestryContainers.kit) && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    private fun ProtectedAccess.refresh() {
        val display = player.invMap.getOrPut(DISPLAY)
        display.fillNulls()
        for ((slot, item) in items.withIndex()) { val count = store.count(player, ForestryContainers.kit, item.asRSCM()); if (count > 0) display[slot] = InvObj(item, count) }
        invTransmit(display); invTransmit(inv)
    }
    private suspend fun ProtectedAccess.quantity(): Int = when (player.vars[QUANTITY]) { 1 -> 5; 2 -> countDialog(); 3 -> Int.MAX_VALUE; else -> 1 }
    private suspend fun ProtectedAccess.withdraw(event: IfModalButton) {
        if (!owned() || !player.ui.containsModal(MAIN) || event.comsub !in 0..239 || event.comsub % 12 != 0) return
        val index = event.comsub / 12
        val item = items.getOrNull(index) ?: return
        if (event.op == IfButtonOp.Op1) { VarPlayerIntMapSetter.set(player, "varbit.forestry_kit_selected_item", index + 1); return }
        if (event.op == IfButtonOp.Op10) { mes(dev.openrune.ServerCacheManager.getItem(item.asRSCM())!!.examine); return }
        val origin = coords
        val amount = when (event.op) { IfButtonOp.Op2 -> quantity(); IfButtonOp.Op3 -> 1; IfButtonOp.Op4 -> 5; IfButtonOp.Op5 -> countDialog(); IfButtonOp.Op6 -> Int.MAX_VALUE; else -> return }
        ifCloseChat()
        if (amount > 0 && owned() && coords == origin && player.ui.containsModal(MAIN)) { store.withdraw(player, ForestryContainers.kit, item.asRSCM(), amount, inv); refresh() }
    }
    private suspend fun ProtectedAccess.deposit(event: IfModalButton) {
        if (!owned() || !player.ui.containsModal(MAIN)) return
        val reference = inv[event.comsub] ?: return
        if (event.obj?.id != reference.id) return
        if (event.op == IfButtonOp.Op10) { objExamine(inv, event.comsub); return }
        val origin = coords
        val amount = when (event.op) { IfButtonOp.Op1 -> quantity(); IfButtonOp.Op2 -> 1; IfButtonOp.Op3 -> 5; IfButtonOp.Op4 -> countDialog(); IfButtonOp.Op5 -> Int.MAX_VALUE; else -> return }
        ifCloseChat()
        if (amount > 0 && owned() && coords == origin && player.ui.containsModal(MAIN) && inv[event.comsub] === reference) { store.store(player, ForestryContainers.kit, reference.id, amount); refresh() }
    }
    companion object {
        const val MAIN = "interface.forestry_kit_main"
        private const val DISPLAY = "inv.forestry_kit"
        private const val QUANTITY = "varbit.forestry_kit_selectedquantity"

        // Native enum 817 in the accepted client cache; newer dictionaries name that ID differently.
        val items = listOf("obj.forestry_currency", "obj.nature_offerings", "obj.forestry_ration", "obj.forestry_secateurs_attachment", "obj.leaves", "obj.leaves_oak", "obj.leaves_willow", "obj.leaves_yew", "obj.leaves_maple", "obj.leaves_magic", "obj.ramble_lumberjack_top", "obj.ramble_lumberjack_legs", "obj.ramble_lumberjack_hat", "obj.ramble_lumberjack_boots", "obj.forestry_lumberjack_top", "obj.forestry_lumberjack_legs", "obj.forestry_lumberjack_hat", "obj.forestry_lumberjack_boots", "obj.skillcape_woodcutting", "obj.skillcape_woodcutting_trimmed")
    }
}
