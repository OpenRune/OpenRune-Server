package org.rsmod.content.skills.crafting.interfaces

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.content.skills.crafting.util.CraftingGamevals
import org.rsmod.game.entity.Player

/** The `skillmain` quantity column, shared by the gold and silver crafting interfaces. */
internal class MakeQuantityColumn(
    val component: (String) -> String,
    /** The bar `skillmain` counts to decide which buttons exist. */
    val countedObj: String,
    /** Offsets the buttons lay out with, since gold stacks them and silver puts them in a row. */
    val stepX: Int,
    val stepY: Int,
) {
    val buttons: List<MakeQuantity> by lazy {
        MakeQuantity.entries.filter { CraftingGamevals.exists(component(it.button)) }
    }

    val someButton: String by lazy { component("make_some") }
}

/** A fixed quantity button and the amount it selects, null meaning X which asks first. */
internal enum class MakeQuantity(val button: String, val amount: Int?) {
    One("make_1", 1),
    Five("make_5", 5),
    Ten("make_10", 10),
    X("make_x", null),
    All("make_all", MAX_QUANTITY),
}

private var Player.makeQuantity by intVarp(CraftingConstants.VARP_MAKEX_CRAFTING)

/** Reset before opening so the client's on load draw picks up the right button. */
internal fun ProtectedAccess.resetMakeQuantity() {
    player.makeQuantity = 1
}

internal fun ProtectedAccess.makeQuantity(): Int = player.makeQuantity.coerceAtLeast(1)

/** Mirrors the clicked button into the varp, prompting first for the X button. */
internal suspend fun ProtectedAccess.selectMakeQuantity(
    column: MakeQuantityColumn,
    quantity: MakeQuantity,
) {
    val amount = quantity.amount
    if (amount == null) {
        promptMakeQuantity(column)
    } else {
        player.makeQuantity = amount
    }
}

/** Asks for an amount and lets `skillmain_setup` work out which button that lands on. */
private suspend fun ProtectedAccess.promptMakeQuantity(column: MakeQuantityColumn) {
    player.makeQuantity = 0
    val max = minOf(inv.count(column.countedObj), MAX_QUANTITY)
    val input = countDialog("Enter amount: (1-$max)")
    player.makeQuantity = input.coerceAtLeast(1)
    refreshMakeQuantity(column)
}

/** Rebuilds the quantity column by re-running the script the interface itself calls on load. */
private fun ProtectedAccess.refreshMakeQuantity(column: MakeQuantityColumn) {
    runClientScript(
        SKILLMAIN_INIT,
        column.component("makex").asRSCM(RSCMType.COMPONENT),
        column.stepX,
        column.stepY,
        column.countedObj.asRSCM(RSCMType.OBJ),
        *MakeQuantity.entries.map { column.component(it.button).asRSCM(RSCMType.COMPONENT) }.toTypedArray(),
        column.someButton.asRSCM(RSCMType.COMPONENT),
    )
}

/** `clientscript,skillmain_init`, the make quantity column shared across skill interfaces. */
private const val SKILLMAIN_INIT = 2926

/** The most `skillmain_init` will select, being a full inventory. */
internal const val MAX_QUANTITY = 28
