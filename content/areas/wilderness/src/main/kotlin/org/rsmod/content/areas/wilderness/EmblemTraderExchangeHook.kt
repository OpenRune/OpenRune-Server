package org.rsmod.content.areas.wilderness

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

/** The Edgeville trader can exchange artefacts supplied by a separate content module. */
public interface EmblemTraderExchangeHook {
    public fun hasArtefacts(player: Player): Boolean
    public suspend fun exchange(access: ProtectedAccess)
}
