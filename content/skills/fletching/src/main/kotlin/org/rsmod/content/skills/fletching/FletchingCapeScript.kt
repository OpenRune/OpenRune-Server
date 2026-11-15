package org.rsmod.content.skills.fletching

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpWorn2
import org.rsmod.api.script.onOpWorn3
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.fletchingCapeUses by intVarBit("varbit.fletching_cape_uses")
private var Player.fletchingCapeDay by intVarBit("varbit.fletching_cape_day")
private var Player.runeday by intVarBit("varbit.current_runeday")

class FletchingCapeScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (cape in CAPES) {
            onOpWorn2(cape) { boost() }
            onOpWorn3(cape) { search() }
            onOpHeld3(cape) { search() }
        }
    }

    private fun ProtectedAccess.boost() {
        statBoost("stat.fletching", constant = 1, percent = 0)
    }

    private fun ProtectedAccess.search() {
        if (player.fletchingCapeDay != player.runeday) {
            player.fletchingCapeDay = player.runeday
            player.fletchingCapeUses = 0
        }
        if (player.fletchingCapeUses >= DAILY_USES) {
            mes("You have already searched the cape the maximum number of times today.")
            return
        }
        if (inv.freeSpace() < REWARDS.size) {
            mes("You need at least ${REWARDS.size} free inventory spaces to search the cape.")
            return
        }
        REWARDS.forEach { invAdd(inv, it) }
        player.fletchingCapeUses++
        mes("You search the cape and find a mithril grapple and a bronze crossbow.")
    }

    private companion object {
        private val CAPES = listOf("obj.skillcape_fletching", "obj.skillcape_fletching_trimmed")
        private val REWARDS =
            listOf("obj.xbows_grapple_tip_bolt_mithril_rope", "obj.xbows_crossbow_bronze")
        private const val DAILY_USES = 3
    }
}
