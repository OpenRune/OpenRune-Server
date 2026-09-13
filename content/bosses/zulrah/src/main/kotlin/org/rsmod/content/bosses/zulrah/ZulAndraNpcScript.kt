package org.rsmod.content.bosses.zulrah

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import org.rsmod.api.script.onEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ZulAndraNpcScript : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcStateEvents.Create> { anchor(npc) }
        onEvent<NpcStateEvents.Respawn> { anchor(npc) }
    }

    internal fun anchor(npc: Npc) {
        if (npc.spawnCoords.level != 0 || npc.spawnCoords.x !in 2160..2239 ||
            npc.spawnCoords.z !in 3008..3071 || npc.id !in stationaryIds) return
        npc.movementLocked = true
        npc.mode = NpcMode.None
    }

    private val stationaryIds by lazy {
        listOf("npc.snakeboss_highpriest", "npc.snakeboss_gnome_1",
            "npc.snakeboss_fishingspot", "npc.snakeboss_fishingspot_fake")
            .map { it.asRSCM(RSCMType.NPC) }.toSet()
    }
}
