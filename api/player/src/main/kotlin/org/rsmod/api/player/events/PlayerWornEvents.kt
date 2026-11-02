package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import dev.openrune.util.Wearpos
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Any item being put on or taken off, unlike the content-group keyed `HeldEquipEvents`. Published
 * after the item has moved. Items displaced by an equip (a shield by a two-handed weapon) each get
 * an [Unequip].
 */
public class PlayerWornEvents {
    public data class Equip(
        override val player: Player,
        public val item: ItemServerType,
        public val wearpos: Wearpos,
    ) : UnboundEvent, PlayerEvent

    public data class Unequip(
        override val player: Player,
        public val item: ItemServerType,
        public val wearpos: Wearpos,
    ) : UnboundEvent, PlayerEvent
}
