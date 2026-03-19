package org.rsmod.api.account.character.stats

import dev.openrune.ServerCacheManager
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.game.entity.Player

public class CharacterStatApplier : CharacterDataStage.Applier<CharacterStatData> {
    override fun apply(player: Player, data: CharacterStatData) {
        for (loaded in data.stats) {
            val (type, vis, base, fineXp) = loaded
            val stat =
                ServerCacheManager.getStats(type) ?: error("Error loading stat on load: $type")
            player.statMap.setCurrentLevel(stat, vis.toByte())
            player.statMap.setBaseLevel(stat, base.toByte())
            player.statMap.setFineXP(stat, fineXp)
        }
    }
}
