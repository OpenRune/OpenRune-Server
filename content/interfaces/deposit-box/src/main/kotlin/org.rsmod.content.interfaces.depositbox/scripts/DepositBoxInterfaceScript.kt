package org.rsmod.content.interfaces.depositbox.scripts

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenSub
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.worn.WornUnequipResult
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOpen
import org.rsmod.content.interfaces.bank.QuantityMode
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.depositbox.bankDepositInv
import org.rsmod.content.interfaces.depositbox.bankDepositWorn
import org.rsmod.content.interfaces.depositbox.configs.DepositBoxConstants
import org.rsmod.content.interfaces.depositbox.depositInventoryItem
import org.rsmod.content.interfaces.depositbox.depositOption1Qty
import org.rsmod.content.interfaces.depositbox.depositQuantityInput
import org.rsmod.content.interfaces.depositbox.depositQuantityMode
import org.rsmod.content.interfaces.depositbox.hideDepositWornButton
import org.rsmod.content.interfaces.depositbox.playDepositAnim
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DepositBoxInterfaceScript
@Inject
constructor(private val eventBus: EventBus, private val bankInv: BankInvScript) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen(DepositBoxConstants.INTERFACE_MAIN) { player.onDepositBoxOpen() }
        onIfClose(DepositBoxConstants.INTERFACE_MAIN) { player.onDepositBoxClose() }

        onIfModalButton(DepositBoxConstants.COMP_ITEMS) { itemOp(it.comsub, it.op) }

        onIfModalButton(DepositBoxConstants.COMP_QUANTITY_1) { depositQuantityMode = QuantityMode.One }
        onIfModalButton(DepositBoxConstants.COMP_QUANTITY_5) { depositQuantityMode = QuantityMode.Five }
        onIfModalButton(DepositBoxConstants.COMP_QUANTITY_10) { depositQuantityMode = QuantityMode.Ten }
        onIfModalButton(DepositBoxConstants.COMP_QUANTITY_ALL) { depositQuantityMode = QuantityMode.All }
        /* An X input that exactly matches a preset button (1/5/10) selects that button instead */
        onIfModalButton(DepositBoxConstants.COMP_QUANTITY_X) {
            val input = countDialog()
            if (input <= 0) {
                return@onIfModalButton
            }
            val preset = presetQuantityMode(input)
            if (preset != null) {
                depositQuantityMode = preset
            } else {
                depositQuantityInput = input
                depositQuantityMode = QuantityMode.X
            }
        }

        onIfModalButton(DepositBoxConstants.COMP_DEPOSIT_INV) {
            if (bankDepositInv(bankInv)) {
                playDepositAnim()
            }
        }
        onIfModalButton(DepositBoxConstants.COMP_DEPOSIT_WORN) {
            if (bankDepositWorn(bankInv)) {
                playDepositAnim()
            }
        }

        onIfModalButton(DepositBoxConstants.COMP_DEPOSITWORN_TOGGLE) {
            player.hideDepositWornButton = !player.hideDepositWornButton
        }

        for (wearpos in DepositBoxConstants.WORN_WEAPOS_SLOTS) {
            onIfModalButton(DepositBoxConstants.wornComponent(wearpos)) { wornOp(wearpos, it.op) }
        }
    }

    /** Preset quantity button matching an exact amount, or null if only "X" can represent it. */
    private fun presetQuantityMode(amount: Int): QuantityMode? =
        when (amount) {
            1 -> QuantityMode.One
            5 -> QuantityMode.Five
            10 -> QuantityMode.Ten
            else -> null
        }

    private fun Player.onDepositBoxOpen() {
        setItemEvents()
        disableMainInventory()
    }

    private fun Player.onDepositBoxClose() {
        enableMainInventory()
    }

    /** Item options are drawn dynamically by the cache (`bank_depositbox_drawslot`). op1 becomes the selected quantity. */
    private fun Player.setItemEvents() {
        ifSetEvents(
            DepositBoxConstants.COMP_ITEMS,
            inv.indices,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op10,
            IfEvent.Depth1,
        )
    }

    /** op1 = selected quantity (mirrors the left-click), op2-4 = 1/5/10, op5 = X prompt, op6 = All, op10 = Examine. */
    private suspend fun ProtectedAccess.itemOp(slot: Int, op: IfButtonOp) {
        if (inv[slot] == null) {
            return
        }
        when (op) {
            IfButtonOp.Op1 -> depositInventoryItem(bankInv, slot, depositOption1Qty())
            IfButtonOp.Op2 -> depositInventoryItem(bankInv, slot, 1)
            IfButtonOp.Op3 -> depositInventoryItem(bankInv, slot, 5)
            IfButtonOp.Op4 -> depositInventoryItem(bankInv, slot, 10)
            IfButtonOp.Op5 -> { //x quantity
                val input = countDialog()
                if (input > 0) {
                    depositInventoryItem(bankInv, slot, input)
                }
            }
            IfButtonOp.Op6 -> depositInventoryItem(bankInv, slot, Int.MAX_VALUE)
            IfButtonOp.Op10 -> objExamine(inv, slot)
            else -> {}
        }
    }

    /** Clicking a worn item unequips it into the inventory rather than depositing it. */
    private fun ProtectedAccess.wornOp(wornSlot: Int, op: IfButtonOp) {
        if (op == IfButtonOp.Op10) {
            objExamine(worn, wornSlot)
            return
        }
        if (worn[wornSlot] == null) {
            return
        }
        val result = wornUnequip(wornSlot)
        if (result is WornUnequipResult.Fail) {
            result.message?.let { mes(it) }
        }
    }

    /** The box has its own inventory grid, so the real inventory is swapped to the no-ops variant (to gray things out).
     * It's an overlay swap (not a modal), so the other side-panel tabs stay usable since that's the accurate behavior. */
    private fun Player.disableMainInventory() {
        ifCloseOverlay(DepositBoxConstants.INVENTORY_INTERFACE, eventBus)
        ifOpenSub(
            DepositBoxConstants.INVENTORY_DISABLED,
            DepositBoxConstants.INVENTORY_MAIN_TARGET,
            IfSubType.Overlay,
            eventBus,
        )
    }

    /** Swaps the normal inventory back in, which re-runs the standard inventory open (restoring its events and draw).*/
    private fun Player.enableMainInventory() {
        ifCloseOverlay(DepositBoxConstants.INVENTORY_DISABLED, eventBus)
        ifOpenSub(
            DepositBoxConstants.INVENTORY_INTERFACE,
            DepositBoxConstants.INVENTORY_MAIN_TARGET,
            IfSubType.Overlay,
            eventBus,
        )
    }
}
