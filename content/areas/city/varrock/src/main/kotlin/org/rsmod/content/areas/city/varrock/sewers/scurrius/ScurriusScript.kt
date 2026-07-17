package org.rsmod.content.areas.city.varrock.sewers.scurrius

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScurriusScript
@Inject
constructor(private val npcRepo: NpcRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        //onOpLoc1(ENTRANCE_LOC) { enterPublicRoom() }
        //onOpLoc2(ENTRANCE_LOC) { enterPrivateInstance() }
        //onOpLoc1(EXIT_LOC) { exit() }
        //onOpLoc2(EXIT_LOC) { exit() }
    }

    private suspend fun ProtectedAccess.enterPublicRoom() {
        arriveDelay()
        telejump(PLAYER_SPAWN)
    }

//    private suspend fun ProtectedAccess.enterPrivateInstance() {
//        arriveDelay()
//        val instance =
//            instanceRepo.create(ARENA_TEMPLATE, ENTRANCE_COORDS)
//                ?: run {
//                    mes("The instance is currently unavailable. Please try again.")
//                    return
//                }
//        val bossCoords = instance.region.normal[BOSS_SPAWN]
//        npcRepo.add(Npc(NPC_SCURRIUS, bossCoords), Int.MAX_VALUE)
//        instanceRepo.enter(instance, player)
//        telejump(instance.region.normal[PLAYER_SPAWN])
//    }

//    private suspend fun ProtectedAccess.exit() {
//        arriveDelay()
//        val instance = instanceRepo.instanceOf(player)
//        if (instance != null) {
//            instanceRepo.leave(instance, player)
//        }
//        telejump(ENTRANCE_COORDS)
//    }

    companion object {
        private const val ENTRANCE_LOC = "loc.rat_boss_entrance"
        private const val NPC_SCURRIUS = "npc.rat_boss_instance"
        private const val EXIT_LOC = "loc.rat_boss_exit"

        private val ENTRANCE_COORDS = CoordGrid(3281, 9868)
        private val BOSS_SPAWN = CoordGrid(3305, 9874)
        private val PLAYER_SPAWN = CoordGrid(3290, 9868)

        private val ARENA_TEMPLATE =
            RegionTemplate.create {
                copyAllLevels(410, 1231) {
                    zoneWidth = 6
                    zoneLength = 6
                }
            }
    }
}
