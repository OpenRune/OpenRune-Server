package org.rsmod.api.player.events.skilling

import org.rsmod.events.UnboundEvent

public data class SkillingProductPrepareEvent(
    public val product: SkillingProduct,
) : UnboundEvent
