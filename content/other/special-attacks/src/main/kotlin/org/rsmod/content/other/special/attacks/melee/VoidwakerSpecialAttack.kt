package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Disrupt is a guaranteed Magic hit worth 50?150% of the wielder's normal melee maximum hit.
 *
 * Its hit type intentionally remains Magic, so Magic damage mitigation?including Protect from
 * Magic?continues to work even though the roll is based on the equipped sword's melee strength.
 */
class VoidwakerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val disrupt = Disrupt(manager)
        registerMelee("obj.voidwaker", disrupt)
        registerMelee("obj.br_voidwaker", disrupt)
        registerMelee("obj.deadman_blighted_voidwaker", disrupt)
        registerMelee("obj.deadman_voidwaker", disrupt)
    }

    private class Disrupt(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            disrupt(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            disrupt(target, attack)
            return true
        }

        private fun ProtectedAccess.disrupt(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.human_special02_voidwaker")
            soundSynth(DISRUPT_SOUND)
            spotanim(
                spot = "spotanim.fx_voidwaker02_special",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            target.spotanim(
                spot = "spotanim.fx_voidwaker_impact",
                height = 0,
                slot = constants.spotanim_slot_combat,
            )

            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            // Deliberately do not make an accuracy roll: Disrupt always lands a Magic hit.
            val damage =
                if (maxHit > 0) {
                    random.of((maxHit / 2)..((maxHit * 3) / 2))
                } else {
                    0
                }
            val source = player
            manager.queueMagicHit(source = this, target = target, damage = damage, clientDelay = 0)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value for the bonus xp - no impact callback needed.
            if (damage > 0) {
                // Voidwaker grants two Magic experience per point of damage dealt.
                source.statAdvance("stat.magic", damage * MAGIC_XP_PER_DAMAGE)
            }
            manager.continueCombat(this, target)
        }
        private companion object {
            const val MAGIC_XP_PER_DAMAGE = 2.0

            /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
            const val DISRUPT_SOUND = 2945
        }
    }
}
