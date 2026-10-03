package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

class DarkBowSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager, private val formula: RangedSpecialDamage) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (weapon in listOf("obj.darkbow", "obj.darkbow_green", "obj.darkbow_blue",
            "obj.darkbow_yellow", "obj.darkbow_white", "obj.br_darkbow",
            "obj.deadman_blighted_dark_bow", "obj.deadman_darkbow")) {
            registerRanged(weapon, DarkBow(manager, ammunition, formula))
        }
        registerRanged("obj.bh_darkbow_imbue", DarkBow(manager, ammunition, formula, bountyHunter = true))
    }

    private class DarkBow(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val formula: RangedSpecialDamage,
        private val bountyHunter: Boolean = false,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = selectAndShootSpecial(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = selectAndShootSpecial(target, attack)

        private fun ProtectedAccess.selectAndShootSpecial(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val righthandType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)

            val canUseAmmo = ammunition.attemptAmmoUsage(player, righthandType, quiverType)
            if (!canUseAmmo) {
                manager.stopCombat(this)
                return false
            }

            // All valid ammunition requires a `proj_travel` param to build the projectiles.
            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            if (travelSpotanim == null || quiverType.paramOrNull(params.proj_launch_double) == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }

            val quiverCount = player.quiver?.count ?: 0
            if (quiverCount < 2) {
                manager.stopCombat(this)
                mes("You need to have at least 2 arrows in your quiver for this special attack.")
                return false
            }

            val descentOfDragons = quiverType.isCategoryType("category.dragon_arrow")
            if (descentOfDragons) {
                descentOfDragons(target, attack, quiverType, RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id))
                manager.continueCombat(this, target)
                return true
            }

            descentOfDarkness(target, attack, quiverType, RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id))
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.descentOfDarkness(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType,
            travelSpot: String,
        ) {
            val launchSpot = quiverType.paramOrNull(params.proj_launch_double)
            anim("seq.human_bow")
            soundSynth("synth.darkbow_doublefire")
            soundSynth("synth.darkbow_shadow_attack")
            spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM, launchSpot!!.id), height = 96, slot = constants.spotanim_slot_combat)

            val descentTravel = "spotanim.darkbow_generic_smoke_arrow_flight"
            val descentImpact = "spotanim.darkbow_smoke_arrow_impact"
            val impactSynth = "synth.darkbow_shadow_impact"

            manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_one")
            val proj1 = manager.spawnProjectile(this, target, travelSpot, "projanim.doublearrow_one")
            val clientDelay1 = proj1.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay1, radius = 10)

            manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_two")
            val proj2 = manager.spawnProjectile(this, target, travelSpot, "projanim.doublearrow_two")
            val clientDelay2 = proj2.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay2, radius = 10)

            target.spotanim(descentImpact, height = 96, delay = clientDelay2)

            val damage =
                calculateDamage(target, attack, damageRange = 5..Int.MAX_VALUE, multiplier = 1.3)
            val hitDelay1 = proj1.serverCycles
            val hitDelay2 = proj2.serverCycles

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay1,
            )

            val firstHit = manager.queueRangedHit(this, target, quiverType, damage[0], clientDelay1, hitDelay1)
            manager.giveCombatXp(this, target, attack, firstHit.damage)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay2,
            )

            val secondHit = manager.queueRangedDamage(this, target, quiverType, damage[1], hitDelay2)
            manager.giveCombatXp(this, target, attack, secondHit.damage)

            if (player.quiver?.count == 1) {
                mes("You now have only 1 arrow left in your quiver.")
            }
        }

        private fun ProtectedAccess.descentOfDragons(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType,
            travelSpot: String,
        ) {
            val launchSpot = quiverType.paramOrNull(params.proj_launch_double)
            anim("seq.human_bow")
            soundSynth("synth.darkbow_doublefire")
            soundSynth("synth.darkbow_dragon_attack")
            spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM, launchSpot!!.id), height = 96, slot = constants.spotanim_slot_combat)

            val descentTravel = "spotanim.darkbow_dragon_head_flying_projanim"
            val descentImpact = "spotanim.darkbow_dragon_head_flying_impact_anim"
            val impactSynth = "synth.darkbow_shadow_impact"

            manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_one")
            val proj1 = manager.spawnProjectile(this, target, travelSpot, "projanim.doublearrow_one")
            val clientDelay1 = proj1.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay1, radius = 10)

            manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_two")
            val proj2 = manager.spawnProjectile(this, target, travelSpot, "projanim.doublearrow_two")
            val clientDelay2 = proj2.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay2, radius = 10)

            target.spotanim(descentImpact, height = 96, delay = clientDelay2)

            val damage = calculateDamage(target, attack, damageRange = 8..48, multiplier = 1.5)
            val hitDelay1 = proj1.serverCycles
            val hitDelay2 = proj2.serverCycles

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay1,
            )

            val firstHit = manager.queueRangedHit(this, target, quiverType, damage[0], clientDelay1, hitDelay1)
            manager.giveCombatXp(this, target, attack, firstHit.damage)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay2,
            )

            val secondHit = manager.queueRangedDamage(this, target, quiverType, damage[1], hitDelay2)
            manager.giveCombatXp(this, target, attack, secondHit.damage)

            if (player.quiver?.count == 1) {
                mes("You now have only 1 arrow left in your quiver.")
            }
        }

        private fun ProtectedAccess.calculateDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            damageRange: IntRange,
            multiplier: Double,
        ): DescentHit {
            fun accuracySuccess(): Boolean {
                return manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            }
            val damage =
                formula.maximum(
                    source = player,
                    target = target,
                    attack = attack,
                    multiplier = multiplier,
                )
            fun roll(): Int {
                val minimum = damageRange.first + if (bountyHunter) 2 else 0
                val rolled = rollDamage(accuracySuccess(), damage, minimum, damageRange.last, bountyHunter) { random.of(it) }
                return formula.modifyRolledHit(player, target, attack, rolled)
            }
            val first = roll()
            val second = roll()
            return DescentHit(first, second)
        }

        private data class DescentHit(val first: Int, val second: Int) {
            operator fun get(index: Int): Int =
                when (index) {
                    0 -> first
                    1 -> second
                    else -> throw ArrayIndexOutOfBoundsException()
                }
        }
    }

    internal companion object {
        fun rollDamage(accurate: Boolean, maximum: Int, minimum: Int, cap: Int,
                       bountyHunter: Boolean, roll: (IntRange) -> Int): Int {
            if (!accurate) return minimum
            val range = if (bountyHunter) minimum..maximum.coerceIn(minimum, cap) else 0..maximum.coerceAtLeast(0)
            return roll(range).coerceIn(minimum, cap)
        }
    }
}
