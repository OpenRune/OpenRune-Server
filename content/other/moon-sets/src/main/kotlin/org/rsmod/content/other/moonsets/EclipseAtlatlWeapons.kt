package org.rsmod.content.other.moonsets

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.burn.Burn
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.worn.MoonSet
import org.rsmod.api.random.GameRandom
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

class EclipseAtlatlWeapons
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val burn: Burn,
    private val random: GameRandom,
    private val worldQueues: WorldQueueList,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val atlatl = EclipseAtlatl(manager, ammunition, burn, random, worldQueues)
        register("obj.eclipse_atlatl", atlatl)
        register("obj.br_eclipse_atlatl", atlatl)
    }

    private class EclipseAtlatl(
        private val manager: WeaponAttackManager,
        private val ammunition: RangedAmmoManager,
        private val burn: Burn,
        private val random: GameRandom,
        private val worldQueues: WorldQueueList,
    ) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            throwDart(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            throwDart(target, attack)
            return true
        }

        private fun ProtectedAccess.throwDart(target: PathingEntity, attack: CombatAttack.Ranged) {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)

            if (quiverType == null || !ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return
            }

            val travel = quiverType.paramOrNull(params.proj_travel)
            if (travel == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return
            }

            manager.playWeaponFx(this, attack)

            val travelName = RSCM.getReverseMapping(RSCMType.SPOTANIM, travel.id)
            val projectile = manager.spawnProjectile(this, target, travelName, "projanim.arrow")
            val (serverDelay, clientDelay) = projectile.durations
            ammunition.useQuiverAmmo(player, quiverType, target.coords, dropDelay = serverDelay)

            val accurate =
                manager.rollRangedAccuracy(this, target, attack.type, attack.style, attack.type, 1.0)
            val damage =
                if (accurate) {
                    manager.rollRangedMaxHit(this, target, attack.type, attack.style, 1.0, 0)
                } else {
                    0
                }
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, quiverType, damage, clientDelay, serverDelay)

            if (accurate && MoonSet.Eclipse.wearsSetEffect(player) && random.of(100) < BURN_CHANCE) {
                spotanim(PROC_SPOTANIM, slot = constants.spotanim_slot_combat)
                val stillValid = target.validityCheck()
                worldQueues.add(serverDelay) {
                    if (stillValid()) {
                        burn.inflict(target)
                    }
                }
            }

            manager.continueCombat(this, target)
        }
    }

    private companion object {
        const val BURN_CHANCE = 20
        const val PROC_SPOTANIM = "spotanim.special_atlatl_spotanim"
    }
}
