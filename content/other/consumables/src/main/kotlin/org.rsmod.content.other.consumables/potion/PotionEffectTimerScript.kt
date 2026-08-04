package org.rsmod.content.other.consumables.potion

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PotionEffectTimerScript
@Inject
constructor(
    private val effects: PotionEffectService,
    private val specialEffects: PotionSpecialEffectService,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            effects.onLogin(player)
            specialEffects.onLogin(player)
        }

        onPlayerLogout {
            effects.onLogout(player)
            specialEffects.onLogout(player)
        }

        onPlayerTimer(
            PotionEffectService.STAMINA_TIMER,
        ) {
            effects.processStamina(this)
        }

        onPlayerTimer(
            PotionEffectService.ANTIFIRE_TIMER,
        ) {
            effects.processDragonfireProtection(
                access = this,
                fullProtection = false,
            )
        }

        onPlayerTimer(
            PotionEffectService.SUPER_ANTIFIRE_TIMER,
        ) {
            effects.processDragonfireProtection(
                access = this,
                fullProtection = true,
            )
        }

        onPlayerTimer(
            PotionEffectService.DIVINE_TIMER,
        ) {
            effects.processDivineEffects(this)
        }

        onPlayerTimer(
            PotionSpecialEffectService.MENAPHITE_REMEDY_TIMER,
        ) {
            specialEffects.processMenaphiteRemedy(this)
        }

        onPlayerTimer(
            PotionEffectService.PRAYER_REGENERATION_TIMER,
        ) {
            effects.processPrayerRegeneration(this)
        }
    }
}
