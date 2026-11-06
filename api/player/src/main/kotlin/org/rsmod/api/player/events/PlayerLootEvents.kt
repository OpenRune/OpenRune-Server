package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent
import org.rsmod.game.inv.Inventory

public sealed interface LootSource {
    public class Npc(public val npc: org.rsmod.game.entity.Npc) : LootSource

    public class Loc(public val loc: String) : LootSource

    public class Obj(public val obj: String) : LootSource
}

/** [destination] of null leaves the item on the ground. */
public class LootItem(
    public var item: String,
    public var count: Int,
    public var destination: Inventory?,
)

/**
 * [inventory] is null when the item went to the ground, to the pet system, or to a receive hook such
 * as a bonecrusher.
 */
public data class ReceivedLoot(
    public val item: String,
    public val count: Int,
    public val inventory: Inventory?,
)

/**
 * One loot handed to [PlayerLootEvents.Prepare.player]: a kill's drops, a chest opening, a cache.
 * Each event is published once per loot, with every item in it.
 */
public class PlayerLootEvents {
    /**
     * Runs before the collection log and every drop hook see the loot. Change, add or remove [items]
     * freely; an item with a count of 0 or less is skipped. Conditions and bonus drops are already
     * resolved. A destination that cannot hold a whole item leaves it on the ground instead.
     */
    public class Prepare(
        override val player: Player,
        public val source: LootSource,
        public val items: MutableList<LootItem>,
    ) : UnboundEvent, PlayerEvent

    /** The final items, wherever they went. Not published when nothing was given. */
    public class Received(
        override val player: Player,
        public val source: LootSource,
        public val items: List<ReceivedLoot>,
    ) : UnboundEvent, PlayerEvent
}
