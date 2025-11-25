package org.alter.plugins.content.combat

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.model.Tile
import org.alter.game.model.attr.*
import org.alter.game.model.collision.rayCast
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.game.model.timer.ATTACK_DELAY
import org.alter.plugins.content.combat.strategy.CombatStrategy
import org.alter.plugins.content.combat.strategy.MagicCombatStrategy
import org.alter.plugins.content.combat.strategy.MeleeCombatStrategy
import org.alter.plugins.content.combat.strategy.RangedCombatStrategy
import org.alter.plugins.content.combat.strategy.magic.CombatSpell
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.interfaces.attack.AttackTab
import org.alter.game.model.move.MovementQueue.StepType
import org.alter.game.model.move.walkRoute
import org.alter.game.model.move.hasMoveDestination
import org.alter.game.model.move.stopMovement
import java.lang.ref.WeakReference

/**
 * @author Tom <rspsmods@gmail.com>
 */
object Combat {
    private val logger = KotlinLogging.logger {}

    // Combat-specific attributes (spell casting, damage modifiers)
    val CASTING_SPELL = AttributeKey<CombatSpell>()
    val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
    val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()
    val BOLT_ENCHANTMENT_EFFECT = AttributeKey<Boolean>()
    val ALWAYS_MAX_HIT = AttributeKey<Boolean>()
    val DRAGON_BATTLEAXE_BONUS = AttributeKey<Double>()

    // Pathfinding tracking attributes (combat-specific, not shared)
    val WAS_IN_RANGE_WHEN_STOPPED_ATTR = AttributeKey<Boolean>(temp = true)
    val LAST_PATH_TARGET_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    val LAST_PATH_CALCULATION_CYCLE_ATTR = AttributeKey<Int>(temp = true)
    val LAST_SUCCESSFUL_WALK_TARGET_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    val CAN_BE_STUCK_ATTR = AttributeKey<Boolean>(temp = true)
    val STUCK_TARGET_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    val STUCK_PAWN_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    val CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR = AttributeKey<Int>(temp = true)
    val STUCK_AXIS_ATTR = AttributeKey<String>(temp = true)

    const val PRIORITY_PID_VARP = "varp.playertarget"
    const val SELECTED_AUTOCAST_VARBIT = "varbits.autocast_spell"
    const val DEFENSIVE_MAGIC_CAST_VARBIT = "varbits.autocast_defmode"
    const val COMBAT_TIMEOUT_TICKS = 50

    fun reset(pawn: Pawn) {
        // Remove this pawn from the target's attacker list
        val target = pawn.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (target != null) {
            target.attr.removeFromSet(COMBAT_ATTACKERS_ATTR, WeakReference(pawn))
        }
        pawn.attr.remove(COMBAT_TARGET_FOCUS_ATTR)
        // Clear combat state
        pawn.attr[COMBAT_STATE_ATTR] = CombatState.IDLE
        pawn.attr.remove(LAST_COMBAT_ACTION_TIME_ATTR)
        pawn.attr.remove(COMBAT_PARTNER_ATTR)  // Clear combat partner
        pawn.attr.remove(SWITCHING_FROM_TARGET_ATTR)  // Clear switching state
        // Clear movement tracking attributes
        pawn.attr.remove(WAS_IN_RANGE_WHEN_STOPPED_ATTR)
        pawn.attr.remove(LAST_PATH_TARGET_POSITION_ATTR)
        pawn.attr.remove(LAST_PATH_CALCULATION_CYCLE_ATTR)
        pawn.attr.remove(LAST_SUCCESSFUL_WALK_TARGET_POSITION_ATTR)
        pawn.attr.remove(STUCK_TARGET_POSITION_ATTR)
        pawn.attr.remove(STUCK_PAWN_POSITION_ATTR)
        pawn.attr.remove(CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR)
    }

    fun canAttack(
        pawn: Pawn,
        target: Pawn,
        combatClass: CombatClass,
    ): Boolean = canEngage(pawn, target) && getStrategy(combatClass).canAttack(pawn, target)

