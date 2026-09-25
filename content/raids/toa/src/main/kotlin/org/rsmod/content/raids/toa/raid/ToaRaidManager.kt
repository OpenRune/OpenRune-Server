package org.rsmod.content.raids.toa.raid

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region

/**
 * Registry and state changes for running raids. Like [ToaPartyManager], it is
 * plain `Player` code with no `ProtectedAccess`, so it can be called from
 * logout hooks and for other players. Everything that needs the
 * RegionRepository or suspends (fades, dialogs) lives in [ToaRaidScript].
 */
object ToaRaidManager {

    /** Transient (cleared on logout): the raid this player belongs to. */
    private val CURRENT_RAID = AttributeKey<ToaRaid>()

    var Player.currentRaid: ToaRaid?
        get() = attr[CURRENT_RAID]
        private set(value) {
            if (value != null) attr[CURRENT_RAID] = value else attr.remove(CURRENT_RAID)
        }

    /** Raids by the lobby party that started them, so followers find the raid. */
    private val raids = HashMap<ToaLobbyParty, ToaRaid>()

    /**
     * Source of toa_mycontroller values. Vanilla sends a large, unique id per
     * room instance (17254814, then 17254938 for the next room), and -1 when
     * you leave. Only uniqueness and "> 0 means in a raid" matter to us.
     */
    private var nextControllerId = 1

    /**
     * Releases an ended raid's room regions. Set by [ToaRaidScript], which
     * owns the injected RegionRepository; this object can't inject it.
     */
    var releaseRooms: ((ToaRaid) -> Unit)? = null

    // ---- Client vars ----

    /** Varp, saved by OpenRune (every varp is Perm by default). Doubles as our
     * "logged out inside the raid" flag at login, see [ToaRaidScript]. */
    var Player.toaController by intVarp("varp.toa_mycontroller")
    private var Player.hudPartySlot by intVarBit("varbit.toa_client_partyslot")
    private var Player.hudRaidLevel by intVarBit("varbit.toa_client_raid_level")
    private var Player.kickedFromRaid by intVarBit("varbit.toa_kicked_from_raid")

    const val CONTROLLER_NONE = -1

    /** toa_client_p0..p7: 0 empty slot, 1..27 health, 30 dead, 31 not in this room. */
    private const val HUD_STATE_EMPTY = 0
    private const val HUD_STATE_FULL_HEALTH = 27
    private const val HUD_STATE_ELSEWHERE = 31

    private const val SCRIPT_HUD_STATUS_NAMES = 6585 // toa_hud_statusnames

    // ---- Queries ----

    fun raidFor(party: ToaLobbyParty): ToaRaid? = raids[party]

    fun isInRaid(player: Player): Boolean = player.currentRaid != null

    // ---- Lifecycle ----

    /**
     * Registers a new raid for [party]. The caller has already built its first
     * room (so a full region pool fails before anything changes).
     */
    fun start(party: ToaLobbyParty, mainHall: Region): ToaRaid {
        val raid = ToaRaid(party, party.settings.copy())
        raid.rooms[ToaRoom.MAIN_HALL] = mainHall
        raid.controllerIds[ToaRoom.MAIN_HALL] = nextControllerId++
        raids[party] = raid
        return raid
    }

    /**
     * Marks [player] as inside [raid], in [room]. Call before teleporting so
     * the lobby-area exit sees them as in the raid.
     */
    fun markEntered(player: Player, raid: ToaRaid, room: ToaRoom) {
        player.currentRaid = raid
        raid.playerRooms[player] = room
        player.toaController = raid.controllerIds.getValue(room)
    }

