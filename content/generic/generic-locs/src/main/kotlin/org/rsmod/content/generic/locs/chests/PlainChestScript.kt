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
        for ((closed, opened) in CHESTS) {
            onOpLoc1(closed) { open(it.loc, opened) }
            onOpLoc2(opened) { mes(ChestConstants.DEFAULT) }
            onOpLoc3(opened) { shut(it.loc, closed) }
        }
    }

    private suspend fun ProtectedAccess.open(chest: BoundLocInfo, opened: String) {
        arriveDelay()
        anim("seq.human_openchest")
        soundSynth(OPEN_SOUND)
        locRepo.change(chest, opened, ChestConstants.DURATION)
    }

    private suspend fun ProtectedAccess.shut(chest: BoundLocInfo, closed: String) {
        arriveDelay()
        anim("seq.human_openchest")
        locRepo.change(chest, closed, ChestConstants.DURATION)
    }

    private companion object {
        val CHESTS =
            listOf(
                "loc.chestclosed" to "loc.chestopen",
                "loc.kr_camelot_chestclosed" to "loc.kr_camelot_chestopen",
            )
        const val OPEN_SOUND = 52
    }
}
