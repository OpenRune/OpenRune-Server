package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
internal class ZulrahEncounterController @Inject constructor(
    private val instances: InstanceManager,
    private val npcs: NpcRepository,
    private val players: PlayerList,
    private val locs: LocRepository,
    private val world: WorldRepository,
    private val collision: CollisionFlagMap,
    private val interactions: AiPlayerInteractions,
    private val rayCast: RayCastValidator,
    private val combat: ZulrahCombat,
    private val deps: BossDeps,
) {
    private enum class State { Fighting, Dying, Finished }

    private class Fight(
        val owner: Player,
        val id: InstanceId,
        val npc: Npc,
        val origin: CoordGrid,
    ) {
        var state = State.Fighting
        var submerged = false
        val clouds = mutableMapOf<CoordGrid, Cloud>()
        val snakes = mutableSetOf<Npc>()
        val dyingSnakes = mutableSetOf<Npc>()
        var lastCloudDamageTick = Int.MIN_VALUE
        var exit: LocInfo? = null
        var tailTarget: TailTarget? = null
        var tailStun: TailStun? = null
    }

    private data class Cloud(val loc: LocInfo)
    private class TailTarget(val tile: CoordGrid)
    private class TailStun(
        val previousActionDelay: Int,
        val actionDelay: Int,
        val preserveUntrackedFreeze: Boolean,
        val pausedCombatFreezeExpiry: Int?,
    )

    private val fights = mutableMapOf<InstanceId, Fight>()
    private val npcFights = mutableMapOf<Npc, Fight>()
    private val arenaTypes = mutableMapOf<String, NpcServerType>()

    fun register(owner: Player, session: InstanceSession, npc: Npc) {
        check(session.key == ZulrahIsland.KEY && session.owner == owner.uuid)
        check(instances.sessionForPlayer(owner) === session && owner.uuid in session.occupants)
        check(session.id !in fights)
        val fight = Fight(owner, session.id, npc, npc.coords)
        fights[session.id] = fight
        npcFights[npc] = fight
    }

    fun owns(npc: Npc): Boolean = npc in npcFights

    fun canAttack(player: Player, npc: Npc): Boolean {
        val fight = npcFights[npc] ?: return true
        return fight.owner === player && npc.hitpoints > 0 && valid(fight) &&
            fight.state == State.Fighting && (npc !== fight.npc || !fight.submerged)
    }

    fun modifyHit(npc: Npc, hit: HitBuilder) {
        val fight = npcFights[npc] ?: return
        if (fight.state != State.Fighting || !valid(fight) || (npc === fight.npc && fight.submerged)) {
            hit.damage = 0
        } else if (npc === fight.npc) {
            hit.damage = combat.capIncoming(hit.damage)
        }
    }

    private fun valid(fight: Fight): Boolean {
        if (fights[fight.id] !== fight) return false
        val session = instances.sessionForId(fight.id) ?: return false
        val arrival = instances.resolveCoord(session, ZulrahIsland.arrival) ?: return false
        val owner = fight.owner
        return !owner.loggingOut && !owner.pendingLogout && players.any { it === owner } &&
            instances.sessionForPlayer(owner) === session && owner.uuid in session.occupants &&
            owner.hitpoints > 0 && owner.coords.level == arrival.level &&
            owner.coords.chebyshevDistance(arrival) <= 64
    }

    private fun active(npc: Npc): Fight? =
        npcFights[npc]?.takeIf {
            valid(it) && it.state == State.Fighting && npc.isSlotAssigned && npc.hitpoints > 0
        }

    fun tick(tick: Int) {
        for (fight in fights.values.toList()) {
            if (!valid(fight)) {
                end(fight.id)
                continue
            }
            if (fight.state != State.Fighting || fight.npc.hitpoints <= 0) continue
            targetOwner(fight, fight.npc)
            for (snake in fight.snakes.toList()) {
                if (snake.isSlotAssigned && snake.hitpoints > 0 && snake !in fight.dyingSnakes) {
                    targetOwner(fight, snake)
                }
            }
            if (fight.lastCloudDamageTick >= tick) continue
            fight.lastCloudDamageTick = tick
            if (fight.clouds.keys.any { cloudContains(it, fight.owner.coords) }) {
                combat.cloudDamage(fight.owner)
            }
        }
    }

    fun emerge(npc: Npc, event: ZulrahRoutineEvent) {
        val fight = active(npc) ?: return
        fight.tailTarget = null
        npc.clearFacingLock()
        val type = requireNotNull(ServerCacheManager.getNpc(event.symbol.asRSCM(RSCMType.NPC)))
        npc.transmog(type, Int.MAX_VALUE)
        npc.telejump(collision, fight.origin.translate(event.x, event.z))
        npc.ignoreCombatInteractions = false
        npc.showAllOps()
        fight.submerged = false
        npc.apPlayer2(fight.owner, interactions)
    }

    fun dive(npc: Npc) {
        val fight = active(npc) ?: return
        fight.tailTarget = null
        fight.submerged = true
        npc.hideAllOps()

        // Hiding NPC options does not cancel an existing player attack or spell interaction.
        val owner = fight.owner
        if ((owner.interaction as? InteractionNpc)?.target === npc) {
            owner.clearInteraction()
            owner.abortRoute()
            owner.resetFaceEntity()
            owner.clearMapFlag()
        }
    }

    fun tailWindup(npc: Npc, attack: ZulrahTailAttack) {
        val fight = active(npc) ?: return
        if (npc !== fight.npc || fight.submerged) return
        val target = TailTarget(fight.owner.coords)
        npc.lockFacing(target.tile)
        fight.tailTarget = target
        if (!tailLineOfSight(npc, target.tile)) return
        deps.worldQueues.add(attack.impactDelay) {
            if (active(npc) !== fight || fight.submerged || fight.tailTarget !== target) return@add
            fight.tailTarget = null
            val player = fight.owner
            if (player.coords != target.tile || !tailLineOfSight(npc, player.coords)) return@add
            val hit = combat.tailHit(npc, player, attack.damage)
            if (hit.damage > 0 && active(npc) === fight) stun(fight, attack.stunTicks)
        }
    }

    private fun tailLineOfSight(npc: Npc, tile: CoordGrid): Boolean =
        rayCast.hasLineOfSight(tile, npc.coords, destWidth = npc.size, destLength = npc.size)

    private fun stun(fight: Fight, ticks: Int) {
        clearTailStun(fight)
        val player = fight.owner
        val until = deps.mapClock.cycle + ticks
        val freezeExpiry = combatFreezeExpiry(player)
        val stun = TailStun(
            previousActionDelay = player.actionDelay,
            actionDelay = maxOf(player.actionDelay, until),
            preserveUntrackedFreeze = player.frozen && freezeExpiry == null,
            pausedCombatFreezeExpiry = freezeExpiry?.takeIf { it <= until },
        )
        fight.tailStun = stun
        if (stun.pausedCombatFreezeExpiry != null) player.clearTimer(COMBAT_FREEZE)
        player.frozen = true
        player.actionDelay = stun.actionDelay
        player.routeDestination.clear()
        player.routeRequest = null
        player.resetAnim()
        player.anim("seq.human_stunned")
        player.spotanim("spotanim.stunned", height = STUN_SPOT_HEIGHT, slot = STUN_SPOT_SLOT)
        deps.worldQueues.add(ticks) {
            if (fight.tailStun === stun) clearTailStun(fight)
        }
    }

    private fun clearTailStun(fight: Fight) {
        val stun = fight.tailStun ?: return
        fight.tailStun = null
        val player = fight.owner
        if (player.actionDelay == stun.actionDelay) player.actionDelay = stun.previousActionDelay
        if (combatFreezeExpiry(player) == null && !stun.preserveUntrackedFreeze) {
            val expiry = stun.pausedCombatFreezeExpiry
            when {
                expiry == null -> player.frozen = false
                expiry > deps.mapClock.cycle -> player.timer(COMBAT_FREEZE, expiry - deps.mapClock.cycle)
                else -> CombatEffects.unfreeze(player)
            }
        }
        // Protocol sentinel: clear this effect's slot without clearing other spotanims.
        PathingEntityCommon.spotanim(player, 0xFFFF, 0, 0, STUN_SPOT_SLOT)
    }

    @OptIn(InternalApi::class)
    private fun combatFreezeExpiry(player: Player): Int? {
        val key = COMBAT_FREEZE.asRSCM(RSCMType.TIMER).toShort()
        val timer = player.timerMap[key] ?: return null
        return player.timerMap.extractExpiry(timer)
    }

    fun launchAttack(npc: Npc, event: ZulrahRoutineEvent) {
        val fight = active(npc) ?: return
        if (fight.submerged) return
        val target = fight.owner
        if (!rayCast.hasLineOfSight(target.coords, npc.coords,
                destWidth = npc.size, destLength = npc.size)) return
        npc.clearFacingLock()
        npc.facePlayer(target)
        world.projAnim(projectile(fight, event, target))
        val type = if (event.symbol == "spotanim.snakeboss_fireball") HitType.Magic else HitType.Ranged
        val delay = event.endtime / 30
        if (combat.attack(npc, target, type, 41, delay)) venomOnImpact(fight, delay)
    }

    fun launchHazard(npc: Npc, event: ZulrahRoutineEvent) {
        val fight = active(npc) ?: return
        if (fight.submerged) return
        npc.resetFaceEntity()
        npc.lockFacing(fight.origin.translate(event.target.x, event.target.z))
        world.projAnim(projectile(fight, event))
        deps.worldQueues.add(event.impactDelay) {
            if (active(npc) === fight) impact(fight, event)
        }
    }

    private fun venomOnImpact(fight: Fight, delay: Int) {
        deps.worldQueues.add(delay) {
            if (valid(fight)) PlayerVenom.tryVenom(fight.owner)
        }
    }

    private fun projectile(fight: Fight, event: ZulrahRoutineEvent, target: Player? = null): ProjAnim =
        ProjAnim(
            event.symbol.asRSCM(RSCMType.SPOTANIM), event.startheight, event.endheight,
            event.starttime, event.endtime, event.angle, event.progress, 0,
            if (target != null) -(target.slotId + 1) else 0,
            fight.origin.translate(event.source.x, event.source.z),
            target?.coords ?: fight.origin.translate(event.target.x, event.target.z),
        )

    private fun impact(fight: Fight, event: ZulrahRoutineEvent) {
        val tick = deps.mapClock.cycle
        val tile = fight.origin.translate(event.target.x, event.target.z)
        if (event.kind == "gas") {
            fight.clouds.remove(tile)?.let { locs.del(it.loc, Int.MAX_VALUE) }
            val loc = locs.add(tile, "loc.snakeboss_poisoncloud", Int.MAX_VALUE,
                LocAngle[event.rotation], LocShape.CentrepieceStraight)
            val cloud = Cloud(loc)
            fight.clouds[tile] = cloud
            deps.worldQueues.add(event.cloudLifetime) {
                if (fights[fight.id] === fight && fight.clouds[tile] === cloud) {
                    fight.clouds.remove(tile)
                    locs.del(loc, Int.MAX_VALUE)
                }
            }
        } else {
            val snake = Npc(arenaType(event.spawn), tile)
            if (event.spawn == MAGIC_SNAKE) snake.apRangeOverride = 3
            npcs.add(snake, Int.MAX_VALUE)
            snake.respawns = false
            snake.anim("seq.snakeboss_pet_spawn")
            snake.currentMapClock = tick
            snake.delay(3)
            snake.actionDelay = tick + 3
            fight.snakes += snake
            npcFights[snake] = fight
            instances.attachNpc(fight.id, snake)
            targetOwner(fight, snake)
            deps.encounterRegistry.of(snake).lastAbilityTick = tick
            deps.worldQueues.add(SNAKELING_LIFETIME_TICKS) {
                if (npcFights[snake] === fight && snake.isSlotAssigned && snake !in fight.dyingSnakes) {
                    snake.hitpoints = 0
                    snake.queueDeath()
                }
            }
        }
    }

    private fun arenaType(symbol: String): NpcServerType = arenaTypes.getOrPut(symbol) {
        val cached = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
        cached.copy(maxRange = 64, wanderRange = 0, defaultMode = NpcMode.None).also {
            it.paramMap = cached.paramMap
        }
    }

    private fun targetOwner(fight: Fight, npc: Npc) {
        if (fight.state != State.Fighting) return
        val mode = if (npc === fight.npc || npc.id == MAGIC_SNAKE.asRSCM(RSCMType.NPC)) {
            NpcMode.ApPlayer2
        } else {
            NpcMode.OpPlayer2
        }
        val interaction = npc.interaction as? InteractionPlayer
        if (npc.mode == mode && interaction?.target === fight.owner) return
        if (mode == NpcMode.ApPlayer2) npc.apPlayer2(fight.owner, interactions)
        else npc.opPlayer2(fight.owner, interactions)
    }

    fun snakeAttack(npc: Npc, target: Player) {
        val fight = active(npc) ?: return
        if (fight.owner !== target || npc !in fight.snakes || npc in fight.dyingSnakes) return
        val magic = npc.id == MAGIC_SNAKE.asRSCM(RSCMType.NPC)
        val delay = 1
        if (magic) world.projAnim(ProjAnim(
            "spotanim.snakeboss_minion_spell".asRSCM(RSCMType.SPOTANIM), 60, 65,
            10, 30, 0, 92, 0, -(target.slotId + 1), npc.coords, target.coords,
        ))
        val accurate = combat.attack(npc, target, if (magic) HitType.Magic else HitType.Melee,
            if (magic) 13 else 15, delay)
        if (accurate) venomOnImpact(fight, delay)
    }

    fun beginDeath(npc: Npc): Boolean {
        val fight = npcFights[npc] ?: return false
        if (npc !== fight.npc) {
            if (npc in fight.dyingSnakes || !npc.isSlotAssigned) return false
            fight.dyingSnakes += npc
        } else {
            if (fight.state != State.Fighting) return false
            fight.state = State.Dying
            // Finish the native timer before deletion clears the NPC uid.
            if (valid(fight)) instances.handleBossKill(npc, deps.mapClock.cycle)
            fight.tailTarget = null
            clearTailStun(fight)
            clearClouds(fight)
            for (snake in fight.snakes.toList()) {
                if (snake.isSlotAssigned && snake !in fight.dyingSnakes) {
                    snake.hitpoints = 0
                    snake.delay = deps.mapClock.cycle
                    snake.queueDeath()
                }
            }
        }
        npc.hideAllOps()
        npc.clearInteraction()
        npc.ignoreCombatInteractions = true
        npc.movementLocked = true
        npc.resetFaceEntity()
        return true
    }

    fun finishDeath(npc: Npc, spawnDrops: ((CoordGrid) -> Unit)? = null) {
        val fight = npcFights[npc] ?: return
        if (npc !== fight.npc) {
            removeSnake(fight, npc)
            return
        }
        if (!valid(fight) || fight.state != State.Dying) return
        val dropCoords = fight.owner.coords
        val deathCoords = npc.coords
        fight.state = State.Finished
        if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        try {
            if (spawnDrops != null) {
                // The native table hook reads npc.coords, not spawnDrops' dropCoords.
                // The NPC is already unregistered: no live position or collision is changed.
                npc.coords = dropCoords
                spawnDrops(dropCoords)
            }
        } finally {
            npc.coords = deathCoords
            exitTile(dropCoords)?.let { tile ->
                fight.exit = locs.add(tile, "loc.snakeboss_exit", Int.MAX_VALUE,
                    LocAngle.West, LocShape.CentrepieceStraight)
            }
        }
    }

    private fun exitTile(origin: CoordGrid): CoordGrid? {
        for (radius in 1..2) {
            val candidates = buildList {
                for (x in -radius..radius) for (z in -radius..radius) {
                    if (maxOf(kotlin.math.abs(x), kotlin.math.abs(z)) != radius) continue
                    val tile = origin.translate(x, z)
                    if (rayCast.hasLineOfWalk(origin, tile)) add(tile)
                }
            }
            if (candidates.isNotEmpty()) return deps.random.pick(candidates)
        }
        return null
    }

    fun end(instanceId: InstanceId) {
        val fight = fights.remove(instanceId) ?: return
        npcFights.remove(fight.npc)
        clearHazards(fight)
        fight.exit?.let { locs.del(it, Int.MAX_VALUE) }
        if (fight.npc.isSlotAssigned) npcs.del(fight.npc, Int.MAX_VALUE)
    }

    fun deleted(npc: Npc) {
        val fight = npcFights.remove(npc) ?: return
        fight.snakes.remove(npc)
        fight.dyingSnakes.remove(npc)
        if (npc === fight.npc && fight.state != State.Finished) {
            fights.remove(fight.id)
            clearHazards(fight)
            fight.exit?.let { locs.del(it, Int.MAX_VALUE) }
        }
    }

    private fun removeSnake(fight: Fight, snake: Npc) {
        fight.snakes.remove(snake)
        fight.dyingSnakes.remove(snake)
        npcFights.remove(snake)
        if (snake.isSlotAssigned) npcs.del(snake, Int.MAX_VALUE)
    }

    private fun clearHazards(fight: Fight) {
        fight.tailTarget = null
        clearTailStun(fight)
        clearClouds(fight)
        fight.snakes.toList().forEach { removeSnake(fight, it) }
    }

    private fun clearClouds(fight: Fight) {
        fight.clouds.values.toList().forEach { locs.del(it.loc, Int.MAX_VALUE) }
        fight.clouds.clear()
    }

    companion object {
        private const val COMBAT_FREEZE = "timer.combat_freeze"
        private const val STUN_SPOT_SLOT = 2
        private const val STUN_SPOT_HEIGHT = 100
        const val SNAKELING_LIFETIME_TICKS = 67

        fun cloudContains(origin: CoordGrid, tile: CoordGrid): Boolean =
            tile.level == origin.level && tile.x - origin.x in 0..2 && tile.z - origin.z in 0..2

        const val MELEE_SNAKE = "npc.snakeboss_minion_melee"
        const val MAGIC_SNAKE = "npc.snakeboss_minion_magic"
        val BOSS_FORMS = listOf(ZulrahIsland.RANGED_FORM,
            "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic")
    }
}
