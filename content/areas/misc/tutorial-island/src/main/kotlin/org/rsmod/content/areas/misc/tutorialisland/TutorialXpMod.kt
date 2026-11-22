package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.game.entity.Player

internal class TutorialXpMod : XpMod {
    override fun Player.modifier(stat: String): Double {
        if (!inTutorial) {
            return 0.0
        }
        if (tutorialXpBlocked(stat)) {
            return -1.0
        }
        val rate = xpRate * globalXpRate
        return if (rate > 0.0) (1.0 / rate) - 1.0 else -1.0
    }
}
