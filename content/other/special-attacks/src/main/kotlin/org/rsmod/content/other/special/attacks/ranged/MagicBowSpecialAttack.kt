package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

/** Powershot: a normal-damage arrow that bypasses the ranged accuracy roll. */
class MagicBowSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val powershot = MagicBow(manager, ammunition)
        registerRanged("obj.magic_longbow", powershot)
        registerRanged("obj.trail_composite_bow_magic", powershot)
    }

    private class MagicBow(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = powershot(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = powershot(target, attack)

        private fun ProtectedAccess.powershot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            val projectileType = weaponType.paramOrNull(params.proj_type)
            if (travelSpotanim == null || projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }
            if (!playRangedWeaponFx(weaponType)) {
                manager.stopCombat(this)
                mes("The bow fails to fire.")
                return false
            }

            // `spotanim.sp_attack_glow_arrow_launch` (250) is Powershot's own graphic - every other
            // id in this project's spec-graphic block (246-258) is already claimed by a different
            // special (Snapshot's 256/249, Rune thrownaxe's 257/258, Dragon dagger/mace/hasta's
            // 252/251/253/254, etc.), leaving 250 unused by elimination. Overrides the ammo's plain
            // proj_launch/proj_travel entirely, same pattern as Snapshot's fix - live testing
            // confirmed the ammo's default colours showed instead of the green Powershot glow.
            spotanim(POWERSHOT_GLOW_SPOTANIM, height = 96, slot = constants.spotanim_slot_combat)

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    POWERSHOT_GLOW_SPOTANIM,
                    RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id),
                )

            // Powershot deliberately skips rollRangedDamage (no accuracy roll needed - guaranteed
            // to hit) and also can't use rollRangedMaxHit/calculateRangedMaxHit: those apply the
            // standard formula (gear ranged strength, prayer/void boosts, Slayer helmet(i) etc.),
            // but the wiki documents Powershot as a custom, stripped-down formula that ignores all
            // of that - only visible Ranged level and the ammo's own ranged strength matter. Same
            // formula and constants as Magic shortbow's Snapshot special in this same package.
            val maxHit =
                MagicBowDamage.maxHit(
                    rangedLevel = stat("stat.ranged"),
                    ammoRangedStrength = quiverType.param(params.ranged_strength),
                )
            val damage =
                MagicBowDamage.resolveDamage(
                    maxHit = maxHit,
                    isMaxHit = player.adminMaxHit,
                    rollInclusive = { range -> random.of(range) },
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
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val POWERSHOT_GLOW_SPOTANIM = "spotanim.sp_attack_glow_arrow_launch"
    }
}

/**
 * Pure Powershot max-hit math, kept separate from [ProtectedAccess] so the formula can be unit
 * tested directly against the wiki's stated formula instead of only through a live combat roll.
 */
internal object MagicBowDamage {
    /**
     * Wiki: `Maximum Hit = floor(0.5 + (Visible Ranged Level + 10) * (Ammo Ranged Strength + 64)
     * / 640)`. `(n + 320) / 640` under integer division is exactly that `floor(0.5 + n/640)` -
     * 320/640 is 0.5, so adding it before truncating is the standard round-to-nearest trick.
     */
    fun maxHit(rangedLevel: Int, ammoRangedStrength: Int): Int {
        val effectiveRanged = rangedLevel + POWERSHOT_LEVEL_BONUS
        return (effectiveRanged * (ammoRangedStrength + RANGED_STRENGTH_BASE) + ROUNDING) / DIVISOR
    }

    fun resolveDamage(maxHit: Int, isMaxHit: Boolean, rollInclusive: (IntRange) -> Int): Int =
        if (isMaxHit) maxHit else rollInclusive(0..maxHit)

    const val POWERSHOT_LEVEL_BONUS = 10
    const val RANGED_STRENGTH_BASE = 64
    const val ROUNDING = 320
    const val DIVISOR = 640
}
