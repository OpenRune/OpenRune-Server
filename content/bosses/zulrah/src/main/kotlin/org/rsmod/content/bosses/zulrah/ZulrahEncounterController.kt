package org.rsmod.content.bosses.zulrah

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RayCastValidator
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitBuilder
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
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
    private val objs: ObjRepository,
) {
    internal enum class State { AwaitingContinue, Pending, Fighting, Dying, Finished, EvidenceLimit }
    internal class Fight(val owner: Player, val id: InstanceId) {
        var state = State.AwaitingContinue
        var npc: Npc? = null
        var origin = CoordGrid.ZERO
        var submerged = false
        val clouds = mutableMapOf<CoordGrid, Cloud>()
        val snakes = mutableSetOf<Npc>()
        val dyingSnakes = mutableSetOf<Npc>()
        var lastCloudDamageTick = Int.MIN_VALUE
        var exit: LocInfo? = null
        var lootTile: CoordGrid? = null
        var timerStart = 0
        var durationTicks = 0
        var completionClaimed = false
        var tailTarget: TailTarget? = null
        var tailStun: TailStun? = null
    }
    internal data class Cloud(val loc: LocInfo)
    internal class TailTarget(val tile: CoordGrid)
    internal data class TailStun(
        val previousMovementDelay: Int,
        val previousActionDelay: Int,
        val movementDelay: Int,
        val actionDelay: Int,
    )
    private val fights = mutableMapOf<InstanceId, Fight>()
    private val npcFights = mutableMapOf<Npc, Fight>()
    private val arenaTypes = mutableMapOf<String, NpcServerType>()

    fun enter(player: Player, instanceId: InstanceId) {
        val session = instances.sessionForId(instanceId) ?: return
        if (session.key != ZulrahIsland.KEY || session.owner != player.uuid) return
        if (instances.sessionForPlayer(player) !== session || player.uuid !in session.occupants) return
        fights.putIfAbsent(instanceId, Fight(player, instanceId))
    }

    fun continueEntry(player: Player, instanceId: InstanceId, tick: Int): Boolean {
        val fight = fights[instanceId] ?: return false
        val session = instances.sessionForId(instanceId) ?: return false
        val arrival = instances.resolveCoord(session, ZulrahIsland.arrival) ?: return false
        if (fight.owner !== player || fight.state != State.AwaitingContinue ||
            instances.sessionForPlayer(player) !== session || player.uuid !in session.occupants ||
            players.none { it === player } || player.hitpoints <= 0 ||
            player.coords.level != arrival.level || player.coords.chebyshevDistance(arrival) > 64) return false
        fight.state = State.Pending
        deps.worldQueues.add((tick - deps.mapClock.cycle + 1).coerceAtLeast(1)) {
            if (valid(fight) && fight.state == State.Pending) spawn(fight)
        }
        return true
    }

    fun end(instanceId: InstanceId) {
        val fight = fights.remove(instanceId) ?: return
        clearHazards(fight)
        fight.exit?.let { locs.del(it, Int.MAX_VALUE) }
        fight.lootTile?.let { tile -> objs.findAll(tile).toList().forEach { objs.del(it, Int.MAX_VALUE) } }
        npcFights.entries.removeIf { it.value === fight }
    }

    fun owns(npc: Npc): Boolean = npcFights.containsKey(npc)
    fun state(instanceId: InstanceId): State? = fights[instanceId]?.state
    fun canAttack(player: Player, npc: Npc): Boolean {
        val fight = npcFights[npc] ?: return true
        if (fight.owner !== player || npc.hitpoints <= 0) return false
        return if (npc === fight.npc) fight.state == State.Fighting && !fight.submerged
        else fight.state == State.Fighting || fight.state == State.EvidenceLimit
    }

    fun modifyHit(npc: Npc, hit: HitBuilder) {
        val fight = npcFights[npc] ?: return
        if (npc === fight.npc && (fight.state != State.Fighting || fight.submerged)) hit.damage = 0
        else if (npc === fight.npc) hit.damage = combat.capIncoming(hit.damage)
    }

    private fun valid(fight: Fight): Boolean {
        if (fights[fight.id] !== fight) return false
        val session = instances.sessionForId(fight.id) ?: return false
        val arrival = instances.resolveCoord(session, ZulrahIsland.arrival) ?: return false
        val owner = fight.owner
        return players.any { it === owner } && instances.sessionForPlayer(owner) === session &&
            owner.uuid in session.occupants && owner.hitpoints > 0 &&
            owner.coords.level == arrival.level && owner.coords.chebyshevDistance(arrival) <= 64
    }

    private fun active(npc: Npc): Fight? =
        npcFights[npc]?.takeIf { valid(it) && it.state == State.Fighting && npc.hitpoints > 0 }

    fun evidenceLimit(npc: Npc) {
        val fight = npcFights[npc] ?: return
        if (fight.state != State.Fighting) return
        fight.state = State.EvidenceLimit
        npc.hideAllOps()
        npc.clearInteraction()
        clearClouds(fight)
    }

    fun damageClouds(tick: Int) {
        for (fight in fights.values.toList()) {
            if (fight.state != State.Fighting || fight.lastCloudDamageTick >= tick) continue
            fight.lastCloudDamageTick = tick
            val owner = fight.owner
            val session = instances.sessionForId(fight.id) ?: continue
            if (fight.npc?.hitpoints == 0 || owner.hitpoints <= 0 || players.none { it === owner } ||
                instances.sessionForPlayer(owner) !== session || owner.uuid !in session.occupants) continue
            val inCloud = fight.clouds.keys.any { cloudContains(it, owner.coords) }
            if (inCloud) combat.cloudDamage(owner)
        }
    }

    private fun spawn(fight: Fight) {
        val session = requireNotNull(instances.sessionForId(fight.id))
        fight.origin = requireNotNull(instances.resolveCoord(session, ZulrahIsland.openingSpawn))
        val npc = Npc(arenaType(ZulrahIsland.RANGED_FORM), fight.origin).apply {
            respawnDir = Direction.South
            faceDirection(Direction.South)
            movementLocked = true
            apRangeOverride = 64
            apRequiresLineOfSight = false
            vars["varn.skip_killcount"] = 1
        }
        npcs.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        instances.attachNpc(fight.id, npc)
        fight.npc = npc
        npcFights[npc] = fight
        fight.state = State.Fighting
        fight.timerStart = deps.mapClock.cycle + 3
        npc.apPlayer2(fight.owner, interactions)
    }

    fun emerge(npc: Npc, event: ZulrahRoutineEvent) {
        val fight = active(npc) ?: return
        fight.tailTarget = null
        npc.clearFacingLock()
        // The generic DSL transmog reassigns UID; Zulrah must keep one identity across forms.
        npc.transmog(requireNotNull(ServerCacheManager.getNpc(event.symbol.asRSCM(RSCMType.NPC))), Int.MAX_VALUE)
        npc.telejump(collision, fight.origin.translate(event.x, event.z))
        npc.apPlayer2(fight.owner, interactions)
        npc.showAllOps()
        fight.submerged = false
    }

    fun dive(npc: Npc) {
        val fight = active(npc) ?: return
        fight.tailTarget = null
        fight.submerged = true
        npc.hideAllOps()
        npc.delay(3)
    }

    fun tailWindup(npc: Npc, attack: ZulrahTailAttack) {
        val fight = active(npc) ?: return
        if (npc !== fight.npc || fight.submerged) return
        val tile = fight.owner.coords
        npc.lockFacing(tile)
        val target = TailTarget(tile)
        fight.tailTarget = target
        if (!tailLineOfSight(npc, tile)) return
        deps.worldQueues.add(attack.impactDelay) {
            if (active(npc) !== fight || fight.submerged || fight.tailTarget !== target) return@add
            fight.tailTarget = null
            val player = fight.owner
            if (player.coords != target.tile ||
                !tailLineOfSight(npc, player.coords)) return@add
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
        val stun = TailStun(player.movementDelay, player.actionDelay,
            maxOf(player.movementDelay, until), maxOf(player.actionDelay, until))
        fight.tailStun = stun
        player.movementDelay = stun.movementDelay
        player.actionDelay = stun.actionDelay
        player.routeDestination.clear()
        player.routeRequest = null
        // Replace the same-tick defend animation with the native stun reaction.
        player.resetAnim()
        player.anim("seq.human_stunned")
        player.spotanim("spotanim.stunned", height = STUN_SPOT_HEIGHT, slot = STUN_SPOT_SLOT)
        player.spam("<col=ff0000>You've been stunned!</col>")
        deps.worldQueues.add(ticks) {
            if (fight.tailStun === stun) clearTailStun(fight)
        }
    }

    private fun clearTailStun(fight: Fight) {
        val stun = fight.tailStun ?: return
        fight.tailStun = null
        val player = fight.owner
        if (player.movementDelay == stun.movementDelay) player.movementDelay = stun.previousMovementDelay
        if (player.actionDelay == stun.actionDelay) player.actionDelay = stun.previousActionDelay
        // The cached effect outlasts this stun. Stop only its slot, preserving other effects.
        PathingEntityCommon.spotanim(player, 65535, 0, 0, STUN_SPOT_SLOT)
    }

    private fun faceOwner(npc: Npc) {
        val fight = active(npc) ?: return
        npc.clearFacingLock()
        npc.facePlayer(fight.owner)
    }

    fun launchAttack(npc: Npc, event: ZulrahRoutineEvent) {
        val fight = active(npc) ?: return
        val target = fight.owner
        if (!rayCast.hasLineOfSight(target.coords, npc.coords,
            destWidth = npc.size, destLength = npc.size)) return
        faceOwner(npc)
        world.projAnim(projectile(fight, event, target))
        val type = if (event.symbol == "spotanim.snakeboss_fireball") HitType.Magic else HitType.Ranged
        val delay = event.endtime / 30
        if (combat.attack(npc, target, type, 41, delay)) venomOnImpact(fight, delay)
    }

    fun launchHazard(npc: Npc, event: ZulrahRoutineEvent) {
        val fight = active(npc) ?: return
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
            deps.repeatTick(SNAKELING_LIFETIME_TICKS, onTick = {
                if (!valid(fight) || snake !in fight.snakes || snake.hitpoints <= 0) false
                else {
                    targetOwner(fight, snake)
                    true
                }
            })
        }
    }

    private fun arenaType(symbol: String): NpcServerType = arenaTypes.getOrPut(symbol) {
        val cached = requireNotNull(ServerCacheManager.getNpc(symbol.asRSCM(RSCMType.NPC)))
        // The private arena owns pursuit; generic overworld leashing must not turn summons into wanderers.
        cached.copy(maxRange = 64, wanderRange = 0, defaultMode = NpcMode.None).also {
            it.paramMap = cached.paramMap
        }
    }

    private fun targetOwner(fight: Fight, snake: Npc) {
        if (fight.state != State.Fighting && fight.state != State.EvidenceLimit) return
        val mode = if (snake.id == MAGIC_SNAKE.asRSCM(RSCMType.NPC)) NpcMode.ApPlayer2 else NpcMode.OpPlayer2
        val interaction = snake.interaction as? InteractionPlayer
        if (snake.mode == mode && interaction?.target === fight.owner) return
        if (mode == NpcMode.ApPlayer2) snake.apPlayer2(fight.owner, interactions)
        else snake.opPlayer2(fight.owner, interactions)
    }

    fun snakeAttack(npc: Npc, target: Player) {
        val fight = npcFights[npc] ?: return
        if ((fight.state != State.Fighting && fight.state != State.EvidenceLimit) ||
            fight.owner !== target || target.hitpoints <= 0 ||
            npc.hitpoints <= 0 || !valid(fight)) return
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
            fight.durationTicks = (deps.mapClock.cycle - fight.timerStart).coerceAtLeast(1)
            fight.tailTarget = null
            clearTailStun(fight)
            clearClouds(fight)
            for (snake in fight.snakes.toList()) {
                if (snake !in fight.dyingSnakes) {
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

    fun finishDeath(npc: Npc, spawnLoot: (CoordGrid) -> Unit = {}) {
        val fight = npcFights[npc] ?: return
        if (npc !== fight.npc) {
            removeSnake(fight, npc)
            return
        }
        if (!valid(fight) || fight.state != State.Dying) return
        fight.state = State.Finished
        val tile = fight.owner.coords
        fight.lootTile = tile
        spawnLoot(tile)
        if (npc.isSlotAssigned) npcs.del(npc, Int.MAX_VALUE)
        exitTile(tile)?.let { exit ->
            fight.exit = locs.add(exit, "loc.snakeboss_exit", Int.MAX_VALUE,
                LocAngle.West, LocShape.CentrepieceStraight)
        }
    }

    private fun exitTile(loot: CoordGrid): CoordGrid? {
        for (radius in 1..2) {
            val candidates = buildList {
                for (x in -radius..radius) for (z in -radius..radius) {
                    if (maxOf(kotlin.math.abs(x), kotlin.math.abs(z)) != radius) continue
                    val tile = loot.translate(x, z)
                    if (rayCast.hasLineOfWalk(loot, tile)) add(tile)
                }
            }
            if (candidates.isNotEmpty()) return deps.random.pick(candidates)
        }
        return null
    }

    fun claimCompletion(npc: Npc, hero: Player): Int? {
        val fight = npcFights[npc] ?: return null
        if (npc !== fight.npc || hero !== fight.owner || fight.state != State.Finished ||
            fight.completionClaimed || !valid(fight)) return null
        fight.completionClaimed = true
        return fight.durationTicks
    }

    fun deleted(npc: Npc) {
        val fight = npcFights.remove(npc) ?: return
        fight.snakes.remove(npc)
        fight.dyingSnakes.remove(npc)
        if (npc === fight.npc && fight.state == State.Fighting) {
            fight.state = State.EvidenceLimit
            clearHazards(fight)
        }
    }

    fun mayUseExit(player: Player, tile: CoordGrid): Boolean {
        val fight = instances.sessionForPlayer(player)?.id?.let(fights::get) ?: return false
        return fight.owner === player && fight.state == State.Finished && fight.exit?.coords == tile
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
        const val LOOT_LIFETIME_TICKS = 18000
        const val STUN_SPOT_SLOT = 2
        const val STUN_SPOT_HEIGHT = 100
        const val SNAKELING_LIFETIME_TICKS = 67
        fun cloudContains(origin: CoordGrid, tile: CoordGrid): Boolean =
            tile.level == origin.level && tile.x - origin.x in 0..2 && tile.z - origin.z in 0..2

        const val MELEE_SNAKE = "npc.snakeboss_minion_melee"
        const val MAGIC_SNAKE = "npc.snakeboss_minion_magic"
        val BOSS_FORMS = listOf(ZulrahIsland.RANGED_FORM, "npc.snakeboss_boss_melee", "npc.snakeboss_boss_magic")
    }
}
