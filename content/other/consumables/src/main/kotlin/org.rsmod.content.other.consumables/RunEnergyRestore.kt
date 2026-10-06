package org.rsmod.content.other.consumables

import org.rsmod.api.config.constants
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.api.player.protect.ProtectedAccess

internal const val RUN_ENERGY_PER_PERCENT: Int = constants.run_max_energy / 100

internal fun runEnergyAfterRestore(current: Int, percent: Int): Int {
    if (percent <= 0) {
        return current
    }
    val restored = current + percent * RUN_ENERGY_PER_PERCENT
    return maxOf(current, restored.coerceAtMost(constants.run_max_energy))
}

internal fun ProtectedAccess.restoreRunEnergy(percent: Int) {
    val restored = runEnergyAfterRestore(player.runEnergy, percent)
    if (restored == player.runEnergy) {
        return
    }
    player.runEnergy = restored
    UpdateRun.energy(player, restored)
}
