package org.rsmod.content.other.pets

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathDropContext
import org.rsmod.api.death.NpcDeathDropHook

class PetDropHook @Inject constructor(private val rewards: PetRewards) : NpcDeathDropHook {
    override fun tryConsume(context: NpcDeathDropContext): Boolean =
        rewards.give(context.hero, context.dropType.internalName)
}
