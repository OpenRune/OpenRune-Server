package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.accuracy.melee.ReducedMeleeDefenceAccuracy
import org.rsmod.api.config.constants
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

class AncientWarriorSpecialAttacks @Inject constructor(
    private val accuracy: ReducedMeleeDefenceAccuracy,
    private val damage: MeleeSpecialDamage,
    private val rng: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val feint = Strike(manager, true, 0)
        LONGSWORDS.forEach { registerMelee(it, feint) }
        HAMMERS.forEach { registerMelee(it, Strike(manager, false, 30)) }
        registerMelee("obj.statius_warhammer_bh", Strike(manager, false, 75))
    }

    private inner class Strike(private val manager: SpecialAttackManager, private val longsword: Boolean,
                               private val drainPercent: Int) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim(if (longsword) LONGSWORD_ANIMATION else HAMMER_ANIMATION)
            if (!longsword) spotanim(HAMMER_EFFECT, height = 0, slot = constants.spotanim_slot_combat)
            val accurate = if (longsword) accuracy.roll(player, target, attack, MeleeAttackType.Stab, 25) else
                manager.rollMeleeAccuracy(this, target, attack.type, attack.style, MeleeAttackType.Crush, 1.0)
            val base = damage.maximum(player, target, attack, deferReductions = true).coerceAtLeast(1)
            val minimum = (base * (if (longsword) 20 else 25) / 100).coerceAtLeast(1)
            val maximum = base * (if (longsword) 120 else 125) / 100
            val rolled = if (!accurate) 0 else if (player.adminMaxHit) maximum else rng.of(minimum, maximum)
            val amount = damage.modifyRolledHit(player, target, attack, rolled)
            val hit = manager.queueMeleeHit(this, target, amount, 1)
            manager.giveCombatXp(this, target, attack, hit.damage)
            if (!longsword && accurate) attachDefenceDrain(hit, player, target, drainPercent)
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        val LONGSWORDS = listOf("obj.vestas_longsword", "obj.br_vestas_longsword", "obj.bh_vestas_longsword", "obj.vestas_longsword_bh")
        val HAMMERS = listOf("obj.statius_warhammer", "obj.br_statius_warhammer")
        const val LONGSWORD_ANIMATION = "seq.human_dragon_sword_spec"
        const val HAMMER_ANIMATION = "seq.dragon_warhammer_sa_player"
        const val HAMMER_EFFECT = "spotanim.statius_hammer_sa_spotanim"

        fun attachDefenceDrain(hit: Hit, source: Player, target: PathingEntity, percent: Int) {
            val sourceUid = source.uid.packed
            val targetUid = when (target) { is Npc -> target.uid.packed; is Player -> target.uid.packed }
            hit.impactEffects.add { actual ->
                val valid = when (target) {
                    is Npc -> target.isSlotAssigned && target.uid.packed == targetUid
                    is Player -> target.isSlotAssigned && target.uid.packed == targetUid
                }
                if (actual > 0 && valid && source.isSlotAssigned && source.uid.packed == sourceUid) {
                    when (target) {
                        is Npc -> target.defenceLvl -= target.defenceLvl * percent / 100
                        is Player -> target.statSub("stat.defence", target.stat("stat.defence") * percent / 100, 0)
                    }
                }
            }
        }
    }
}
