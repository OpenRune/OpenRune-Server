package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.player.PvPAreaAttackManager
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.hit.queueImpactHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.player.PlayerRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Lingering Lightning and the Deadman Lightning Strike use the cache-defined energy costs.
 *
 * The two League hits and both PvM variants are modeled independently from Deadman's documented
 * player-only branch. No substitute effect is assigned to the League weapon's unverified PvP
 * lightning behavior; its verified two-hit portion still applies against a player.
 */
class ThunderKhopeshSpecialAttack
@Inject
constructor(private val lightning: ThunderKhopeshLightning) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee(
            "obj.thunder_khopesh",
            LingeringLightning(
                manager = manager,
                lightning = lightning,
                variant = KhopeshVariant.League,
            ),
        )
        registerMelee(
            "obj.deadman_thunder_khopesh",
            LingeringLightning(
                manager = manager,
                lightning = lightning,
                variant = KhopeshVariant.Deadman,
            ),
        )
    }

    private class LingeringLightning(
        private val manager: SpecialAttackManager,
        private val lightning: ThunderKhopeshLightning,
        private val variant: KhopeshVariant,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = lingeringLightning(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean =
            when (variant) {
                KhopeshVariant.League -> leaguePlayerStrike(target, attack)
                KhopeshVariant.Deadman -> deadmanPlayerStrike(target, attack)
            }

        private fun ProtectedAccess.lingeringLightning(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            playSpecialVisuals(target)
            val normalMaximum =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val hits = doubleStrike(target, attack)
            if (hits.any { it.successful }) {
                lightning.schedulePvm(
                    access = this,
                    manager = manager,
                    attack = attack,
                    centre = target.coords,
                    maximumHit = normalMaximum * variant.pvmLightningPercent / 100,
                )
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.leaguePlayerStrike(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            playSpecialVisuals(target)
            doubleStrike(target, attack)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.deadmanPlayerStrike(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            playSpecialVisuals(target)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = DEADMAN_PLAYER_MAX_HIT_MULTIPLIER,
                )
            val targetTile = target.coords
            manager.giveCombatXp(this, target, attack, damage)
            val initialHit = manager.queueMeleeHit(this, target, damage)
            if (initialHit.damage > 0) {
                lightning.scheduleDeadmanPvp(
                    access = this,
                    primary = target,
                    targetTile = targetTile,
                    initialDamage = initialHit.damage,
                )
            }
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.playSpecialVisuals(target: PathingEntity) {
            anim("seq.human_special_khopesh")
            spotanim("spotanim.fx_khopesh_special")
            target.spotanim("spotanim.fx_khopesh_lightning_special")
        }

        private fun ProtectedAccess.doubleStrike(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): List<KhopeshRoll> {
            val first = rollStrike(target, attack)
            val second = rollStrike(target, attack)
            manager.giveCombatXp(this, target, attack, first.damage + second.damage)
            manager.queueMeleeHit(this, target, first.damage, delay = FIRST_HIT_DELAY)
            manager.queueMeleeHit(this, target, second.damage, delay = SECOND_HIT_DELAY)
            return listOf(first, second)
        }

        private fun ProtectedAccess.rollStrike(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): KhopeshRoll {
            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            if (!successful) {
                return KhopeshRoll(successful = false, damage = 0)
            }

            val maximum =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = DOUBLE_STRIKE_MAX_HIT_MULTIPLIER,
                )
            val damage =
                if (maximum > 0) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = DOUBLE_STRIKE_MAX_HIT_MULTIPLIER,
                    )
                } else {
                    0
                }
            return KhopeshRoll(successful = true, damage = damage)
        }
    }

    private enum class KhopeshVariant(val pvmLightningPercent: Int) {
        League(pvmLightningPercent = 225),
        Deadman(pvmLightningPercent = 100),
    }

    private companion object {
        const val DOUBLE_STRIKE_MAX_HIT_MULTIPLIER: Double = 1.5
        const val DEADMAN_PLAYER_MAX_HIT_MULTIPLIER: Double = 1.3
        const val FIRST_HIT_DELAY: Int = 1
        const val SECOND_HIT_DELAY: Int = 2
    }
}

private data class KhopeshRoll(val successful: Boolean, val damage: Int)

/**
 * Resolves fixed-tile lightning after the initiating special has already completed its melee
 * setup. Each delayed bolt uses the dedicated revision-240 lightning graphic.
 */
