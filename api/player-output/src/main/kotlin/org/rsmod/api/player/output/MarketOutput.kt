package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.misc.player.UpdateStockMarketSlotV2
import net.rsprot.protocol.game.outgoing.misc.player.UpdateTradingPostV2
import org.rsmod.game.entity.Player

/**
 * Sets or clears a Grand Exchange offer slot on the client.
 * The V2 offer carries a 64-bit unit price and completed gold amount.
 */
public fun Player.updateStockMarketSlot(
    slot: Int,
    update: UpdateStockMarketSlotV2.StockMarketUpdateType,
) {
    require(slot in 0..255) { "Invalid stock market slot: $slot" }
    client.write(UpdateStockMarketSlotV2(slot, update))
}

/**
 * Replaces or clears the client's Trading Post offer list.
 * V2 offer prices are 64-bit values.
 */
public fun Player.updateTradingPost(update: UpdateTradingPostV2.TradingPostUpdateType) {
    client.write(UpdateTradingPostV2(update))
}
