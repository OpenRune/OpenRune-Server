package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Shield Bash: hits valid targets in the wielder's 11x11 surrounding area. NPC targets receive
 * two independent hits while player targets receive one. Every resolved positive hit drains the
 * target's highest current offensive stat by 5%, preferring melee (Attack + Strength), then
 * Ranged, then Magic when values are tied.
 */
class DinhsBulwarkSpecialAttack
@Inject
constructor(
    private val targets: AreaMeleeTargetSelector,
    private val pvp: PvPAreaAttackManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val shieldBash = ShieldBash(manager, targets, pvp)
        registerMelee("obj.dinhs_bulwark", shieldBash)
        registerMelee("obj.dinhs_bulwark_ornament", shieldBash)
    }

    private class ShieldBash(
        private val manager: SpecialAttackManager,
        private val targets: AreaMeleeTargetSelector,
        private val pvp: PvPAreaAttackManager,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = shieldBash(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = shieldBash(target, attack)

        private fun ProtectedAccess.shieldBash(
            primary: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim("seq.human_dinhs_bulwark_bash")

            val affected =
                if (mapMultiway()) {
                    targets.select(
                        source = this,
                        primary = primary,
                        tiles = targets.square(player.coords, radius = AREA_RADIUS),
                        npcLimit = MAX_TARGETS,
                        playerLimit = MAX_TARGETS,
                        totalLimit = MAX_TARGETS,
                    )
                } else {
                    listOf(primary)
                }

            for (target in affected) {
                val hitCount = if (target is Player) 1 else NPC_HIT_COUNT
                var totalDamage = 0
                repeat(hitCount) {
                    val damage =
                        manager.rollMeleeDamage(
                            source = this,
                            target = target,
                            attack = attack,
                            accuracyMultiplier =
                                if (target is Npc && attack.style == MeleeAttackStyle.Defensive) {
                                    DEFENSIVE_NPC_ACCURACY
                                } else {
                                    ACCURACY_MULTIPLIER
                                },
                            maxHitMultiplier = 1.0,
                            blockType = MeleeAttackType.Crush,
                        )
                    totalDamage += damage
                    // Real OSRS doesn't clamp damage after the roll, so the already-known damage
                    // here is the authentic value - no impact callback needed to gate the drain.
                    manager.queueMeleeHit(source = this, target = target, damage = damage)
                    if (damage > 0) {
                        drainHighestOffence(target)
                    }
                }
                manager.giveCombatXp(this, target, attack, totalDamage)

                if (target is Player && target !== primary) {
                    pvp.applySecondarySpecialAttack(this, target)
                }
            }
            manager.continueCombat(this, primary)
            return true
        }
    }

    private companion object {
        private const val AREA_RADIUS: Int = 5
        private const val MAX_TARGETS: Int = 10
        private const val NPC_HIT_COUNT: Int = 2
        private const val ACCURACY_MULTIPLIER: Double = 1.2
        private const val DEFENSIVE_NPC_ACCURACY: Double = 0.8
        private const val DRAIN_PERCENT: Int = 5
    }
}

private fun drainHighestOffence(target: PathingEntity) {
    when (target) {
        is Player -> target.drainHighestOffence()
        is Npc -> target.drainHighestOffence()
    }
}

private fun Player.drainHighestOffence() {
    val attack = stat("stat.attack")
    val strength = stat("stat.strength")
    val ranged = stat("stat.ranged")
    val magic = stat("stat.magic")
    when {
        attack + strength >= ranged && attack + strength >= magic -> {
            statSub("stat.attack", constant = attack * DRAIN_PERCENT / 100, percent = 0)
            statSub("stat.strength", constant = strength * DRAIN_PERCENT / 100, percent = 0)
        }
        ranged >= magic -> statSub("stat.ranged", constant = ranged * DRAIN_PERCENT / 100, percent = 0)
        else -> statSub("stat.magic", constant = magic * DRAIN_PERCENT / 100, percent = 0)
    }
}

private fun Npc.drainHighestOffence() {
    val attack = attackLvl
    val strength = strengthLvl
    val ranged = rangedLvl
    val magic = magicLvl
    when {
        attack + strength >= ranged && attack + strength >= magic -> {
            attackLvl = (attack - attack * DRAIN_PERCENT / 100).coerceAtLeast(0)
            strengthLvl = (strength - strength * DRAIN_PERCENT / 100).coerceAtLeast(0)
        }
        ranged >= magic -> rangedLvl = (ranged - ranged * DRAIN_PERCENT / 100).coerceAtLeast(0)
        else -> magicLvl = (magic - magic * DRAIN_PERCENT / 100).coerceAtLeast(0)
    }
}

private const val DRAIN_PERCENT: Int = 5
