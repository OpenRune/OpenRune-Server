package org.rsmod.content.skills.smithing

import org.rsmod.content.skills.smithing.util.SmithingProductMeta
import org.rsmod.game.entity.Player

/** Allows content to deny specific anvil products, returning the message to show. */
public fun interface SmithingRestrictHook {
    public fun restrict(player: Player, product: SmithingProductMeta): String?
}
