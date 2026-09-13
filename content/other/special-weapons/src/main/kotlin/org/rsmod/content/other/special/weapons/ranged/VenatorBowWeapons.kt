package org.rsmod.content.other.special.weapons.ranged

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.CombatChargeManager
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.obj.charges.ObjChargeManager.Companion.isFailure
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.weapons.RangedWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

class VenatorBowWeapons
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val charges: CombatChargeManager,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val attackValidateHooks: Set<NpcAttackValidateHook>,
) : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        register(
            "obj.venator_bow",
            VenatorBow(manager, ammunition, charges, npcRepo, worldRepo, attackValidateHooks),
        )
    }

    private class VenatorBow(
        private val manager: WeaponAttackManager,
        private val ammunition: RangedAmmoManager,
        private val charges: CombatChargeManager,
        private val npcRepo: NpcRepository,
        private val worldRepo: WorldRepository,
        private val attackValidateHooks: Set<NpcAttackValidateHook>,
    ) : RangedWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean {
            shoot(target, attack, bounce = false)
            return true
        }

        private fun ProtectedAccess.shoot(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
            bounce: Boolean = true,
        ) {
            val righthandType = getInvObj(attack.weapon)
            val quiverType = getOrNull(player.quiver)

            val canUseAmmo = ammunition.attemptAmmoUsage(player, righthandType, quiverType)
            if (!canUseAmmo) {
                manager.stopCombat(this)
                return
            }

            val travelSpotanim = quiverType?.paramOrNull(params.proj_travel)
            if (travelSpotanim == null) {
                manager.stopCombat(this)
                mes("You are unable to fire your ammunition.")
                return
            }

            val chargeResult = charges.attemptDetractWeapon(player, CHARGE_VAROBJ)
            if (chargeResult.isFailure()) {
                manager.stopCombat(this)
                return
            }

            manager.playWeaponFx(this, attack)

            val launchSpotanim = quiverType.paramOrNull(params.proj_launch)
            val launchSpotanimName =
                launchSpotanim?.let { RSCM.getReverseMapping(RSCMType.SPOTANIM, it.id) }
            spotanim(launchSpotanimName, height = 96, slot = constants.spotanim_slot_combat)

            val travelSpotanimName = RSCM.getReverseMapping(RSCMType.SPOTANIM, travelSpotanim.id)
            val primaryProj = manager.spawnProjectile(this, target, travelSpotanimName, "projanim.arrow")
            val (primaryServerDelay, primaryClientDelay) = primaryProj.durations

            ammunition.useQuiverAmmo(
                player = player,
                quiverType = quiverType,
                dropCoord = target.coords,
                dropDelay = primaryServerDelay,
            )

            val primaryDamage = manager.rollRangedDamage(this, target, attack)
            manager.giveCombatXp(this, target, attack, primaryDamage)
            manager.queueRangedHit(
                this,
                target,
                quiverType,
                primaryDamage,
                primaryClientDelay,
                primaryServerDelay,
            )

            if (bounce && target is Npc && mapMultiway()) {
                fireBounceChain(
                    access = this,
                    origin = target,
                    excluded = setOf(target),
                    primary = target,
                    attack = attack,
                    quiverType = quiverType,
                    travelSpotanimName = travelSpotanimName,
                    chainStartTime = primaryProj.endTime,
                    remainingBounces = 2,
                )
            }

            val remainingCharges = charges.getWeaponCharges(player, CHARGE_VAROBJ)
            notifyChargeThreshold(remainingCharges)

            if (chargeResult.fullyUncharged) {
                manager.stopCombat(this)
                mes("Your venator bow has run out of charges and reverts to its uncharged form.")
                return
            }

            manager.continueCombat(this, target)
        }

        private fun fireBounceChain(
            access: ProtectedAccess,
            origin: Npc,
            excluded: Set<Npc>,
            primary: Npc,
            attack: CombatAttack.Ranged,
            quiverType: ItemServerType?,
            travelSpotanimName: String,
            chainStartTime: Int,
            remainingBounces: Int,
        ) {
            if (remainingBounces <= 0) {
                return
            }

            val candidates = candidatesNear(access.player, origin, exclude = excluded)
            val pool = if (remainingBounces == 1) candidates + primary else candidates
            val nextTarget = pool.minByOrNull { centreDistance(origin, it) } ?: return

            val travelSpotanimId = travelSpotanimName.asRSCM(RSCMType.SPOTANIM)
            val raw =
                ProjAnim.fromBoundsToNpc(origin.bounds(), nextTarget, travelSpotanimId, "projanim.arrow")
            val chained =
                raw.copy(startTime = chainStartTime, endTime = chainStartTime + (raw.endTime - raw.startTime))
            worldRepo.projAnim(chained)
            val (serverDelay, clientDelay) = chained.durations

            access.soundSynth(if (remainingBounces == 2) RICOCHET_1_SYNTH else RICOCHET_2_SYNTH)

            val damage =
                manager.rollRangedDamage(
                    source = access,
                    target = nextTarget,
                    attack = attack,
                    maxHitMultiplier = BOUNCE_MAX_HIT_MULTIPLIER,
                )
            manager.giveCombatXp(access, nextTarget, attack, damage)
            manager.queueRangedHit(access, nextTarget, quiverType, damage, clientDelay, serverDelay)

            fireBounceChain(
                access = access,
                origin = nextTarget,
                excluded = excluded + nextTarget,
                primary = primary,
                attack = attack,
                quiverType = quiverType,
                travelSpotanimName = travelSpotanimName,
                chainStartTime = chained.endTime,
                remainingBounces = remainingBounces - 1,
            )
        }

        private fun candidatesNear(player: Player, origin: Npc, exclude: Set<Npc>): List<Npc> =
            npcRepo
                .findAll(ZoneKey.from(origin.coords), zoneRadius = 1)
                .filter { it !in exclude }
                .filter { it.isValidTarget() }
                .filter { it.visType.hasOp(InteractionOp.Op2.slot) }
                .filter { centreDistance(origin, it) <= BOUNCE_RADIUS }
                .filter { canAttack(player, it) }
                .toList()

        private fun canAttack(player: Player, npc: Npc): Boolean =
            attackValidateHooks.none { it.validate(player, npc) is NpcAttackValidateResult.Deny }

        private fun centreDistance(a: Npc, b: Npc): Int = centreOf(a).chebyshevDistance(centreOf(b))

        private fun centreOf(npc: Npc): CoordGrid = npc.coords.translate(npc.size / 2, npc.size / 2)

        private fun ProtectedAccess.notifyChargeThreshold(remaining: Int) {
            val message =
                when {
                    remaining == 100 || remaining == 50 -> "$remaining charges remaining."
                    remaining > 0 && remaining % 500 == 0 -> "$remaining charges remaining."
                    else -> null
                } ?: return
            mes("Your venator bow has $message")
        }

        private companion object {
            const val CHARGE_VAROBJ = "varobj.venator_bow_charges"
            const val BOUNCE_RADIUS = 2
            const val BOUNCE_MAX_HIT_MULTIPLIER = 2.0 / 3.0
            const val RICOCHET_1_SYNTH = 6672
            const val RICOCHET_2_SYNTH = 6735
        }
    }
}
