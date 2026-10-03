package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import java.util.EnumSet
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.DamageReductionAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatNpcAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatRangedAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.DamageReductionAttributeCollector
import org.rsmod.api.combat.formulas.maxhit.ranged.PvNRangedMaxHit
import org.rsmod.api.combat.formulas.maxhit.ranged.PvPRangedMaxHit
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.config.refs.params
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** Applies a special's damage roll and limits before the engine's target reductions. */
class RangedSpecialDamage @Inject constructor(
    private val rangedAttributes: CombatRangedAttributeCollector,
    private val npcAttributes: CombatNpcAttributeCollector,
    private val npcMaxHit: PvNRangedMaxHit,
    private val playerMaxHit: PvPRangedMaxHit,
    private val reductions: DamageReductionAttributeCollector,
    private val random: GameRandom,
) {
    fun maximum(source: Player, target: PathingEntity, attack: CombatAttack.Ranged, multiplier: Double): Int {
        val ranged = rangedAttributes.collect(source, attack.type, attack.style)
        val base = when (target) {
            is Npc -> npcMaxHit.computeModifiedDamage(source,
                max(target.magicLvl, target.visType.param(params.attack_magic)),
                attack.style, ranged, targetAttributes(source, target))
            is Player -> playerMaxHit.computeModifiedDamage(source, attack.style, ranged)
        }
        return (base * multiplier).toInt()
    }

    fun modifyRolledHit(source: Player, target: PathingEntity, attack: CombatAttack.Ranged, damage: Int): Int {
        val ranged = rangedAttributes.collect(source, attack.type, attack.style)
        val npc = targetAttributes(source, target)
        return when (target) {
            is Npc -> npcMaxHit.modifyPostSpec(source, damage, 0, ranged, npc)
            is Player -> {
                val modified = playerMaxHit.modifyPostSpec(source, damage, 0, ranged, npc)
                if (DamageReductionAttributes.ElysianProc in reductions.collectPvP(target, random)) modified * 3 / 4 else modified
            }
        }
    }

    private fun targetAttributes(source: Player, target: PathingEntity): EnumSet<CombatNpcAttributes> = when (target) {
        is Npc -> npcAttributes.collect(target.visType, target, target.hitpoints,
            target.baseHitpointsLvl, source.vars["varp.slayer_target"].let { task ->
                task > 0 && target.visType.paramOrNull(BaseParams.slayer_task_id) == task
            })
        is Player -> EnumSet.noneOf(CombatNpcAttributes::class.java)
    }
}
