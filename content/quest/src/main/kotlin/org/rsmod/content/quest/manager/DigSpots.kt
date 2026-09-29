package org.rsmod.content.quest.manager

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld1
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Spade digs are shared by several quests. A handler claims a dig by returning true; the first
 * claiming handler wins, and an unclaimed dig finds nothing.
 */
object DigSpots {
    private val handlers = mutableListOf<suspend ProtectedAccess.(CoordGrid) -> Boolean>()

    fun register(handler: suspend ProtectedAccess.(CoordGrid) -> Boolean) {
        handlers += handler
    }

    /** Runs the registered handlers for the player's tile; true when one claimed the dig. */
    suspend fun dig(access: ProtectedAccess): Boolean {
        val coords = access.player.coords
        return handlers.any { it(access, coords) }
    }
}

class SpadeDigScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1("obj.spade") { dig() }
    }

    private suspend fun ProtectedAccess.dig() {
        anim("seq.human_dig")
        soundSynth("synth.digspade")
        if (DigSpots.dig(this)) {
            return
        }
        delay(1)
        mes("Nothing interesting happens.")
    }
}
