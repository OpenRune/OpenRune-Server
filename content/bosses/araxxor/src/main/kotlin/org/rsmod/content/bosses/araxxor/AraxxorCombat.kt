package org.rsmod.content.bosses.araxxor

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.accuracy.magic.NvPMagicAccuracy
import org.rsmod.api.combat.formulas.accuracy.melee.NvPMeleeAccuracy
import org.rsmod.api.combat.formulas.accuracy.ranged.NvPRangedAccuracy
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

internal class AraxxorCombat @Inject constructor(
    private val melee: NvPMeleeAccuracy,
    private val ranged: NvPRangedAccuracy,
    private val magic: NvPMagicAccuracy,
    private val random: GameRandom,
    private val modifier: PlayerHitModifier,
    private val processor: InstantPlayerHitProcessor,
) {
    fun style(npc: Npc, target: Player): HitType = when {
        Bounds(target.coords).isWithinDistance(Bounds(npc.coords, npc.size), 1) -> HitType.Melee
        magic.computeDefenceRoll(target) < ranged.computeDefenceRoll(target) -> HitType.Magic
        else -> HitType.Ranged
    }

    fun roll(npc: Npc, target: Player, style: HitType, maxHit: Int? = null): Hit {
        val chance = when (style) {
            HitType.Melee -> melee.getHitChance(npc, target, MeleeAttackType.Crush)
            HitType.Ranged -> ranged.getHitChance(npc, target)
            HitType.Magic -> magic.getHitChance(npc, target)
            HitType.Typeless -> Int.MAX_VALUE
        }
        val protected = when (style) {
            HitType.Melee -> target.vars["varbit.prayer_protectfrommelee"] == 1
            HitType.Ranged -> target.vars["varbit.prayer_protectfrommissiles"] == 1
            HitType.Magic -> target.vars["varbit.prayer_protectfrommagic"] == 1
            HitType.Typeless -> false
        }
        val maximum = maxHit ?: AraxxorAttackRules.maxHit(style, protected)
        val damage = if (AccuracyFormulae.isSuccessfulHit(chance, random)) random.of(0..maximum) else 0
        return snapshot(npc, target, style, damage)
    }

    fun snapshot(npc: Npc, target: Player, style: HitType, damage: Int): Hit {
        val group = hitmark_groups.regular_damage
        val zero = hitmark_groups.zero_damage
        val mark = group.lit.asRSCM(RSCMType.HITMARK)
        val builder = HitBuilder(
            type = style, damage = damage.coerceAtLeast(0), sourceUid = npc.uid.packed,
            sourceSlot = npc.slotId, isFromNpc = true, isFromPlayer = false, clientDelay = 0,
            righthandType = null, secondaryType = null,
            targetHitmark = mark, sourceHitmark = mark,
            publicHitmark = group.tint?.asRSCM(RSCMType.HITMARK) ?: mark,
            zeroDamageHitmarkLit = zero.lit.asRSCM(RSCMType.HITMARK),
            zeroDamageHitmarkTint = zero.tint?.asRSCM(RSCMType.HITMARK),
            maxDamageHitmarkLit = group.max?.asRSCM(RSCMType.HITMARK),
            targetMaxDamageThreshold = Int.MAX_VALUE, sourceMaxDamageThreshold = Int.MAX_VALUE,
        )
        builder.penetration = 100
        with(modifier) { builder.modify(target) }
        return builder.build()
    }

    fun impact(npc: Npc, target: Player, hit: Hit) {
        if (target.hitpoints <= 0) return
        with(processor) { target.process(hit) }
        target.combatPlayDefendAnim()
        if (npc.isSlotAssigned && npc.hitpoints > 0) target.queueCombatRetaliate(npc)
    }
}

internal object AraxxorAttackRules {
    fun distance(from: CoordGrid, size: Int, to: CoordGrid, targetSize: Int): Int = maxOf(
        0, from.x - (to.x + targetSize - 1), to.x - (from.x + size - 1),
        from.z - (to.z + targetSize - 1), to.z - (from.z + size - 1))

    fun rupturaNpcMax(distance: Int, egg: Boolean): Int {
        val damage = when (distance) { 0 -> 80; 1 -> 64; 2, 3 -> 33; else -> 0 }
        return if (egg) damage.coerceAtMost(64) else damage
    }

    /** Fan centred on the target bearing; duplicate rounded tiles are emitted only once. */
    fun spray(origin: CoordGrid, target: CoordGrid): List<CoordGrid> {
        val bearing = kotlin.math.atan2((target.z - origin.z).toDouble(), (target.x - origin.x).toDouble())
        val radius = kotlin.math.hypot((target.x - origin.x).toDouble(), (target.z - origin.z).toDouble()).coerceAtLeast(4.0)
        return (-4..4).map { step ->
            val angle = bearing + step * kotlin.math.PI / 12
            origin.translate(kotlin.math.round(kotlin.math.cos(angle) * radius).toInt(),
                kotlin.math.round(kotlin.math.sin(angle) * radius).toInt())
        }.distinct()
    }

    fun ray(from: CoordGrid, through: CoordGrid, length: Int): List<CoordGrid> {
        val dx = through.x - from.x
        val dz = through.z - from.z
        val scale = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dz))
        if (scale == 0) return emptyList()
        return (1..length).map {
            from.translate(kotlin.math.round(dx.toDouble() * it / scale).toInt(),
                kotlin.math.round(dz.toDouble() * it / scale).toInt())
        }
    }

    fun maxHit(style: HitType, protected: Boolean): Int = when (style) {
        HitType.Melee -> if (protected) 5 else 38
        HitType.Ranged -> if (protected) 18 else 38
        HitType.Magic -> if (protected) 0 else 21
        HitType.Typeless -> error("Hazards require explicit damage")
    }

    fun cleave(boss: CoordGrid, size: Int, target: CoordGrid): List<CoordGrid> {
        val centre = boss.translate(size / 2, size / 2)
        return if (kotlin.math.abs(target.x - centre.x) >= kotlin.math.abs(target.z - centre.z)) {
            (-1..1).map { target.translate(0, it) }
        } else {
            (-1..1).map { target.translate(it, 0) }
        }
    }
}
