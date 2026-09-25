package org.rsmod.content.raids.toa.raid

import org.rsmod.content.raids.toa.party.ToaInvocation
import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartySettings
import org.rsmod.content.raids.toa.raid.encounter.MainHallEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.WardensFirstEncounter
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region

/** One room's finished challenge, for the "Total:" time and the scoreboard later. */
data class ChallengeResult(val room: ToaRoom, val ticks: Int)

/**
 * One running raid. Port of Offline_Scape TOARaidParty.
 *
 * **One current room.** Like Offline_Scape, the raid has a single [current] room: the one the
 * party is heading into. Moving on builds a fresh instance and makes it current. A room that is
 * no longer current is torn down as soon as the last player leaves it. So the nexus is rebuilt
 * every time you return to it (its doors then show which paths are done), every room entry gets a
 * new `toa_mycontroller` id as in the capture, and a raid never holds more than a couple of
 * regions.
 *
 * @property lobbyParty the party that started it. Members stay in it for the whole raid.
 * @property settings the invocations at the moment the leader entered.
 */
class ToaRaid(val lobbyParty: ToaLobbyParty, val settings: ToaPartySettings, val deps: ToaRaidDeps) {

    /**
     * Everyone who was in the party when the leader entered, minus leavers, in lobby order. The
     * index is the HUD slot (Offline_Scape `generateHudPlayerList`).
     */
    val players: MutableList<Player> = lobbyParty.members.toMutableList()

    /** Which room each player inside is in. Players still in the lobby have no entry. */
    private val locations = HashMap<Player, ToaEncounter>()

    /** Every built, not yet destroyed room: [current] plus old ones that still hold stragglers. */
    private val encounters = ArrayList<ToaEncounter>()

    /** The room the party is heading into. */
    var current: ToaEncounter? = null
        private set

    /** The last path the leader chose. Decides where you arrive back in the nexus. */
    var lastPath: ToaPath? = null

    val pathsCompleted: MutableList<ToaPath> = ArrayList()

    /** Boss level per path (`ToaPath.ordinal`), shown on the HUD. */
    val pathLevels = IntArray(ToaPath.entries.size)

    /** Whether the one-off Pathseeker/finder/master levels have been added. */
    var pathLevelsInitialised = false

    val challengeResults: MutableList<ChallengeResult> = ArrayList()

    /** Map cycle the raid timer started at (first path entered), or -1. */
    var startCycle = -1
        private set

    /** Map cycle the raid finished at (second Wardens room beaten), or -1. */
    var endCycle = -1
        private set

    var failedTimeLimit = false
        private set

    /** Offline_Scape TOARaidParty: the time-limit invocations. `null` without one. */
    val timeLimitMinutes: Int? =
        when {
            isActive("Walk for It") -> 40
            isActive("Jog for It") -> 35
            isActive("Run for It") -> 30
            isActive("Sprint for It") -> 25
            else -> null
        }

    val timerStarted: Boolean
        get() = startCycle >= 0

    val finished: Boolean
        get() = endCycle >= 0

    /** Whether the invocation called [name] (cache struct param 1160) is on for this raid. */
    fun isActive(name: String): Boolean {
        val invocation = ToaInvocation.ALL.firstOrNull { it.name == name } ?: return false
        return settings.isActive(invocation)
    }

    // ---- Party ----

    fun isLeader(player: Player): Boolean = lobbyParty.isLeader(player)

    val leaderName: String
        get() = lobbyParty.leader?.displayName ?: lobbyParty.leaderName

    // ---- Where everyone is ----

    fun isInside(player: Player): Boolean = player in locations

    fun encounterOf(player: Player): ToaEncounter? = locations[player]

    /** Players in [encounter], in HUD order. */
    fun playersIn(encounter: ToaEncounter): List<Player> =
        players.filter { locations[it] === encounter }

    /**
     * Players who aren't in [current]: still in the lobby or in an older room. Offline_Scape
     * `needsAbandonRequest` asks the leader about them before moving on.
     */
    fun stragglers(): List<Player> {
        val current = current ?: return emptyList()
        return players.filter { locations[it] !== current }
    }

