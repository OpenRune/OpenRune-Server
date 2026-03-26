package org.alter.game.combat.ai

import org.alter.game.combat.CombatSystem
import org.alter.game.combat.CombatZoneUtil
import org.alter.game.combat.DisengageReason
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * The NPC is actively engaged in combat. On enter it registers with
 * [CombatSystem] so the attack pipeline runs each tick. This state
 * monitors for death, target loss, and leash distance.
 *
 * Named CombatAiState to avoid collision with [org.alter.game.combat.CombatState].
 */
class CombatAiState(private val target: Pawn) : AiState {

    override fun onEnter(npc: Npc) {
        val combatSystem = CombatSystem.instance
        val strategy = combatSystem.resolveStrategy(npc)
        val combatStyle = combatSystem.resolveCombatStyle(npc)
        combatSystem.engage(npc, target, strategy, combatStyle)
        npc.facePawn(target)
    }

    override fun tick(npc: Npc): AiState? {
        // NPC died
        if (npc.isDead()) {
            return DeadState()
        }

        // Target dead or offline
        if (target.isDead() || (target is Player && !target.isOnline)) {
            // Try to retarget in multi-combat zones
            val newTarget = tryRetarget(npc)
            if (newTarget != null) {
                CombatSystem.instance.disengage(npc, DisengageReason.TARGET_DEAD)
                return CombatAiState(newTarget)
            }
            return RetreatingState()
        }

        // Leash check
        val distFromSpawn = npc.tile.getDistance(npc.spawnTile)
        if (distFromSpawn > npc.combatDef.leashDistance) {
            return RetreatingState()
        }

        // CombatSystem.processTick() handles the actual attack pipeline
        return null
    }

    override fun onExit(npc: Npc) {
        CombatSystem.instance.disengage(npc, DisengageReason.MANUAL)
        npc.resetFacePawn()
    }

    /**
     * In multi-combat zones, look for the closest valid player target
     * within the NPC's aggro radius. Returns null in single-combat zones
     * or if no valid target is found.
     */
    private fun tryRetarget(npc: Npc): Player? {
        val world = npc.world
        if (!CombatZoneUtil.isMultiCombatZone(world, npc.tile)) return null

        val combatDef = npc.combatDef
        if (combatDef.aggressiveRadius <= 0) return null

        var closestTarget: Player? = null
        var closestDistance = Int.MAX_VALUE

        world.players.forEach { player ->
            if (!player.isOnline || player.isDead()) return@forEach
            if (!player.tile.isWithinRadius(npc.tile, combatDef.aggressiveRadius)) return@forEach
            if (player.tile.height != npc.tile.height) return@forEach

            val restriction = CombatZoneUtil.checkCombatRestriction(world, npc, player)
            if (restriction != null) return@forEach

            val distance = npc.tile.getDistance(player.tile)
            if (distance < closestDistance) {
                closestDistance = distance
                closestTarget = player
            }
        }
        return closestTarget
    }
}
