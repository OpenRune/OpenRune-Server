package org.rsmod.api.combat.commons.magic

import org.rsmod.game.entity.Player

/**
 * Decides whether [player] counts as having completed [quest] (a `dbrow.quest_*` key without its
 * prefix) for the purpose of casting a quest-locked spell. Implemented by the quest module so the
 * active quest-requirement policy applies.
 */
public fun interface SpellQuestRequirement {
    public fun hasCompleted(player: Player, quest: String): Boolean
}
