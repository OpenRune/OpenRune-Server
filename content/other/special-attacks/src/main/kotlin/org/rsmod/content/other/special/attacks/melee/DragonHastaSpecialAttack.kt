package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.ShoveStunService
import org.rsmod.api.combat.commons.npc.combatPlayDefendAnim
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.hit.modifier.BypassProtectionPrayerPlayerHitModifier
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.route.StepFactory
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.map.Direction
import org.rsmod.game.map.translate
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Unleash consumes the whole available special bar (at least 5%) and gives 5% accuracy plus
 * 2.5% maximum damage for every full 5% consumed. It always rolls against Stab defence and its
 * player-versus-player hit bypasses Protect from Melee.
 *
 * The rev-240 cache retains the Dragon hasta's historic `brut_dragon_spear` aliases.
 */
class DragonHastaSpecialAttack
@Inject
constructor(
    private val collision: CollisionFlagMap,
    private val stepFactory: StepFactory,
    private val stuns: ShoveStunService,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val unleash = Unleash(manager)
        val shove = Shove(manager, collision, stepFactory, stuns)

        // Shove: all normal, poisoned and Bounty Hunter corrupted Dragon spear variants,
        // plus the cache-native Zamorakian spear and hasta aliases.
        registerMelee("obj.dragon_spear", shove)
        registerMelee("obj.dragon_spear_p", shove)
        registerMelee("obj.dragon_spear_p+", shove)
        registerMelee("obj.dragon_spear_p++", shove)
        registerMelee("obj.bh_dragon_spear_corrupted", shove)
        registerMelee("obj.bh_dragon_spear_p_corrupted", shove)
        registerMelee("obj.bh_dragon_spear_p+_corrupted", shove)
        registerMelee("obj.bh_dragon_spear_p++_corrupted", shove)
        registerMelee("obj.tbwt_dragon_spear_kp", shove)
        registerMelee("obj.zamorak_spear", shove)
        registerMelee("obj.zamorak_hasta", shove)

        registerMelee("obj.brut_dragon_spear", unleash)
        registerMelee("obj.brut_dragon_spear_p", unleash)
        registerMelee("obj.brut_dragon_spear_p+", unleash)
        registerMelee("obj.brut_dragon_spear_p++", unleash)
        registerMelee("obj.brut_dragon_spear_kp", unleash)
    }

    private class Unleash(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = unleash(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = unleash(target, attack)

        private fun ProtectedAccess.unleash(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            val energyUsed = manager.getSpecialEnergy(this)
            if (energyUsed < MINIMUM_ENERGY) {
                mes("You don't have enough power left.")
                return false
            }

            val multipliers = UnleashMultipliers.forEnergy(energyUsed)

            // The cache assigns this weapon a specialized energy requirement, so it owns its
            // complete-bar debit rather than relying on the generic special-attack debit.
            manager.drainAllSpecialEnergy(this)

            // Reuses Sunspear's thrust anim - the generic placeholder didn't sync correctly live.
            anim("seq.human_weapons_sunspear_spec")
            spotanim(
                spot = "spotanim.dragon_hasta_spec_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = multipliers.accuracy,
                    maxHitMultiplier = multipliers.maxHit,
                    blockType = MeleeAttackType.Stab,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(
                source = this,
                target = target,
                damage = damage,
                modifier = BypassProtectionPrayerPlayerHitModifier,
            )
            manager.continueCombat(this, target)
            return true
        }
    }

    /**
     * Shove is guaranteed utility: it never rolls accuracy and deliberately never queues a hit.
     * It stuns for five map cycles (three seconds). Per the wiki's "Stun (status)" page, Shove's
     * stun has no immunity window and can be chained indefinitely. A blocked tile prevents only
     * displacement, never the valid stun.
     */
    private class Shove(
        private val manager: SpecialAttackManager,
        private val collision: CollisionFlagMap,
        private val stepFactory: StepFactory,
        private val stuns: ShoveStunService,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = shove(target)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = shove(target)

        private fun ProtectedAccess.shove(target: PathingEntity): Boolean {
            if (target.size > 1) {
                mes("That creature is too large to knock back!")
                return false
            }
            if (stuns.isStunned(target)) {
                return false
            }

            anim("seq.shove")
            soundSynth(SHOVE_SOUND)
            spotanim(
                spot = "spotanim.sp_attack_shove_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            // Human sequence 1066 is invalid for non-human NPC skeletons.
            // NPC targets use their cache-defined defend animation; PvP players
            // retain the canonical shove-stun reaction sequence.
            when (target) {
                is Npc -> target.combatPlayDefendAnim()
                is Player -> target.anim("seq.stunned_shove")
            }

            target.spotanim(
                spot = "spotanim.stunned_shove",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            stuns.applyStun(target, STUN_CYCLES)
            manager.setNextAttackDelay(this, SHOVE_ATTACK_DELAY)
            tryPush(target)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.tryPush(target: PathingEntity) {
            val direction = Direction.between(player.bounds(), target.bounds())
            val destination = target.coords.translate(direction)
            val validated =
                when (target) {
                    is Player ->
                        stepFactory.validated(
                            source = target.coords,
                            dest = destination,
                            size = target.size,
                            extraFlag = CollisionFlag.BLOCK_PLAYERS,
                        )

                    is Npc -> {
                        val strategy = target.collisionStrategy ?: return
                        stepFactory.validated(target, target.coords, destination, strategy)
                    }
                }
            // [StepFactory] falls back from a blocked diagonal to a cardinal step. Shove must not:
            // it moves the target one tile directly away from the attacker or leaves it in place.
            if (validated != destination) {
                return
            }
            PathingEntityCommon.teleport(target, collision, destination)
        }
    }

    private companion object {
        const val MINIMUM_ENERGY: Int = 50
        const val STUN_CYCLES: Int = 5

        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val SHOVE_SOUND = 2544
        const val SHOVE_ATTACK_DELAY: Int = 5
    }
}

/**
 * Unleash's energy-to-multiplier scaling. Wiki worked example: 100% energy (1000 in this engine's
 * 0-1000 special-energy scale) gives doubled accuracy and a 50% damage boost - `forEnergy(1000)`
 * matches both exactly.
 */
internal object UnleashMultipliers {
    private const val ENERGY_PER_CHUNK: Int = 50
    private const val ACCURACY_PER_CHUNK: Double = 0.05
    private const val DAMAGE_PER_CHUNK: Double = 0.025

    data class Multipliers(val accuracy: Double, val maxHit: Double)

    fun forEnergy(energyUsed: Int): Multipliers {
        val fivePercentChunks = energyUsed / ENERGY_PER_CHUNK
        return Multipliers(
            accuracy = 1.0 + (fivePercentChunks * ACCURACY_PER_CHUNK),
            maxHit = 1.0 + (fivePercentChunks * DAMAGE_PER_CHUNK),
        )
    }
}
