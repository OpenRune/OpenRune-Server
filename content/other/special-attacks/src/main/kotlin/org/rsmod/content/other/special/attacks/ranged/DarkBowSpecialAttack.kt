package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.SpotanimType
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

class DarkBowSpecialAttack @Inject constructor(private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged("obj.darkbow", DarkBow(manager, ammunition))
        registerRanged("obj.darkbow_green", DarkBow(manager, ammunition))
        registerRanged("obj.darkbow_blue", DarkBow(manager, ammunition))
        registerRanged("obj.darkbow_yellow", DarkBow(manager, ammunition))
        registerRanged("obj.darkbow_white", DarkBow(manager, ammunition))
        registerRanged("obj.bh_darkbow_imbue", DarkBow(manager, ammunition))
        registerRanged("obj.br_darkbow", DarkBow(manager, ammunition))
        registerRanged("obj.deadman_blighted_dark_bow", DarkBow(manager, ammunition))
        registerRanged("obj.deadman_darkbow", DarkBow(manager, ammunition))
    }

    private class DarkBow(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
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
            if (travelSpotanim == null) {
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
                descentOfDragons(target, attack, quiverType)
                manager.continueCombat(this, target)
                return true
            }

            descentOfDarkness(target, attack, quiverType)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.descentOfDarkness(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType,
        ) {
            val launchSpot = quiverType.paramOrNull(params.proj_launch_double)
            anim(DARK_BOW_FIRE_SEQUENCE)
            soundSynth("synth.darkbow_doublefire")
            soundSynth("synth.darkbow_shadow_attack")
            spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM,launchSpot!!.id), height = 96, slot = constants.spotanim_slot_combat)

            val descentTravel = "spotanim.darkbow_generic_smoke_arrow_flight"
            val descentImpact = "spotanim.darkbow_smoke_arrow_impact"
            val impactSynth = "synth.darkbow_shadow_impact"

            // Was spawning two overlapping projectiles per arrow - this special one PLUS a second,
            // separate one using the ammo's own plain proj_travel colour, both flying the same
            // path at once. Live testing confirmed that reads as "the wrong arrow in the air"
            // regardless of ammo, since the plain one renders on top of/alongside the special one.
            // The ammo's travel spotanim is no longer resolved/passed in at all - calculateEndTime
            // (and so clientCycles/serverCycles) depends only on the projanim type, not the
            // spotanim, so dropping the second spawn doesn't change any of the timing below.
            val proj1 = manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_one")
            val clientDelay1 = proj1.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay1, radius = 10)

            val proj2 = manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_two")
            val clientDelay2 = proj2.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay2, radius = 10)

            target.spotanim(descentImpact, height = 96, delay = clientDelay2)

            val damage =
                calculateDamage(
                    target,
                    attack,
                    damageRange = DarkBowDamage.DESCENT_OF_DARKNESS_RANGE,
                    multiplier = DarkBowDamage.DESCENT_OF_DARKNESS_MULTIPLIER,
                )
            val hitDelay1 = proj1.serverCycles
            val hitDelay2 = proj2.serverCycles

            manager.giveCombatXp(this, target, attack, damage.total)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay1,
            )

            manager.queueRangedHit(this, target, quiverType, damage[0], clientDelay2, hitDelay1)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay2,
            )

            // Both hits resolve on the first arrow's tick (hitDelay1), not the second arrow's own
            // (slightly later) serverCycles - doublearrow_one/two intentionally have different
            // stepMultiplier values in projectiles.toml for the visual high/low arc, which grows
            // with distance and was causing the two hits to land a tick apart at real combat range.
            // Same fix as Magic shortbow's Snapshot: the arc/offset is purely visual, not a real
            // gameplay delay between the two hits.
            manager.queueRangedDamage(this, target, quiverType, damage[1], hitDelay1)

            if (player.quiver?.count == 1) {
                mes("You now have only 1 arrow left in your quiver.")
            }
        }

        private fun ProtectedAccess.descentOfDragons(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType,
        ) {
            val launchSpot = quiverType.paramOrNull(params.proj_launch_double)
            anim(DARK_BOW_FIRE_SEQUENCE)
            soundSynth("synth.darkbow_doublefire")
            soundSynth("synth.darkbow_dragon_attack")
            spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM,launchSpot!!.id), height = 96, slot = constants.spotanim_slot_combat)

            val descentTravel = "spotanim.darkbow_dragon_head_flying_projanim"
            val descentImpact = "spotanim.darkbow_dragon_head_flying_impact_anim"
            val impactSynth = "synth.darkbow_shadow_impact"

            // Was spawning two overlapping projectiles per arrow - this special one PLUS a second,
            // separate one using the ammo's own plain proj_travel colour, both flying the same
            // path at once. Live testing confirmed that reads as "the wrong arrow in the air"
            // regardless of ammo, since the plain one renders on top of/alongside the special one.
            // The ammo's travel spotanim is no longer resolved/passed in at all - calculateEndTime
            // (and so clientCycles/serverCycles) depends only on the projanim type, not the
            // spotanim, so dropping the second spawn doesn't change any of the timing below.
            val proj1 = manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_one")
            val clientDelay1 = proj1.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay1, radius = 10)

            val proj2 = manager.spawnProjectile(this, target, descentTravel, "projanim.doublearrow_two")
            val clientDelay2 = proj2.clientCycles
            manager.soundArea(target, impactSynth, delay = clientDelay2, radius = 10)

            target.spotanim(descentImpact, height = 96, delay = clientDelay2)

            val damage =
                calculateDamage(
                    target,
                    attack,
                    damageRange = DarkBowDamage.DESCENT_OF_DRAGONS_RANGE,
                    multiplier = DarkBowDamage.DESCENT_OF_DRAGONS_MULTIPLIER,
                )
            val hitDelay1 = proj1.serverCycles
            val hitDelay2 = proj2.serverCycles

            manager.giveCombatXp(this, target, attack, damage.total)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay1,
            )

            manager.queueRangedHit(this, target, quiverType, damage[0], clientDelay2, hitDelay1)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = hitDelay2,
            )

            // Both hits resolve on the first arrow's tick (hitDelay1), not the second arrow's own
            // (slightly later) serverCycles - doublearrow_one/two intentionally have different
            // stepMultiplier values in projectiles.toml for the visual high/low arc, which grows
            // with distance and was causing the two hits to land a tick apart at real combat range.
            // Same fix as Magic shortbow's Snapshot: the arc/offset is purely visual, not a real
            // gameplay delay between the two hits.
            manager.queueRangedDamage(this, target, quiverType, damage[1], hitDelay1)

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
                manager.calculateRangedMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = multiplier,
                    boltSpecDamage = 0,
                )
            val first =
                DarkBowDamage.resolveHit(
                    accuracySuccess = accuracySuccess(),
                    damageRange = damageRange,
                    rollRawDamage = { random.of(0..damage) },
                )
            val second =
                DarkBowDamage.resolveHit(
                    accuracySuccess = accuracySuccess(),
                    damageRange = damageRange,
                    rollRawDamage = { random.of(0..damage) },
                )
            return DescentHit(first, second)
        }

        private data class DescentHit(val first: Int, val second: Int) {
            val total: Int
                get() = first + second

            operator fun get(index: Int): Int =
                when (index) {
                    0 -> first
                    1 -> second
                    else -> throw ArrayIndexOutOfBoundsException()
                }
        }
    }

    private companion object {
        const val DARK_BOW_FIRE_SEQUENCE = "seq.human_bow"
    }
}

/**
 * Pure per-hit damage math for Descent of Darkness/Dragons, kept separate from [ProtectedAccess]
 * so the range clamping can be unit tested directly instead of only through a live combat roll.
 */
internal object DarkBowDamage {
    /** Wiki: both variants cap each hit at 48; Darkness also has a 5 damage floor. */
    val DESCENT_OF_DARKNESS_RANGE = 5..48
    const val DESCENT_OF_DARKNESS_MULTIPLIER = 1.3

    /** Wiki: both variants cap each hit at 48; Dragons also has an 8 damage floor. */
    val DESCENT_OF_DRAGONS_RANGE = 8..48
    const val DESCENT_OF_DRAGONS_MULTIPLIER = 1.5

    fun resolveHit(accuracySuccess: Boolean, damageRange: IntRange, rollRawDamage: () -> Int): Int =
        if (!accuracySuccess) 0 else rollRawDamage().coerceIn(damageRange)
}
