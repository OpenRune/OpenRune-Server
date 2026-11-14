package org.rsmod.content.quest.area.asgarnia.icemountain

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Singleton
class BimCutscene
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val ruins: BimRuinsCutscene,
) {
    fun ensureEntrance(stage: Int) {
        val entrance = CoordGrid(ENTRANCE_DOOR_X, ENTRANCE_DOOR_Z)
        locRepo.findAll(entrance).toList().forEach { loc ->
            when (loc.id) {
                CUTSCENE_DOOR_ID,
                RUBBLE_ID,
                ENTRANCE_ID,
                DOOR_ID,
                -> locRepo.del(loc, Int.MAX_VALUE)
            }
        }
        if (stage >= BimStage.INSIDE_CUTSCENE) {
            locRepo.add(entrance, DOOR, Int.MAX_VALUE, ENTRANCE_ANGLE, ENTRANCE_SHAPE)
        } else {
            locRepo.add(entrance, ENTRANCE, Int.MAX_VALUE, ENTRANCE_ANGLE, ENTRANCE_SHAPE)
        }
    }

    suspend fun playEntrance(access: ProtectedAccess, quest: BelowIceMountain) {
        removeBaseCrew()
        removeEntranceRamarno()
        ruins.play(access)
        ensureEntrance(BimStage.INSIDE_CUTSCENE)
    }

    fun release(player: Player) {
        ruins.release(player)
    }

    private fun removeBaseCrew() {
        for (type in BASE_CREW) {
            for (tile in BASE_CREW_TILES) {
                npcRepo
                    .findAll(tile)
                    .filter { it.isType(type) }
                    .toList()
                    .forEach { npcRepo.del(it, Int.MAX_VALUE) }
            }
        }
    }

    private fun removeEntranceRamarno() {
        npcRepo
            .findAll(CoordGrid(2951, 5779))
            .filter { it.isType("npc.camdozaal_ramarno_entrance_multi") }
            .toList()
            .forEach { npcRepo.del(it, Int.MAX_VALUE) }
    }

    private companion object {
        const val ENTRANCE_DOOR_X = 2999
        const val ENTRANCE_DOOR_Z = 3493
        val ENTRANCE_ANGLE = LocAngle[1]
        val ENTRANCE_SHAPE = LocShape.CentrepieceStraight

        const val ENTRANCE = "loc.bim_entrance"
        const val DOOR = "loc.bim_door"
        const val CUTSCENE_DOOR = "loc.bim_door_cutscene"
        const val RUBBLE = "loc.bim_rubble_3"

        val ENTRANCE_ID = ENTRANCE.asRSCM(RSCMType.LOC)
        val DOOR_ID = DOOR.asRSCM(RSCMType.LOC)
        val CUTSCENE_DOOR_ID = CUTSCENE_DOOR.asRSCM(RSCMType.LOC)
        val RUBBLE_ID = RUBBLE.asRSCM(RSCMType.LOC)

        val BASE_CREW =
            listOf(
                "npc.bim_willow_outside_entrace",
                "npc.bim_checkal_temple_outside",
                "npc.bim_marley_temple_outside",
                "npc.bim_burntof_temple_outside",
            )

        val BASE_CREW_TILES =
            listOf(
                CoordGrid(2997, 3494),
                CoordGrid(2994, 3494),
                CoordGrid(2992, 3497),
                CoordGrid(2997, 3489),
            )
    }
}
