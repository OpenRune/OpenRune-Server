package org.rsmod.api.account.character.stats

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player

public class CharacterStatApplier : CharacterDataStage.Applier<CharacterStatData> {
    override fun apply(player: Player, data: CharacterStatData) {
        for (loaded in data.stats) {
            val (type, vis, base, fineXp) = loaded
            val stat = RSCM.getReverseMapping(RSCMType.STAT, type)
            player.statMap.setCurrentLevel(stat, vis.toByte())
            player.statMap.setBaseLevel(stat, base.toByte())
            player.statMap.setFineXP(stat, fineXp)
        }
    }
}
