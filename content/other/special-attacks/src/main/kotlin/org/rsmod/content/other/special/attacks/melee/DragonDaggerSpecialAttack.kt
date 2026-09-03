package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.WeaponPoisonEffect
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj

/**
 * Puncture: two independent hits, each using 115% accuracy and maximum damage.
 *
 * The attacker's selected melee style is preserved, but the target always defends
 * using Slash defence.
 */
class DragonDaggerSpecialAttack @Inject constructor(private val poison: WeaponPoisonEffect) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val puncture = DragonDagger(manager, poison)
        registerMelee("obj.dragon_dagger", puncture)
        registerMelee("obj.dragon_dagger_p", puncture)
        registerMelee("obj.dragon_dagger_p+", puncture)
        registerMelee("obj.dragon_dagger_p++", puncture)
        registerMelee("obj.br_dragon_dagger", puncture)
        registerMelee("obj.bh_dragon_dagger_corrupted", puncture)
        registerMelee("obj.bh_dragon_dagger_p_corrupted", puncture)
        registerMelee("obj.bh_dragon_dagger_p+_corrupted", puncture)
        registerMelee("obj.bh_dragon_dagger_p++_corrupted", puncture)
    }

    private class DragonDagger(
        private val manager: SpecialAttackManager,
        private val poison: WeaponPoisonEffect,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            // Wiki: "There is a slight delay between the two hits when the special is used on
            // NPCs" - confirmed against a reference implementation of this exact special
            // (Zenyte-based Offline_Scape/Near Reality, PUNCTURE in SpecialAttack.java) as
            // exactly one tick, not the two this file had before.
            puncture(target, attack, secondHitDelay = 1)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            // Wiki: "When used against other players, both hits are applied simultaneously" -
            // same tick, not one tick apart like this file had before.
            puncture(target, attack, secondHitDelay = 0)
            return true
        }

        private fun ProtectedAccess.puncture(
            target: PathingEntity,
            attack: CombatAttack.Melee,
            secondHitDelay: Int,
        ) {
            anim("seq.puncture")
            // Height and sound confirmed against the same reference (`new Graphics(252, 0,
            // 100)`, `player.sendSound(PUNCTURE_SOUND)` = synth 2537, unaliased in this cache).
            soundSynth(PUNCTURE_SOUND)
            spotanim(
                spot = "spotanim.sp_attack_puncture_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val first =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.15,
                    maxHitMultiplier = 1.15,
                    blockType = MeleeAttackType.Slash,
                )
            val second =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.15,
                    maxHitMultiplier = 1.15,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second, delay = secondHitDelay)
            // Puncture's two hits are still ordinary weapon hits for poison purposes - a
            // dragon dagger(p)/(p+)/(p++) rolls its usual 25% chance independently on each one.
            attack.weapon?.let { weapon ->
                poison.rollOnMeleeHit(player, target, getInvObj(weapon), first)
                poison.rollOnMeleeHit(player, target, getInvObj(weapon), second)
            }
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val PUNCTURE_SOUND = 2537
    }
}
