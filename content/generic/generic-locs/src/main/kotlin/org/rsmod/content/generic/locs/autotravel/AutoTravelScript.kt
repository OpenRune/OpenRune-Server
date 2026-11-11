package org.rsmod.content.generic.locs.autotravel

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpContentLoc1
import org.rsmod.api.script.onOpContentLoc2
import org.rsmod.api.script.onOpContentLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Serves the staircases and gangplanks tagged by the travel coverage report. */
class AutoTravelScript @Inject constructor(private val traveller: AutoTraveller) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpContentLoc1(TravelKind.Up.content) { go(it.loc, TravelKind.Up, up = true) }
        onOpContentLoc1(TravelKind.Down.content) { go(it.loc, TravelKind.Down, up = false) }
        onOpContentLoc1(TravelKind.Option.content) { chooseDirection(it.loc) }
        onOpContentLoc2(TravelKind.Option.content) { go(it.loc, TravelKind.Option, up = true) }
        onOpContentLoc3(TravelKind.Option.content) { go(it.loc, TravelKind.Option, up = false) }
        onOpContentLoc1(TravelKind.Cross.content) { go(it.loc, TravelKind.Cross, up = true) }
    }

    private suspend fun ProtectedAccess.chooseDirection(loc: BoundLocInfo) {
        val up = choice2("Climb up.", true, "Climb down.", false, title = "Climb up or down?")
        go(loc, TravelKind.Option, up)
    }

    private suspend fun ProtectedAccess.go(loc: BoundLocInfo, kind: TravelKind, up: Boolean) {
        with(traveller) { travel(loc, kind, up) }
    }
}
