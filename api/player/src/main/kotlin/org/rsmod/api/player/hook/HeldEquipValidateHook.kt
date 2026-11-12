package org.rsmod.api.player.hook

import dev.openrune.types.ItemServerType
import org.rsmod.game.entity.Player

public fun interface HeldEquipValidateHook {
    /** @return a refusal message, or `null` to allow the equip. */
    public fun validate(player: Player, type: ItemServerType): String?
}
