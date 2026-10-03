package org.rsmod.content.other.special.attacks

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.PlayerAttackManager
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

class DorgeshuunSpecialAttacks @Inject constructor(
    private val effects: PlayerAttackManager,
    private val ammunition: RangedAmmoManager,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        DAGGERS.forEach { registerMelee(it, Backstab(manager)) }
        registerRanged("obj.dttd_bone_crossbow", Snipe(manager))
    }

    private fun ProtectedAccess.requireQuest(): Boolean {
        if (QuestRequirements.hasCompleted(player, QUEST)) return true
        mes("You must complete Death to the Dorgeshuun to use this special attack.")
        return false
    }

    private inner class Backstab(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            if (!requireQuest()) { manager.stopCombat(this); return false }
            anim("seq.dttd_player_stab_bone_dagger")
            spotanim("spotanim.dttd_dagger_sp_attack_spotanim", height = 0, slot = constants.spotanim_slot_combat)
            val accurate = !target.damageContributions.wasLastDamagedBy(player) ||
                manager.rollMeleeAccuracy(this, target, attack.type, attack.style, attack.type, 1.0)
            val damage = if (accurate) manager.rollMeleeMaxHit(this, target, attack.type, attack.style, 1.0) else 0
            val hit = manager.queueMeleeHit(this, target, damage, 1)
            attachDefenceDrain(hit, player, target)
            manager.giveCombatXp(this, target, attack, hit.damage)
            manager.continueCombat(this, target)
            return true
        }
    }

    private inner class Snipe(private val manager: SpecialAttackManager) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged): Boolean = perform(target, attack)
        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            if (!requireQuest()) { manager.stopCombat(this); return false }
            val weapon = getInvObj(attack.weapon)
            val ammo = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weapon, ammo)) { manager.stopCombat(this); return false }
            val trajectory = weapon.paramOrNull(params.proj_type)
            if (ammo == null || (player.quiver?.count ?: 0) < 1 || trajectory == null) {
                mes("You need compatible ammunition to use this special attack.")
                manager.stopCombat(this)
                return false
            }
            effects.playWeaponFx(player, attack)
            ammo.paramOrNull(params.proj_launch)?.let {
                spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM, it.id), height = 96, slot = constants.spotanim_slot_combat)
            }
            val projectile = manager.spawnProjectile(this, target, "spotanim.dttd_bone_crossbowbolt_travel_sp_attack",
                RSCM.getReverseMapping(RSCMType.PROJANIM, trajectory.id))
            val accurate = !target.damageContributions.wasLastDamagedBy(player) ||
                manager.rollRangedAccuracy(this, target, attack.type, attack.style, attack.type, 1.0)
            val damage = if (accurate) manager.rollRangedMaxHit(this, target, attack.type, attack.style, 1.0, 0) else 0
            val hit = manager.queueRangedHit(this, target, ammo, damage, projectile.clientCycles, projectile.serverCycles)
            attachDefenceDrain(hit, player, target)
            manager.giveCombatXp(this, target, attack, hit.damage)
            ammunition.useQuiverAmmo(player, ammo, target.coords, projectile.serverCycles)
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        const val QUEST = "quest_deathtothedorgeshuun"
        val DAGGERS = listOf("obj.dttd_bone_dagger", "obj.dttd_bone_dagger_p", "obj.dttd_bone_dagger_p+", "obj.dttd_bone_dagger_p++")

        fun attachDefenceDrain(hit: Hit, source: Player, target: PathingEntity) {
            val sourceUid = source.uid.packed
            val targetUid = when (target) { is Npc -> target.uid.packed; is Player -> target.uid.packed }
            hit.impactEffects.add { damage ->
                val valid = when (target) {
                    is Npc -> target.isSlotAssigned && target.uid.packed == targetUid
                    is Player -> target.isSlotAssigned && target.uid.packed == targetUid
                }
                if (damage > 0 && valid && source.isSlotAssigned && source.uid.packed == sourceUid) {
                    when (target) {
                        is Npc -> if (target.defenceLvl >= target.baseDefenceLvl) target.defenceLvl = (target.defenceLvl - damage).coerceAtLeast(0)
                        is Player -> if (target.stat("stat.defence") >= target.statBase("stat.defence")) target.statSub("stat.defence", damage, 0)
                    }
                }
            }
        }
    }
}
