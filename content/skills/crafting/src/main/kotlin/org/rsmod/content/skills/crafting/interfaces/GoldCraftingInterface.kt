package org.rsmod.content.skills.crafting.interfaces

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.table.crafting.CraftingGoldRow
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.beginCraft
import org.rsmod.content.skills.crafting.hasCraftingMaterials
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.meetsUnlocks
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class GoldCraftingInterfaceScript : PluginScript() {
    override fun ScriptContext.startup() {
        check(goldSlots.isNotEmpty()) { "No gold crafting recipes resolved" }

        for (slot in goldSlots) {
            onIfModalButton(slot.component) { craftGoldSlot(slot) }
        }
        for (quantity in GOLD_COLUMN.buttons) {
            onIfModalButton(GOLD_COLUMN.component(quantity.button)) {
                selectMakeQuantity(GOLD_COLUMN, quantity)
            }
        }
        onIfModalButton(GOLD_COLUMN.someButton) {}
    }
}

private var Player.goldLastType by intVarBit(VARBIT_GOLD_LASTTYPE)

fun ProtectedAccess.openGoldCrafting() {
    resetMakeQuantity()
    ifOpenMainModal(INTERFACE_GOLD_CRAFTING)
    for (slot in goldSlots) {
        ifSetEvents(slot.component, -1..-1, IfEvent.Op1)
        ifSetHide(slot.component, hide = !slotUnlocked(slot))
    }
}

private fun ProtectedAccess.slotUnlocked(slot: GoldSlot): Boolean =
    slot.products.isEmpty() || slot.products.any { player.meetsUnlocks(it) }

fun ProtectedAccess.hasGoldCraftingBars(): Boolean = inv.contains(CraftingConstants.GOLD_BAR)

private fun ProtectedAccess.craftableProduct(slot: GoldSlot): CraftingProduct? =
    slot.products.firstOrNull { hasCraftingMaterials(it) }

private suspend fun ProtectedAccess.craftGoldSlot(slot: GoldSlot) {
    if (!slotUnlocked(slot)) {
        return
    }
    val product = craftableProduct(slot) ?: return
    player.goldLastType = slot.lastType
    ifClose()
    beginCraft(product, makeQuantity())
}

private val GOLD_COLUMN =
    MakeQuantityColumn(
        component = ::goldComponent,
        countedObj = CraftingConstants.GOLD_BAR,
        stepX = 0,
        stepY = 40,
    )

internal data class GoldSlot(
    val component: String,
    val lastType: Int,
    val products: List<CraftingProduct>,
)

internal val goldSlots: List<GoldSlot> by lazy {
    CraftingGoldRow.all()
        .groupBy { it.interfaceComponent }
        .map { (component, rows) ->
            GoldSlot(component, rows.first().interfaceSlot, rows.map { it.toCraftingProduct() })
        }
        .sortedBy { it.lastType }
}

private const val INTERFACE_GOLD_CRAFTING = "interface.crafting_gold"

private const val VARBIT_GOLD_LASTTYPE = "varbit.crafting_gold_item_lasttype"

private fun goldComponent(name: String): String = "component.crafting_gold:$name"
