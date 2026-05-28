package org.rsmod.content.slayer.dialogue

import org.rsmod.api.table.slayer.SlayerAreaRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.game.entity.Player

object KonarSlayerDialogueHelpers {

    fun findArea(areaId: Int): SlayerAreaRow? {
        return areaId.takeIf { it != 0 }
            ?.let { id -> SlayerAreaRow.Companion.all().find { it.areaId == id } }
    }

    fun currentArea(player: Player): SlayerAreaRow? = findArea(player.vars["varp.slayer_area"])


    fun areaShortName(area: SlayerAreaRow): String {
        return area.areaNameInHelper.takeIf(String::isNotBlank)
            ?: area.areaText.takeIf(String::isNotBlank)
            ?: "the assigned location"
    }

    fun areaDescription(area: SlayerAreaRow): String {
        return area.areaText.takeIf(String::isNotBlank)
            ?: "You'll find them in ${area.areaNameInHelper}."
    }

    fun monsterName(task: SlayerTaskRow) = task.nameLowercase.ifBlank { task.nameUppercase }

}