@Singleton
class ThunderKhopeshLightning
@Inject
constructor(
    private val worldQueues: WorldQueueList,
    private val random: GameRandom,
    private val npcs: NpcRepository,
    private val players: PlayerRepository,
    private val npcHitModifier: NpcHitModifier,
    private val npcAttackValidateHooks: Set<NpcAttackValidateHook>,
    private val pvp: PvPAreaAttackManager,
) {
    /**
     * The published PvM description says the bolt is delayed but does not specify a separate
     * client-cycle count. The next world cycle is the narrowest supported delayed resolve and
     * preserves the target's captured tile for the 3x3 impact.
     */
    fun schedulePvm(
        access: ProtectedAccess,
        manager: SpecialAttackManager,
        attack: CombatAttack.Melee,
        centre: CoordGrid,
        maximumHit: Int,
    ) {
        if (maximumHit <= 0) {
            return
        }
        val source = access.player
        val sourceUid = source.uid.packed
        worldQueues.add(PVM_LIGHTNING_DELAY) {
            if (!source.isCurrent(sourceUid)) {
                return@add
            }

            val zone = ZoneKey.from(centre)
            for (target in npcs.findAll(zone, zoneRadius = SEARCH_ZONE_RADIUS)) {
                if (!target.occupiesLightningTile(centre) || !canAttack(source, target)) {
                    continue
                }
                target.spotanim("spotanim.fx_khopesh_lightning_special_extra")
                val damage = random.of(0..maximumHit)
                manager.giveCombatXp(access, target, attack, damage)
                target.queueHit(
                    source = source,
                    delay = TARGET_HIT_DELAY,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = npcHitModifier,
                )
            }
        }
    }

    /**
     * Captures the tile and multi-combat state at use time. Each queue is armed from that same
     * moment, which yields the documented 7, 9, 11, and 13 tick bolt schedule.
     */
    fun scheduleDeadmanPvp(
        access: ProtectedAccess,
        primary: Player,
        targetTile: CoordGrid,
        initialDamage: Int,
    ) {
        if (initialDamage <= 0) {
            return
        }

        val source = access.player
        val sourceUid = source.uid.packed
        val primaryUid = primary.uid.packed
        val multiway = access.mapMultiway()
        val registeredSecondaryTargets = IdentityHashMap<Player, Unit>()
        repeat(DEADMAN_BOLT_COUNT) { index ->
            val delay = DEADMAN_FIRST_BOLT_DELAY + (index * DEADMAN_BOLT_INTERVAL)
            worldQueues.add(delay) {
                if (!source.isCurrent(sourceUid)) {
                    return@add
                }

                val affected =
                    targetsAt(
                        source = source,
                        primary = primary,
                        primaryUid = primaryUid,
                        targetTile = targetTile,
                        multiway = multiway,
                    )
                for (target in affected) {
                    target.spotanim("spotanim.fx_khopesh_lightning_special_extra")
                    val damage =
                        initialDamage * random.of(
                            DEADMAN_MINIMUM_DAMAGE_PERCENT..DEADMAN_MAXIMUM_DAMAGE_PERCENT,
                        ) / 100
                    target.queueImpactHit(
                        source = source,
                        delay = TARGET_HIT_DELAY,
                        type = HitType.Magic,
                        damage = damage,
                    )
                    if (target !== primary && registeredSecondaryTargets.put(target, Unit) == null) {
                        pvp.applySecondarySpecialAttack(access, target)
                    }
                }
            }
        }
    }

    private fun targetsAt(
        source: Player,
        primary: Player,
        primaryUid: Int,
        targetTile: CoordGrid,
        multiway: Boolean,
    ): List<Player> {
        val candidates =
            players
                .findAll(targetTile)
                .filter { it !== source && pvp.canAttack(source, it) }
        if (!multiway) {
            return candidates
                .filter { it === primary && it.uid.packed == primaryUid }
                .take(SINGLE_COMBAT_TARGET_LIMIT)
                .toList()
        }
        return candidates.take(MULTI_COMBAT_TARGET_LIMIT).toList()
    }

    private fun canAttack(source: Player, target: Npc): Boolean {
        if (!target.isValidTarget() || !target.visType.hasOp(InteractionOp.Op2.slot)) {
            return false
        }
        return npcAttackValidateHooks.all { hook ->
            hook.validate(source, target) !is NpcAttackValidateResult.Deny
        }
    }

    private fun Npc.occupiesLightningTile(centre: CoordGrid): Boolean =
        bounds().asSequence().any { it.chebyshevDistance(centre) <= LIGHTNING_RADIUS }

    private fun Player.isCurrent(uid: Int): Boolean = isSlotAssigned && this.uid.packed == uid

    private companion object {
        const val PVM_LIGHTNING_DELAY: Int = 1
        const val TARGET_HIT_DELAY: Int = 1
        const val LIGHTNING_RADIUS: Int = 1
        const val SEARCH_ZONE_RADIUS: Int = 1

        const val DEADMAN_BOLT_COUNT: Int = 4
        const val DEADMAN_FIRST_BOLT_DELAY: Int = 7
        const val DEADMAN_BOLT_INTERVAL: Int = 2
        const val DEADMAN_MINIMUM_DAMAGE_PERCENT: Int = 30
        const val DEADMAN_MAXIMUM_DAMAGE_PERCENT: Int = 45
        const val SINGLE_COMBAT_TARGET_LIMIT: Int = 1
        const val MULTI_COMBAT_TARGET_LIMIT: Int = 9
    }
}
