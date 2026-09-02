package org.rsmod.content.other.special.attacks.melee

import dev.openrune.util.Wearpos
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
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.isAnyType
import org.rsmod.game.inv.isType

/**
 * Blood Infusion requires the full Blood Moon set. It pays a quarter of the wielder's current
 * Hitpoints, then performs the macuahuitl's two hits without the normal sequential accuracy gate.
 * Both the minimum and maximum total damage are raised by 25%; any landed hit guarantees the
 * Bloodrager one-tick attack-speed benefit for the following attack.
 */
class DualMacuahuitlSpecialAttack @Inject constructor(private val random: GameRandom) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val bloodInfusion = BloodInfusion(manager, random)
        registerMelee("obj.dual_macuahuitl", bloodInfusion)
        registerMelee("obj.br_dual_macuahuitl", bloodInfusion)
    }

    private class BloodInfusion(
        private val manager: SpecialAttackManager,
        private val random: GameRandom,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = bloodInfusion(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = bloodInfusion(target, attack)

        private fun ProtectedAccess.bloodInfusion(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            if (!player.isWearingBloodMoonSet()) {
                mes("You need to be wearing the full Blood Moon armour set to use this special attack.")
                manager.stopCombat(this)
                return false
            }

            val selfDamage = stat("stat.hitpoints") / SELF_DAMAGE_DIVISOR
            if (selfDamage > 0) {
                takeInstantHit(type = HitType.Typeless, damage = selfDamage)
            }

            anim("seq.pmoon_macuahuitl_crush")
            spotanim(
                spot = "spotanim.special_dual_macuahuitl_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val normalMax =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val hits =
                DualMacuahuitlDamage.roll(
                    normalMax = normalMax,
                    rollAccuracy = {
                        manager.rollMeleeAccuracy(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            blockType = attack.type,
                            multiplier = 1.0,
                        )
                    },
                    rollDamage = { range ->
                        DualMacuahuitlDamage.resolveDamage(
                            range = range,
                            isMaxHit = player.adminMaxHit,
                            rollRandom = { r ->
                                when {
                                    r.first <= 0 -> random.of(r.last + 1)
                                    r.first == r.last -> r.first
                                    else -> random.of(r)
                                }
                            },
                        )
                    },
                )

            val totalDamage = hits.sum()
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.queueMeleeHit(this, target, hits[0], delay = 1)
            manager.queueMeleeHit(this, target, hits[1], delay = 2)

            // Dual macuahuitls have a four-tick attack rate. Bloodrager advances the next attack
            // by one tick whenever either special hit lands.
            if (hits.any { it > 0 }) {
                manager.setNextAttackDelay(this, BLOODRAGER_ATTACK_DELAY)
            }
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val SELF_DAMAGE_DIVISOR = 4
        const val BLOODRAGER_ATTACK_DELAY = 3
    }
}

/** Pure damage distribution for Blood Infusion's independent two-hit sequence. */
internal object DualMacuahuitlDamage {
    fun roll(
        normalMax: Int,
        rollAccuracy: () -> Boolean,
        rollDamage: (IntRange) -> Int,
    ): IntArray {
        val minimum = (normalMax * MINIMUM_PERCENT + 99) / 100
        val maximum = normalMax * MAXIMUM_PERCENT / 100
        return intArrayOf(
            rollHit(splitLower(minimum), splitLower(maximum), rollAccuracy, rollDamage),
            rollHit(splitUpper(minimum), splitUpper(maximum), rollAccuracy, rollDamage),
        )
    }

    private fun rollHit(
        minimum: Int,
        maximum: Int,
        rollAccuracy: () -> Boolean,
        rollDamage: (IntRange) -> Int,
    ): Int = if (!rollAccuracy()) 0 else rollDamage(minimum..maximum)

    /** Forces the top of the range when `::maxhit` is active, same convention as elsewhere. */
    fun resolveDamage(range: IntRange, isMaxHit: Boolean, rollRandom: (IntRange) -> Int): Int =
        when {
            range.last <= 0 -> 0
            isMaxHit -> range.last
            else -> rollRandom(range)
        }

    private fun splitLower(value: Int): Int = value / 2

    private fun splitUpper(value: Int): Int = value - splitLower(value)

    private const val MINIMUM_PERCENT = 25
    private const val MAXIMUM_PERCENT = 125
}

private fun Player.isWearingBloodMoonSet(): Boolean =
    worn[Wearpos.Hat.slot]?.isType("obj.blood_moon_helm") == true &&
        worn[Wearpos.Torso.slot]?.isType("obj.blood_moon_chestplate") == true &&
        worn[Wearpos.Legs.slot]?.isType("obj.blood_moon_tassets") == true &&
        worn[Wearpos.RightHand.slot]?.isAnyType("obj.dual_macuahuitl", "obj.br_dual_macuahuitl") == true
