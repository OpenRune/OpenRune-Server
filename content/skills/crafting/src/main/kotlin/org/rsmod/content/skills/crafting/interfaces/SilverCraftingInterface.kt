package org.rsmod.content.skills.crafting.interfaces

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.table.crafting.CraftingSilverRow
import org.rsmod.content.skills.crafting.CraftingProduct
import org.rsmod.content.skills.crafting.beginCraft
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SilverCraftingInterfaceScript : PluginScript() {
    override fun ScriptContext.startup() {
        check(silverSlots.isNotEmpty()) { "No silver crafting recipes resolved" }

        for (slot in silverSlots) {
            onIfModalButton(slot.component) { craftSilverSlot(slot) }
        }
        for (quantity in SILVER_COLUMN.buttons) {
            onIfModalButton(SILVER_COLUMN.component(quantity.button)) {
                selectMakeQuantity(SILVER_COLUMN, quantity)
            }
        }
        onIfModalButton(SILVER_COLUMN.someButton) {}
    }
}

private var Player.silverLastType by intVarBit(VARBIT_SILVER_LASTTYPE)

fun ProtectedAccess.openSilverCrafting() {
    resetMakeQuantity()
    ifOpenMainModal(INTERFACE_SILVER_CRAFTING)
    for (slot in silverSlots) {
        ifSetEvents(slot.component, -1..-1, IfEvent.Op1)
    }
}

fun ProtectedAccess.hasSilverCraftingBars(): Boolean = inv.contains(CraftingConstants.SILVER_BAR)

private suspend fun ProtectedAccess.craftSilverSlot(slot: SilverSlot) {
    player.silverLastType = slot.lastType
    ifClose()
    beginCraft(slot.product, makeQuantity())
}

private val SILVER_COLUMN =
    MakeQuantityColumn(
        component = ::silverComponent,
        countedObj = CraftingConstants.SILVER_BAR,
        stepX = 50,
        stepY = 0,
    )

internal data class SilverSlot(
    val component: String,
    val lastType: Int,
    val product: CraftingProduct,
)

internal val silverSlots: List<SilverSlot> by lazy {
    CraftingSilverRow.all()
        .map { SilverSlot(it.interfaceComponent, it.interfaceSlot, it.toCraftingProduct()) }
        .sortedBy { it.lastType }
}

private const val INTERFACE_SILVER_CRAFTING = "interface.silver_crafting"

private const val VARBIT_SILVER_LASTTYPE = "varbit.crafting_silver_item_lasttype"

private fun silverComponent(name: String): String = "component.silver_crafting:$name"
