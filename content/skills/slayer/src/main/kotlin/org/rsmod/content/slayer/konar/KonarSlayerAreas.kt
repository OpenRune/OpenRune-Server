package org.rsmod.content.slayer.konar

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.table.slayer.SlayerAreaRow
import org.rsmod.api.table.slayer.SlayerMasterTaskRow
import org.rsmod.content.slayer.core.SlayerBossTasks
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

object KonarSlayerAreas {

    fun resolveTaskArea(player: Player, masterTask: SlayerMasterTaskRow, preferredAreaId: Int?): Int? {
        val eligible = eligibleAreas(player, masterTask)
        if (eligible.isEmpty()) return null

        if (preferredAreaId != null && preferredAreaId != 0) {
            if (eligible.any { it.areaId == preferredAreaId }) {
                return preferredAreaId
            }
        }
        return eligible.random().areaId
    }

    fun eligibleAreas(player: Player, masterTask: SlayerMasterTaskRow): List<SlayerAreaRow> {
        if (masterTask.areas.isEmpty()) return emptyList()
        val allowWilderness = SlayerBossTasks.isBossTask(masterTask.task.id)
        return masterTask.areas.filter { area ->
            meetsAreaQuestRequirement(area, player) &&
                (allowWilderness || !isWildernessSlayerArea(area))
        }
    }

    fun meetsAreaQuestRequirement(area: SlayerAreaRow, @Suppress("UNUSED_PARAMETER") player: Player): Boolean {
        return true
    }

    fun isWildernessSlayerArea(area: SlayerAreaRow): Boolean =
        area.areaNameInHelper.equals("Wilderness", ignoreCase = true) ||
            area.areaText.contains("Wilderness", ignoreCase = true)

    fun countsKillInTaskArea(player: Player, npc: Npc, areaChecker: AreaChecker): Boolean {
        if (player.vars["varbit.slayer_master"] != 8) return true

        val areaVar = player.vars["varp.slayer_area"]
        if (areaVar == 0) return true

        val areaName = RSCM.getReverseMapping(RSCMType.AREA, areaVar)
        return areaChecker.inArea(areaName, npc.coords)
    }

}
