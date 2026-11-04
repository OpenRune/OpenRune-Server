package org.rsmod.api.player.events

import dev.openrune.types.ItemServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Rune costs of a spell cast outside a recorded attack: teleports, alchemy and every other non-combat
 * spell. A spell inside an attack uses [PlayerAttackEvents.AttackPrepare] and
 * [PlayerAttackEvents.AttackResult] instead. A spell with nothing to pay, such as one covered by
 * elemental staves, publishes neither.
 */
public class PlayerSpellRuneEvents {
    /**
     * The spell is about to pay. [saveChance] is a percentage rolled separately for each rune type.
     * A saved rune is not spent. Costs that are not runes, such as a banana, are never saved.
     */
    public class Prepare(
        override val player: Player,
        public val spell: ItemServerType,
        public var saveChance: Int = 0,
    ) : UnboundEvent, PlayerEvent

    /** The spell paid. [runes] lists each cost and whether it was saved. */
    public class Spent(
        override val player: Player,
        public val spell: ItemServerType,
        public val runes: List<ResourceSpend>,
    ) : UnboundEvent, PlayerEvent
}
