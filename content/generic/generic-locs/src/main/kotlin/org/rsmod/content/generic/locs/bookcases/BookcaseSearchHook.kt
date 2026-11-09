package org.rsmod.content.generic.locs.bookcases

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

public interface BookcaseSearchHook {
    public fun claims(player: Player, bookcase: BoundLocInfo): Boolean

    public suspend fun ProtectedAccess.search(bookcase: BoundLocInfo)
}
