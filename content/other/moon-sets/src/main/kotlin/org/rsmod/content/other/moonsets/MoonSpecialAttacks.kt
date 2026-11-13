package org.rsmod.content.other.moonsets

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.combat.commons.styles.MagicAttackStyle
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.burn.Burn
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.worn.MoonSet
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class MoonSpecialAttacks
@Inject
constructor(
    private val random: GameRandom,
    private val speeds: WeaponSpeeds,
    private val burn: Burn,
    private val playerAttacks: PlayerAttackManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val infusion = BloodInfusion(manager, Macuahuitl(random), speeds, playerAttacks)
        registerMelee("obj.dual_macuahuitl", infusion)
        registerMelee("obj.br_dual_macuahuitl", infusion)

        val shackles = BreakShackles(manager, random)
        registerMelee("obj.frostmoon_spear", shackles)
        registerMelee("obj.br_frostmoon_spear", shackles)

        val eclipse = Eclipse(manager, random, burn)
        registerRanged("obj.eclipse_atlatl", eclipse)
        registerRanged("obj.br_eclipse_atlatl", eclipse)
    }

    private class BloodInfusion(
        private val manager: SpecialAttackManager,
        private val macuahuitl: Macuahuitl,
        private val speeds: WeaponSpeeds,
        private val playerAttacks: PlayerAttackManager,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = infuse(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = infuse(target, attack)

        private fun ProtectedAccess.infuse(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            if (!MoonSet.Blood.wearsFunctionalSet(player)) {
                manager.stopCombat(this)
                mes("You need to be wearing the full Blood Moon armour set to use this attack.")
                return false
            }

            val cost = player.hitpoints / 4
            if (cost > 0) {
                player.statSub("stat.hitpoints", constant = cost, percent = 0)
            }

            playerAttacks.playWeaponFx(player, attack)
            spotanim(
                "spotanim.special_dual_macuahuitl_spotanim",
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val maxHit = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.25)
            val accuracy = {
                manager.rollMeleeAccuracy(this, target, attack.type, attack.style, attack.type, 1.25)
            }
            val result = macuahuitl.swing(maxHit, sequential = false, accuracy, accuracy)
            manager.queueMeleeHit(this, target, result.damage[0], delay = 1)
            manager.queueMeleeHit(this, target, result.damage[1], delay = 2)
            manager.giveCombatXp(this, target, attack, result.total)

            if (macuahuitl.bloodrageTriggers(result, guaranteed = true)) {
                manager.setNextAttackDelay(this, max(1, speeds.actual(player) - 1))
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private class BreakShackles(
        private val manager: SpecialAttackManager,
        private val random: GameRandom,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            val frozenFor = CombatEffects.frozenTicks(target)
            if (frozenFor > 0) {
                CombatEffects.unfreeze(target)
            }
            return shatter(target, attack, frozenFor)
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            val frozenFor = CombatEffects.frozenTicks(target)
            if (target.isFrozen) {
                CombatEffects.unfreeze(target)
            }
            return shatter(target, attack, frozenFor)
        }

        private fun ProtectedAccess.shatter(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            bindTicks: Int,
        ): Boolean {
            if (!MoonSet.Blue.wearsFunctionalSet(player)) {
                manager.stopCombat(this)
                mes("You need to be wearing the full Blue Moon armour set to use this attack.")
                return false
            }

            anim("seq.ice_shatter_special_attack")
            spotanim(
                "spotanim.special_spell_spear_spotanim",
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val accuracyBonus = 1.0 + bindTicks * BONUS_PER_TICK
            val damageBonus = 1.0 + min(bindTicks * BONUS_PER_TICK, MAX_DAMAGE_BONUS)
            val type = MeleeAttackType.Crush
            val style = MeleeAttackStyle.Defensive

            val accurate =
                manager.rollMeleeAccuracy(this, target, type, style, type, accuracyBonus)
            val maxHit = manager.calculateMeleeMaxHit(this, target, type, style, damageBonus)
            val damage = if (accurate && maxHit > 0) random.of(1..maxHit) else 0

            // The spear's special trains Strength even though it plays the Fend animation.
            val training = attack.copy(style = MeleeAttackStyle.Aggressive)
            manager.giveCombatXp(this, target, training, damage)
            manager.queueMeleeHit(this, target, damage)
            if (player.vars["varbit.autocast_set"] == 0) {
                manager.continueCombat(this, target)
            }
            return true
        }

        private companion object {
            const val BONUS_PER_TICK = 0.015
            const val MAX_DAMAGE_BONUS = 1.125
        }
    }

    private class Eclipse(
        private val manager: SpecialAttackManager,
        private val random: GameRandom,
        private val burn: Burn,
    ) : RangedSpecialAttack {
        override val meleeDistance: Boolean = true

        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = eclipse(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = eclipse(target, attack)

        private fun ProtectedAccess.eclipse(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            if (!MoonSet.Eclipse.wearsFunctionalSet(player)) {
                manager.stopCombat(this)
                mes("You need to be wearing the full Eclipse Moon armour set to use this attack.")
                return false
            }

            anim("seq.human_special_atlatl_01")
            spotanim(
                "spotanim.special_atlatl_cast_spotanim",
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            // The burns are consumed on launch, even if the accuracy roll then fails.
            val burnBonus = min(MAX_BURN_BONUS, burn.consume(target))
            val accurate = manager.rollStaffAccuracy(this, target, MagicAttackStyle.Accurate, 1.5)

            var damage = 0
            if (accurate) {
                val base =
                    manager.calculateRangedMaxHit(this, target, attack.type, attack.style, 1.0, 0)
                val maxHit = base + burnBonus
                val minHit = max(1, burnBonus / 2)
                damage = if (maxHit >= minHit) random.of(minHit..maxHit) else 0
            }

            target.spotanim("spotanim.special_atlatl_impact_spotanim", height = 96)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = 0)
            manager.continueCombat(this, target)
            return true
        }

        private companion object {
            const val MAX_BURN_BONUS = 50
        }
    }
}
