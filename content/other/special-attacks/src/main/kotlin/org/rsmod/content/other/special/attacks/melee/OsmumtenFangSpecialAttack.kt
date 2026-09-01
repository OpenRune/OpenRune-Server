package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Eviscerate rolls against Stab defence with 50% extra accuracy and uses the Fang's true maximum
 * hit rather than its normal 85% damage cap.
 */
class OsmumtenFangSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val eviscerate = Eviscerate(manager)
        registerMelee("obj.osmumtens_fang", eviscerate)
        registerMelee("obj.osmumtens_fang_ornament", eviscerate)
        registerMelee("obj.br_osmumtens_fang", eviscerate)
    }

    private class Eviscerate(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            eviscerate(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            eviscerate(target, attack)
            return true
        }

        private fun ProtectedAccess.eviscerate(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.weapon_sword_osmumten03_special")
            // Same blind height=96-copied-from-Dragon-claws pattern fixed on several other
            // specials this session - reported too high live, dropped to ground level.
            spotanim(
                spot = "spotanim.spotanim_weapon_sword_osmumten_special",
                height = 0,
            )

            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Stab,
                    multiplier = 1.5,
                )
            val damage =
                if (successful) {
                    val maxHit =
                        manager.calculateMeleeMaxHit(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            multiplier = 1.0,
                        )
                    random.of(OsmumtenFangDamage.minHit(maxHit)..maxHit)
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }
}

/**
 * Pure Eviscerate math, kept separate from [ProtectedAccess] so it can be unit tested directly
 * against the wiki's own worked example (true max hit 60 rolls 9-60) instead of only through a
 * live combat roll.
 */
internal object OsmumtenFangDamage {
    /** Eviscerate uses the true (uncapped) max hit, with a 15%-of-max floor, minimum 1. */
    fun minHit(maxHit: Int): Int = (maxHit * 15 / 100).coerceAtLeast(1)
}
