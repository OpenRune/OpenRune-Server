package org.rsmod.api.inv.storage

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

public data class PlayerItemStorageContext(
    public val player: Player,
    public val inventory: Inventory,
    public val itemInternal: String,
    public val source: ItemStorageSource = ItemStorageSource.Default,
    public val incomingCount: Int = 0,
)

/** Only explicit gameplay producers select a source; ordinary transfers keep their normal rules. */
public enum class ItemStorageSource {
    Default,
    GroundPickup,
    Runecrafting,
}
