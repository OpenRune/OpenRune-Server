package org.rsmod.game.inv

import org.rsmod.game.entity.Player

public interface InvVirtualStorage {
    public fun additionalCount(player: Player, inventory: Inventory, itemInternal: String): Int

    /** True when virtual storage provides any item belonging to [contentGroup]. */
    public fun providesContentGroup(player: Player, inventory: Inventory, contentGroup: Int): Boolean =
        false
}

public object InvVirtualStorageHolder {
    public var instance: InvVirtualStorage? = null
}
