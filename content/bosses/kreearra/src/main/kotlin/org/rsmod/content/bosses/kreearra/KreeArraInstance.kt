package org.rsmod.content.bosses.kreearra

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

class KreeArraInstance @Inject constructor(registry: BossInstanceRegistry) :
    InstanceScript(registry) {

    override fun settingsRow(): String = "dbrow.instance_kreearra"

    override fun area(): InstanceArea = INSTANCE

    override fun ScriptContext.configure() {
        onEnterObject { enterArmadylPublic() }
        onOpLoc2(GOD_DOOR) { enterArmadylPrivate() }
        onOpLoc3(GOD_DOOR) { peekArmadylPublic() }
        onExitObject { defaultLeaveFlow() }
    }

    private suspend fun ProtectedAccess.enterArmadylPublic() {
        if (manager.sessionForPlayer(player) != null) {
            defaultLeaveFlow()
            return
        }
        if (!hasRequiredKillcount()) return
        enterPublicRoom(INSTANCE)
    }

    private suspend fun ProtectedAccess.enterArmadylPrivate() {
        if (!hasRequiredKillcount()) return
        defaultInstanceEntry()
    }

    private fun ProtectedAccess.peekArmadylPublic() {
        val session = manager.sessionsForKey(key).firstOrNull { it.isServerOwned }
        val count = session?.occupants?.size ?: 0
        if (count == 0) {
            mes("The Armadyl eyrie is currently empty.")
        } else {
            mes(
                "There ${if (count == 1) "is" else "are"} $count player${if (count == 1) "" else "s"} " +
                    "in the Armadyl eyrie."
            )
        }
    }

    private suspend fun ProtectedAccess.hasRequiredKillcount(): Boolean {
        val killcount = player.vars["varbit.godwars_counter_armadyl"]
        if (killcount < REQUIRED_KILLCOUNT) {
            mes(
                "You need a killcount of at least $REQUIRED_KILLCOUNT of Armadyl's followers " +
                    "to enter."
            )
            return false
        }
        VarPlayerIntMapSetter.set(
            player,
            "varbit.godwars_counter_armadyl",
            killcount - REQUIRED_KILLCOUNT,
        )
        return true
    }

    private companion object {
        private const val REQUIRED_KILLCOUNT = 40

        private const val GOD_DOOR = "loc.godwars_dungeon_door_armadyl"

        private val INSTANCE =
            InstanceArea.copyRegions(
                regionIds = listOf(11346),
                level = 2,
                npcSpawns =
                    listOf(
                        InstanceNpc("npc.godwars_armadyl_avatar", CoordGrid(2832, 5302, 2)),
                        InstanceNpc(
                            "npc.godwars_armadyl_bodyguard_geerin",
                            CoordGrid(2828, 5299, 2),
                        ),
                        InstanceNpc(
                            "npc.godwars_armadyl_bodyguard_kilisa",
                            CoordGrid(2833, 5297, 2),
                        ),
                        InstanceNpc("npc.godwars_armadyl_bodyguard_skree", CoordGrid(2840, 5303, 2)),
                    ),
            )
    }
}
