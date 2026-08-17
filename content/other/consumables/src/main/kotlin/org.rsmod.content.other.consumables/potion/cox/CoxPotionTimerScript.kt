package org.rsmod.content.other.consumables.potion.cox

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CoxPotionTimerScript
@Inject
constructor(
    private val effects: CoxPotionEffect,
    private val prayerEnhance: CoxPrayerEnhanceEffect,
    private val overload: CoxOverloadEffect,
) : PluginScript() {
    override fun ScriptContext.startup() {
        /*
         * Temporary behavior until Chambers sessions own snapshots and
         * restoration. This prevents raid-only effects from leaking into
         * the normal world if unrestricted potion testing is enabled.
         */
        onPlayerLogin {
            effects.clearSessionEffects(player)
        }

        onPlayerLogout {
            effects.clearSessionEffects(player)
        }

        onPlayerTimer(
            CoxPrayerEnhanceEffect.TIMER,
        ) {
            prayerEnhance.process(this)
        }

        onPlayerTimer(
            CoxOverloadEffect.DAMAGE_TIMER,
        ) {
            overload.processDamage(this)
        }

        onPlayerTimer(
            CoxOverloadEffect.TIMER,
        ) {
            overload.process(this)
        }
    }
}
