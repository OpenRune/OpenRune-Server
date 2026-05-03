package org.rsmod.api.hit.plugin

import org.rsmod.api.player.hit.DeferredPlayerHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlayerHitScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerQueueWithArgs("queue.hit") { processQueuedHit(it.args) }
        onPlayerQueueWithArgs("queue.impact_hit") { processQueuedDeferredHit(it.args) }
    }

    private fun ProtectedAccess.processQueuedDeferredHit(deferred: DeferredPlayerHit) {
        val (builder, modifier) = deferred
        processQueuedHit(builder, modifier)
    }
}
