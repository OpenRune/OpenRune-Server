package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Smash rolls ordinary Crush accuracy and deals 25-125% of the wielder's normal maximum hit.
 * A non-zero impact lowers current Defence by 30%; the Bounty Hunter variant uses its 75% drain.
 */
class StatiusWarhammerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val standard = Smash(manager, defenceDrainPercent = 30)
        registerMelee("obj.statius_warhammer", standard)
        registerMelee("obj.br_statius_warhammer", standard)

        registerMelee(
            "obj.statius_warhammer_bh",
            Smash(manager, defenceDrainPercent = 75),
        )
    }

    private inner class Smash(
        private val manager: SpecialAttackManager,
        private val defenceDrainPercent: Int,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            smash(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            smash(target, attack)
            return true
        }

        private fun ProtectedAccess.smash(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.human_blunt_pound")
            spotanim(
                spot = "spotanim.statius_hammer_sa_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Crush,
                    multiplier = 1.0,
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
                    val minHit = normalMax / 4
                    val maxHit = normalMax + (normalMax / 4)
                    random.of(minHit..maxHit)
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value to gate the Defence drain on - no impact callback needed.
            if (damage > 0) {
                reduceDefence(target, defenceDrainPercent)
            }
            manager.continueCombat(this, target)
        }
    }

    private fun reduceDefence(target: PathingEntity, percent: Int) {
        when (target) {
            is Player -> {
                val drain = target.stat("stat.defence") * percent / 100
                if (drain > 0) {
                    target.statSub("stat.defence", constant = drain, percent = 0)
                }
            }
            is Npc -> {
                val drain = target.defenceLvl * percent / 100
                target.defenceLvl = (target.defenceLvl - drain).coerceAtLeast(0)
            }
        }
    }
}
