package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpcU
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TurgallScript @Inject constructor(private val parts: DorgeshKaanParts) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TURGALL) { with(parts) { talk(it.npc) } }
        onOpNpcU(TURGALL) { with(parts) { useOn(it.npc, it.objType) } }
        onOpLocU(BOILER, SPANNER) { with(parts) { takePart(heavy = true) } }
        onOpLocU(CONSOLE, SPANNER) { with(parts) { takePart(heavy = false) } }
        onPlayerCoordsChanged {
            if (
                DorgeshKaanCourse.teleportedOut(lastKnownCoords, player.coords) &&
                    DorgeshKaanCourse.carriesPart(player)
            ) {
                player.strongQueue(BREAK_PARTS, 1)
            }
        }
        onPlayerQueue(BREAK_PARTS) { DorgeshKaanCourse.breakParts(this) }
    }

    private companion object {
        const val TURGALL = "npc.dorgesh_male_engineer"
        const val SPANNER = "obj.dorgesh_spanner"
        const val BOILER = "loc.dorgesh_old_generator_boiler"
        const val CONSOLE = "loc.dorgesh_old_generator_console"
        const val BREAK_PARTS = "queue.dorgesh_agility_break_parts"
    }
}
