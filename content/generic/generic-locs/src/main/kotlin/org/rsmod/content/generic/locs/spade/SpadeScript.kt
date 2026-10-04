package org.rsmod.content.generic.locs.spade

import jakarta.inject.Inject
import org.rsmod.api.player.hook.SpadeDigHook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SpadeScript @Inject constructor(private val hooks: Set<@JvmSuppressWildcards SpadeDigHook>) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1("obj.spade") { dig() }
    }

    private suspend fun ProtectedAccess.dig() {
        val hook = hooks.firstOrNull { it.claims(player) }
        if (hook != null && !with(hook) { beforeDig() }) {
            return
        }
        anim("seq.human_dig")
        soundSynth("synth.digspade")
        delay(DigTicks)
        resetAnim()
        if (hook == null) {
            mes("You dig a hole in the ground... but find nothing.")
            return
        }
        with(hook) { dig() }
    }

    private companion object {
        private const val DigTicks = 2
    }
}
