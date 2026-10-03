package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import java.util.EnumSet
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.formulas.attributes.CombatNpcAttributes
import org.rsmod.api.combat.formulas.attributes.DamageReductionAttributes
import org.rsmod.api.combat.formulas.attributes.collector.CombatMeleeAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.CombatNpcAttributeCollector
import org.rsmod.api.combat.formulas.attributes.collector.DamageReductionAttributeCollector
import org.rsmod.api.combat.formulas.maxhit.melee.PvNMeleeMaxHit
import org.rsmod.api.combat.formulas.maxhit.melee.PvPMeleeMaxHit
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** Retains the engine's equipment modifiers while keeping special and post-special stages distinct. */
class MeleeSpecialDamage @Inject constructor(
    private val meleeAttributes: CombatMeleeAttributeCollector,
    private val npcAttributes: CombatNpcAttributeCollector,
    private val npcMaxHit: PvNMeleeMaxHit,
    private val playerMaxHit: PvPMeleeMaxHit,
    private val reductions: DamageReductionAttributeCollector,
    private val random: GameRandom,
) {
    fun maximum(source: Player, target: PathingEntity, attack: CombatAttack.Melee,
                firstPercent: Int = 100, secondPercent: Int = 100, deferReductions: Boolean = false): Int {
        val melee = meleeAttributes.collect(source, attack.type)
        val npc = targetAttributes(source, target)
        val base = when (target) {
            is Npc -> npcMaxHit.computeModifiedDamage(source, attack.style, melee, npc)
            is Player -> playerMaxHit.computeModifiedDamage(source, attack.style, melee, npc)
        }
        val boosted = scaledMaximum(base, firstPercent, secondPercent)
        // Claws reduce each split hit; Voidwaker uses magic rather than Corp's melee reduction.
        if (deferReductions) return boosted
        return when (target) {
            is Npc -> npcMaxHit.modifyPostSpec(source, boosted, melee, npc)
            is Player -> reduce(target, playerMaxHit.modifyPostSpec(source, boosted, melee, npc))
        }
    }

    fun reduce(target: Player, damage: Int): Int =
        if (DamageReductionAttributes.ElysianProc in reductions.collectPvP(target, random)) damage * 3 / 4 else damage

    fun modifyRolledHit(source: Player, target: PathingEntity, attack: CombatAttack.Melee, damage: Int): Int {
        val melee = meleeAttributes.collect(source, attack.type)
        val npc = targetAttributes(source, target)
        return when (target) {
            is Npc -> npcMaxHit.modifyPostSpec(source, damage, melee, npc)
            is Player -> reduce(target, playerMaxHit.modifyPostSpec(source, damage, melee, npc))
        }
    }

    private fun targetAttributes(source: Player, target: PathingEntity): EnumSet<CombatNpcAttributes> = when (target) {
        is Npc -> npcAttributes.collect(target.visType, target, target.hitpoints,
            target.baseHitpointsLvl, source.vars["varp.slayer_target"].let { task ->
                task > 0 && target.visType.paramOrNull(BaseParams.slayer_task_id) == task
            })
        is Player -> EnumSet.noneOf(CombatNpcAttributes::class.java)
    }

    internal companion object {
        fun scaledMaximum(base: Int, firstPercent: Int, secondPercent: Int): Int =
            (base * firstPercent / 100) * secondPercent / 100
    }
}
