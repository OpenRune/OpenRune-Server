package org.rsmod.api.droptable

import org.rsmod.game.entity.Player

public enum class ClueTier(public val difficulty: Int, public val boxObj: String) {
    Beginner(0, "obj.league_clue_box_beginner"),
    Easy(1, "obj.league_clue_box_easy"),
    Medium(2, "obj.league_clue_box_medium"),
    Hard(3, "obj.league_clue_box_hard"),
    Elite(4, "obj.league_clue_box_elite"),
    Master(5, "obj.league_clue_box_master");

    public companion object {
        public fun parse(name: String): ClueTier =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) }
                ?: error(
                    "Unknown clue tier '$name'. " +
                        "Expected one of: ${entries.joinToString { it.name.lowercase() }}",
                )
    }
}

/**
 * The treasure trails module registers what a clue roll of a tier hands out (a clue scroll, a
 * scroll box, or nothing when the player is at their limit), so drop tables stay free of a
 * dependency on it.
 */
public object ClueScrollDrops {
    @Volatile public var resolve: (Player, ClueTier) -> String? = { _, _ -> null }
}

public fun clue(tier: ClueTier, condition: (Player) -> Boolean = { true }): DropRollItem =
    DropRollItem(obj = tier.boxObj, count = 1..1, condition = condition, clue = tier)

/** The obj this drop hands [player], or null when a clue roll is withheld. */
public fun DropRollItem.resolveObj(player: Player): String? {
    val tier = clue ?: return transformObj(player) ?: obj
    return ClueScrollDrops.resolve(player, tier)
}
