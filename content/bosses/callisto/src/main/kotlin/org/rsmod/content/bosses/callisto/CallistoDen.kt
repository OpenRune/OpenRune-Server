package org.rsmod.content.bosses.callisto

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CallistoDen
@Inject
constructor(private val playerList: PlayerList, private val random: GameRandom) : PluginScript() {

    private data class Entrance(
        val loc: String,
        val arrival: CoordGrid,
        val exits: List<CoordGrid>,
        val den: BearDen,
    )

    private val entrances =
        listOf(
            Entrance(
                "loc.wild_callisto_singles_entrance01",
                CoordGrid(1758, 11552, 0),
                listOf(CoordGrid(3115, 3675, 0)),
                ARTIO_DEN,
            ),
            Entrance(
                "loc.wild_callisto_entrance01",
                CoordGrid(3360, 10336, 0),
                listOf(
                    CoordGrid(3360, 10295, 0),
                    CoordGrid(3341, 10255, 0),
                    CoordGrid(3376, 10255, 0),
                ),
                CALLISTO_DEN,
            ),
        )

    override fun ScriptContext.startup() {
        for (entrance in entrances) {
            onOpLoc1(entrance.loc) { enterDen(entrance) }
            onOpLoc2(entrance.loc) { peekDen(entrance) }
            onOpLoc3(entrance.loc) { checkFee() }
        }
        onOpLoc1(EXIT_LOC) { leaveDen() }
    }

    private suspend fun ProtectedAccess.enterDen(entrance: Entrance) {
        arriveDelay()
        if (coinsAvailable() < ENTRY_FEE) {
            mes("You need $FEE_TEXT in your inventory or bank to pay the entry fee.")
            return
        }
        if (vars[NO_WARNING_VARP] == 0) {
            objbox(FEE_OBJ, FEE_OBJ_ZOOM, FEE_WARNING)
            val choice =
                choice3(
                    "Yes.",
                    FeeChoice.Pay,
                    "Yes, and don't ask again.",
                    FeeChoice.PayAndSilence,
                    "No.",
                    FeeChoice.Decline,
                    title = "Pay $FEE_TEXT to enter?",
                )
            if (choice == FeeChoice.Decline) return
            if (choice == FeeChoice.PayAndSilence) vars[NO_WARNING_VARP] = 1
        }
        val source = payFee() ?: return
        mes("<col=ef1020>You enter the cave and $FEE_TEXT is taken from $source to pay the entry fee.")
        anim(CRAWL_SEQ)
        delay(1)
        telejump(entrance.arrival, TeleportType.Exempt)
        anim(LAND_SEQ)
    }

    private fun ProtectedAccess.coinsAvailable(): Int =
        invTotal(inv, COINS) + invTotal(inv(BANK_INV), COINS)

    private fun ProtectedAccess.payFee(): String? {
        val bank = inv(BANK_INV)
        val fromInv = min(invTotal(inv, COINS), ENTRY_FEE)
        val fromBank = ENTRY_FEE - fromInv
        if (invTotal(bank, COINS) < fromBank) return null
        if (fromInv > 0) invDel(inv, COINS, fromInv)
        if (fromBank > 0) invDel(bank, COINS, fromBank)
        return when {
            fromBank == 0 -> "your inventory"
            fromInv == 0 -> "your bank"
            else -> "your inventory and bank"
        }
    }

    private fun ProtectedAccess.peekDen(entrance: Entrance) {
        val occupied = playerList.any { entrance.den.contains(it.coords) }
        if (occupied) {
            mes(
                "You peek into the darkness and can make out some movement. " +
                    "There is activity inside."
            )
        } else {
            mes(
                "You peek into the darkness and can make out no movement. " +
                    "There is no activity inside."
            )
        }
    }

    private suspend fun ProtectedAccess.checkFee() {
        objbox(FEE_OBJ, FEE_OBJ_ZOOM, FEE_WARNING)
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

    private suspend fun ProtectedAccess.leaveDen() {
        arriveDelay()
        val entrance = entrances.firstOrNull { it.den.contains(player.coords) } ?: return
        telejump(entrance.exits[random.of(entrance.exits.size)], TeleportType.Exempt)
    }

    private enum class FeeChoice {
        Pay,
        PayAndSilence,
        Decline,
    }

    private companion object {
        private const val EXIT_LOC = "loc.wild_callisto_exit01"
        private const val COINS = "obj.coins"
        private const val BANK_INV = "inv.bank"
        private const val NO_WARNING_VARP = "varp.wilderness_boss_fee_nowarn"
        private const val ENTRY_FEE = 50_000
        private const val FEE_TEXT = "50,000 coins"
        private const val FEE_OBJ = "obj.coins_10000"
        private const val FEE_OBJ_ZOOM = 400
        private const val FEE_WARNING =
            "You need to pay a 50,000 coin fee to enter.<br>" +
                "This can be taken from your inventory, bank, or both."
        private const val CRAWL_SEQ = "seq.godwars_human_crawling"
        private const val LAND_SEQ = "seq.human_falling_end"
    }
}
