package org.rsmod.content.skills.fishing

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.game.entity.Player

class FishingLevelBoosts @Inject constructor(private val areas: AreaChecker) :
    InvisibleLevelMod("stat.fishing") {
    override fun Player.calculateBoost(): Int {
        var boost = 0
        if (areas.inArea("area.fishing_guild", coords)) {
            boost += 7
        }
        return boost
    }
}
