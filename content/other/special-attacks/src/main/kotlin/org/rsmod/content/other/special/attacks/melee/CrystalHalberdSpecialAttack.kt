package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Sweep: 110% maximum damage against Slash defence. Large NPCs receive a second independent hit
 * with 75% accuracy; otherwise, multi-combat sweeps the three-tile-wide line through the target.
 */
class CrystalHalberdSpecialAttack
@Inject
constructor(
    private val targets: AreaMeleeTargetSelector,
    private val pvp: PvPAreaAttackManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val sweep = Sweep(manager, targets, pvp)
        registerMelee("obj.crystal_halberd", sweep)
        registerMelee("obj.crystal_halberd_2500", sweep)
    }

    private class Sweep(
        private val manager: SpecialAttackManager,
        private val targets: AreaMeleeTargetSelector,
        private val pvp: PvPAreaAttackManager,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = sweep(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = sweep(target, attack)

        private fun ProtectedAccess.sweep(
            primary: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim("seq.dragon_halberd_special_attack")
            spotanim(HalberdSpecialVisuals.forTarget(player.coords, primary.coords))

            val affected =
                if (mapMultiway() && primary.size == 1) {
                    targets.select(
                        source = this,
                        primary = primary,
                        tiles = targets.halberdSweep(player, primary),
                        npcLimit = MAX_NPC_TARGETS,
                        playerLimit = MAX_PLAYER_TARGETS,
                    )
                } else {
                    listOf(primary)
                }

            for (target in affected) {
                val hitCount = if (target === primary && target is Npc && target.size > 1) 2 else 1
                var totalDamage = 0
                repeat(hitCount) { hit ->
                    val damage =
                        manager.rollMeleeDamage(
                            source = this,
                            target = target,
                            attack = attack,
                            accuracyMultiplier = if (hit == 0) 1.0 else SECOND_HIT_ACCURACY,
                            maxHitMultiplier = MAX_HIT_MULTIPLIER,
                            blockType = MeleeAttackType.Slash,
                        )
                    totalDamage += damage
                    manager.queueMeleeHit(this, target, damage)
                }
                manager.giveCombatXp(this, target, attack, totalDamage)

                if (target is Player && target !== primary) {
                    pvp.applySecondarySpecialAttack(this, target)
                }
            }
            manager.continueCombat(this, primary)
            return true
        }

        private companion object {
            private const val MAX_NPC_TARGETS: Int = 10
            private const val MAX_PLAYER_TARGETS: Int = 3
            private const val MAX_HIT_MULTIPLIER: Double = 1.1
            private const val SECOND_HIT_ACCURACY: Double = 0.75
        }
    }
}
