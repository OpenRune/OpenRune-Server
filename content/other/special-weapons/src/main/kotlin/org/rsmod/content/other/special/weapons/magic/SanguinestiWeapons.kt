package org.rsmod.content.other.special.weapons.magic

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.weapons.*
import org.rsmod.content.other.special.weapons.scripts.charge.SanguinestiCharges
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

class SanguinestiWeapons : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for ((charged, empty) in SanguinestiCharges.variants) for (symbol in listOf(charged, empty)) register(symbol, Staff(manager))
    }
    private class Staff(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Staff): Boolean {
            mes("You cannot use this powered staff against other players.")
            manager.stopCombat(this)
            return true
        }
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Staff): Boolean {
            val weapon = player.worn[Wearpos.RightHand.slot]
            if (weapon == null || weapon != attack.weapon || SanguinestiCharges.variant(weapon) == null) { manager.stopCombat(this); return true }
            val charges = SanguinestiCharges.count(weapon)
            if (charges == 0) {
                player.worn[Wearpos.RightHand.slot] = SanguinestiCharges.write(weapon, 0)
                mes("Your Sanguinesti staff has no charges. Add blood runes to charge it.")
                manager.stopCombat(this)
                return true
            }
            val holy = SanguinestiCharges.variant(weapon) == SanguinestiCharges.variants[1]
            val suffix = if (holy) "_justiciar" else ""
            val seq = getInvObj(weapon).param(params.attack_anim_stance1)
            anim(RSCM.getReverseMapping(RSCMType.SEQ, seq.id))
            spotanim("spotanim.sanguinesti_staff_casting$suffix", height = 100)
            manager.setNextAttackDelay(this, 4)
            val projectile = manager.spawnProjectile(this, target, "spotanim.sanguinesti_staff_travel$suffix", "projanim.magic_spell")
            val accurate = manager.rollStaffAccuracy(this, target, attack)
            if (accurate) {
                val leech = random.of(5) == 0
                val damage = manager.rollStaffMaxHit(this, target, (player.magicLvl / 3).coerceAtLeast(1)) + if (leech) 8 else 0
                target.spotanim("spotanim.sanguinesti_staff_impact$suffix", delay = projectile.clientCycles, height = 100)
                manager.giveCombatXp(this, target, attack, damage)
                val hit = manager.queueMagicHit(this, target, damage, projectile.clientCycles, projectile.serverCycles)
                if (leech) {
                    val source = player
                    val uid = source.uid
                    hit.impactEffects.add { actualDamage ->
                        if (actualDamage > 0 && source.isSlotAssigned && source.uid == uid) {
                            source.statHeal("stat.hitpoints", actualDamage / 2, 0)
                            source.spotanim("spotanim.sanguinesti_staff_heal$suffix", height = 100)
                        }
                    }
                }
            } else {
                target.spotanim("spotanim.failedspell_impact", delay = projectile.clientCycles, height = 100)
                manager.queueSplashHit(this, target, projectile.clientCycles, projectile.serverCycles)
            }
            player.worn[Wearpos.RightHand.slot] = SanguinestiCharges.write(weapon, charges - 1)
            if (charges == 1) { mes("Your Sanguinesti staff has run out of charges."); manager.stopCombat(this) } else manager.continueCombat(this, target)
            return true
        }
    }
}
