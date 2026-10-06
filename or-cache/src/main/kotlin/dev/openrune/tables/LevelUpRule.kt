package dev.openrune.tables

/** Decides whether a level-up plays a stat's alternate jingle instead of its normal one. */
enum class LevelUpRule(val id: Int) {
    Single(0),
    Unlocks(1),
    GuideList(2),
    FromLevel(3),
    Parity(4),
    EveryTenth(5),
    MaxLevel(6);

    companion object {
        private val byId = entries.associateBy(LevelUpRule::id)

        fun of(id: Int): LevelUpRule = byId[id] ?: error("Unknown level-up rule id: $id")
    }
}
