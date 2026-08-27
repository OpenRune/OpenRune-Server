package org.rsmod.content.other.special.attacks.melee

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

class DragonClawsSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.dragon_claws", DragonClaws(manager))
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
                height = 96,
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

            var totalDamage = 0
            for (damage in hits) {
                totalDamage += damage
                manager.queueMeleeHit(this, target, damage)
            }
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.continueCombat(this, target)
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
        ): IntArray {
            val hits = IntArray(4)
            when {
                rollAccuracy(target, attack) -> {
                    hits[0] = rollRange(maxHit / 2, maxHit - 1)
                    hits[1] = hits[0] / 2
                    hits[2] = hits[1] / 2
                    hits[3] = hits[2] + 1
                }
                rollAccuracy(target, attack) -> {
                    hits[1] = rollRange((maxHit * 3) / 8, (maxHit * 7) / 8)
                    hits[2] = hits[1] / 2
                    hits[3] = hits[2] + 1
                }
                rollAccuracy(target, attack) -> {
                    hits[2] = rollRange(maxHit / 4, (maxHit * 3) / 4)
                    hits[3] = hits[2] + 1
                }
                rollAccuracy(target, attack) -> {
                    hits[3] = rollRange(maxHit / 4, (maxHit * 5) / 4)
                }
                else -> {
                    // All four rolls missed: ~2/3 chance of 2 total "sympathy" damage split
                    // across two of the four hits, ~1/3 chance of a clean 0-0-0-0.
                    if (random.of(3) < 2) {
                        when (random.of(4)) {
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

        /** Inclusive random range, safely handling a lower bound of `0` or below. */
        private fun ProtectedAccess.rollRange(min: Int, max: Int): Int {
            val safeMax = max.coerceAtLeast(0)
            val safeMin = min.coerceIn(0, safeMax)
            return if (safeMin <= 0) {
                random.of(maxExclusive = safeMax + 1)
            } else {
                random.of(safeMin, safeMax)
            }
        }
    }
}
