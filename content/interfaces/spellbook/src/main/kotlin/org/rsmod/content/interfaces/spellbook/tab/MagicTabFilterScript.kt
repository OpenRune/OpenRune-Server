package org.rsmod.content.interfaces.spellbook.tab

import dev.openrune.definition.type.widget.IfEvent
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MagicTabFilterScript : PluginScript() {

    override fun ScriptContext.startup() {
        onIfOpen("interface.magic_spellbook") { player.onTabOpen() }

        onIfOverlayButton("component.magic_spellbook:filtermenu") {
            player.toggleFilter(it.comsub)
        }
    }

    private fun Player.onTabOpen() {
        ifSetEvents("component.magic_spellbook:filtermenu", 0..6, IfEvent.Op1)
    }

    private fun Player.toggleFilter(comsub: Int) {
        when (comsub) {
            0 -> showCombatSpells = !showCombatSpells
            1 -> showTeleportSpells = !showTeleportSpells
            2 -> showUtilitySpells = !showUtilitySpells
            3 -> showLackLevelSpells = !showLackLevelSpells
            4 -> showLackRuneSpells = !showLackRuneSpells
            5 -> showWithoutReq = !showWithoutReq
            6 -> showResized = !showResized
            else -> throw IllegalStateException("Unhandled comsub: $comsub")
        }
    }
}

private var Player.showCombatSpells by boolVarBit("varbit.magic_filter_blockcombat")
private var Player.showTeleportSpells by boolVarBit("varbit.magic_filter_blockteleport")
private var Player.showUtilitySpells by boolVarBit("varbit.magic_filter_blockutility")
private var Player.showLackRuneSpells by boolVarBit("varbit.magic_filter_blocklackrunes")
private var Player.showLackLevelSpells by boolVarBit("varbit.magic_filter_blocklacklevel")
private var Player.showWithoutReq by boolVarBit("varbit.magic_filter_blocklocked")
private var Player.showResized by boolVarBit("varbit.magic_spellbook_blockresizing")
