package org.alter.plugins.content.combat

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.attr.COMBAT_ATTACKERS_ATTR
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.FACING_PAWN_ATTR
import org.alter.game.model.attr.INTERACTING_PLAYER_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.move.MovementQueue.StepType
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
        // Hack: Force attack if within this distance for melee (to make NPC stop and face player if they are running away)
        private const val FORCE_ATTACK_DISTANCE_MELEE = 4
    }

    init {
        setCombatLogic {
            pawn.queue {
                while (true) {
                    // Check if target is still valid (not logged out or teleported away)
                    val target = pawn.getCombatTarget()
                    if (target == null) {
                        Combat.reset(pawn)
                        pawn.resetFacePawn()
                        break
                    }

                    // Check if target is a player and if they're still in the area (render distance)
                    // If player is too far away (teleported/logged out), stop combat
                    if (target is Player) {
                        // Safely check if player is still registered in the world
                        // Use try-catch to handle cases where player is being removed
                        val isPlayerInWorld = try {
                            target.index >= 0 && world.players.contains(target)
                        } catch (e: Exception) {
                            false
                        }

                        if (!isPlayerInWorld) {
                            logger.info { "[COMBAT] Target player ${target.username} is no longer in world, resetting combat" }
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }

                        // Check if player is still within render distance (typically 15 tiles)
                        val distance = try {
                            pawn.tile.getDistance(target.tile)
                        } catch (e: Exception) {
                            // If we can't calculate distance, player is likely gone
                            999
                        }

                        if (distance > 15) {
                            logger.info { "[COMBAT] Target player ${target.username} is too far away (distance=$distance), resetting combat" }
                            Combat.reset(pawn)
                            pawn.resetFacePawn()
                            break
                        }

                    }

                    if (!cycle(pawn, this)) {
                        break
                    }
                    wait(1)
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
        val startTime = System.currentTimeMillis()
        val target = pawn.getCombatTarget() ?: return false
        val pawnName = if (pawn is Player) "Player(${(pawn as Player).username})" else "NPC(${(pawn as Npc).id})"
        val targetName = if (target is Player) "Player(${(target as Player).username})" else "NPC(${(target as Npc).id})"

        // Check if pawn or target is dead - END COMBAT IMMEDIATELY
        if (pawn.isDead() || !pawn.isAlive()) {
            logger.info { "[COMBAT] $pawnName -> $targetName: Pawn is dead, ending combat" }
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }
        if (target.isDead() || !target.isAlive()) {
            logger.info { "[COMBAT] $pawnName -> $targetName: Target is dead, ending combat" }
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        // Get combat strategy and calculate ranges
        val strategy = CombatConfigs.getCombatStrategy(pawn)
        val attackRange = strategy.getAttackRange(pawn)
        val pawnSize = pawn.getSize()
        val targetSize = target.getSize()
        val pawnTile = pawn.tile
        val targetTile = target.tile
        val distance = pawnTile.getDistance(targetTile)

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

        val isDiagonalForMelee = isMelee && dx > 0 && dz > 0 && !isCardinallyAdjacent

        // Handle same-tile case
        val areOnSameTile = pawnTile.x == targetTile.x && pawnTile.z == targetTile.z && pawnTile.height == targetTile.height
        if (areOnSameTile) {
            val adjacentTile = findAdjacentWalkableTile(pawn)
            if (adjacentTile != null) {
                pawn.walkTo(adjacentTile)
                while (pawn.hasMoveDestination()) {
                    queue.wait(1)
                    if (!target.isAlive() || !pawn.isAlive()) {
                        return false
                    }
                }
                return cycle(pawn, queue) // Restart cycle after moving away
            }
        }

        // Check if we're in attack range
        val reached = if (distance <= attackRange && (!isMelee || (isCardinallyAdjacent && !isDiagonalForMelee))) {
            pawn.world.reachStrategy.reached(
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
        } else {
            false
        }

        // Hack: Force attack if within 4 tiles for melee (to make NPC stop and face player)
        // This helps catch moving targets by forcing them to stop and turn around
        // Only apply this when NPC is actively moving and BEFORE combat has started (first attack)
        val hasCombatStarted = pawn.attr[Combat.LAST_COMBAT_CYCLE_ATTR] != null
        val pawnIsMoving = pawn.hasMoveDestination()
        val forceAttack = !reached && isMelee && distance <= FORCE_ATTACK_DISTANCE_MELEE && !hasCombatStarted && pawnIsMoving

        // Single-combat check: If NPC is in attack range and player is already in combat with someone else, de-aggress
        // This must happen when NPC is within range, not immediately when combat starts
        if (pawn is Npc && target is Player && !target.tile.isMulti(world) && (reached || forceAttack || distance <= attackRange + 1)) {
            val playerCombatTarget = target.getCombatTarget()
            val playerAttackers = target.attr[COMBAT_ATTACKERS_ATTR]

            // Check if player is in combat with someone else (not this NPC)
            val isPlayerInCombatWithOther = (playerCombatTarget != null && playerCombatTarget != pawn) ||
                (playerAttackers != null && playerAttackers.any { attackerRef ->
                    val attacker = attackerRef.get()
                    attacker != null && attacker != pawn && attacker.isAttacking()
                })

            if (isPlayerInCombatWithOther) {
                logger.info { "[COMBAT] NPC ${(pawn as Npc).id} is in range of player ${target.username} who is already in combat in single-combat area, de-aggressing" }
                Combat.reset(pawn)
                pawn.resetFacePawn()
                return false
            }
        }

        val cycleTime = System.currentTimeMillis() - startTime
        logger.info { "[COMBAT] $pawnName -> $targetName: cycle start - distance=$distance, attackRange=$attackRange, reached=$reached, forceAttack=$forceAttack, pawnTile=(${pawnTile.x},${pawnTile.z}), targetTile=(${targetTile.x},${targetTile.z}), cycleTime=${cycleTime}ms" }

        // If reached or force attack, attack
        if (reached || forceAttack) {
            return handleAttack(pawn, target, strategy, queue, pawnName, targetName)
        }

        // Not reached - need to pathfind
        return handlePathfinding(pawn, target, attackRange, isMelee, pawnSize, targetSize, routeLogic, queue, pawnName, targetName)
    }

    /**
     * Handle attack when in range
     */
    private suspend fun handleAttack(pawn: Pawn, target: Pawn, strategy: CombatStrategy, queue: QueueTask, pawnName: String, targetName: String): Boolean {
        val attackStartTime = System.currentTimeMillis()
        logger.info { "[ATTACK] $pawnName -> $targetName: Starting attack at (${pawn.tile.x},${pawn.tile.z})" }

        // Check if pawn or target is dead - END COMBAT IMMEDIATELY
        if (pawn.isDead() || !pawn.isAlive() || target.isDead() || !target.isAlive()) {
            logger.info { "[ATTACK] $pawnName -> $targetName: Pawn or target is dead, ending combat" }
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        pawn.stopMovement()
        pawn.facePawn(target)

        val canEngage = Combat.canEngage(pawn, target)
        if (!canEngage) {
            val isAlreadyInCombat = pawn.isAttacking() && pawn.getCombatTarget() == target
            if (!isAlreadyInCombat) {
                Combat.reset(pawn)
                pawn.resetFacePawn()
                return false
            }
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
        if (target != pawn.attr[FACING_PAWN_ATTR]?.get()) {
            pawn.facePawn(target)
        }

        // Attack if delay is ready
        if (Combat.isAttackDelayReady(pawn)) {
            if (pawn is Player && AttackTab.isSpecialEnabled(pawn) && pawn.getEquipment(EquipmentType.WEAPON) != null) {
                AttackTab.disableSpecial(pawn)
                if (SpecialAttacks.execute(pawn, target, pawn.world)) {
                    Combat.postAttack(pawn, target)
                    val attackTime = System.currentTimeMillis() - attackStartTime
                    logger.info { "[ATTACK] $pawnName -> $targetName: Special attack executed, time=${attackTime}ms" }
                    return true
                }
                pawn.message("You don't have enough power left.")
            }
            strategy.attack(pawn, target)
            Combat.postAttack(pawn, target)
            val attackTime = System.currentTimeMillis() - attackStartTime
            logger.info { "[ATTACK] $pawnName -> $targetName: Regular attack executed, time=${attackTime}ms" }
        } else {
            logger.info { "[ATTACK] $pawnName -> $targetName: Attack delay not ready" }
        }

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
    private suspend fun handlePathfinding(
        pawn: Pawn,
        target: Pawn,
        attackRange: Int,
        isMelee: Boolean,
        pawnSize: Int,
        targetSize: Int,
        routeLogic: Int,
        queue: QueueTask,
        pawnName: String,
        targetName: String
    ): Boolean {
        // Check if pawn or target is dead - END COMBAT IMMEDIATELY
        if (pawn.isDead() || !pawn.isAlive() || target.isDead() || !target.isAlive()) {
            logger.info { "[PATHFIND] $pawnName -> $targetName: Pawn or target is dead, ending combat" }
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return false
        }

        val pathfindStartTime = System.currentTimeMillis()
        val pawnTile = pawn.tile
        val targetTile = target.tile
        val lastPathTargetPos = pawn.attr[Combat.LAST_PATH_TARGET_POSITION_ATTR]
        val lastSuccessfulWalkPos = pawn.attr[Combat.LAST_SUCCESSFUL_WALK_TARGET_POSITION_ATTR]

        // Check if target moved from last path position, or from last successful walk if no path position
        // Cache position differences to avoid recalculating
        val comparePos = lastPathTargetPos ?: lastSuccessfulWalkPos
        val targetMoved = if (comparePos != null) {
            comparePos.x != targetTile.x ||
            comparePos.z != targetTile.z ||
            comparePos.height != targetTile.height
        } else {
            true // No previous position, assume target moved
        }

        // Calculate how far target moved (in tiles) - reuse position differences
        val targetMoveDistance = if (comparePos != null) {
            val dx = Math.abs(targetTile.x - comparePos.x)
            val dz = Math.abs(targetTile.z - comparePos.z)
            val dh = Math.abs(targetTile.height - comparePos.height)
            dx + dz + dh // Manhattan distance
        } else {
            // No previous position at all - need new path
            Int.MAX_VALUE
        }

        // Check if target moved exactly 1 tile from last successful walk position (for dumb pathfinding)
        val targetMovedOneTile = lastSuccessfulWalkPos != null && targetMoveDistance == 1

        // Determine which pathfinding to use
        // Use dumb pathfinding ONLY if:
        // 1. Original routeLogic is 0 (dumb) AND
        // 2. Target moved exactly 1 tile from last successful walk position
        val useDumbPathfinding = routeLogic == 0 && targetMovedOneTile && lastSuccessfulWalkPos != null
        val actualRouteLogic = if (useDumbPathfinding) 0 else 1

        // Check if we're currently stuck (path calculation failed previously)
        val stuckTargetPos = pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR]
        val stuckPawnPos = pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR]
        val isStuck = stuckTargetPos != null && stuckPawnPos != null

        // If we're stuck, only recalculate if target OR pawn moved from the stuck position
        // This prevents constant recalculation when stuck and neither is moving
        val shouldRecalculateWhenStuck = if (isStuck && stuckTargetPos != null && stuckPawnPos != null) {
            // Check if target moved from stuck position
            val targetMovedFromStuck = stuckTargetPos.x != targetTile.x ||
                stuckTargetPos.z != targetTile.z ||
                stuckTargetPos.height != targetTile.height
            // Check if pawn moved from stuck position
            val pawnMovedFromStuck = stuckPawnPos.x != pawnTile.x ||
                stuckPawnPos.z != pawnTile.z ||
                stuckPawnPos.height != pawnTile.height
            // Recalculate if either moved
            targetMovedFromStuck || pawnMovedFromStuck
        } else {
            true // Not stuck, can recalculate normally
        }

        // Early exit: If we're stuck and neither has moved, skip recalculation entirely
        if (isStuck && !shouldRecalculateWhenStuck) {
            // Still stuck and neither moved - don't recalculate
            // But keep facing the target
            pawn.facePawn(target)
            logger.info { "[PATHFIND] $pawnName -> $targetName: Still stuck, neither moved - skipping path calculation" }
            return true // Continue cycle but don't recalculate
        }

        // If target is moving, be more aggressive about recalculating paths to catch up
        // This helps us catch up to targets moving in a straight line
        val targetIsMoving = target.hasMoveDestination()
        val needsNewPath = if (targetIsMoving) {
            // Target is moving - recalculate if we don't have a path OR if target moved at all
            // This ensures we're always pathing to catch up to moving targets
            (!pawn.hasMoveDestination() || targetMoved) && shouldRecalculateWhenStuck
        } else {
            // Target is stationary - only recalculate if we don't have a path or target moved significantly
            (!pawn.hasMoveDestination() || targetMoved) && shouldRecalculateWhenStuck
        }

        if (needsNewPath) {
            // Stop existing movement
            if (pawn.hasMoveDestination()) {
                pawn.stopMovement()
            }

            logger.info { "[PATHFIND] $pawnName -> $targetName: Calculating new path. Target moved: $targetMoved, moveDistance: $targetMoveDistance, hasMoveDestination: ${pawn.hasMoveDestination()}, useDumbPathfinding: $useDumbPathfinding, targetMovedOneTile: $targetMovedOneTile" }
            // Calculate and execute path
            val pathExecuted = calculateAndExecutePath(
                pawn, target, attackRange, isMelee, pawnSize, targetSize, actualRouteLogic, pawnName, targetName
            )

            if (pathExecuted) {
                // Immediately check if we're already in range (target might be moving towards us or we're close enough)
                // This allows us to start attacking immediately instead of following until target stops
                if (isInRange(pawn, target, attackRange, isMelee, pawnSize, targetSize)) {
                    logger.info { "[PATHFIND] $pawnName -> $targetName: Already in range after path calculation, stopping movement to attack immediately" }
                    pawn.stopMovement()
                    pawn.attr[Combat.LAST_PATH_TARGET_POSITION_ATTR] = targetTile
                    pawn.attr.remove(Combat.STUCK_TARGET_POSITION_ATTR)
                    pawn.attr.remove(Combat.STUCK_PAWN_POSITION_ATTR)
                    // Continue cycle to attack immediately
                    return true
                }

                // Check if path actually gets us closer to target
                // If we're still at the same distance or further, we're effectively stuck
                val currentDistance = pawnTile.getDistance(targetTile)
                val lastStuckTargetPos = pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR]
                val lastStuckPawnPos = pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR]
                val wasStuck = lastStuckTargetPos != null && lastStuckPawnPos != null

                // If we were stuck and target hasn't moved, check if we're still stuck
                if (wasStuck && lastStuckTargetPos != null && lastStuckPawnPos != null) {
                    // Check if target hasn't moved from stuck position
                    val targetNotMoved = lastStuckTargetPos.x == targetTile.x &&
                        lastStuckTargetPos.z == targetTile.z &&
                        lastStuckTargetPos.height == targetTile.height
                    // Check if pawn hasn't moved from stuck position
                    val pawnNotMoved = lastStuckPawnPos.x == pawnTile.x &&
                        lastStuckPawnPos.z == pawnTile.z &&
                        lastStuckPawnPos.height == pawnTile.height

                    // If neither moved and we're still out of range, we're still stuck
                    if (targetNotMoved && pawnNotMoved && currentDistance > attackRange) {
                        logger.info { "[PATHFIND] $pawnName -> $targetName: Path found but still stuck at distance=$currentDistance (neither moved), keeping stuck state" }
                        // Keep stuck state - path didn't help
                        return true // Continue cycle but stay stuck
                    }
                }

                // Check if NPC actually has a move destination after path execution
                // If not, and we're out of range, we might be blocked (fallback route found but can't move)
                // Mark as stuck to prevent constant recalculation
                val hasMoveDestination = pawn.hasMoveDestination()
                val targetIsMoving = target.hasMoveDestination()

                if (!hasMoveDestination && !targetIsMoving && currentDistance > attackRange) {
                    // Path was "executed" but NPC doesn't have a move destination
                    // This likely means fallback route was used but NPC is blocked
                    // Mark as stuck to prevent constant recalculation
                    val canBeStuck = pawn.attr[Combat.CAN_BE_STUCK_ATTR] ?: true
                    if (canBeStuck) {
                        pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR] = targetTile
                        pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR] = pawnTile
                        logger.info { "[PATHFIND] $pawnName -> $targetName: Path executed but no move destination (likely blocked by wall), marking as stuck at pawn (${pawnTile.x},${pawnTile.z}) target (${targetTile.x},${targetTile.z})" }
                        return true // Continue cycle but stay stuck
                    }
                }

                // Also check: if we have a move destination but we were previously stuck and neither moved,
                // the movement might be queued but will fail. Check on next cycle if we actually moved.
                // For now, if we were stuck and still at same position, keep stuck state
                if (wasStuck && lastStuckPawnPos != null) {
                    val pawnNotMoved = lastStuckPawnPos.x == pawnTile.x &&
                        lastStuckPawnPos.z == pawnTile.z &&
                        lastStuckPawnPos.height == pawnTile.height
                    if (pawnNotMoved && currentDistance > attackRange) {
                        // Still at same position - keep stuck state
                        logger.info { "[PATHFIND] $pawnName -> $targetName: Path executed but still at same position, keeping stuck state" }
                        return true // Continue cycle but stay stuck
                    }
                }

                // Track target position when we successfully start a path
                pawn.attr[Combat.LAST_PATH_TARGET_POSITION_ATTR] = targetTile
                // Clear stuck state - we successfully found a path that helps
                pawn.attr.remove(Combat.STUCK_TARGET_POSITION_ATTR)
                pawn.attr.remove(Combat.STUCK_PAWN_POSITION_ATTR)
                // Clear fallback counter on successful path
                pawn.attr.remove(Combat.CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR)
                val pathfindTime = System.currentTimeMillis() - pathfindStartTime
                logger.info { "[PATHFIND] $pawnName -> $targetName: Path executed successfully, tracking target at (${targetTile.x},${targetTile.z}), time=${pathfindTime}ms" }
                // Path executed - continue cycle to check if we're in range
                return true
            } else {
                val pathfindTime = System.currentTimeMillis() - pathfindStartTime
                logger.info { "[PATHFIND] $pawnName -> $targetName: Path execution failed, time=${pathfindTime}ms" }
                // Path calculation failed - mark as stuck and check canBeStuck
                val canBeStuck = pawn.attr[Combat.CAN_BE_STUCK_ATTR] ?: true
                if (canBeStuck) {
                    // Mark as stuck at current target and pawn positions - only recalculate when either moves
                    pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR] = targetTile
                    pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR] = pawnTile
                    logger.info { "[PATHFIND] $pawnName -> $targetName: Path failed and canBeStuck=true - NPC is stuck at pawn (${pawnTile.x},${pawnTile.z}) target (${targetTile.x},${targetTile.z}), will wait for movement" }
                    return true // Continue cycle but NPC stays stuck until target or pawn moves
                } else {
                    // Don't mark as stuck if canBeStuck=false - keep trying
                    logger.info { "[PATHFIND] $pawnName -> $targetName: Path failed but canBeStuck=false - will retry next cycle" }
                    return true // Continue cycle to retry
                }
            }
        } else {
            // Already have a path - CONTINUOUSLY check if we're in range while following
            // This allows us to catch up to moving targets and start combat immediately
            // Check range BEFORE checking if we're stuck, so we can attack immediately when in range
            if (isInRange(pawn, target, attackRange, isMelee, pawnSize, targetSize)) {
                logger.info { "[PATHFIND] $pawnName -> $targetName: Reached range while following path, stopping movement to attack" }
                pawn.stopMovement()
                // Clear stuck state when we reach target
                pawn.attr.remove(Combat.STUCK_TARGET_POSITION_ATTR)
                pawn.attr.remove(Combat.STUCK_PAWN_POSITION_ATTR)
                return true // Continue cycle to attack immediately
            }

            // Calculate current distance for logging
            val currentDistance = pawnTile.getDistance(targetTile)

            // If target is moving, recalculate path every cycle to catch up
            // This is essential for catching moving targets - we need to update our path constantly
            if (target.hasMoveDestination() && targetMoved) {
                // Target is moving and has moved - recalculate path to catch up
                logger.info { "[PATHFIND] $pawnName -> $targetName: Target is moving (distance=$currentDistance), recalculating path to catch up" }
                pawn.stopMovement()
                // Force recalculation by clearing the last path position
                pawn.attr.remove(Combat.LAST_PATH_TARGET_POSITION_ATTR)
                return true // Continue cycle to recalculate
            }

            // Check if we're stuck and neither target nor pawn has moved - don't recalculate
            val currentStuckTargetPos = pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR]
            val currentStuckPawnPos = pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR]
            if (currentStuckTargetPos != null && currentStuckPawnPos != null) {
                val targetDx = Math.abs(targetTile.x - currentStuckTargetPos.x)
                val targetDz = Math.abs(targetTile.z - currentStuckTargetPos.z)
                val targetDh = Math.abs(targetTile.height - currentStuckTargetPos.height)
                val targetNotMoved = targetDx + targetDz + targetDh == 0

                val pawnDx = Math.abs(pawnTile.x - currentStuckPawnPos.x)
                val pawnDz = Math.abs(pawnTile.z - currentStuckPawnPos.z)
                val pawnDh = Math.abs(pawnTile.height - currentStuckPawnPos.height)
                val pawnNotMoved = pawnDx + pawnDz + pawnDh == 0

                if (targetNotMoved && pawnNotMoved) {
                    // Still stuck and neither has moved - don't recalculate
                    // But keep facing the target
                    pawn.facePawn(target)
                    logger.info { "[PATHFIND] $pawnName -> $targetName: Still stuck, neither moved - waiting for movement" }
                    return true // Continue cycle but don't recalculate
                }
            }

            // Not in range yet, continue following path - keep facing target
            // Check range again next cycle (continuous checking while moving)
            pawn.facePawn(target)
            return true // Continue cycle to check again
        }
    }

    /**
     * Calculate and execute a path to the target
     * Returns true if path was successfully executed
     */
    private fun calculateAndExecutePath(
        pawn: Pawn,
        target: Pawn,
        attackRange: Int,
        isMelee: Boolean,
        pawnSize: Int,
        targetSize: Int,
        routeLogic: Int,
        pawnName: String,
        targetName: String
    ): Boolean {
        val calcStartTime = System.currentTimeMillis()
        val pawnTile = pawn.tile
        val targetTile = target.tile

        // Calculate destination tile based on attack range
        val destinationTile = if (isMelee) {
            // For melee, find the closest cardinal adjacent tile (not diagonal)
            // Try all cardinal directions and pick the closest walkable one
            val directions = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
            directions.mapNotNull { dir ->
                val testTile = targetTile.transform(dir.getDeltaX(), dir.getDeltaZ())
                // Check if this tile is walkable and would put us in cardinal adjacency
                if (pawn.world.canTraverse(testTile, dir.getOpposite(), pawn, pawnSize)) {
                    testTile
                } else {
                    null
                }
            }.minByOrNull { it.getDistance(pawnTile) } ?: run {
                // If no cardinal adjacent tile found, try findCardinalAdjacentTile as fallback
                findCardinalAdjacentTile(pawn, target, pawnSize) ?: targetTile
            }
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

        val result = when (routeLogic) {
            1 -> executeSmartRoute(pawn, pawnTile, destinationTile, targetSize, isMelee, pawnSize, pawnName, targetName)
            0 -> executeDumbRoute(pawn, pawnTile, destinationTile, targetSize, isMelee, pawnSize, pawnName, targetName)
            else -> false
        }
        val calcTime = System.currentTimeMillis() - calcStartTime
        logger.info { "[PATHFIND] $pawnName -> $targetName: Path calculation completed, result=$result, time=${calcTime}ms" }
        return result
    }

    /**
     * Execute smart route (A* pathfinding)
     */
    private fun executeSmartRoute(
        pawn: Pawn,
        pawnTile: Tile,
        destinationTile: Tile,
        targetSize: Int,
        isMelee: Boolean,
        pawnSize: Int,
        pawnName: String,
        targetName: String
    ): Boolean {
        val routeStartTime = System.currentTimeMillis()
        val route = pawn.world.smartRouteFinder.findRoute(
            level = pawnTile.height,
            srcX = pawnTile.x,
            srcZ = pawnTile.z,
            destX = destinationTile.x,
            destZ = destinationTile.z,
            locShape = -2,
            destWidth = targetSize,
            destLength = targetSize
        )
        val routeTime = System.currentTimeMillis() - routeStartTime

        if (route.isNotEmpty()) {
            logger.info { "[PATHFIND] $pawnName -> $targetName: Smart route found with ${route.size} tiles from (${pawnTile.x},${pawnTile.z}) to (${destinationTile.x},${destinationTile.z}), routeTime=${routeTime}ms" }
            // Smart route found - reset fallback counter
            pawn.attr.remove(Combat.CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR)
            // Face target while pathfinding
            pawn.facePawn(pawn.getCombatTarget() ?: return false)
            pawn.walkRoute(route, StepType.NORMAL)
            return true
        }

        // Route is empty - this is okay, pathfinding across walls doesn't need to be perfect
        // Try fallback, but if that also fails, it's fine - the NPC will get stuck behind walls
        // which is allowed behavior
        logger.info { "[PATHFIND] $pawnName -> $targetName: Smart route empty, trying fallback, routeTime=${routeTime}ms" }

        // Track consecutive fallback attempts
        val consecutiveFallbacks = pawn.attr[Combat.CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR] ?: 0
        val newConsecutiveFallbacks = consecutiveFallbacks + 1
        pawn.attr[Combat.CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR] = newConsecutiveFallbacks

        // If we've tried fallback 5 times in a row, stop recalculating until player moves
        if (newConsecutiveFallbacks >= 5) {
            logger.info { "[PATHFIND] $pawnName -> $targetName: 5 consecutive fallback attempts, marking as stuck and stopping recalculation until player moves" }
            val target = pawn.getCombatTarget()
            if (target != null) {
                pawn.attr[Combat.STUCK_TARGET_POSITION_ATTR] = target.tile
                pawn.attr[Combat.STUCK_PAWN_POSITION_ATTR] = pawnTile
            }
            return false // Return false to trigger stuck handling
        }

        val fallbackResult = executeFallbackMove(pawn, pawnTile, destinationTile, isMelee, pawnSize, pawnName, targetName)
        if (fallbackResult) {
            // Face target while pathfinding
            pawn.facePawn(pawn.getCombatTarget() ?: return false)
            // Check if NPC actually has a move destination after fallback route
            // If not, the fallback route didn't actually work (NPC is blocked)
            if (!pawn.hasMoveDestination()) {
                logger.info { "[PATHFIND] $pawnName -> $targetName: Fallback route found but NPC cannot move (blocked), marking as stuck" }
                // Reset fallback counter since we're marking as stuck
                pawn.attr.remove(Combat.CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR)
                return false // Return false to trigger stuck handling
            }
            // Fallback succeeded - reset counter
            pawn.attr.remove(Combat.CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR)
        } else {
            logger.info { "[PATHFIND] $pawnName -> $targetName: No path found - NPC may be stuck behind wall (this is allowed)" }
            // Fallback failed - counter will continue on next attempt
        }
        return fallbackResult
    }

    /**
     * Execute dumb route (simple pathfinding)
     * Note: destinationTile here should be the destination tile (already calculated for attack range)
     */
    private fun executeDumbRoute(
        pawn: Pawn,
        pawnTile: Tile,
        destinationTile: Tile,
        targetSize: Int,
        isMelee: Boolean,
        pawnSize: Int,
        pawnName: String,
        targetName: String
    ): Boolean {
        val dumbStartTime = System.currentTimeMillis()
        val destination = pawn.world.dumbRouteFinder.naiveDestination(
            sourceX = pawnTile.x,
            sourceZ = pawnTile.z,
            sourceWidth = pawnSize,
            sourceLength = pawnSize,
            targetX = destinationTile.x,
            targetZ = destinationTile.z,
            targetWidth = targetSize,
            targetLength = targetSize
        )

        val dx = destination.x - pawnTile.x
        val dz = destination.z - pawnTile.z
        val route = LinkedList<Tile>()

        if (isMelee) {
            // Cardinal moves only
            val horizontalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z, pawnTile.height)
            if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, horizontalMove), pawn, pawnSize)) {
                route.add(horizontalMove)
            } else {
                val verticalMove = Tile(pawnTile.x, pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
                if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, verticalMove), pawn, pawnSize)) {
                    route.add(verticalMove)
                }
            }
        } else {
            // Try diagonal, then horizontal, then vertical
            val diagonalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
            if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, diagonalMove), pawn, pawnSize)) {
                route.add(diagonalMove)
            } else {
                val horizontalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z, pawnTile.height)
                if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, horizontalMove), pawn, pawnSize)) {
                    route.add(horizontalMove)
                } else {
                    val verticalMove = Tile(pawnTile.x, pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
                    if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, verticalMove), pawn, pawnSize)) {
                        route.add(verticalMove)
                    }
                }
            }
        }

        val dumbTime = System.currentTimeMillis() - dumbStartTime
        if (route.isNotEmpty()) {
            logger.info { "[PATHFIND] $pawnName -> $targetName: Dumb route found with ${route.size} tiles, time=${dumbTime}ms" }
            // Face target while pathfinding
            pawn.facePawn(pawn.getCombatTarget() ?: return false)
            pawn.walkRoute(route, stepType = StepType.NORMAL)
            return true
        }

        logger.info { "[PATHFIND] $pawnName -> $targetName: Dumb route empty, time=${dumbTime}ms" }
        return false
    }

    /**
     * Fallback: try a single cardinal move towards destination
     */
    private fun executeFallbackMove(
        pawn: Pawn,
        pawnTile: Tile,
        destinationTile: Tile,
        isMelee: Boolean,
        pawnSize: Int,
        pawnName: String,
        targetName: String
    ): Boolean {
        val dx = destinationTile.x - pawnTile.x
        val dz = destinationTile.z - pawnTile.z
        val fallbackRoute = LinkedList<Tile>()

        if (isMelee) {
            // Cardinal moves only
            if (Math.abs(dx) >= Math.abs(dz)) {
                val horizontalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z, pawnTile.height)
                if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, horizontalMove), pawn, pawnSize)) {
                    fallbackRoute.add(horizontalMove)
                } else {
                    val verticalMove = Tile(pawnTile.x, pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
                    if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, verticalMove), pawn, pawnSize)) {
                        fallbackRoute.add(verticalMove)
                    }
                }
            } else {
                val verticalMove = Tile(pawnTile.x, pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
                if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, verticalMove), pawn, pawnSize)) {
                    fallbackRoute.add(verticalMove)
                } else {
                    val horizontalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z, pawnTile.height)
                    if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, horizontalMove), pawn, pawnSize)) {
                        fallbackRoute.add(horizontalMove)
                    }
                }
            }
        } else {
            // Try diagonal, then horizontal, then vertical
            val diagonalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
            if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, diagonalMove), pawn, pawnSize)) {
                fallbackRoute.add(diagonalMove)
            } else {
                val horizontalMove = Tile(pawnTile.x + dx.coerceIn(-1, 1), pawnTile.z, pawnTile.height)
                if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, horizontalMove), pawn, pawnSize)) {
                    fallbackRoute.add(horizontalMove)
                } else {
                    val verticalMove = Tile(pawnTile.x, pawnTile.z + dz.coerceIn(-1, 1), pawnTile.height)
                    if (pawn.world.canTraverse(pawnTile, Direction.between(pawnTile, verticalMove), pawn, pawnSize)) {
                        fallbackRoute.add(verticalMove)
                    }
                }
            }
        }

        if (fallbackRoute.isNotEmpty()) {
            logger.info { "[PATHFIND] $pawnName -> $targetName: Fallback route found with ${fallbackRoute.size} tiles" }
            // Face target while pathfinding
            pawn.facePawn(pawn.getCombatTarget() ?: return false)
            pawn.walkRoute(fallbackRoute, StepType.NORMAL)
            return true
        }

        logger.info { "[PATHFIND] $pawnName -> $targetName: Fallback route also empty" }
        return false
    }

    // Removed followPathUntilReached - pathfinding is now handled directly in handlePathfinding
    // No more waiting loops - immediate recalculation on every cycle

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

            if (!isCardinallyAdjacent || isDiagonal) {
                return false
            }
        }

        return pawn.world.reachStrategy.reached(
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
            val testTile = pawnTile.transform(dir.getDeltaX(), dir.getDeltaZ())
            pawn.world.canTraverse(pawnTile, dir, pawn, pawnSize)
        }?.let { dir ->
            pawnTile.transform(dir.getDeltaX(), dir.getDeltaZ())
        }
    }

    /**
     * Find a cardinal (non-diagonal) adjacent tile next to target for melee combat
     */
    private fun findCardinalAdjacentTile(pawn: Pawn, target: Pawn, pawnSize: Int): Tile? {
        val targetTile = target.tile
        val targetSize = target.getSize()
        val directions = listOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)

        return directions.firstOrNull { dir ->
            val testTile = targetTile.transform(dir.getDeltaX(), dir.getDeltaZ())
            pawn.world.canTraverse(testTile, dir.getOpposite(), pawn, pawnSize)
        }?.let { dir ->
            targetTile.transform(dir.getDeltaX(), dir.getDeltaZ())
        }
    }
}
