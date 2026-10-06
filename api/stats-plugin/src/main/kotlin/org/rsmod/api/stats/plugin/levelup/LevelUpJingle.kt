package org.rsmod.api.stats.plugin.levelup

import dev.openrune.tables.LevelUpRule

internal class LevelUpJingle(
    private val rule: LevelUpRule,
    private val normal: Int,
    private val alternate: Int?,
    private val threshold: Int?,
) {
    init {
        require(rule == LevelUpRule.Single || alternate != null) {
            "Level-up rule $rule needs an alternate jingle."
        }
        require(rule != LevelUpRule.FromLevel || threshold != null) {
            "Level-up rule $rule needs a threshold level."
        }
    }

    fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean): Int {
        val alternate = alternate ?: return normal
        val useAlternate =
            when (rule) {
                LevelUpRule.Single -> false
                LevelUpRule.Unlocks -> guideList && unlocks
                LevelUpRule.GuideList -> guideList
                LevelUpRule.FromLevel -> level >= checkNotNull(threshold)
                LevelUpRule.Parity -> level % 2 != 0
                LevelUpRule.EveryTenth -> level % 10 == 0
                LevelUpRule.MaxLevel -> level >= maxLevel
            }
        return if (useAlternate) alternate else normal
    }
}
