package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.misc.client.ResetInteractionMode
import net.rsprot.protocol.game.outgoing.misc.client.SetInteractionMode
import org.rsmod.game.entity.Player

public object InteractionModes {
    public const val WORLD_DEFAULT: Int = -2

    public const val TILE_MODE_DISABLED: Int = 0
    public const val TILE_MODE_WALK: Int = 1
    public const val TILE_MODE_HEADING: Int = 2

    public const val ENTITY_MODE_DISABLED: Int = 0
    public const val ENTITY_MODE_ALL: Int = 1
    public const val ENTITY_MODE_EXAMINE: Int = 2

    /** @see [SetInteractionMode] */
    public fun setInteractionMode(player: Player, worldId: Int, tileMode: Int, entityMode: Int) {
        player.client.write(SetInteractionMode(worldId, tileMode, entityMode))
    }

    /** @see [ResetInteractionMode] */
    public fun resetInteractionMode(player: Player, worldId: Int) {
        player.client.write(ResetInteractionMode(worldId))
    }
}
