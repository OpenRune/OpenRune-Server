package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.accuracy.melee.ReducedMeleeDefenceAccuracy
import org.rsmod.api.config.constants
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class BountyHunterMaceSpecialAttack @Inject constructor(
    private val accuracy: ReducedMeleeDefenceAccuracy,
    private val formula: MeleeSpecialDamage,
    private val rng: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.bh_dragon_mace_imbue", Shatter(manager))
    }

    private inner class Shatter(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim("seq.shatter")
            spotanim("spotanim.sp_attack_shatter_spotanim", height = 0, slot = constants.spotanim_slot_combat)
            val accurate = accuracy.roll(player, target, attack, MeleeAttackType.Crush, 60, 125)
            val rolled = if (!accurate) 0 else {
                val maximum = formula.maximum(player, target, attack, firstPercent = 150, deferReductions = true).coerceAtLeast(1)
                if (player.adminMaxHit) maximum else rng.of(1, maximum)
            }
            val damage = if (!accurate) 0 else formula.modifyRolledHit(player, target, attack, rolled)
            val hit = manager.queueMeleeHit(this, target, damage, 1)
            manager.giveCombatXp(this, target, attack, hit.damage)
            manager.continueCombat(this, target)
            return true
        }
    }
}
