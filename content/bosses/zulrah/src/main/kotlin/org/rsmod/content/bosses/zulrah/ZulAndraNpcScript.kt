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
        val stationaryIds = setOf(
            "npc.snakeboss_highpriest".asRSCM(RSCMType.NPC),
            "npc.snakeboss_gnome_1".asRSCM(RSCMType.NPC),
            "npc.snakeboss_fishingspot".asRSCM(RSCMType.NPC),
            "npc.snakeboss_fishingspot_fake".asRSCM(RSCMType.NPC),
        )

        onEvent<NpcStateEvents.Create> { anchor(npc, stationaryIds) }
        onEvent<NpcStateEvents.Respawn> { anchor(npc, stationaryIds) }
    }

    private fun anchor(npc: Npc, stationaryIds: Set<Int>) {
        if (npc.id !in stationaryIds) return

        val spawn = npc.spawnCoords
        if (spawn.level != 0 || spawn.x !in 2160..2239 || spawn.z !in 3008..3071) return

        npc.movementLocked = true
        npc.mode = NpcMode.None
    }
}
