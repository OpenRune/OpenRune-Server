package org.rsmod.content.interfaces.prayer.tab.scripts

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PrayerTabFilterScript : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen("interface.prayerbook") { player.onTabOpen() }
        onIfOverlayButton("component.prayerbook:filtermenu") { player.toggleFilter(it.comsub) }
    }

    private fun Player.onTabOpen() {
        ifSetEvents("component.prayerbook:filtermenu", 0..4, IfEvent.Op1)
    }

    private fun Player.toggleFilter(comsub: Int) {
        when (comsub) {
            0 -> showLowerTiers = !showLowerTiers
            1 -> showTiered = !showTiered
            2 -> showRapidHealing = !showRapidHealing
            3 -> showWithoutLevel = !showWithoutLevel
            4 -> showWithoutReq = !showWithoutReq
            else -> throw IllegalStateException("Unhandled comsub: $comsub")
        }
    }
}

private var Player.showLowerTiers by boolVarBit("varbit.prayer_filter_blocklowtier")
private var Player.showTiered by boolVarBit("varbit.prayer_filter_allowcombinedtier")
private var Player.showRapidHealing by boolVarBit("varbit.prayer_filter_blockhealing")
private var Player.showWithoutLevel by boolVarBit("varbit.prayer_filter_blocklacklevel")
private var Player.showWithoutReq by boolVarBit("varbit.prayer_filter_blocklocked")
