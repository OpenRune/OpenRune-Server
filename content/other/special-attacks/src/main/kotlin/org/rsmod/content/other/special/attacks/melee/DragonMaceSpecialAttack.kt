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

/** Shatter: a single Crush hit with 125% accuracy and 150% maximum damage. */
class DragonMaceSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val shatter = DragonMace(manager)
        registerMelee("obj.dragon_mace", shatter)
        registerMelee("obj.bh_dragon_mace_imbue", shatter)
        registerMelee("obj.bh_dragon_mace_corrupted", shatter)
    }

    private class DragonMace(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            shatter(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            shatter(target, attack)
            return true
        }

        private fun ProtectedAccess.shatter(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.shatter")
            // Height and sound confirmed against a reference implementation of this exact
            // special (Zenyte-based Offline_Scape/Near Reality, SHATTER in SpecialAttack.java:
            // `new Graphics(251, 0, 100)`, `player.sendSound(SHATTER_SOUND)` = synth 2541,
            // unaliased in this cache).
            soundSynth(SHATTER_SOUND)
            spotanim(
                spot = "spotanim.sp_attack_shatter_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.25,
                    maxHitMultiplier = 1.5,
                    blockType = MeleeAttackType.Crush,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val SHATTER_SOUND = 2541
    }
}
