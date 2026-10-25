package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.random.GameRandom
import org.rsmod.content.other.pets.PetRewards
import org.rsmod.game.entity.Player

enum class FarmingRewardRoll(val pet: Boolean, val seed: Boolean) {
    NONE(false, false),
    PET(true, false),
    SEED(false, true),
    BOTH(true, true),
}

@Singleton
class FarmingRewards @Inject constructor(private val pets: PetRewards, private val random: GameRandom) {
    fun prepare(player: Player, crop: Crop): FarmingRewardRoll {
        val pet = crop.petChance > 0 && pets.rollSkillingPetChance(player, STAT_FARMING, crop.petChance)
        val chance = crop.hesporiSeedChance
        val seed = chance != null && player.vars[HESPORI_SEEDS] == 0 && random.of(chance) == 0
        return when {
            pet && seed -> FarmingRewardRoll.BOTH
            pet -> FarmingRewardRoll.PET
            seed -> FarmingRewardRoll.SEED
            else -> FarmingRewardRoll.NONE
        }
    }

    fun complete(player: Player, roll: FarmingRewardRoll) {
        if (roll.seed) player.invAdd(player.inv, HESPORI_SEED, 1, ignoreVirtualStorage = true)
        if (roll.pet) pets.give(player, TANGLEROOT)
    }

    private companion object {
        const val HESPORI_SEEDS = "varp.farming_hespori_seed_opt_out"
        const val HESPORI_SEED = "obj.hespori_seed"
        const val TANGLEROOT = "obj.skillpetfarming"
    }
}
