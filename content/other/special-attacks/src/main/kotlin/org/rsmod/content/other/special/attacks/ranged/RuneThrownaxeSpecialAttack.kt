package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import dev.openrune.types.varp.baseVar
import dev.openrune.types.varp.bits
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.config.constants
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.type.getInvObj
import org.rsmod.game.vars.VarPlayerIntMap
import org.rsmod.map.zone.ZoneKey
import org.rsmod.utils.bits.withBits

/**
 * Chainhit makes one rune thrownaxe ricochet from each landed target to the next eligible
 * opponent. Each hop is resolved only after the previous projectile has hit, so failed accuracy
 * rolls terminate the sequence instead of becoming a simultaneous area hit.
 */
class RuneThrownaxeSpecialAttack
@Inject
constructor(
    private val ammunition: RangedAmmoManager,
    private val chainTargets: RuneThrownaxeChainTargets,
    private val pvp: PvPAreaAttackManager,
    private val worldRepo: WorldRepository,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerRanged("obj.rune_thrownaxe", Chainhit(manager, ammunition, chainTargets, pvp, worldRepo))
    }

    private class Chainhit(
        private val manager: SpecialAttackManager,
        private val ammunition: RangedAmmoManager,
        private val chainTargets: RuneThrownaxeChainTargets,
        private val pvp: PvPAreaAttackManager,
        private val worldRepo: WorldRepository,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = chainhit(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = chainhit(target, attack)

        private fun ProtectedAccess.chainhit(
            primary: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val weaponType = getInvObj(attack.weapon)
            if (!ammunition.attemptAmmoUsage(player, weaponType, ammo = null)) {
                manager.stopCombat(this)
                return false
            }

            anim(CHAINHIT_SEQUENCE)
            spotanim(
                spot = CHAINHIT_LAUNCH_SPOTANIM,
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val projectile =
                manager.spawnProjectile(
                    source = this,
                    target = primary,
                    spotanim = CHAINHIT_TRAVEL_SPOTANIM,
                    projanim = THROWN_PROJANIM,
                )
            val roll = rollChainhit(primary, attack)
            val chain = ChainState(source = this, attack = attack, primary = primary)

            // Chainhit throws one axe, regardless of how many targets it subsequently reaches.
            ammunition.useThrownWeapon(
                player = player,
                weaponType = weaponType,
                dropCoord = primary.coords,
                dropDelay = projectile.serverCycles,
            )
            manager.giveCombatXp(this, primary, attack, roll.damage)
            manager.queueRangedHit(
                source = this,
                target = primary,
                ammo = null,
                damage = roll.damage,
                clientDelay = projectile.clientCycles,
                hitDelay = projectile.serverCycles,
            )
            // The accuracy roll is already known synchronously (real OSRS doesn't clamp damage
            // after the roll), so a failed hit simply never starts the chain - no impact callback
            // needed to gate it.
            if (roll.successful) {
                chain.continueFrom(primary)
            }

            if (player.righthand == null) {
                mes("That was your last one!")
            } else {
                manager.continueCombat(this, primary)
            }
            return true
        }

        /**
         * The damage side of Chainhit deliberately omits only the attacker's offensive Ranged
         * prayers. The normal max-hit path still retains equipment, style, Void, target-specific,
         * and final-hit protection handling.
         */
        private fun ProtectedAccess.rollChainhit(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): ChainhitRoll {
            val successful =
                manager.rollRangedAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            if (!successful) {
                return ChainhitRoll(successful = false, damage = 0)
            }

            val damage =
                player.withOffensiveRangedPrayersMasked {
                    manager.rollRangedMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.0,
                        boltSpecDamage = 0,
                    )
                }
            return ChainhitRoll(successful = true, damage = damage)
        }

        /**
         * Holds only this short-lived ricochet sequence. The primary energy has already been
         * deducted when the special returns; every additional target pays a further 10% immediately
         * as the axe leaves for that target.
         */
        private inner class ChainState(
            private val source: ProtectedAccess,
            private val attack: CombatAttack.Ranged,
            primary: PathingEntity,
        ) {
            private val sourcePlayer: Player = source.player
            private val sourceUid: Int = sourcePlayer.uid.packed
            private val hitTargets: MutableList<PathingEntity> = mutableListOf(primary)
            private var hitCount: Int = 1

            fun continueFrom(previous: PathingEntity) {
                if (
                    hitCount >= MAX_CHAIN_TARGETS ||
                        !sourcePlayer.isSlotAssigned ||
                        sourcePlayer.uid.packed != sourceUid ||
                        !source.mapMultiway() ||
                        !manager.hasSpecialEnergy(source, ENERGY_PER_TARGET)
                ) {
                    return
                }

                val next = chainTargets.next(source, previous, hitTargets) ?: return
                val roll = source.rollChainhit(next, attack)

                // Additional Chainhit energy is paid for the attempted extra target. A failed
                // accuracy roll still consumes that target's hop and ends this sequence.
                manager.takeSpecialEnergy(source, ENERGY_PER_TARGET)
                hitTargets += next
                hitCount++

                val projectile = worldRepo.spawnChainhitProjectile(previous, next)
                manager.giveCombatXp(source, next, attack, roll.damage)
                manager.queueRangedHit(
                    source = source,
                    target = next,
                    ammo = null,
                    damage = roll.damage,
                    clientDelay = projectile.clientCycles,
                    hitDelay = projectile.serverCycles,
                )
                if (next is Player) {
                    pvp.applySecondarySpecialAttack(source, next)
                }
                // The accuracy roll is already known synchronously (real OSRS doesn't clamp
                // damage after the roll), so a failed hit simply never continues the chain - no
                // impact callback needed to gate it.
                if (roll.successful) {
                    continueFrom(next)
                }
            }
        }
    }

    private companion object {
        const val CHAINHIT_SEQUENCE = "seq.chainhit"
        const val CHAINHIT_LAUNCH_SPOTANIM = "spotanim.sp_attack_chainhit_launch_spotanim"
        const val CHAINHIT_TRAVEL_SPOTANIM = "spotanim.sp_attack_chainhit_travel_spotanim"
        const val THROWN_PROJANIM = "projanim.thrown"
        const val ENERGY_PER_TARGET = 100
        const val MAX_CHAIN_TARGETS = 5
    }
}

