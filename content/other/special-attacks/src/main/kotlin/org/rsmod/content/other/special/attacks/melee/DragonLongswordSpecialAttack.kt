package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class DragonLongswordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val cleave = DragonLongsword(manager)
        registerMelee("obj.dragon_longsword", cleave)
        registerMelee("obj.bh_dragon_longsword_corrupted", cleave)

        // The Bounty Hunter imbue has the same 25% damage increase, but has a
        // 25% accuracy increase and attacks one game cycle faster.
        registerMelee(
            "obj.bh_dragon_longsword_imbue",
            DragonLongsword(manager, accuracyMultiplier = 1.25, nextAttackDelay = 4),
        )
    }

    private class DragonLongsword(
        private val manager: SpecialAttackManager,
        private val accuracyMultiplier: Double = 1.0,
        private val nextAttackDelay: Int? = null,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            if (nextAttackDelay != null) {
                mes("This special attack can only be used against other players.")
                manager.stopCombat(this)
                return false
            }
            cleave(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            cleave(target, attack)
            return true
        }

        private fun ProtectedAccess.cleave(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.cleave")
            if (nextAttackDelay != null) {
                manager.setNextAttackDelay(this, nextAttackDelay)
            }

            soundSynth("synth.cleave")
            spotanim(
                spot = "spotanim.sp_attack_cleave_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = accuracyMultiplier,
                    maxHitMultiplier = 1.25,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }
}
