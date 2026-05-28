package org.rsmod.content.slayer.dialogue

import dev.openrune.types.enums.enum
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.content.slayer.dialogue.KonarSlayerDialogueHelpers
import org.rsmod.game.entity.Player

object SlayerTaskTips {
    private const val FALLBACK_TIP = "Try not to die."

    private val taskTips by lazy { enum<Int, String>("slayer_task_tips") }

    fun tipsFor(task: SlayerTaskRow, player: Player): List<String> {
        val tips = mutableListOf<String>()

        taskTips[task.id]?.takeIf { it.isNotBlank() }?.let { tips.add(it) }

        KonarSlayerDialogueHelpers.currentArea(player)?.areaHint?.takeIf { it.isNotBlank() }?.let { tips.add(it) }

        if (tips.isEmpty()) {
            tips.add(FALLBACK_TIP)
        }
        return tips
    }
}
