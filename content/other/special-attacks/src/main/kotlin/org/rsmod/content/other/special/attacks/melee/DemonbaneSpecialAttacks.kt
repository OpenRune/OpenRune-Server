package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Darklight, Arclight, and Emberlight share Weaken. It makes a normal melee hit against Stab
 * defence and, only when the accuracy roll succeeds, drains Attack, Strength, and Defence by a
 * base-level percentage plus one. The drain stacks additively.
 */
class DemonbaneSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val darklight = Weaken(manager, demonDrainPercent = 10)
        registerMelee("obj.darklight", darklight)

        val arclight = Weaken(manager, demonDrainPercent = 10)
        registerMelee("obj.arclight", arclight)
        registerMelee("obj.arclight_inactive", arclight)

        // Only Emberlight's own wiki page documents a "Sound effects" table for this special;
        // Darklight and Arclight have none, so they get no sound here rather than a guess.
        val emberlight = Weaken(manager, demonDrainPercent = 15, sounds = EMBERLIGHT_SOUNDS)
        registerMelee("obj.emberlight", emberlight)
    }

    private inner class Weaken(
        private val manager: SpecialAttackManager,
        private val demonDrainPercent: Int,
        private val sounds: IntArray = IntArray(0),
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            weaken(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            weaken(target, attack)
            return true
        }

        private fun ProtectedAccess.weaken(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.dark_spec_player")
            spotanim("spotanim.dark_spec_spot", slot = constants.spotanim_slot_combat)
            sounds.forEachIndexed { index, sound -> soundSynth(sound, delay = index * SOUND_SPACING) }
            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Stab,
                    multiplier = 1.0,
                )
            val damage =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.0,
                    )
                } else {
                    0
                }
            val drainPercent =
                if (target is Npc && target.visType.param(params.demon) != 0) {
                    demonDrainPercent
                } else {
                    NORMAL_DRAIN_PERCENT
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            if (successful) {
                drainCombatStats(target, drainPercent)
            }
            manager.continueCombat(this, target)
        }
    }

    private fun drainCombatStats(target: PathingEntity, percent: Int) {
        when (target) {
            is Player -> {
                target.statSub("stat.attack", constant = 1, percent = percent)
                target.statSub("stat.strength", constant = 1, percent = percent)
                target.statSub("stat.defence", constant = 1, percent = percent)
            }
            is Npc -> {
                target.attackLvl = (target.attackLvl - drainAmount(target.baseAttackLvl, percent)).coerceAtLeast(0)
                target.strengthLvl =
                    (target.strengthLvl - drainAmount(target.baseStrengthLvl, percent)).coerceAtLeast(0)
                target.defenceLvl =
                    (target.defenceLvl - drainAmount(target.baseDefenceLvl, percent)).coerceAtLeast(0)
            }
        }
    }

    private fun drainAmount(baseLevel: Int, percent: Int): Int = (baseLevel * percent / 100) + 1

    private companion object {
        const val NORMAL_DRAIN_PERCENT: Int = 5
        const val SOUND_SPACING: Int = 20

        // Emberlight's own wiki "Sound effects" table: crouch/spin/stab, one per special-attack
        // part.
        val EMBERLIGHT_SOUNDS = intArrayOf(9319, 9320, 9321)
    }
}
