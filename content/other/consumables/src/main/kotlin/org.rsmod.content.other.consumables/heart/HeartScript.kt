package org.rsmod.content.other.consumables.heart

import jakarta.inject.Inject
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HeartScript
@Inject
constructor(
    private val effects: HeartEffectService,
) : PluginScript() {
    override fun ScriptContext.startup() {
        HeartType.entries.forEach { type ->
            onOpHeld1(type.item) {
                effects.activate(
                    access = this,
                    type = type,
                )
            }
        }

        onPlayerLogin {
            effects.onLogin(player)
        }

        onPlayerLogout {
            effects.onLogout(player)
        }

        onPlayerTimer(
            HeartEffectService.TIMER,
        ) {
            effects.process(this)
        }
    }
}
