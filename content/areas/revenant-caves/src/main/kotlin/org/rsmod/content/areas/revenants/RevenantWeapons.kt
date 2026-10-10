package org.rsmod.content.areas.revenants

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.weapons.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

class RevenantWeapons : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        for (kind in RevenantCharges.weapons) for (name in listOf(kind.empty, kind.charged)) {
            when {
                kind.charged.contains("bow_") || kind.charged.contains("webweaver_") -> register(name, Bow(manager))
                kind.charged.contains("sceptre_") || kind.charged.contains("accursed_") -> if (!kind.charged.endsWith("_recol")) register(name, Staff(manager))
                else -> register(name, Mace(manager))
            }
        }
    }
    private class Mace(private val manager: WeaponAttackManager) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = strike(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = strike(target, attack)
        private fun ProtectedAccess.strike(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            manager.playWeaponFx(this, attack)
            val damage = manager.rollMeleeDamage(this, target, attack, accuracyMultiplier = 1.0, maxHitMultiplier = 1.0)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }
    }
    private class Bow(private val manager: WeaponAttackManager) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean = shoot(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged): Boolean = shoot(target, attack)
        private fun ProtectedAccess.shoot(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            if (!ready(attack.weapon, manager)) return true
            manager.playWeaponFx(this, attack)
            val upgraded = RevenantCharges.kind(attack.weapon)?.charged?.contains("webweaver") == true
            spotanim(if (upgraded) "spotanim.wild_cave_bow_arrow_launch02" else "spotanim.wild_cave_bow_arrow_launch", height = 96)
            val projectile = manager.spawnProjectile(this, target, if (upgraded) "spotanim.wild_cave_bow_arrow_travel02" else "spotanim.wild_cave_bow_arrow_travel", "projanim.arrow")
            val damage = manager.rollRangedDamage(this, target, attack)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueRangedHit(this, target, null, damage, projectile.clientCycles, projectile.serverCycles)
            manager.continueCombat(this, target)
            return true
        }
    }
    private class Staff(private val manager: WeaponAttackManager) : MagicWeapon {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Staff): Boolean = cast(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Staff): Boolean = cast(target, attack)
        private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Staff): Boolean {
            if (!ready(attack.weapon, manager)) return true
            val item = getInvObj(attack.weapon)
            anim(RSCM.getReverseMapping(RSCMType.SEQ, item.param(params.attack_anim_stance1).id))
            manager.setNextAttackDelay(this, 4)
            val upgraded = RevenantCharges.kind(attack.weapon)?.charged?.contains("accursed") == true
            val projectile = manager.spawnProjectile(this, target, "spotanim.spells_thammaron01_travel01", "projanim.magic_spell")
            val accurate = manager.rollStaffAccuracy(this, target, attack)
            val damage = if (accurate) manager.rollStaffMaxHit(this, target, (player.magicLvl / 3 - if (upgraded) 6 else 8).coerceAtLeast(1)) else 0
            if (accurate) {
                manager.giveCombatXp(this, target, attack, damage)
                manager.queueMagicHit(this, target, damage, projectile.clientCycles, projectile.serverCycles)
            } else manager.queueSplashHit(this, target, projectile.clientCycles, projectile.serverCycles)
            manager.continueCombat(this, target)
            return true
        }
    }
    private companion object {
        fun ProtectedAccess.ready(item: org.rsmod.game.inv.InvObj, manager: WeaponAttackManager): Boolean {
            val current = player.worn[Wearpos.RightHand.slot]
            val kind = current?.let(RevenantCharges::kind)
            if (current != item || kind == null || RevenantCharges.count(current) <= kind.deposit) {
                mes("Your weapon needs ether beyond its 1,000-ether activation deposit.")
                manager.stopCombat(this)
                return false
            }
            return true
        }
    }
}
