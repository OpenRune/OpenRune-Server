package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class DragonClawsSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.dragon_claws", DragonClaws(manager))
        registerMelee("obj.bh_dragon_claws_corrupted", DragonClaws(manager))
        registerMelee("obj.br_dragon_claws", DragonClaws(manager))
        registerMelee("obj.dragon_claws_ornament", DragonClaws(manager))
        registerMelee("obj.deadman_blighted_dragon_claws", DragonClaws(manager))
    }

    /**
     * "Slice and Dice" - four hits in succession. Each hit rolls accuracy independently (same
     * odds as a normal hit) until one connects; once a hit lands, every remaining hit is
     * guaranteed to also land, with damage cascading down from whichever hit was first to
     * connect. If all four accuracy rolls fail, there's still a small chance of "sympathy"
     * damage.
     *
     * Damage ranges and chaining validated against every worked example on the wiki (e.g.
     * 35-17-8-9, 0-30-15-16, 0-0-22-23, 0-0-0-46) and the four listed all-miss patterns.
     *
     * @see <a href="https://oldschool.runescape.wiki/w/Dragon_claws#Special_attack">Dragon claws - Special attack</a>
     */
    private class DragonClaws(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            sliceAndDice(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            sliceAndDice(target, attack)
            return true
        }

        private fun ProtectedAccess.sliceAndDice(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.human_dragon_claws_spec")
            spotanim(
                spot = "spotanim.dragon_claws_spot",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )

            val hits = rollHits(target, attack, maxHit)

            // The wiki's own "Sound effects" table for this item names four distinct clips -
            // one per swing beat, third and fourth sharing a clip - not the single generic sound
            // this previously played (2537, wrong: that's Dragon dagger/Abyssal dagger's own
            // Puncture sound, confirmed dead wrong live). Every beat plays regardless of whether
            // that swing's roll actually connected, same as `hits` itself always has 4 entries.
            //
            // All 4 calls land in the same server tick, and `soundSynth`'s `delay` is exactly
            // what the packet has for this: without it, only the first of several same-tick
            // synth sounds actually plays client-side (confirmed live - the 1/2/3/3 clips were
            // firing but silently collapsing into just the first). Staggered using this
            // codebase's own existing per-hit-sound delay convention (`StandardPlayerHitProcessor`
            // uses 20 for a single hit-reaction sound), one unit of spacing per swing beat.
            var totalDamage = 0
            for ((index, damage) in hits.withIndex()) {
                totalDamage += damage
                soundSynth(sliceAndDiceSound(index), delay = index * HIT_SOUND_SPACING)
                manager.queueMeleeHit(this, target, damage)
            }
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.continueCombat(this, target)
        }

        private fun sliceAndDiceSound(hitIndex: Int): Int =
            when (hitIndex) {
                0 -> DRAGONCLAWS_SPECIAL_1_SOUND
                1 -> DRAGONCLAWS_SPECIAL_2_SOUND
                else -> DRAGONCLAWS_SPECIAL_3_SOUND
            }

        /**
         * Whichever hit is first to connect stands in for every hit that would have preceded it,
         * so the earlier it lands, the wider its damage range. Everything after it is a
         * guaranteed hit (no further accuracy rolls) that roughly halves each time, except the
         * final hit in the chain, which gets a `+1` bump instead of being halved again.
         */
        private fun ProtectedAccess.rollHits(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            maxHit: Int,
        ): IntArray =
            DragonClawsDamage.rollHits(
                maxHit = maxHit,
                rollAccuracy = { rollAccuracy(target, attack) },
                rollRange = { range -> rollRange(range.first, range.last) },
                rollSympathyTriggers = { random.of(3) < 2 },
                rollSympathyPattern = { random.of(4) },
            )

        private fun ProtectedAccess.rollAccuracy(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean =
            manager.rollMeleeAccuracy(
                source = this,
                target = target,
                attackType = attack.type,
                attackStyle = attack.style,
                blockType = MeleeAttackType.Slash,
                multiplier = 1.0,
            )

        /**
         * Inclusive random range, safely handling a lower bound of `0` or below. Rolled directly
         * with [random] rather than through `manager.rollMeleeDamage` (the multi-hit split has no
         * single call that fits), so it has to check `adminMaxHit` itself - the same convention
         * every `PlayerAttackManager` roll function uses (return the top of the range instead of
         * rolling) - or `::maxhit` silently does nothing for this special.
         */
        private fun ProtectedAccess.rollRange(min: Int, max: Int): Int =
            DragonClawsDamage.resolveRange(
                min = min,
                max = max,
                isMaxHit = player.adminMaxHit,
                rollInclusive = { lo, hi -> random.of(lo, hi) },
                rollUpTo = { exclusiveBound -> random.of(maxExclusive = exclusiveBound) },
            )

        private companion object {
            // Sourced directly from the Dragon claws wiki page's own "Sound effects" table
            // (Name/Description/ID), not the reference port - unaliased in this cache's
            // gamevals, no `synth.` name exists for any of them.
            const val DRAGONCLAWS_SPECIAL_1_SOUND = 4138
            const val DRAGONCLAWS_SPECIAL_2_SOUND = 4140
            const val DRAGONCLAWS_SPECIAL_3_SOUND = 4141

            /** No wiki-confirmed exact timing exists for these four beats; matches the spacing
             * already used elsewhere in this codebase for a single hit-reaction sound
             * (`StandardPlayerHitProcessor`'s `defendSound, delay = 20`). */
            const val HIT_SOUND_SPACING = 20
        }
    }
}

