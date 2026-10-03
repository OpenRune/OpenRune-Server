package org.rsmod.api.combat.formulas.accuracy.melee

import jakarta.inject.Inject
import java.util.EnumSet
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.HIT_CHANCE_SCALE
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMeleeAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatNpcAttributeCollector
import org.rsmod.api.combat.formulas.isSlayerTask
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

public class ReducedMeleeDefenceAccuracy @Inject constructor(
    private val npcMelee: PvNMeleeAccuracy,
    private val playerMelee: PvPMeleeAccuracy,
    private val meleeAttributes: CombatMeleeAttributeCollector,
    private val npcAttributes: CombatNpcAttributeCollector,
    private val rng: GameRandom,
) {
    public fun roll(source: Player, target: PathingEntity, attack: CombatAttack.Melee,
                    blockType: MeleeAttackType, defencePercent: Int, attackPercent: Int = 100): Boolean {
        require(defencePercent in 0..100)
        require(attackPercent > 0)
        if (source.adminMaxHit) return true
        val npc = when (target) {
            is Npc -> npcAttributes.collect(target.visType, target, target.hitpoints,
                target.baseHitpointsLvl, target.visType.isSlayerTask(source))
            is Player -> EnumSet.noneOf(CombatNpcAttributes::class.java)
        }
        val equipment = meleeAttributes.collect(source, attack.type)
        val baseAttackRoll = npcMelee.computeAttackRoll(source, attack.type, attack.style, equipment, npc)
        val attackRoll = (baseAttackRoll.toLong() * attackPercent / 100).toInt()
        val defenceRoll = when (target) {
            is Npc -> npcMelee.computeDefenceRoll(target.visType, target.defenceLvl,
                source.vars["varbit.toa_client_raid_level"], blockType, npc)
            is Player -> playerMelee.computeDefenceRoll(target, blockType)
        }
        val reduced = (defenceRoll.toLong() * defencePercent / 100).toInt()
        return rng.of(HIT_CHANCE_SCALE) < AccuracyOperations.calculateHitChance(attackRoll, reduced, npc)
    }
}
