package org.rsmod.api.combat.commons.npc

import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public fun interface NpcAttackOverrideHook {
    public fun override(npc: Npc, target: Player): NpcAttackOverride?
}

public sealed class NpcAttackOverride {
    public data class Fixed(val damage: Int, val penetration: Int = 0) : NpcAttackOverride()

    public data class MaxHit(val maxHit: Int, val guaranteed: Boolean = false) :
        NpcAttackOverride()
}

public fun NpcAttackOverride?.guaranteesHit(): Boolean =
    this is NpcAttackOverride.MaxHit && guaranteed

public fun NpcAttackOverride?.penetration(): Int =
    (this as? NpcAttackOverride.Fixed)?.penetration ?: 0

public inline fun NpcAttackOverride?.rollDamage(
    accurate: Boolean,
    standardMaxHit: () -> Int,
    roll: (maxHit: Int) -> Int,
): Int =
    when (this) {
        is NpcAttackOverride.Fixed -> damage
        is NpcAttackOverride.MaxHit -> if (accurate) roll(maxHit) else 0
        null -> if (accurate) roll(standardMaxHit()) else 0
    }
