package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.annotations.InternalApi
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.raid.ChallengeResult
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRaidDeps
import org.rsmod.content.raids.toa.raid.ToaRaidManager
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.toaRestore
import org.rsmod.content.raids.toa.raid.wipeAftermath
import org.rsmod.game.entity.Player
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/** Offline_Scape EncounterStage. */
enum class ToaStage {
    NOT_STARTED,
    STARTED,
    COMPLETED,
}

/** Where and how a player lands when they travel into a room (instance coords). */
data class Arrival(val coords: CoordGrid, val facing: Direction?)

/**
 * One built room of a raid. Port of Offline_Scape TOARaidArea, the base of every room.
 *
 * `open` rather than abstract: rooms without content yet (the puzzles and the reward room) use this
 * class as-is. Phase B gives each room its own subclass that overrides the hooks below.
 *
 * Coordinates: room data is in STATIC coords. [coords] converts static -> instance and
 * [staticCoords] converts back. The copy has no rotation and keeps levels, so both are exact.
 *
 * Tasks: [schedule] runs world-queue work that is dropped when the room is destroyed, completed or
 * reset ([stopTasks]). World queues can't be cancelled, so each task checks a generation number
 * instead.
 */
open class ToaEncounter(
    val raid: ToaRaid,
    val room: ToaRoom,
    val region: Region,
    val controllerId: Int,
) {
    protected val deps: ToaRaidDeps
        get() = raid.deps

    var stage: ToaStage = ToaStage.NOT_STARTED
        private set

    /** Set once the room's region has been released. */
    var destroyed: Boolean = false
        private set

    /** Map cycle the challenge started at. */
    var startCycle: Int = 0
        private set

    /** Carried over from the first Wardens room to the second (one challenge in two rooms). */
    var challengeName: String = room.challengeName ?: ""
        private set

    private var taskGeneration = 0

    /** Players in this room, in HUD order. */
    val players: List<Player>
        get() = raid.playersIn(this)

    /** Players in this room who are inside the challenge area. */
    val challengePlayers: List<Player>
        get() = players.filter(::inChallengeArea)

    // ---- Coordinates ----

    /** Static -> instance. Works on every level. */
    fun coords(static: CoordGrid): CoordGrid = region.normal[static]

    /** Instance -> static. [CoordGrid.NULL] for coords outside any instance. */
    fun staticCoords(coords: CoordGrid): CoordGrid = deps.regions.normalizeCoords(coords)

    fun inChallengeArea(player: Player): Boolean {
        val static = staticCoords(player.coords)
        if (static == CoordGrid.NULL) return false
        return room.inChallengeArea(static)
    }

    /**
     * Where a player travelling into this room lands. The default is the room's spawn tile
     * (Offline_Scape `getRandomizedSpawnTile`).
     */
    open fun arrival(): Arrival = Arrival(coords(room.randomSpawn(deps.random)), facing = null)

    // ---- Hooks (Offline_Scape constructed / enter / leave / onRoomStart / onRoomEnd / onRoomReset) ----

    /** The region exists and the room is [ToaRaid.current]. Spawn locs and NPCs here. */
    open fun onBuilt() {}

    /** [player] has been placed in this room; called just before their teleport lands. */
    open fun onEnter(player: Player) {}

    /** [player] moved to another room or left the raid. */
    open fun onLeave(player: Player) {}

    protected open fun onStart() {}

    protected open fun onComplete() {}

    protected open fun onReset() {}

    // ---- Challenge lifecycle ----

    /**
     * Offline_Scape `startRoom`, plus the "Challenge started" message it sent from the barrier and
     * teleport crystal. Does nothing if the challenge has already started.
     */
    fun start() {
        if (stage != ToaStage.NOT_STARTED) return
        stage = ToaStage.STARTED
        startCycle = deps.mapClock.cycle
        onStart()
        for (player in raid.players) {
            player.mes("Challenge started: $challengeName")
        }
    }

    /**
     * Continues a challenge that began in another room (the Wardens fight moving into its second
     * room): same start time and name, and already started.
     */
    internal fun continueChallenge(from: ToaEncounter) {
        stage = ToaStage.STARTED
        startCycle = from.startCycle
        challengeName = from.challengeName
    }

    /** Offline_Scape `completeRoom`: records the time, sends the messages, runs [onComplete]. */
    fun complete() {
        if (stage == ToaStage.COMPLETED) return
        stage = ToaStage.COMPLETED

        val now = deps.mapClock.cycle
        val ticks = now - startCycle
        raid.challengeResults += ChallengeResult(room, ticks)
        val duration = ToaRaid.formatTicks(ticks)
        val total = ToaRaid.formatTicks(raid.totalChallengeTicks())

        val isEnd = room == ToaRoom.WARDENS_SECOND_ROOM
        if (isEnd) {
            raid.finish(now)
        }

        for (player in players) {
            if (!isEnd) {
                player.mes(
                    "Challenge complete: $challengeName. Duration: <col=ef1020>$duration</col>. " +
                        "Total: <col=ef1020>$total</col>"
                )
            } else {
                sendRaidCompleteMessages(player, duration, total, now)
            }
        }

        recoverPlayers()
        stopTasks()
        onComplete()
        if (isEnd) {
            ToaRaidManager.refreshTimers(raid)
        }
    }

    private fun sendRaidCompleteMessages(player: Player, duration: String, total: String, now: Int) {
        val mode = raid.settings.mode
        val raidTime = ToaRaid.formatTicks(raid.elapsedTicks(now))
        player.mes("Challenge complete: $challengeName. Duration: <col=ef1020>$duration</col>")
        player.mes("Tombs of Amascut: $mode Mode challenge completion time: <col=ef1020>$total</col>")
        player.mes("Tombs of Amascut: $mode Mode total completion time: <col=ef1020>$raidTime</col>")
        // TODO: "Tombs of Amascut: Your Points:" once points exist; kill count.
        val limit = raid.timeLimitMinutes ?: return
        // Offline_Scape's copies of these two lines ended in a broken "</col".
        if (raid.failedTimeLimit) {
            player.mes("<col=FF0000>Your party failed to beat the overall target time of $limit:00</col>")
        } else {
            player.mes("<col=00FF00>Your party beat the overall target time of $limit:00!</col>")
        }
    }

    /**
     * Offline_Scape completeRoom's per-player part: ghosts come back to life, anyone outside the
     * challenge area is brought into it, and after a boss (not a puzzle) everyone is fully
     * restored.
     *
     * TODO: check the restore against a capture. Offline_Scape called `reset()` here, which
     * restores stats; confirm vanilla does that after a boss.
     */
    @OptIn(InternalApi::class)
    private fun recoverPlayers() {
        val challengeSpawn = room.challengeSpawn
        val restore = room.kind != ToaRoom.Kind.PUZZLE
        for (player in players) {
            raid.revive(player)
            if (restore) player.toaRestore()
            if (challengeSpawn != null && !inChallengeArea(player)) {
                val dest = coords(challengeSpawn)
                deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
            }
        }
        ToaRaidManager.refreshHudStates(raid)
    }

    /**
     * Offline_Scape checkRoomReset. Once everyone left in a running room is a ghost (nobody alive,
     * nobody still inside the challenge area), the party has wiped: it costs an attempt, the room
     * resets, and each player gets [wipeAftermath]. With no attempts left the raid fails instead.
     *
     * Called after each death and whenever someone leaves the raid.
     */
    @OptIn(InternalApi::class)
    fun checkRoomReset() {
        if (destroyed || stage != ToaStage.STARTED) return
        val inRoom = players
        // Offline_Scape counted a wipe even for an empty room; nobody is left to see it, so don't.
        if (inRoom.isEmpty()) return
        if (inRoom.any { inChallengeArea(it) || !raid.isGhost(it) }) return

        raid.teamDeaths++
        val retry = raid.canRetryAfter()
        val encounter = this
        for (player in inRoom) {
            // Forced, like a cutscene: must happen even if they have a dialog open.
            deps.launcher.launchLenient(player) { wipeAftermath(encounter, retry) }
        }
        reset()
    }

    /** Offline_Scape `resetRoom`. TODO: honey locusts (4-6, unless On a Diet or the raid failed). */
    fun reset() {
        stage = ToaStage.NOT_STARTED
        stopTasks()
        onReset()
    }

    /**
     * `::toacomplete`. Starts the challenge if needed and completes it. Rooms whose challenge
     * doesn't end with [complete] (the first Wardens room) override this.
     */
    open fun debugComplete() {
        start()
        complete()
    }

    // ---- Tasks ----

    /** Runs [action] after [ticks], unless the room is destroyed, completed or reset first. */
    fun schedule(ticks: Int, action: () -> Unit) {
        val generation = taskGeneration
        deps.worldQueues.add(ticks) {
            if (!destroyed && generation == taskGeneration) {
                action()
            }
        }
    }

    /** Drops every task scheduled so far (Offline_Scape `stopRunningTasks`). */
    fun stopTasks() {
        taskGeneration++
    }

    /**
     * Releases the region. Unprotecting is enough: the repository reclaims empty, unprotected
     * regions (and clears their NPCs and locs) the next time it allocates one.
     */
    internal fun destroy() {
        if (destroyed) return
        destroyed = true
        stopTasks()
        deps.regionRepo.unprotect(region)
    }

    override fun toString(): String = "ToaEncounter(room=$room, controllerId=$controllerId, stage=$stage)"
}
