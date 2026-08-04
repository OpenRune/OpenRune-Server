package org.rsmod.content.other.consumables.food

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FoodSpecialEffectTimerScript
@Inject
constructor(
    private val effects: FoodSpecialEffectService,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerTimer(
            FoodSpecialEffectService.DELAYED_HEAL_TIMER,
        ) {
            effects.processDelayedHeals(this)
        }
    }
}
