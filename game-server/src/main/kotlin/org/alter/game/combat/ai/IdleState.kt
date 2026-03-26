package org.alter.game.combat.ai

import org.alter.game.combat.CombatZoneUtil
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player

/**
 * The default AI state for NPCs. Scans for aggro targets at a configurable
 * interval and transitions to [AggressiveState] when one is found.
 */
class IdleState : AiState {
    private var scanCounter = 0

    override fun onEnter(npc: Npc) {
        scanCounter = 0
    }

    override fun tick(npc: Npc): AiState? {
        val combatDef = npc.combatDef
        if (combatDef.aggressiveRadius <= 0) return null

        scanCounter++
        val scanInterval = maxOf(combatDef.aggroTargetDelay, 1)
        if (scanCounter % scanInterval != 0) return null

        val target = findAggroTarget(npc) ?: return null
        return AggressiveState(target)
    }

    override fun onExit(npc: Npc) {}

    private fun findAggroTarget(npc: Npc): Player? {
        val combatDef = npc.combatDef
        val npcCombatLevel = npc.def.combatLevel
        val world = npc.world
        val currentTick = world.currentCycle.toLong()

        var closestTarget: Player? = null
        var closestDistance = Int.MAX_VALUE

        world.players.forEach { player ->
            if (!player.tile.isWithinRadius(npc.tile, combatDef.aggressiveRadius)) return@forEach
            if (player.tile.height != npc.tile.height) return@forEach
            if (player.combatLevel > npcCombatLevel * 2) return@forEach

            // Aggressive timer check — if the player has been in this region
            // longer than the configured timer, stop being aggressive to them.
            if (combatDef.aggressiveTimer > 0) {
                val regionId = player.tile.regionId
                val entryTick = player.regionEntryTicks[regionId]
                if (entryTick != null && (currentTick - entryTick) > combatDef.aggressiveTimer) {
                    return@forEach
                }
            }

            // Single-combat zone restriction
            val restriction = CombatZoneUtil.checkCombatRestriction(world, npc, player)
            if (restriction != null) return@forEach

            // Custom aggro check (e.g. wilderness level, quest state)
            val aggroCheck = npc.aggroCheck
            if (aggroCheck != null && !aggroCheck(npc, player)) return@forEach

            val distance = npc.tile.getDistance(player.tile)
            if (distance < closestDistance) {
                closestDistance = distance
                closestTarget = player
            }
        }
        return closestTarget
    }
}
