package org.rsmod.api.bosses.spec

/**
 * Gives every player fighting the boss a private hp pool, for a shared npc whose own hp is locked
 * (`param.hitpoints_locked`). Player hits still go through the normal pipeline for xp and hitmarks;
 * the damage is taken from the attacker's pool instead of the npc. Content decides who has a pool
 * through [org.rsmod.api.bosses.runtime.PersonalHpTracker.add]; hits from anyone else are ignored.
 *
 * [hpVarbit] is written on every change. [onDeath] names the ability run once, against the player
 * whose pool reached 0; it runs without npc access, like a hit reaction.
 */
data class PersonalHpSpec(
    val maxHitpoints: Int,
    val hpVarbit: String? = null,
    val onDeath: String? = null,
)
