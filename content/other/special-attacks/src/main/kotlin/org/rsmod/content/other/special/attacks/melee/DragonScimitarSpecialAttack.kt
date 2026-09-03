package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.prayer.disableProtectionPrayers
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** Sever: a single normal-maximum hit with 125% accuracy that rolls against Slash defence. */
class DragonScimitarSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val sever = Sever(manager)
        registerMelee("obj.dragon_scimitar", sever)
        registerMelee("obj.dragon_scimitar_ornament", sever)
        registerMelee("obj.br_dragon_scimitar", sever)
        registerMelee("obj.bh_dragon_scimitar_corrupted", sever)
    }

    private class Sever(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            sever(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            sever(target, attack)
            return true
        }

        private fun ProtectedAccess.sever(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.sp_attack_dragon_scimitar")
            // Height and sound confirmed against a reference implementation of this exact
            // special (Zenyte-based Offline_Scape/Near Reality, SEVER in SpecialAttack.java:
            // `new Graphics(347, 0, 100)`, `player.sendSound(SEVER_SOUND)` = synth 2540,
            // unaliased in this cache).
            soundSynth(SEVER_SOUND)
            spotanim(
                spot = "spotanim.sp_attack_dragon_scimitar_trail_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.0,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            // Real OSRS doesn't clamp damage after the roll, so the already-known damage here is
            // the authentic value to gate the prayer-disable on - no impact callback needed.
            if (damage > 0) {
                (target as? Player)?.disableProtectionPrayers()
            }
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val SEVER_SOUND = 2540
    }
}
