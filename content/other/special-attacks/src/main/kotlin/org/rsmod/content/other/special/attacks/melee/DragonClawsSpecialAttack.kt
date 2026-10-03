package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
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

class DragonClawsSpecialAttack @Inject constructor(private val rng: GameRandom) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val handler = SliceAndDice(manager)
        WEAPONS.forEach { registerMelee(it, handler) }
    }

    private inner class SliceAndDice(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim(ANIMATION)
            spotanim(EFFECT, height = 0, slot = constants.spotanim_slot_combat)
            val maximum = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.0).coerceAtLeast(0)
            val amounts = roll(maximum, rng, player.adminMaxHit) {
                manager.rollMeleeAccuracy(this, target, attack.type, attack.style, MeleeAttackType.Slash, 1.0)
            }
            amounts.forEachIndexed { index, amount ->
                val hit = manager.queueMeleeHit(this, target, amount, 1 + index / 2)
                manager.giveCombatXp(this, target, attack, hit.damage)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        const val ANIMATION = "seq.human_dragon_claws_spec"
        const val EFFECT = "spotanim.dragon_claws_spot"
        val WEAPONS = listOf("obj.dragon_claws", "obj.br_dragon_claws", "obj.dragon_claws_ornament",
            "obj.bh_dragon_claws_corrupted", "obj.deadman_blighted_dragon_claws")

        /** Stop accuracy rolls at the first success; remaining splats are derived, not rerolled. */
        fun roll(maximum: Int, random: GameRandom, forceMaximum: Boolean = false, accurate: () -> Boolean): IntArray {
            require(maximum >= 0)
            val first = (0..3).firstOrNull { accurate() }
            if (first == null) return if (random.of(3) == 0) intArrayOf(0, 0, 0, 0) else intArrayOf(0, 0, 1, 1)
            if (maximum == 0) return intArrayOf(0, 0, 0, 0)
            val low = maximum * (4 - first) / 4
            val high = maximum + low - 1
            val total = if (forceMaximum) high else random.of(low, high)
            return when (first) {
                0 -> intArrayOf(total / 2, total / 4, total / 8, total / 8 + 1)
                1 -> intArrayOf(0, total / 2, total / 4, total / 4 + 1)
                2 -> intArrayOf(0, 0, total / 2, total / 2 + 1)
                else -> intArrayOf(0, 0, 0, total + 1)
            }
        }
    }
}
