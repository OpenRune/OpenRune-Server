package org.rsmod.content.areas.wilderness

import kotlin.math.min
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess

public suspend fun ProtectedAccess.tryPayWildernessBossFee(
    fee: Int,
    onPaid: suspend ProtectedAccess.(source: String) -> Unit,
) {
    val feeText = feeText(fee)
    if (coinsAvailable() < fee) {
        mes("You need $feeText in your inventory or bank to pay the entry fee.")
        return
    }
    if (vars[NO_WARNING_VARP] == 0) {
        objbox(FEE_OBJ, FEE_OBJ_ZOOM, feeWarning(feeText))
        val choice =
            choice3(
                "Yes.",
                FeeChoice.Pay,
                "Yes, and don't ask again.",
                FeeChoice.PayAndSilence,
                "No.",
                FeeChoice.Decline,
                title = "Pay $feeText to enter?",
            )
        if (choice == FeeChoice.Decline) return
        if (choice == FeeChoice.PayAndSilence) vars[NO_WARNING_VARP] = 1
    }
    val source = payFee(fee) ?: return
    onPaid(source)
}

public suspend fun ProtectedAccess.checkWildernessBossFee(fee: Int) {
    objbox(FEE_OBJ, FEE_OBJ_ZOOM, feeWarning(feeText(fee)))
    if (vars[NO_WARNING_VARP] == 0) return
    val warnAgain =
        choice2(
            "Warn me about the fee again.",
            true,
            "Keep entering without a warning.",
            false,
        )
    if (warnAgain) vars[NO_WARNING_VARP] = 0
}

private fun feeText(fee: Int): String = "%,d coins".format(fee)

private fun feeWarning(feeText: String): String =
    "You need to pay a $feeText fee to enter.<br>" +
        "This can be taken from your inventory, bank, or both."

private fun ProtectedAccess.coinsAvailable(): Int =
    invTotal(inv, COINS) + invTotal(inv(BANK_INV), COINS)

private fun ProtectedAccess.payFee(fee: Int): String? {
    val bank = inv(BANK_INV)
    val fromInv = min(invTotal(inv, COINS), fee)
    val fromBank = fee - fromInv
    if (invTotal(bank, COINS) < fromBank) return null
    if (fromInv > 0) invDel(inv, COINS, fromInv)
    if (fromBank > 0) invDel(bank, COINS, fromBank)
    return when {
        fromBank == 0 -> "your inventory"
        fromInv == 0 -> "your bank"
        else -> "your inventory and bank"
    }
}

private enum class FeeChoice {
    Pay,
    PayAndSilence,
    Decline,
}

private const val COINS = "obj.coins"
private const val BANK_INV = "inv.bank"
private const val NO_WARNING_VARP = "varp.wilderness_boss_fee_nowarn"
private const val FEE_OBJ = "obj.coins_10000"
private const val FEE_OBJ_ZOOM = 400
