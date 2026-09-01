package org.rsmod.content.other.special.attacks.melee

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.SoulreaperStackDecay
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Behead consumes the axe's Soul Stacks rather than ordinary special-attack energy. The cache
 * marks its energy requirement as specialised, so this map owns the stack validation and debit.
 */
class SoulreaperAxeSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val behead = Behead(manager)
        registerMelee("obj.soulreaper", behead)
        registerMelee("obj.soulreaper_axe_orn", behead)
    }

    private class Behead(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = behead(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = behead(target, attack)

        private fun ProtectedAccess.behead(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            val stacks = vars[SOUL_STACKS].coerceIn(0, MAX_SOUL_STACKS)
            if (stacks == 0) {
                mes("You need at least one Soul Stack to use this special attack.")
                manager.stopCombat(this)
                return false
            }

            // Behead has one human animation. The normal and ornamented axes differ only in the
            // revision-240 combat graphic.
            val ornamented = player.righthand?.id == ORNAMENTED_SOULREAPER_ID
            anim(BEHEAD_SEQUENCE)
            spotanim(
                spot = if (ornamented) ORNAMENTED_BEHEAD_SPOTANIM else BEHEAD_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                // 96 (blindly copied from Dragon claws) still sat too high even at 48 per live
                // feedback - dropped to ground level. Visual-tuning guess, not verified against a
                // real screenshot; needs your eyes to confirm.
                height = 0,
            )

            // The normal strength bonus is removed first. Behead applies its own damage and
            // accuracy bonus from the consumed count, rather than doubling the active stacks.
            SoulreaperStackDecay.clear(player)
            VarPlayerIntMapSetter.set(player, SOUL_STACKS, 0)
            statHeal("stat.hitpoints", constant = stacks * HEAL_PER_STACK, percent = 0)

            // Wiki: 12% accuracy and 6% max/min damage per stack - not the same percentage for
            // both (5 stacks = 60% accuracy, 30% max hit, and a *minimum* damage of 30% of that
            // boosted max hit). The minimum floor isn't expressible via the standard
            // rollMeleeDamage (always rolls 1..maxHit), so this rolls it directly, same pattern
            // as Dragon claws' rollRange - including respecting adminMaxHit for testability.
            val accuracyMultiplier = SoulreaperAxeDamage.accuracyMultiplier(stacks)
            val damageMultiplier = SoulreaperAxeDamage.damageMultiplier(stacks)
            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = accuracyMultiplier,
                )
            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = damageMultiplier,
                )
            val damage =
                SoulreaperAxeDamage.resolveDamage(
                    successful = successful,
                    maxHit = maxHit,
                    stacks = stacks,
                    isMaxHit = player.adminMaxHit,
                    rollInclusive = { lo, hi -> random.of(lo, hi) },
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val SOUL_STACKS = "varp.soulreaper_stacks"
        const val MAX_SOUL_STACKS = 5
        val ORNAMENTED_SOULREAPER_ID = "obj.soulreaper_axe_orn".asRSCM(RSCMType.OBJ)
        const val BEHEAD_SEQUENCE = "seq.ancient_axe_special"
        const val BEHEAD_SPOTANIM = "spotanim.ancient_axe_special_spotanim"
        const val ORNAMENTED_BEHEAD_SPOTANIM = "spotanim.ancient_axe_special_spotanim_orn"
        const val HEAL_PER_STACK = 8
    }
}

/**
 * Pure damage math for Behead, kept separate from [ProtectedAccess] so the per-stack scaling and
 * the minimum-damage floor can be unit tested directly against the wiki's numbers.
 */
internal object SoulreaperAxeDamage {
    fun accuracyMultiplier(stacks: Int): Double = 1.0 + (stacks * ACCURACY_BONUS_PER_STACK)

    fun damageMultiplier(stacks: Int): Double = 1.0 + (stacks * DAMAGE_BONUS_PER_STACK)

    /** 6% of the (already-boosted) max hit per stack - 30% of it at the full 5 stacks. */
    fun minimumDamage(maxHit: Int, stacks: Int): Int = (maxHit * stacks * DAMAGE_BONUS_PER_STACK).toInt()

    fun resolveDamage(
        successful: Boolean,
        maxHit: Int,
        stacks: Int,
        isMaxHit: Boolean,
        rollInclusive: (Int, Int) -> Int,
    ): Int {
        if (!successful) {
            return 0
        }
        return if (isMaxHit) {
            maxHit
        } else {
            rollInclusive(minimumDamage(maxHit, stacks), maxHit)
        }
    }

    const val ACCURACY_BONUS_PER_STACK = 0.12
    const val DAMAGE_BONUS_PER_STACK = 0.06
}
