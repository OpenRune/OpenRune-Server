package org.rsmod.content.raids.toa.raid

import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcInfo
import org.rsmod.game.entity.Player

private const val RAID_RENDER_DISTANCE = 104

private const val RAID_ZONE_SEARCH_RADIUS = 8

private const val DEFAULT_ZONE_SEARCH_RADIUS = 3

private fun NetworkService<Player>.npcInfo(player: Player): NpcInfo? =
    infoProtocols.npcInfoProtocol.getOrNull(player.slotId)

internal fun NetworkService<Player>.extendNpcView(player: Player) {
    val info = npcInfo(player) ?: return
    info.setRenderDistance(RAID_RENDER_DISTANCE)
    info.setZoneSearchRadius(RAID_ZONE_SEARCH_RADIUS)
}

internal fun NetworkService<Player>.resetNpcView(player: Player) {
    val info = npcInfo(player) ?: return
    info.resetRenderDistance()
    info.setZoneSearchRadius(DEFAULT_ZONE_SEARCH_RADIUS)
}
