package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FoundrySuppliesScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(BUCKET_PILE) { takeBucket() }
    }

    private suspend fun ProtectedAccess.takeBucket() {
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(TAKE_SEQ, delay = TAKE_ANIM_DELAY)
        delay(1)
        invAdd(inv, BUCKET_EMPTY, 1)
        mes("You take an empty bucket.")
        soundSynth(TAKE_SYNTH)
    }

    private companion object {
        const val BUCKET_PILE = "loc.my2arm_throne_room_buckets"
        const val BUCKET_EMPTY = "obj.bucket_empty"
        const val TAKE_SEQ = "seq.human_pickupfloor"
        const val TAKE_SYNTH = "synth.pick2"
        const val TAKE_ANIM_DELAY = 10
    }
}
