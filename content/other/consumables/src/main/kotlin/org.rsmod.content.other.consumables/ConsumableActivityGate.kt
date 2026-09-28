package org.rsmod.content.other.consumables

import org.rsmod.game.entity.Player

/**
 * Implemented by an activity's own module (a raid, a minigame) so its activity-only consumables
 * can be used inside it. Bound as a set in [ConsumablesModule]; an activity adds itself with
 * `addSetBinding<ConsumableActivityGate>(...)`.
 */
interface ConsumableActivityGate {
    /** `true` if [player] is inside [activity]: a potion's `raidOnly` or `minigameOnly` key. */
    fun isInside(player: Player, activity: String): Boolean

    /** The other players [player] is doing [activity] with, for effects that reach allies. */
    fun allies(player: Player, activity: String): List<Player> = emptyList()
}
