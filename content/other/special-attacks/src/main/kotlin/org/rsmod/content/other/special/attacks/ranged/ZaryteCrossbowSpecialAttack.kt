package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

/**
 * Evoke is an ordinary crossbow shot with double accuracy. When that accuracy roll succeeds, an
 * equipped enchanted bolt's effect is forced for this shot. This deliberately retains the normal
 * quiver, projectile, impact, and ammunition paths instead of replacing a bolt with a generic
 * ranged hit.
 */
class ZaryteCrossbowSpecialAttack
@Inject
constructor(private val ammunition: RangedAmmoManager) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val evoke = Evoke(manager, ammunition)
        registerRanged("obj.zaryte_xbow", evoke)
        registerRanged("obj.br_zaryte_xbow", evoke)
    }

    private class Evoke(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = evoke(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = evoke(target, attack)

        private fun ProtectedAccess.evoke(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            val projectileType = weaponType.paramOrNull(params.proj_type)
            if (quiverType == null || projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }
            anim(if (target is Npc) "seq.zcb_attack_pvn" else "seq.zcb_attack")
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            val landed =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = EVOKE_ACCURACY_MULTIPLIER,
                )
            val bolt =
                ArmadylEnchantedBolt
                    .from(quiverType)
                    ?.takeIf { it.isEffectiveAgainst(target) }
            val damage = if (landed) evokeDamage(target, attack, bolt) else 0
            val projectile =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = "spotanim.zcb_specialattack",
                    projanim = RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id),
                )

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = projectile.serverCycles,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = quiverType,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value for every bolt effect below - no impact callback needed.
            if (landed) {
                bolt?.zaryteApplyEffect(player, target, damage)
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.evokeDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            bolt: ArmadylEnchantedBolt?,
        ): Int {
            if (bolt == ArmadylEnchantedBolt.Ruby) {
                val selfDamage = ZaryteCrossbowSpecialDamage.rubySelfDamage(player.hitpoints)
                if (selfDamage > 0) {
                    // A real hit (not statSub) so a hitsplat actually shows on the shooter. The
                    // blood-sacrifice visual itself is played by zaryteApplyEffect below (falls
                    // through to ArmadylEnchantedBolt.applyEffect), on the target.
                    takeInstantHit(type = HitType.Typeless, damage = selfDamage)
                }
                return ZaryteCrossbowSpecialDamage.rubyDamage(target.zaryteCurrentHitpoints())
            }

            return manager.rollRangedMaxHit(
                source = this,
                target = target,
                attackType = attack.type,
                attackStyle = attack.style,
                multiplier = bolt?.zaryteMaxHitMultiplier ?: 1.0,
                boltSpecDamage = bolt?.zaryteBonusDamage(player, target) ?: 0,
            )
        }
    }

    private companion object {
        const val EVOKE_ACCURACY_MULTIPLIER: Double = 2.0
    }
}

/**
 * Zaryte's passive bonus applies to a forced bolt effect too. The normal Zaryte attack path can
 * continue to resolve its ordinary bolt procs separately; this keeps Evoke's guaranteed effect
 * exact without changing Armadyl Eye's behavior.
 */
private fun ArmadylEnchantedBolt.zaryteBonusDamage(
    source: Player,
    target: PathingEntity,
): Int =
    when (this) {
        ArmadylEnchantedBolt.Opal -> ZaryteCrossbowSpecialDamage.opalBonus(source.rangedLvl)
        ArmadylEnchantedBolt.Pearl ->
            ZaryteCrossbowSpecialDamage.pearlBonus(
                visibleRangedLevel = source.rangedLvl,
                fieryTarget = target.isZaryteFiery(),
            )

        ArmadylEnchantedBolt.Dragonstone -> {
            val base = ZaryteCrossbowSpecialDamage.dragonstoneBonus(source.rangedLvl)
            if (target is Player) {
                DragonfireProtection.resolveMaxHit(
                    player = target,
                    type = DragonfireProtection.DragonfireType.Chromatic,
                    baseMax = base,
                )
            } else {
                base
            }
        }

        else -> 0
    }

