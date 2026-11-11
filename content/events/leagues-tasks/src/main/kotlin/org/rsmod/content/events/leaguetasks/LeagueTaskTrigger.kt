package org.rsmod.content.events.leaguetasks

/**
 * How a league task requirement is met; a task file names it in `trigger`, in snake case. A
 * [isState] trigger describes something the player has rather than did, so it is also checked a
 * tick after login. Any other trigger is an event: with `times` above 1, or on a task with more than
 * one requirement, it needs a `counter` varbit to remember progress.
 */
public enum class LeagueTaskTrigger(public val isState: Boolean) {
    /** At least `times` of the `stat` skills (every skill when empty) have a base level of `value`. */
    Level(isState = true),

    /** Every skill has a base level of `value`. */
    BaseLevel(isState = true),
    TotalLevel(isState = true),
    CombatLevel(isState = true),

    /** At least `times` of the `stat` skills (every skill when empty) have `value` xp. */
    Xp(isState = true),

    /** At least `times` of the `obj` items are worn at once. */
    Equip(isState = true),

    /** The player gets the kill of an `npc`, an npc of a `category`, or a `slayer_task` npc. */
    Kill(isState = false),

    /**
     * A skilling action gives one of the `obj` products (any when empty) from a `stat` skill (any
     * when empty), done with one of the `tool` items when the requirement lists any. Progress advances by
     * the number of items made.
     */
    Product(isState = false),

    /** One of the `obj` spells is cast, its runes paid. */
    Spell(isState = false),

    /** An item (one of `obj`, any when empty) is alched for at least `value` coins. */
    Alchemy(isState = false),

    /** One of the `obj` logs (any when empty) is burnt, on the ground or on a fire. */
    Firemaking(isState = false),

    /** One of the `obj` bones (any when empty) is buried; scattering ashes does not count. */
    Bury(isState = false),

    /** One of the `obj` items (any when empty) is offered at an altar, the Ectofuntus or a bowl. */
    Offer(isState = false),

    /** One of the `obj` foods or potions is eaten or drunk. */
    Consume(isState = false),

    /** The `varp` or `varbit` holds at least `value`: quest progress, kill counts, unlocks. */
    Var(isState = true),

    /** The `interface` is opened. */
    Interface(isState = false),

    /** The player enters one of the `area` areas. */
    Enter(isState = false),

    /** One of the `obj` items enters the player's possession; progress advances by the amount. */
    Obtain(isState = false),

    /** One of the `obj` items is bought from a shop; progress advances by the amount. */
    ShopBuy(isState = false),

    /** Option `option` (any when unset) finishes on one of the `npc` npcs or `loc` locs. */
    Interact(isState = false),

    /** One of the `npc` npcs is pickpocketed. */
    Pickpocket(isState = false),

    /** One of the `loc` stalls is stolen from, giving one of the `obj` items when listed. */
    Steal(isState = false),

    /** A special attack is performed, with one of the `obj` weapons when listed. */
    SpecialAttack(isState = false),

    /** One of the `obj` ammo items is fired; progress advances by the amount used. */
    Ammo(isState = false),

    /**
     * The `emote` (its emote tab name) is performed, near one of the `npc` npcs or `loc` locs when
     * the requirement lists any.
     */
    Emote(isState = false),

    /**
     * One of the `obj` items (any when empty) is used on one of the `npc` npcs or `loc` locs and its
     * handler runs to the end. Only items with a handler on that target count.
     */
    UseOn(isState = false),

    /**
     * At least `value` distinct collection log slots are filled across the `collection` category
     * rows, or across the whole log when the requirement lists none.
     */
    CollectionSlots(isState = true),

    /** One of the `obj` items is picked from a loc such as flax or a cabbage. */
    Pick(isState = false),

    /** A slayer task is finished. */
    SlayerTaskComplete(isState = false),

    /** An npc (one of `npc`, any when empty) is killed for the player's slayer task. */
    SlayerKill(isState = false),

    /** A superior slayer creature is killed. */
    SuperiorKill(isState = false);

    public companion object {
        private val BY_NAME = entries.associateBy { it.name.lowercase() }

        /** Accepts `special_attack` as well as `SpecialAttack`. */
        public fun named(name: String): LeagueTaskTrigger? =
            BY_NAME[name.replace("_", "").lowercase()]
    }
}
