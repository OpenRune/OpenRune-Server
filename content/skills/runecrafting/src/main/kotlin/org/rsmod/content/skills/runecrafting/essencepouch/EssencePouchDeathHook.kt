package org.rsmod.content.skills.runecrafting.essencepouch

import org.rsmod.api.death.PlayerDeathContext
import org.rsmod.api.death.PlayerDeathDropHook

internal class EssencePouchDeathHook : PlayerDeathDropHook {
    override fun prepareCompletion(context: PlayerDeathContext): () -> Unit {
        val carried = EssencePouch.Tier.entries.filter { EssencePouch.ownsTier(context.player, it) }
        return { carried.filterNot { EssencePouch.ownsTier(context.player, it) }.forEach { EssencePouch.clearStorage(context.player, it) } }
    }
}
