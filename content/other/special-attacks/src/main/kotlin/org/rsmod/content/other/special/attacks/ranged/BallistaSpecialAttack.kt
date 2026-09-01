package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
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

/** Concentrated shot: one ballista javelin with 25% more accuracy and maximum damage. */
class BallistaSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val concentratedShot = Ballista(manager, ammunition, ornamented = false)
        registerRanged("obj.light_ballista", concentratedShot)
        registerRanged("obj.br_light_ballista", concentratedShot)
        registerRanged("obj.heavy_ballista", concentratedShot)
        registerRanged("obj.br_heavy_ballista", concentratedShot)
        registerRanged("obj.heavy_ballista_ornament", Ballista(manager, ammunition, ornamented = true))
    }

    private class Ballista(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val ornamented: Boolean,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = concentratedShot(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = concentratedShot(target, attack)

        private fun ProtectedAccess.concentratedShot(
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

            anim(BallistaAnimation.resolve(ornamented = ornamented, targetIsNpc = target is Npc))
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
            spotanim("spotanim.ballista_special", height = 96, slot = constants.spotanim_slot_combat)

            val launchSpot =
                quiverType.paramOrNull(params.proj_launch)?.let {
                    RSCM.getReverseMapping(RSCMType.SPOTANIM, it.id)
                }
            spotanim(launchSpot, height = 96, slot = constants.spotanim_slot_combat)

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id),
                    RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id),
                )
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = CONCENTRATED_SHOT_MULTIPLIER,
                    maxHitMultiplier = CONCENTRATED_SHOT_MULTIPLIER,
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
        const val CONCENTRATED_SHOT_MULTIPLIER = 1.25
    }
}

/** Pure animation selection, kept separate from [ProtectedAccess] so it can be unit tested. */
internal object BallistaAnimation {
    fun resolve(ornamented: Boolean, targetIsNpc: Boolean): String =
        when {
            ornamented && targetIsNpc -> "seq.ballista02_special_attack_pvn"
            ornamented -> "seq.ballista02_special_attack"
            targetIsNpc -> "seq.ballista_special_attack_pvn"
            else -> "seq.ballista_special_attack"
        }
}
