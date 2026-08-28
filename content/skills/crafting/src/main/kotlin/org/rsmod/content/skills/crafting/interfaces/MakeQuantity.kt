package org.rsmod.content.skills.crafting.interfaces

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.skills.crafting.util.CraftingConstants
import org.rsmod.game.entity.Player

internal class MakeQuantityColumn(
    val component: (String) -> String,
    val countedObj: String,
    val stepX: Int,
    val stepY: Int,
) {
    val buttons: List<MakeQuantity> = MakeQuantity.entries

    val someButton: String by lazy { component("make_some") }
}

internal enum class MakeQuantity(val button: String, val amount: Int?) {
    One("make_1", 1),
    Five("make_5", 5),
    Ten("make_10", 10),
    X("make_x", null),
    All("make_all", MAX_QUANTITY),
}

private var Player.makeQuantity by intVarp(CraftingConstants.VARP_MAKEX_CRAFTING)

internal fun ProtectedAccess.resetMakeQuantity() {
    player.makeQuantity = 1
}

internal fun ProtectedAccess.makeQuantity(): Int = player.makeQuantity.coerceAtLeast(1)

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

private suspend fun ProtectedAccess.promptMakeQuantity(column: MakeQuantityColumn) {
    player.makeQuantity = 0
    val max = minOf(inv.count(column.countedObj), MAX_QUANTITY)
    val input = countDialog("Enter amount: (1-$max)")
    player.makeQuantity = input.coerceAtLeast(1)
    refreshMakeQuantity(column)
}

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

private const val SKILLMAIN_INIT = 2926

internal const val MAX_QUANTITY = 28
