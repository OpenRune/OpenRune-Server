package org.rsmod.api.combat.manager

import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** Content-owned accuracy/max-roll rules, applied before XP and special damage splitting. */
@Singleton
public class NpcMaxHitRegistry {
    private data class Rule(val melee: (Player, MeleeAttackType?) -> Boolean,
        val ranged: (Player, RangedAttackType?) -> Boolean)
    private val rules = mutableMapOf<Int, Rule>()
    public fun register(id: Int, melee: (Player, MeleeAttackType?) -> Boolean,
        ranged: (Player, RangedAttackType?) -> Boolean) {
        check(id !in rules) { "Duplicate NPC max-hit rule: $id" }
        rules[id] = Rule(melee, ranged)
    }
    public fun melee(player: Player, target: PathingEntity, type: MeleeAttackType?): Boolean =
        (target as? Npc)?.let { rules[it.id]?.melee?.invoke(player, type) } == true
    public fun ranged(player: Player, target: PathingEntity, type: RangedAttackType?): Boolean =
        (target as? Npc)?.let { rules[it.id]?.ranged?.invoke(player, type) } == true
}