    /**
     * Removes [player] from their raid (abandon or logout). Port of
     * Offline_Scape TOARaidParty.leave: they also leave the lobby party, which
     * vanilla confirms (toa_client_partystatus goes 1 -> 0 on abandon).
     *
     * @param logout when `true`, toa_mycontroller is left set so the login
     *   hook knows to put them back outside the raid.
     */
    fun leave(player: Player, logout: Boolean) {
        val raid = player.currentRaid ?: return
        player.currentRaid = null
        raid.playerRooms.remove(player)
        raid.players.remove(player)

        if (!logout) {
            resetClientVars(player)
        }

        val party = raid.lobbyParty
        if (ToaPartyManager.leaveParty(player)) {
            ToaPartyManager.refreshViewers(party, exclude = player)
        }

        // Everyone else's HUD shifts up a slot, like generateHudPlayerList().
        refreshHud(raid)
        if (raid.players.isEmpty()) end(raid)
    }

    /**
     * Called by [ToaPartyManager.leaveParty] for every party exit. Handles a
     * member who never entered (still in the lobby) leaving the party: kicked,
     * walked out of the lobby, or logged out. They lose their raid slot, and
     * the raid ends if nobody is left. Players inside go through [leave].
     */
    fun onLeftParty(player: Player, party: ToaLobbyParty) {
        val raid = raids[party] ?: return
        if (raid.isInside(player)) return
        if (!raid.players.remove(player)) return
        refreshHud(raid)
        if (raid.players.isEmpty()) end(raid)
    }

    private fun end(raid: ToaRaid) {
        raids.remove(raid.lobbyParty)
        releaseRooms?.invoke(raid)
    }

    /**
     * Clears a player's raid vars. Also used at login, since varbits in
     * toa_temp_transmit_* are saved like every other var.
     */
    fun resetClientVars(player: Player) {
        player.toaController = CONTROLLER_NONE
        player.hudPartySlot = 0
        player.hudRaidLevel = 0
        player.kickedFromRaid = 0
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            VarPlayerIntMapSetter.set(player, "varbit.toa_client_p$i", HUD_STATE_EMPTY)
        }
    }

    // ---- HUD (481) ----

    /**
     * Sends [viewer] the whole HUD state: their slot, the raid level, every
     * slot's health orb and the names. Offline_Scape: sendHud() +
     * refreshHudStates() + refreshHudPlayers().
     *
     * Capture (solo, entry): toa_client_partyslot=1, toa_client_p0=27,
     * toa_client_raid_level=25, then toa_hud_statusnames("OnlyPans", "", ...).
     */
    fun sendHud(viewer: Player, raid: ToaRaid) {
        viewer.hudPartySlot = raid.players.indexOf(viewer) + 1
        viewer.hudRaidLevel = raid.settings.raidLevel
        sendHudStates(viewer, raid)

        val names = Array(ToaLobbyParty.MAX_PARTY_MEMBERS) { i ->
            raid.players.getOrNull(i)?.displayName ?: ""
        }
        viewer.runClientScript(SCRIPT_HUD_STATUS_NAMES, *names)
    }

    /** Re-sends the HUD to everyone inside [raid]. */
    fun refreshHud(raid: ToaRaid) {
        for (player in raid.playerRooms.keys) {
            sendHud(player, raid)
        }
    }

    private fun sendHudStates(viewer: Player, raid: ToaRaid) {
        val viewerRoom = raid.playerRooms[viewer]
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            val member = raid.players.getOrNull(i)
            val state = when {
                member == null -> HUD_STATE_EMPTY
                raid.playerRooms[member] != viewerRoom -> HUD_STATE_ELSEWHERE
                else -> healthState(member)
            }
            VarPlayerIntMapSetter.set(viewer, "varbit.toa_client_p$i", state)
        }
    }

    /**
     * Health orb value. Offline_Scape uses 1 + min(28, floor(hp/max * 28)),
     * which gives 29 at full health, but the vanilla capture shows 27 at full,
     * so this is scaled to 1..27.
     * TODO: re-check against a capture of a damaged player; refresh on HP change.
     */
    private fun healthState(player: Player): Int {
        val max = player.baseHitpointsLvl.coerceAtLeast(1)
        val scaled = 1 + (player.hitpoints * (HUD_STATE_FULL_HEALTH - 1)) / max
        return scaled.coerceIn(1, HUD_STATE_FULL_HEALTH)
    }
}
