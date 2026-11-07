package org.rsmod.content.events.leaguetasks.pack

/** How a `dbtable.league_task_trigger` row is met; [id] is what the row's `trigger` column holds. */
enum class LeagueTaskTrigger(val id: Int) {
    /** At least `count` of the `stat` skills (every skill when empty) have a base level of `value`. */
    Level(1),
    /** Every skill has a base level of `value`. */
    BaseLevel(2),
    TotalLevel(3),
    CombatLevel(4),
    /** At least `count` of the `stat` skills (every skill when empty) have `value` xp. */
    Xp(5);

    companion object {
        private val BY_ID = entries.associateBy(LeagueTaskTrigger::id)

        fun of(id: Int): LeagueTaskTrigger? = BY_ID[id]
    }
}
