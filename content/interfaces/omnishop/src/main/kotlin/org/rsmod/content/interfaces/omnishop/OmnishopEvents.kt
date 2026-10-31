package org.rsmod.content.interfaces.omnishop

import dev.openrune.types.ItemServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class OmnishopPurchaseCheck(
    public val player: Player,
    public val shop: Int,
    public val stock: Int,
    public val obj: ItemServerType,
) : UnboundEvent {
    public var failure: String? = null
    public var maximum: Int = Int.MAX_VALUE
    public var output: ItemServerType? = obj
    public var outputVars: Int = 0
}

public data class OmnishopPurchaseCompleted(
    public val player: Player,
    public val shop: Int,
    public val stock: Int,
    public val obj: ItemServerType,
    public val quantity: Int,
) : UnboundEvent
