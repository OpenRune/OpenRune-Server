package org.alter.plugins.content.combat

import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.model.Tile
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.COMBAT_ATTACKERS_ATTR
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.attr.LAST_HIT_ATTR
import org.alter.game.model.attr.LAST_HIT_BY_ATTR
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
    val CASTING_SPELL = AttributeKey<CombatSpell>()
    val DAMAGE_DEAL_MULTIPLIER = AttributeKey<Double>()
    val DAMAGE_TAKE_MULTIPLIER = AttributeKey<Double>()
    val BOLT_ENCHANTMENT_EFFECT = AttributeKey<Boolean>()
    val ALWAYS_MAX_HIT = AttributeKey<Boolean>()
    val DRAGON_BATTLEAXE_BONUS = AttributeKey<Double>()
    // Tracks the world cycle when the player last engaged in combat (attacked or was attacked)
    val LAST_COMBAT_CYCLE_ATTR = AttributeKey<Int>(temp = true)
    // Tracks whether the pawn was in range when they stopped moving (to prevent unnecessary recursion)
    val WAS_IN_RANGE_WHEN_STOPPED_ATTR = AttributeKey<Boolean>(temp = true)
    // Tracks the target's position when path was last calculated (to detect if target moved significantly)
    val LAST_PATH_TARGET_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    // Tracks when path was last calculated (to throttle recalculation)
    val LAST_PATH_CALCULATION_CYCLE_ATTR = AttributeKey<Int>(temp = true)
    // Tracks the target's position when we successfully completed walking to it (for dumb pathfinding condition)
    val LAST_SUCCESSFUL_WALK_TARGET_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    // Flag to control whether NPCs can get stuck behind obstacles (default: true)
    // When true: NPCs will stop pathfinding if they get stuck behind walls/obstacles (allows players to trap NPCs)
    // When false: NPCs will always try to path around obstacles (ultra-smart pathfinding)
    val CAN_BE_STUCK_ATTR = AttributeKey<Boolean>(temp = true)
    // Tracks the target's position when the pawn last got stuck (to avoid recalculating when stuck and target hasn't moved)
    val STUCK_TARGET_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    // Tracks the pawn's position when it last got stuck (to detect if pawn moved from blocked position)
    val STUCK_PAWN_POSITION_ATTR = AttributeKey<Tile>(temp = true)
    // Tracks consecutive fallback route attempts (when smart route is empty)
    val CONSECUTIVE_FALLBACK_ATTEMPTS_ATTR = AttributeKey<Int>(temp = true)
    const val PRIORITY_PID_VARP = "varp.playertarget"
    const val SELECTED_AUTOCAST_VARBIT = "varbits.autocast_spell"
    const val DEFENSIVE_MAGIC_CAST_VARBIT = "varbits.autocast_defmode"
    // 10 seconds = 100 ticks (600ms per tick, 10 seconds = 10000ms / 100ms per tick = 100 ticks)
    const val COMBAT_TIMEOUT_TICKS = 100

    fun reset(pawn: Pawn) {
        // Remove this pawn from the target's attacker list
        val target = pawn.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (target != null) {
            target.attr.removeFromSet(COMBAT_ATTACKERS_ATTR, WeakReference(pawn))
        }
        pawn.attr.remove(COMBAT_TARGET_FOCUS_ATTR)
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

            // Single-combat check: If player is trying to attack an NPC, check if player is already in combat with another NPC
            if (pawn is Player && !pawn.tile.isMulti(pawn.world)) {
                val currentTarget = pawn.getCombatTarget()
                if (currentTarget is Npc && currentTarget != target) {
                    // Check if the previous target is dead - if so, allow immediate attack (no cooldown after killing)
                    if (currentTarget.isDead() || !currentTarget.isAlive()) {
                        // Player just killed an NPC - allow immediate attack on other NPCs
                        Combat.reset(pawn)
                    } else {
                        // Check if 10 seconds have passed since player's last combat action
                        val lastCombatCycle = pawn.attr[LAST_COMBAT_CYCLE_ATTR] ?: 0
                        val currentCycle = pawn.world.currentCycle
                        val cyclesSinceCombat = currentCycle - lastCombatCycle

                        if (cyclesSinceCombat < COMBAT_TIMEOUT_TICKS) {
                            pawn.message("You're already in combat!")
                            return false
                        }
                        // 10 seconds have passed - allow targeting new NPC and reset old target
                        Combat.reset(pawn)
                    }
                }

                // Check if NPC is already in combat with another player (and < 10 seconds have passed since NPC's last combat)
                val npcAttackers = target.attr[COMBAT_ATTACKERS_ATTR]
                if (npcAttackers != null && npcAttackers.isNotEmpty()) {
                    val otherPlayerAttacker = npcAttackers.firstOrNull { attackerRef ->
                        val attacker = attackerRef.get()
                        attacker != null && attacker is Player && attacker != pawn && attacker.isAttacking()
                    }?.get() as? Player

                    if (otherPlayerAttacker != null) {
                        // Check if the other player is dead - if so, allow immediate attack (no cooldown after player death)
                        if (otherPlayerAttacker.isDead() || !otherPlayerAttacker.isAlive()) {
                            // Player that was fighting this NPC is dead - allow immediate attack
                            // No need to check timeout, the NPC is now free
                        } else {
                            // Check if 10 seconds have passed since the NPC's last combat action (when it received hits)
                            // If NPC has never been in combat, LAST_COMBAT_CYCLE_ATTR will be 0, making cyclesSinceCombat huge (>= timeout)
                            val npcLastCombatCycle = target.attr[LAST_COMBAT_CYCLE_ATTR] ?: 0
                            val currentCycle = pawn.world.currentCycle
                            val npcCyclesSinceCombat = currentCycle - npcLastCombatCycle

                            // If NPC has never been in combat (npcLastCombatCycle == 0), npcCyclesSinceCombat will be huge and >= timeout
                            // Only check timeout if NPC has actually been in combat (npcLastCombatCycle > 0)
                            if (npcLastCombatCycle > 0 && npcCyclesSinceCombat < COMBAT_TIMEOUT_TICKS) {
                                pawn.message("Someone else is fighting that!")
                                return false
                            }
                            // 10 seconds have passed since NPC's last combat (or NPC has never been in combat) - allow attacking
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
                // NPCs should ALWAYS be able to attack players they're already in combat with
                val isAlreadyInCombat = pawn.isAttacking() && pawn.getCombatTarget() == target
                // NPCs should ALWAYS be able to attack players who just attacked them
                val playerTarget = target.getCombatTarget()
                val playerJustAttackedThisNpc = playerTarget == pawn

                if (!isAlreadyInCombat && !playerJustAttackedThisNpc) {
                    // Check if this NPC's previous target (if it was a player) is dead
                    // If so, allow immediate aggression (no cooldown after killing player)
                    val npcPreviousTarget = pawn.getCombatTarget()
                    val previousTargetIsDead = npcPreviousTarget is Player && (npcPreviousTarget.isDead() || !npcPreviousTarget.isAlive())
                    val npcCanAggressImmediately = previousTargetIsDead

                    // Check if this NPC has been out of combat for 10+ seconds (no received hits)
                    // If not, it can only aggress if the player is not in combat with another NPC
                    val npcLastCombatCycle = pawn.attr[LAST_COMBAT_CYCLE_ATTR] ?: 0
                    val currentCycle = target.world.currentCycle
                    val npcCyclesSinceCombat = currentCycle - npcLastCombatCycle
                    val npcCanAggressNewTarget = npcCanAggressImmediately || npcCyclesSinceCombat >= COMBAT_TIMEOUT_TICKS

                    // Check if player is already in combat with another NPC
                    val attackers = target.attr[COMBAT_ATTACKERS_ATTR]
                    if (attackers != null) {
                        val hasOtherNpcAttacker = attackers.any { attackerRef ->
                            val attacker = attackerRef.get()
                            attacker != null && attacker is Npc && attacker != pawn && attacker.isAttacking()
                        }
                        if (hasOtherNpcAttacker) {
                            // Player is in combat with another NPC
                            // Only allow this NPC to aggress if it has been out of combat for 10+ seconds OR its previous target (player) is dead
                            if (!npcCanAggressNewTarget) {
                                return false
                            }
                            // NPC has been out of combat for 10+ seconds OR killed its previous target - allow aggression even though player is targeted by another NPC
                        }
                    }

                    // Check if player is already attacking another NPC (but allow if it's this NPC)
                    if (playerTarget is Npc && playerTarget != pawn) {
                        // Player is attacking another NPC
                        // Only allow this NPC to aggress if it has been out of combat for 10+ seconds OR its previous target (player) is dead
                        if (!npcCanAggressNewTarget) {
                            return false
                        }
                        // NPC has been out of combat for 10+ seconds OR killed its previous target - allow aggression even though player is attacking another NPC
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
