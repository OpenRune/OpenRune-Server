package org.rsmod.content.raids.toa.raid

import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartySettings
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region
import java.util.EnumMap

/**
 * One running raid. Port of Offline_Scape's TOARaidParty (the parts needed
 * for entering and leaving; paths, deaths, timers and rewards come later).
 *
 * @property lobbyParty the party that started it. Members stay in that party
 *   for the whole raid (vanilla keeps toa_client_partystatus at 1).
 * @property settings a snapshot of the invocations/raid level at the moment
 *   the leader entered, so later lobby edits can't change a running raid.
 */
class ToaRaid(val lobbyParty: ToaLobbyParty, val settings: ToaPartySettings) {

    /**
     * The raid's players, in lobby-party order: everyone who was in the party
     * when the leader entered, minus anyone who has since left. Their index is
     * their HUD slot, and the list is rebuilt as people leave, exactly like
     * Offline_Scape's `players` / `generateHudPlayerList()`.
     */
    val players: MutableList<Player> = lobbyParty.members.toMutableList()

    /** Built room instances. Protected in the RegionRepository until [ToaRaidManager.end]. */
    val rooms: EnumMap<ToaRoom, Region> = EnumMap(ToaRoom::class.java)

    /** Value sent in toa_mycontroller for each room (vanilla: a new id per room). */
    val controllerIds: EnumMap<ToaRoom, Int> = EnumMap(ToaRoom::class.java)

    /** The room each player inside is in. Players still in the lobby have no entry. */
    val playerRooms: MutableMap<Player, ToaRoom> = HashMap()

    /** Whether [player] has entered and not left. */
    fun isInside(player: Player): Boolean = player in playerRooms
}
