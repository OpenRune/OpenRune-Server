package org.rsmod.api.droptable

import org.rsmod.game.entity.Player

/**
 * The treasure trails module registers how a dropped clue scroll resolves (e.g. into a scroll
 * box), so drop tables stay free of a dependency on it.
 */
public object ClueScrollDrops {
    /** Returns the obj to drop in place of the table's clue scroll, or `null` to keep it. */
    @Volatile public var resolve: (Player, String) -> String? = { _, _ -> null }
}
