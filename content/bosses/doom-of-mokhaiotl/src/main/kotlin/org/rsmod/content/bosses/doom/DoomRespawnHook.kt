package org.rsmod.content.bosses.doom

import jakarta.inject.Inject
import org.rsmod.api.death.PlayerRespawnHook
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.table.InstanceSettingsRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class DoomRespawnHook
@Inject
constructor(private val instances: InstanceManager, private val loot: DoomLoot) : PlayerRespawnHook {
    private val key by lazy { InstanceSettingsRow.getRow(DoomInstance.SETTINGS_ROW).key }

    override fun respawnCoords(player: Player): CoordGrid? {
        if (instances.sessionForPlayer(player)?.key != key) return null
        loot.reset(player)
        return DoomArena.LOBBY
    }
}
