package org.rsmod.api.config

import dev.openrune.types.NpcServerType
import dev.openrune.types.SlayerTaskKey
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.game.entity.Player

object SlayerTaskCredit {
    private const val TARGET_VARP = "varp.slayer_target"
    private const val BOSS_VARBIT = "varbit.slayer_target_bossid"

    fun taskKey(player: Player): Int {
        val taskId = player.vars[TARGET_VARP]
        if (taskId != SlayerTaskKey.BOSS_TASK_ID) return taskId
        return SlayerTaskKey.of(taskId, player.vars[BOSS_VARBIT])
    }

    fun counts(type: NpcServerType, key: Int): Boolean {
        if (key <= 0) return false
        val primary = type.paramOrNull(BaseParams.slayer_task_id)
        val extras = type.paramOrNull(BaseParams.slayer_task_ids)
        return SlayerTaskKey.contains(primary, extras, key)
    }

    fun keys(type: NpcServerType): List<Int> {
        val primary = type.paramOrNull(BaseParams.slayer_task_id)?.takeIf { it > 0 }
        val extras = type.paramOrNull(BaseParams.slayer_task_ids)
        if (extras.isNullOrEmpty()) return listOfNotNull(primary)
        val parsed = extras.split(',').mapNotNull { it.toIntOrNull() }
        return if (primary == null) parsed else listOf(primary) + parsed
    }
}
