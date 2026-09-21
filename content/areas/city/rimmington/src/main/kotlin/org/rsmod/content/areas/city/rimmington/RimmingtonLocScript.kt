package org.rsmod.content.areas.city.rimmington

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RimmingtonLocScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(WARDROBE_SHUT) { openWardrobe(it.loc) }
        onOpLoc3(WARDROBE_OPEN) { shutWardrobe(it.loc) }
        onOpLoc2(WARDROBE_OPEN) { mes("The wardrobe is empty.") }
    }

    private suspend fun ProtectedAccess.openWardrobe(wardrobe: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_openbigcupboard")
        soundSynth("synth.wardrobe_open")
        delay(1)
        locRepo.change(wardrobe, WARDROBE_OPEN, WARDROBE_DURATION)
    }

    private suspend fun ProtectedAccess.shutWardrobe(wardrobe: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_closechest")
        soundSynth("synth.wardrobe_close")
        delay(1)
        locRepo.change(wardrobe, WARDROBE_SHUT, WARDROBE_DURATION)
    }

    private companion object {
        const val WARDROBE_SHUT = "loc.spookywardrobe"
        const val WARDROBE_OPEN = "loc.spookywardrobe_open"
        const val WARDROBE_DURATION = 500
    }
}
