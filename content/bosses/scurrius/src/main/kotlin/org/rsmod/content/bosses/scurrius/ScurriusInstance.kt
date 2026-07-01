package org.rsmod.content.bosses.scurrius

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceEnterTransition
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.enterLocObjects
import org.rsmod.api.instances.exitLocObjects
import org.rsmod.api.instances.withInstanceEnterTransition
import org.rsmod.api.instances.withInstanceLeaveTransition
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class ScurriusInstance @Inject constructor(
    registry: BossInstanceRegistry
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_scurrius"

    override fun area(): InstanceArea = PRIVATE_AREA

    override fun ScriptContext.configure() {
        val row = settingsRowData()
        onEnterPrelude { _, enter ->
            anim("seq.human_reachforladder")
            delay(1)
            withInstanceEnterTransition(InstanceEnterTransition(), enter)
        }

        onEnterObject { enterPublicRoom(PUBLIC_AREA) }

        row.enterLocObjects().forEach { loc ->
            onOpLoc2(loc) { defaultInstanceEntry() }
        }
        row.exitLocObjects().forEach { loc ->
            onOpLoc2(loc) { quickEscapeScurrius() }
            onOpLoc3(loc) { peekPublicRoom() }
        }

        onExitObject { crossLeaveScurrius() }
    }

    private suspend fun ProtectedAccess.crossLeaveScurrius() {
        anim("seq.human_ledge_off_right")
        delay(1)
        withInstanceLeaveTransition {
            defaultLeaveFlow()
        }
    }

    private suspend fun ProtectedAccess.quickEscapeScurrius() {
        defaultLeaveFlow()
    }

    private fun ProtectedAccess.peekPublicRoom() {
        val session = manager.sessionsForKey(key).firstOrNull { it.isServerOwned }
        val count = session?.occupants?.size ?: 0
        if (count == 0) {
            mes("The public Scurrius room is currently empty.")
        } else {
            mes("There ${if (count == 1) "is" else "are"} $count player${if (count == 1) "" else "s"} in the public Scurrius room.")
        }
    }

    private companion object {

        private fun seq(id: Int): String =
            RSCM.getReverseMapping(RSCMType.SEQ, id) ?: error("Missing seq id: $id")

        private val PUBLIC_AREA = InstanceArea.Companion.copyRegions(
            centerRegionId = 13210,
            npcSpawns = listOf(InstanceNpc("npc.rat_boss_normal", CoordGrid(3299, 9867))),
        )
        private val PRIVATE_AREA = InstanceArea.Companion.copyRegions(
            centerRegionId = 13210,
            npcSpawns = listOf(InstanceNpc("npc.rat_boss_instance", CoordGrid(3299, 9867))),
        )
    }
}
