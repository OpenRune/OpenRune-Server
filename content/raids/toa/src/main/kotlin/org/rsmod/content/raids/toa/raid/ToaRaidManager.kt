package org.rsmod.content.raids.toa.raid

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartyManager
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Player

/**
 * Registry and state changes for running raids. Like [ToaPartyManager], it is plain `Player` code
 * with no `ProtectedAccess`, so it can be called from logout hooks and for other players.
 * Everything that suspends (fades, dialogs) lives in ToaTravel.kt and [ToaRaidScript].
 */
object ToaRaidManager {

    /** Transient (cleared on logout): the raid this player is inside. */
    private val CURRENT_RAID = AttributeKey<ToaRaid>()

    var Player.currentRaid: ToaRaid?
        get() = attr[CURRENT_RAID]
        private set(value) {
            if (value != null) attr[CURRENT_RAID] = value else attr.remove(CURRENT_RAID)
        }

    /** Raids by the lobby party that started them, so followers find the raid. */
    private val raids = HashMap<ToaLobbyParty, ToaRaid>()

    /**
     * Source of toa_mycontroller values. Vanilla sends a large, unique id per room instance
     * (17254814, then 17254938 for the next room), and -1 when you leave. Only uniqueness and
     * "> 0 means in a raid" matter to us.
     */
    private var nextControllerId = 1

    internal fun nextControllerId(): Int = nextControllerId++

    // ---- Client vars ----

    /** Varp, saved by OpenRune (every varp is Perm by default). Doubles as our "logged out
     * inside the raid" flag at login, see [ToaRaidScript]. */
    var Player.toaController by intVarp("varp.toa_mycontroller")
    private var Player.hudPartySlot by intVarBit("varbit.toa_client_partyslot")
    private var Player.hudRaidLevel by intVarBit("varbit.toa_client_raid_level")
    private var Player.hudCurrentPath by intVarBit("varbit.toa_client_current_path")

    /** Capture: 1 on entering a path room, 0 on leaving the raid. Name aside, it tracks
     * "in a room other than the nexus". */
    private var Player.kickedFromRaid by intVarBit("varbit.toa_kicked_from_raid")

    const val CONTROLLER_NONE = -1

    /** toa_client_p0..p7: 0 empty slot, 1..27 health, 30 dead, 31 not in this room. */
    private const val HUD_STATE_EMPTY = 0
    private const val HUD_STATE_FULL_HEALTH = 27
    private const val HUD_STATE_DEAD = 30
    private const val HUD_STATE_ELSEWHERE = 31

    private const val HUD_REFRESH_TICKS = 1

    private const val SCRIPT_HUD_STATUS_NAMES = 6585 // toa_hud_statusnames
    private const val SCRIPT_SPEEDRUN_TIME_UPDATE = 6580 // toa_speedrun_time_update

    // ---- Queries ----

    fun raidFor(party: ToaLobbyParty): ToaRaid? = raids[party]

    fun isInRaid(player: Player): Boolean = player.currentRaid != null

    // ---- Lifecycle ----

    /**
     * Creates a raid for [party] and builds its nexus. Nothing is registered if the region pool
     * is full, so the caller can just report the failure.
     */
    fun start(party: ToaLobbyParty, deps: ToaRaidDeps): ToaRaid? {
        val raid = ToaRaid(party, party.settings.copy(), deps)
        raid.advanceTo(ToaRoom.MAIN_HALL) ?: return null
        raids[party] = raid
        scheduleHudRefresh(raid)
        return raid
    }

    /**
     * Refreshes every player's health orbs once a tick for the raid's lifetime. The hit events
     * only cover damage (healing publishes nothing), and a varbit is only sent when its value
     * changes, so a steady refresh costs almost nothing and catches both. A world queue re-armed
     * each tick; it stops by itself when the raid ends.
     */
    private fun scheduleHudRefresh(raid: ToaRaid) {
        raid.deps.worldQueues.add(HUD_REFRESH_TICKS) {
            if (raid.ended) return@add
            refreshHudStates(raid)
            scheduleHudRefresh(raid)
        }
    }

