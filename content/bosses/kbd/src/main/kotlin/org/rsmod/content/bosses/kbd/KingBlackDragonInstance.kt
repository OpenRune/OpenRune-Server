package org.rsmod.content.bosses.kbd

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.instances.RegionLocal
import org.rsmod.api.instances.enterLocObjects
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.api.script.onOpLoc1
import org.rsmod.map.CoordGrid
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc5
import org.rsmod.plugin.scripts.ScriptContext

class KingBlackDragonInstance @Inject constructor(
    registry: BossInstanceRegistry
) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_kbd"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        val row = settingsRowData()

        //TODO MAKE THE LEAVER LIKE YOU PULLED IT AND CORRECT ANIM

        onEnterObject { enterPublicRoom(INSTANCE) }

        row.enterLocObjects().forEach { loc ->
            onOpLoc5(loc) { defaultInstanceEntry() }
            onOpLoc2(loc) { peekPublicRoom() }
        }

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
        private val INSTANCE = InstanceArea.copyRegions(
            regionIds = listOf(9033),
            npcSpawns = listOf(InstanceNpc("npc.king_dragon", RegionLocal(0, 35, 73, 31, 22))),
        )
    }
}
