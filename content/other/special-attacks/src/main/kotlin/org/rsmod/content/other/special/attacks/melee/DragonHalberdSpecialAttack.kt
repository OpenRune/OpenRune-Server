package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * Sweep hits a large primary NPC twice or sweeps the three-tile line through a small target in
 * multi-combat. Every hit uses Slash defence, 110% maximum damage, and an independent roll.
 */
class DragonHalberdSpecialAttack
@Inject
constructor(
    private val targets: AreaMeleeTargetSelector,
    private val pvp: PvPAreaAttackManager,
    private val worldRepo: WorldRepository,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val sweep = Sweep(manager, targets, pvp, worldRepo)
        registerMelee("obj.dragon_halberd", sweep)
        registerMelee("obj.bh_dragon_halberd_corrupted", sweep)
    }

    private class Sweep(
        private val manager: SpecialAttackManager,
        private val targets: AreaMeleeTargetSelector,
        private val pvp: PvPAreaAttackManager,
        private val worldRepo: WorldRepository,
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
            soundSynth(SWEEP_SOUND)
            // Ground-tile graphic, not attached to the caster: target's own tile for a
            // single-tile target, otherwise the midpoint between target and caster.
            val tile =
                if (primary.size == 1) {
                    primary.coords
                } else {
                    val centre = primary.bounds()
                    CoordGrid(
                        x = ((centre.fineCentreX + player.coords.x) / 2.0).toInt(),
                        z = ((centre.fineCentreZ + player.coords.z) / 2.0).toInt(),
                        level = player.coords.level,
                    )
                }
            spotanimMap(
                repo = worldRepo,
                internal = HalberdSpecialVisuals.forTarget(player.coords, tile),
                coord = tile,
                height = 96,
            )

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

            /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
            private const val SWEEP_SOUND = 2533
        }
    }
}
