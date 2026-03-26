package org.alter.game.combat.ai

import org.alter.game.combat.CombatZoneUtil
import org.alter.game.model.combat.CombatClass
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.move.walkTo

/**
 * The NPC is pursuing a target it has aggroed on. Pathfinds toward
 * the target each tick and transitions to [CombatAiState] once in range,
 * or [RetreatingState] if the target is lost or leash distance is exceeded.
 */
class AggressiveState(private val target: Player) : AiState {

    override fun onEnter(npc: Npc) {
        npc.facePawn(target)
    }

    override fun tick(npc: Npc): AiState? {
        // Target gone
        if (!target.isOnline || target.isDead()) {
            return RetreatingState()
        }

        // Leash check — too far from spawn
        val distFromSpawn = npc.tile.getDistance(npc.spawnTile)
        if (distFromSpawn > npc.combatDef.leashDistance) {
            return RetreatingState()
        }

        // Different height plane
        if (npc.tile.height != target.tile.height) {
            return RetreatingState()
        }

        // Check if within attack range
        val attackRange = getAttackRange(npc)
        val distToTarget = npc.tile.getDistance(target.tile)

        if (distToTarget <= attackRange) {
            // Re-check single-combat restriction before engaging
            val restriction = CombatZoneUtil.checkCombatRestriction(npc.world, npc, target)
            if (restriction != null) {
                return RetreatingState()  // Target now in combat with someone else
            }
            return CombatAiState(target)
        }

        // Walk toward target
        npc.walkTo(target.tile)
        return null
    }

    override fun onExit(npc: Npc) {}

    companion object {
        fun getAttackRange(npc: Npc): Int = when (npc.combatClass) {
            CombatClass.MELEE -> 1
            CombatClass.RANGED -> 7
            CombatClass.MAGIC -> 10
        }
    }
}