    /**
     * Puts [player] in [encounter] and sends the vars that go with it. Call just before the
     * teleport (see ToaTravel.travel).
     *
     * The first time a path room is entered, the raid timer starts (capture: "The timer has
     * started!" at the path teleport).
     */
    fun moveTo(player: Player, encounter: ToaEncounter) {
        val raid = encounter.raid
        val room = encounter.room
        val firstEntry = !raid.isInside(player)

        player.currentRaid = raid
        raid.place(player, encounter)
        player.toaController = encounter.controllerId
        player.kickedFromRaid = if (room.kind == ToaRoom.Kind.MAIN_HALL) 0 else 1
        player.hudCurrentPath = room.hudPath
        if (firstEntry) {
            ToaPartyManager.setPartyStatus(player, ToaPartyManager.PARTY_STATUS_IN_PARTY)
        }

        if (room.kind != ToaRoom.Kind.MAIN_HALL && !raid.timerStarted) {
            raid.startTimer(raid.deps.mapClock.cycle)
            raid.timeLimitMinutes?.let { limit ->
                for (member in raid.players) {
                    member.mes("Overall time to beat: <col=ef1020>$limit:00</col>. The timer has started!")
                }
            }
            refreshTimers(raid)
        }

        refreshHudStates(raid)
    }

    /**
     * Removes [player] from their raid (abandon or logout). Port of Offline_Scape
     * TOARaidParty.leave: they also leave the lobby party, which vanilla confirms
     * (toa_client_partystatus goes 1 -> 0 on abandon).
     *
     * @param logout when `true`, toa_mycontroller is left set so the login hook knows to put
     *   them back outside the raid.
     */
    fun leave(player: Player, logout: Boolean) {
        val raid = player.currentRaid ?: return
        val room = raid.encounterOf(player)

        // Offline_Scape TOARaidArea.onLogout: logging out inside a running challenge counts as a
        // death. (Offline_Scape also let you rejoin after logging back in; we don't yet.)
        if (logout && room != null && room.stage == ToaStage.STARTED && room.inChallengeArea(player)) {
            raid.totalDeaths++
            for (other in room.players) {
                if (other !== player) {
                    other.mes(
                        "<col=ff0000>${player.displayName}</col> has logged out. " +
                            "Total deaths: <col=ff0000>${raid.totalDeaths}</col>."
                    )
                }
            }
        }

        raid.revive(player)
        raid.stopDying(player)
        player.currentRaid = null
        raid.remove(player)
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
        // Offline_Scape TOARaidParty.leave: if only ghosts are left behind, the room resets.
        room?.checkRoomReset()
        if (raid.players.isEmpty()) end(raid)
    }

    /**
     * Called by [ToaPartyManager.leaveParty] for every party exit. Handles a member who never
     * entered (still in the lobby) leaving the party: kicked, walked out of the lobby, or logged
     * out. They lose their raid slot, and the raid ends if nobody is left. Players inside go
     * through [leave].
     */
    fun onLeftParty(player: Player, party: ToaLobbyParty) {
        val raid = raids[party] ?: return
        if (raid.isInside(player)) return
        if (!raid.players.remove(player)) return
        refreshHud(raid)
        if (raid.players.isEmpty()) end(raid)
    }

    private fun end(raid: ToaRaid) {
        raid.ended = true
        raids.remove(raid.lobbyParty)
        raid.destroyAll()
    }

