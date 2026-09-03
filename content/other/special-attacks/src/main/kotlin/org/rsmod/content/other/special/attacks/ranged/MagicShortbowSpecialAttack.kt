package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull

/**
 * Snapshot fires two independent arrows. Its hit rolls use the cache-backed projectile flow, but
 * retain the unusual Old School max-hit calculation: visible Ranged level plus ten and the
 * ammunition's ranged-strength value only. Gear strength, Void, prayers, Slayer, and Salve do
 * not enter the damage calculation; the normal ranged accuracy calculation still does.
 *
 * Visuals, all taken from the cache rather than inferred:
 * - `seq.snapshot` (1074) is the spec animation (confirmed by several RuneLite plugins that key
 *   off it as `RANGED_MAGIC_SHORTBOW_SPEC`). It is two identical draw-and-release cycles back to
 *   back, 27 client cycles each (frame delays 2,2,2,2,8,5,2,2,2,2, twice) - a sped-up double of
 *   the normal `human_bow` (426) draw. The second arrow is therefore loosed 27 client cycles after
 *   the first: within the same server tick, not a tick later.
 * - `spotanim.sp_attack_snapshot_spotanim` (256) is Snapshot's launch glow, one per draw. A
 *   reference implementation of this special (Zenyte-based Offline_Scape/Near Reality) instead
 *   uses a single shared glow (250, also Powershot's own) applied once - tried matching that
 *   exactly, but live testing preferred this original per-draw version, so it's staying.
 * - Live testing confirmed the in-flight arrow itself needs to glow, not just the draw -
 *   `spotanim.sp_attack_glow_arrow_travel` (249) overrides the ammo's own `proj_travel` for both
 *   shots. The same reference confirms this id and timing exactly (`Projectile(249, ...)` for
 *   both arrows).
 * - Sound (`SNAPSHOT_SOUND` = synth 2545, unaliased in this cache) confirmed against the same
 *   reference.
 */
class MagicShortbowSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val worldRepo: WorldRepository,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val snapshot = Snapshot(manager, ammunition, worldRepo)
        registerRanged("obj.magic_shortbow", snapshot)
        registerRanged("obj.magic_shortbow_i", snapshot)
        registerRanged("obj.br_magic_bow", snapshot)
    }

    private class Snapshot(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val worldRepo: WorldRepository,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = snapshot(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = snapshot(target, attack)

        private fun ProtectedAccess.snapshot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)
            if (!ammunition.attemptAmmoUsage(player, weaponType, quiverType)) {
                manager.stopCombat(this)
                return false
            }

            if ((player.quiver?.count ?: 0) < ARROWS_PER_SNAPSHOT) {
                manager.stopCombat(this)
                mes("You need to have at least 2 arrows in your quiver for this special attack.")
                return false
            }

            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            val projectileType = weaponType.paramOrNull(params.proj_type)
            if (travelSpotanim == null || projectileType == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return false
            }

            anim(SNAPSHOT_SEQUENCE)
            weaponType.paramOrNull(params.attack_sound_stance1)?.let { soundSynth(it) }
            soundSynth(SNAPSHOT_SOUND)

            // One launch glow per draw, on the shooter. The second is scheduled for the second
            // draw via the spotanim's own client-cycle delay, in a different slot so it doesn't
            // overwrite the first before it has played. This replaces the ammo's generic
            // proj_launch graphic entirely - sending both into the same slot lets the ammo's
            // plain launch overwrite the spec glow, which is why an earlier version showed
            // rune-arrow-coloured launches instead of the green glow.
            spotanim(SNAPSHOT_GLOW_SPOTANIM, height = 96, slot = constants.spotanim_slot_combat)
            spotanim(
                SNAPSHOT_GLOW_SPOTANIM,
                delay = SECOND_ARROW_OFFSET_CYCLES,
                height = 96,
                slot = SECOND_GLOW_SPOTANIM_SLOT,
            )

            // Live testing confirmed the in-flight arrow needs to glow too (not just the draw) -
            // `sp_attack_glow_arrow_travel` (249), sitting right next to the launch glow (250) in
            // the same spec-graphic id block. Overrides the ammo's own plain proj_travel entirely
            // rather than layering on top of it.
            val travelSpot = SNAPSHOT_TRAVEL_GLOW_SPOTANIM
            val projanim = RSCM.getReverseMapping(RSCMType.PROJANIM, projectileType.id)
            val travelSpotId = travelSpot.asRSCM(RSCMType.SPOTANIM)
            // `manager.spawnProjectile` dispatches immediately using the ammo's generic
            // `projanim.arrow` delay (41 client cycles) - tuned for a normal single-draw bow shot,
            // not this animation. `seq.snapshot`'s own draw-and-release cycle is only 27 cycles, so
            // that default fires the arrow 14 cycles into the *second* draw instead of during the
            // first. Building the ProjAnim by hand instead lets us launch it at cycle 16 (where the
            // held-draw frames end and the snap/release frames begin) without dispatching the
            // wrongly-timed default first. Shifting both startTime and endTime by the same amount
            // keeps the real flight duration (baked additively into endTime) unchanged - only when
            // it starts moving changes.
            val defaultProjectile =
                when (target) {
                    is Npc -> ProjAnim.fromBoundsToNpc(player.bounds(), target, travelSpotId, projanim)
                    is Player ->
                        ProjAnim.fromBoundsToPlayer(player.bounds(), target, travelSpotId, projanim)
                }
            val launchDelta = defaultProjectile.startTime - FIRST_ARROW_RELEASE_CYCLE
            val firstProjectile =
                defaultProjectile.copy(
                    startTime = FIRST_ARROW_RELEASE_CYCLE,
                    endTime = defaultProjectile.endTime - launchDelta,
                )
            worldRepo.projAnim(firstProjectile)
            // Same arrow, same speed and arc, launched 27 client cycles later to match the second
            // draw in the animation. Two projectiles with byte-identical timing dispatched in the
            // same tick render as one, so the offset is what makes the second arrow visible - it
            // is not a server-side delay() (a whole tick is far too slow) and it is not a
            // different projanim (Dark bow's doublearrow_one/two pair has different angle and
            // stepMultiplier values in projectiles.toml, an intentional high/low-arc effect that
            // is wrong here).
            val secondProjectile =
                firstProjectile.copy(
                    startTime = firstProjectile.startTime + SECOND_ARROW_OFFSET_CYCLES,
                    endTime = firstProjectile.endTime + SECOND_ARROW_OFFSET_CYCLES,
                )
            worldRepo.projAnim(secondProjectile)

            val firstDamage = snapshotDamage(target, attack, quiverType)
            val secondDamage = snapshotDamage(target, attack, quiverType)

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = firstProjectile.serverCycles,
            )
            manager.queueRangedHit(
                source = this,
                target = target,
                ammo = quiverType,
                damage = firstDamage,
                clientDelay = firstProjectile.clientCycles,
                hitDelay = firstProjectile.serverCycles,
            )

            // Both hits register on the first arrow's tick, not the second arrow's own (slightly
            // later, visual-only) serverCycles - the 27-client-cycle offset is purely for the
            // second arrow to render as a separate projectile, not a real gameplay delay between
            // the two hits.
            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = firstProjectile.serverCycles,
            )
            manager.queueRangedDamage(
                source = this,
                target = target,
                ammo = quiverType,
                damage = secondDamage,
                hitDelay = firstProjectile.serverCycles,
            )
            manager.giveCombatXp(this, target, attack, firstDamage + secondDamage)

            if (player.quiver?.count == 1) {
                mes("You now have only 1 arrow left in your quiver.")
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.snapshotDamage(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            ammo: ItemServerType,
        ): Int {
            val successful =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = SNAPSHOT_ACCURACY_MULTIPLIER,
                )
            if (!successful) {
                return 0
            }

            val maxHit = snapshotMaxHit(ammo)
            return if (player.adminMaxHit) maxHit else random.of(0..maxHit)
        }

        private fun ProtectedAccess.snapshotMaxHit(ammo: ItemServerType): Int {
            val effectiveRanged = stat("stat.ranged") + SNAPSHOT_LEVEL_BONUS
            val rangedStrength = ammo.param(params.ranged_strength)
            return (effectiveRanged * (rangedStrength + RANGED_STRENGTH_BASE) + ROUNDING) / DIVISOR
        }
    }

    private companion object {
        const val SNAPSHOT_SEQUENCE = "seq.snapshot"
        const val SNAPSHOT_GLOW_SPOTANIM = "spotanim.sp_attack_snapshot_spotanim"
        const val SNAPSHOT_TRAVEL_GLOW_SPOTANIM = "spotanim.sp_attack_glow_arrow_travel"

        /** Unaliased in this cache's gamevals - no `synth.` name exists for it. */
        const val SNAPSHOT_SOUND = 2545

        /** Any slot other than [constants.spotanim_slot_combat], so the two glows coexist. */
        const val SECOND_GLOW_SPOTANIM_SLOT = 0

        /**
         * Client cycles between the two draws in `seq.snapshot`: one full draw-and-release cycle
         * (frame delays 2+2+2+2+8+5+2+2+2+2). 30 client cycles = 1 server tick.
         */
        const val SECOND_ARROW_OFFSET_CYCLES = 27

        /**
         * Client cycle within the first draw where the arrow actually leaves the bow: the pulled-
         * back hold ends and the snap/release frames begin (2+2+2+2+8 into `seq.snapshot`'s frame
         * delays). Live-tested as too late at the ammo's generic `projanim.arrow` delay of 41,
         * which fires the arrow into the *second* draw instead of the first - needs your eyes to
         * confirm/tune this exact value.
         */
        const val FIRST_ARROW_RELEASE_CYCLE = 16

        const val ARROWS_PER_SNAPSHOT = 2
        const val SNAPSHOT_LEVEL_BONUS = 10
        const val RANGED_STRENGTH_BASE = 64
        const val ROUNDING = 320
        const val DIVISOR = 640
        const val SNAPSHOT_ACCURACY_MULTIPLIER = 10.0 / 7.0
    }
}
