package org.rsmod.content.minigames.gauntlet

internal enum class RewardTier(val tableKey: String?) {
    NONE(null),
    JUNK(GauntletRewardKeys.JUNK),
    INCOMPLETE(GauntletRewardKeys.INCOMPLETE),
    NORMAL(GauntletRewardKeys.NORMAL),
    CORRUPTED(GauntletRewardKeys.CORRUPTED),
}

internal object GauntletRewardTables {
    const val INCOMPLETE_DAMAGE = 50

    fun tierForDamage(damage: Int): RewardTier =
        when {
            damage >= INCOMPLETE_DAMAGE -> RewardTier.INCOMPLETE
            damage > 0 -> RewardTier.JUNK
            else -> RewardTier.NONE
        }
}
