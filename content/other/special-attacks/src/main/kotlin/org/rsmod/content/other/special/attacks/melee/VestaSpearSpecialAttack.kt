package org.rsmod.content.other.special.attacks.melee

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.config.constants
import org.rsmod.api.player.hit.modifier.VestaSpearCombatImmunity
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Spear Wall grants eight game cycles of melee and ranged immunity and strikes up to sixteen valid
 * targets in an eight-tile square when used in a multi-combat area. Each hit has normal accuracy
 * but is capped at half of the regular melee maximum hit.
 *
 * Both cache variants use the cache-mapped 50% special-attack energy requirement. The Bounty
 * Hunter variant's successful hit uses its one-tick-faster follow-up attack delay.
 */
class VestaSpearSpecialAttack
@Inject
constructor(
    private val targets: AreaMeleeTargetSelector,
    private val pvp: PvPAreaAttackManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val spearWall = SpearWall(manager, targets, pvp)
        registerMelee("obj.vestas_spear", spearWall)
        registerMelee(
            "obj.vestas_spear_bh",
            SpearWall(
                manager = manager,
                targets = targets,
                pvp = pvp,
                successfulNextAttackDelay = BOUNTY_HUNTER_NEXT_ATTACK_DELAY,
            ),
        )
    }

    private class SpearWall(
        private val manager: SpecialAttackManager,
        private val targets: AreaMeleeTargetSelector,
        private val pvp: PvPAreaAttackManager,
        private val successfulNextAttackDelay: Int? = null,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = spearWall(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = spearWall(target, attack)

        private fun ProtectedAccess.spearWall(
            primary: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            // These cache-native effects do not have RSCM aliases in this revision.
            player.anim(RSCM.getReverseMapping(RSCMType.SEQ, VESTA_SPEAR_WALL_ANIMATION))
            // Confirmed against a reference implementation of this exact special (Zenyte-based
            // Offline_Scape/Near Reality, SPEAR_WALL in SpecialAttack.java). Unaliased in this
            // cache's gamevals.
            soundSynth(SPEAR_WALL_SOUND)
            player.spotanim(
                spot = RSCM.getReverseMapping(RSCMType.SPOTANIM, VESTA_SPEAR_WALL_SPOTANIM),
                height = 0,
                slot = constants.spotanim_slot_combat,
            )
            VestaSpearCombatImmunity.activate(player)

            val affected =
                if (mapMultiway()) {
                    targets.select(
                        source = this,
                        primary = primary,
                        tiles = targets.square(player.coords, radius = AREA_RADIUS),
                        npcLimit = MAX_TARGETS,
                        playerLimit = MAX_TARGETS,
                        totalLimit = MAX_TARGETS,
                        searchZoneRadius = SEARCH_ZONE_RADIUS,
                    )
                } else {
                    listOf(primary)
                }

            var totalDamage = 0
            for (target in affected) {
                val damage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = 1.0,
                        maxHitMultiplier = MAX_HIT_MULTIPLIER,
                    )
                totalDamage += damage
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMeleeHit(this, target, damage)

                if (target is Player && target !== primary) {
                    pvp.applySecondarySpecialAttack(this, target)
                }
            }
            if (successfulNextAttackDelay != null && totalDamage > 0) {
                manager.setNextAttackDelay(this, successfulNextAttackDelay)
            }
            manager.continueCombat(this, primary)
            return true
        }
    }

    private companion object {
        private const val VESTA_SPEAR_WALL_ANIMATION: Int = 8184
        private const val VESTA_SPEAR_WALL_SPOTANIM: Int = 1627
        private const val AREA_RADIUS: Int = 8
        private const val SEARCH_ZONE_RADIUS: Int = 2
        private const val MAX_TARGETS: Int = 16
        private const val MAX_HIT_MULTIPLIER: Double = 0.5
        private const val BOUNTY_HUNTER_NEXT_ATTACK_DELAY: Int = 4

        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        private const val SPEAR_WALL_SOUND = 2529
    }
}
