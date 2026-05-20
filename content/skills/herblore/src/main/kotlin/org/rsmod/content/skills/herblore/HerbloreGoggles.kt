package org.rsmod.content.skills.herblore

import org.rsmod.api.player.hat
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType


object HerbloreGoggles {

    val GOGGLE_TYPES = listOf("obj.mm_alchemist_hat", "obj.mm_alchemist_hat_alt")

    val EXCLUDED_SECONDARIES = setOf("obj.sote_crystal_dust", "obj.ashes")

    fun isWearing(player: Player): Boolean {
        val hat = player.hat ?: return false
        return GOGGLE_TYPES.any { hat.isType(it) }
    }

    fun canSaveForRecipe(potion: HerbloreFinishedRow): Boolean {
        if (potion.secondaries.size == 1) {
            val secondary = potion.secondaries.first().internalName
            return secondary !in EXCLUDED_SECONDARIES
        }
        return potion.secondaries.none { it.internalName in EXCLUDED_SECONDARIES }
    }

    fun rollSavedSecondaryCounts(
        player: Player,
        random: GameRandom,
        potion: HerbloreFinishedRow,
    ): SavedSecondaries {
        if (!isWearing(player) || !canSaveForRecipe(potion)) {
            return SavedSecondaries.NONE
        }
        if (!random.randomBoolean(100 / 10)) {
            return SavedSecondaries.NONE
        }
        return if (potion.secondaries.size == 1) {
            val secondary = potion.secondaries.first().internalName
            val fullAmount = potion.secondariesAmount ?: 1
            SavedSecondaries(single = secondary to fullAmount)
        } else {
            SavedSecondaries(
                perSecondary = potion.secondaries.associate { it.internalName to 1 },
            )
        }
    }

    data class SavedSecondaries(
        val single: Pair<String, Int>? = null,
        val perSecondary: Map<String, Int> = emptyMap(),
    ) {
        fun savedAmount(secondaryName: String): Int =
            single?.takeIf { it.first == secondaryName }?.second
                ?: perSecondary[secondaryName]
                ?: 0

        companion object {
            val NONE = SavedSecondaries()
        }
    }
}
