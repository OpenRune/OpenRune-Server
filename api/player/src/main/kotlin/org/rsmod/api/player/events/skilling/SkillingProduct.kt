package org.rsmod.api.player.events.skilling

import org.rsmod.game.entity.Player

public class SkillingProduct(
    public val player: Player,
    public val skill: String,
    public var item: String,
    public var count: Int = 1,
    public var experience: Double = 0.0,
    public var grantsExperience: Boolean = true,
    public val source: SkillingProductSource,
    public val isBonus: Boolean = false,
    public var cancelled: Boolean = false,
    public var depletes: Boolean = true,
    /**
     * Set by a prepare listener to send the product somewhere other than the inventory (such as
     * the bank); returns `false` when it couldn't, and the inventory is used instead.
     */
    public var deliver: ((SkillingProduct) -> Boolean)? = null,
)
