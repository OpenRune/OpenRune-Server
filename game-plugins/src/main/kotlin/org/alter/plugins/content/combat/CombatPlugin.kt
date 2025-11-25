package org.alter.plugins.content.combat

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementQueue.StepType
import org.alter.game.model.move.getMoveDestination
import org.alter.game.model.move.getMovementPath
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.move.stopMovement
import org.alter.game.model.move.walkRoute
import org.alter.game.model.move.walkTo
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.plugins.content.combat.specialattack.SpecialAttacks
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.rsmod.routefinder.RouteCoordinates
import org.rsmod.routefinder.collision.CollisionStrategy
import java.lang.ref.WeakReference
import java.util.LinkedList

/**
 * Combat plugin - handles combat logic for players and NPCs
 */
class CombatPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * Grace distance for combat while moving.
         * In OSRS, due to the tick system (600ms), there's a visual discrepancy between where entities appear
         * and where they actually are (true tile). This grace distance accounts for that by allowing attacks
         * when entities are moving and will be in range within 1-2 ticks.
         *
         * This prevents the issue where you visually look adjacent to an NPC but are actually 3-4 tiles away
         * on the server (true tile), making combat feel unresponsive.
         *
         * Set to 3 to allow attacks at distance 4 (1 + 3) during chase, matching OSRS behavior.
         */
        private const val MOVEMENT_GRACE_DISTANCE = 3
    }

    init {
        setCombatLogic {
            pawn.queue {
                try {
                    // Store attack initiation time when combat starts (if not already set by attack())
                    // attack() sets it using reflection before calling executeCombat()
                    // If reflection fails or attack() didn't set it, we set it here as a fallback
                    if (pawn.attr[ATTACK_INITIATED_TIME_ATTR] == null) {
                        pawn.attr[ATTACK_INITIATED_TIME_ATTR] = System.currentTimeMillis()
                    }

                    // IMMEDIATE FIRST CYCLE: Check if we can attack immediately without waiting
                    // This allows instant attacks when already in range
                    val target = pawn.getCombatTarget()
                    if (target != null && !target.isDead() && target.isAlive()) {
                        val firstCycleResult = try {
                            cycle(pawn, this)
                        } catch (e: Exception) {
                            false
                        }
                        if (!firstCycleResult) {
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            return@queue
                        }
                    }

                    while (true) {
                        // Check if target is still valid (not logged out or teleported away)
                        val target = pawn.getCombatTarget()
                        if (target == null) {
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }

                        // Check if target is dead - END COMBAT IMMEDIATELY
                        if (target.isDead() || !target.isAlive()) {
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }

                    // Check if target is still in the world
                    if (target is Player) {
                        // Safely check if player is still registered in the world
                        val isPlayerInWorld = try {
                            target.index >= 0 && world.players.contains(target)
                        } catch (e: Exception) {
                            false
                        }

                        if (!isPlayerInWorld) {
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }

                        // Check if player is still within render distance (typically 15 tiles)
                        val distance = try {
                            pawn.tile.getDistance(target.tile)
                        } catch (e: Exception) {
                            999
                        }

                        if (distance > 15) {
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }
                    } else if (target is Npc) {
                        // Check if NPC is dead or removed from world
                        val isNpcDead = target.isDead() || !target.isAlive()
                        val isNpcInWorld = try {
                            target.index >= 0 && world.npcs.contains(target)
                        } catch (e: Exception) {
                            false
                        }

                        if (isNpcDead || !isNpcInWorld) {
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }
                    }

                        val cycleResult = try {
                            cycle(pawn, this)
                        } catch (e: Exception) {
                            false
                        }
                        if (!cycleResult) {
                            break
                        }
                        wait(1)
                    }
                } catch (e: Exception) {
                    Combat.reset(pawn)
                    pawn.resetFacePawn()
                }
            }
        }

        onPlayerOption("Attack") {
            val target = pawn.attr[INTERACTING_PLAYER_ATTR]?.get() ?: return@onPlayerOption
            player.attack(target)
        }
    }

    /**
     * Main combat cycle - simplified approach:
     * 1. Check if reached -> if yes, attack
     * 2. If not reached:
     *    - If target moved OR no path -> calculate new path
     *    - Execute path and follow it
     *    - Check each tick if reached
     *    - If path ends but not reached -> recalculate
     */
    suspend fun cycle(pawn: Pawn, queue: QueueTask): Boolean {
        val target = pawn.getCombatTarget() ?: return false
        val pawnTile = pawn.tile
        val targetTile = target.tile
        val pawnName = if (pawn is Player) "Player(${pawn.username})" else "NPC(${(pawn as? Npc)?.id ?: "unknown"})"
        val targetName = if (target is Player) "Player(${target.username})" else "NPC(${(target as? Npc)?.id ?: "unknown"})"

        // Calculate distance first (needed for render distance check)
        val distance = pawnTile.getDistance(targetTile)

        // Update combat state based on time since last action
        val currentTime = System.currentTimeMillis()
        val lastActionTime = pawn.attr[LAST_COMBAT_ACTION_TIME_ATTR] ?: currentTime
        val timeSinceAction = currentTime - lastActionTime
        val currentState = pawn.attr[COMBAT_STATE_ATTR] ?: CombatState.IDLE

        // Transition ACTIVE → ENGAGING after 5 seconds of no combat action
        if (currentState == CombatState.ACTIVE && timeSinceAction > COMBAT_STATE_TIMEOUT_MS) {
            pawn.attr[COMBAT_STATE_ATTR] = CombatState.ENGAGING
            logger.info {
                "[COMBAT-CYCLE] $pawnName -> $targetName: State transition ACTIVE → ENGAGING " +
                "(${timeSinceAction}ms since last action)"
            }
        }

        // Check if target is out of render distance - END COMBAT
        val renderDistance = 15 // OSRS render distance is 15 tiles
        if (distance > renderDistance) {
            logger.info {
                "[COMBAT-CYCLE] $pawnName -> $targetName: Target out of render distance " +
                "(distance=$distance > $renderDistance), ending combat"
            }
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        // Check if pawn or target is dead - END COMBAT IMMEDIATELY
        if (pawn.isDead() || !pawn.isAlive()) {
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }
        if (target.isDead() || !target.isAlive()) {
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        // Get combat strategy and calculate ranges
        val strategy = CombatConfigs.getCombatStrategy(pawn)
        val attackRange = strategy.getAttackRange(pawn)
        val pawnSize = pawn.getSize()
        val targetSize = target.getSize()
        // pawnTile, targetTile, and distance already set above
        val targetMoving = target.hasMoveDestination()

        // Verbose logging removed - use debug level if needed

        // Get route logic (smart vs dumb pathfinding)
        var routeLogic = 1 // Default to smart
        if (pawn.entityType.isNpc) {
            routeLogic = (pawn as Npc).routeLogic
        }

        // Check if we're in range and can attack
        val isMelee = attackRange == 1
        // Cache dx/dz calculations to avoid recalculating
        val dx = if (isMelee) Math.abs(pawnTile.x - targetTile.x) else 0
        val dz = if (isMelee) Math.abs(pawnTile.z - targetTile.z) else 0

        val isCardinallyAdjacent = if (isMelee) {
            val horizontallyAdjacent = dx <= pawnSize && dz == 0
            val verticallyAdjacent = dz <= pawnSize && dx == 0
            val touching = Combat.areBordering(
                pawnTile.x, pawnTile.z, pawnSize, pawnSize,
                targetTile.x, targetTile.z, targetSize, targetSize
            )
            touching && (horizontallyAdjacent || verticallyAdjacent)
        } else {
            true
        }

        // Handle same-tile case
        val areOnSameTile = pawnTile.x == targetTile.x && pawnTile.z == targetTile.z && pawnTile.height == targetTile.height

        if (areOnSameTile) {
            // If already moving, wait for movement to complete (don't queue another walk)
            if (pawn.hasMoveDestination()) {
                return true
            }
            val adjacentTile = findAdjacentWalkableTile(pawn)
            if (adjacentTile != null) {
                // Use walkRoute instead of walkTo to avoid interrupting queues
                // walkRoute just sets up movement without calling interruptQueues
                val route = LinkedList<Tile>()
                route.add(adjacentTile)
                pawn.walkRoute(route, StepType.NORMAL)
                // Return true to continue the cycle - it will check again next tick
                // When movement completes, the cycle will naturally continue with combat
                return true
            }
        }

        // If player is moving (from same-tile walk-away), wait for movement to complete before pathfinding
        // This prevents pathfinding from interrupting the walk-away movement
        // Once movement completes, the cycle will naturally continue and check range
        // Check if we're in attack range with grace distance for moving entities
        //
        // OSRS True Tile System:
        // - Visual position (client animation) != True tile (server position)
        // - Movement happens in discrete ticks (600ms)
        // - Client animates smoothly between ticks, creating visual discrepancy
        // - You may LOOK adjacent to an NPC but actually be 3-4 tiles away (true tile)
        //
        // Solution: Grace distance ONLY for PLAYERS chasing (not NPCs)
        // - If PLAYER is chasing: Allow attack within (attackRange + MOVEMENT_GRACE_DISTANCE)
        // - If NPC is attacking: Use exact attackRange (no grace, must be in true range)
        // - If stationary: Use exact attackRange
        // - This accounts for the fact that entities will move closer on the next tick
        // - "isChasing" includes having a move destination, since movement completes at end of tick
        val isPlayerChasing = pawn is Player && (targetMoving || pawn.hasMoveDestination())
        val effectiveRange = if (isPlayerChasing) attackRange + MOVEMENT_GRACE_DISTANCE else attackRange

        // NPCs must have line of sight - cannot attack through walls/objects
        // For melee: Check if NPC can actually reach the target (no wall between them)
        // For ranged/magic: Check projectile line of sight
        val hasLineOfSight = if (pawn is Npc) {
            if (isMelee && isCardinallyAdjacent) {
                // For melee when cardinally adjacent, check if we can traverse to target
                // This catches walls between adjacent tiles
                val direction = Direction.between(pawnTile, targetTile)
                pawn.world.canTraverse(pawnTile, direction, pawn, pawnSize)
            } else if (isMelee) {
                // For melee at distance, check if we can reach
                pawn.hasLineOfSightTo(target, projectile = false, maximumDistance = effectiveRange)
            } else {
                // For ranged/magic, check projectile line of sight
                pawn.hasLineOfSightTo(target, projectile = true, maximumDistance = effectiveRange)
            }
        } else {
            true // Players always have line of sight (handled differently)
        }
        val reached = distance <= effectiveRange && hasLineOfSight

        // Verbose logging removed - use debug level if needed

        // No force attack - using grace distance system instead
        val forceAttack = false

        // Single-combat check: If NPC has reached the player and player is in ACTIVE combat with someone else, de-aggress
        // This check only happens when NPC reaches attack range, allowing NPCs to continue chasing until they arrive
        // Uses state-based logic: only de-aggress if player is ACTUALLY in combat (dealt/received damage) with another NPC
        // Just targeting another NPC is NOT enough - they must have exchanged damage
        if (pawn is Npc && target is Player && !target.tile.isMulti(world) && reached) {
            val playerCombatTarget = target.getCombatTarget()
            val playerLastActionTime = target.attr[LAST_COMBAT_ACTION_TIME_ATTR] ?: 0L
            val timeSincePlayerAction = System.currentTimeMillis() - playerLastActionTime

            // Only de-aggress if:
            // 1. Player has a different target
            // 2. Player has exchanged damage with that target within the last 5 seconds
            // 3. That target is a different NPC (not this one)
            if (playerCombatTarget != null && playerCombatTarget != pawn && playerCombatTarget is Npc) {
                // Check if the other NPC is actually in combat (has recent action time)
                val otherNpcLastActionTime = playerCombatTarget.attr[LAST_COMBAT_ACTION_TIME_ATTR] ?: 0L
                val timeSinceOtherNpcAction = System.currentTimeMillis() - otherNpcLastActionTime

                // Both player and the other NPC must have recent combat actions (within 5 seconds)
                val playerInActiveCombat = timeSincePlayerAction < COMBAT_STATE_TIMEOUT_MS
                val otherNpcInActiveCombat = timeSinceOtherNpcAction < COMBAT_STATE_TIMEOUT_MS

                if (playerInActiveCombat && otherNpcInActiveCombat) {
                    logger.info {
                        "[COMBAT-CYCLE] $pawnName -> $targetName: Player is in ACTIVE combat with NPC(${playerCombatTarget.id}) " +
                        "(playerAction=${timeSincePlayerAction}ms ago, npcAction=${timeSinceOtherNpcAction}ms ago), de-aggressing"
                    }
                    Combat.reset(pawn)
                    pawn.resetFacePawn()
                    return false
                }
            }
        }

        // If reached or force attack, attack
        if (reached || forceAttack) {
            return handleAttack(pawn, target, strategy, pawnName, targetName)
        }

        // Not reached - need to pathfind
        handlePathfinding(pawn, target, attackRange, isMelee, pawnSize, targetSize)

        // Return true to continue the combat loop
        // The main loop will wait(1) and call cycle() again next tick
        // DON'T wait(1) here - that would cause double-waiting (main loop already waits)
        return true
    }

    /**
     * Handle attack when in range
     */
    private fun handleAttack(pawn: Pawn, target: Pawn, strategy: CombatStrategy, pawnName: String, targetName: String): Boolean {

        // Check if pawn or target is dead - END COMBAT IMMEDIATELY
        if (pawn.isDead() || !pawn.isAlive() || target.isDead() || !target.isAlive()) {
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        // Don't stop movement - allow attacking while moving (keep pace with target)
        pawn.facePawn(target)

        val canEngage = Combat.canEngage(pawn, target)
        if (!canEngage) {
            // canEngage returned false - could be:
            // 1. Player tried to switch targets while in combat (blocked by single-combat rule)
            // 2. Can't attack this NPC for other reasons (slayer, etc.)
            //
            // In case 1: DON'T reset combat - player is still in combat with original partner
            // In case 2: Reset combat
            //
            // Check if player has a combat partner (meaning they're in active combat)
            val combatPartner = pawn.attr[COMBAT_PARTNER_ATTR]?.get()
            if (combatPartner != null && combatPartner != target) {
                // Player is in combat with someone else - this was a blocked switch attempt
                // DON'T reset combat, just return false to end THIS combat queue
                // The original combat with combatPartner continues via NPC retaliation
                return false
            }
            // Otherwise, combat was legitimately blocked (e.g., can't attack this NPC)
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        val canAttackLock = pawn.lock.canAttack()
        if (!canAttackLock) {
            Combat.reset(pawn)
            return false
        }

        if (pawn is Player) {
            pawn.setVarp(Combat.PRIORITY_PID_VARP, target.index)
            if (!pawn.attr.has(Combat.CASTING_SPELL) && pawn.getVarbit(Combat.SELECTED_AUTOCAST_VARBIT) != 0) {
                val spell = CombatSpell.values.firstOrNull { it.autoCastId == pawn.getVarbit(Combat.SELECTED_AUTOCAST_VARBIT) }
                if (spell != null) {
                    pawn.attr[Combat.CASTING_SPELL] = spell
                }
            }
        }

        // Face target
        val currentFacing = pawn.attr[FACING_PAWN_ATTR]?.get()
        if (target != currentFacing) {
            pawn.facePawn(target)
        }

        // Check if attack delay is ready (respects weapon speed)
        if (!Combat.isAttackDelayReady(pawn)) {
            return true // Stay in combat but wait for attack delay
        }

        // Attack if delay is ready
        val currentTime = System.currentTimeMillis()
        if (pawn is Player && AttackTab.isSpecialEnabled(pawn) && pawn.getEquipment(EquipmentType.WEAPON) != null) {
            AttackTab.disableSpecial(pawn)
            if (SpecialAttacks.execute(pawn, target, pawn.world)) {
                // Update combat state, action time, and COMBAT PARTNER for both
                pawn.attr[COMBAT_STATE_ATTR] = CombatState.ACTIVE
                pawn.attr[LAST_COMBAT_ACTION_TIME_ATTR] = currentTime
                pawn.attr[COMBAT_PARTNER_ATTR] = WeakReference(target)
                target.attr[COMBAT_STATE_ATTR] = CombatState.ACTIVE
                target.attr[LAST_COMBAT_ACTION_TIME_ATTR] = currentTime
                target.attr[COMBAT_PARTNER_ATTR] = WeakReference(pawn)
                logger.info { "[COMBAT] $pawnName ATTACK (special) -> $targetName | actionTime=$currentTime" }
                Combat.postAttack(pawn, target)
                return true
            }
            pawn.message("You don't have enough power left.")
        }

        // ========================================
        // ATTACK IS GOING THROUGH - SET ALL COMBAT STATE NOW
        // ========================================

        // Track that we are ACTUALLY attacking this target (only set when attack executes)
        target.attr.addToSet(COMBAT_ATTACKERS_ATTR, WeakReference(pawn))

        // If attacking an NPC that is currently moving, stop their movement
        // This matches OSRS behavior where NPCs stop their path when attacked
        if (target.entityType.isNpc && target.hasMoveDestination()) {
            target.stopMovement()
        }

        // Update combat state, action time, and COMBAT PARTNER for both
        // This is the KEY - COMBAT_PARTNER_ATTR tracks who we're ACTUALLY fighting
        pawn.attr[COMBAT_STATE_ATTR] = CombatState.ACTIVE
        pawn.attr[LAST_COMBAT_ACTION_TIME_ATTR] = currentTime
        pawn.attr[COMBAT_PARTNER_ATTR] = WeakReference(target)
        target.attr[COMBAT_STATE_ATTR] = CombatState.ACTIVE
        target.attr[LAST_COMBAT_ACTION_TIME_ATTR] = currentTime
        target.attr[COMBAT_PARTNER_ATTR] = WeakReference(pawn)
        logger.info { "[COMBAT] $pawnName ATTACK -> $targetName | partner set | actionTime=$currentTime" }

        strategy.attack(pawn, target)
        Combat.postAttack(pawn, target)
        return true
    }

    /**
     * Handle pathfinding when not in range
     * Simple logic: if target moved or we don't have a path, calculate new path
     * Use dumb pathfinding ONLY if target moved exactly 1 tile from last successful walk position
     *
     * Note: NPCs can get stuck behind obstacles by default (canBeStuck = true).
     * This allows players to trap NPCs behind walls/bushes. Set canBeStuck = false
     * on an NPC to make it ultra-smart and always path around obstacles.
     */
    private fun handlePathfinding(
        pawn: Pawn,
        target: Pawn,
        attackRange: Int,
        isMelee: Boolean,
        pawnSize: Int,
        targetSize: Int
    ): Boolean {
        // Check if pawn or target is dead - END COMBAT IMMEDIATELY
        if (pawn.isDead() || !pawn.isAlive() || target.isDead() || !target.isAlive()) {
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        val pawnTile = pawn.tile
        val targetTile = target.tile
        val lastPathTargetPos = pawn.attr[Combat.LAST_PATH_TARGET_POSITION_ATTR]

        // Check if target moved AT ALL from last path position
        val targetMoved = if (lastPathTargetPos != null) {
            lastPathTargetPos.x != targetTile.x ||
            lastPathTargetPos.z != targetTile.z ||
            lastPathTargetPos.height != targetTile.height
        } else {
            true // No previous position, need initial path
        }

        // Check if we're currently stuck (path calculation failed previously)
        val stuckTargetPos = pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR]
        val stuckPawnPos = pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR]
        val isStuck = stuckTargetPos != null && stuckPawnPos != null

        val canBeStuck = pawn.attr[Combat.CAN_BE_STUCK_ATTR] ?: true

        // Pathfinding priority:
        // 1. Players/NPCs with canBeStuck=false: Smart (A*/Dijkstra)
        // 2. NPCs: Dumb pathfinding (diagonal first, then cardinal)
        val actualRouteLogic = when {
            pawn is Player || !canBeStuck -> 1 // Smart pathfinding
            else -> 0 // Dumb pathfinding for NPCs
        }

        // If stuck, check if target or pawn moved - if so, clear stuck state and retry
        if (isStuck && stuckTargetPos != null && stuckPawnPos != null) {
            val targetMovedFromStuck = stuckTargetPos.x != targetTile.x ||
                stuckTargetPos.z != targetTile.z ||
                stuckTargetPos.height != targetTile.height
            val pawnMovedFromStuck = stuckPawnPos.x != pawnTile.x ||
                stuckPawnPos.z != pawnTile.z ||
                stuckPawnPos.height != pawnTile.height

            if (!targetMovedFromStuck && !pawnMovedFromStuck) {
                // Still stuck and neither moved - just face target, don't recalculate
                pawn.facePawn(target)
                return true
            }
            // Target or pawn moved - clear stuck state to retry pathfinding
            pawn.attr.remove(Combat.STUCK_TARGET_POSITION_ATTR)
            pawn.attr.remove(Combat.STUCK_PAWN_POSITION_ATTR)
        }

        // Determine if we need a new path
        // ALWAYS recalculate when:
        // 1. No current path (NPC finished walking or never started)
        // 2. Target moved (even 1 tile - a new path might be available)
        val needsNewPath = when {
            // No current path - always need to calculate
            !pawn.hasMoveDestination() -> true
            // Target moved - recalculate (new parallel/direct path might be available)
            targetMoved -> true
            // Otherwise, keep following current path
            else -> false
        }


        if (needsNewPath) {
            // Don't stop movement - allow attacking while moving
            // The new path will override the old one automatically

            // Calculate and execute path
            val pathExecuted = calculateAndExecutePath(
                pawn, attackRange, isMelee, pawnSize, targetSize, actualRouteLogic
            )
            if (pathExecuted) {
                // Track target position when we successfully start a path
                pawn.attr[Combat.LAST_PATH_TARGET_POSITION_ATTR] = targetTile
                // Clear stuck state - we successfully found a path
                pawn.attr.remove(Combat.STUCK_TARGET_POSITION_ATTR)
                pawn.attr.remove(Combat.STUCK_PAWN_POSITION_ATTR)
                return true
            } else {
                // Path calculation failed - mark as stuck
                val canBeStuck = pawn.attr[Combat.CAN_BE_STUCK_ATTR] ?: true
                if (canBeStuck) {
                    pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR] = targetTile
                    pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR] = pawnTile
                } else {
                }
                return true
            }
        } else {
            // Already have a path - check if we're in range
            val inRange = isInRange(pawn, target, attackRange, isMelee, pawnSize, targetSize)
            if (inRange) {
                pawn.stopMovement()
                pawn.attr.remove(Combat.STUCK_TARGET_POSITION_ATTR)
                pawn.attr.remove(Combat.STUCK_PAWN_POSITION_ATTR)
                return true // Continue cycle to attack
            }

            // Continue following current path
            pawn.facePawn(target)
            return true
        }
    }

    /**
     * Calculate and execute a path to the target
     * Returns true if path was successfully executed
     */
    private fun calculateAndExecutePath(
        pawn: Pawn,
        attackRange: Int,
        isMelee: Boolean,
        pawnSize: Int,
        targetSize: Int,
        routeLogic: Int
    ): Boolean {
        val pawnTile = pawn.tile
        val target = pawn.getCombatTarget() ?: return false
        val targetTile = target.tile

        // Calculate destination tile based on attack range
        // For melee: pass target's tile directly to BFS - BFS will calculate requested tiles (adjacent to target)
        // For ranged/magic: calculate a destination within attack range
        val destinationTile = if (isMelee) {
            // For melee, pass the target's tile directly to BFS
            // BFS will calculate requested tiles as all tiles adjacent to the target (within melee range)
            // For dumb pathfinding, executeDumbRoute will handle moving 1 tile towards target
            targetTile
        } else {
            // For ranged/magic, calculate a destination that's within attackRange from target
            // Don't pathfind to the target's tile - stop when we're in range
            val currentDistance = pawnTile.getDistance(targetTile)
            if (currentDistance <= attackRange) {
                // Already in range, don't move
                pawnTile
            } else {
                // Calculate a tile that's attackRange tiles away from target (in direction from target to pawn)
                // This ensures we pathfind towards the target but stop when in range
                val dx = targetTile.x - pawnTile.x
                val dz = targetTile.z - pawnTile.z
                val distance = Math.max(Math.abs(dx), Math.abs(dz)) // Chebyshev distance
                if (distance > 0) {
                    // Calculate how many steps we need to take to be attackRange away from target
                    val stepsToTake = distance - attackRange
                    if (stepsToTake > 0) {
                        // Move towards target by stepsToTake tiles
                        val stepX = if (dx != 0) (dx.toDouble() / distance * stepsToTake).toInt() else 0
                        val stepZ = if (dz != 0) (dz.toDouble() / distance * stepsToTake).toInt() else 0
                        Tile(pawnTile.x + stepX, pawnTile.z + stepZ, pawnTile.height)
                    } else {
                        // Already close enough, don't move
                        pawnTile
                    }
                } else {
                    targetTile
                }
            }
        }

        return when (routeLogic) {
            1 -> executeSmartRoute(pawn, pawnTile, destinationTile, targetSize, pawnSize)
            0 -> executeDumbRoute(pawn, pawnTile, isMelee)
            else -> executeDumbRoute(pawn, pawnTile, isMelee)
        }
    }

    /**
     * Execute smart route (A-star/Dijkstra pathfinding)
     * Used for players or NPCs with canBeStuck=false
     */
    private fun executeSmartRoute(
        pawn: Pawn,
        pawnTile: Tile,
        destinationTile: Tile,
        targetSize: Int,
        pawnSize: Int
    ): Boolean {
        // Use smart pathfinding (A*/Dijkstra) - available for players or NPCs with canBeStuck=false
        val route = pawn.world.smartRouteFinder.findRoute(
            level = pawnTile.height,
            srcX = pawnTile.x,
            srcZ = pawnTile.z,
            destX = destinationTile.x,
            destZ = destinationTile.z,
            destWidth = targetSize,
            destLength = targetSize,
            srcSize = pawnSize,
            collision = CollisionStrategy.Normal,
            locShape = -2
        )

        if (route.success && route.waypoints.isNotEmpty()) {

            // Face target while pathfinding
            pawn.facePawn(pawn.getCombatTarget() ?: return false)

            // Convert RouteCoordinates to Tiles for walkRoute
            val tilePath = route.waypoints.map { Tile(it.x, it.z, it.level) }
            pawn.walkRoute(LinkedList(tilePath), StepType.NORMAL)
            return true
        }

        // No path found
        return false
    }

    /**
     * Execute dumb route (RuneScape authentic NPC pathfinding)
     *
     * Behavior:
     * 1. TRY DIAGONAL: If both X and Z need adjustment, try diagonal first
     * 2. FALLBACK TO CARDINAL: If diagonal blocked, try horizontal or vertical
     * 3. Builds a FULL PATH to target (not just 1 tile) to avoid stuttering
     *
     * This allows NPCs to move diagonally when possible, making movement faster and more natural.
     */
    private fun executeDumbRoute(
        pawn: Pawn,
        pawnTile: Tile,
        isMelee: Boolean
    ): Boolean {
        val target = pawn.getCombatTarget() ?: return false
        val pawnSize = pawn.getSize()
        val targetTile = target.tile
        val targetIsMoving = target.hasMoveDestination()

        val dx = targetTile.x - pawnTile.x
        val dz = targetTile.z - pawnTile.z
        val route = LinkedList<Tile>()

        // Chebyshev distance
        val currentDistance = Math.max(Math.abs(dx), Math.abs(dz))

        // If already within melee range (1 tile) for melee attacks AND target is not moving, don't move closer
        if (isMelee && currentDistance <= 1 && !targetIsMoving) {
            return false
        }

        // Build a FULL PATH to target
        var currentTile = pawnTile
        val maxSteps = 10
        var steps = 0

        while (steps < maxSteps) {
            val curDx = targetTile.x - currentTile.x
            val curDz = targetTile.z - currentTile.z
            val curDistance = Math.max(Math.abs(curDx), Math.abs(curDz))

            // Stop if we've reached melee range (for melee) or the target tile
            if (isMelee && curDistance <= 1) {
                break
            }
            if (curDistance == 0) {
                break
            }

            var nextTile: Tile? = null
            val stepX = curDx.coerceIn(-1, 1)
            val stepZ = curDz.coerceIn(-1, 1)

            // If both axes need adjustment, try DIAGONAL first
            if (curDx != 0 && curDz != 0) {
                val diagonalMove = Tile(currentTile.x + stepX, currentTile.z + stepZ, currentTile.height)
                if (pawn.world.canTraverse(currentTile, Direction.between(currentTile, diagonalMove), pawn, pawnSize)) {
                    nextTile = diagonalMove
                }
            }

            // If diagonal failed or not applicable, try cardinal directions
            if (nextTile == null) {
                // Prefer the axis with smaller distance (align first)
                if (Math.abs(curDx) <= Math.abs(curDz) && curDx != 0) {
                    // Try horizontal first
                    val horizontalMove = Tile(currentTile.x + stepX, currentTile.z, currentTile.height)
                    if (pawn.world.canTraverse(currentTile, Direction.between(currentTile, horizontalMove), pawn, pawnSize)) {
                        nextTile = horizontalMove
                    }
                }

                if (nextTile == null && curDz != 0) {
                    // Try vertical
                    val verticalMove = Tile(currentTile.x, currentTile.z + stepZ, currentTile.height)
                    if (pawn.world.canTraverse(currentTile, Direction.between(currentTile, verticalMove), pawn, pawnSize)) {
                        nextTile = verticalMove
                    }
                }

                if (nextTile == null && curDx != 0) {
                    // Try horizontal as last resort
                    val horizontalMove = Tile(currentTile.x + stepX, currentTile.z, currentTile.height)
                    if (pawn.world.canTraverse(currentTile, Direction.between(currentTile, horizontalMove), pawn, pawnSize)) {
                        nextTile = horizontalMove
                    }
                }
            }

            if (nextTile != null) {
                route.add(nextTile)
                currentTile = nextTile
                steps++
            } else {
                // Can't move - stuck
                break
            }
        }

        if (route.isNotEmpty()) {
            pawn.facePawn(target)
            pawn.walkRoute(route, stepType = StepType.NORMAL)
            return true
        }

        return false
    }

    /**
     * Check if pawn is in attack range of target
     */
    private fun isInRange(pawn: Pawn, target: Pawn, attackRange: Int, isMelee: Boolean, pawnSize: Int, targetSize: Int): Boolean {
        val pawnTile = pawn.tile
        val targetTile = target.tile
        val distance = pawnTile.getDistance(targetTile)

        if (distance > attackRange) {
            return false
        }

        if (isMelee) {
            val dx = Math.abs(pawnTile.x - targetTile.x)
            val dz = Math.abs(pawnTile.z - targetTile.z)
            val horizontallyAdjacent = dx <= pawnSize && dz == 0
            val verticallyAdjacent = dz <= pawnSize && dx == 0
            val touching = Combat.areBordering(
                pawnTile.x, pawnTile.z, pawnSize, pawnSize,
                targetTile.x, targetTile.z, targetSize, targetSize
            )
            val isCardinallyAdjacent = touching && (horizontallyAdjacent || verticallyAdjacent)
            val isDiagonal = dx > 0 && dz > 0 && !isCardinallyAdjacent
            val reachResult = pawn.world.reachStrategy.reached(
                flags = pawn.world.collision,
                level = pawnTile.height,
                srcX = pawnTile.x,
                srcZ = pawnTile.z,
                destX = targetTile.x,
                destZ = targetTile.z,
                destWidth = targetSize,
                destLength = targetSize,
                srcSize = pawnSize,
                locShape = -2
            )
            return isCardinallyAdjacent && !isDiagonal && reachResult
        }

        val reachResult = pawn.world.reachStrategy.reached(
            flags = pawn.world.collision,
            level = pawnTile.height,
            srcX = pawnTile.x,
            srcZ = pawnTile.z,
            destX = targetTile.x,
            destZ = targetTile.z,
            destWidth = targetSize,
            destLength = targetSize,
            srcSize = pawnSize,
            locShape = -2
        )
        return reachResult
    }

    /**
     * Find an adjacent walkable tile (any direction) for same-tile cases
     */
    private fun findAdjacentWalkableTile(pawn: Pawn): Tile? {
        val directions = listOf(
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.NORTH_EAST, Direction.NORTH_WEST, Direction.SOUTH_EAST, Direction.SOUTH_WEST
        )
        val pawnTile = pawn.tile
        val pawnSize = pawn.getSize()

        return directions.firstOrNull { dir ->
            pawn.world.canTraverse(pawnTile, dir, pawn, pawnSize)
        }?.let { dir ->
            pawnTile.transform(dir.getDeltaX(), dir.getDeltaZ())
        }
    }

    /**
     * Find a cardinal (non-diagonal) adjacent tile next to target for melee combat.
     * Properly considers both pawn size and target size.
     *
     * For a target of size N, we need to find a position where:
     * 1. The pawn (with its size) can be placed
     * 2. The pawn's bounding box is cardinally adjacent to the target's bounding box (bordering)
     * 3. The adjacency is cardinal (not diagonal) - exactly one axis is adjacent
     * 4. The position is walkable
     */
    private fun findCardinalAdjacentTile(pawn: Pawn, target: Pawn, pawnSize: Int): Tile? {
        val targetTile = target.tile
        val targetSize = target.getSize()
        val pawnTile = pawn.tile

        // Target's bounding box
        val targetMinX = targetTile.x
        val targetMaxX = targetTile.x + targetSize - 1
        val targetMinZ = targetTile.z
        val targetMaxZ = targetTile.z + targetSize - 1

        // Try each cardinal direction
        val directions = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)

        for (dir in directions) {
            // Calculate candidate tiles for this direction
            // For cardinal adjacency, pawn must be exactly 1 tile away in this direction
            // and overlap on the perpendicular axis
            val candidateTiles = when (dir) {
                Direction.NORTH -> {
                    // Pawn's maxZ must be targetMinZ - 1 (exactly 1 tile north)
                    // Pawn's z = targetMinZ - pawnSize
                    // Pawn's x must overlap with target's x range for adjacency
                    // Overlap means: pawnMinX <= targetMaxX && pawnMaxX >= targetMinX
                    // So: x <= targetMaxX && x + pawnSize - 1 >= targetMinX
                    // Therefore: x >= targetMinX - pawnSize + 1 && x <= targetMaxX
                    val minX = targetMinX - pawnSize + 1
                    val maxX = targetMaxX
                    (minX..maxX).map { x ->
                        Tile(x, targetMinZ - pawnSize, targetTile.height)
                    }
                }
                Direction.SOUTH -> {
                    // Pawn's minZ must be targetMaxZ + 1 (exactly 1 tile south)
                    // Pawn's z = targetMaxZ + 1
                    // Same X overlap logic as NORTH
                    val minX = targetMinX - pawnSize + 1
                    val maxX = targetMaxX
                    (minX..maxX).map { x ->
                        Tile(x, targetMaxZ + 1, targetTile.height)
                    }
                }
                Direction.EAST -> {
                    // Pawn's minX must be targetMaxX + 1 (exactly 1 tile east)
                    // Pawn's x = targetMaxX + 1
                    // Pawn's z must overlap with target's z range for adjacency
                    // Overlap means: pawnMinZ <= targetMaxZ && pawnMaxZ >= targetMinZ
                    // So: z <= targetMaxZ && z + pawnSize - 1 >= targetMinZ
                    // Therefore: z >= targetMinZ - pawnSize + 1 && z <= targetMaxZ
                    val minZ = targetMinZ - pawnSize + 1
                    val maxZ = targetMaxZ
                    (minZ..maxZ).map { z ->
                        Tile(targetMaxX + 1, z, targetTile.height)
                    }
                }
                Direction.WEST -> {
                    // Pawn's maxX must be targetMinX - 1 (exactly 1 tile west)
                    // Pawn's x = targetMinX - pawnSize
                    // Same Z overlap logic as EAST
                    val minZ = targetMinZ - pawnSize + 1
                    val maxZ = targetMaxZ
                    (minZ..maxZ).map { z ->
                        Tile(targetMinX - pawnSize, z, targetTile.height)
                    }
                }
                else -> emptyList()
            }

            // Try each candidate tile
            for (candidateTile in candidateTiles) {
                // Check if pawn can traverse to this tile
                val directionToCandidate = Direction.between(pawnTile, candidateTile)
                if (directionToCandidate == Direction.NONE) {
                    continue
                }
                if (!pawn.world.canTraverse(pawnTile, directionToCandidate, pawn, pawnSize)) {
                    continue
                }

                // Calculate pawn's bounding box at candidate position
                val pawnMinX = candidateTile.x
                val pawnMaxX = candidateTile.x + pawnSize - 1
                val pawnMinZ = candidateTile.z
                val pawnMaxZ = candidateTile.z + pawnSize - 1

                // Check if bordering (adjacent, not overlapping) - use original sizes for areBordering
                val isBordering = Combat.areBordering(
                    candidateTile.x, candidateTile.z, pawnSize, pawnSize,
                    targetTile.x, targetTile.z, targetSize, targetSize
                )

                if (!isBordering) {
                    continue
                }

                // Check cardinal adjacency: exactly one axis should be adjacent, not both
                // For NORTH/SOUTH: pawn should be adjacent on Z axis, overlap on X axis
                // For EAST/WEST: pawn should be adjacent on X axis, overlap on Z axis
                val isCardinal = when (dir) {
                    Direction.NORTH, Direction.SOUTH -> {
                        // Vertical adjacency: pawn's Z edge touches target's Z edge
                        val zAdjacent = (dir == Direction.NORTH && pawnMaxZ == targetMinZ - 1) ||
                                      (dir == Direction.SOUTH && pawnMinZ == targetMaxZ + 1)
                        // Must overlap on X axis (not diagonal)
                        val xOverlaps = pawnMinX <= targetMaxX && pawnMaxX >= targetMinX
                        zAdjacent && xOverlaps
                    }
                    Direction.EAST, Direction.WEST -> {
                        // Horizontal adjacency: pawn's X edge touches target's X edge
                        val xAdjacent = (dir == Direction.WEST && pawnMaxX == targetMinX - 1) ||
                                       (dir == Direction.EAST && pawnMinX == targetMaxX + 1)
                        // Must overlap on Z axis (not diagonal)
                        val zOverlaps = pawnMinZ <= targetMaxZ && pawnMaxZ >= targetMinZ
                        xAdjacent && zOverlaps
                    }
                    else -> false
                }

                if (isCardinal) {
                    return candidateTile
                }
            }
        }

        return null
    }
}
