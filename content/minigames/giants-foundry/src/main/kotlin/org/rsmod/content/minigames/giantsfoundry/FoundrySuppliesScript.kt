package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Buckets for cooling the poured metal: the pile of buckets (also in Weiss).
 * https://oldschool.runescape.wiki/w/Pile_of_Buckets
 */
class FoundrySuppliesScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(BUCKET_PILE) { takeBucket() }
    }

    private fun ProtectedAccess.takeBucket() {
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(TAKE_SEQ)
        invAdd(inv, BUCKET_EMPTY, 1)
        // Slayermusiq1's Sleeping Giants video (youtube.com/watch?v=io6mQxi1ddI, 3:50).
        mes("You take an empty bucket.")
    }

    private companion object {
        const val BUCKET_PILE = "loc.my2arm_throne_room_buckets"
        const val BUCKET_EMPTY = "obj.bucket_empty"
        const val TAKE_SEQ = "seq.human_pickuptable_walkmerge"
    }
}
