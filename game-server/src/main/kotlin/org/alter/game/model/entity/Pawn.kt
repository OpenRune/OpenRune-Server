package org.alter.game.model.entity

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import org.alter.game.action.NpcDeathAction
import org.alter.game.action.PlayerDeathAction
import org.alter.game.event.Event
import org.alter.game.info.NpcInfo
import org.alter.game.info.PlayerInfo
import org.alter.game.model.*
import org.alter.game.model.attr.COMBAT_ATTACKERS_ATTR
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.*
import org.alter.game.model.bits.INFINITE_VARS_STORAGE
import org.alter.game.model.bits.InfiniteVarsType
import org.alter.game.model.collision.rayCast
import org.alter.game.model.combat.DamageMap
import org.alter.game.model.move.MovementQueue
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.move.walkTo
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.queue.QueueTaskSet
import org.alter.game.model.queue.TaskPriority
import org.alter.game.model.queue.impl.PawnQueueTaskSet
import org.alter.game.model.timer.*
import org.alter.game.plugin.Plugin
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.InterruptActionEvent
import org.alter.game.pluginnew.event.impl.TimerEvent
import org.alter.game.service.log.LoggerService
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCMType
import org.rsmod.routefinder.RouteCoordinates
import java.lang.ref.WeakReference
import java.util.*

/**
 * A controllable character in the world that is used by something, or someone,
 * for their own purpose.
 *
 * @author Tom <rspsmods@gmail.com>
 */
abstract class Pawn(val world: World) : Entity() {
    /**
     * The index assigned when this [Pawn] is successfully added to a [PawnList].
     */
    var index = -1

    internal var interaction: Interaction? = null
    /**
     * The 3D [Tile] that this pawn was standing on, in the last game cycle.
     */
    internal var lastTile: Tile? = null

    /**
     * The last tile that was set for the pawn's [org.alter.game.model.region.Chunk].
     */
    internal var lastChunkTile: Tile? = null

    /**
     * Whether or not this pawn can teleported this game cycle.
     */
    internal var moved = false

    /**
     * @see [MovementQueue]
     */
    internal val movementQueue by lazy { MovementQueue(this) }

    /**
     * The current directions that this pawn is moving.
     */
    internal var steps: MovementQueue.StepDirection? = null

    /**
     * The last [Direction] this pawn was facing.
     */
    var lastFacingDirection: Direction = Direction.SOUTH

    /**
     * A public getter property for [lastFacingDirection].
     */
    val faceDirection: Direction
        get() = lastFacingDirection

    /**
     * The current [LockState] which filters what actions this pawn can perform.
     */
    var lock = LockState.NONE

    /**
     * The attributes attached to the pawn.
     *
     * @see AttributeMap
     */
    val attr = AttributeMap()

    /**
     * The timers attached to the pawn.
     *
     * @see TimerMap
     */
    val timers = TimerMap()

    internal val queues: QueueTaskSet = PawnQueueTaskSet()

    /**
     * The equipment bonus for the pawn.
     */
    val equipmentBonuses = IntArray(14)

    /**
     * The current prayer icon that the pawn has active.
     */
    var prayerIcon = -1

    /**
     * Transmog is the action of turning into an npc. This value is equal to the
     * npc id of the npc you want to turn into, visually.
     */
    private var transmogId = -1

    /**
     * A list of pending [Hit]s.
     */
    private val pendingHits = mutableListOf<Hit>()

    /**
     * A [DamageMap] to keep track of who has dealt damage to this pawn.
     */
    val damageMap = DamageMap()

    /**
     * A flag which indicates if this pawn is visible to players in the world.
     */
    var invisible = false

    /**
     * Handles logic before any synchronization tasks are executed.
     */
    abstract fun cycle()

    fun isDead(): Boolean = getCurrentHp() == 0

    fun isAlive(): Boolean = !isDead()

    /**
     * If the player has running enabled.
     * @TODO Rename to proper name this one is bullshit.
     */
    abstract fun isRunning(): Boolean

    abstract fun getSize(): Int

