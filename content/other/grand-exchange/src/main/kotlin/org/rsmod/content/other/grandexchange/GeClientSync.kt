package org.rsmod.content.other.grandexchange

import dev.openrune.ServerCacheManager
import dev.openrune.types.util.UncheckedType
import net.rsprot.protocol.game.outgoing.misc.player.UpdateStockMarketSlotV2.SetStockMarketSlot
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.updateStockMarketSlot
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.other.grandexchange.market.GeAccount
import org.rsmod.content.other.grandexchange.market.GeHistoryEntry
import org.rsmod.content.other.grandexchange.market.GeOffer
import org.rsmod.content.other.grandexchange.market.GeOfferType
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

/** Mirrors offer state to the client: the stockmarket slot packet and the per-slot collect invs. */
@OptIn(UncheckedType::class)
object GeClientSync {
    val COLLECT_INVS: List<String> =
        listOf(
            "inv.tradingpost_sell_0",
            "inv.tradingpost_sell_1",
            "inv.tradingpost_sell_2",
            "inv.tradingpost_sell_3",
            "inv.tradingpost_sell_4",
            "inv.tradingpost_sell_5",
            "inv.ge_collect_6",
            "inv.ge_collect_7",
        )

    /*
     * rsprot's `ResetStockMarketSlot` encoder always writes slot 0, so resetting any slot would
     * clear the first one. An all-zero offer is the same reset on the wire, for the right slot.
     */
    private val EMPTY_SLOT = SetStockMarketSlot(0, 0, 0, 0, 0, 0)

    private const val STATUS_ACTIVE = 2
    private const val STATUS_FINISHED = 5
    private const val STATUS_SELL = 8

    fun collectInv(player: Player, slot: Int): Inventory =
        player.invMap.getOrPut(COLLECT_INVS[slot])

    fun syncAll(player: Player, account: GeAccount) {
        for (slot in 0 until GeAccount.SLOT_COUNT) {
            syncSlot(player, account, slot)
        }
    }

    fun syncSlot(player: Player, account: GeAccount, slot: Int) {
        val offer = account.slots[slot]
        val inv = collectInv(player, slot)
        if (offer == null) {
            player.updateStockMarketSlot(slot, EMPTY_SLOT)
            inv[0] = null
            inv[1] = null
            return
        }
        val status =
            (if (offer.active) STATUS_ACTIVE else STATUS_FINISHED) or
                (if (offer.type == GeOfferType.Sell) STATUS_SELL else 0)
        player.updateStockMarketSlot(
            slot,
            SetStockMarketSlot(
                status = status,
                obj = offer.obj,
                price = offer.price,
                count = offer.count,
                completedCount = offer.completed,
                completedGold = offer.gold,
            ),
        )
        inv[0] = if (offer.collectItems > 0) InvObj(offer.obj, offer.collectItems) else null
        inv[1] =
            if (offer.collectCoins > 0) {
                InvObj(GrandExchange.COINS, minOf(offer.collectCoins, Int.MAX_VALUE.toLong()).toInt())
            } else {
                null
            }
    }

    fun fillMessage(player: Player, offer: GeOffer) {
        if (offer.completed <= 0 || offer.aborted) {
            return
        }
        val name = ServerCacheManager.getItem(offer.obj)?.name ?: "item"
        val buying = offer.type == GeOfferType.Buy
        val text =
            if (offer.active) {
                val verb = if (buying) "Bought" else "Sold"
                "Grand Exchange: $verb ${offer.completed} / ${offer.count} x $name."
            } else {
                val verb = if (buying) "buying" else "selling"
                "Grand Exchange: Finished $verb ${offer.count} x $name."
            }
        player.mes(text)
    }

    fun lastOffer(player: Player, entry: GeHistoryEntry) {
        val price = minOf(entry.gold / entry.count.coerceAtLeast(1), Int.MAX_VALUE.toLong())
        VarPlayerIntMapSetter.set(player, "varp.ge_last_offer_item", entry.obj)
        VarPlayerIntMapSetter.set(player, "varp.ge_last_offer_quantity", entry.count)
        VarPlayerIntMapSetter.set(player, "varp.ge_last_offer_price", price.toInt())
        VarPlayerIntMapSetter.set(player, "varp.ge_last_offer_type", entry.type.ordinal)
    }
}
