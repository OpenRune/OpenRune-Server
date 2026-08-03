package org.rsmod.content.bosses.kril

import jakarta.inject.Inject
import org.rsmod.api.instances.BossInstanceRegistry
import org.rsmod.api.instances.InstanceArea
import org.rsmod.api.instances.InstanceNpc
import org.rsmod.api.instances.InstanceScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class KrilInstance
@Inject
constructor(registry: BossInstanceRegistry) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_kril"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterObject { enterZamorakPublic() }
        onOpLoc2(GOD_DOOR) { enterZamorakPrivate() }
        onOpLoc3(GOD_DOOR) { peekZamorakPublic() }
        onExitObject { defaultLeaveFlow() }
    }

    private suspend fun ProtectedAccess.enterZamorakPublic() {
        if (manager.sessionForPlayer(player) != null) {
            defaultLeaveFlow()
            return
        }
        if (!hasRequiredKillcount()) return
        enterPublicRoom(INSTANCE)
    }

    private suspend fun ProtectedAccess.enterZamorakPrivate() {
        if (!hasRequiredKillcount()) return
        defaultInstanceEntry()
    }

    private fun ProtectedAccess.peekZamorakPublic() {
        val session = manager.sessionsForKey(key).firstOrNull { it.isServerOwned }
        val count = session?.occupants?.size ?: 0
        if (count == 0) {
            mes("The Zamorak fortress is currently empty.")
        } else {
            mes(
                "There ${if (count == 1) "is" else "are"} $count player${if (count == 1) "" else "s"} " +
                    "in the Zamorak fortress."
            )
        }
    }

    private suspend fun ProtectedAccess.hasRequiredKillcount(): Boolean {
        val killcount = player.vars["varbit.godwars_counter_zamorak"]
        if (killcount < REQUIRED_KILLCOUNT) {
            mes(
                "You need a killcount of at least $REQUIRED_KILLCOUNT of Zamorak's followers " +
                    "to enter."
            )
            return false
        }
        VarPlayerIntMapSetter.set(
            player,
            "varbit.godwars_counter_zamorak",
            killcount - REQUIRED_KILLCOUNT,
        )
        return true
    }

    private companion object {
        private const val REQUIRED_KILLCOUNT = 40

        private const val GOD_DOOR = "loc.godwars_dungeon_door_zamorak"

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(11603),
                level = 2,
                npcSpawns =
                    listOf(
                        InstanceNpc("npc.godwars_zamorak_avatar", CoordGrid(2925, 5322, 2)),
                        InstanceNpc(
                            "npc.godwars_ancient_black_demon",
                            CoordGrid(2921, 5319, 2),
                        ),
                        InstanceNpc(
                            "npc.godwars_ancient_greater_demon",
                            CoordGrid(2932, 5328, 2),
                        ),
                        InstanceNpc(
                            "npc.godwars_ancient_lesser_demon",
                            CoordGrid(2919, 5327, 2),
                        ),
                    ),
            )
    }
}
