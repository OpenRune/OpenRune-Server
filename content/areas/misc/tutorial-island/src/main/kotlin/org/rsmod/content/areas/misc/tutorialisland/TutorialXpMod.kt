package org.rsmod.content.areas.misc.tutorialisland

import org.rsmod.api.player.stat.stat
import org.rsmod.api.stats.xpmod.XpMod
import org.rsmod.game.entity.Player

internal class TutorialXpMod : XpMod {
    override fun Player.modifier(stat: String): Double {
        if (!inTutorial) {
            return 0.0
        }
        if (stat == "stat.hitpoints") {
            return -1.0
        }
        return if (stat(stat) >= TUTORIAL_SKILL_CAP) -1.0 else 0.0
    }

    private companion object {
        private const val TUTORIAL_SKILL_CAP = 3
    }
}
