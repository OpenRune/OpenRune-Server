package org.rsmod.content.skills.farming

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.output.UpdateInventory
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfModalDrag
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.events.EventBus
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingSeedVaultScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.kebos_farming_guild_seed_vault") { open() }
        onIfOpen(FarmingSeedVault.MAIN) {
            if (FarmingSeedVault.blocked(player)) {
                player.ifClose(eventBus)
                return@onIfOpen
            }
            FarmingSeedVault.initialize(player)
            val vault = FarmingSeedVault.store(player)
            player.startInvTransmit(vault)
            for (component in ITEMS) {
                player.ifSetEvents(component, 0..vault.size,
                    IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5,
                    IfEvent.Op6, IfEvent.Op7, IfEvent.Op8, IfEvent.Op9, IfEvent.Op10,
                    IfEvent.Depth1, IfEvent.DragTarget)
            }
            player.ifSetEvents(DEPOSIT, player.inv.indices,
                IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5,
                IfEvent.Op6, IfEvent.Op7, IfEvent.Op9, IfEvent.Op10)
        }
        onIfClose(FarmingSeedVault.MAIN) { player.stopInvTransmit(FarmingSeedVault.store(player)) }
        for (component in ITEMS) {
            onIfModalButton(component) { withdraw(it) }
            for (target in ITEMS) {
                onIfModalDrag(component, target) {
                    val from = it.selectedSlot ?: return@onIfModalDrag
                    val into = it.targetSlot ?: return@onIfModalDrag
                    FarmingSeedVault.reorder(player, from, into)
                    refresh()
                }
            }
        }
        onIfModalButton(DEPOSIT) { deposit(it) }
        onIfModalButton("component.seed_vault:deposit_all") {
            if (!FarmingSeedVault.accessible(player)) return@onIfModalButton
            for (slot in inv.indices) {
                val obj = inv[slot] ?: continue
                FarmingSeedVault.deposit(player, slot, obj, obj.count)
            }
            refresh()
        }
        for ((component, amount) in listOf("x1" to 1, "x5" to 5, "x10" to 10, "xall" to Int.MAX_VALUE)) {
            onIfModalButton("component.seed_vault:$component") {
                if (FarmingSeedVault.accessible(player)) VarPlayerIntMapSetter.set(player, FarmingSeedVault.SELECTED, amount)
            }
        }
        onIfModalButton("component.seed_vault:xx") {
            if (!FarmingSeedVault.accessible(player)) return@onIfModalButton
            val amount = if (it.op == IfButtonOp.Op2 || vars[FarmingSeedVault.REQUESTED] <= 0) countDialog() else vars[FarmingSeedVault.REQUESTED]
            if (amount > 0 && FarmingSeedVault.accessible(player)) {
                VarPlayerIntMapSetter.set(player, FarmingSeedVault.REQUESTED, amount)
                VarPlayerIntMapSetter.set(player, FarmingSeedVault.SELECTED, amount)
            }
        }
        onIfModalButton("component.seed_vault:lock_menu") {
            if (!FarmingSeedVault.accessible(player)) return@onIfModalButton
            if (it.op == IfButtonOp.Op2) {
                VarPlayerIntMapSetter.set(player, FarmingSeedVault.IGNORE_LOCKS, 1 - vars[FarmingSeedVault.IGNORE_LOCKS])
            } else {
                ifOpenOverlay(LOCK_INTERFACE, "component.seed_vault:popup")
                runClientScript("clientscript.[clientscript,bank_side_locks_init]".asRSCM(), "component.seed_vault:popup".asRSCM())
                ifSetEvents("component.bank_side_locks:slots", inv.indices, IfEvent.Op1)
                ifSetEvents("component.bank_side_locks:ignorelocks", -1..-1, IfEvent.Op1)
                ifSetEvents("component.bank_side_locks:extraoptions", -1..-1, IfEvent.Op1)
                ifSetEvents("component.bank_side_locks:done", -1..-1, IfEvent.Op1)
            }
        }
        onIfOverlayButton("component.bank_side_locks:slots") {
            if (!FarmingSeedVault.accessible(player) || it.comsub !in inv.indices || vars[FarmingSeedVault.IGNORE_LOCKS] != 0) return@onIfOverlayButton
            VarPlayerIntMapSetter.set(player, FarmingSeedVault.LOCKS, vars[FarmingSeedVault.LOCKS] xor (1 shl it.comsub))
        }
        onIfOverlayButton("component.bank_side_locks:ignorelocks") {
            if (FarmingSeedVault.accessible(player)) VarPlayerIntMapSetter.set(player, FarmingSeedVault.IGNORE_LOCKS, 1 - vars[FarmingSeedVault.IGNORE_LOCKS])
        }
        onIfOverlayButton("component.bank_side_locks:extraoptions") {
            if (FarmingSeedVault.accessible(player)) VarPlayerIntMapSetter.set(player, FarmingSeedVault.SHOW_LOCKS, 1 - vars[FarmingSeedVault.SHOW_LOCKS])
        }
        onIfOverlayButton("component.bank_side_locks:done") { ifCloseSub(LOCK_INTERFACE) }
    }

    private fun ProtectedAccess.open() {
        if (FarmingSeedVault.blocked(player)) return
        // shortcut: accounts have no PIN state; require the shared PIN authority when that is added.
        FarmingSeedVault.initialize(player)
        player.startInvTransmit(FarmingSeedVault.store(player))
        ifOpenMainSidePair(FarmingSeedVault.MAIN, FarmingSeedVault.SIDE)
    }

    private suspend fun ProtectedAccess.deposit(event: IfModalButton) {
        if (!FarmingSeedVault.accessible(player) || event.comsub !in inv.indices) return
        if (event.op == IfButtonOp.Op9) {
            if (vars[FarmingSeedVault.SHOW_LOCKS] != 0 && vars[FarmingSeedVault.IGNORE_LOCKS] == 0) {
                VarPlayerIntMapSetter.set(player, FarmingSeedVault.LOCKS, vars[FarmingSeedVault.LOCKS] xor (1 shl event.comsub))
            }
            return
        }
        val expected = inv[event.comsub] ?: return
        if (event.op == IfButtonOp.Op10) return objExamine(inv, event.comsub)
        val requested = quantity(event.op) ?: return
        if (FarmingSeedVault.deposit(player, event.comsub, expected, requested) == 0) {
            mes("You can't deposit that item in the seed vault.")
        }
        refresh()
    }

    private suspend fun ProtectedAccess.withdraw(event: IfModalButton) {
        if (!FarmingSeedVault.accessible(player)) return
        val vault = FarmingSeedVault.store(player)
        if (event.comsub !in vault.indices) return
        val expected = vault[event.comsub] ?: return
        val placeholder = getInvObj(expected).isPlaceholder
        when {
            event.op == IfButtonOp.Op10 -> return objExamine(vault, event.comsub)
            event.op == IfButtonOp.Op8 -> {
                if (!FarmingSeedVault.favourite(player, event.comsub) && placeholder) FarmingSeedVault.removePlaceholders(player, event.comsub)
            }
            placeholder && event.op == IfButtonOp.Op9 -> FarmingSeedVault.removePlaceholders(player)
            placeholder -> FarmingSeedVault.removePlaceholders(player, event.comsub)
            else -> {
                val requested = if (event.op == IfButtonOp.Op9) Int.MAX_VALUE else quantity(event.op) ?: return
                if (FarmingSeedVault.withdraw(player, event.comsub, expected, requested, event.op == IfButtonOp.Op9) == 0) mes("You don't have enough inventory space.")
            }
        }
        refresh()
    }

    private suspend fun ProtectedAccess.quantity(op: IfButtonOp): Int? = when (op) {
        IfButtonOp.Op1 -> vars[FarmingSeedVault.SELECTED].coerceAtLeast(1)
        IfButtonOp.Op2 -> 1
        IfButtonOp.Op3 -> 5
        IfButtonOp.Op4 -> 10
        IfButtonOp.Op5 -> vars[FarmingSeedVault.REQUESTED].coerceAtLeast(1)
        IfButtonOp.Op6 -> countDialog().also { if (it > 0) VarPlayerIntMapSetter.set(player, FarmingSeedVault.REQUESTED, it) }
        IfButtonOp.Op7 -> Int.MAX_VALUE
        else -> null
    }

    private fun ProtectedAccess.refresh() {
        UpdateInventory.updateInvFull(player, FarmingSeedVault.store(player))
    }

    private companion object {
        const val DEPOSIT = "component.seed_vault_deposit:inv"
        const val LOCK_INTERFACE = "interface.bank_side_locks"
        val ITEMS = listOf("component.seed_vault:obj_list", "component.seed_vault:text_list")
    }
}
