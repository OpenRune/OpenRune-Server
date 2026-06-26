package org.rsmod.content.areas.city.varrock.scurrius

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.plugin.scripts.ScriptContext

class ScurriusInstance @Inject constructor(
    registry: BossInstanceRegistry
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_scurrius"

    override fun area(): InstanceArea = PRIVATE_AREA

    override fun ScriptContext.configure() {
        val row = settingsRowData()

        onEnterObject { enterPublicRoom(PUBLIC_AREA) }
        onOpLoc2(row.enterObject) { defaultInstanceEntry() }
        onOpLoc3(row.enterObject) { peekPublicRoom() }

        onExitObject { defaultLeaveFlow() }
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
        private val PUBLIC_AREA = InstanceArea.copyRegions(
            centerRegionId = 13210,
            npcSpawns = listOf(InstanceNpc("npc.rat_boss_normal", CoordGrid(3299, 9867))),
        )
        private val PRIVATE_AREA = InstanceArea.copyRegions(
            centerRegionId = 13210,
            npcSpawns = listOf(InstanceNpc("npc.rat_boss_instance", CoordGrid(3299, 9867))),
        )
    }
}
