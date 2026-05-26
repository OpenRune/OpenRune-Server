package org.rsmod.api.combat.formulas

import dev.openrune.ServerCacheManager
import dev.openrune.types.NpcServerType
import org.rsmod.api.config.refs.BaseParams
import org.rsmod.game.entity.Player

/**
 * Hit chance formulas internally use decimals (e.g., `1%` = `0.01`, `100%` = `1.0`). To maintain
 * consistency with other combat formulas that use whole integers, we scale them using this
 * constant.
 */
internal const val HIT_CHANCE_SCALE: Int = 10_000

internal fun scale(base: Int, multiplier: Int, divisor: Int): Int = (base * multiplier) / divisor

internal fun NpcServerType.isSlayerTask(player: Player): Boolean {
    val taskId = player.vars["varp.slayer_target"]
    if (taskId <= 0) return false

    val category = paramOrNull(BaseParams.slayer_task_id)
    if (category != null && category > 0 && category == taskId) {
        return true
    }

    return superiorNpcIdsByTaskId[taskId]?.contains(id) == true
}

private val superiorNpcIdsByTaskId: Map<Int, Set<Int>> by lazy { buildSuperiorNpcIdsByTask() }

private fun buildSuperiorNpcIdsByTask(): Map<Int, Set<Int>> {
    val map = mutableMapOf<Int, MutableSet<Int>>()
    for ((_, type) in ServerCacheManager.getNpcs()) {
        val taskId = type.paramOrNull(BaseParams.slayer_task_id) ?: continue
        val superiorId = type.paramOrNull<NpcServerType>(BaseParams.slayer_superior)?.id ?: continue
        map.getOrPut(taskId) { mutableSetOf() }.add(superiorId)
    }
    return map
}
