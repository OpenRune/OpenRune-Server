package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ProjAnimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.death.NpcAttackValidateHook
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.ui.BossCountdown
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

@Singleton
class ZulrahEncounterManager
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val locs: LocRepository,
    private val interactions: AiPlayerInteractions,
    private val countdown: BossCountdown,
) : NpcAttackValidateHook {
    private val fights = mutableMapOf<InstanceId, Fight>()
    private val waiting = mutableMapOf<InstanceId, WaitingSpawn>()
    private val npcFights = IdentityHashMap<Npc, Fight>()
    private val rays = RayCastValidator(deps.collision)

    fun start(player: Player, session: InstanceSession) {
        if (session.id in fights || session.id in waiting || session.owner != player.uuid) return
        check(instances.sessionForPlayer(player) === session)
        waitForSpawn(player, session, FIRST_SPAWN_SECONDS)
    }

    private fun spawn(player: Player, session: InstanceSession) {
        val origin = resolve(session, CoordGrid(2266, 3073, 0))
        val boss = Npc(type("npc.snakeboss_boss_ranged"), origin)
        boss.movementLocked = true
        boss.apRangeOverride = 32
        boss.apRequiresLineOfSight = false
        boss.noneMode()
        deps.npcRepo.add(boss, Int.MAX_VALUE)
        boss.respawns = false
        check(instances.registerSessionNpc(player, boss))
        val fight = Fight(session, player, boss, ZulrahRotations.all[deps.random.of(4)])
        fights[session.id] = fight
        npcFights[boss] = fight
        enterPhase(fight, initial = true)
    }

    fun stop(instanceId: InstanceId) {
        waiting.remove(instanceId)?.let { countdown.clear(it.owner, COUNTDOWN_OWNER) }
        val fight = fights.remove(instanceId) ?: return
        countdown.clear(fight.owner, COUNTDOWN_OWNER)
        fight.finished = true
        clearHazards(fight)
        npcFights.remove(fight.boss)
        instances.detachNpc(instanceId, fight.boss)
        if (fight.boss.isSlotAssigned) deps.npcRepo.del(fight.boss, Int.MAX_VALUE)
    }

    fun tick() {
        for (pending in waiting.values.toList()) {
            if (!validOwner(pending.owner, pending.session) || !instances.canSpawnBosses(pending.owner)) {
                stop(pending.session.id)
                continue
            }
            if (deps.mapClock.cycle >= pending.spawnAt) {
                waiting.remove(pending.session.id)
                countdown.clear(pending.owner, COUNTDOWN_OWNER)
                spawn(pending.owner, pending.session)
            } else {
                showCountdown(pending)
            }
        }
        for (fight in fights.values.toList()) {
            if (!validOwner(fight)) {
                stop(fight.session.id)
                continue
            }
            if (fight.finished) continue
            if (!fight.boss.isSlotAssigned || fight.boss.hitpoints <= 0) {
                finish(fight)
                continue
            }
            val tick = deps.mapClock.cycle
            val due = fight.pending.filter { it.tick <= tick }
            fight.pending.removeAll(due.toSet())
            for (event in due) {
                if (!fight.finished && validOwner(fight)) event.action()
            }
            processClouds(fight, tick)
            val elapsed = tick - fight.phaseStart
            if (fight.diving) {
                if (elapsed >= fight.phaseEnd + SUBMERGE_TICKS) {
                    fight.phaseIndex++
                    if (fight.phaseIndex >= fight.rotation.size) {
                        fight.rotation = ZulrahRotations.all[deps.random.of(4)]
                        fight.phaseIndex = 1
                    }
                    enterPhase(fight, initial = false)
                }
                continue
            }
            if (elapsed >= fight.phaseEnd) {
                dive(fight)
                continue
            }
            if (elapsed >= EMERGE_TICKS && !fight.attackable) {
                fight.attackable = true
                fight.boss.showAllOps()
            }
            if (elapsed >= fight.nextAction) {
                val action = fight.phase.actions.getOrNull(fight.actionIndex++) ?: ZulrahAction.Attack
                val tailWindupFits = fight.phase.form != ZulrahForm.Melee ||
                    action != ZulrahAction.Attack || elapsed + 4 <= fight.phaseEnd
                if (tailWindupFits) performAction(fight, action)
                fight.nextAction += actionDelay(fight.phase, action)
            }
        }
    }

    override fun validate(player: Player, npc: Npc): NpcAttackValidateResult {
        val fight = npcFights[npc] ?: return NpcAttackValidateResult.Pass
        if (fight.owner !== player || !validOwner(fight)) {
            return NpcAttackValidateResult.Deny("This is another player's encounter.")
        }
        if (fight.finished || (npc === fight.boss && !fight.attackable)) {
            return NpcAttackValidateResult.Deny("Zulrah is beneath the swamp.")
        }
        return NpcAttackValidateResult.BypassSingleWayPvnRestriction
    }

    fun modifyHit(npc: Npc, hit: HitBuilder) {
        val fight = npcFights[npc] ?: return
        if (
            fight.finished || !validOwner(fight) ||
                (hit.isFromPlayer && hit.sourceUid != fight.owner.uid.packed) ||
                (npc === fight.boss && !fight.attackable)
        ) {
            hit.damage = 0
        } else if (npc === fight.boss && hit.damage > 50) {
            hit.damage = 45 + deps.random.of(6)
        }
    }

    fun beginDeath(npc: Npc): CoordGrid? {
        val fight = npcFights[npc] ?: return null
        if (npc !== fight.boss) return null
        if (fight.deathStarted) return null
        fight.deathStarted = true
        finish(fight)
        fight.owner.mes("Zulrah has been defeated.")
        return resolve(fight.session, CoordGrid(2268, 3069, 0))
    }

    fun completeDeath(npc: Npc) {
        val fight = fights.values.firstOrNull { it.boss === npc } ?: return
        if (!fight.deathStarted || npc.isSlotAssigned) return
        fights.remove(fight.session.id)
        npcFights.remove(npc)
        instances.detachNpc(fight.session.id, npc)
        if (validOwner(fight) && instances.canSpawnBosses(fight.owner)) {
            waitForSpawn(fight.owner, fight.session, RESPAWN_SECONDS)
        }
    }

    fun onHitImpact(player: Player, hit: Hit) {
        val fight = fights.values.firstOrNull { it.owner === player } ?: return
        val effect = fight.pendingHits.remove(hit) ?: return
        if (!fight.finished && validOwner(fight) && hit.damage > 0 && effect.venom) {
            PlayerVenom.tryVenom(player)
        }
    }

    fun onNpcDeleted(npc: Npc) {
        val fight = npcFights.remove(npc) ?: return
        fight.minions.remove(npc)
        if (npc !== fight.boss || !fight.deathStarted) instances.detachNpc(fight.session.id, npc)
    }

    fun minionAttack(npc: Npc, target: Player) {
        val fight = npcFights[npc] ?: return
        if (fight.finished || target !== fight.owner || !validOwner(fight)) return
        val minion = fight.minions[npc] ?: return
        if (deps.mapClock.cycle < minion.readyAt) return
        minion.readyAt = deps.mapClock.cycle + 3
        npc.anim("seq.snakeboss_pet_attack")
        val magic = npc.visType.isType("npc.snakeboss_minion_magic")
        val hitType = if (magic) HitType.Magic else HitType.Melee
        val damage = if (accuracy(npc, target, hitType)) deps.random.of(if (magic) 14 else 16) else 0
        val delay = if (magic) projectile(npc, target, "spotanim.snakeboss_minion_spell") else 1
        val hit = target.finishNpcHit(npc, delay, hitType, damage, deps.playerHitModifier)
        fight.pendingHits[hit] = ImpactEffect(venom = true)
    }

    private fun enterPhase(fight: Fight, initial: Boolean) {
        val phase = fight.phase
        val npc = fight.boss
        val npcType = type(phase.form.npcSymbol)
        npc.transmog(npcType, Int.MAX_VALUE)
        assignUid(npc)
        val dest = resolve(fight.session, phase.position.coords)
        PathingEntityCommon.telejump(npc, deps.collision, dest)
        npc.anim(if (initial) "seq.snakeboss_spawn" else "seq.snakeboss_emergefast")
        npc.hideAllOps()
        npc.noneMode()
        npc.clearFacingLock()
        npc.facePlayer(fight.owner)
        fight.phaseStart = deps.mapClock.cycle
        fight.actionIndex = 0
        fight.attackIndex = 0
        fight.nextAction = EMERGE_TICKS
        fight.phaseEnd = maxOf(
            phase.durationTicks ?: 0,
            EMERGE_TICKS + phase.actions.sumOf { actionDelay(phase, it) },
        )
        fight.diving = false
        fight.attackable = false
        val safeTiles = safeTiles(fight, phase)
        val unsafeClouds = fight.clouds.keys.filter { origin ->
            safeTiles.any { it.chebyshevDistance(origin.translate(1, 1)) <= 2 }
        }
        for (origin in unsafeClouds) fight.clouds.remove(origin)?.let { locs.del(it.loc, Int.MAX_VALUE) }
    }

    private fun dive(fight: Fight) {
        fight.diving = true
        fight.attackable = false
        fight.boss.hideAllOps()
        fight.boss.anim("seq.snakeboss_sinkfast")
        schedule(fight, 2) {
            if (fight.boss.isSlotAssigned && !fight.finished) {
                fight.boss.clearQueue("queue.hit")
                deps.npcRepo.hide(fight.boss, 3)
            }
        }
    }

    private fun performAction(fight: Fight, action: ZulrahAction) {
        when (action) {
            ZulrahAction.Attack -> attack(fight)
            ZulrahAction.Cloud -> repeat(2) { launchCloud(fight) }
            ZulrahAction.Snakeling -> launchMinion(fight)
        }
    }

    private fun attack(fight: Fight) {
        val npc = fight.boss
        val player = fight.owner
        if (fight.phase.form == ZulrahForm.Melee) {
            val targetTile = player.coords
            npc.lockFacing(targetTile)
            npc.anim(if (fight.attackIndex++ % 2 == 0) "seq.snakeboss_attack_tail_left" else "seq.snakeboss_attack_tail_right")
            schedule(fight, 3) {
                if (player.coords.chebyshevDistance(targetTile) <= 1) {
                    val hit = player.finishNpcHit(npc, 1, HitType.Typeless, 20 + deps.random.of(11), deps.playerHitModifier)
                    fight.pendingHits[hit] = ImpactEffect()
                    player.frozen = true
                    player.routeDestination.clear()
                    player.timer("timer.combat_freeze", 5)
                }
            }
            return
        }
        val ranged = fight.phase.jadFirstRanged?.let { first -> (fight.attackIndex % 2 == 0) == first }
            ?: (fight.phase.form == ZulrahForm.Ranged || deps.random.of(4) == 0)
        fight.attackIndex++
        if (!rays.hasLineOfSight(npc.coords, player.coords, npc.size, npc.size)) return
        val hitType = if (ranged) HitType.Ranged else HitType.Magic
        npc.anim("seq.snakeboss_attack_acidx1")
        npc.facePlayer(player)
        val delay = projectile(npc, player, if (ranged) "spotanim.snakeboss_orb" else "spotanim.snakeboss_fireball")
        val damage = if (accuracy(npc, player, hitType)) deps.random.of(42) else 0
        val hit = player.finishNpcHit(npc, delay, hitType, damage, deps.playerHitModifier)
        fight.pendingHits[hit] = ImpactEffect(venom = true)
    }

    private fun launchCloud(fight: Fight) {
        val nextPhase = fight.rotation.getOrNull(fight.phaseIndex + 1)
        val safeTiles = safeTiles(fight, fight.phase) + nextPhase?.let { safeTiles(fight, it) }.orEmpty()
        val center = randomWalkable(fight, fight.owner.coords, 5) { tile ->
            safeTiles.none { it.chebyshevDistance(tile) <= 2 } &&
                (-1..1).all { dx -> (-1..1).all { dz -> isWalkable(tile.translate(dx, dz)) } }
        } ?: return
        fight.boss.anim("seq.snakeboss_attack_acidx1")
        val delay = projectile(fight.boss, center, "spotanim.snakeboss_double_orb")
        schedule(fight, delay) {
            val origin = center.translate(-1, -1)
            val existing = fight.clouds.entries.firstOrNull { it.key.chebyshevDistance(origin) <= 2 }
            if (existing != null) {
                existing.value.expiresAt = deps.mapClock.cycle + CLOUD_DURATION
            } else if (fight.clouds.size < 7 && locs.findAll(origin).none { it.layer == 2 }) {
                val loc = LocInfo(2, origin, LocEntity("loc.snakeboss_poisoncloud".asRSCM(RSCMType.LOC), LocShape.CentrepieceStraight.id, LocAngle.West.id))
                if (locs.add(loc, Int.MAX_VALUE)) fight.clouds[origin] = Cloud(loc, deps.mapClock.cycle + CLOUD_DURATION)
            }
        }
    }

    private fun processClouds(fight: Fight, tick: Int) {
        val expired = fight.clouds.filterValues { tick >= it.expiresAt }.keys
        for (tile in expired) fight.clouds.remove(tile)?.let { locs.del(it.loc, Int.MAX_VALUE) }
        val standingInCloud = fight.clouds.keys.any { origin ->
            fight.owner.coords.x in origin.x until origin.x + 3 &&
                fight.owner.coords.z in origin.z until origin.z + 3
        }
        if (standingInCloud && tick >= fight.nextCloudHit) {
            fight.nextCloudHit = tick + 2
            val hit = fight.owner.queueHit(fight.boss, 1, HitType.Typeless, 1 + deps.random.of(5), deps.playerHitModifier)
            fight.pendingHits[hit] = ImpactEffect(venom = true)
        }
    }

    private fun launchMinion(fight: Fight) {
        val tile = randomWalkable(fight, fight.owner.coords, 4) ?: return
        fight.boss.anim("seq.snakeboss_attack_acidx1")
        val delay = projectile(fight.boss, tile, "spotanim.snakeboss_egg")
        schedule(fight, delay) {
            if (fight.minions.size >= 4) return@schedule
            val magic = deps.random.of(2) == 0
            val symbol = if (magic) "npc.snakeboss_minion_magic" else "npc.snakeboss_minion_melee"
            val npc = Npc(type(symbol), tile)
            deps.npcRepo.add(npc, 67)
            npc.respawns = false
            instances.attachNpc(fight.session.id, npc)
            npcFights[npc] = fight
            fight.minions[npc] = Minion(deps.mapClock.cycle + 3)
            npc.anim("seq.snakeboss_pet_spawn")
            if (magic) npc.apPlayer2(fight.owner, interactions) else npc.opPlayer2(fight.owner, interactions)
            schedule(fight, 67) { removeMinion(fight, npc) }
        }
    }

    private fun removeMinion(fight: Fight, npc: Npc) {
        fight.minions.remove(npc)
        npcFights.remove(npc)
        instances.detachNpc(fight.session.id, npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    private fun clearHazards(fight: Fight) {
        fight.pending.clear()
        fight.owner.queueList.removeIf { it.args is Hit && fight.pendingHits.containsKey(it.args) }
        fight.pendingHits.clear()
        for (minion in fight.minions.keys.toList()) removeMinion(fight, minion)
        for (cloud in fight.clouds.values) locs.del(cloud.loc, Int.MAX_VALUE)
        fight.clouds.clear()
    }

    private fun finish(fight: Fight) {
        if (fight.finished) return
        fight.finished = true
        fight.attackable = false
        clearHazards(fight)
    }

    private fun validOwner(fight: Fight): Boolean =
        validOwner(fight.owner, fight.session)

    private fun validOwner(player: Player, session: InstanceSession): Boolean =
        player.isValidTarget() && instances.sessionForPlayer(player) === session

    private fun waitForSpawn(player: Player, session: InstanceSession, seconds: Int) {
        val pending = WaitingSpawn(session, player, deps.mapClock.cycle, seconds)
        waiting[session.id] = pending
        showCountdown(pending)
    }

    private fun showCountdown(pending: WaitingSpawn) {
        val elapsedMillis = (deps.mapClock.cycle - pending.startedAt) * TICK_MILLIS
        val seconds = ((pending.seconds * 1000 - elapsedMillis + 999) / 1000).coerceAtLeast(1)
        countdown.show(pending.owner, COUNTDOWN_OWNER, "Zulrah", pending.spawnAt - deps.mapClock.cycle, seconds)
    }

    private fun schedule(fight: Fight, delay: Int, action: () -> Unit) {
        fight.pending += Pending(deps.mapClock.cycle + delay, action)
    }

    private fun actionDelay(phase: ZulrahPhase, action: ZulrahAction): Int =
        if (phase.form == ZulrahForm.Melee && action == ZulrahAction.Attack) 6 else 3

    private fun safeTiles(fight: Fight, phase: ZulrahPhase): List<CoordGrid> =
        phase.safeOffsets.mapNotNull { (dx, dz) ->
            nearestWalkable(resolve(fight.session, CoordGrid(2266 + dx, 3073 + dz, 0)))
        }

    private fun nearestWalkable(center: CoordGrid): CoordGrid? {
        for (radius in 0..3) {
            for (dx in -radius..radius) for (dz in -radius..radius) {
                val tile = center.translate(dx, dz)
                if (isWalkable(tile)) return tile
            }
        }
        return null
    }

    private fun randomWalkable(fight: Fight, center: CoordGrid, radius: Int, accept: (CoordGrid) -> Boolean = { true }): CoordGrid? {
        val tiles = buildList {
            for (dx in -radius..radius) for (dz in -radius..radius) {
                val tile = center.translate(dx, dz)
                if (tile != fight.owner.coords && isWalkable(tile) && accept(tile)) add(tile)
            }
        }
        return tiles.takeIf { it.isNotEmpty() }?.let { it[deps.random.of(it.size)] }
    }

    private fun isWalkable(tile: CoordGrid): Boolean =
        deps.collision.isZoneValid(tile) &&
            deps.collision[tile.x, tile.z, tile.level] and
            (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR) == 0

    private fun accuracy(npc: Npc, player: Player, hitType: HitType): Boolean = when (hitType) {
        HitType.Ranged -> deps.accuracy.rollRangedAccuracy(npc, player, deps.random)
        HitType.Magic -> deps.accuracy.rollMagicAccuracy(npc, player, deps.random)
        HitType.Melee -> deps.accuracy.rollMeleeAccuracy(npc, player, null, deps.random)
        HitType.Typeless -> true
    }

    private fun projectile(npc: Npc, target: Player, symbol: String): Int {
        val projectile = ProjAnim.fromNpcToPlayer(npc, target, symbol.asRSCM(RSCMType.SPOTANIM), PROJECTILE)
        deps.worldRepo.projAnim(projectile)
        return projectile.serverCycles
    }

    private fun projectile(npc: Npc, target: CoordGrid, symbol: String): Int {
        val projectile = ProjAnim.fromNpcToCoord(npc, target, symbol.asRSCM(RSCMType.SPOTANIM), PROJECTILE)
        deps.worldRepo.projAnim(projectile)
        return projectile.serverCycles
    }

    private fun resolve(session: InstanceSession, coords: CoordGrid): CoordGrid =
        checkNotNull(instances.resolveCoord(session, coords)) { "Zulrah template tile is outside its instance: $coords" }

    @OptIn(InternalApi::class)
    private fun assignUid(npc: Npc) = npc.assignUid()

    private class Fight(val session: InstanceSession, val owner: Player, val boss: Npc, var rotation: List<ZulrahPhase>) {
        var phaseIndex = 0
        var phaseStart = 0
        var phaseEnd = 0
        var nextAction = 0
        var actionIndex = 0
        var attackIndex = 0
        var nextCloudHit = 0
        var diving = false
        var attackable = false
        var finished = false
        var deathStarted = false
        val phase: ZulrahPhase get() = rotation[phaseIndex]
        val pending = mutableListOf<Pending>()
        val pendingHits = IdentityHashMap<Hit, ImpactEffect>()
        val minions = IdentityHashMap<Npc, Minion>()
        val clouds = mutableMapOf<CoordGrid, Cloud>()
    }

    private class Minion(var readyAt: Int)
    private class ImpactEffect(val venom: Boolean = false)
    private data class Pending(val tick: Int, val action: () -> Unit)
    private class Cloud(val loc: LocInfo, var expiresAt: Int)

    private class WaitingSpawn(val session: InstanceSession, val owner: Player, val startedAt: Int, val seconds: Int) {
        val spawnAt = startedAt + (seconds * 1000 + TICK_MILLIS - 1) / TICK_MILLIS
    }

    companion object {
        internal const val FIRST_SPAWN_SECONDS = 5
        internal const val RESPAWN_SECONDS = 10
        private const val TICK_MILLIS = 600
        private const val COUNTDOWN_OWNER = "zulrah"
        private const val EMERGE_TICKS = 4
        private const val SUBMERGE_TICKS = 5
        private const val CLOUD_DURATION = 30
        private val PROJECTILE = ProjAnimType(startHeight = 80, endHeight = 30, delay = 40, angle = 20, lengthAdjustment = 10, progress = 0, stepMultiplier = 2)

        fun type(symbol: String): NpcServerType = checkNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC))) { "Missing Zulrah NPC: $symbol" }
    }
}

private val ZulrahForm.npcSymbol: String get() = when (this) {
    ZulrahForm.Ranged -> "npc.snakeboss_boss_ranged"
    ZulrahForm.Melee -> "npc.snakeboss_boss_melee"
    ZulrahForm.Magic -> "npc.snakeboss_boss_magic"
}

private val ZulrahPosition.coords: CoordGrid get() = when (this) {
    ZulrahPosition.North -> CoordGrid(2266, 3073, 0)
    ZulrahPosition.South -> CoordGrid(2266, 3062, 0)
    ZulrahPosition.East -> CoordGrid(2276, 3071, 0)
    ZulrahPosition.West -> CoordGrid(2256, 3071, 0)
}
