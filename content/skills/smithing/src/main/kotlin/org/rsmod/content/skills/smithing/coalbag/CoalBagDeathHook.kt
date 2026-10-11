package org.rsmod.content.skills.smithing.coalbag

import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook

internal class CoalBagDeathHook : PlayerDeathDropHook {
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val carried = CoalBag.owns(context.player)
        return { if (carried && !CoalBag.owns(context.player)) CoalBag.clear(context.player) }
    }
}
