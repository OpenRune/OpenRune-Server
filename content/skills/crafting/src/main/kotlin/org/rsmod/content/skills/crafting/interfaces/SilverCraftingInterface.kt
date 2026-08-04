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
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SilverCraftingInterfaceScript : PluginScript() {
    override fun ScriptContext.startup() {
        check(silverProducts.isNotEmpty()) { "No silver crafting recipes resolved" }

        for (slot in silverSlots) {
            onIfModalButton(slot.component) { craftSilverSlot(slot) }
        }
        for (quantity in SILVER_COLUMN.buttons) {
            onIfModalButton(SILVER_COLUMN.component(quantity.button)) {
                selectMakeQuantity(SILVER_COLUMN, quantity)
            }
        }
        if (CraftingGamevals.exists(SILVER_COLUMN.someButton)) {
            onIfModalButton(SILVER_COLUMN.someButton) {}
        }
    }
}

private var Player.silverLastType by intVarBit(VARBIT_SILVER_LASTTYPE)

/** Opens the silver interface. */
fun ProtectedAccess.openSilverCrafting() {
    resetMakeQuantity()
    ifOpenMainModal(INTERFACE_SILVER_CRAFTING)
    for (slot in silverSlots) {
        ifSetEvents(slot.component, -1..-1, IfEvent.Op1)
    }
}

/** Bars alone are enough to open, since the interface tells the player which moulds it needs. */
fun ProtectedAccess.hasSilverCraftingBars(): Boolean = inv.contains(CraftingConstants.SILVER_BAR)

/** [beginCraft] validates level, mould and materials, and caps the amount to the inventory. */
private suspend fun ProtectedAccess.craftSilverSlot(slot: SilverSlot) {
    val product = silverProducts[slot.output] ?: return
    player.silverLastType = slot.lastType
    ifClose()
    beginCraft(product, makeQuantity())
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
    val output: String,
    val lastType: Int,
)

private val SILVER_SLOTS: List<SilverSlot> =
    withSilverLastTypes(
        listOf(
            "opal_ring" to "obj.opal_ring",
            "jade_ring" to "obj.jade_ring",
            "topaz_ring" to "obj.topaz_ring",
            "opal_necklace" to "obj.opal_necklace",
            "jade_necklace" to "obj.jade_necklace",
            "topaz_necklace" to "obj.topaz_necklace",
            "opal_amulet" to "obj.unstrung_opal_amulet",
            "jade_amulet" to "obj.unstrung_jade_amulet",
            "topaz_amulet" to "obj.unstrung_topaz_amulet",
            "opal_bracelet" to "obj.opal_bracelet",
            "jade_bracelet" to "obj.jade_bracelet",
            "topaz_bracelet" to "obj.topaz_bracelet",
            "holy_symbol" to "obj.nostringstar",
            "unholy_symbol" to "obj.nostringsnake",
            "sickle" to "obj.silver_sickle",
            "lightning_rod" to "obj.fenk_conductor",
            "crossbow_bolt" to "obj.xbows_crossbow_bolts_silver_unfeathered",
            "tiara" to "obj.tiara",
            "ivandis" to "obj.burgh_rod_command1",
            "agrith_sigil" to "obj.agrith_sigil",
        )
    )

/** Numbers each slot, which is what the highlight box reads. */
private fun withSilverLastTypes(slots: List<Pair<String, String>>): List<SilverSlot> =
    slots.mapIndexed { index, (component, output) ->
        SilverSlot(silverComponent(component), output, lastType = index + 1)
    }

/** The interface's slots, with unresolvable components dropped. */
internal val silverSlots: List<SilverSlot> by lazy {
    SILVER_SLOTS.filter { CraftingGamevals.exists(it.component) }
}

/** Every silver recipe keyed by the obj it makes, which is how a slot finds its recipe. */
internal val silverProducts: Map<String, CraftingProduct> by lazy {
    CraftingSilverRow.all().associate { it.output.internalName to it.toCraftingProduct() }
}

private const val INTERFACE_SILVER_CRAFTING = "interface.silver_crafting"

/** Last-selected-item varbit, which is what the highlight box reads. */
private const val VARBIT_SILVER_LASTTYPE = "varbit.crafting_silver_item_lasttype"

/** A component of the silver crafting interface, by its name in the interface JSON. */
private fun silverComponent(name: String): String = "component.silver_crafting:$name"
