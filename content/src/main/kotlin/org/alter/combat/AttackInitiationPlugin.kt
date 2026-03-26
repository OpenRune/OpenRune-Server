package org.alter.combat

import org.alter.api.ext.message
import org.alter.game.combat.CombatSystem
import org.alter.game.combat.CombatZoneUtil
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.NpcAttackEvent

class AttackInitiationPlugin : PluginEvent() {
    override fun init() {
        onEvent<NpcAttackEvent> {
            val player = this.player
            val npc = this.npc

            // Validate target is alive and in world
            if (npc.isDead() || npc.index == -1) return@onEvent

            // Single-combat restriction check
            val restriction = CombatZoneUtil.checkCombatRestriction(npc.world, player, npc)
            if (restriction != null) {
                player.message(restriction)
                return@onEvent
            }

            // Resolve strategy and style
            val strategy = CombatSystem.instance.resolveStrategy(player)
            val style = CombatSystem.instance.resolveCombatStyle(player)

            // Engage
            CombatSystem.instance.engage(player, npc, strategy, style)
        }
    }
}
