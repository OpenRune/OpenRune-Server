package org.alter.combat

import org.alter.api.ext.getVarp
import org.alter.game.combat.CombatSystem
import org.alter.game.model.LockState
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.PostAttackEvent

/**
 * Automatically retaliates against an attacker when an idle player is hit.
 *
 * Retaliation is triggered when ALL of the following hold:
 *   - The hit target is a [Player]
 *   - The hit dealt damage > 0
 *   - Auto-retaliate is enabled (varp.option_nodef == 0)
 *   - The player is not already in combat
 *   - The player's lock state is [LockState.NONE]
 */
class AutoRetaliatePlugin : PluginEvent() {

    override fun init() {
        onEvent<PostAttackEvent> {
            val player = target as? Player ?: return@onEvent
            if (damage <= 0) return@onEvent

            // Auto-retaliate varp: 0 = enabled
            if (player.getVarp("varp.option_nodef") != 0) return@onEvent

            // Player must be idle
            if (CombatSystem.instance.isInCombat(player)) return@onEvent
            if (player.lock != LockState.NONE) return@onEvent

            // Engage the attacker
            val strategy = CombatSystem.instance.resolveStrategy(player)
            val style = CombatSystem.instance.resolveCombatStyle(player)
            CombatSystem.instance.engage(player, attacker, strategy, style)
        }
    }
}
