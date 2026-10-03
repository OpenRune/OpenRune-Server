package org.rsmod.content.other.special.weapons.magic

import dev.openrune.util.Wearpos
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.worn.AyakCharges
import org.rsmod.api.weapons.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

class AyakWeapons : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (symbol in listOf(AyakCharges.CHARGED, AyakCharges.EMPTY)) register(symbol, Eye(manager))
    }
    private class Eye(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Staff): Boolean {
            mes("The Eye of Ayak cannot be used against players.")
            manager.stopCombat(this)
            return true
        }
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Staff): Boolean {
            val weapon = player.worn[Wearpos.RightHand.slot]
            if (weapon == null || weapon != attack.weapon || !AyakCharges.accepts(weapon)) { manager.stopCombat(this); return true }
            val charges = AyakCharges.count(weapon)
            if (charges == 0) {
                player.worn[Wearpos.RightHand.slot] = AyakCharges.write(weapon, 0)
                mes("Your Eye of Ayak has no charges. Charge it with runes or demon tears.")
                manager.stopCombat(this)
                return true
            }
            anim("seq.human_eye_of_ayak_normal")
            spotanim("spotanim.vfx_ayak_player_normal_spotanim")
            manager.setNextAttackDelay(this, 3)
            val projectile = manager.spawnProjectile(this, target, "spotanim.vfx_ayak_normal_projectile", "projanim.magic_spell")
            val accurate = manager.rollStaffAccuracy(this, target, attack)
            if (accurate) {
                val damage = manager.rollStaffMaxHit(this, target, AyakCharges.baseMaxHit(player.magicLvl))
                target.spotanim("spotanim.vfx_ayak_normal_impact", delay = projectile.clientCycles)
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, projectile.clientCycles, projectile.serverCycles)
            } else {
                target.spotanim("spotanim.failedspell_impact", height = 100, delay = projectile.clientCycles)
                manager.queueSplashHit(this, target, projectile.clientCycles, projectile.serverCycles)
            }
            player.worn[Wearpos.RightHand.slot] = AyakCharges.write(weapon, charges - 1)
            if (charges == 1) { mes("Your Eye of Ayak has run out of charges."); manager.stopCombat(this) } else manager.continueCombat(this, target)
            return true
        }
    }
}
