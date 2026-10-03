package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.player.stat.rangedLvl
import org.rsmod.api.player.stat.statDrain
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

class RangedWeaponSpecialAttacks @Inject constructor(
    private val ammunition: RangedAmmoManager,
    private val queues: WorldQueueList,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (spec in RangedWeaponSpec.entries) {
            val handler = WeaponAttack(manager, spec)
            for (weapon in spec.weapons) registerRanged(weapon, handler)
        }
    }

    private inner class WeaponAttack(
        private val manager: SpecialAttackManager,
        private val spec: RangedWeaponSpec,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Ranged): Boolean = fire(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Ranged): Boolean = fire(target, attack)

        private fun ProtectedAccess.fire(target: PathingEntity, attack: CombatAttack.Ranged): Boolean {
            val weapon = getInvObj(attack.weapon)
            val ammo = if (spec.thrown) weapon else getOrNull(player.quiver)
            val count = if (spec.thrown) attack.weapon.count else player.quiver?.count ?: 0
            if (!ammunition.attemptAmmoUsage(player, weapon, ammo)) {
                manager.stopCombat(this)
                return false
            }
            val travel = ammo?.paramOrNull(params.proj_travel)
            val projectile = weapon.paramOrNull(params.proj_type)
            if (ammo == null || (travel == null && spec.travel == null) || projectile == null || count < spec.hits) {
                mes("You need enough compatible ammunition for this special attack.")
                manager.stopCombat(this)
                return false
            }
            anim(spec.animation)
            val launch = ammo.paramOrNull(params.proj_launch)?.let { RSCM.getReverseMapping(RSCMType.SPOTANIM, it.id) }
            spotanim(spec.launch ?: launch, height = 96, slot = constants.spotanim_slot_combat)
            for (index in 0 until spec.hits) {
                val trajectory = if (spec == RangedWeaponSpec.MagicShortbow) {
                    if (index == 0) "projanim.doublearrow_one" else "projanim.doublearrow_two"
                } else RSCM.getReverseMapping(RSCMType.PROJANIM, projectile.id)
                val travelName = spec.travel ?: RSCM.getReverseMapping(RSCMType.SPOTANIM, checkNotNull(travel).id)
                val proj = manager.spawnProjectile(this, target, travelName, trajectory)
                val accurate = spec.guaranteed || manager.rollRangedAccuracy(this, target, attack.type, attack.style, attack.type, spec.accuracy)
                val damage = when {
                    !accurate -> 0
                    spec.ammoOnlyMaxHit -> random.of(0..RangedSpecialRules.ammoOnlyMaxHit(player.rangedLvl, ammo.param(params.ranged_strength))).coerceAtLeast(1)
                    else -> manager.rollRangedMaxHit(this, target, attack.type, attack.style, spec.damage, 0)
                }
                val hit = manager.queueRangedHit(this, target, if (spec.thrown) null else ammo, damage, proj.clientCycles, proj.serverCycles)
                manager.giveCombatXp(this, target, attack, hit.damage)
                if (spec == RangedWeaponSpec.Seercull && hit.damage > 0) {
                    val uid = when (target) { is Npc -> target.uid.packed; is Player -> target.uid.packed }
                    val drain = hit.damage
                    queues.add(proj.serverCycles) {
                        when (target) {
                            is Npc -> if (target.isSlotAssigned && target.uid.packed == uid) {
                                target.magicLvl = minOf(target.magicLvl, (target.baseMagicLvl - drain).coerceAtLeast(0))
                            }
                            is Player -> if (target.isSlotAssigned && target.uid.packed == uid) target.statDrain("stat.magic", drain, 0)
                        }
                    }
                }
                if (spec.thrown) ammunition.useThrownWeapon(player, weapon, target.coords, proj.serverCycles)
                else ammunition.useQuiverAmmo(player, ammo, target.coords, proj.serverCycles)
            }
            manager.continueCombat(this, target)
            return true
        }
    }
}

internal object RangedSpecialRules {
    // Snapshot, Powershot and Soulshot use the ammunition strength only, with no prayer/gear multiplier.
    fun ammoOnlyMaxHit(ranged: Int, ammoStrength: Int): Int = ((ranged + 10) * (ammoStrength + 64) + 320) / 640
}

internal enum class RangedWeaponSpec(
    val weapons: List<String>, val animation: String, val hits: Int = 1,
    val accuracy: Double = 1.0, val damage: Double = 1.0,
    val guaranteed: Boolean = false, val ammoOnlyMaxHit: Boolean = false,
    val thrown: Boolean = false, val launch: String? = null, val travel: String? = null,
) {
    MagicShortbow(listOf("obj.magic_shortbow", "obj.magic_shortbow_i"), "seq.snapshot", hits = 2, accuracy = 10.0 / 7.0, ammoOnlyMaxHit = true, launch = "spotanim.sp_attack_snapshot_spotanim"),
    MagicLongbow(listOf("obj.magic_longbow", "obj.trail_composite_bow_magic"), "seq.human_bow", guaranteed = true, ammoOnlyMaxHit = true),
    Seercull(listOf("obj.daganoth_cave_magic_shortbow"), "seq.human_bow", guaranteed = true, ammoOnlyMaxHit = true),
    Ballista(listOf("obj.light_ballista", "obj.heavy_ballista", "obj.br_light_ballista", "obj.br_heavy_ballista"), "seq.ballista_special_attack", accuracy = 1.25, damage = 1.25, launch = "spotanim.ballista_special"),
    OrnateBallista(listOf("obj.heavy_ballista_ornament"), "seq.ballista02_special_attack", accuracy = 1.25, damage = 1.25, launch = "spotanim.ballista_special"),
    DragonKnife(listOf("obj.dragon_knife", "obj.br_dragon_knife"), "seq.human_dragon_tknives_spec", hits = 2, thrown = true, travel = "spotanim.dragon_tknife_travel_spec"),
    PoisonedDragonKnife(listOf("obj.dragon_knife_p", "obj.dragon_knife_p+", "obj.dragon_knife_p++"), "seq.human_dragon_tknives_spec_poison", hits = 2, thrown = true, travel = "spotanim.dragon_tknife_travel_spec_p"),
}
