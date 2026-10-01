package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
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

class DragonTwoHandedSpecialAttack @Inject constructor(private val targets: MeleeAreaTargets) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val special = Powerstab(manager)
        WEAPONS.forEach { registerMelee(it, special) }
    }

    private inner class Powerstab(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim("seq.dragon_two_handed_sword")
            spotanim("spotanim.dh_sword_update_dragon_2h_sword_special_spotanim", slot = constants.spotanim_slot_combat)
            // Powerstab is centred on the attacker; secondary NPCs require multiway, LoS and attack validation.
            val victims = targets.collect(player, target, radius = 1, limit = 14, centre = player.coords) {
                it.coords.level == player.coords.level &&
                    it.coords.x <= player.coords.x + 1 && it.coords.x + it.size - 1 >= player.coords.x - 1 &&
                    it.coords.z <= player.coords.z + 1 && it.coords.z + it.size - 1 >= player.coords.z - 1
            }
            for (victim in victims) {
                val amount = manager.rollMeleeDamage(this, victim, attack, 1.0, 1.0, blockType = MeleeAttackType.Slash)
                val hit = manager.queueMeleeHit(this, victim, amount, 1)
                manager.giveCombatXp(this, victim, attack, hit.damage)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        val WEAPONS = listOf("obj.dragon_2h_sword", "obj.bh_dragon_2h_sword_corrupted")
    }
}
