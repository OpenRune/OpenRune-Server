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
    const val PRIORITY_PID_VARP = "varp.playertarget"
    const val SELECTED_AUTOCAST_VARBIT = "varbits.autocast_spell"
    const val DEFENSIVE_MAGIC_CAST_VARBIT = "varbits.autocast_defmode"

    fun reset(pawn: Pawn) {
        // Remove this pawn from the target's attacker list
        val target = pawn.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (target != null) {
            target.attr.removeFromSet(COMBAT_ATTACKERS_ATTR, WeakReference(pawn))
        }
        pawn.attr.remove(COMBAT_TARGET_FOCUS_ATTR)
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

        if (pawn.attr.has(CASTING_SPELL) && pawn is Player && pawn.getVarbit(SELECTED_AUTOCAST_VARBIT) == 0) {
            reset(pawn)
            pawn.attr.remove(CASTING_SPELL)
        }

    }

    fun postDamage(
        pawn: Pawn,
        target: Pawn,
    ) {
        if (target.isDead()) {
            return
        }

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
                                if (!pawn.isAlive() || !target.isAlive()) {
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
        } else if (target is Player) {
            if (!target.isOnline || target.invisible) {
                return false
            }

            if (!target.lock.canBeAttacked()) {
                return false
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
