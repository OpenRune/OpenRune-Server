package org.alter.plugins.content.combat.strategy

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.Skills
import org.alter.api.WeaponType
import org.alter.api.ext.hasWeaponType
import org.alter.api.ext.playSound
import org.alter.game.model.combat.XpMode
import org.alter.game.model.entity.AreaSound
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.plugins.content.combat.Combat
import org.alter.plugins.content.combat.CombatConfigs
import org.alter.plugins.content.combat.dealHit
import org.alter.plugins.content.combat.formula.MeleeCombatFormula
import org.alter.game.model.Direction
import java.lang.IllegalStateException

/**
 * @author Tom <rspsmods@gmail.com>
 */
object MeleeCombatStrategy : CombatStrategy {

    private val logger = KotlinLogging.logger {}

    private fun pawnName(pawn: Pawn): String {
        return when (pawn) {
            is Player -> "Player(${pawn.username})"
            is Npc -> "NPC(${pawn.id})"
            else -> "Pawn"
        }
    }
    override fun getAttackRange(pawn: Pawn): Int {
        if (pawn is Player) {
            val halberd = pawn.hasWeaponType(WeaponType.HALBERD)
            return if (halberd) 2 else 1
        }
        return 1
    }

    override fun canAttack(
        pawn: Pawn,
        target: Pawn,
    ): Boolean {
        // Check distance - melee requires pawns to be within attack range
        val attackRange = getAttackRange(pawn)
        val distance = pawn.tile.getDistance(target.tile)
        logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: canAttack check: distance=$distance, attackRange=$attackRange" }
        if (distance > attackRange) {
            logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: Distance check failed: $distance > $attackRange" }
            return false
        }
        // For range 1 (most melee weapons), check if they're adjacent in cardinal directions only (not diagonal)
        // For range 2 (halberds), distance check above is sufficient
        if (attackRange == 1) {
            val isCardinallyAdjacent = areCardinallyAdjacent(pawn, target)
            logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: isCardinallyAdjacent=$isCardinallyAdjacent" }
            if (!isCardinallyAdjacent) {
                logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: Cardinal adjacency check failed" }
                return false
            }
        }
        // Check if we can reach the target (considering entity sizes and collision)
        // Use reachStrategy instead of rayCast for melee, as it's more appropriate for adjacent combat
        val pawnSize = pawn.getSize()
        val targetSize = target.getSize()
        val canReach = pawn.world.reachStrategy.reached(
            flags = pawn.world.collision,
            level = pawn.tile.height,
            srcX = pawn.tile.x,
            srcZ = pawn.tile.z,
            destX = target.tile.x,
            destZ = target.tile.z,
            destWidth = targetSize,
            destLength = targetSize,
            srcSize = pawnSize,
            locShape = -2
        )
        logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: canReach=$canReach" }
        if (!canReach) {
            logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: reachStrategy check failed" }
        }
        return canReach
    }

    override fun attack(
        pawn: Pawn,
        target: Pawn,
    ) {
        val world = pawn.world
        val animation = CombatConfigs.getAttackAnimation(pawn)
        pawn.animate(animation)
        if (target is Player) {
            when (pawn) {
                is Npc -> {
                    CombatConfigs.getCombatDef(pawn)!!.let {
                        if (it.defaultAttackSoundArea) {
                            world.spawn(
                                AreaSound(pawn.tile, it.defaultAttackSound, it.defaultAttackSoundRadius, it.defaultAttackSoundVolume),
                            )
                        } else {
                            target.playSound(pawn.combatDef.defaultAttackSound, pawn.combatDef.defaultAttackSoundVolume)
                        }
                    }
                }
                is Player -> {
                    // @TODO Later
                }
            }
        }
        val formula = MeleeCombatFormula
        val accuracy = formula.getAccuracy(pawn, target)
        val maxHit = formula.getMaxHit(pawn, target)
        val landHit = accuracy >= world.randomDouble()

        val damage = pawn.dealHit(target = target, maxHit = maxHit, landHit = landHit, delay = 1).hit.hitmarks.sumOf { it.damage }

        if (damage > 0 && pawn.entityType.isPlayer) {
            addCombatXp(pawn as Player, target, damage)
        }
    }

    /**
     * Checks if two pawns are adjacent in cardinal directions only (N, S, E, W), not diagonal.
     * This is required for melee combat (range 1).
     */
    private fun areCardinallyAdjacent(pawn: Pawn, target: Pawn): Boolean {
        val dx = Math.abs(pawn.tile.x - target.tile.x)
        val dz = Math.abs(pawn.tile.z - target.tile.z)
        val pawnSize = pawn.getSize()
        val targetSize = target.getSize()

        // For melee, we need to be adjacent in cardinal directions only
        // This means exactly one of dx or dz should be within the size range, but not both
        // We're cardinally adjacent if:
        // - dx is within size range AND dz is 0 (horizontal adjacency)
        // - dz is within size range AND dx == 0 (vertical adjacency)
        // But NOT if both dx and dz are > 0 (diagonal)

        val horizontallyAdjacent = dx <= pawnSize && dz == 0
        val verticallyAdjacent = dz <= pawnSize && dx == 0

        // Also need to check that we're actually bordering (not overlapping or too far)
        val touching = Combat.areBordering(
            pawn.tile.x, pawn.tile.z, pawnSize, pawnSize,
            target.tile.x, target.tile.z, targetSize, targetSize
        )

        val result = touching && (horizontallyAdjacent || verticallyAdjacent)
        logger.debug { "[MELEE] ${pawnName(pawn)} -> ${pawnName(target)}: areCardinallyAdjacent: dx=$dx, dz=$dz, pawnSize=$pawnSize, targetSize=$targetSize, horizontallyAdjacent=$horizontallyAdjacent, verticallyAdjacent=$verticallyAdjacent, touching=$touching, result=$result" }
        return result
    }

    private fun addCombatXp(
        player: Player,
        target: Pawn,
        damage: Int,
    ) {
        val modDamage = if (target.entityType.isNpc) Math.min(target.getCurrentHp(), damage) else damage
        val mode = CombatConfigs.getXpMode(player)
        val multiplier = if (target is Npc) Combat.getNpcXpMultiplier(target) else 1.0

        when (mode) {
            XpMode.ATTACK -> {
                player.addXp(Skills.ATTACK, modDamage * 4.0 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            XpMode.STRENGTH -> {
                player.addXp(Skills.STRENGTH, modDamage * 4.0 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            XpMode.DEFENCE -> {
                player.addXp(Skills.DEFENCE, modDamage * 4.0 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            XpMode.SHARED -> {
                player.addXp(Skills.ATTACK, modDamage * 1.33 * multiplier)
                player.addXp(Skills.STRENGTH, modDamage * 1.33 * multiplier)
                player.addXp(Skills.DEFENCE, modDamage * 1.33 * multiplier)
                player.addXp(Skills.HITPOINTS, modDamage * 1.33 * multiplier)
            }
            else -> throw IllegalStateException("Unknown $mode in MeleeCombatStrategy.")
        }
    }
}
