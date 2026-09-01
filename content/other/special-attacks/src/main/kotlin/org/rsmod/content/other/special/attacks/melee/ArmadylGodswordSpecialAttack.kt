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

/** The Armadyl godsword's standard special: a single accurate, high-damage Slash hit. */
class ArmadylGodswordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val standard = ArmadylGodsword(manager, "seq.ags_special_player", STANDARD_SPOTANIM)
        registerMelee("obj.ags", standard)
        registerMelee("obj.br_ags", standard)
        registerMelee("obj.deadman_ags", standard)
        registerMelee(
            "obj.agsg",
            ArmadylGodsword(manager, "seq.ags_special_ornate_player", ORNATE_SPOTANIM),
        )
        registerMelee(
            "obj.deadman_blighted_ags",
            ArmadylGodsword(manager, "seq.ags_special_blighted_player", STANDARD_SPOTANIM),
        )
    }

    private class ArmadylGodsword(
        private val manager: SpecialAttackManager,
        private val sequence: String,
        private val graphic: String,
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
            anim(sequence)
            target.spotanim(
                spot = graphic,
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 2.0,
                    maxHitMultiplier = 1.375,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }

    private companion object {
        const val STANDARD_SPOTANIM = "spotanim.dh_sword_update_armadyl_special_spotanim"
        const val ORNATE_SPOTANIM = "spotanim.armadyl_special_spotanim_gold"
    }
}
