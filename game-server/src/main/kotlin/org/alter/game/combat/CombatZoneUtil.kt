package org.alter.game.combat

import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR

object CombatZoneUtil {

    fun isMultiCombatZone(world: World, tile: Tile): Boolean {
        val regionId = tile.regionId
        if (world.getMultiCombatRegions().contains(regionId)) return true
        val chunkHash = tile.chunkCoords.hashCode()
        return world.getMultiCombatChunks().contains(chunkHash)
    }

    /**
     * Check if [attacker] can attack [target] under single/multi-combat rules.
     * Returns null if allowed, or a message string if blocked.
     */
    fun checkCombatRestriction(world: World, attacker: Pawn, target: Pawn): String? {
        if (isMultiCombatZone(world, target.tile)) return null

        // Check if target NPC ignores single-combat rules
        if (target is Npc && target.combatDef.ignoreSingleCombat) return null
        if (attacker is Npc && attacker.combatDef.ignoreSingleCombat) return null

        // In single-combat: check if attacker is already fighting someone else
        val attackerTarget = attacker.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (attackerTarget != null && attackerTarget != target) {
            return if (attacker is Player) "I'm already under attack." else null
        }

        // In single-combat: check if target is already fighting someone else
        val targetTarget = target.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (targetTarget != null && targetTarget != attacker) {
            return if (attacker is Player) "Someone else is already fighting that." else null
        }

        return null
    }
}
