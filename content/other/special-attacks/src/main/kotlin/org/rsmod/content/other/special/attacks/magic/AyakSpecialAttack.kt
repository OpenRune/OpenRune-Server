package org.rsmod.content.other.special.attacks.magic

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.npc.MagicDefenceDrain
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.worn.AyakCharges
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList

class AyakSpecialAttack @Inject constructor(private val queues: WorldQueueList) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMagic(AyakCharges.CHARGED, SoulRend(manager))
    }
    private inner class SoulRend(private val manager: SpecialAttackManager) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Staff): Boolean {
            mes("The Eye of Ayak cannot be used against players.")
            manager.stopCombat(this)
            return false
        }
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Staff): Boolean {
            val weapon = player.worn[Wearpos.RightHand.slot]
            if (weapon == null || weapon != attack.weapon || !AyakCharges.accepts(weapon)) { manager.stopCombat(this); return false }
            val charges = AyakCharges.count(weapon)
            if (charges == 0) {
                player.worn[Wearpos.RightHand.slot] = AyakCharges.write(weapon, 0)
                mes("Your Eye of Ayak has no charges.")
                manager.stopCombat(this)
                return false
            }
            anim("seq.human_eye_of_ayak_special")
            spotanim("spotanim.vfx_ayak_player_special_spotanim")
            manager.setNextAttackDelay(this, 5)
            val accurate = manager.rollStaffAccuracy(this, target, attack.style, 2.0)
            // Soul Rend scales the base hit before equipment damage bonuses and their rounding.
            val baseMaxHit = AyakCharges.baseMaxHit(player.magicLvl) * 13 / 10
            val damage = if (accurate) manager.rollStaffMaxHit(this, target, baseMaxHit, 1.0) else 0
            val delay = 2
            target.spotanim(if (accurate) "spotanim.vfx_ayak_impact_special_spotanim" else "spotanim.failedspell_impact", delay = 60)
            val hit = manager.queueMagicHit(this, target, damage, 60, delay)
            manager.giveCombatXp(this, target, attack, hit.damage)
            if (hit.damage > 0) {
                val targetUid = target.uid
                val respawnCycle = target.lifecycleRespawnCycle
                queues.add(delay) {
                    if (target.isSlotAssigned && target.uid == targetUid && target.lifecycleRespawnCycle == respawnCycle && target.hitpoints > 0) MagicDefenceDrain.apply(target, hit.damage)
                }
            }
            player.worn[Wearpos.RightHand.slot] = AyakCharges.write(weapon, charges - 1)
            if (charges == 1) manager.stopCombat(this) else manager.continueCombat(this, target)
            return true
        }
    }
}
