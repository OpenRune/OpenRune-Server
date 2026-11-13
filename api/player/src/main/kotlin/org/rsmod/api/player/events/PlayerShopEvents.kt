package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.shop.Shop

/**
 * [BuyPrepare] and [SellPrepare] run before a transaction and whenever the player checks a value,
 * so handlers must only set fields. [Bought] and [Sold] follow once per completed transaction, after
 * items and currency have moved; a click that fails publishes neither. `shop.inv.type` identifies
 * the shop and amounts are in `shop.currency`.
 */
public class PlayerShopEvents {
    /**
     * [item] is what the player receives; swap it for the noted form to deliver notes. [baseCost]
     * goes through the normal stock pricing, which never charges below 1, so [free] is separate.
     * A non-null [refusal] stops the purchase and is shown to the player.
     */
    public class BuyPrepare(
        override val player: Player,
        public val shop: Shop,
        public val stock: ItemServerType,
        public var item: ItemServerType,
        public var baseCost: Int,
        public var free: Boolean = false,
        public var depletesStock: Boolean = true,
        public var refusal: String? = null,
    ) : UnboundEvent, PlayerEvent

    /** [item] is unnoted. A non-null [refusal] stops the sale and is shown to the player. */
    public class SellPrepare(
        override val player: Player,
        public val shop: Shop,
        public val item: ItemServerType,
        public var baseCost: Int,
        public var refusal: String? = null,
    ) : UnboundEvent, PlayerEvent

    /** [item] is the shop's stock form, even if [BuyPrepare] delivered notes. */
    public data class Bought(
        override val player: Player,
        public val shop: Shop,
        public val item: ItemServerType,
        public val count: Int,
        public val cost: Int,
    ) : UnboundEvent, PlayerEvent

    /** [item] is unnoted, as the shop receives it. */
    public data class Sold(
        override val player: Player,
        public val shop: Shop,
        public val item: ItemServerType,
        public val count: Int,
        public val payment: Int,
    ) : UnboundEvent, PlayerEvent
}
