package org.rsmod.content.interfaces.prayer.tab.scripts

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.output.ClientScripts
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
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
        onIfOverlayButton("component.toplevel_osrs_stretch:stone5") { player.selectStoneOp(it.op) }
    }

    private fun Player.onTabOpen() {
        ifSetEvents("component.prayerbook:filtermenu", 0..4, IfEvent.Op1)
    }

    private fun Player.selectStoneOp(op: IfButtonOp) {
        if (op != IfButtonOp.Op2) {
            return
        }
        hideFilterButton = !hideFilterButton
        ClientScripts.toplevelSidebuttonSwitch(this, 5)
        soundSynth("synth.interface_select")
    }

    private fun Player.toggleFilter(comsub: Int) {
        when (comsub) {
            0 -> blockLowTier = !blockLowTier
            1 -> {
                if (!blockLowTier) {
                    mes(COMBINED_TIER_LOCKED)
                    return
                }
                allowCombinedTier = !allowCombinedTier
            }
            2 -> blockHealing = !blockHealing
            3 -> blockLackLevel = !blockLackLevel
            4 -> blockLocked = !blockLocked
            else -> throw IllegalStateException("Unhandled comsub: $comsub")
        }
    }
}

private const val COMBINED_TIER_LOCKED =
    "This option can only be toggled when the \"Show lower tiers of tiered prayers\" is off."

private var Player.hideFilterButton by boolVarBit("varbit.prayer_hidefilterbutton")

private var Player.blockLowTier by boolVarBit("varbit.prayer_filter_blocklowtier")
private var Player.allowCombinedTier by boolVarBit("varbit.prayer_filter_allowcombinedtier")
private var Player.blockHealing by boolVarBit("varbit.prayer_filter_blockhealing")
private var Player.blockLackLevel by boolVarBit("varbit.prayer_filter_blocklacklevel")
private var Player.blockLocked by boolVarBit("varbit.prayer_filter_blocklocked")
