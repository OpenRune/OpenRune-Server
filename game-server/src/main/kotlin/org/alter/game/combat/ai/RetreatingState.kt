package org.alter.game.combat.ai

import org.alter.game.combat.CombatSystem
import org.alter.game.combat.DisengageReason
import org.alter.game.model.entity.Npc
import org.alter.game.model.move.walkTo

/**
 * The NPC is returning to its spawn tile after losing its target or
 * exceeding leash distance. Once it arrives, it resets HP and stats
 * and transitions back to [IdleState].
 */
class RetreatingState : AiState {

    override fun onEnter(npc: Npc) {
        // Disengage from combat if still tracked
        if (CombatSystem.instance.isInCombat(npc)) {
            CombatSystem.instance.disengage(npc, DisengageReason.OUT_OF_RANGE)
        }
        npc.resetFacePawn()
        npc.walkTo(npc.spawnTile)
    }

    override fun tick(npc: Npc): AiState? {
        // Arrived at spawn tile
        if (npc.tile.sameAs(npc.spawnTile)) {
            resetNpc(npc)
            return IdleState()
        }

        // If movement stopped but we're not at spawn yet, re-issue walk
        if (!npc.movementQueue.hasDestination()) {
            npc.walkTo(npc.spawnTile)
        }

        return null
    }

    override fun onExit(npc: Npc) {}

    private fun resetNpc(npc: Npc) {
        npc.setCurrentHp(npc.combatDef.hitpoints)
        // Reset combat stats to their max levels
        for (i in 0 until npc.stats.nStats) {
            npc.stats.setCurrentLevel(i, npc.stats.getMaxLevel(i))
        }
    }
}
