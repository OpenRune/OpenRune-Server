package org.rsmod.game.inv

import org.rsmod.game.entity.Player

public fun interface InvVirtualStorage {
    public fun additionalCount(player: Player, inventory: Inventory, itemInternal: String): Int
}

public object InvVirtualStorageHolder {
    public var instance: InvVirtualStorage? = null
}
