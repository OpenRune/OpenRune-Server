package org.rsmod.api.combat.formulas.accuracy.melee

import jakarta.inject.Inject
import java.util.EnumSet
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.formulas.HIT_CHANCE_SCALE
import org.rsmod.api.combat.formulas.accuracy.AccuracyOperations
import org.rsmod.api.combat.formulas.accuracy.magic.PvNMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.magic.PvPMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.melee.PvNMeleeAccuracy
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMeleeAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatNpcAttributeCollector
import org.rsmod.api.combat.formulas.isSlayerTask
import org.rsmod.api.npc.MagicDefenceDrain
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** Blessed Saradomin sword retains melee offence but contests the target's magic defence. */
public class MeleeAgainstMagicAccuracy @Inject constructor(
    private val melee: PvNMeleeAccuracy,
    private val npcMagic: PvNMagicAccuracy,
    private val playerMagic: PvPMagicAccuracy,
    private val meleeAttributes: CombatMeleeAttributeCollector,
    private val npcAttributes: CombatNpcAttributeCollector,
    private val rng: GameRandom,
) {
    public fun roll(source: Player, target: PathingEntity, attack: CombatAttack.Melee): Boolean {
        if (source.adminMaxHit) return true
        val npc = when (target) {
            is Npc -> npcAttributes.collect(target.visType, target, target.hitpoints,
                target.baseHitpointsLvl, target.visType.isSlayerTask(source))
            is Player -> EnumSet.noneOf(CombatNpcAttributes::class.java)
        }
        val equipment = meleeAttributes.collect(source, attack.type)
        val attackRoll = melee.computeAttackRoll(source, attack.type, attack.style, equipment, npc)
        val defenceRoll = when (target) {
            is Npc -> npcMagic.computeDefenceRoll(target.visType, target.defenceLvl, target.magicLvl,
                source.vars["varbit.toa_client_raid_level"], npc, target.vars[MagicDefenceDrain.VAR])
            is Player -> playerMagic.computeDefenceRoll(target)
        }
        return rng.of(HIT_CHANCE_SCALE) < AccuracyOperations.calculateHitChance(attackRoll, defenceRoll, npc)
    }
}
