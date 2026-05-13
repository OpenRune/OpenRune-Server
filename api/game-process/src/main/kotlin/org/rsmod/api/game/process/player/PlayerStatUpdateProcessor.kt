package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import jakarta.inject.Inject
import org.rsmod.api.player.output.UpdateStat
import org.rsmod.api.player.stat.stat
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerPersistenceHints

public class PlayerStatUpdateProcessor
@Inject
constructor(private val invisibleLevels: InvisibleLevels) {
    public fun process(player: Player) {
        if (player.pendingStatUpdates.isEmpty) {
            return
        }
        player.updatePendingStats()
    }

    private fun Player.updatePendingStats() {
        var nextStat = pendingStatUpdates.nextSetBit(0)
        while (nextStat >= 0) {
            val statName = RSCM.getReverseMapping(RSCMType.STAT,ServerCacheManager.getStats().getValue(nextStat).id)
            updateStatXp(statName)
            nextStat = pendingStatUpdates.nextSetBit(nextStat + 1)
        }
        pendingStatUpdates.clear()
        PlayerPersistenceHints.notify(this)
    }

    private fun Player.updateStatXp(stat: String) {
        val currXp = statMap.getXP(stat)
        val currLvl = stat(stat)
        val hiddenLevel = currLvl + invisibleLevels.get(this, stat)
        UpdateStat.update(this, ServerCacheManager.getStats(stat.asRSCM(RSCMType.STAT))!!, currXp, currLvl, hiddenLevel)
    }
}
