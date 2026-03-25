package org.alter.combat

import org.alter.game.combat.CombatSystem
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.WorldTickEvent

/**
 * Wires [CombatSystem.processTick] into the world tick loop.
 *
 * The CombatSystem is a no-op until combats are registered (Layer 2+),
 * so this registration is safe to enable unconditionally during Layer 1.
 */
class CombatSystemBootstrap : PluginEvent() {

    override fun init() {
        val combatSystem = CombatSystem(EventManager)
        onEvent<WorldTickEvent> { combatSystem.processTick() }
    }
}
