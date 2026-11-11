package org.rsmod.api.player.events

import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.api.table.slayer.SlayerTaskRow
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

public class PlayerSlayerEvents {
    /**
     * A kill that will count toward the player's task, before the count changes, so the task is
     * still readable on its final kill. [extraKills] and [skipDecrement] start as the bracelets'
     * rolls. Only the player the kill is credited to gets this.
     */
    public class KillPrepare(
        override val player: Player,
        public val npc: Npc,
        public var extraKills: Int,
        public var skipDecrement: Boolean,
    ) : UnboundEvent, PlayerEvent

    /**
     * A finished task, before anything is cleared, so its vars and mortifier are still readable.
     * Handlers grant other rewards directly. [points] is a field because the cap and chat message
     * live in the slayer module; it starts at what the player would naturally earn, which is 0
     * early in a streak, so [basePoints] is the master's per-task value before any streak rules.
     *
     * @param task null if the target var does not match a task row.
     * @param amount the task's original kill count.
     * @param streak includes this task. Mortimer has his own streak.
     */
    public class TaskComplete(
        override val player: Player,
        public val master: SlayerMastersRow,
        public val task: SlayerTaskRow?,
        public val bossId: Int,
        public val amount: Int,
        public val streak: Int,
        public val basePoints: Int,
        public var points: Int,
    ) : UnboundEvent, PlayerEvent
}
