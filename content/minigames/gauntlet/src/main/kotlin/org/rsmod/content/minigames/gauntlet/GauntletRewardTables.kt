package org.rsmod.content.minigames.gauntlet

internal enum class RewardTier(val tableKey: String?) {
    NONE(null),
    JUNK(GauntletRewardKeys.JUNK),
    INCOMPLETE(GauntletRewardKeys.INCOMPLETE),
    NORMAL(GauntletRewardKeys.NORMAL),
    CORRUPTED(GauntletRewardKeys.CORRUPTED),
}

internal object GauntletRewardTables {
    const val INCOMPLETE_POINTS = 50

    fun tierForPoints(points: Int): RewardTier =
        when {
            points >= INCOMPLETE_POINTS -> RewardTier.INCOMPLETE
            points > 0 -> RewardTier.JUNK
            else -> RewardTier.NONE
        }
}
