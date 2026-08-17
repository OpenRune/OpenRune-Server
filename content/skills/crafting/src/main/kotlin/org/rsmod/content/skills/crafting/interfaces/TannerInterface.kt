package org.rsmod.content.skills.crafting.interfaces

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import java.awt.Color
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.table.crafting.CraftingHandRow
import org.rsmod.content.skills.crafting.CraftingSection
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The tanner interface predates cs2 and is packed in the legacy if1 format, so it has no onLoad
 *  hook to hang scripts on and every component has to be driven from here. */
class TannerInterfaceScript : PluginScript() {
    override fun ScriptContext.startup() {
        for ((slot, row) in tannerSlotRows) {
            for (op in TannerOp.entries) {
                onIfModalButton(tannerSlotOp(slot.letter, op.suffix)) {
                    performTan(slot, row, op)
                }
            }
        }
    }
}

/** Opens the tanner and paints the grid. */
fun ProtectedAccess.openTanner(prices: TannerPrices = TannerPrices.Table) {
    if (tannerSlotRows.isEmpty()) {
        return
    }
    player.attr[TANNER_PRICES] = prices
    ifOpenMainModal(INTERFACE_TANNER)
    for ((slot, _) in tannerSlotRows) {
        for (op in TannerOp.entries) {
            ifSetEvents(tannerSlotOp(slot.letter, op.suffix), -1..-1, IfEvent.Op1)
        }
    }
    renderTannerSlots(prices)
}

/** What one tanner charges per hide. */
class TannerPrices private constructor(private val bySlot: Map<Char, Int>) {
    /** What this tanner charges for one hide of [row]. */
    internal fun priceOf(slot: TannerSlot, row: TanningRecipe): Int =
        bySlot[slot.letter] ?: row.cost

    companion object {
        /** The tanning table's own prices, used by Ellis and the Crafting Guild tanner. */
        val Table: TannerPrices = TannerPrices(emptyMap())

        fun of(
            soft: Int,
            hard: Int,
            snakeskin: Int,
            swampSnakeskin: Int,
            dragonhide: Int,
        ): TannerPrices =
            TannerPrices(
                mapOf(
                    'a' to soft,
                    'b' to hard,
                    'c' to swampSnakeskin,
                    'd' to snakeskin,
                    'e' to dragonhide,
                    'f' to dragonhide,
                    'g' to dragonhide,
                    'h' to dragonhide,
                ),
            )
    }
}

/** Prices the open interface was opened with, falling back to the table when absent. */
private val TANNER_PRICES = AttributeKey<TannerPrices>()

/** Tans [row]'s hide, up to [requested], at [pricePerHide] coins each. */
fun ProtectedAccess.tanHides(
    row: TanningRecipe,
    requested: Int,
    pricePerHide: Int = row.cost,
): Int {
    val hide = row.input
    val held = inv.count(hide)
    if (held == 0) {
        mes("You don't have any ${itemName(hide)} to tan.")
        return 0
    }

    val coinsHeld = inv.count(CraftingConstants.COINS)
    val affordable = if (pricePerHide > 0) coinsHeld / pricePerHide else Int.MAX_VALUE
    if (affordable == 0) {
        mes("You haven't got enough coins to pay for ${itemName(row.output)}.")
        return 0
    }

    val amount = minOf(requested, held, affordable)
    if (amount <= 0) {
        return 0
    }

    val totalCost = pricePerHide * amount
    if (!invDel(inv, CraftingConstants.COINS, totalCost).success) {
        return 0
    }
    if (!invDel(inv, hide, amount).success) {
        invAdd(inv, CraftingConstants.COINS, totalCost)
        return 0
    }
    invAdd(inv, row.output, amount)

    val message =
        when {
            requested > held -> "You have run out of ${itemName(hide)}."
            amount < requested ->
                "You haven't got enough coins to pay for ${itemName(row.output)}."
            else -> "The tanner tans your ${itemName(hide)}."
        }
    mes(message)
    return amount
}

/** Handles one slot click. */
private suspend fun ProtectedAccess.performTan(
    slot: TannerSlot,
    row: TanningRecipe,
    op: TannerOp,
) {
    val requested =
        when (op) {
            TannerOp.Tan1 -> 1
            TannerOp.Tan5 -> 5
            TannerOp.TanAll -> Int.MAX_VALUE
            TannerOp.TanX -> countDialog().coerceAtLeast(0)
        }
    val prices = player.attr.getOrDefault(TANNER_PRICES, TannerPrices.Table)
    val tanned = if (requested > 0) tanHides(row, requested, prices.priceOf(slot, row)) else 0
    if (tanned > 0) {
        ifClose()
    } else if (player.ui.containsModal(INTERFACE_TANNER)) {
        renderTannerSlots(prices)
    }
}

