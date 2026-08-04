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
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.content.skills.crafting.util.meetsUnlocks
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The gold crafting interface, opened by a gold bar on a furnace or the furnace's smelt op. */
class GoldCraftingInterfaceScript : PluginScript() {
    override fun ScriptContext.startup() {
        check(goldProducts.isNotEmpty()) { "No gold crafting recipes resolved" }

        for (slot in goldSlots) {
            onIfModalButton(slot.component) { craftGoldSlot(slot) }
        }
        for (quantity in GOLD_COLUMN.buttons) {
            onIfModalButton(GOLD_COLUMN.component(quantity.button)) {
                selectMakeQuantity(GOLD_COLUMN, quantity)
            }
        }
        if (CraftingGamevals.exists(GOLD_COLUMN.someButton)) {
            onIfModalButton(GOLD_COLUMN.someButton) {}
        }
    }
}

/** Slot the highlight box is drawn behind. See [GoldSlot.lastType]. */
private var Player.goldLastType by intVarBit(VARBIT_GOLD_LASTTYPE)

fun ProtectedAccess.openGoldCrafting() {
    resetMakeQuantity()
    ifOpenMainModal(INTERFACE_GOLD_CRAFTING)
    for (slot in goldSlots) {
        ifSetEvents(slot.component, -1..-1, IfEvent.Op1)
        ifSetHide(slot.component, hide = !slotUnlocked(slot))
    }
}

/** Whether any recipe this slot can make passes its gates. */
private fun ProtectedAccess.slotUnlocked(slot: GoldSlot): Boolean {
    val products = slot.outputs.mapNotNull { goldProducts[it] }
    return products.isEmpty() || products.any { player.meetsUnlocks(it) }
}

fun ProtectedAccess.hasGoldCraftingBars(): Boolean = inv.contains(CraftingConstants.GOLD_BAR)

/** The recipe a slot would make now, or null when the player can make none of them. */
private fun ProtectedAccess.craftableProduct(slot: GoldSlot): CraftingProduct? =
    slot.outputs.firstNotNullOfOrNull { output ->
        goldProducts[output]?.takeIf { hasCraftingMaterials(it) }
    }

/** [beginCraft] re-validates level, mould and materials, and caps the amount to the inventory. */
private suspend fun ProtectedAccess.craftGoldSlot(slot: GoldSlot) {
    if (!slotUnlocked(slot)) {
        return
    }
    val product = craftableProduct(slot) ?: return
    player.goldLastType = slot.lastType
    ifClose()
    beginCraft(product, makeQuantity())
}

/** Gold's quantity column is stacked down the left of the interface. */
private val GOLD_COLUMN =
    MakeQuantityColumn(
        component = ::goldComponent,
        countedObj = CraftingConstants.GOLD_BAR,
        stepX = 0,
        stepY = 40,
    )

/** A mould section, and the value its first slot is numbered from. */
internal enum class GoldSection(val lastTypeBase: Int) {
    Rings(1),
    Necklaces(10),
    Amulets(18),
    Bracelets(26),
}

internal data class GoldSlot(
    val section: GoldSection,
    val component: String,
    val outputs: List<String>,
    val lastType: Int,
)