    fun canAttack(
        pawn: Pawn,
        target: Pawn,
        strategy: CombatStrategy,
    ): Boolean = canEngage(pawn, target) && strategy.canAttack(pawn, target)

    fun isAttackDelayReady(pawn: Pawn): Boolean = !pawn.timers.has(ATTACK_DELAY)

    fun postAttack(
        pawn: Pawn,
        target: Pawn,
    ) {
        pawn.timers[ATTACK_DELAY] = CombatConfigs.getAttackDelay(pawn)
        target.timers[ACTIVE_COMBAT_TIMER] = 17 // 10,2 seconds
        pawn.attr[BOLT_ENCHANTMENT_EFFECT] = false

        pawn.attr[LAST_HIT_ATTR] = WeakReference(target)
        target.attr[LAST_HIT_BY_ATTR] = WeakReference(pawn)

        // Track combat time for both pawn and target (for 10-second timeout logic)
        val currentCycle = pawn.world.currentCycle
        pawn.attr[LAST_COMBAT_CYCLE_ATTR] = currentCycle
        target.attr[LAST_COMBAT_CYCLE_ATTR] = currentCycle

        if (pawn.attr.has(CASTING_SPELL) && pawn is Player && pawn.getVarbit(SELECTED_AUTOCAST_VARBIT) == 0) {
            reset(pawn)
            pawn.attr.remove(CASTING_SPELL)
        }

        if (target is Player && target.interfaces.getModal() != -1) {
            target.closeInterface(target.interfaces.getModal())
            target.interfaces.setModal(-1)
        }
    }

