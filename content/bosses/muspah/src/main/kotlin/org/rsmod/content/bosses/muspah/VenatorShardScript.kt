package org.rsmod.content.bosses.muspah

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VenatorShardScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1("obj.venator_shard") { combine() }
    }

    private suspend fun ProtectedAccess.combine() {
        if (invTotal(inv, "obj.venator_shard") < REQUIRED_SHARDS) {
            mes("You need $REQUIRED_SHARDS venator shards to construct a venator bow.")
            return
        }

        if (invDel(inv, "obj.venator_shard", REQUIRED_SHARDS).failure) {
            return
        }

        invAdd(inv, "obj.venator_bow_uncharged", 1)
        objbox("obj.venator_bow_uncharged", 400, "The shards assemble into a venator bow.")
    }

    private companion object {
        const val REQUIRED_SHARDS = 5
    }
}
