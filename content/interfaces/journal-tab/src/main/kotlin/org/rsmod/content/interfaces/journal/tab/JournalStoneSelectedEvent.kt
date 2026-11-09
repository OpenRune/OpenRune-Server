package org.rsmod.content.interfaces.journal.tab

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** The journal side stone was clicked to show the tab (op 1); the client switches tabs itself. */
public data class JournalStoneSelectedEvent(override val player: Player) : UnboundEvent, PlayerEvent
