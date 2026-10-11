package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Energy Drain rolls 25% more accurately against Slash defence. A damaging PvP impact transfers
 * up to 10% run energy from its target to the attacker.
 */
class AbyssalWhipSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val energyDrain = EnergyDrain(manager)
        registerMelee("obj.abyssal_whip", energyDrain)
        registerMelee("obj.abyssal_whip_lava", energyDrain)
        registerMelee("obj.abyssal_whip_ice", energyDrain)
        registerMelee("obj.abyssal_tentacle", energyDrain)
        registerMelee("obj.br_abyssal_whip", energyDrain)
        registerMelee("obj.league_3_whip", energyDrain)
        registerMelee("obj.league_3_whip_tentacle", energyDrain)
    }

    private class EnergyDrain(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            energyDrain(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            energyDrain(target, attack)
            return true
        }

        private fun ProtectedAccess.energyDrain(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.slayer_abyssal_whip_attack")

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.0,
                    blockType = MeleeAttackType.Slash,
                )
            val source = player
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known pre-clamp
            // `damage` here is the authentic value to gate the energy transfer on - no need for
            // an impact callback to observe it after the fact.
            target.spotanim(
                spot = "spotanim.sp_attack_abyssal_whip",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )
            if (damage > 0) {
                (target as? Player)?.let { transferRunEnergy(source, it) }
            }
            manager.continueCombat(this, target)
        }

        private fun transferRunEnergy(source: Player, target: Player) {
            val transferable =
                minOf(
                    RUN_ENERGY_TRANSFER,
                    target.runEnergy.coerceAtLeast(0),
                    (constants.run_max_energy - source.runEnergy).coerceAtLeast(0),
                )
            if (transferable > 0) {
                source.runEnergy = (source.runEnergy + transferable).coerceAtMost(constants.run_max_energy)
                target.runEnergy = (target.runEnergy - transferable).coerceAtLeast(0)
                UpdateRun.energy(source, source.runEnergy)
                UpdateRun.energy(target, target.runEnergy)
            }
            target.mes("You feel drained!")
        }
    }

    private companion object {
        const val RUN_ENERGY_TRANSFER = 100
    }
}
