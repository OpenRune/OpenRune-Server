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
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

/**
 * Snipe: a normal bone-bolt shot that cannot miss if this player was not the target's most recent
 * source of positive final damage. A landed shot drains Defence by the final damage dealt, only
 * while the target's Defence has not already been reduced below its base level.
 */
class DorgeshuunCrossbowSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged("obj.dttd_bone_crossbow", Snipe(manager, ammunition))
    }

    private class Snipe(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = snipe(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = snipe(target, attack)

        private fun ProtectedAccess.snipe(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val quiverType =
                getOrNull(player.quiver)
                    ?: run {
                        manager.stopCombat(this)
                        mes("There is no ammo left in your quiver.")
                        return false
                    }
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            val projectileType = weaponType.paramOrNull(params.proj_type)
            if (projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }

            when (target) {
                is Npc -> anim("seq.dttd_player_fire_bone_crossbow_pvn")
                is Player -> anim("seq.dttd_player_fire_bone_crossbow")
            }
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
            spotanim(
                "spotanim.bone_crossbow_launch",
                height = 0,
                slot = constants.spotanim_slot_combat,
            )

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    SNIPE_TRAVEL_SPOTANIM,
                    RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id),
                )
            val successful = target.isUnsuspecting(player)
                || manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            val damage =
                if (successful) {
                    manager.rollRangedMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.0,
                        boltSpecDamage = 0,
                    )
                } else {
                    0
                }

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
            // Real OSRS doesn't clamp damage after the roll, so the already-known pre-clamp
            // damage is the authentic value for the Defence drain - no impact callback needed.
            if (damage > 0) {
                reduceDefence(target, damage)
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun PathingEntity.isUnsuspecting(source: Player): Boolean {
            val sourceUuid = source.uuid
            return sourceUuid == null || lastDamagingPlayerUuid != sourceUuid
        }
    }

    private companion object {
        const val SNIPE_TRAVEL_SPOTANIM = "spotanim.dttd_bone_crossbowbolt_travel_sp_attack"
    }
}

private fun reduceDefence(target: PathingEntity, damage: Int) {
    when (target) {
        is Player -> {
            if (target.stat("stat.defence") >= target.statBase("stat.defence")) {
                target.statSub("stat.defence", constant = damage, percent = 0)
            }
        }
        is Npc -> {
            if (target.defenceLvl >= target.baseDefenceLvl) {
                target.defenceLvl = (target.defenceLvl - damage).coerceAtLeast(0)
            }
        }
    }
}
