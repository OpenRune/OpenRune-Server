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
        registerMelee("obj.dragon_longsword", DragonLongsword(manager))
        registerMelee("obj.bh_dragon_longsword_corrupted", DragonLongsword(manager))
        registerMelee("obj.bh_dragon_longsword_imbue", DragonLongsword(manager, imbued = true))
    }

    private class DragonLongsword(private val manager: SpecialAttackManager, private val imbued: Boolean = false) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
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
                    accuracyMultiplier = if (imbued) 1.25 else 1.0,
                    maxHitMultiplier = 1.25,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            if (imbued) manager.setNextAttackDelay(this, 4)
            manager.continueCombat(this, target)
        }
    }
}
