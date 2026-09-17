package org.rsmod.content.generic.locs.chests

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PlainChestScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CLOSED) { open(it.loc) }
        onOpLoc2(OPENED) { mes(ChestConstants.DEFAULT) }
        onOpLoc3(OPENED) { shut(it.loc) }
    }

    private suspend fun ProtectedAccess.open(chest: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_openchest")
        soundSynth(OPEN_SOUND)
        locRepo.change(chest, OPENED, ChestConstants.DURATION)
    }

    private suspend fun ProtectedAccess.shut(chest: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_openchest")
        locRepo.change(chest, CLOSED, ChestConstants.DURATION)
    }

    private companion object {
        const val CLOSED = "loc.chestclosed"
        const val OPENED = "loc.chestopen"
        const val OPEN_SOUND = 52
    }
}