    // ---- Rooms ----

    /**
     * Builds [room] as a new instance and makes it [current]. The previous current room is torn
     * down right away if nobody is in it.
     *
     * @return `null` when the region pool is full; nothing has changed then.
     */
    fun advanceTo(room: ToaRoom): ToaEncounter? {
        val region = deps.regionRepo.add(room.template) ?: return null
        // Protected so the repository doesn't reclaim it while it is briefly empty, e.g. before
        // the first player has arrived. Unprotected again in ToaEncounter.destroy().
        deps.regionRepo.protect(region)

        val encounter = createEncounter(room, region, ToaRaidManager.nextControllerId())
        encounters += encounter
        val previous = current
        current = encounter
        encounter.onBuilt()
        previous?.let(::releaseIfIdle)
        return encounter
    }

    private fun createEncounter(room: ToaRoom, region: Region, controllerId: Int): ToaEncounter =
        when {
            room == ToaRoom.MAIN_HALL -> MainHallEncounter(this, room, region, controllerId)
            room == ToaRoom.WARDENS_FIRST_ROOM -> WardensFirstEncounter(this, room, region, controllerId)
            room == ToaRoom.WARDENS_SECOND_ROOM -> WardensSecondEncounter(this, room, region, controllerId)
            room.kind == ToaRoom.Kind.BOSS -> ToaBossEncounter(this, room, region, controllerId)
            // Puzzles and the reward room: plain rooms until Phase B gives them content.
            else -> ToaEncounter(this, room, region, controllerId)
        }

    /** Moves [player] into [encounter]. Use [ToaRaidManager.moveTo], which also sends the vars. */
    internal fun place(player: Player, encounter: ToaEncounter) {
        val previous = locations.put(player, encounter)
        if (previous === encounter) return
        previous?.let {
            it.onLeave(player)
            releaseIfIdle(it)
        }
        encounter.onEnter(player)
    }

    /** Takes [player] out of whatever room they are in. */
    internal fun remove(player: Player) {
        val previous = locations.remove(player) ?: return
        previous.onLeave(player)
        releaseIfIdle(previous)
    }

    private fun releaseIfIdle(encounter: ToaEncounter) {
        if (encounter === current || locations.containsValue(encounter)) return
        encounter.destroy()
        encounters.remove(encounter)
    }

    /** Tears down every room. Called once when the raid ends. */
    internal fun destroyAll() {
        for (encounter in encounters) {
            encounter.destroy()
        }
        encounters.clear()
        locations.clear()
        current = null
    }

    // ---- Timer ----

    internal fun startTimer(cycle: Int) {
        if (!timerStarted) startCycle = cycle
    }

    /** Offline_Scape `setCompletion`: stops the clock and checks the time limit. */
    internal fun finish(cycle: Int) {
        if (finished) return
        endCycle = cycle
        val limit = timeLimitMinutes ?: return
        failedTimeLimit = elapsedTicks(cycle) > limit * TICKS_PER_MINUTE
    }

    /** Raid timer value at [cycle]: 0 before the first path, frozen once finished. */
    fun elapsedTicks(cycle: Int): Int =
        when {
            !timerStarted -> 0
            finished -> endCycle - startCycle
            else -> cycle - startCycle
        }

    fun totalChallengeTicks(): Int = challengeResults.sumOf { it.ticks }

    companion object {
        /** 100 ticks of 600 ms. */
        const val TICKS_PER_MINUTE = 100

        /**
         * "m:ss", or "h:mm:ss" past an hour. Offline_Scape's `formatTime` printed "1:" for a whole
         * minute; this always prints the seconds.
         */
        fun formatTicks(ticks: Int): String {
            val totalSeconds = ticks.coerceAtLeast(0) * 3 / 5
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds / 60) % 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%d:%02d".format(minutes, seconds)
            }
        }
    }
}
