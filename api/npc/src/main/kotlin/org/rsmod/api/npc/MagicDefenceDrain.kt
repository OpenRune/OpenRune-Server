package org.rsmod.api.npc

import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Npc

/** Per-spawn bonus reduction, separate from Magic level and the shared NPC definition. */
public object MagicDefenceDrain {
    public const val VAR: String = "varn.magic_defence_bonus_drain"

    public fun bonus(base: Int, drain: Int): Int = if (base <= 0) base else (base - drain.coerceAtLeast(0)).coerceAtLeast(0)

    public fun apply(target: Npc, amount: Int): Int {
        val base = target.visType.param(params.defence_magic).coerceAtLeast(0)
        val before = target.vars[VAR].coerceIn(0, base)
        val added = amount.coerceIn(0, base - before)
        if (added > 0) target.vars[VAR] = before + added
        return added
    }
}
