package org.rsmod.content.generic.locs.search

import dev.openrune.types.ObjectServerType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

public interface CrateSearchHook {
    public fun claims(player: Player, crate: BoundLocInfo): Boolean

    public suspend fun ProtectedAccess.search(crate: BoundLocInfo, type: ObjectServerType)
}