/**
 * Pure damage distribution for Slice and Dice's four-hit cascade, kept separate from
 * [ProtectedAccess] so the arithmetic can be unit tested directly against the wiki's worked
 * examples instead of only through a live combat roll.
 */
internal object DragonClawsDamage {
    fun rollHits(
        maxHit: Int,
        rollAccuracy: () -> Boolean,
        rollRange: (IntRange) -> Int,
        rollSympathyTriggers: () -> Boolean,
        rollSympathyPattern: () -> Int,
    ): IntArray {
        val hits = IntArray(4)
        when {
            rollAccuracy() -> {
                hits[0] = rollRange(maxHit / 2..maxHit - 1)
                hits[1] = hits[0] / 2
                hits[2] = hits[1] / 2
                hits[3] = hits[2] + 1
            }
            rollAccuracy() -> {
                hits[1] = rollRange(((maxHit * 3) / 8)..((maxHit * 7) / 8))
                hits[2] = hits[1] / 2
                hits[3] = hits[2] + 1
            }
            rollAccuracy() -> {
                hits[2] = rollRange((maxHit / 4)..((maxHit * 3) / 4))
                hits[3] = hits[2] + 1
            }
            rollAccuracy() -> {
                hits[3] = rollRange((maxHit / 4)..((maxHit * 5) / 4))
            }
            else -> {
                // All four rolls missed: ~2/3 chance of 2 total "sympathy" damage split across
                // two of the four hits, ~1/3 chance of a clean 0-0-0-0.
                if (rollSympathyTriggers()) {
                    when (rollSympathyPattern()) {
                        0 -> {
                            hits[0] = 1
                            hits[1] = 1
                        }
                        1 -> {
                            hits[2] = 1
                            hits[3] = 1
                        }
                        2 -> {
                            hits[0] = 1
                            hits[2] = 1
                        }
                        else -> {
                            hits[1] = 1
                            hits[3] = 1
                        }
                    }
                }
            }
        }
        return hits
    }

    /** Inclusive random range, safe against a lower bound of `0` or below, honoring `::maxhit`. */
    fun resolveRange(
        min: Int,
        max: Int,
        isMaxHit: Boolean,
        rollInclusive: (Int, Int) -> Int,
        rollUpTo: (Int) -> Int,
    ): Int {
        val safeMax = max.coerceAtLeast(0)
        val safeMin = min.coerceIn(0, safeMax)
        if (isMaxHit) {
            return safeMax
        }
        return if (safeMin <= 0) {
            rollUpTo(safeMax + 1)
        } else {
            rollInclusive(safeMin, safeMax)
        }
    }
}
