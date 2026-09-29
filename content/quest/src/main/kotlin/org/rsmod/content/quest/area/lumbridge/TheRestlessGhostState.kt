package org.rsmod.content.quest.area.lumbridge

import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

internal var Player.restlessGhostRisingTile by intVarp("varp.restless_ghost_rising_tile")
internal var Player.restlessGhostReturnTile by intVarp("varp.restless_ghost_return_tile")

internal fun restoreRestlessGhostLogin(player: Player) {
    if (player.restlessGhostReturnTile == 0) return
    player.coords = ReleaseScene.ReturnTile
    player.restlessGhostReturnTile = 0
}
