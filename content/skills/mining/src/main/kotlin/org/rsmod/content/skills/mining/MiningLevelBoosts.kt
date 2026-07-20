package org.rsmod.content.skills.mining

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.stats.levelmod.InvisibleLevelMod
import org.rsmod.game.entity.Player

class MiningLevelBoosts @Inject constructor(private val areas: AreaChecker) :
    InvisibleLevelMod("stat.mining") {
    override fun Player.calculateBoost(): Int {
        var boost = 0
        if (areas.inArea("area.mining_guild", coords)) {
            boost += 7
        }
        if (wearingCelestial()) {
            boost += 4
        }
        return boost
    }

    private fun Player.wearingCelestial(): Boolean =
        "obj.celestial_ring" in worn ||
            "obj.celestial_ring_charged" in worn ||
            "obj.celestial_signet" in worn ||
            "obj.celestial_signet_charged" in worn
}
