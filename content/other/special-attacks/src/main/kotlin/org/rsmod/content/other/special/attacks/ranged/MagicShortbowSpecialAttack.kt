package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
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

/**
 * Snapshot fires two independent arrows. Its hit rolls use the cache-backed projectile flow, but
 * retain the unusual Old School max-hit calculation: visible Ranged level plus ten and the
 * ammunition's ranged-strength value only. Gear strength, Void, prayers, Slayer, and Salve do
 * not enter the damage calculation; the normal ranged accuracy calculation still does.
 */
class MagicShortbowSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val snapshot = Snapshot(manager, ammunition)
        registerRanged("obj.magic_shortbow", snapshot)
        registerRanged("obj.magic_shortbow_i", snapshot)
        registerRanged("obj.br_magic_bow", snapshot)
    }

    private class Snapshot(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = snapshot(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = snapshot(target, attack)

        private fun ProtectedAccess.snapshot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            if ((player.quiver?.count ?: 0) < ARROWS_PER_SNAPSHOT) {
                manager.stopCombat(this)
                mes("You need to have at least 2 arrows in your quiver for this special attack.")
                return false
            }

            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            val projectileType = weaponType.paramOrNull(params.proj_type)
            if (travelSpotanim == null || projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }

            anim("seq.snapshot")
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
            target.spotanim(
                "spotanim.sp_attack_snapshot_spotanim",
                height = 96,
                slot = constants.spotanim_slot_combat,
            )
            // Matches PvNCombat's own standard ranged-attack handling: the launch spotanim plays
            // on the shooter (a muzzle-flash-style effect), not the target, and is genuinely
            // absent for some ammo (no proj_launch param) - null is a real, expected case here.
            val launchSpotanim =
                quiverType.paramOrNull(params.proj_launch_double)
                    ?: quiverType.paramOrNull(params.proj_launch)
            val launchSpotanimName =
                launchSpotanim?.let { RSCM.getReverseMapping(RSCMType.SPOTANIM, it.id) }
            spotanim(launchSpotanimName, height = 96, slot = constants.spotanim_slot_combat)

            val travelSpot = RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id)
            val projanim = RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id)
            val firstProjectile = manager.spawnProjectile(this, target, travelSpot, projanim)
            val secondProjectile = manager.spawnProjectile(this, target, travelSpot, projanim)
            val firstDamage = snapshotDamage(target, attack, quiverType)
            val secondDamage = snapshotDamage(target, attack, quiverType)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = firstProjectile.serverCycles,
            )
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = quiverType,
                damage = firstDamage,
                clientDelay = firstProjectile.clientCycles,
                hitDelay = firstProjectile.serverCycles,
            )

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = secondProjectile.serverCycles,
            )
            manager.queueRangedDamage(
                source = this,
                target = target,
                ammo = quiverType,
                damage = secondDamage,
                hitDelay = secondProjectile.serverCycles,
            )
            manager.giveCombatXp(this, target, attack, firstDamage + secondDamage)

            if (player.quiver?.count == 1) {
                mes("You now have only 1 arrow left in your quiver.")
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.snapshotDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            ammo: ItemServerType,
        ): Int {
            val successful =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = SNAPSHOT_ACCURACY_MULTIPLIER,
                )
            if (!successful) {
                return 0
            }

            val maxHit = snapshotMaxHit(ammo)
            return if (player.adminMaxHit) maxHit else random.of(0..maxHit)
        }

        private fun ProtectedAccess.snapshotMaxHit(ammo: ItemServerType): Int {
            val effectiveRanged = stat("stat.ranged") + SNAPSHOT_LEVEL_BONUS
            val rangedStrength = ammo.param(params.ranged_strength)
            return (effectiveRanged * (rangedStrength + RANGED_STRENGTH_BASE) + ROUNDING) / DIVISOR
        }
    }

    private companion object {
        const val ARROWS_PER_SNAPSHOT = 2
        const val SNAPSHOT_LEVEL_BONUS = 10
        const val RANGED_STRENGTH_BASE = 64
        const val ROUNDING = 320
        const val DIVISOR = 640
        const val SNAPSHOT_ACCURACY_MULTIPLIER = 10.0 / 7.0
    }
}