    fun postDamage(
        pawn: Pawn,
        target: Pawn,
    ) {
        // If target or attacker is dead, end combat immediately
        if (target.isDead() || !target.isAlive() || pawn.isDead() || !pawn.isAlive()) {
            Combat.reset(pawn)
            pawn.resetFacePawn()
            return
        }

        // Track combat time when damage is dealt (for 10-second timeout logic)
        val currentCycle = pawn.world.currentCycle
        pawn.attr[LAST_COMBAT_CYCLE_ATTR] = currentCycle
        target.attr[LAST_COMBAT_CYCLE_ATTR] = currentCycle

        /*
         * Don't override the animation if one is already set. @Z-Kris
         */
        val hasBlock = target.previouslySetAnim != -1

        if (!hasBlock) {
            target.animate(CombatConfigs.getBlockAnimation(target))
            if (target is Npc) {
                val npcDefs = target.combatDef
                if (npcDefs.defaultBlockSoundArea) {
                    target.world.spawn(
                        AreaSound(target.tile, npcDefs.defaultBlockSound, npcDefs.defaultBlockSoundRadius, npcDefs.defaultBlockSoundVolume),
                    )
                } else {
                    (pawn as Player).playSound(npcDefs.defaultBlockSound, npcDefs.defaultBlockSoundVolume)
                }
            }
        }

        if (target.lock.canAttack()) {
            if (target.entityType.isNpc) {
                if (!target.attr.has(COMBAT_TARGET_FOCUS_ATTR) || target.attr[COMBAT_TARGET_FOCUS_ATTR]!!.get() != pawn) {
                    target.attack(pawn)
                }
            } else if (target is Player) {
                // Auto retaliate: check if enabled and target is not already in combat
                val autoRetaliateEnabled = target.getVarp(AttackTab.DISABLE_AUTO_RETALIATE_VARP) == 0
                if (autoRetaliateEnabled && target.getCombatTarget() != pawn) {
                    val strategy = CombatConfigs.getCombatStrategy(target)
                    val attackRange = strategy.getAttackRange(target)

                    // If within range, attack immediately
                    if (target.tile.isWithinRadius(pawn.tile, attackRange + pawn.getSize())) {
                        target.attack(pawn)
                    } else {
                        // Pathfind to attacker and then attack
                        target.queue {
                            val route = target.world.smartRouteFinder.findRoute(
                                level = target.tile.height,
                                srcX = target.tile.x,
                                srcZ = target.tile.z,
                                destX = pawn.tile.x,
                                destZ = pawn.tile.z,
                                locShape = -2,
                                destWidth = pawn.getSize(),
                                destLength = pawn.getSize()
                            )
                            target.walkRoute(route, StepType.NORMAL)

                            // Wait until in range or timeout
                            var ticksWaited = 0
                            val maxTicks = 50
                            while (ticksWaited < maxTicks && target.hasMoveDestination()) {
                                wait(1)
                                ticksWaited++
                                if (!pawn.isAlive() || !target.isAlive() || pawn.isDead() || target.isDead()) {
                                    return@queue
                                }
                                if (target.tile.isWithinRadius(pawn.tile, attackRange + pawn.getSize())) {
                                    target.stopMovement()
                                    target.attack(pawn)
                                    return@queue
                                }
                            }

                            // Final check
                            if (target.tile.isWithinRadius(pawn.tile, attackRange + pawn.getSize())) {
                                target.attack(pawn)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getNpcXpMultiplier(npc: Npc): Double {
        val attackLvl = npc.combatDef.attack
        val strengthLvl = npc.combatDef.strength
        val defenceLvl = npc.combatDef.defence
        val hitpoints = npc.getMaxHp()

        val averageLvl = Math.floor((attackLvl + strengthLvl + defenceLvl + hitpoints) / 4.0)
        val averageDefBonus =
            Math.floor(
                (
                    npc.getBonus(
                        BonusSlot.DEFENCE_STAB,
                    ) + npc.getBonus(BonusSlot.DEFENCE_SLASH) + npc.getBonus(BonusSlot.DEFENCE_CRUSH)
                ) / 3.0,
            )
        return 1.0 + Math.floor(averageLvl * (averageDefBonus + npc.getStrengthBonus() + npc.getAttackBonus()) / 5120.0) / 40.0
    }

    fun raycast(
        pawn: Pawn,
        target: Pawn,
        distance: Int,
        projectile: Boolean,
    ): Boolean {
        val world = pawn.world
        val start = pawn.tile
        val end = target.tile

        return start.isWithinRadius(end, distance) && world.lineValidator.rayCast(start, end, projectile = projectile)
    }

    suspend fun moveToAttackRange(
        queue: QueueTask,
        pawn: Pawn,
        target: Pawn,
        distance: Int,
        projectile: Boolean,
    ): Boolean {
        val world = pawn.world
        val start = pawn.tile
        val end = target.tile

        val srcSize = pawn.getSize()
        val dstSize = Math.max(distance, target.getSize())

        val touching =
            if (distance > 1) {
                areOverlapping(start.x, start.z, srcSize, srcSize, end.x, end.z, dstSize, dstSize)
            } else {
                areBordering(start.x, start.z, srcSize, srcSize, end.x, end.z, dstSize, dstSize)
            }
        val withinRange = touching && world.lineValidator.rayCast(start, end, projectile = projectile)
        if (withinRange) {
            return true
        }

        // Pathfind to target if not in range
        val route = world.smartRouteFinder.findRoute(
            level = pawn.tile.height,
            srcX = pawn.tile.x,
            srcZ = pawn.tile.z,
            destX = target.tile.x,
            destZ = target.tile.z,
            locShape = -2,
            destWidth = target.getSize(),
            destLength = target.getSize()
        )
        pawn.walkRoute(route, StepType.NORMAL)

        // Wait until we reach the target or timeout
        var ticksWaited = 0
        val maxTicks = 50 // Maximum ticks to wait for pathfinding
        while (ticksWaited < maxTicks && pawn.hasMoveDestination()) {
            queue.wait(1)
            ticksWaited++
            if (!target.isAlive() || !pawn.isAlive()) {
                return false
            }
            // Check if we're now in range
            val newTouching = if (distance > 1) {
                areOverlapping(pawn.tile.x, pawn.tile.z, srcSize, srcSize, target.tile.x, target.tile.z, dstSize, dstSize)
            } else {
                areBordering(pawn.tile.x, pawn.tile.z, srcSize, srcSize, target.tile.x, target.tile.z, dstSize, dstSize)
            }
            if (newTouching && world.lineValidator.rayCast(pawn.tile, target.tile, projectile = projectile)) {
                return true
            }
        }

        // Check final range
        val finalTouching = if (distance > 1) {
            areOverlapping(pawn.tile.x, pawn.tile.z, srcSize, srcSize, target.tile.x, target.tile.z, dstSize, dstSize)
        } else {
            areBordering(pawn.tile.x, pawn.tile.z, srcSize, srcSize, target.tile.x, target.tile.z, dstSize, dstSize)
        }
        return finalTouching && world.lineValidator.rayCast(pawn.tile, target.tile, projectile = projectile)
    }

    fun getProjectileLifespan(
        source: Pawn,
        target: Tile,
        type: ProjectileType,
    ): Int =
        when (type) {
            ProjectileType.MAGIC -> {
                val fastRoute = source.tile.getChebyshevDistance(target)
                5 + (fastRoute * 10)
            }
            else -> {
                val distance = source.tile.getDistance(target)
                type.calculateLife(distance)
            }
        }

    fun canEngage(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        if (pawn.isDead() || target.isDead()) {
            return false
        }

        val maxDistance =
            when {
                pawn is Player && pawn.hasLargeViewport() -> Player.LARGE_VIEW_DISTANCE
                else -> Player.NORMAL_VIEW_DISTANCE
            }
        if (!pawn.tile.isWithinRadius(target.tile, maxDistance)) {
            return false
        }

        val pvp = pawn.entityType.isPlayer && target.entityType.isPlayer

        if (pawn is Player) {
            if (!pawn.isOnline) {
                return false
            }

            if (pawn.hasWeaponType(WeaponType.BULWARK) && pawn.getAttackStyle() == 3) {
                pawn.message("Your bulwark is in its defensive state and can't be used to attack.")
                return false
            }

            if (pawn.invisible && pvp) {
                pawn.message("You can't attack while invisible.")
                return false
            }
        } else if (pawn is Npc) {
            if (!pawn.isSpawned()) {
                return false
            }
        }

        if (target is Npc) {
            if (!target.isSpawned()) {
                return false
            }
            if (!target.def.isAttackable() || target.combatDef.hitpoints == -1) {
                (pawn as? Player)?.message("You can't attack this npc.")
                return false
            }
            if (pawn is Player && target.combatDef.slayerReq > pawn.getSkills().getBaseLevel(Skills.SLAYER)) {
                pawn.message("You need a higher Slayer level to know how to wound this monster.")
                return false
            }

            // ============================================
            // SINGLE-COMBAT CHECK - SIMPLIFIED LOGIC
            // ============================================
            // Rule: If player hit or was hit in last 5 seconds, they're in combat.
            // If they're in combat with NPC A, they CANNOT attack NPC B until 5 seconds pass.
            // That's it. No complex state management.
            if (pawn is Player && !pawn.tile.isMulti(pawn.world)) {
                val lastActionTime = pawn.attr[LAST_COMBAT_ACTION_TIME_ATTR] ?: 0L
                val currentTime = System.currentTimeMillis()
                val timeSinceAction = currentTime - lastActionTime
                val isInRecentCombat = lastActionTime > 0 && timeSinceAction < COMBAT_STATE_TIMEOUT_MS

                // Who was I ACTUALLY fighting? (set when damage is dealt/received)
                val combatPartner = pawn.attr[COMBAT_PARTNER_ATTR]?.get()

                logger.info {
                    "[COMBAT] canEngage: Player(${pawn.username}) -> ${if (target is Npc) "NPC(${target.id})" else "Player"} | " +
                    "combatPartner=${if (combatPartner is Npc) "NPC(${combatPartner.id})" else combatPartner?.toString()} | " +
                    "lastAction=${timeSinceAction}ms ago | inCombat=$isInRecentCombat"
                }

                // If player is dead, allow (combat will reset)
                if (pawn.isDead() || !pawn.isAlive()) {
                    logger.info { "[COMBAT] canEngage: ALLOWED - player is dead" }
                }
                // If I'm in recent combat AND I have a combat partner AND it's NOT the target I'm clicking
                else if (isInRecentCombat && combatPartner != null && combatPartner != target) {
                    // Is my combat partner dead?
                    if (combatPartner.isDead() || !combatPartner.isAlive()) {
                        logger.info { "[COMBAT] canEngage: ALLOWED - combat partner is dead" }
                        pawn.attr.remove(COMBAT_PARTNER_ATTR)
                        pawn.attr.remove(LAST_COMBAT_ACTION_TIME_ATTR)
                    } else {
                        // BLOCKED - I'm in combat with someone else
                        // Player already pathed here - just show message and don't attack
                        // DON'T stop movement - they already walked, just can't attack
                        logger.info { "[COMBAT] canEngage: BLOCKED - in combat with different target (${timeSinceAction}ms < 5000ms)" }
                        pawn.message("You're already in combat.")
                        return false
                    }
                }
                // If 5+ seconds passed, clear combat partner
                else if (!isInRecentCombat && combatPartner != null) {
                    logger.info { "[COMBAT] canEngage: Clearing stale combat partner (${timeSinceAction}ms >= 5000ms)" }
                    pawn.attr.remove(COMBAT_PARTNER_ATTR)
                }

                // Check if NPC is in ACTIVE combat with another player
                if (target is Npc) {
                    val npcCombatPartner = target.attr[COMBAT_PARTNER_ATTR]?.get()
                    val npcLastAction = target.attr[LAST_COMBAT_ACTION_TIME_ATTR] ?: 0L
                    val npcTimeSinceAction = currentTime - npcLastAction
                    val npcInRecentCombat = npcLastAction > 0 && npcTimeSinceAction < COMBAT_STATE_TIMEOUT_MS

                    if (npcInRecentCombat && npcCombatPartner != null && npcCombatPartner != pawn && npcCombatPartner is Player) {
                        if (npcCombatPartner.isDead() || !npcCombatPartner.isAlive()) {
                            // Other player is dead - NPC is free
                        } else {
                            pawn.message("Someone else is fighting that!")
                            return false
                        }
                    }
                }
            }
        } else if (target is Player) {
            if (!target.isOnline || target.invisible) {
                return false
            }

            if (!target.lock.canBeAttacked()) {
                return false
            }

            // Single-combat check: If NPC is trying to attack a player, check if player is already in combat with another NPC
            if (pawn is Npc && !target.tile.isMulti(pawn.world)) {
                val currentTime = System.currentTimeMillis()
                val playerLastAction = target.attr[LAST_COMBAT_ACTION_TIME_ATTR] ?: 0L
                val playerTimeSinceAction = currentTime - playerLastAction
                val playerInRecentCombat = playerLastAction > 0 && playerTimeSinceAction < COMBAT_STATE_TIMEOUT_MS

                // Who is the player ACTUALLY fighting? (set when damage is dealt/received)
                val playerCombatPartner = target.attr[COMBAT_PARTNER_ATTR]?.get()

                // Is this NPC already the player's combat partner?
                val isAlreadyCombatPartner = playerCombatPartner == pawn

                logger.info {
                    "[COMBAT] canEngage (NPC->Player): NPC(${pawn.id}) -> Player(${target.username}) | " +
                    "playerCombatPartner=${if (playerCombatPartner is Npc) "NPC(${playerCombatPartner.id})" else playerCombatPartner?.toString()} | " +
                    "playerLastAction=${playerTimeSinceAction}ms ago | playerInCombat=$playerInRecentCombat | isAlreadyPartner=$isAlreadyCombatPartner"
                }

                // If player is in recent combat with a DIFFERENT NPC, this NPC cannot attack
                if (playerInRecentCombat && playerCombatPartner != null && !isAlreadyCombatPartner) {
                    // Is the player's combat partner dead?
                    if (playerCombatPartner.isDead() || !playerCombatPartner.isAlive()) {
                        logger.info { "[COMBAT] canEngage (NPC->Player): ALLOWED - player's combat partner is dead" }
                        // Clear the player's stale combat partner
                        target.attr.remove(COMBAT_PARTNER_ATTR)
                        target.attr.remove(LAST_COMBAT_ACTION_TIME_ATTR)
                    } else {
                        // Player is in active combat with another NPC - this NPC cannot attack
                        logger.info { "[COMBAT] canEngage (NPC->Player): BLOCKED - player in combat with different NPC (${playerTimeSinceAction}ms < 5000ms)" }
                        return false
                    }
                }
            }

            if (pvp) {
                pawn as Player

                if (!inPvpArea(pawn)) {
                    pawn.message("You can't attack players here.")
                    return false
                }

                if (!inPvpArea(target)) {
                    pawn.message("You can't attack ${target.username} there.")
                    return false
                }

                val combatLvlRange = getValidCombatLvlRange(pawn)
                if (target.combatLevel !in combatLvlRange) {
                    pawn.message("You can't attack ${target.username} - your level different is too great.")
                    return false
                }
            }
        }
        return true
    }

    private fun inPvpArea(player: Player): Boolean = player.inWilderness()

    private fun getValidCombatLvlRange(player: Player): IntRange {
        val wildLvl = player.tile.getWildernessLevel()
        val minLvl = Math.max(Skills.MIN_COMBAT_LVL, player.combatLevel - wildLvl)
        val maxLvl = Math.min(Skills.MAX_COMBAT_LVL, player.combatLevel + wildLvl)
        return minLvl..maxLvl
    }

    private fun getStrategy(combatClass: CombatClass): CombatStrategy =
        when (combatClass) {
            CombatClass.MELEE -> MeleeCombatStrategy
            CombatClass.RANGED -> RangedCombatStrategy
            CombatClass.MAGIC -> MagicCombatStrategy
        }

    private fun areOverlapping(
        x1: Int,
        z1: Int,
        width1: Int,
        length1: Int,
        x2: Int,
        z2: Int,
        width2: Int,
        length2: Int,
    ): Boolean {
        val a = Box(x1, z1, width1 - 1, length1 - 1)
        val b = Box(x2, z2, width2 - 1, length2 - 1)

        if (a.x1 > b.x2 || b.x1 > a.x2) {
            return false
        }

        if (a.y1 > b.y2 || b.y1 > a.y2) {
            return false
        }

        return true
    }

    /**
     * Checks to see if two AABB are bordering, but not overlapping.
     */
    fun areBordering(
        x1: Int,
        z1: Int,
        width1: Int,
        length1: Int,
        x2: Int,
        z2: Int,
        width2: Int,
        length2: Int,
    ): Boolean {
        val a = Box(x1, z1, width1 - 1, length1 - 1)
        val b = Box(x2, z2, width2 - 1, length2 - 1)

        if (b.x1 in a.x1..a.x2 && b.y1 in a.y1..a.y2 || b.x2 in a.x1..a.x2 && b.y2 in a.y1..a.y2) {
            return false
        }

        if (b.x1 > a.x2 + 1) {
            return false
        }

        if (b.x2 < a.x1 - 1) {
            return false
        }

        if (b.y1 > a.y2 + 1) {
            return false
        }

        if (b.y2 < a.y1 - 1) {
            return false
        }
        return true
    }

    data class Box(val x: Int, val y: Int, val width: Int, val length: Int) {
        val x1: Int get() = x

        val x2: Int get() = x + width

        val y1: Int get() = y

        val y2: Int get() = y + length
    }
}