/** Each slot's component and the objs its `dbtable.crafting_gold` row makes there. */
private val GOLD_SLOTS: List<GoldSlot> = withLastTypes(
    listOf(
        rawSlot(GoldSection.Rings, "gold_ring", "obj.gold_ring"),
        rawSlot(GoldSection.Rings, "sapphire_ring", "obj.sapphire_ring"),
        rawSlot(GoldSection.Rings, "emerald_ring", "obj.emerald_ring"),
        rawSlot(GoldSection.Rings, "ruby_ring", "obj.ruby_ring"),
        rawSlot(GoldSection.Rings, "diamond_ring", "obj.diamond_ring"),
        rawSlot(GoldSection.Rings, "dragon_ring", "obj.dragonstone_ring"),
        rawSlot(GoldSection.Rings, "onyx_ring", "obj.onyx_ring"),
        rawSlot(GoldSection.Rings, "zenyte_ring", "obj.zenyte_ring"),
        // Drawn eighth but numbered last, and the slayer and eternal rings share the one slot.
        rawSlot(GoldSection.Rings, "slayer_ring", "obj.slayer_ring_eternal", "obj.slayer_ring_8"),

        rawSlot(GoldSection.Necklaces, "gold_necklace", "obj.gold_necklace"),
        rawSlot(GoldSection.Necklaces, "sapphire_necklace", "obj.sapphire_necklace"),
        rawSlot(GoldSection.Necklaces, "emerald_necklace", "obj.emerald_necklace"),
        rawSlot(GoldSection.Necklaces, "ruby_necklace", "obj.ruby_necklace"),
        rawSlot(GoldSection.Necklaces, "diamond_necklace", "obj.diamond_necklace"),
        rawSlot(GoldSection.Necklaces, "dragon_necklace", "obj.dragonstone_necklace"),
        rawSlot(GoldSection.Necklaces, "onyx_necklace", "obj.onyx_necklace"),
        rawSlot(GoldSection.Necklaces, "zenyte_necklace", "obj.zenyte_necklace"),

        rawSlot(GoldSection.Amulets, "gold_amulet", "obj.unstrung_gold_amulet"),
        rawSlot(GoldSection.Amulets, "sapphire_amulet", "obj.unstrung_sapphire_amulet"),
        rawSlot(GoldSection.Amulets, "emerald_amulet", "obj.unstrung_emerald_amulet"),
        rawSlot(GoldSection.Amulets, "ruby_amulet", "obj.unstrung_ruby_amulet"),
        rawSlot(GoldSection.Amulets, "diamond_amulet", "obj.unstrung_diamond_amulet"),
        rawSlot(GoldSection.Amulets, "dragon_amulet", "obj.unstrung_dragonstone_amulet"),
        rawSlot(GoldSection.Amulets, "onyx_amulet", "obj.unstrung_onyx_amulet"),
        rawSlot(GoldSection.Amulets, "zenyte_amulet", "obj.unstrung_zenyte_amulet"),

        rawSlot(GoldSection.Bracelets, "gold_bracelet", "obj.jewl_gold_bracelet"),
        rawSlot(GoldSection.Bracelets, "sapphire_bracelet", "obj.jewl_sapphire_bracelet"),
        rawSlot(GoldSection.Bracelets, "emerald_bracelet", "obj.jewl_emerald_bracelet"),
        rawSlot(GoldSection.Bracelets, "ruby_bracelet", "obj.jewl_ruby_bracelet"),
        rawSlot(GoldSection.Bracelets, "diamond_bracelet", "obj.jewl_diamond_bracelet"),
        rawSlot(GoldSection.Bracelets, "dragon_bracelet", "obj.jewl_dragonstone_bracelet"),
        rawSlot(GoldSection.Bracelets, "onyx_bracelet", "obj.jewl_onyx_bracelet"),
        rawSlot(GoldSection.Bracelets, "zenyte_bracelet", "obj.zenyte_bracelet"),
    ),
)

/** A slot before its lastType is assigned. */
private fun rawSlot(section: GoldSection, component: String, vararg outputs: String): GoldSlot =
    GoldSlot(section, goldComponent(component), outputs.toList(), lastType = 0)

/** Numbers each slot within its section, which is what the highlight box reads. */
private fun withLastTypes(slots: List<GoldSlot>): List<GoldSlot> {
    val next = mutableMapOf<GoldSection, Int>()
    return slots.map { slot ->
        val index = next.getOrDefault(slot.section, 0)
        next[slot.section] = index + 1
        slot.copy(lastType = slot.section.lastTypeBase + index)
    }
}

/** The interface's slots, with unresolvable components dropped so startup cannot abort. */
internal val goldSlots: List<GoldSlot> by lazy {
    GOLD_SLOTS.filter { CraftingGamevals.exists(it.component) }
}

/** Every gold recipe keyed by the obj it makes, which is how a slot finds its recipe. */
internal val goldProducts: Map<String, CraftingProduct> by lazy {
    CraftingGoldRow.all().associate { it.output.internalName to it.toCraftingProduct() }
}

private const val INTERFACE_GOLD_CRAFTING = "interface.crafting_gold"

/** Last-selected-item varbit, which is what the highlight box reads. */
private const val VARBIT_GOLD_LASTTYPE = "varbit.crafting_gold_item_lasttype"

/**  A component of the gold crafting interface, by its name in the interface JSON. */
private fun goldComponent(name: String): String = "component.crafting_gold:$name"
