package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import kotlin.math.abs
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

class HalberdSpecialAttacks @Inject constructor(private val targets: MeleeAreaTargets) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (weapon in WEAPONS) registerMelee(weapon, Sweep(manager, weapon.contains("crystal")))
    }

    private inner class Sweep(private val manager: SpecialAttackManager, private val crystal: Boolean) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = sweep(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = sweep(target, attack)

        private fun ProtectedAccess.sweep(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim("seq.dragon_halberd_special_attack")
            val dx = target.coords.x - coords.x
            val dz = target.coords.z - coords.z
            val direction = if (abs(dz) >= abs(dx)) {
                if (dz >= 0) "north" else "south"
            } else if (dx >= 0) "east" else "west"
            val colour = if (crystal) "white" else "red"
            spotanim("spotanim.dragon_halberd_special_${direction}_$colour", slot = constants.spotanim_slot_combat)
            val victims = if (target.size > 1) listOf(target) else targets.collect(player, target, 2, 10) {
                it.size == 1 && MeleeSpecialRules.inHalberdSweep(coords, target.coords, it.coords)
            }
            for (victim in victims) {
                repeat(MeleeSpecialRules.halberdHits(victim.size)) { index ->
                    val damage = manager.rollMeleeDamage(
                        this, victim, attack,
                        accuracyMultiplier = if (index == 0) 1.0 else 0.75,
                        maxHitMultiplier = 1.1,
                        blockType = MeleeAttackType.Slash,
                    )
                    val hit = manager.queueMeleeHit(this, victim, damage, 1 + index)
                    manager.giveCombatXp(this, victim, attack, hit.damage)
                }
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        val WEAPONS = listOf("obj.dragon_halberd", "obj.bh_dragon_halberd_corrupted", "obj.crystal_halberd", "obj.crystal_halberd_2500")
    }
}
