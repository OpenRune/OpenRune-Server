package org.rsmod.content.interfaces.journal.tab

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** Op 1 on the journal side stone; the client switches tabs itself. */
public data class JournalStoneSelectedEvent(override val player: Player) : UnboundEvent, PlayerEvent
