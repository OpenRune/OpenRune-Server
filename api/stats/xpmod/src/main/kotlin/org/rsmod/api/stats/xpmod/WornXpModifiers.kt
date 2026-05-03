package org.rsmod.api.stats.xpmod

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.StatType
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.getInvObj

class WornXpModifiers : XpMod {
    override fun Player.modifier(stat: String): Double {
        val percent = worn.sumOf { it?.modPercent(stat) ?: 0 }
        return percent / 100.0
    }

    private fun InvObj.modPercent(stat: String): Int {
        val objType = getInvObj(this)
        if (objType.paramOrNull(params.xpmod_stat) != ServerCacheManager.getStats(stat.asRSCM(RSCMType.STAT))) {
            return 0
        }
        return objType.param(params.xpmod_percent)
    }
}
