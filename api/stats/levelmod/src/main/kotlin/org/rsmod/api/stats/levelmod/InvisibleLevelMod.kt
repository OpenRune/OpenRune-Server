package org.rsmod.api.stats.levelmod

import dev.openrune.types.StatType
import org.rsmod.game.entity.Player

abstract class InvisibleLevelMod(internal val stat: String) {
    /**
     * Calculates the invisible level boost for the [stat] skill of the [Player] in scope. The boost
     * is a positive integer added to the player's visual level to influence skill success rate
     * rolls.
     */
    abstract fun Player.calculateBoost(): Int
}
