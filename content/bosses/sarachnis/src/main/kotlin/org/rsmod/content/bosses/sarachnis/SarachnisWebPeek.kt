package org.rsmod.content.bosses.sarachnis

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.entity.PlayerList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SarachnisWebPeek
@Inject
constructor(
    private val playerList: PlayerList,
    private val areaChecker: AreaChecker,
) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpLoc2(WEB_LOC) { peek() }
        onOpLoc2(SLASHED_WEB_LOC) { peek() }
    }

    private fun ProtectedAccess.peek() {
        val count = playerList.count { areaChecker.inArea(LAIR_AREA, it.coords) }
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
        private const val LAIR_AREA = "area.sarachnis_lair"
    }
}
