package org.alter.combat

import org.alter.api.ext.message
import org.alter.game.combat.CombatSystem
import org.alter.game.combat.CombatZoneUtil
import org.alter.game.model.attr.INTERACTING_NPC_ATTR
import org.alter.game.model.move.walkTo
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.NpcAttackEvent
import java.lang.ref.WeakReference

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

            // Set the NPC as the interaction target and walk toward it
            player.attr[INTERACTING_NPC_ATTR] = WeakReference(npc)
            player.facePawn(npc)
            player.walkTo(npc.tile)

            // Resolve strategy and style
            val strategy = CombatSystem.instance.resolveStrategy(player)
            val style = CombatSystem.instance.resolveCombatStyle(player)

            // Engage — CombatSystem tracks the combat state and will process
            // attacks once the attack delay is ready. The player walks toward
            // the NPC each tick via the movement queue set above.
            CombatSystem.instance.engage(player, npc, strategy, style)
        }
    }
}
