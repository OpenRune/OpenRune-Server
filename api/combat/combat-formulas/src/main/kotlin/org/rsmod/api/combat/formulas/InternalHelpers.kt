package org.rsmod.api.combat.formulas

import dev.openrune.types.NpcServerType
import org.rsmod.game.entity.Player

/**
 * Hit chance formulas internally use decimals (e.g., `1%` = `0.01`, `100%` = `1.0`). To maintain
 * consistency with other combat formulas that use whole integers, we scale them using this
 * constant.
 */
internal const val HIT_CHANCE_SCALE: Int = 10_000

internal fun scale(base: Int, multiplier: Int, divisor: Int): Int = (base * multiplier) / divisor

internal fun NpcServerType.isSlayerTask(player: Player): Boolean {
    // TODO(combat): Resolve if type is slayer task.
    return false
}
