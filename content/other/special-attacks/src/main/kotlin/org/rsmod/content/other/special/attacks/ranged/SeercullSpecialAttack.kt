package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.stat.baseMagicLvl
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
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
 * Soulshot always lands, but its damage uses only the visible Ranged level and arrow Ranged
 * Strength. Equipment strength, attack style, prayers, Void, Salve, and Slayer bonuses do not
 * contribute to this special's max hit.
 */
class SeercullSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val random: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        // Seercull (cache ID 6724) is absent from the rev-240 obj gameval table.
        registerRanged(SEERCULL_ID, SOULSHOT_ENERGY, Soulshot(manager, ammunition, random))
    }

    private class Soulshot(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val random: GameRandom,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = soulshot(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = soulshot(target, attack)

        private fun ProtectedAccess.soulshot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val arrowType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, arrowType)) {
                manager.stopCombat(this)
                return false
            }

            val travelSpotanim = arrowType?.paramOrNull(params.proj_travel)
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

            // Soulshot's own dedicated effects, found via live in-game id testing (external wiki/
            // RuneLite search found nothing - 472 turned out to be this project's own custom
            // addition, not from the real game). Overrides the ammo's plain proj_launch/proj_travel
            // entirely, same pattern as every other special in this file. Launch glow is this
            // project's own custom white variant of the shared "sp_attack_glow_arrow_launch" family
            // (Powershot's plain one is 250); travel and hit are the real, official Dagannoth
            // Supreme arrow-shower graphics, reused here since Seercull drops from it.
            spotanim(SOULSHOT_LAUNCH_SPOTANIM, height = 96, slot = constants.spotanim_slot_combat)

            val projectile =
                manager.spawnProjectile(
                    this,
                    target,
                    SOULSHOT_TRAVEL_SPOTANIM,
                    RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id),
                )
            target.spotanim(SOULSHOT_HIT_SPOTANIM, height = 0, delay = projectile.clientCycles)

            // Do not use rollRangedDamage/max-hit: those include attack style, prayers, Void,
            // worn strength, and conditional bonuses that Soulshot deliberately ignores.
            val damage = random.of(0..SoulshotDamage.maxHit(player.rangedLvl, arrowType))

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = arrowType,
                dropCoord = target.coords,
                dropDelay = projectile.serverCycles,
            )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = arrowType,
                damage = damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value to gate the Magic drain on - no impact callback needed.
            if (damage > 0) {
                drainSoulshotMagic(target, damage)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val SEERCULL_ID = 6724
        const val SOULSHOT_ENERGY = 1000
        const val SOULSHOT_LAUNCH_SPOTANIM = "spotanim.sp_attack_glow_arrow_launch_white"
        const val SOULSHOT_TRAVEL_SPOTANIM = "spotanim.dagannoth_arrow_spotanim_travel"
        const val SOULSHOT_HIT_SPOTANIM = "spotanim.dagannoth_arrow_spotanim_hit"
    }
}

/** Soulshot's cache-independent max-hit formula: floor(0.5 + ((Ranged + 10) * (Ammo + 64)) / 640). */
internal object SoulshotDamage {
    fun maxHit(visibleRangedLevel: Int, arrow: ItemServerType): Int =
        maxHit(visibleRangedLevel, arrow.param(params.ranged_strength))

    fun maxHit(visibleRangedLevel: Int, arrowRangedStrength: Int): Int =
        ((visibleRangedLevel + 10) * (arrowRangedStrength + 64) + 320) / 640
}

private fun drainSoulshotMagic(target: PathingEntity, damage: Int) {
    when (target) {
        is Player -> {
            if (target.magicLvl >= target.baseMagicLvl) {
                target.statSub("stat.magic", constant = damage, percent = 0)
            }
        }
        is Npc -> {
            if (target.magicLvl >= target.baseMagicLvl) {
                target.magicLvl = (target.magicLvl - damage).coerceAtLeast(0)
            }
        }
    }
}
