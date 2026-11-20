package org.rsmod.api.player.stat

import org.rsmod.game.entity.Player

/** Server-wide skilling hooks that game modes such as leagues install at startup. */
public object SkillModifiers {
    /** Adjusts the success chance of a stat roll; returns the new chance in `0.0..1.0`. */
    @Volatile public var successRate: (Player, String, Double) -> Double = { _, _, rate -> rate }

    /** Multiplies the loot of a skilling action such as thieving. */
    @Volatile public var lootMultiplier: (Player, String) -> Int = { _, _ -> 1 }

    /** When `true`, the player meets every level requirement of [stat]'s actions. */
    @Volatile public var ignoresLevels: (Player, String) -> Boolean = { _, _ -> false }

    /** Multiplies how much a harvest of [stat] (such as a farming patch) gives. */
    @Volatile public var yieldMultiplier: (Player, String) -> Int = { _, _ -> 1 }

    /** Shortens a skilling action's repeat delay in ticks; never below 1. */
    @Volatile public var actionTicks: (Player, String, Int) -> Int = { _, _, ticks -> ticks }

    public fun meetsLevel(player: Player, stat: String, level: Int, required: Int): Boolean =
        level >= required || ignoresLevels(player, stat)

    public fun ticks(player: Player, stat: String, ticks: Int): Int =
        actionTicks(player, stat, ticks).coerceAtLeast(1)

    /** When `true`, Slayer masters let the player pick the task and how many to kill. */
    @Volatile public var choosesSlayerTasks: (Player) -> Boolean = { false }

    /** When `true`, the player can gather [stat]'s resources without a tool. */
    @Volatile public var bareHanded: (Player, String) -> Boolean = { _, _ -> false }

    /** Extra percent chance, on top of any gear, that a potion's secondaries are not used up. */
    @Volatile public var secondarySaveChance: (Player) -> Int = { 0 }

    /** When `true`, eating food does not use it up. */
    @Volatile public var keepsFood: (Player) -> Boolean = { false }

    public fun alwaysSucceeds(player: Player, stat: String): Boolean =
        successRate(player, stat, 0.0) >= 1.0
}
