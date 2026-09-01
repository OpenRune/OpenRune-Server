package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.content.other.special.attacks.melee.AreaMeleeTargetSelector
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getOrNull

/**
 * Annihilate fires one bolt at the selected target. In a multi-combat area, that one successful
 * roll also damages every eligible entity in the target's 3x3 square: +20% to the primary and
 * -20% to all secondary targets. Enchanted-bolt effects intentionally do not occur.
 */
class DragonCrossbowSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val targets: AreaMeleeTargetSelector,
    private val pvp: PvPAreaAttackManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val annihilate = Annihilate(manager, ammunition, targets, pvp)
        registerRanged("obj.xbows_crossbow_dragon", annihilate)
        registerRanged("obj.bh_xbows_crossbow_dragon_corrupted", annihilate)
        registerRanged("obj.br_xbows_crossbow_dragon", annihilate)
    }

    private class Annihilate(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val targets: AreaMeleeTargetSelector,
        private val pvp: PvPAreaAttackManager,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = annihilate(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = annihilate(target, attack)

        private fun ProtectedAccess.annihilate(
            primary: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType =
                requireNotNull(
                    ServerCacheManager.getItem(
                        "obj.xbows_crossbow_dragon".asRSCM(RSCMType.OBJ)
                    )
                )
            val quiverType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            val projectileType = weaponType.paramOrNull(params.proj_type)
            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            if (quiverType == null || projectileType == null || travelSpotanim == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }
            if (!playRangedWeaponFx(weaponType)) {
                manager.stopCombat(this)
                mes("The crossbow fails to fire.")
                return false
            }

            val launchSpotanim =
                quiverType.paramOrNull(params.proj_launch)?.let {
                    RSCM.getReverseMapping(RSCMType.SPOTANIM, it.id)
                }
            spotanim(
                launchSpotanim,
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val affected =
                if (mapMultiway()) {
                    targets.select(
                        source = this,
                        primary = primary,
                        tiles = targets.square(primary.coords, radius = 1),
                        npcLimit = MAX_TARGETS,
                        playerLimit = MAX_TARGETS,
                        totalLimit = MAX_TARGETS,
                    )
                } else {
                    listOf(primary)
                }

            // Annihilate performs one ordinary accuracy/damage roll against the selected target.
            // The resulting raw hit is shared by the area rather than re-rolled per victim.
            val rawDamage =
                manager.rollRangedDamage(
                    source = this,
                    target = primary,
                    attack = attack,
                )
            val projectileSpotanim =
                RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id)
            val projectileTypeName =
                RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id)
            val projectiles =
                affected.associateWith { target ->
                    manager.spawnProjectile(
                        this,
                        target,
                        projectileSpotanim,
                        projectileTypeName,
                    )
                }

            val primaryProjectile = requireNotNull(projectiles[primary])
            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = primary.coords,
                dropDelay = primaryProjectile.serverCycles,
            )

            for (target in affected) {
                val damage =
                    if (target === primary) {
                        DragonCrossbowSpecialDamage.primary(rawDamage)
                    } else {
                        DragonCrossbowSpecialDamage.secondary(rawDamage)
                    }
                val projectile = requireNotNull(projectiles[target])
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueRangedHit(
                    source = this,
                    target = target,
                    ammo = quiverType,
                    damage = damage,
                    clientDelay = projectile.clientCycles,
                    hitDelay = projectile.serverCycles,
                )
                if (target is Player && target !== primary) {
                    pvp.applySecondarySpecialAttack(this, target)
                }
            }
            manager.continueCombat(this, primary)
            return true
        }
    }

    private companion object {
        const val MAX_TARGETS: Int = 10
    }
}

/** Integer-floor damage scaling for Annihilate's primary and surrounding targets. */
internal object DragonCrossbowSpecialDamage {
    fun primary(rawDamage: Int): Int = rawDamage.coerceAtLeast(0) * 6 / 5

    fun secondary(rawDamage: Int): Int = rawDamage.coerceAtLeast(0) * 4 / 5
}
