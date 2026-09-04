package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.NextCycleRangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/** Momentum Throw: one normal-damage thrown axe with 25% accuracy, dispatched next cycle. */
class DragonThrownaxeSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val momentumThrow = MomentumThrow(manager, ammunition)
        registerRanged("obj.dragon_thrownaxe", momentumThrow)
        registerRanged("obj.br_dragon_thrownaxe", momentumThrow)
    }

    private class MomentumThrow(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
    ) : NextCycleRangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = momentumThrow(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = momentumThrow(target, attack)

        private fun ProtectedAccess.momentumThrow(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            if (!ammunition.attemptAmmoUsage(player, weaponType, ammo = null)) {
                manager.stopCombat(this)
                return false
            }

            val travelSpotanim = SPECIAL_TRAVEL_SPOTANIM
            val projectileType =
                weaponType.paramOrNull(params.proj_type)?.let {
                    RSCM.getReverseMapping(RSCMType.PROJANIM, it.id)
                } ?: DEFAULT_PROJANIM
            anim(SPECIAL_ANIM)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
            spotanim(SPECIAL_LAUNCH_SPOTANIM, height = 96, slot = constants.spotanim_slot_combat)

            val projectile = manager.spawnProjectile(this, target, travelSpotanim, projectileType)
            val damage =
                manager.rollRangedDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = MOMENTUM_THROW_ACCURACY,
                )

            ammunition.useThrownWeapon(
                player = player,
                weaponType = weaponType,
                dropCoord = target.coords,
                dropDelay = projectile.serverCycles,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = null,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )

            if (player.righthand == null) {
                mes("That was your last one!")
                return true
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val MOMENTUM_THROW_ACCURACY = 1.25
        const val SPECIAL_ANIM = "seq.human_dragon_taxe_spec"
        const val SPECIAL_TRAVEL_SPOTANIM = "spotanim.dragon_taxe_travel_spec"
        const val SPECIAL_LAUNCH_SPOTANIM = "spotanim.dragon_taxe_launch_spec"
        const val DEFAULT_PROJANIM = "projanim.thrown"
    }
}