    abstract fun getCurrentHp(): Int

    abstract fun getMaxHp(): Int

    abstract fun setCurrentHp(level: Int)

    /**
     * Lock the pawn to the default [LockState.FULL] state.
     */
    fun lock() {
        lock = LockState.FULL
    }

    /**
     * Unlock the pawn and set it to [LockState.NONE] state.
     */
    fun unlock() {
        lock = LockState.NONE
    }

    /**
     * Checks if the pawn has any lock state set.
     */
    fun isLocked(): Boolean = lock != LockState.NONE
    fun getTransmogId(): Int = transmogId

    fun setTransmogId(transmogId: Int) {
        this.transmogId = transmogId
        if (entityType.isPlayer) {
            PlayerInfo(this as Player).syncAppearance()
        }
    }


    fun getCentreTile(): Tile = tile.transform(getSize() shr 1, getSize() shr 1)

    /**
     * Gets the tile the pawn is currently facing towards.
     */
    // Credits: Kris#1337
    fun getFrontFacingTile(
        target: Tile,
        offset: Int = 0,
    ): Tile {
        val size = (getSize() shr 1)
        val centre = getCentreTile()

        val granularity = 2048
        val lutFactor = (granularity / (Math.PI * 2)) // Lookup table factor

        val theta = Math.atan2((target.z - centre.z).toDouble(), (target.x - centre.x).toDouble())
        var angle = Math.toDegrees((((theta * lutFactor).toInt() + offset) and (granularity - 1)) / lutFactor)
        if (angle < 0) {
            angle += 360
        }
        angle = Math.toRadians(angle)

        val tx = Math.round(centre.x + (size * Math.cos(angle))).toInt()
        val tz = Math.round(centre.z + (size * Math.sin(angle))).toInt()
        return Tile(tx, tz, tile.height)
    }

    /**
     * Alias for [getFrontFacingTile] using a [Pawn] as the target tile.
     */
    fun getFrontFacingTile(
        target: Pawn,
        offset: Int = 0,
    ): Tile = getFrontFacingTile(target.getCentreTile(), offset)

    /**
     * Initiate combat with [target].
     *
     * This sets up the PENDING target and executes the combat plugin.
     * - COMBAT_TARGET_FOCUS_ATTR = who we're TRYING to attack (for pathfinding)
     * - COMBAT_PARTNER_ATTR = who we're ACTUALLY fighting (set only when damage is dealt)
     * - COMBAT_ATTACKERS_ATTR = set only when attack actually goes through (in handleAttack)
     *
     * The actual combat checks (single-combat, cooldowns) are done in canEngage().
     */
    fun attack(target: Pawn) {
        val logger = KotlinLogging.logger {}
        val attackerName = if (this is Player) "Player(${username})" else "NPC(${(this as? Npc)?.id ?: "unknown"})"
        val targetName = if (target is Player) "Player(${target.username})" else "NPC(${(target as? Npc)?.id ?: "unknown"})"

        logger.info { "[COMBAT] $attackerName attack() -> $targetName" }

        // Always face the target
        facePawn(target)

        // Clear old interactions and queues
        resetInteractions()
        interruptQueues()

        // Set PENDING target (who we're TRYING to attack - needed for pathfinding)
        // This does NOT mean we're in combat with them yet
        attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)

        // DON'T add to COMBAT_ATTACKERS_ATTR here - that happens in handleAttack when attack goes through
        // DON'T stop NPC movement here - that happens in handleAttack when attack goes through

