package org.rsmod.api.combat.formulas.accuracy.ranged

import dev.openrune.types.NpcServerType
import org.rsmod.api.combat.accuracy.npc.NpcRangedAccuracy
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Npc

public class NvNRangedAccuracy {
    public fun getHitChance(npc: Npc, target: Npc): Int =
        computeHitChance(
            source = npc,
            sourceType = npc.visType,
            target = target.visType,
            targetDefence = target.defenceLvl,
        )

    public fun computeHitChance(
        source: Npc,
        sourceType: NpcServerType,
        target: NpcServerType,
        targetDefence: Int,
    ): Int {
        val attackRoll = computeAttackRoll(source, sourceType)
        val defenceRoll = computeDefenceRoll(target, targetDefence)
        return AccuracyOperations.calculateHitChance(attackRoll, defenceRoll)
    }

    public fun computeAttackRoll(source: Npc, sourceType: NpcServerType): Int {
        val effectiveRanged = NpcRangedAccuracy.calculateEffectiveRanged(source.rangedLvl)
        val rangedBonus = sourceType.param(params.attack_ranged)
        return NpcRangedAccuracy.calculateBaseAttackRoll(effectiveRanged, rangedBonus)
    }

    public fun computeDefenceRoll(target: NpcServerType, targetDefence: Int): Int {
        val effectiveDefence = NpcRangedAccuracy.calculateEffectiveDefence(targetDefence)
        val defenceBonus = target.param(params.defence_ranged)
        return NpcRangedAccuracy.calculateBaseDefenceRoll(effectiveDefence, defenceBonus)
    }
}
