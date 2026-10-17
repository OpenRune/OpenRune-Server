package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Feint makes one accuracy roll using the selected attack style against one quarter of the
 * target's Stab defence. A successful hit deals an inclusive 20-120% of the normal melee maximum
 * hit.
 *
 * The Vesta's longsword variants use the cache-mapped 25% special-attack energy requirement.
 */
class VestaLongswordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val feint = Feint(manager)
        registerMelee("obj.vestas_longsword", feint)
        registerMelee("obj.br_vestas_longsword", feint)
        registerMelee("obj.bh_vestas_longsword", feint)
        registerMelee("obj.vestas_longsword_bh", feint)
    }

    private class Feint(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = feint(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = feint(target, attack)

        private fun ProtectedAccess.feint(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            player.anim(VESTA_LONGSWORD_FEINT_ANIMATION)

            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Stab,
                    multiplier = 1.0,
                    defenceMultiplier = STAB_DEFENCE_MULTIPLIER,
                )
            val damage =
                if (successful) {
                    val normalMax =
                        manager.calculateMeleeMaxHit(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            multiplier = 1.0,
                        )
                    val range = VestaLongswordFeintDamage.range(normalMax)
                    VestaLongswordFeintDamage.resolveDamage(
                        range = range,
                        isMaxHit = player.adminMaxHit,
                        rollInclusive = { r -> random.of(r) },
                    )
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        private const val VESTA_LONGSWORD_FEINT_ANIMATION: String = "seq.human_dragon_sword_spec"
        private const val STAB_DEFENCE_MULTIPLIER: Double = 0.25
    }
}

internal object VestaLongswordFeintDamage {
    private const val MINIMUM_PERCENT: Int = 20
    private const val MAXIMUM_PERCENT: Int = 120

    /** Returns the inclusive 20-120% Feint damage range for [normalMax]. */
    fun range(normalMax: Int): IntRange {
        require(normalMax >= 0) { "Normal maximum hit must not be negative: $normalMax" }
        val minimum = normalMax.percentOf(MINIMUM_PERCENT)
        val maximum = normalMax.percentOf(MAXIMUM_PERCENT)
        return minimum..maximum
    }

    /** Forces the top of the range when `::maxhit` is active, same convention as elsewhere. */
    fun resolveDamage(range: IntRange, isMaxHit: Boolean, rollInclusive: (IntRange) -> Int): Int =
        when {
            range.first == range.last -> range.first
            isMaxHit -> range.last
            else -> rollInclusive(range)
        }

    private fun Int.percentOf(percent: Int): Int = (toLong() * percent / 100L).toInt()
}
