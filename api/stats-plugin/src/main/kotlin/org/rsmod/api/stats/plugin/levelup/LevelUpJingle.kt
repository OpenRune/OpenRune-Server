package org.rsmod.api.stats.plugin.levelup

internal sealed interface LevelUpJingle {
    fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean): String

    data class Single(val jingle: String) : LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            jingle
    }

    data class Unlocks(val normal: String, val unlock: String) : LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            if (guideList && unlocks) unlock else normal
    }

    data class GuideList(val normal: String, val unlock: String) : LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            if (guideList) unlock else normal
    }

    data class FromLevel(val below: String, val from: String, val threshold: Int) :
        LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            if (level >= threshold) from else below
    }

    data class Parity(val even: String, val odd: String) : LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            if (level % 2 == 0) even else odd
    }

    data class EveryTenth(val normal: String, val tenth: String) : LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            if (level % 10 == 0) tenth else normal
    }

    data class MaxLevel(val normal: String, val max: String) : LevelUpJingle {
        override fun select(level: Int, maxLevel: Int, unlocks: Boolean, guideList: Boolean) =
            if (level >= maxLevel) max else normal
    }
}
