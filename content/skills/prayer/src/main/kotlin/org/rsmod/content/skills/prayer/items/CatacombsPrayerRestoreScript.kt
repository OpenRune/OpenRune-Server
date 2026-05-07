package org.rsmod.content.skills.prayer.items

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.events.prayer.PrayerSkillAction
import org.rsmod.api.player.events.skilling.SkillingActionCompleteEvent
import org.rsmod.api.player.events.skilling.SkillingActionContext
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.prayer.PrayerBuryEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CatacombsPrayerRestoreScript @Inject constructor(private val areas: AreaChecker) : PluginScript() {

    override fun ScriptContext.startup() {
        onEvent<SkillingActionCompleteEvent> {
            val restore = when (val context = context) {
                is SkillingActionContext.Prayer -> when (val action = context.action) {
                    is PrayerSkillAction.BuryComplete -> if (PrayerBuryEvents.Companion.isDemonicAsh(action.itemInternal)) 0 else action.catacombsBonePrayerRestore
                    is PrayerSkillAction.BonecrusherCrushComplete -> action.catacombsBonePrayerRestore
                }
            }
            if (restore <= 0) return@onEvent
            if (!areas.inArea("area.catacombs_of_kourend", player.coords)) return@onEvent
            player.statBoost("stat.prayer", restore, 0)
        }
    }
}
