package org.rsmod.content.other.special.weapons.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.ranged.RangedAmmunition
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.NpcPoisonEffectService
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.hat
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ranged.BlowpipeAmmo
import org.rsmod.api.player.righthand
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/**
 * Toxic/Rosewood blowpipe normal attacks. Both store their own darts (and, for the toxic family,
 * Zulrah's scales) packed inside the weapon item itself via [BlowpipeAmmo] rather than in the
 * quiver, so the generic cache-driven ranged fallback (which only knows about quiver ammo) can
 * never fire them - this is the piece that was missing, not anything about the special attacks
 * themselves (those were already correctly written, just blocked on [BlowpipeAmmo] not existing).
 *
 * Wiki-verified: Toxic blowpipe attacks have a 25% chance to envenom (100% against an NPC target
 * while wearing a serpentine helm); firing has a 1/3 chance to *not* consume a scale. Rosewood
 * blowpipe has neither scales nor any poison/venom chance.
 */
class BlowpipeWeapons
@Inject
constructor(
    private val random: GameRandom,
    private val poisons: NpcPoisonEffectService,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val toxic = ToxicBlowpipe(manager, random, poisons)
        register("obj.toxic_blowpipe_loaded", toxic)
        register("obj.toxic_blowpipe_loaded_ornament", toxic)

        val rosewood = RosewoodBlowpipe(manager, random)
        register("obj.rosewood_blowpipe", rosewood)
    }

    private class ToxicBlowpipe(
        private val manager: WeaponAttackManager,
        private val random: GameRandom,
        private val poisons: NpcPoisonEffectService,
    ) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = fire(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = fire(target, attack)

        private fun ProtectedAccess.fire(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val equipped = player.righthand
            val dart = BlowpipeAmmo.loadedDart(equipped)
            if (dart == null || !BlowpipeAmmo.canUseLoadedDart(equipped)) {
                manager.stopCombat(this)
                mes("Your toxic blowpipe has no usable darts loaded.")
                return true
            }
            if (!BlowpipeAmmo.hasScales(equipped, 1)) {
                manager.stopCombat(this)
                mes("Your toxic blowpipe has run out of scales.")
                return true
            }

            val travelSpotanim = dart.type.paramOrNull(params.proj_travel)
            if (travelSpotanim == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return true
            }

            val consumeDart = !RangedAmmunition.conserveAmmo(player, random)
            val consumeScale = !random.randomBoolean(SCALE_CONSERVE_ROLL)

            manager.playWeaponFx(this, attack)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id),
                    weaponType.projectileType(),
                )
            val (serverDelay, clientDelay) = projectile.durations

            val damage = manager.rollRangedDamage(this, target, attack)
            val guaranteedNpcVenom = target is Npc && EquipmentChecks.isSerpentineHelm(player.hat)
            val venom =
                damage > 0 && (guaranteedNpcVenom || random.randomBoolean(VENOM_ROLL_DENOMINATOR))

            val consumption =
                BlowpipeAmmo.consume(
                    player = player,
                    darts = if (consumeDart) 1 else 0,
                    scales = if (consumeScale) 1 else 0,
                )
            if (consumption == null) {
                manager.stopCombat(this)
                return true
            }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, dart.type, damage, clientDelay, serverDelay)
            if (venom) {
                when (target) {
                    is Npc -> poisons.applyVenom(player, target)
                    is Player -> PlayerVenom.tryVenom(target)
                }
            }

            if (!consumption.canFire) {
                if (consumption.dartsLeft == 0) {
                    mes("Your toxic blowpipe has run out of darts.")
                } else {
                    mes("Your toxic blowpipe has run out of scales.")
                }
                manager.stopCombat(this)
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }
    }

    private class RosewoodBlowpipe(
        private val manager: WeaponAttackManager,
        private val random: GameRandom,
    ) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = fire(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = fire(target, attack)

        private fun ProtectedAccess.fire(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val equipped = player.righthand
            val dart = BlowpipeAmmo.loadedDart(equipped)
            if (dart == null || !BlowpipeAmmo.canUseLoadedDart(equipped)) {
                manager.stopCombat(this)
                mes("Your rosewood blowpipe has no usable darts loaded.")
                return true
            }

            val travelSpotanim = dart.type.paramOrNull(params.proj_travel)
            if (travelSpotanim == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return true
            }

            val consumeDart = !RangedAmmunition.conserveAmmo(player, random)

            manager.playWeaponFx(this, attack)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id),
                    weaponType.projectileType(),
                )
            val (serverDelay, clientDelay) = projectile.durations

            val damage = manager.rollRangedDamage(this, target, attack)

            val consumption = BlowpipeAmmo.consume(player, darts = if (consumeDart) 1 else 0)
            if (consumption == null) {
                manager.stopCombat(this)
                return true
            }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, dart.type, damage, clientDelay, serverDelay)

            if (consumption.becameEmpty) {
                mes("Your rosewood blowpipe has run out of darts.")
                manager.stopCombat(this)
            } else {
                manager.continueCombat(this, target)
            }
            return true
        }
    }

    private companion object {
        const val SCALE_CONSERVE_ROLL: Int = 3
        const val VENOM_ROLL_DENOMINATOR: Int = 4
    }
}

private fun ItemServerType.projectileType(): String =
    paramOrNull(params.proj_type)?.let { RSCM.getReverseMapping(RSCMType.PROJANIM, it.id) }
        ?: "projanim.thrown"
