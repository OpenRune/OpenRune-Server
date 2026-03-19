package org.rsmod.api.combat.formulas.accuracy.magic

import dev.openrune.types.NpcServerType
import org.rsmod.api.combat.accuracy.npc.NpcMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Npc

public class NvNMagicAccuracy {
    public fun getHitChance(npc: Npc, target: Npc): Int =
        computeHitChance(
            source = npc,
            sourceType = npc.visType,
            target = target,
            targetType = target.visType,
        )

    public fun computeHitChance(
        source: Npc,
        sourceType: NpcServerType,
        target: Npc,
        targetType: NpcServerType,
    ): Int {
        val attackRoll = computeAttackRoll(source, sourceType)
        val defenceRoll = computeDefenceRoll(targetType, target.defenceLvl, target.magicLvl)
        return AccuracyOperations.calculateHitChance(attackRoll, defenceRoll)
    }

    public fun computeAttackRoll(source: Npc, sourceType: NpcServerType): Int {
        val effectiveMagic = NpcMagicAccuracy.calculateEffectiveMagic(source.magicLvl)
        val magicBonus = sourceType.param(params.attack_magic)
        return NpcMagicAccuracy.calculateBaseAttackRoll(effectiveMagic, magicBonus)
    }

    public fun computeDefenceRoll(
        target: NpcServerType,
        targetDefence: Int,
        targetMagic: Int,
    ): Int {
        val defenceLevel =
            if (target.param(params.magic_defence_uses_defence_level)) {
                targetDefence
            } else {
                targetMagic
            }
        val effectiveDefence = NpcMagicAccuracy.calculateEffectiveDefence(defenceLevel)
        val defenceBonus = target.param(params.defence_magic)
        return NpcMagicAccuracy.calculateBaseDefenceRoll(effectiveDefence, defenceBonus)
    }
}