/** Paints the eight slots, colouring each label by whether the hide is held. */
private fun ProtectedAccess.renderTannerSlots(prices: TannerPrices) {
    for ((slot, row) in tannerSlotRows) {
        val name = tannerSlotName(slot.letter)
        val price = tannerSlotPrice(slot.letter)
        ifSetObj(tannerSlotModel(slot.letter), slot.input, TANNER_MODEL_ZOOM)
        ifSetText(name, slot.label)
        ifSetText(price, coins(prices.priceOf(slot, row)))
        val colour = if (inv.count(slot.input) > 0) LABEL_AVAILABLE else LABEL_UNAVAILABLE
        player.setColour(name, colour)
        player.setColour(price, colour)
    }
}

/** One slot of the grid. */
internal data class TannerSlot(
    val letter: Char,
    val label: String,
    val input: String,
    val output: String,
)

/** Which op stack position was clicked. */
internal enum class TannerOp(val suffix: String) {
    Tan1("1"),
    Tan5("5"),
    TanX("x"),
    TanAll("all"),
}

/** The grid in display order, with the top row a to d and the bottom row e to h. */
private val TANNER_SLOTS =
    listOf(
        TannerSlot('a', "Soft leather", "obj.cow_hide", "obj.leather"),
        TannerSlot('b', "Hard leather", "obj.cow_hide", "obj.hard_leather"),
        TannerSlot('c', "Snakeskin", "obj.templetrek_swamp_snake_hide", "obj.village_snake_skin"),
        TannerSlot('d', "Snakeskin", "obj.village_snake_hide", "obj.village_snake_skin"),
        TannerSlot('e', "Green d'hide", "obj.dragonhide_green", "obj.dragon_leather"),
        TannerSlot('f', "Blue d'hide", "obj.dragonhide_blue", "obj.dragon_leather_blue"),
        TannerSlot('g', "Red d'hide", "obj.dragonhide_red", "obj.dragon_leather_red"),
        TannerSlot('h', "Black d'hide", "obj.dragonhide_black", "obj.dragon_leather_black"),
    )

private const val INTERFACE_TANNER = "interface.tanner"

/** Model zoom for the tanner interface's slot model components. Higher values = larger */
private const val TANNER_MODEL_ZOOM = 260

/** Label colour when the hide is held, matching the blue baked into the interface JSON. */
private val LABEL_AVAILABLE: Color = Color(0, 207, 255)

/** Label colour when the player has none of the required hide. */
private val LABEL_UNAVAILABLE: Color = Color(254, 0, 0)

/** One tanning exchange off a `crafting_hand` row, being a hide plus coins for one leather. */
data class TanningRecipe(val input: String, val output: String, val cost: Int)

/** Every tanning row. Thakkrad's yak curing reads this catalogue too. */
internal val tanningRecipes: List<TanningRecipe> by lazy {
    CraftingHandRow.all()
        .filter { it.section == CraftingSection.TANNING.id }
        .map { TanningRecipe(it.input.first().internalName, it.output.first().internalName, it.cost ?: 0) }
}

/** Slots paired with their tanning recipes, which the NPC scripts also iterate. */
internal val tannerSlotRows: List<Pair<TannerSlot, TanningRecipe>> by lazy {
    TANNER_SLOTS.mapNotNull { slot ->
        val row = tanningRecipes.firstOrNull { it.input == slot.input && it.output == slot.output }
        row?.let { slot to it }
    }
}

/** Tannable hide objs, used by the NPC used item on and greeting logic. */
internal val tannableHideObjs: Set<String> by lazy { TANNER_SLOTS.map { it.input }.toSet() }

/** The leather objs tanning produces. */
internal val tannedLeatherObjs: Set<String> by lazy { TANNER_SLOTS.map { it.output }.toSet() }

/** How many hides the player is carrying that a tanner will take. */
internal fun ProtectedAccess.heldTannableHides(): Int = tannableHideObjs.sumOf { inv.count(it) }

/** Coin count worded for chat. */
private fun coins(amount: Int): String = if (amount == 1) "1 coin" else "$amount coins"

/** Lowercased item name, falling back to the raw gameval. */
internal fun itemName(internal: String): String {
    val id = CraftingGamevals.objOrNull(internal) ?: return internal
    return ServerCacheManager.getItem(id)?.name?.lowercase() ?: internal
}

// The tanner interface's 4x2 hide grid uses one slot group per letter a to h. Each group has
// a model, a name text, a price text, and four op buttons for Tan 1, 5, X and All.
private fun tannerSlotModel(letter: Char): String = "component.tanner:tanning_${letter}_model"

private fun tannerSlotName(letter: Char): String = "component.tanner:tanning_${letter}_text"

private fun tannerSlotPrice(letter: Char): String = "component.tanner:tanning_${letter}_price"

/** [op] is one of "1", "5", "x", "all". */
private fun tannerSlotOp(letter: Char, op: String): String =
    "component.tanner:tanning_${letter}_$op"