    /**
     * Clears a player's raid vars. Also used at login, since every var is saved.
     */
    fun resetClientVars(player: Player) {
        player.toaController = CONTROLLER_NONE
        player.hudPartySlot = 0
        player.hudRaidLevel = 0
        player.hudCurrentPath = 0
        player.kickedFromRaid = 0
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            VarPlayerIntMapSetter.set(player, "varbit.toa_client_p$i", HUD_STATE_EMPTY)
        }
        for (path in ToaPath.entries) {
            VarPlayerIntMapSetter.set(player, path.levelVarbit, 0)
        }
    }

    // ---- HUD (481) ----

    /**
     * Sends [viewer] the whole HUD: their slot, the raid level, every slot's orb, path levels,
     * names and the timer. Offline_Scape sendHud() + refreshHudStates() + refreshHudPlayers() +
     * refreshPathLevel() + refreshTimer().
     *
     * Capture (solo, entry): toa_client_partyslot=1, toa_client_p0=27, toa_client_raid_level=25,
     * then toa_hud_statusnames("OnlyPans", "", ...).
     */
    fun sendHud(viewer: Player, raid: ToaRaid) {
        viewer.hudPartySlot = raid.players.indexOf(viewer) + 1
        viewer.hudRaidLevel = raid.settings.raidLevel
        viewer.hudCurrentPath = raid.encounterOf(viewer)?.room?.hudPath ?: 0
        sendHudStates(viewer, raid)
        for (path in ToaPath.entries) {
            VarPlayerIntMapSetter.set(viewer, path.levelVarbit, raid.pathLevels[path.ordinal])
        }

        val names = Array(ToaLobbyParty.MAX_PARTY_MEMBERS) { i ->
            raid.players.getOrNull(i)?.displayName ?: ""
        }
        viewer.runClientScript(SCRIPT_HUD_STATUS_NAMES, *names)
        sendTimer(viewer, raid)
    }

    /** Re-sends the whole HUD to everyone inside [raid]. */
    fun refreshHud(raid: ToaRaid) {
        for (player in raid.players) {
            if (raid.isInside(player)) sendHud(player, raid)
        }
    }

    /** Re-sends only the health/location orbs to everyone inside [raid]. */
    fun refreshHudStates(raid: ToaRaid) {
        for (player in raid.players) {
            if (raid.isInside(player)) sendHudStates(player, raid)
        }
    }

    /** Re-sends the timer to everyone inside [raid] (started or finished). */
    fun refreshTimers(raid: ToaRaid) {
        for (player in raid.players) {
            if (raid.isInside(player)) sendTimer(player, raid)
        }
    }

    private fun sendHudStates(viewer: Player, raid: ToaRaid) {
        val viewerRoom = raid.encounterOf(viewer)
        for (i in 0 until ToaLobbyParty.MAX_PARTY_MEMBERS) {
            val member = raid.players.getOrNull(i)
            val state =
                when {
                    member == null -> HUD_STATE_EMPTY
                    raid.encounterOf(member) !== viewerRoom -> HUD_STATE_ELSEWHERE
                    raid.isGhost(member) -> HUD_STATE_DEAD
                    else -> healthState(member)
                }
            VarPlayerIntMapSetter.set(viewer, "varbit.toa_client_p$i", state)
        }
    }

    /**
     * toa_speedrun_time_update(ticks, frozen). The client shows `ticks` and counts up from it
     * unless `frozen`. Capture: (0, frozen) on entry, before any path. Offline_Scape sent
     * (elapsed, running) and (total, frozen) once finished. The boolean is sent as an int.
     */
    private fun sendTimer(viewer: Player, raid: ToaRaid) {
        val ticks = raid.elapsedTicks(raid.deps.mapClock.cycle)
        val frozen = !raid.timerStarted || raid.finished
        viewer.runClientScript(SCRIPT_SPEEDRUN_TIME_UPDATE, ticks, if (frozen) 1 else 0)
    }

    /**
     * Health orb value. Offline_Scape uses 1 + min(28, floor(hp/max * 28)), which gives 29 at
     * full health, but the vanilla capture shows 27 at full, so this is scaled to 1..27.
     * TODO: re-check the 1..27 scale against a capture of a damaged player.
     */
    private fun healthState(player: Player): Int {
        val max = player.baseHitpointsLvl.coerceAtLeast(1)
        val scaled = 1 + (player.hitpoints * (HUD_STATE_FULL_HEALTH - 1)) / max
        return scaled.coerceIn(1, HUD_STATE_FULL_HEALTH)
    }
}
