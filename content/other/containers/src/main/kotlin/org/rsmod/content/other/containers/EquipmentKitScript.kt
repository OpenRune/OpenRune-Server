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
import org.rsmod.game.inv.Inventory
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** These two native screens share quantity controls and retain their original persistent inventories. */
internal class EquipmentKitScript @Inject constructor(private val store: ContainerStore) : PluginScript() {
    override fun ScriptContext.startup() {
        for (kit in kits) {
            onOpHeld1(kit.item) { if (inv[it.slot]?.id == kit.item.asRSCM()) open(kit) }
            onIfOpen(kit.main) { if (store.owns(player, kit.spec)) player.startInvTransmit(player.invMap.getOrPut(kit.storage)) }
            onIfClose(kit.main) { player.invMap[kit.storage]?.let { player.stopInvTransmit(it) } }
            onIfModalButton(kit.comp("items")) { transfer(kit, it, false) }
            onIfModalButton(kit.sideComp) { transfer(kit, it, true) }
            onIfModalButton(kit.comp("fill")) {
                if (!owned(kit)) return@onIfModalButton
                for (item in inv.filterNotNull().map { it.id }.distinct()) store.store(player, kit.spec, item, Int.MAX_VALUE)
                refresh(kit)
            }
            onIfModalButton(kit.comp("empty")) {
                if (!owned(kit)) return@onIfModalButton
                for ((id, count) in store.contents(player, kit.spec).toMap()) store.withdraw(player, kit.spec, id.toInt(), count, inv)
                refresh(kit)
            }
            for ((name, quantity) in listOf("1" to 0, "5" to 1, "all" to 2, "x" to 3)) {
                onIfModalButton(kit.comp(name)) { if (owned(kit)) VarPlayerIntMapSetter.set(player, QUANTITY, quantity) }
            }
        }
    }

    private fun ProtectedAccess.open(kit: Kit) {
        if (!store.owns(player, kit.spec)) return
        invTransmit(player.invMap.getOrPut(kit.storage))
        ifOpenMainSidePair(kit.main, kit.side)
        runClientScript(kit.initScript, *listOf("universe", "fill", "empty", "items", "scrollbar", "1", "5", "x", "all").map { kit.comp(it).asRSCM() }.toTypedArray())
        runClientScript(kit.sideScript, kit.sideComp.asRSCM())
        ifSetEvents(kit.comp("items"), player.invMap.getOrPut(kit.storage).indices, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op10)
        ifSetEvents(kit.sideComp, inv.indices, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op10)
        for (button in listOf("fill", "empty", "1", "5", "x", "all")) ifSetEvents(kit.comp(button), -1..0, IfEvent.Op1)
    }

    private fun ProtectedAccess.owned(kit: Kit): Boolean {
        if (store.owns(player, kit.spec) && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut) return true
        ifClose(); return false
    }

    private suspend fun ProtectedAccess.transfer(kit: Kit, event: IfModalButton, deposit: Boolean) {
        if (!owned(kit)) return
        val source: Inventory = if (deposit) inv else player.invMap.getOrPut(kit.storage)
        val item = source[event.comsub] ?: return
        if (event.obj?.id != item.id) return
        if (event.op == IfButtonOp.Op10) { objExamine(source, event.comsub); return }
        val quantity = when (event.op) {
            IfButtonOp.Op1 -> when (player.vars[QUANTITY]) { 1 -> 5; 2 -> Int.MAX_VALUE; 3 -> countDialog(); else -> 1 }
            IfButtonOp.Op2 -> 1
            IfButtonOp.Op3 -> 5
            IfButtonOp.Op4 -> countDialog()
            IfButtonOp.Op5 -> Int.MAX_VALUE
            else -> return
        }
        ifCloseChat()
        if (quantity <= 0 || !owned(kit) || source[event.comsub] !== item) return
        val moved = if (deposit) store.store(player, kit.spec, item.id, quantity) else store.withdraw(player, kit.spec, item.id, quantity, inv)
        if (moved == 0) mes(if (deposit) "That item cannot be stored, or the kit is full." else "Your inventory cannot hold that item.")
        refresh(kit)
    }

    private fun ProtectedAccess.refresh(kit: Kit) {
        invTransmit(player.invMap.getOrPut(kit.storage))
        invTransmit(inv)
    }

    private data class Kit(val item: String, val main: String, val side: String, val storage: String, val initScript: Int, val sideScript: Int) {
        val spec get() = ContainerCatalog.byId[item.asRSCM()]!!
        fun comp(name: String) = "component.${main.substringAfter('.')}:$name"
        val sideComp get() = "component.${side.substringAfter('.')}:items"
    }
    private companion object {
        const val QUANTITY = "varbit.ii_elnock_storage_selectedquantity"
        val kits = listOf(
            Kit("obj.tackle_box", "interface.tackle_box_main", "interface.tackle_box_side", "inv.tackle_box", 4049, 4061),
            Kit("obj.huntsmans_kit", "interface.huntsmans_kit", "interface.huntsmans_kit_side", "inv.huntsmans_kit", 4988, 5000),
        )
    }
}
