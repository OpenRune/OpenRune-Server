package org.rsmod.content.other.moonsets

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.MoonSet
import org.rsmod.api.random.GameRandom
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class BloodrageWeapons
@Inject
constructor(private val random: GameRandom, private val speeds: WeaponSpeeds) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val macuahuitl = DualMacuahuitl(manager, Macuahuitl(random), speeds)
        register("obj.dual_macuahuitl", macuahuitl)
        register("obj.br_dual_macuahuitl", macuahuitl)
    }

    private class DualMacuahuitl(
        private val manager: WeaponAttackManager,
        private val macuahuitl: Macuahuitl,
        private val speeds: WeaponSpeeds,
    ) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            swing(target, attack)
            return true
        }

        private fun ProtectedAccess.swing(target: PathingEntity, attack: CombatAttack.Melee) {
            manager.playWeaponFx(this, attack)
            val maxHit = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.0)
            val accuracy = {
                manager.rollMeleeAccuracy(this, target, attack.type, attack.style, attack.type, 1.0)
            }
            val result = macuahuitl.swing(maxHit, sequential = true, accuracy, accuracy)
            manager.queueMeleeHit(this, target, result.damage[0], delay = 1)
            manager.queueMeleeHit(this, target, result.damage[1], delay = 2)
            manager.giveCombatXp(this, target, attack, result.total)

            if (MoonSet.Blood.wearsSetEffect(player) && macuahuitl.bloodrageTriggers(result, false)) {
                manager.setNextAttackDelay(this, max(1, speeds.actual(player) - 1))
                spotanim(PROC_SPOTANIM, slot = constants.spotanim_slot_combat)
            }
            manager.continueCombat(this, target)
        }
    }

}

private const val PROC_SPOTANIM = "spotanim.special_dual_macuahuitl_spotanim"