private val ArmadylEnchantedBolt.zaryteMaxHitMultiplier: Double
    get() =
        when (this) {
            ArmadylEnchantedBolt.Diamond -> ZaryteCrossbowSpecialDamage.DIAMOND_MAX_HIT_MULTIPLIER
            ArmadylEnchantedBolt.Onyx -> ZaryteCrossbowSpecialDamage.ONYX_MAX_HIT_MULTIPLIER
            else -> 1.0
        }

/**
 * Real OSRS doesn't clamp damage after the roll, so [damage] (the already-known, pre-clamp value)
 * is the authentic figure for every bolt effect below - no impact callback needed. Every enchanted
 * bolt effect has its own dedicated activation spotanim (see [ArmadylEnchantedBolt.spotanim]),
 * played on the target - the branches below that don't override it fall through to
 * [ArmadylEnchantedBolt.applyEffect], which plays it.
 */
private fun ArmadylEnchantedBolt.zaryteApplyEffect(
    source: Player,
    target: PathingEntity,
    damage: Int,
) {
    when (this) {
        ArmadylEnchantedBolt.Sapphire -> {
            val player = target as? Player ?: return
            if (damage <= 0) {
                return
            }
            target.spotanim(spot = spotanim, height = 0)
            val requested = ZaryteCrossbowSpecialDamage.sapphirePrayerDrain(source.rangedLvl)
            val drained = min(requested, player.prayerLvl)
            if (drained <= 0) {
                return
            }
            player.statSub("stat.prayer", constant = drained, percent = 0)
            source.statHeal(
                "stat.prayer",
                constant = ZaryteCrossbowSpecialDamage.sapphirePrayerRestore(drained),
                percent = 0,
            )
        }

        ArmadylEnchantedBolt.Emerald -> {
            val player = target as? Player ?: return
            if (damage > 0) {
                target.spotanim(spot = spotanim, height = 0)
                CombatEffects.poison(player, ZaryteCrossbowSpecialDamage.EMERALD_POISON_DAMAGE)
            }
        }

        else -> applyEffect(source, target, damage)
    }
}

private fun PathingEntity.zaryteCurrentHitpoints(): Int =
    when (this) {
        is Npc -> hitpoints
        is Player -> hitpoints
    }

private fun PathingEntity.isZaryteFiery(): Boolean =
    this is Npc &&
        (
            (visType.paramOrNull(params.draconic) ?: 0) != 0 ||
                (visType.paramOrNull(params.elemental_weakness_type) ?: -1) ==
                    constants.elemental_weakness_water
        )

/** Pure values for Evoke's Zaryte-strengthened enchanted-bolt effects. */
internal object ZaryteCrossbowSpecialDamage {
    const val DIAMOND_MAX_HIT_MULTIPLIER: Double = 1.26
    const val ONYX_MAX_HIT_MULTIPLIER: Double = 1.32
    const val EMERALD_POISON_DAMAGE: Int = 6

    fun rubyDamage(targetHitpoints: Int): Int =
        min(targetHitpoints.coerceAtLeast(0) * 22 / 100, RUBY_DAMAGE_CAP)

    fun rubySelfDamage(sourceHitpoints: Int): Int = sourceHitpoints.coerceAtLeast(0) / 10

    fun opalBonus(visibleRangedLevel: Int): Int = visibleRangedLevel.coerceAtLeast(0) / 9

    fun pearlBonus(
        visibleRangedLevel: Int,
        fieryTarget: Boolean,
    ): Int = visibleRangedLevel.coerceAtLeast(0) / if (fieryTarget) 13 else 18

    fun dragonstoneBonus(visibleRangedLevel: Int): Int =
        visibleRangedLevel.coerceAtLeast(0) * 22 / 100

    fun sapphirePrayerDrain(visibleRangedLevel: Int): Int =
        visibleRangedLevel.coerceAtLeast(0) / 18

    fun sapphirePrayerRestore(drainedPrayer: Int): Int = drainedPrayer.coerceAtLeast(0) / 2

    private const val RUBY_DAMAGE_CAP: Int = 110
}
