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
        onOpLoc1(CLOSED) { open(it.loc) }
        onOpLoc2(OPENED) { search() }
        onOpLoc3(OPENED) { shut(it.loc) }
    }

    private suspend fun ProtectedAccess.open(drawers: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_SEQ)
        soundSynth(OPEN_SYNTH)
        delay(1)
        locRepo.change(drawers, OPENED, DURATION)
    }

    private suspend fun ProtectedAccess.search() {
        arriveDelay()
        mes("You search the drawers but find nothing.")
    }

    private suspend fun ProtectedAccess.shut(drawers: BoundLocInfo) {
        arriveDelay()
        anim(OPEN_SEQ)
        delay(1)
        locRepo.change(drawers, CLOSED, DURATION)
    }

    private companion object {
        const val CLOSED = "loc.drawers2"
        const val OPENED = "loc.drawers2open"
        const val OPEN_SEQ = "seq.human_openchest"
        const val OPEN_SYNTH = "synth.drawer_open"
        const val DURATION = 500
    }
}
