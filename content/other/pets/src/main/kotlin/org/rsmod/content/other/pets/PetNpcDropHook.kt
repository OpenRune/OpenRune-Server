package org.rsmod.content.other.pets

import jakarta.inject.Inject
import org.rsmod.api.death.NpcDeathKillContext
import org.rsmod.api.death.NpcDeathKillHook
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.PetDropsRow

/**
 * Rolls boss pets listed in `dbtable.pet_drops`. Pets that already sit in an npc's drop table are
 * handled by [PetDropHook] instead, which intercepts the item before it reaches the ground.
 */
class PetNpcDropHook
@Inject
constructor(private val rewards: PetRewards, private val random: GameRandom) : NpcDeathKillHook {
    private val byNpc: Map<Int, Drop> =
        PetDropsRow.all().associate { it.npc.id to Drop(it.pet.internalName, it.rate) }

    override fun onKill(context: NpcDeathKillContext) {
        val drop = byNpc[context.npc.id] ?: return
        if (drop.rate <= 0 || random.of(drop.rate) != 0) {
            return
        }
        rewards.give(context.hero, drop.pet)
    }

    private class Drop(val pet: String, val rate: Int)
}
