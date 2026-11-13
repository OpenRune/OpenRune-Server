package org.rsmod.content.other.moonsets

import org.rsmod.api.player.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

internal fun PathingEntity.validityCheck(): () -> Boolean =
    when (this) {
        is Npc -> {
            val castUid = uid
            val check = { uid == castUid && hitpoints > 0 }
            check
        }
        is Player -> ::isValidTarget
    }
