package org.rsmod.content.other.special.weapons.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class MultiHitMeleeWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val weapon = GlacialTemotliFamily(manager)
        register("obj.glacial_temotli", weapon)
        register("obj.sulphur_blades", weapon)
        register("obj.earthbound_tecpatl", weapon)
        register("obj.barrows_torag_weapon", weapon)
        register("obj.barrows_torag_weapon_100", weapon)
        register("obj.barrows_torag_weapon_75", weapon)
        register("obj.barrows_torag_weapon_50", weapon)
        register("obj.barrows_torag_weapon_25", weapon)
    }

    private class GlacialTemotliFamily(private val manager: WeaponAttackManager) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            manager.playWeaponFx(this, attack)
            val totalDamage = rollAndQueueHits(target, attack)
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.continueCombat(this, target)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            manager.playWeaponFx(this, attack)
            val totalDamage = rollAndQueueHits(target, attack)
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.rollAndQueueHits(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Int {
            var totalDamage = 0
            for (roundUp in booleanArrayOf(false, true)) {
                val damage =
                    manager.rollMeleeDamage(
                        source = this,
                        target = target,
                        attack = attack,
                        accuracyMultiplier = 1.0,
                        maxHitMultiplier = 0.5,
                        roundMaxHitUp = roundUp,
                    )
                totalDamage += damage
                manager.queueMeleeHit(this, target, damage)
            }
            return totalDamage
        }
    }
}
