package org.rsmod.content.events.leaguetasks.pack

/**
 * How a `dbtable.league_task_trigger` row is met; [id] is what the row's `trigger` column holds.
 * A [isState] trigger describes something the player has rather than did, so it is also checked a
 * tick after login. Any other trigger is an event: with a `count` above 1, or on a task with more
 * than one row, its row needs a `counter` varbit to remember progress.
 */
enum class LeagueTaskTrigger(val id: Int, val isState: Boolean) {
    /** At least `count` of the `stat` skills (every skill when empty) have a base level of `value`. */
    Level(1, isState = true),
    /** Every skill has a base level of `value`. */
    BaseLevel(2, isState = true),
    TotalLevel(3, isState = true),
    CombatLevel(4, isState = true),
    /** At least `count` of the `stat` skills (every skill when empty) have `value` xp. */
    Xp(5, isState = true),
    /** At least `count` of the `obj` items are worn at once. */
    Equip(6, isState = true),
    /** The player gets the kill of an `npc`, an npc of a `category`, or a `slayer_task` npc. */
    Kill(7, isState = false),
    /**
     * A skilling action gives one of the `obj` products (any when empty) from a `stat` skill (any
     * when empty), done with one of the `tool` items when the row lists any. Progress advances by
     * the number of items made.
     */
    Product(8, isState = false),
    /** One of the `obj` spells is cast, its runes paid. */
    Spell(9, isState = false),
    /** An item (one of `obj`, any when empty) is alched for at least `value` coins. */
    Alchemy(10, isState = false),
    /** One of the `obj` logs (any when empty) is burnt, on the ground or on a fire. */
    Firemaking(11, isState = false),
    /** One of the `obj` bones (any when empty) is buried; scattering ashes does not count. */
    Bury(12, isState = false),
    /** One of the `obj` items (any when empty) is offered at an altar, the Ectofuntus or a bowl. */
    Offer(13, isState = false),
    /** One of the `obj` foods or potions is eaten or drunk. */
    Consume(14, isState = false),
    /** The `varp` or `varbit` holds at least `value`: quest progress, kill counts, unlocks. */
    Var(15, isState = true),
    /** The `interface` is opened. */
    Interface(16, isState = false);

    companion object {
        private val BY_ID = entries.associateBy(LeagueTaskTrigger::id)

        fun of(id: Int): LeagueTaskTrigger? = BY_ID[id]
    }
}
