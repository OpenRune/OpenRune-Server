package org.rsmod.content.other.special.attacks.melee

import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.combat.commons.BindEffectService
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isAnyType

/**
 * Break Shackles uses any active binding duration on the target to boost its normal melee hit,
 * then breaks that binding only after a positive final hit lands.
 *
 * Reads/breaks binds via [BindEffectService] - the shared tracker also used by Zamorak godsword's
 * Ice Cleave (see `ImpactMeleeSpecialAttacks.kt`). Deliberately separate from the pre-existing
 * magic freeze-spell mechanic (Snare/Entangle/Bind/Ice Barrage): a target frozen by a spell won't
 * show a remaining bind duration here. Documented scope limit, not an oversight.
 */
class BlueMoonSpearSpecialAttack
@Inject
constructor(private val binds: BindEffectService) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val breakShackles = BreakShackles(manager, binds)
        registerMelee("obj.frostmoon_spear", breakShackles)
        registerMelee("obj.br_frostmoon_spear", breakShackles)
    }

    private class BreakShackles(
        private val manager: SpecialAttackManager,
        private val binds: BindEffectService,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = breakShackles(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = breakShackles(target, attack)

        private fun ProtectedAccess.breakShackles(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            if (!player.isWearingBlueMoonSet()) {
                mes("You need to be wearing the full Blue Moon armour set to use this special attack.")
                manager.stopCombat(this)
                return false
            }

            val multipliers = BlueMoonSpearDamage.multipliers(binds.cyclesRemaining(target))
            anim("seq.human_zamorakspear_lunge")
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = multipliers.accuracy,
                    maxHitMultiplier = multipliers.maxHit,
                )

            // Break Shackles always grants Strength XP, independently of the selected spear style.
            manager.giveCombatXp(
                source = this,
                target = target,
                attack = attack.copy(style = MeleeAttackStyle.Aggressive),
                damage = damage,
            )
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value - no impact callback needed to gate the bind break.
            if (damage > 0) {
                binds.breakBind(target)
            }
            manager.continueCombat(this, target)
            return true
        }
    }
}

/** Pure duration-to-damage conversion used by Break Shackles. */
internal object BlueMoonSpearDamage {
    data class Multipliers(
        val accuracy: Double,
        val maxHit: Double,
    )

    fun multipliers(bindingCycles: Int): Multipliers {
        require(bindingCycles >= 0) {
            "bindingCycles must not be negative. (bindingCycles=$bindingCycles)"
        }
        val bonus = bindingCycles * BONUS_PER_BIND_CYCLE
        return Multipliers(
            accuracy = 1.0 + bonus,
            maxHit = 1.0 + min(bonus, MAX_DAMAGE_BONUS),
        )
    }

    private const val BONUS_PER_BIND_CYCLE: Double = 0.015
    private const val MAX_DAMAGE_BONUS: Double = 1.125
}

private fun Player.isWearingBlueMoonSet(): Boolean =
    worn[Wearpos.Hat.slot].isAnyType(
        "obj.frost_moon_helm",
        "obj.frost_moon_helm_degraded",
        "obj.br_frost_moon_helm",
    ) &&
        worn[Wearpos.Torso.slot].isAnyType(
            "obj.frost_moon_chestplate",
            "obj.frost_moon_chestplate_degraded",
            "obj.br_frost_moon_chestplate",
        ) &&
        worn[Wearpos.Legs.slot].isAnyType(
            "obj.frost_moon_tassets",
            "obj.frost_moon_tassets_degraded",
            "obj.br_frost_moon_tassets",
        ) &&
        worn[Wearpos.RightHand.slot].isAnyType(
            "obj.frostmoon_spear",
            "obj.br_frostmoon_spear",
        )
