package org.rsmod.content.other.consumables.potion.nmz

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NightmareZonePotionTimerScript
@Inject
constructor(
    private val effects: NightmareZonePotionEffect,
    private val overload: NightmareZoneOverloadEffect,
) : PluginScript() {
    override fun ScriptContext.startup() {
        /*
         * Temporary behavior until a Nightmare Zone dream session owns
         * cleanup and logout restoration.
         */
        onPlayerLogin {
            effects.clearSessionEffects(player)
        }

        onPlayerLogout {
            effects.clearSessionEffects(player)
        }

        onPlayerTimer(
            NightmareZoneOverloadEffect.DAMAGE_TIMER,
        ) {
            overload.processDamage(this)
        }

        onPlayerTimer(
            NightmareZoneOverloadEffect.TIMER,
        ) {
            overload.process(this)
        }
    }
}
