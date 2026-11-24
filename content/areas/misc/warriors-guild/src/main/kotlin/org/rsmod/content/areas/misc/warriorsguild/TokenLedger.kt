package org.rsmod.content.areas.misc.warriorsguild

import org.rsmod.api.invtx.invAdd
import org.rsmod.game.entity.Player

internal object TokenLedger {
    const val TOKEN = "obj.warguild_tokens"

    fun credit(player: Player, amount: Int) {
        player.tokensOwed = saturatingAdd(player.tokensOwed, amount)
    }

    fun creditKegs(player: Player, amount: Int) {
        player.kegTokensOwed = saturatingAdd(player.kegTokensOwed, amount)
    }

    fun transferKegs(player: Player): Int {
        val kegs = player.kegTokensOwed
        if (kegs <= 0) return 0
        player.kegTokensOwed = 0
        credit(player, kegs)
        return kegs
    }

    fun claim(player: Player): Claim {
        val owed = player.tokensOwed
        if (owed <= 0) return Claim.NothingOwed
        val tx = player.invAdd(player.inv, TOKEN, owed)
        if (tx.failure) return Claim.NoSpace
        player.tokensOwed = 0
        return Claim.Paid(owed)
    }

    private fun saturatingAdd(current: Int, amount: Int): Int =
        (current.toLong() + amount).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()

    sealed class Claim {
        data object NothingOwed : Claim()

        data object NoSpace : Claim()

        data class Paid(val amount: Int) : Claim()
    }
}
