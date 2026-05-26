package org.rsmod.content.slayer.konar

import org.rsmod.api.table.slayer.SlayerAreaRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.game.entity.Player

object KonarSlayerDialogueHelpers {

    fun findArea(areaId: Int): SlayerAreaRow? {
        if (areaId == 0) return null
        return SlayerAreaRow.all().find { it.areaId == areaId }
    }

    fun currentArea(player: Player): SlayerAreaRow? = findArea(player.vars["varp.slayer_area"])

    fun areaShortName(area: SlayerAreaRow): String =
        area.areaNameInHelper.takeIf { it.isNotBlank() }
            ?: area.areaText.takeIf { it.isNotBlank() }
            ?: "the assigned location"

    fun areaDescription(area: SlayerAreaRow): String =
        area.areaText.takeIf { it.isNotBlank() }
            ?: "You'll find them in ${area.areaNameInHelper}."

    fun monsterName(task: SlayerTaskRow): String = task.nameLowercase.ifBlank { task.nameUppercase }
}
