package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
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
 * Pulverize is a slower, 25%-more-accurate Crush attack that removes 35% of current Defence when
 * it deals damage.
 */
class ElderMaulSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val pulverize = Pulverize(manager)
        registerMelee("obj.elder_maul", pulverize)
        registerMelee("obj.br_elder_maul", pulverize)
        registerMelee("obj.elder_maul_ornament", pulverize)
    }

    private class Pulverize(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            pulverize(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            pulverize(target, attack)
            return true
        }

        private fun ProtectedAccess.pulverize(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            // Pulverize is one cycle slower than the maul's normal six-cycle attack.
            manager.setNextAttackDelay(this, 7)
            anim("seq.human_elder_maul_spec")
            // Confirmed against a reference implementation of this exact special (Zenyte-based
            // Offline_Scape/Near Reality, PULVERIZE in SpecialAttack.java: SHIELD_BASH_SOUND -
            // just how the reference itself names this constant, not shared with another weapon).
            // Unaliased in this cache's gamevals.
            soundSynth(SHIELD_BASH_SOUND)
            spotanim(
                spot = "spotanim.spotanim_elder_maul_special",
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
            )
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.0,
                    blockType = MeleeAttackType.Crush,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Same unverified height guess as the launch spotanim above.
            target.spotanim(spot = "spotanim.spotanim_elder_maul_special_impact", height = 0)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value to gate the Defence drain on - no impact callback needed.
            if (damage > 0) {
                reduceDefenceBy35Percent(target)
            }
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val SHIELD_BASH_SOUND = 3454
    }
}

private fun reduceDefenceBy35Percent(target: PathingEntity) {
    when (target) {
        is Player -> {
            val drain = target.stat("stat.defence") * 35 / 100
            if (drain > 0) {
                target.statSub("stat.defence", constant = drain, percent = 0)
            }
        }
        is Npc -> target.defenceLvl = target.defenceLvl * 65 / 100
    }
}
