package org.rsmod.content.other.consumables.potion.toa

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaPotionTimerScript
@Inject
constructor(
    private val effects: ToaPotionEffect,
    private val smellingSalts: ToaSmellingSaltsEffect,
    private val liquidAdrenaline: ToaLiquidAdrenalineEffect,
    private val overTimeEffects: ToaOverTimeEffect,
) : PluginScript() {
    override fun ScriptContext.startup() {
        /*
         * Temporary behavior until a Tombs session owns pause/resume.
         * This prevents raid-only effects from leaking into the normal world.
         */
        onPlayerLogin {
            effects.clearSessionEffects(player)
        }

        onPlayerLogout {
            effects.clearSessionEffects(player)
        }

        onPlayerTimer(
            ToaSmellingSaltsEffect.TIMER,
        ) {
            smellingSalts.process(this)
        }

        onPlayerTimer(
            ToaLiquidAdrenalineEffect.TIMER,
        ) {
            liquidAdrenaline.process(this)
        }

        onPlayerTimer(
            ToaOverTimeEffect.SILK_DRESSING_TIMER,
        ) {
            overTimeEffects.processSilkDressing(this)
        }

        onPlayerTimer(
            ToaOverTimeEffect.BLESSED_SCARAB_TIMER,
        ) {
            overTimeEffects.processBlessedCrystalScarab(this)
        }
    }
}
