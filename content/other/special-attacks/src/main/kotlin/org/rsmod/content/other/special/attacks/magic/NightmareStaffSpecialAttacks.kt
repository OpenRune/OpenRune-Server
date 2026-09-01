package org.rsmod.content.other.special.attacks.magic

import kotlin.math.min
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.magicLvl
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdd
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MagicSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * The Nightmare staff variants are spell-like, but their special attacks are built-in staff
 * attacks rather than autocast spells. They therefore intentionally use [CombatAttack.Staff]:
 * no spell runes or weapon charges are consumed.
 */
class NightmareStaffSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val immolate = Immolate(manager)
        registerMagic("obj.nightmare_staff_volatile", immolate)
        registerMagic("obj.br_nightmare_staff_volatile", immolate)
        registerMagic("obj.deadman_blighted_volatile_staff", immolate)
        registerMagic("obj.deadman_nightmare_staff_volatile", immolate)

        registerMagic("obj.nightmare_staff_eldritch", Invocate(manager))
    }

    private class Immolate(private val manager: SpecialAttackManager) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            immolate(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            immolate(target, attack)
            return true
        }

        private fun ProtectedAccess.immolate(
            target: PathingEntity,
            attack: CombatAttack.Staff,
        ) {
            anim(VOLATILE_CAST_ANIM)
            spotanim(
                spot = VOLATILE_CAST_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                height = SPOTANIM_HEIGHT,
            )
            target.spotanim(
                spot = VOLATILE_HIT_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                height = SPOTANIM_HEIGHT,
            )

            val successful =
                manager.rollStaffAccuracy(
                    source = this,
                    target = target,
                    attackStyle = attack.style,
                    multiplier = VOLATILE_ACCURACY_MULTIPLIER,
                )
            val damage =
                if (successful) {
                    manager.rollStaffMaxHit(
                        source = this,
                        target = target,
                        baseMaxHit = NightmareStaffSpecialDamage.volatileBaseMaxHit(player.magicLvl),
                        multiplier = 1.0,
                    )
                } else {
                    0
                }

            statAdvance("stat.magic", SPECIAL_CAST_XP)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = 0)
            manager.continueCombat(this, target)
        }
    }

    private class Invocate(private val manager: SpecialAttackManager) : MagicSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Staff,
        ): Boolean {
            invocate(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Staff,
        ): Boolean {
            invocate(target, attack)
            return true
        }

        private fun ProtectedAccess.invocate(
            target: PathingEntity,
            attack: CombatAttack.Staff,
        ) {
            anim(ELDRITCH_CAST_ANIM)
            spotanim(
                spot = ELDRITCH_CAST_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                height = SPOTANIM_HEIGHT,
            )
            target.spotanim(
                spot = ELDRITCH_HIT_SPOTANIM,
                slot = constants.spotanim_slot_combat,
                height = SPOTANIM_HEIGHT,
            )

            val successful =
                manager.rollStaffAccuracy(
                    source = this,
                    target = target,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val rawDamage =
                if (successful) {
                    manager.rollStaffMaxHit(
                        source = this,
                        target = target,
                        baseMaxHit = NightmareStaffSpecialDamage.eldritchBaseMaxHit(player.magicLvl),
                        multiplier = 1.0,
                    )
                } else {
                    0
                }

            val source = player
            statAdvance("stat.magic", SPECIAL_CAST_XP)
            manager.giveCombatXp(this, target, attack, rawDamage)
            manager.queueMagicHit(source = this, target = target, damage = rawDamage, clientDelay = 0)
            // Real OSRS doesn't clamp damage after the roll, so the already-known pre-mitigation
            // rawDamage is the authentic value to restore prayer from - no impact callback needed.
            if (rawDamage > 0) {
                restoreInvocatePrayer(source, rawDamage)
            }
            manager.continueCombat(this, target)
        }

        private fun restoreInvocatePrayer(source: Player, rawDamage: Int) {
            val restore = rawDamage / 2
            if (restore <= 0) {
                return
            }
            val current = source.stat("stat.prayer")
            val added = min(ELDRITCH_PRAYER_CAP, current + restore) - current
            if (added > 0) {
                source.statAdd("stat.prayer", constant = added, percent = 0)
            }
        }
    }

    private companion object {
        const val VOLATILE_ACCURACY_MULTIPLIER = 1.5
        const val SPECIAL_CAST_XP = 10.0
        const val ELDRITCH_PRAYER_CAP = 120
        const val SPOTANIM_HEIGHT = 96

        const val VOLATILE_CAST_ANIM = "seq.nightmare_staff_special"
        const val VOLATILE_CAST_SPOTANIM = "spotanim.nightmare_staff_volatile_cast_spotanim"
        const val VOLATILE_HIT_SPOTANIM = "spotanim.nightmare_staff_volatile_hit_spotanim"

        const val ELDRITCH_CAST_ANIM = "seq.nightmare_staff_special"
        const val ELDRITCH_CAST_SPOTANIM = "spotanim.nightmare_staff_eldritch_cast_spotanim"
        const val ELDRITCH_HIT_SPOTANIM = "spotanim.nightmare_staff_eldritch_hit_spotanim"
    }
}

/** Base spell hit before the staff's normal magic-damage bonuses are applied. */
internal object NightmareStaffSpecialDamage {
    fun volatileBaseMaxHit(visibleMagicLevel: Int): Int =
        scaledBaseMaxHit(visibleMagicLevel, VOLATILE_BASE_MAX_HIT)

    fun eldritchBaseMaxHit(visibleMagicLevel: Int): Int =
        scaledBaseMaxHit(visibleMagicLevel, ELDRITCH_BASE_MAX_HIT)

    private fun scaledBaseMaxHit(visibleMagicLevel: Int, ceiling: Int): Int =
        ((ceiling * visibleMagicLevel.coerceAtLeast(0)) / MAX_MAGIC_LEVEL + 1).coerceAtMost(ceiling)

    private const val MAX_MAGIC_LEVEL = 99
    private const val VOLATILE_BASE_MAX_HIT = 58
    private const val ELDRITCH_BASE_MAX_HIT = 44
}
