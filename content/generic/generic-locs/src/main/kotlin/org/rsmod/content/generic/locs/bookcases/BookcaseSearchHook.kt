package org.rsmod.content.generic.locs.bookcases

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo

/**
 * Lets content replace the generic bookcase search for particular bookcases. The first bound hook
 * that [claims] the searched bookcase runs [search] in place of the generic messages.
 */
public interface BookcaseSearchHook {
    public fun claims(player: Player, bookcase: BoundLocInfo): Boolean

    public suspend fun ProtectedAccess.search(bookcase: BoundLocInfo)
}
