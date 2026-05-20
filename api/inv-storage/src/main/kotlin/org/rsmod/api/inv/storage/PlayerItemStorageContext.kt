package org.rsmod.api.inv.storage

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public data class PlayerItemStorageContext(
    public val player: Player,
    public val inventory: Inventory,
    public val itemInternal: String,
)
