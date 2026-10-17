package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Backstab makes a normal-damage strike that cannot miss when this player was not the target's
 * most recent source of positive final damage. A landed hit drains the final damage dealt from
 * Defence, but only while the target has not already been drained below its base Defence level.
 */
class BoneDaggerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val backstab = Backstab(manager)
        registerMelee("obj.dttd_bone_dagger", backstab)
        registerMelee("obj.dttd_bone_dagger_p", backstab)
        registerMelee("obj.dttd_bone_dagger_p+", backstab)
        registerMelee("obj.dttd_bone_dagger_p++", backstab)
    }

    private class Backstab(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            backstab(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            backstab(target, attack)
            return true
        }

        private fun ProtectedAccess.backstab(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.dttd_player_stab_bone_dagger")
            spotanim(
                spot = "spotanim.dttd_dagger_sp_attack_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val successful =
                BoneDaggerBackstab.isUnsuspecting(
                    lastDamagingPlayerUuid = target.lastDamagingPlayerUuid,
                    sourceUuid = player.uuid,
                ) ||
                    manager.rollMeleeAccuracy(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        blockType = MeleeAttackType.Stab,
                        multiplier = 1.0,
                    )
            val damage =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.0,
                    )
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(source = this, target = target, damage = damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known pre-clamp
            // damage is the authentic value for the Defence drain - no impact callback needed.
            if (damage > 0) {
                reduceBoneDaggerDefence(target, damage)
            }
            manager.continueCombat(this, target)
        }
    }
}

internal object BoneDaggerBackstab {
    fun isUnsuspecting(
        lastDamagingPlayerUuid: Long?,
        sourceUuid: Long?,
    ): Boolean = sourceUuid == null || lastDamagingPlayerUuid != sourceUuid
}

private fun reduceBoneDaggerDefence(
    target: PathingEntity,
    damage: Int,
) {
    when (target) {
        is Player -> {
            if (target.stat("stat.defence") >= target.statBase("stat.defence")) {
                target.statSub("stat.defence", constant = damage, percent = 0)
            }
        }
        is Npc -> {
            if (target.defenceLvl >= target.baseDefenceLvl) {
                target.defenceLvl = (target.defenceLvl - damage).coerceAtLeast(0)
            }
        }
    }
}
