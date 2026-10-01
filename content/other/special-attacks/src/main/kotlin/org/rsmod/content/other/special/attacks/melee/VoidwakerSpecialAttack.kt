package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
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

/** A melee weapon's special with a guaranteed magic hit; it is not a melee accuracy shortcut. */
class VoidwakerSpecialAttack @Inject constructor(
    private val damage: MeleeSpecialDamage,
    private val rng: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val special = Disrupt(manager)
        WEAPONS.forEach { registerMelee(it, special) }
    }

    private inner class Disrupt(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim("seq.human_special_voidwaker")
            spotanim("spotanim.fx_voidwaker02_special", slot = constants.spotanim_slot_combat)
            val maximum = damage.maximum(player, target, attack, magic = true).coerceAtLeast(0)
            var amount = if (player.adminMaxHit) maximum * 3 / 2 else rng.of(maximum / 2, maximum * 3 / 2)
            if (target is Player) amount = damage.reduce(target, amount)
            target.spotanim("spotanim.fx_voidwaker_impact", delay = 30, height = 96)
            val hit = manager.queueMagicHit(this, target, amount, clientDelay = 30, hitDelay = 1)
            manager.giveCombatXp(this, target, attack, hit.damage)
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        val WEAPONS = listOf("obj.voidwaker", "obj.br_voidwaker", "obj.deadman_voidwaker", "obj.deadman_blighted_voidwaker")
    }
}
