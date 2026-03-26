package org.alter.game.combat.ai

import org.alter.game.combat.CombatSystem
import org.alter.game.combat.DisengageReason
import org.alter.game.model.entity.Npc

/**
 * The NPC has died. The server's existing [NpcDeathAction] handles the
 * death animation, loot drops, hiding, respawn delay, and HP/stat reset.
 *
 * This state simply disengages from the combat system on entry, then
 * waits for the existing death/respawn pipeline to finish. Once the NPC
 * is alive again (respawned), it transitions back to [IdleState].
 *
 * If the NPC does not respawn (removed from world), this state becomes
 * inert — the NPC and its state machine will be garbage collected.
 */
class DeadState : AiState {

    override fun onEnter(npc: Npc) {
        // Disengage from the combat system if still tracked
        if (CombatSystem.instance.isInCombat(npc)) {
            CombatSystem.instance.disengage(npc, DisengageReason.TARGET_DEAD)
        }
        npc.resetFacePawn()
    }

    override fun tick(npc: Npc): AiState? {
        // The existing NpcDeathAction handles death anim, loot, hiding,
        // respawn delay, moveTo(spawnTile), and setCurrentHp(). Once the
        // NPC is alive again, transition back to idle.
        if (npc.isAlive()) {
            return IdleState()
        }
        return null
    }

    override fun onExit(npc: Npc) {}
}
