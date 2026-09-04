package org.rsmod.content.other.special.attacks.melee

import kotlin.math.min
import org.rsmod.api.combat.commons.CombatAttack
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
 * Sunder doubles accuracy and drains one tenth of its unmodified damage roll through the target's
 * Defence, Attack, Ranged, and Magic levels.
 */
class BarrelchestAnchorSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val sunder = Sunder(manager, maxHitMultiplier = 1.1)
        val imbuedSunder = Sunder(manager, maxHitMultiplier = 1.25)

        registerMelee("obj.brain_anchor", sunder)
        registerMelee("obj.bh_brain_anchor_imbue", imbuedSunder)
    }

    private class Sunder(
        private val manager: SpecialAttackManager,
        private val maxHitMultiplier: Double,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            sunder(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            sunder(target, attack)
            return true
        }

        private fun ProtectedAccess.sunder(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.brain_player_anchor_special_attack")
            soundSynth(SUNDER_SOUND)
            spotanim(
                spot = "spotanim.brain_anchor_special_attack_spot",
                height = 0,
            )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 2.0,
                    maxHitMultiplier = maxHitMultiplier,
                )
            // The drain is determined from the raw damage roll, before target-side modifiers.
            val drain = damage / 10
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value to gate the drain on - no impact callback needed.
            if (damage > 0 && drain > 0) {
                drainSunderStats(target, drain)
            }
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val SUNDER_SOUND = 3481
    }
}

private fun drainSunderStats(target: PathingEntity, amount: Int) {
    var remaining = amount
    when (target) {
        is Player -> {
            for (stat in SUNDER_PLAYER_STAT_ORDER) {
                val drain = min(target.stat(stat), remaining)
                if (drain > 0) {
                    target.statSub(stat, constant = drain, percent = 0)
                    remaining -= drain
                }
                if (remaining == 0) {
                    return
                }
            }
        }
        is Npc -> {
            fun drain(current: Int, apply: (Int) -> Unit) {
                val value = min(current, remaining)
                if (value > 0) {
                    apply(value)
                    remaining -= value
                }
            }

            drain(target.defenceLvl) { target.defenceLvl -= it }
            drain(target.attackLvl) { target.attackLvl -= it }
            drain(target.rangedLvl) { target.rangedLvl -= it }
            drain(target.magicLvl) { target.magicLvl -= it }
        }
    }
}

private val SUNDER_PLAYER_STAT_ORDER =
    listOf(
        "stat.defence",
        "stat.attack",
        "stat.ranged",
        "stat.magic",
    )
