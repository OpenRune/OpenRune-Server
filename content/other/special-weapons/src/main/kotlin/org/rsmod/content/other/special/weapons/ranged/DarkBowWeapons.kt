package org.rsmod.content.other.special.weapons.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

class DarkBowWeapons @Inject constructor(private val ammunition: RangedAmmoManager) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register("obj.darkbow", DarkBow(manager, ammunition))
        register("obj.darkbow_green", DarkBow(manager, ammunition))
        register("obj.darkbow_blue", DarkBow(manager, ammunition))
        register("obj.darkbow_yellow", DarkBow(manager, ammunition))
        register("obj.darkbow_white", DarkBow(manager, ammunition))
        register("obj.bh_darkbow_imbue", DarkBow(manager, ammunition))
    }

    private class DarkBow(
        private val manager: WeaponAttackManager,
        private val ammunition: RangedAmmoManager,
    ) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        private fun ProtectedAccess.shoot(target: PathingEntity, attack: CombatAttack.Ranged) {
            val righthandType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)

            val canUseAmmo = ammunition.attemptAmmoUsage(player, righthandType, quiverType)
            if (!canUseAmmo) {
                manager.stopCombat(this)
                return
            }

            // All valid ammunition requires a `proj_travel` param to build the projectiles.
            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            if (travelSpotanim == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return
            }

            val launchSpotanim = quiverType.paramOrNull(params.proj_launch)
            val quiverCount = player.quiver?.count ?: 0

            if (quiverCount == 1) {
                shootSingleArrow(target, attack, quiverType, RSCM.getReverseMapping(RSCMType.SPOTANIM,launchSpotanim!!.id), RSCM.getReverseMapping(RSCMType.SPOTANIM,travelSpotanim.id))
                manager.continueCombat(this, target)
                return
            }

            if (quiverCount >= 2) {
                val doubleLaunchSpotanim =
                    quiverType.paramOrNull(params.proj_launch_double) ?: launchSpotanim
                shootDoubleArrow(target, attack, quiverType, RSCM.getReverseMapping(RSCMType.SPOTANIM,doubleLaunchSpotanim!!.id), RSCM.getReverseMapping(RSCMType.SPOTANIM,travelSpotanim.id))
                manager.continueCombat(this, target)
                return
            }
        }

        private fun ProtectedAccess.shootSingleArrow(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType,
            launchSpot: String?,
            travelSpot: String,
        ) {
            anim("seq.human_bow")
            soundSynth("synth.darkbow_fire")
            spotanim(launchSpot, height = 96, slot = constants.spotanim_slot_combat)

            val projanim = manager.spawnProjectile(this, target, travelSpot, "projanim.arrow")
            val (serverDelay, clientDelay) = projanim.durations

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = projanim.serverCycles,
            )

            val damage = manager.rollRangedDamage(this, target, attack)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, quiverType, damage, clientDelay, serverDelay)
        }

        private fun ProtectedAccess.shootDoubleArrow(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType,
            launchSpot: String?,
            travelSpot: String,
        ) {
            anim("seq.human_bow")
            soundSynth("synth.darkbow_doublefire")
            spotanim(launchSpot, height = 96, slot = constants.spotanim_slot_combat)

            val proj1 = manager.spawnProjectile(this, target, travelSpot,
                "projanim.doublearrow_one"
            )
            val proj2 = manager.spawnProjectile(this, target, travelSpot,
                "projanim.doublearrow_two"
            )
            val hitDelay1 = proj1.serverCycles
            val hitDelay2 = proj2.serverCycles

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay1,
            )

            val damage1 = manager.rollRangedDamage(this, target, attack)
            val damage2 = manager.rollRangedDamage(this, target, attack)
            val totalDamage = damage1 + damage2

            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.queueRangedHit(this, target, quiverType, damage1, proj2.clientCycles, hitDelay1)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay2,
            )

            manager.queueRangedDamage(this, target, quiverType, damage2, hitDelay2)

            if (player.quiver?.count == 1) {
                mes("You now have only 1 arrow left in your quiver.")
            }
        }
    }
}