/**
 * Finds the next valid Chainhit opponent around the most recently struck entity. It intentionally
 * re-evaluates targets at each impact because targets can move or die while an earlier projectile
 * is in flight.
 */
class RuneThrownaxeChainTargets
@Inject
constructor(
    private val npcs: NpcRepository,
    private val players: PlayerRepository,
    private val npcAttackValidateHooks: Set<NpcAttackValidateHook>,
    private val pvp: PvPAreaAttackManager,
) {
    fun next(
        source: ProtectedAccess,
        previous: PathingEntity,
        alreadyHit: Collection<PathingEntity>,
    ): PathingEntity? {
        val zone = ZoneKey.from(previous.coords)
        val candidates = mutableListOf<PathingEntity>()

        for (npc in npcs.findAll(zone, zoneRadius = SEARCH_ZONE_RADIUS)) {
            if (
                npc.isAlreadyHit(alreadyHit) ||
                    !npc.isWithinDistance(previous, CHAIN_RADIUS) ||
                    !canAttack(source.player, npc)
            ) {
                continue
            }
            candidates += npc
        }

        for (player in players.findAll(zone, zoneRadius = SEARCH_ZONE_RADIUS)) {
            if (
                player === source.player ||
                    player.isAlreadyHit(alreadyHit) ||
                    !player.isWithinDistance(previous, CHAIN_RADIUS) ||
                    !pvp.canAttack(source.player, player)
            ) {
                continue
            }
            candidates += player
        }
        return source.random.pickOrNull(candidates)
    }

    private fun canAttack(source: Player, target: Npc): Boolean {
        if (!target.isValidTarget() || !target.visType.hasOp(InteractionOp.Op2.slot)) {
            return false
        }
        return npcAttackValidateHooks.all { hook ->
            hook.validate(source, target) !is NpcAttackValidateResult.Deny
        }
    }

    private fun PathingEntity.isAlreadyHit(targets: Collection<PathingEntity>): Boolean =
        targets.any { it === this }

    private companion object {
        const val CHAIN_RADIUS = 3
        const val SEARCH_ZONE_RADIUS = 1
    }
}

private data class ChainhitRoll(val successful: Boolean, val damage: Int)

private fun WorldRepository.spawnChainhitProjectile(
    source: PathingEntity,
    target: PathingEntity,
) =
    when (source) {
        is Npc ->
            when (target) {
                is Npc -> projAnim(source, target, CHAINHIT_SPOTANIM, CHAINHIT_PROJANIM)
                is Player -> projAnim(source, target, CHAINHIT_SPOTANIM, CHAINHIT_PROJANIM)
            }

        is Player ->
            when (target) {
                is Npc -> projAnim(source, target, CHAINHIT_SPOTANIM, CHAINHIT_PROJANIM)
                is Player -> projAnim(source, target, CHAINHIT_SPOTANIM, CHAINHIT_PROJANIM)
            }
    }

private inline fun <T> Player.withOffensiveRangedPrayersMasked(block: () -> T): T {
    val saved =
        IntArray(OFFENSIVE_RANGED_PRAYER_VARBITS.size) { index ->
            vars[OFFENSIVE_RANGED_PRAYER_VARBITS[index]]
        }
    for (varbit in OFFENSIVE_RANGED_PRAYER_VARBITS) {
        setRawVarbit(varbit, 0)
    }
    return try {
        block()
    } finally {
        for (index in OFFENSIVE_RANGED_PRAYER_VARBITS.indices) {
            setRawVarbit(OFFENSIVE_RANGED_PRAYER_VARBITS[index], saved[index])
        }
    }
}

/**
 * Temporarily changes the model-only varbit map. This intentionally bypasses the player variable
 * delegate: the values are restored synchronously around a pure combat calculation and must never
 * be transmitted to the client or marked for persistence.
 */
private fun Player.setRawVarbit(internal: String, value: Int) {
    val varbit =
        checkNotNull(ServerCacheManager.getVarbit(internal.asRSCM(RSCMType.VARBIT))) {
            "Missing required varbit: $internal"
        }
    VarPlayerIntMap.assertVarBitBounds(varbit, value)
    val mappedValue = vars[varbit.baseVar]
    vars.backing[varbit.baseVar.id] = mappedValue.withBits(varbit.bits, value)
}

private val CHAINHIT_SPOTANIM: SpotanimType =
    SpotanimType("spotanim.sp_attack_chainhit_travel_spotanim".asRSCM(RSCMType.SPOTANIM))
private const val CHAINHIT_PROJANIM = "projanim.thrown"
private val OFFENSIVE_RANGED_PRAYER_VARBITS =
    arrayOf(
        "varbit.prayer_sharpeye",
        "varbit.prayer_hawkeye",
        "varbit.prayer_eagleeye",
        "varbit.prayer_rigour",
    )
