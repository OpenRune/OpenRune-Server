package org.rsmod.content.bosses.kbd

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc5
import org.rsmod.plugin.scripts.ScriptContext

class KingBlackDragonInstance @Inject constructor(
    registry: BossInstanceRegistry
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_kbd"

    override fun area(): InstanceArea = PRIVATE_AREA

    override fun ScriptContext.configure() {
        val row = settingsRowData()

        onEnterObject { enterPublicRoom(PUBLIC_AREA) }
        onOpLoc5(row.enterObject) { defaultInstanceEntry() }
        onOpLoc2(row.enterObject) { peekPublicRoom() }

        onExitObject { defaultLeaveFlow() }
    }

    private fun ProtectedAccess.peekPublicRoom() {
        val session = manager.sessionsForKey(key).firstOrNull { it.isServerOwned }
        val count = session?.occupants?.size ?: 0
        if (count == 0) {
            mes("The lair is currently empty.")
        } else {
            mes("There ${if (count == 1) "is" else "are"} $count player${if (count == 1) "" else "s"} in the lair.")
        }
    }

    private companion object {
        private val PUBLIC_AREA = InstanceArea.copyRegions(
            regionIds = listOf(9033),
            npcSpawns = listOf(InstanceNpc("npc.king_dragon", RegionLocal(0, 35, 73, 31, 22))),
        )
        private val PRIVATE_AREA = InstanceArea.copyRegions(
            regionIds = listOf(9033),
            npcSpawns = listOf(InstanceNpc("npc.king_dragon", RegionLocal(0, 35, 73, 31, 22))),
        )
    }
}
