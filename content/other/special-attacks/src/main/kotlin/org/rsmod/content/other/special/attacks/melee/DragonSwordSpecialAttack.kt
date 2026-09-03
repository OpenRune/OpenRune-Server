package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.hit.modifier.BypassProtectionPrayerPlayerHitModifier
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** Wild Stab: a single hit with 125% accuracy and maximum damage against Stab defence. */
class DragonSwordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val wildStab = WildStab(manager)

        // The normal Dragon sword retains the historic `dragon_shortsword` cache alias.
        registerMelee("obj.dragon_shortsword", wildStab)
        registerMelee("obj.br_dragon_sword", wildStab)
        registerMelee("obj.bh_dragon_shortsword_corrupted", wildStab)
    }

    private class WildStab(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            wildStab(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            wildStab(target, attack)
            return true
        }

        private fun ProtectedAccess.wildStab(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.human_dragon_sword_spec")
            // Confirmed against a reference implementation of this exact special (Zenyte-based
            // Offline_Scape/Near Reality, WILD_STAB in SpecialAttack.java). Unaliased in this
            // cache's gamevals.
            soundSynth(WILD_STAB_SOUND)
            spotanim(
                spot = "spotanim.dragon_sword_spec_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.25,
                    blockType = MeleeAttackType.Stab,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(
                source = this,
                target = target,
                damage = damage,
                modifier = BypassProtectionPrayerPlayerHitModifier,
            )
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val WILD_STAB_SOUND = 3552
    }
}
