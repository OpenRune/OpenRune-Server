package org.rsmod.content.generic.locs.drawers

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DrawersScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (d in DRAWERS) {
            onOpLoc1(d.closed) { open(it.loc, d) }
            if (d.searchOp == 1) {
                onOpLoc1(d.opened) { search() }
                onOpLoc2(d.opened) { shut(it.loc, d) }
            } else {
                onOpLoc2(d.opened) { search() }
                onOpLoc3(d.opened) { shut(it.loc, d) }
            }
        }
    }

    private suspend fun ProtectedAccess.open(loc: BoundLocInfo, d: Drawers) {
        arriveDelay()
        anim(OPEN_SEQ)
        soundSynth(OPEN_SYNTH)
        delay(1)
        locRepo.change(loc, d.opened, DURATION)
    }

    private suspend fun ProtectedAccess.search() {
        arriveDelay()
        mes("You search the drawers but find nothing.")
    }

    private suspend fun ProtectedAccess.shut(loc: BoundLocInfo, d: Drawers) {
        arriveDelay()
        anim(OPEN_SEQ)
        delay(1)
        locRepo.change(loc, d.closed, DURATION)
    }

    private data class Drawers(val closed: String, val opened: String, val searchOp: Int = 2)

    private companion object {
        const val OPEN_SEQ = "seq.human_openchest"
        const val OPEN_SYNTH = "synth.drawer_open"
        const val DURATION = 500

        val DRAWERS =
            listOf(
                Drawers("loc.drawers1", "loc.drawers1open"),
                Drawers("loc.drawers2", "loc.drawers2open"),
                Drawers("loc.drawers3", "loc.drawers3open"),
                Drawers(
                    "loc.kr_seers_village_drawers1",
                    "loc.kr_seers_village_drawers1open",
                    searchOp = 1,
                ),
            )
    }
}
