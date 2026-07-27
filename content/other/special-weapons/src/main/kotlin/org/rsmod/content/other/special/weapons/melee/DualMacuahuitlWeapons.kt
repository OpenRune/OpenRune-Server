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

class DualMacuahuitlWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register("obj.dual_macuahuitl", DualMacuahuitl(manager))
    }

    private class DualMacuahuitl(private val manager: WeaponAttackManager) : MeleeWeapon {
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
            val firstLanded = rollAccuracy(target, attack)
            val firstDamage = if (firstLanded) rollMaxHit(target, attack) else 0
            manager.queueMeleeHit(this, target, firstDamage, delay = 1)

            val secondLanded = rollAccuracy(target, attack)
            val secondDamage = if (secondLanded) rollMaxHit(target, attack) else 0
            manager.queueMeleeHit(this, target, secondDamage, delay = 2)

            return firstDamage + secondDamage
        }

        private fun ProtectedAccess.rollAccuracy(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean =
            manager.rollMeleeAccuracy(
                source = this,
                target = target,
                attackType = attack.type,
                attackStyle = attack.style,
                blockType = attack.type,
                multiplier = 1.0,
            )

        private fun ProtectedAccess.rollMaxHit(target: PathingEntity, attack: CombatAttack.Melee): Int =
            manager.rollMeleeMaxHit(
                source = this,
                target = target,
                attackType = attack.type,
                attackStyle = attack.style,
                multiplier = 1.0,
            )
    }
}
