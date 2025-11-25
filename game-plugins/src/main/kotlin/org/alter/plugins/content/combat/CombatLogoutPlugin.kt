package org.alter.plugins.content.combat

import org.alter.api.*
import org.alter.api.ext.player
import org.alter.game.model.World
import org.alter.game.model.attr.COMBAT_ATTACKERS_ATTR
import org.alter.game.model.attr.COMBAT_TARGET_FOCUS_ATTR
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.Server

/**
 * Handles combat state cleanup when a player logs out.
 * Resets combat state for all NPCs that were engaged with the logging out player.
 */
class CombatLogoutPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onLogout {
            handleCombatLogout(player)
        }
    }

    private fun handleCombatLogout(player: Player) {
        // Get all NPCs that were attacking this player
        val attackers = player.attr[COMBAT_ATTACKERS_ATTR]
        if (attackers != null) {
            attackers.forEach { attackerRef ->
                val attacker = attackerRef.get()
                if (attacker != null && attacker is Npc) {
                    // Reset the NPC's combat state to IDLE
                    Combat.reset(attacker)
                    attacker.resetFacePawn()
                }
            }
        }

        // Get the NPC this player was attacking
        val target = player.attr[COMBAT_TARGET_FOCUS_ATTR]?.get()
        if (target != null && target is Npc) {
            // Reset the NPC's combat state to IDLE
            Combat.reset(target)
            target.resetFacePawn()
        }

        // Reset the player's own combat state
        Combat.reset(player)
    }
}

