package org.rsmod.content.minigames.giantsfoundry

import jakarta.inject.Inject
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Buckets for cooling the poured metal: the pile of buckets (also in Weiss) and filling one at
 * the foundry's waterfall. https://oldschool.runescape.wiki/w/Pile_of_Buckets
 */
class FoundrySuppliesScript @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(BUCKET_PILE) { takeBucket() }
        onOpLocU(WATERFALL, BUCKET_EMPTY) { fillBucket() }
    }

    private fun ProtectedAccess.takeBucket() {
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(TAKE_SEQ)
        invAdd(inv, BUCKET_EMPTY, 1)
        // UNVERIFIED: the wiki gives no message for taking a bucket.
        mes("You take a bucket from the pile.", ChatType.Spam)
    }

    /** Same as other water sources (`WaterSourceScript`); the waterfall isn't one in the cache. */
    private fun ProtectedAccess.fillBucket() {
        anim(TAKE_SEQ)
        invDel(inv, BUCKET_EMPTY, 1)
        invAdd(inv, BUCKET_WATER, 1)
        mes("You fill the bucket from the waterfall.", ChatType.Spam)
    }

    private companion object {
        const val BUCKET_PILE = "loc.my2arm_throne_room_buckets"
        const val WATERFALL = "loc.giants_foundry_waterfall"
        const val BUCKET_EMPTY = "obj.bucket_empty"
        const val BUCKET_WATER = "obj.bucket_water"
        const val TAKE_SEQ = "seq.human_pickuptable_walkmerge"
    }
}
