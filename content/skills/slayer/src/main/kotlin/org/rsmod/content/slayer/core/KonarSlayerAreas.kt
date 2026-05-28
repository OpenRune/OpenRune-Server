package org.rsmod.content.slayer.core

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.table.slayer.SlayerAreaRow
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

object KonarSlayerAreas {

    fun resolveTaskArea(
        player: Player,
        masterTask: SlayerMasterTaskRow,
        preferredAreaId: Int?
    ): Int? {
        val eligible = eligibleAreas(player, masterTask)
        if (eligible.isEmpty()) {
            return null
        }

        val preferred = preferredAreaId?.takeIf { it != 0 }
            ?.takeIf { id -> eligible.any { it.areaId == id } }

        return preferred ?: eligible.random().areaId
    }

    fun eligibleAreas(
        player: Player,
        masterTask: SlayerMasterTaskRow
    ): List<SlayerAreaRow> {
        if (masterTask.areas.isEmpty()) {
            return emptyList()
        }

        val allowWilderness = SlayerBossTasks.isBossTask(masterTask.task.id)

        return masterTask.areas.filter { area ->
            meetsAreaQuestRequirement(area, player) &&
                (allowWilderness || !isWildernessSlayerArea(area))
        }
    }

    fun meetsAreaQuestRequirement(area: SlayerAreaRow, player: Player): Boolean {
        return true
    }

    fun isWildernessSlayerArea(area: SlayerAreaRow): Boolean {
        return area.areaNameInHelper.equals("Wilderness", ignoreCase = true) ||
            area.areaText.contains("Wilderness", ignoreCase = true)
    }

    fun countsKillInTaskArea(
        player: Player,
        npc: Npc,
        areaChecker: AreaChecker
    ): Boolean {
        if (player.vars["varbit.slayer_master"] != 8) {
            return true
        }

        val areaId = player.vars["varp.slayer_area"]
        if (areaId == 0) {
            return true
        }

        val areaName = RSCM.getReverseMapping(RSCMType.AREA, areaId)
        return areaChecker.inArea(areaName, npc.coords)
    }
}
