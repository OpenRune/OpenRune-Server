package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.hit.modifier.MorriganHamstring
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/**
 * Hamstring differs by cache variant:
 *
 * - Deadman Morrigan's throwing axe: 20-120% damage and a one-tick faster next attack.
 * - Bounty Hunter Morrigan's throwing axe: 50-150% damage with 150% accuracy.
 *
 * A landed hit on a player makes their running energy drain six times faster for one minute.
 */
class MorrigansThrowingAxeSpecialAttack
@Inject
constructor(private val ammunition: RangedAmmoManager) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged(
            "obj.morrigans_thrownaxe",
            Hamstring(
                manager = manager,
                ammunition = ammunition,
                accuracyMultiplier = 1.0,
                minimumDamagePercent = 20,
                maximumDamagePercent = 120,
                speedUpNextAttack = true,
                consumesThrownAmmo = true,
            ),
        )
        registerRanged(
            "obj.morrigans_thrownaxe_bh",
            Hamstring(
                manager = manager,
                ammunition = ammunition,
                accuracyMultiplier = 1.5,
                minimumDamagePercent = 50,
                maximumDamagePercent = 150,
                speedUpNextAttack = false,
                consumesThrownAmmo = false,
            ),
        )
    }

    private class Hamstring(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val accuracyMultiplier: Double,
        private val minimumDamagePercent: Int,
        private val maximumDamagePercent: Int,
        private val speedUpNextAttack: Boolean,
        private val consumesThrownAmmo: Boolean,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = hamstring(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = hamstring(target, attack)

        private fun ProtectedAccess.hamstring(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            if (!ammunition.attemptAmmoUsage(player, weaponType, ammo = null)) {
                manager.stopCombat(this)
                return false
            }

            anim(MORRIGANS_THROWING_AXE_SEQUENCE)
            spotanim(
                spot = MORRIGANS_THROWING_AXE_LAUNCH_SPOTANIM,
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
                slot = constants.spotanim_slot_combat,
            )
            val projectile =
                manager.spawnProjectile(
                    source = this,
                    target = target,
                    spotanim = MORRIGANS_THROWING_AXE_TRAVEL_SPOTANIM,
                    projanim = THROWN_PROJANIM,
                )

            val successful =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = accuracyMultiplier,
                )
            val damage =
                if (successful) {
                    val normalMax =
                        manager.calculateRangedMaxHit(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            multiplier = 1.0,
                            boltSpecDamage = 0,
                        )
                    val range =
                        MorrigansThrowingAxeDamage.range(
                            normalMax = normalMax,
                            minimumPercent = minimumDamagePercent,
                            maximumPercent = maximumDamagePercent,
                        )
                    if (range.first == range.last) range.first else random.of(range)
                } else {
                    0
                }

            // The Deadman weapon's March 2025 change applies to a successful accuracy roll, not
            // merely a positive post-mitigation hit. This must happen before combat is continued.
            if (successful && speedUpNextAttack) {
                manager.setNextAttackDelay(this, weaponType.hamstringNextAttackDelay(attack))
            }

            if (consumesThrownAmmo) {
                ammunition.useThrownWeapon(
                    player = player,
                    weaponType = weaponType,
                    dropCoord = target.coords,
                    dropDelay = projectile.serverCycles,
                )
            }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = null,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // The accuracy roll is already known synchronously (real OSRS doesn't clamp damage
            // after the roll), so this doesn't need to wait for the hit to actually land - no
            // impact callback needed.
            if (successful && target is Player) {
                target.applyMorrigansHamstring()
            }

            if (consumesThrownAmmo && player.righthand == null) {
                mes("That was your last one!")
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }
    }

    private companion object {
        const val MORRIGANS_THROWING_AXE_SEQUENCE = "seq.weapon_morrigans_throwingaxe_special01"
        const val MORRIGANS_THROWING_AXE_LAUNCH_SPOTANIM = "spotanim.morrigans_taxe_spotanim"
        const val MORRIGANS_THROWING_AXE_TRAVEL_SPOTANIM = "spotanim.morrigans_taxe_projanim"
        const val THROWN_PROJANIM = "projanim.thrown"
    }
}

internal object MorrigansThrowingAxeDamage {
    fun range(
        normalMax: Int,
        minimumPercent: Int,
        maximumPercent: Int,
    ): IntRange {
        require(minimumPercent in 0..maximumPercent)
        if (normalMax <= 0) {
            return 0..0
        }
        val minimum = (normalMax.toLong() * minimumPercent / 100L).toInt()
        val maximum = (normalMax.toLong() * maximumPercent / 100L).toInt()
        return minimum..maximum.coerceAtLeast(minimum)
    }
}

private fun dev.openrune.types.ItemServerType.hamstringNextAttackDelay(
    attack: CombatAttack.Ranged,
): Int {
    val baseAttackRate = param(params.attackrate).coerceAtLeast(1)
    val actualAttackRate =
        if (attack.style == RangedAttackStyle.Rapid) {
            (baseAttackRate - 1).coerceAtLeast(1)
        } else {
            baseAttackRate
        }
    return (actualAttackRate - 1).coerceAtLeast(1)
}

private fun Player.applyMorrigansHamstring() {
    MorriganHamstring.activate(this)
    mes("You've been hamstrung! For the next minute, your run energy will drain 6x faster.")
}
