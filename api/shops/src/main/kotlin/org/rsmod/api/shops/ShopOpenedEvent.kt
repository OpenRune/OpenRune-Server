package org.rsmod.api.shops

import dev.openrune.types.InventoryServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public data class ShopOpenedEvent(
    public val player: Player,
    public val inventory: InventoryServerType,
) : UnboundEvent
