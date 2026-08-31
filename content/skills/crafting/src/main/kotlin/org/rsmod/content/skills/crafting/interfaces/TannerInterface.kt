package org.rsmod.content.skills.crafting.interfaces

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.awt.Color
import org.rsmod.api.enums.NamedEnums.kourend_diary_tiers
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.table.crafting.CraftingTanningRow
import org.rsmod.content.skills.crafting.objValues
import org.rsmod.content.skills.crafting.toCraftingProduct
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TannerInterfaceScript : PluginScript() {
    override fun ScriptContext.startup() {
        // Registers every tanning exchange with CraftingRecipes so ::craftmat can resolve leather.
        CraftingTanningRow.all().forEach { it.toCraftingProduct() }
        for ((slot, row) in tannerSlotRows) {
            for (op in TannerOp.entries) {
                onIfModalButton(tannerSlotOp(slot.letter, op.suffix)) {
                    performTan(slot, row, op)
                }
            }
        }
    }
}

fun ProtectedAccess.openTanner(prices: TannerPrices = TannerPrices.Table) {
    if (tannerSlotRows.isEmpty()) {
        return
    }
    player.craftingTannerPrices = prices.ordinal
    ifOpenMainModal(INTERFACE_TANNER)
    for ((slot, _) in tannerSlotRows) {
        for (op in TannerOp.entries) {
            ifSetEvents(tannerSlotOp(slot.letter, op.suffix), -1..-1, IfEvent.Op1)
        }
    }
    renderTannerSlots()
}

enum class TannerPrices(
    private val soft: Int,
    private val hard: Int,
    private val snakeskin: Int,
    private val swampSnakeskin: Int,
    private val dragonhide: Int,
) {
    Table(0, 0, 0, 0, 0),
    Sbott(2, 5, 25, 45, 45),
    Eodan(10, 30, 200, 150, 200);

    internal fun priceOf(slot: TannerSlot, row: TanningRecipe): Int =
        when (slot.letter) {
            'a' -> soft
            'b' -> hard
            'c' -> swampSnakeskin
            'd' -> snakeskin
            else -> dragonhide
        }.takeIf { this != Table } ?: row.cost
}

private var Player.craftingTannerPrices by intVarBit("varbit.crafting_tanner_prices")

internal fun ProtectedAccess.tannerPriceOf(slot: TannerSlot, row: TanningRecipe): Int {
    val prices = TannerPrices.entries.getOrElse(player.craftingTannerPrices) { TannerPrices.Table }
    val base = prices.priceOf(slot, row)
    if (prices != TannerPrices.Eodan) {
        return base
    }
    val remaining = KOUREND_DIARY_TIERS - completedKourendDiaryTiers()
    return base * remaining / KOUREND_DIARY_TIERS
}

private fun ProtectedAccess.completedKourendDiaryTiers(): Int =
    kourend_diary_tiers.filterValuesNotNull().values.count { varbitId ->
        val varbit = ServerCacheManager.getVarbit(varbitId)
        varbit != null && player.vars[varbit] != 0
    }

private const val KOUREND_DIARY_TIERS = 5

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
    val tanned = if (requested > 0) tanHides(row, requested, tannerPriceOf(slot, row)) else 0
    if (tanned > 0) {
        ifClose()
    } else if (player.ui.containsModal(INTERFACE_TANNER)) {
        renderTannerSlots()
    }
}

private fun ProtectedAccess.renderTannerSlots() {
    for ((slot, row) in tannerSlotRows) {
        val name = tannerSlotName(slot.letter)
        val price = tannerSlotPrice(slot.letter)
        ifSetObj(tannerSlotModel(slot.letter), slot.input, TANNER_MODEL_ZOOM)
        ifSetText(name, slot.label)
        ifSetText(price, coins(tannerPriceOf(slot, row)))
        val colour = if (inv.count(slot.input) > 0) LABEL_AVAILABLE else LABEL_UNAVAILABLE
        player.setColour(name, colour)
        player.setColour(price, colour)
    }
}

internal data class TannerSlot(
    val letter: Char,
    val label: String,
    val input: String,
    val output: String,
)

internal enum class TannerOp(val suffix: String) {
    Tan1("1"),
    Tan5("5"),
    TanX("x"),
    TanAll("all"),
}

private const val INTERFACE_TANNER = "interface.tanner"

private const val TANNER_MODEL_ZOOM = 260

private val LABEL_AVAILABLE: Color = Color(0, 207, 255)

private val LABEL_UNAVAILABLE: Color = Color(254, 0, 0)

data class TanningRecipe(val input: String, val output: String, val cost: Int)

internal val tanningRecipes: List<TanningRecipe> by lazy {
    CraftingTanningRow.all().map { it.tanningRecipe() }
}

private fun CraftingTanningRow.tanningRecipe(): TanningRecipe =
    TanningRecipe(
        input = objValues(input).first().internalName,
        output = objValues(output).first().internalName,
        cost = cost,
    )

internal val tannerSlotRows: List<Pair<TannerSlot, TanningRecipe>> by lazy {
    CraftingTanningRow.all()
        .mapNotNull { row ->
            val letter = row.slotLetter?.firstOrNull() ?: return@mapNotNull null
            val recipe = row.tanningRecipe()
            TannerSlot(letter, row.slotLabel.orEmpty(), recipe.input, recipe.output) to recipe
        }
        .sortedBy { (slot, _) -> slot.letter }
}

internal val tannableHideObjs: Set<String> by lazy { tannerSlotRows.map { it.first.input }.toSet() }

internal val tannedLeatherObjs: Set<String> by lazy {
    tannerSlotRows.map { it.first.output }.toSet()
}

internal fun ProtectedAccess.heldTannableHides(): Int = tannableHideObjs.sumOf { inv.count(it) }

private fun coins(amount: Int): String = if (amount == 1) "1 coin" else "$amount coins"

internal fun itemName(internal: String): String =
    ServerCacheManager.getItem(internal.asRSCM(RSCMType.OBJ))?.name?.lowercase() ?: internal

private fun tannerSlotModel(letter: Char): String = "component.tanner:tanning_${letter}_model"

private fun tannerSlotName(letter: Char): String = "component.tanner:tanning_${letter}_text"

private fun tannerSlotPrice(letter: Char): String = "component.tanner:tanning_${letter}_price"

private fun tannerSlotOp(letter: Char, op: String): String =
    "component.tanner:tanning_${letter}_$op"
