package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.BurnEffectService
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Burning Barrage makes up to three accuracy rolls. The first successful roll controls both the
 * total-damage range and the independent burn chance of all three hitsplats.
 */
class BurningClawsSpecialAttack
@Inject
constructor(
    private val burns: BurnEffectService,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val barrage = BurningBarrage(manager, burns)
        // The live cache retains the original "bone claws" aliases for both standard and BR items.
        registerMelee("obj.bone_claws", barrage)
        registerMelee("obj.br_bone_claws", barrage)
    }

    private class BurningBarrage(
        private val manager: SpecialAttackManager,
        private val burns: BurnEffectService,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            burningBarrage(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            burningBarrage(target, attack)
            return true
        }

        private fun ProtectedAccess.burningBarrage(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.human_weapon_burning_claws_02_spec")
            // Wiki-sourced ("burning_claws_swipe_01"), unaliased in this cache's gamevals.
            soundSynth(BURNING_BARRAGE_SOUND)
            spotanim(
                spot = "spotanim.vfx_burning_claws_spec_02",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val result =
                BurningClawDamage.roll(
                    maxHit =
                        manager.calculateMeleeMaxHit(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            multiplier = 1.0,
                        ),
                    rollAccuracy = {
                        manager.rollMeleeAccuracy(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            // Burning claws use the selected Slash or Stab attack style.
                            blockType = attack.type,
                            multiplier = 1.0,
                        )
                    },
                    rollInclusive = { range -> random.of(range) },
                    rollExclusive = { maximum -> random.of(maximum) },
                )

            manager.giveCombatXp(this, target, attack, result.hits.sum())
            result.hits.forEach { hit -> manager.queueMeleeHit(this, target, hit) }

            // The three chances are independent and apply to successful hitsplats, including a
            // guaranteed zero-value hitsplat in the second and third accuracy tiers.
            repeat(BurningClawDamage.HIT_COUNT) {
                if (random.of(100) < result.burnChancePercent) {
                    burns.apply(source = player, target = target)
                }
            }

            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val BURNING_BARRAGE_SOUND = 9316
    }
}

/**
 * Exact Burning Barrage damage construction. Percentage operations and all individual hitsplats
 * are intentionally truncated, matching the live special attack.
 */
internal object BurningClawDamage {
    internal const val HIT_COUNT: Int = 3

    internal data class Result(
        val hits: IntArray,
        val burnChancePercent: Int,
    )

    /**
     * Evaluates accuracy until the first success. A successful tier produces all three hitsplats
     * and gives each one the tier's independent burn chance.
     */
    fun roll(
        maxHit: Int,
        rollAccuracy: () -> Boolean,
        rollInclusive: (IntRange) -> Int,
        rollExclusive: (Int) -> Int,
    ): Result {
        for (tier in 0 until HIT_COUNT) {
            if (!rollAccuracy()) {
                continue
            }

            val total = roll(range(maxHit.coerceAtLeast(0), tier), rollInclusive)
            return Result(
                hits = split(total, tier),
                burnChancePercent = burnChance(tier),
            )
        }
        return Result(hits = misses(rollExclusive), burnChancePercent = 0)
    }

    private fun range(maxHit: Int, tier: Int): IntRange =
        when (tier) {
            0 -> (maxHit * 3 / 4)..(maxHit * 7 / 4)
            1 -> (maxHit / 2)..(maxHit * 3 / 2)
            2 -> (maxHit / 4)..(maxHit * 5 / 4)
            else -> error("Expected an accuracy tier in [0, 2], got $tier")
        }

    private fun roll(
        range: IntRange,
        rollInclusive: (IntRange) -> Int,
    ): Int =
        if (range.first == range.last) {
            range.first
        } else {
            rollInclusive(range)
        }

    private fun split(total: Int, tier: Int): IntArray =
        when (tier) {
            0 -> intArrayOf(total / 4, total / 4, total / 2)
            1 ->
                intArrayOf(
                    (total / 2 - 1).coerceAtLeast(0),
                    (total / 2 - 1).coerceAtLeast(0),
                    2,
                )
            2 -> intArrayOf(1, 1, (total - 2).coerceAtLeast(0))
            else -> error("Expected an accuracy tier in [0, 2], got $tier")
        }

    private fun burnChance(tier: Int): Int =
        when (tier) {
            0 -> 15
            1 -> 30
            2 -> 45
            else -> error("Expected an accuracy tier in [0, 2], got $tier")
        }

    private fun misses(rollExclusive: (Int) -> Int): IntArray =
        when (rollExclusive(5)) {
            0 -> intArrayOf(0, 0, 0)
            1, 2 -> intArrayOf(0, 0, 1)
            3, 4 -> intArrayOf(0, 0, 2)
            else -> error("Random source returned a value outside [0, 4]")
        }
}
