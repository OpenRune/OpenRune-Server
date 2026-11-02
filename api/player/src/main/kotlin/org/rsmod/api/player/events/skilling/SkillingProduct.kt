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

    public var inputs: List<SkillingInput> = emptyList(),

    public var byproducts: List<SkillingByproduct> = emptyList(),

)

public data class SkillingInput(public val item: String, public val count: Int = 1)

public data class SkillingByproduct(public val item: String, public val count: Int = 1)
