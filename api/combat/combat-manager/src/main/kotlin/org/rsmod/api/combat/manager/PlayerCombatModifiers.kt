package org.rsmod.api.combat.manager

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public enum class CombatStyleGroup {
    Melee,
    Ranged,
    Magic,
}

/**
 * Server-wide multipliers on a player's accuracy and max hit against npcs, layered on top of the
 * regular formulas. Game modes such as leagues install [accuracy] and [maxHit] at startup; the
 * defaults leave combat untouched.
 */
public object PlayerCombatModifiers {
    @Volatile
    public var accuracy: (Player, Npc, CombatStyleGroup) -> Double = { _, _, _ -> 1.0 }

    @Volatile
    public var maxHit: (Player, Npc, CombatStyleGroup) -> Double = { _, _, _ -> 1.0 }
}
