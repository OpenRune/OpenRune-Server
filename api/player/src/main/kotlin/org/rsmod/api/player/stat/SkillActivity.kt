package org.rsmod.api.player.stat

import org.rsmod.game.entity.Player

/**
 * A server-wide feed of finished skilling actions, for game modes such as leagues that track
 * player goals. [action] is a short verb such as `cook` or `smelt`; [subjects] are the objs (as
 * `obj.` symbols) or names the action concerned.
 */
public object SkillActivity {
    @Volatile public var listener: (Player, String, List<String>, Int) -> Unit = { _, _, _, _ -> }

    public fun record(player: Player, action: String, vararg subjects: String, count: Int = 1) {
        listener(player, action, subjects.toList(), count)
    }
}
