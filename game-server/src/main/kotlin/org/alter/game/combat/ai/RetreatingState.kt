package org.alter.game.combat.ai

import org.alter.game.combat.CombatSystem
import org.alter.game.combat.DisengageReason
import org.alter.game.model.LockState
import org.alter.game.model.attr.POISON_TICKS_LEFT_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.move.walkTo
import org.alter.game.model.timer.POISON_TIMER

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
        npc.lock = LockState.FULL_WITH_DAMAGE_IMMUNITY
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

    override fun onExit(npc: Npc) {
        npc.lock = LockState.NONE
    }

    private fun resetNpc(npc: Npc) {
        npc.setCurrentHp(npc.combatDef.hitpoints)
        // Reset combat stats to their max levels
        for (i in 0 until npc.stats.nStats) {
            npc.stats.setCurrentLevel(i, npc.stats.getMaxLevel(i))
        }
        // Clear poison/venom
        npc.timers.remove(POISON_TIMER)
        npc.attr.remove(POISON_TICKS_LEFT_ATTR)
    }
}
