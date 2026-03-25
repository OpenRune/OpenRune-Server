package org.alter.combat

import org.alter.game.combat.CombatSystem
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.WorldTickEvent

/**
 * Wires [CombatSystem.processTick] into the world tick loop.
 *
 * Only runs when `combat.useNewSystem: true` is set in game.yml.
 * When false, the legacy CombatPlugin handles all combat and this
 * bootstrap is a no-op, preventing double-processing.
 */
class CombatSystemBootstrap : PluginEvent() {

    override fun init() {
        val combatSystem = CombatSystem(EventManager)
        onEvent<WorldTickEvent> {
            if (world.gameContext.useNewCombatSystem) {
                combatSystem.processTick()
            }
        }
    }
}
