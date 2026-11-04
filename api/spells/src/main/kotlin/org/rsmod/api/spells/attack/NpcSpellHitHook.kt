package org.rsmod.api.spells.attack

import dev.openrune.types.ItemServerType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

/**
 * Content-specific overrides for spell casts against a particular [Npc]. Used by encounters whose
 * monsters replace parts of the standard magic accuracy and damage rules.
 *
 * Hooks are consulted in no particular order; the first non-null [rollHit] result wins, and
 * [alwaysMaxHit] is true if any hook returns `true`.
 */
public interface NpcSpellHitHook {
    /**
     * Forces the hit/splash result for [spell] cast at [npc], or returns `null` to use the
     * standard accuracy formula.
     */
    public fun rollHit(player: Player, npc: Npc, spell: ItemServerType): Boolean? = null

    /**
     * Returns `true` when a successful [spell] hit on [npc] should always roll the spell's maximum
     * damage instead of a random value in the usual range.
     */
    public fun alwaysMaxHit(player: Player, npc: Npc, spell: ItemServerType): Boolean = false

    /**
     * Called after a successful [spell] hit has been queued on [npc], before it impacts. [hitDelay]
     * is the impact delay in cycles used for the primary hit, so cleave-style hits can be queued
     * with the same delay and land on the same tick.
     */
    public fun onHitQueued(
        player: Player,
        npc: Npc,
        spell: ItemServerType,
        damage: Int,
        hitDelay: Int,
    ) {}
}
