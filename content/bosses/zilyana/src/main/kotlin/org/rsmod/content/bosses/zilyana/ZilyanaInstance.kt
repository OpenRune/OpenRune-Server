package org.rsmod.content.bosses.zilyana

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

class ZilyanaInstance
@Inject
constructor(registry: BossInstanceRegistry) : InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_zilyana"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterObject { enterSaradominPublic() }
        onOpLoc2(GOD_DOOR) { enterSaradominPrivate() }
        onOpLoc3(GOD_DOOR) { peekSaradominPublic() }
        onExitObject { defaultLeaveFlow() }
    }

    private suspend fun ProtectedAccess.enterSaradominPublic() {
        if (manager.sessionForPlayer(player) != null) {
            defaultLeaveFlow()
            return
        }
        if (!hasRequiredKillcount()) return
        enterPublicRoom(INSTANCE)
    }

    private suspend fun ProtectedAccess.enterSaradominPrivate() {
        if (!hasRequiredKillcount()) return
        defaultInstanceEntry()
    }

    private fun ProtectedAccess.peekSaradominPublic() {
        val session = manager.sessionsForKey(key).firstOrNull { it.isServerOwned }
        val count = session?.occupants?.size ?: 0
        if (count == 0) {
            mes("The Saradomin encampment is currently empty.")
        } else {
            mes(
                "There ${if (count == 1) "is" else "are"} $count player${if (count == 1) "" else "s"} " +
                    "in the Saradomin encampment."
            )
        }
    }

    private suspend fun ProtectedAccess.hasRequiredKillcount(): Boolean {
        val killcount = player.vars["varbit.godwars_counter_saradomin"]
        if (killcount < REQUIRED_KILLCOUNT) {
            mes(
                "You need a killcount of at least $REQUIRED_KILLCOUNT of Saradomin's followers " +
                    "to enter."
            )
            return false
        }
        VarPlayerIntMapSetter.set(
            player,
            "varbit.godwars_counter_saradomin",
            killcount - REQUIRED_KILLCOUNT,
        )
        return true
    }

    private companion object {
        private const val REQUIRED_KILLCOUNT = 40

        private const val GOD_DOOR = "loc.godwars_dungeon_door_saradomin"

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(11602),
                level = 0,
                npcSpawns =
                    listOf(
                        InstanceNpc("npc.godwars_saradomin_avatar", CoordGrid(2897, 5269, 0)),
                        InstanceNpc("npc.godwars_saradomin_unicorn", CoordGrid(2903, 5261, 0)),
                        InstanceNpc("npc.godwars_saradomin_lion", CoordGrid(2896, 5264, 0)),
                        InstanceNpc("npc.godwars_saradomin_centaur", CoordGrid(2902, 5274, 0)),
                    ),
            )
    }
}
