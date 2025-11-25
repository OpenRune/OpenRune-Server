package org.alter.plugins.content.mechanics.aggro

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.plugins.content.combat.getCombatTarget
import org.alter.plugins.content.combat.isAttacking
import org.alter.game.model.attr.COMBAT_ATTACKERS_ATTR

class NpcAggroPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    val AGGRO_CHECK_TIMER = TimerKey()

    init {
        onGlobalNpcSpawn {
            if (npc.combatDef.aggressiveRadius > 0 && npc.combatDef.aggroTargetDelay > 0) {
                npc.aggroCheck = defaultAggressiveness
                npc.timers[AGGRO_CHECK_TIMER] = npc.combatDef.aggroTargetDelay
            }
        }

        onTimer(AGGRO_CHECK_TIMER) {
            if ((!npc.isAttacking() || npc.tile.isMulti(world)) && npc.lock.canAttack() && npc.isActive()) {
                checkRadius(npc)
            }
            npc.timers[AGGRO_CHECK_TIMER] = npc.combatDef.aggroTargetDelay
        }
    }



val defaultAggressiveness: (Npc, Player) -> Boolean = boolean@{ n, p ->
    if (n.combatDef.aggressiveTimer == Int.MAX_VALUE) {
        return@boolean true
    } else if (n.combatDef.aggressiveTimer == Int.MIN_VALUE) {
        return@boolean false
    }

    if (Math.abs(world.currentCycle - p.lastMapBuildTime) > n.combatDef.aggressiveTimer) {
        return@boolean false
    }

    val npcLvl = n.def.combatLevel
    return@boolean p.combatLevel <= npcLvl * 2
}

fun checkRadius(npc: Npc) {
    val radius = npc.combatDef.aggressiveRadius

    mainLoop@
    for (x in -radius..radius) {
        for (z in -radius..radius) {
            val tile = npc.tile.transform(x, z)
            val chunk = world.chunks.get(tile, createIfNeeded = false) ?: continue

            val players = chunk.getEntities<Player>(tile, EntityType.PLAYER, EntityType.CLIENT)
            if (players.isEmpty()) {
                continue
            }

            val targets = players.filter { canAttack(npc, it) }
            if (targets.isEmpty()) {
                continue
            }

            val target = targets.random()
            if (npc.getCombatTarget() != target) {
                npc.attack(target)
            }
            break@mainLoop
        }
    }
}

fun canAttack(
    npc: Npc,
    target: Player,
): Boolean {
    if (!target.isOnline || target.invisible) {
        return false
    }

    // Single-combat check: In single-combat zones, prevent multiple NPCs from attacking the same player
    if (!target.tile.isMulti(world)) {
        // NPCs should ALWAYS be able to aggress players they're already in combat with
        val isAlreadyInCombat = npc.isAttacking() && npc.getCombatTarget() == target
        // NPCs should ALWAYS be able to aggress players who just attacked them
        val playerTarget = target.getCombatTarget()
        val playerJustAttackedThisNpc = playerTarget == npc

        if (!isAlreadyInCombat && !playerJustAttackedThisNpc) {
            // Check if this NPC's previous target (if it was a player) is dead
            // If so, allow immediate aggression (no cooldown after killing player)
            val npcPreviousTarget = npc.getCombatTarget()
            val previousTargetIsDead = npcPreviousTarget is Player && (npcPreviousTarget.isDead() || !npcPreviousTarget.isAlive())
            val npcCanAggressImmediately = previousTargetIsDead

            // Check if this NPC has been out of combat for 10+ seconds (no received hits)
            // If not, it can only aggress if the player is not in combat with another NPC
            val npcLastCombatCycle = npc.attr[LAST_COMBAT_CYCLE_ATTR] ?: 0
            val currentCycle = world.currentCycle
            val npcCyclesSinceCombat = currentCycle - npcLastCombatCycle
            val npcCanAggressNewTarget = npcCanAggressImmediately || npcCyclesSinceCombat >= org.alter.plugins.content.combat.Combat.COMBAT_TIMEOUT_TICKS

            // Check if player is already in combat with another NPC
            val attackers = target.attr[COMBAT_ATTACKERS_ATTR]
            if (attackers != null) {
                val hasOtherNpcAttacker = attackers.any { attackerRef ->
                    val attacker = attackerRef.get()
                    attacker != null && attacker is Npc && attacker != npc && attacker.isAttacking()
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
            if (playerTarget is Npc && playerTarget != npc) {
                // Player is attacking another NPC
                // Only allow this NPC to aggress if it has been out of combat for 10+ seconds OR its previous target (player) is dead
                if (!npcCanAggressNewTarget) {
                    return false
                }
                // NPC has been out of combat for 10+ seconds OR killed its previous target - allow aggression even though player is attacking another NPC
            }
        }
    }

    return npc.aggroCheck == null || npc.aggroCheck?.invoke(npc, target) == true
}

}