        // Execute the combat plugin - it will handle canEngage() checks
        // Players always use default combat; NPCs use custom combat if available, otherwise default
        if (entityType.isPlayer) {
            world.plugins.executeCombat(this)
        } else if (this is Npc) {
            if (!world.plugins.executeNpcCombat(this)) {
                world.plugins.executeCombat(this)
            }
        }
    }

    fun addHit(hit: Hit) {
        pendingHits.add(hit)
    }

    fun clearHits() {
        pendingHits.clear()
    }

    /**
     * Handle a single cycle for [timers].
     */
    fun timerCycle() {
        try {
            val iterator = timers.getTimers().iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val key = entry.key
                val time = entry.value
                if (time <= 0) {
                    if (key == RESET_PAWN_FACING_TIMER) {
                        resetFacePawn()
                    } else {
                        try {
                            EventManager.post(TimerEvent(key,this))
                            world.plugins.executeTimer(this, key)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    if (!timers.has(key)) {
                        iterator.remove()
                    }
                }
            }

            timers.getTimers().entries.forEach { timer ->
                timer.setValue(timer.value - 1)
            }
        }catch (e : Exception){}
    }

    /**
     * Handle a single cycle for [pendingHits].
     *
     * TICK EATING: In OSRS, eating happens BEFORE damage is applied in the same tick.
     * The cycle order is: queues.cycle() (eating) -> cycle() -> hitsCycle() (damage)
     * This means if you eat on the same tick damage would kill you, you survive.
     *
     * damageDelay: Number of ticks before damage is applied.
     * - 0 = damage applies this tick
     * - 1 = damage applies next tick
     * - etc.
     */
    fun hitsCycle() {
        val hitIterator = pendingHits.iterator()
        iterator@ while (hitIterator.hasNext()) {
            if (isDead()) {
                // Clear all pending hits when dead
                pendingHits.clear()
                break
            }
            // For NPCs, also check if they've been removed from the world
            if (entityType.isNpc) {
                val npc = this as Npc
                if (!npc.world.npcs.contains(npc)) {
                    // NPC has been removed from the world, clear pending hits
                    pendingHits.clear()
                    break
                }
            }
            val hit = hitIterator.next()

            // Check if damage is delayed
            if (hit.damageDelay > 0) {
                hit.damageDelay--
                continue // Skip this hit, process next tick
            }

            if (lock.delaysDamage()) {
                // Lock delays damage further
                continue
            }
            if (!hit.cancelCondition()) {
                for (hitmark in hit.hitmarks) {
                    val hp = getCurrentHp()
                    if (hitmark.damage > hp) {
                        hitmark.damage = hp
                    }
                    if (entityType.isNpc) {
                        val npc = this as Npc
                        NpcInfo(npc).addHitMark(
                            sourceIndex = hitmark.attackerIndex,
                            selfType = hitmark.type,
                            value = hitmark.damage,
                            delay = hit.clientDelay,
                        )
                        NpcInfo(npc).addHeadBar(
                            sourceIndex = hitmark.attackerIndex,
                            selfType = 0,
                            startFill = calculateFill(
                                (((this.getCurrentHp().toDouble() - hitmark.damage) / this.getMaxHp()
                                    .toDouble()) * 100), 30
                            )
                        )
                    } else if (entityType.isPlayer) {
                        val player = this as Player
                        player.avatar.extendedInfo.addHitMark(
                            sourceIndex = hitmark.attackerIndex,
                            selfType = hitmark.type,
                            value = hitmark.damage,
                            delay = hit.clientDelay,
                        )
                        player.avatar.extendedInfo.addHeadBar(
                            sourceIndex = hitmark.attackerIndex,
                            selfType = 0,
                            startFill = calculateFill(
                                (((this.getCurrentHp().toDouble() - hitmark.damage) / this.getMaxHp()
                                    .toDouble()) * 100), 30
                            )
                        )
                    }

                    /*
                     * Only lower the pawn's hp if they do not have infinite
                     * health enabled.
                     */
                    if (INFINITE_VARS_STORAGE.get(this, InfiniteVarsType.HP) == 0) {
                        setCurrentHp(hp - hitmark.damage)
                    }

                    // Update combat state when damage is dealt/received
                    if (hitmark.damage > 0) {
                        attr[COMBAT_STATE_ATTR] = CombatState.ACTIVE
                        attr[LAST_COMBAT_ACTION_TIME_ATTR] = System.currentTimeMillis()
                    }

                    /*
                     * If the pawn has less than or equal to 0 health,
                     * terminate all queues and begin the death logic.
                     */
                    if (getCurrentHp() <= 0) {
                        hit.actions.forEach { action -> action(hit) }
                        if (entityType.isPlayer) {
                            executePlugin(PlayerDeathAction.deathPlugin)
                        } else {
                            executePlugin(NpcDeathAction.deathPlugin)
                        }
                        hitIterator.remove()
                        break@iterator
                    }
                }
                hit.actions.forEach { action -> action(hit) }
                hitIterator.remove()
            }
        }
        if (isDead() && pendingHits.isNotEmpty()) {
            pendingHits.clear()
        }
    }

    fun calculateFill(percentage: Double, width: Int): Int {
        val fill = (width * percentage / 100.0).toInt()
        return if (fill == 0 && percentage != 0.0) return 0 else fill
    }

    fun animate(
        id: String,
        delay: Int = 0,
        interruptable: Boolean = false,
    ) {
        RSCM.requireRSCM(RSCMType.SEQTYPES,id)
        if (previouslySetAnim == -1 || interruptable) {
            if (entityType.isPlayer) {
                previouslySetAnim = id.asRSCM()
            }
            if (this is Player) {
                world.plugins.executeOnAnimation(this, id.asRSCM())
            }
        }
        animateSend(RSCM.NONE, 0)
        animateSend(id, delay)
    }

    private fun animateSend(
        id: String,
        startDelay: Int = 0,
    ) {
        if (entityType.isNpc) {
            NpcInfo(this as Npc).setSequence(id.asRSCM(), startDelay)
        } else if (entityType.isPlayer) {
            PlayerInfo(this as Player).setSequence(id.asRSCM(), startDelay)
        }
    }

    abstract fun graphic(
        id: String,
        height: Int = 0,
        delay: Int = 0,
    )

    fun applyTint(
        hue: Int = 0,
        saturation: Int = 0,
        luminance: Int = 0,
        opacity: Int = 0,
        delay: Int = 0,
        duration: Int = 0,
    ) {
        if (entityType.isNpc) {
            NpcInfo(this as Npc).setTinting(
                startTime = delay,
                endTime = duration,
                hue = hue,
                saturation = saturation,
                lightness = luminance,
                weight = opacity,
            )
        } else if (entityType.isPlayer) {
            PlayerInfo(this as Player).tinting(
                hue,
                saturation,
                luminance,
                opacity,
                delay,
                duration
            )
        }
    }

    fun overrideLevel(level: Int) {
        if (entityType.isPlayer) {
            println("Can't override level for a player")
            return
        }
        NpcInfo(this as Npc).setCombatLevelChange(level)
    }

    fun Npc.setTempName(name: String) {
        NpcInfo(this).setTempName(name)
    }

    fun graphic(graphic: Graphic) {
        graphic(graphic.id, graphic.height, graphic.delay)
    }

    fun forceChat(message: String) {
        if (entityType.isNpc) {
            NpcInfo(this as Npc).setSay(message)
        } else if (entityType.isPlayer) {
            PlayerInfo(this as Player).setSay(message)
        }
    }

    fun faceDirection(direction: Direction) {
        faceTile(Tile(direction.getDeltaX(), direction.getDeltaZ()))
    }

    fun faceTile(
        face: Tile,
        width: Int = 1,
        length: Int = 1,
        instant: Boolean = false,
    ) {
        if (entityType.isPlayer) {
            PlayerInfo(this as Player).setFaceCoord(face, width, length)
        } else if (entityType.isNpc) {
            NpcInfo(this as Npc).setFaceCoord(face, width, length, instant)
        }
    }

    fun facePawn(pawn: Pawn) {
        val index = if (pawn.entityType.isPlayer) pawn.index + 65536 else pawn.index
        if (entityType.isNpc) {
            NpcInfo(this as Npc).setFacePathingEntity(index)
        } else if (entityType.isPlayer) {
            PlayerInfo(this as Player).facePawn(index)
        }

        attr[FACING_PAWN_ATTR] = WeakReference(pawn)
    }

    fun resetFacePawn() {
        if (entityType.isNpc) {
            NpcInfo(this as Npc).setFacePathingEntity(-1)
        } else if (entityType.isPlayer) {
            PlayerInfo(this as Player).facePawn(-1)
        }

        attr.remove(FACING_PAWN_ATTR)
    }

    /**
     * Resets any interaction this pawn had with another pawn.
     */
    fun resetInteractions() {
        // Remove this pawn from the target's attacker list
        val target = attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (target != null) {
            target.attr.removeFromSet(COMBAT_ATTACKERS_ATTR, WeakReference(this))
        }
        attr.remove(COMBAT_TARGET_FOCUS_ATTR)
        attr.remove(INTERACTING_NPC_ATTR)
        attr.remove(INTERACTING_PLAYER_ATTR)
        resetFacePawn()
    }

    fun queue(
        priority: TaskPriority = TaskPriority.STANDARD,
        logic: suspend QueueTask.(CoroutineScope) -> Unit,
    ) {
        queues.queue(this, world.coroutineDispatcher, priority, logic)
    }

    /**
     * Terminates any on-going [QueueTask]s that are being executed by this [Pawn].
     */
    fun interruptQueues() {

        if (this is Player) {
            InterruptActionEvent(this).post()
        }

        stopLoopAnimIfActive()
        try {
            queues.terminateTasks()
        }catch (e: Exception) {
            //TODO FIX THIS NOT THREAD SAFE OR SOOMTHING CAN ERROR BUT DOES NOT BREAK
        }
    }

    /**
     * Stops any active looping animation by removing the attribute and setting animation to NONE.
     * This is called when actions are interrupted to ensure animations don't continue playing.
     */
    fun stopLoopAnimIfActive() {
        if (attr.has(LOOPING_ANIMATION_ATTR)) {
            attr.remove(LOOPING_ANIMATION_ATTR)
        }
    }


    /**
     * Executes a plugin with this [Pawn] as its context.
     */
    fun <T> executePlugin(logic: Plugin.() -> T): T {
        val plugin = Plugin(this)
        return logic(plugin)
    }

    fun triggerEvent(event: Event) {
        world.plugins.executeEvent(this, event)
        world.getService(LoggerService::class.java, searchSubclasses = true)?.logEvent(this, event)
    }

    fun isRouteBlocked(item: GroundItem): Boolean {
        val dir = Direction.between(this.tile, item.tile)
        val collisionFlag = this.world.collision.get(item.tile.x, item.tile.z, item.tile.height)
        println("dir: $dir, collisionFlag: $collisionFlag, DirectionFlag: ${Direction.getDirectionFlag(dir)}")
        return (collisionFlag and Direction.getDirectionFlag(dir)) != 0
    }

    /**
     * Checks if the route between the player and the tile is blocked by a collision flag.
     *
     * @param tile The tile that is being checked for being blocked by a collision flag to the pawn.
     * @return True if the route is blocked, false otherwise.
     */
    fun isRouteBlocked(tile: Tile): Boolean {
        val dir = Direction.between(this.tile, tile)
        val collisionFlag = this.world.collision.get(tile.x, tile.z, tile.height)
        return (collisionFlag and Direction.getDirectionFlag(dir)) != 0
    }

    fun hasLineOfSightTo(
        other: Pawn,
        projectile: Boolean,
        maximumDistance: Int = 1,
    ): Boolean {
        if (this.tile.height != other.tile.height) {
            return false
        }

        if (this.tile.sameAs(other.tile)) {
            return true
        }

        if (this.tile.getDistance(other.tile) > maximumDistance) {
            return false
        }

        return this.world.lineValidator.rayCast(this.tile, other.tile, projectile)
    }

    companion object {
        val EMPTY_TILE_DEQUE = ArrayList<RouteCoordinates>()
    }

    /*
just
    if (newAnim.id == -1 ||
        previouslySetAnim == -1 ||
        newAnim.config.priority >= config<AnimationConfig>(previouslySetAnim).priority
    )

if this is true, set the new anim and update the "previouslySetAnim" id to the newAnim.id
end of tick, reset previouslySetAnim to -1
     */
    var previouslySetAnim = -1
}
