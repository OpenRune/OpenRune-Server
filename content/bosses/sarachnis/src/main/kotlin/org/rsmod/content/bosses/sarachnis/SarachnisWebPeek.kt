package org.rsmod.content.bosses.sarachnis

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.entity.PlayerList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SarachnisWebPeek @Inject constructor(private val playerList: PlayerList) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc2(WEB_LOC) { peek() }
        onOpLoc2(SLASHED_WEB_LOC) { peek() }
    }

    private fun ProtectedAccess.peek() {
        val count =
            playerList.count {
                it.coords.level == LAIR_SW.level && it.isWithinArea(LAIR_SW, LAIR_NE)
            }
        if (count == 0) {
            mes("You listen and hear no adventurers inside the crypt")
        } else if (count == 1) {
            mes("You listen and hear the sounds of 1 adventurer inside the crypt")
        } else {
            mes("You listen and hear the sounds of $count adventurers inside the crypt")
        }
    }

    private companion object {
        private const val WEB_LOC = "loc.sarachnis_web_peek"
        private const val SLASHED_WEB_LOC = "loc.sarachnis_web_peek_slashed"

        private val LAIR_SW = CoordGrid(1830, 9895)
        private val LAIR_NE = CoordGrid(1850, 9907)
    }
}
