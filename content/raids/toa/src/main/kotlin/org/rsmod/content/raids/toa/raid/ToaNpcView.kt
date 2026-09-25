package org.rsmod.content.raids.toa.raid

import net.rsprot.protocol.api.NetworkService
import net.rsprot.protocol.game.outgoing.info.npcinfo.NpcInfo
import org.rsmod.game.entity.Player

/*
 * Extended NPC view inside the raid. Offline_Scape TOARaidArea.enter set
 * `setViewDistance(Player.SCENE_DIAMETER)` in every room and leave() put it back, so NPCs such
 * as the Crondis palm are visible from anywhere in the room instead of only within the default
 * 15 tiles.
 *
 * NPC visibility is decided per player by rsprot's NpcInfo. OpenRune keeps each player's NpcInfo
 * private in RspClient/RspCycle, but the same object is reachable through the injectable
 * NetworkService: infoProtocols.npcInfoProtocol[player.slotId] (NetworkScript.startSession
 * allocates it with player.slotId). No api/net change or reflection needed.
 *
 * Both setters check they run on rsprot's communication thread, which is the game thread that
 * runs content scripts. Don't call these from anything off-thread.
 */

/**
 * Offline_Scape Player.SCENE_DIAMETER. Inside one room (one 64x64 map square) this covers every
 * tile from any tile.
 */
private const val RAID_RENDER_DISTANCE = 104

/**
 * New NPCs are only discovered in zones this far from the player (rsprot KDoc on
 * setRenderDistance: raise the zone search radius too). 8 zones = 64 tiles: the far corner of a
 * room from any tile in it.
 */
private const val RAID_ZONE_SEARCH_RADIUS = 8

/** rsprot's NpcInfo.DEFAULT_ZONE_SEARCH_RADIUS. */
private const val DEFAULT_ZONE_SEARCH_RADIUS = 3

/**
 * `getOrNull`, not `[]`: the index getter throws when no NpcInfo is allocated (a player without
 * a network session).
 */
private fun NetworkService<Player>.npcInfo(player: Player): NpcInfo? =
    infoProtocols.npcInfoProtocol.getOrNull(player.slotId)

/** Called whenever a player is placed in a raid room (ToaRaidManager.moveTo). */
internal fun NetworkService<Player>.extendNpcView(player: Player) {
    val info = npcInfo(player) ?: return
    info.setRenderDistance(RAID_RENDER_DISTANCE)
    info.setZoneSearchRadius(RAID_ZONE_SEARCH_RADIUS)
}

/**
 * Called whenever a player leaves the raid, logout included (ToaRaidManager.leave).
 *
 * The zone radius must be put back by hand: NpcInfo objects are pooled per player slot, and
 * rsprot's onAlloc resets the render distance but not the zone search radius, so a raider
 * logging out with radius 8 would pass it to the next player given that slot.
 */
internal fun NetworkService<Player>.resetNpcView(player: Player) {
    val info = npcInfo(player) ?: return
    info.resetRenderDistance()
    info.setZoneSearchRadius(DEFAULT_ZONE_SEARCH_RADIUS)
}
