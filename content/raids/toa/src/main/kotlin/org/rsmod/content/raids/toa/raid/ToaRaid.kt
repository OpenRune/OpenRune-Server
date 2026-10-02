package org.rsmod.content.raids.toa.raid

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfSubType
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenSub
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.party.ToaLobbyParty
import org.rsmod.content.raids.toa.party.ToaPartySettings
import org.rsmod.content.raids.toa.raid.encounter.MainHallEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaBossEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.WardensFirstEncounter
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle.CrondisPuzzleEncounter
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.ZebakEncounter
import org.rsmod.content.raids.toa.raid.supplies.ToaSupplies
import org.rsmod.game.entity.Player
import org.rsmod.game.region.Region

data class ChallengeResult(val room: ToaRoom, val ticks: Int)

class ToaRaid(val lobbyParty: ToaLobbyParty, val settings: ToaPartySettings, val deps: ToaRaidDeps) {

    val players: MutableList<Player> = lobbyParty.members.toMutableList()

    val partySize: Int = players.size

    private val locations = HashMap<Player, ToaEncounter>()

    private val encounters = ArrayList<ToaEncounter>()

    var ended = false
        internal set

    var current: ToaEncounter? = null
        private set

    var lastPath: ToaPath? = null

    val pathsCompleted: MutableList<ToaPath> = ArrayList()

    val pathLevels = IntArray(ToaPath.entries.size)

    var pathLevelsInitialised = false

    val challengeResults: MutableList<ChallengeResult> = ArrayList()

    var startCycle = -1
        private set

    var endCycle = -1
        private set

    var failedTimeLimit = false
        private set

    val timeLimitMinutes: Int? =
        when {
            isActive(ToaInvocationKey.WalkForIt) -> 40
            isActive(ToaInvocationKey.JogForIt) -> 35
            isActive(ToaInvocationKey.RunForIt) -> 30
            isActive(ToaInvocationKey.SprintForIt) -> 25
            else -> null
        }

    val permittedTeamDeaths: Int? =
        when {
            isActive(ToaInvocationKey.TryAgain) -> 10
            isActive(ToaInvocationKey.Persistence) -> 5
            isActive(ToaInvocationKey.SoftcoreRun) -> 3
            isActive(ToaInvocationKey.HardcoreRun) -> 1
            else -> null
        }

    var teamDeaths = 0
        internal set

    var totalDeaths = 0
        internal set

    private val ghosts = HashSet<Player>()

    private val dying = HashSet<Player>()

    val points = ToaPoints(players)

    val supplies = ToaSupplies()

    val damageMultiplier: Double
        get() = (1.0 + settings.raidLevel * DAMAGE_PER_RAID_LEVEL).coerceAtMost(MAX_DAMAGE_MULTIPLIER)

    val timerStarted: Boolean
        get() = startCycle >= 0

    val finished: Boolean
        get() = endCycle >= 0

    fun isActive(key: ToaInvocationKey): Boolean = settings.isActive(key.invocation)

    fun isGhost(player: Player): Boolean = player in ghosts

    fun isDying(player: Player): Boolean = player in dying

    internal fun startDying(player: Player): Boolean = dying.add(player)

    internal fun stopDying(player: Player) {
        dying.remove(player)
    }

    fun canRetryAfter(extraWipes: Int = 0): Boolean {
        val permitted = permittedTeamDeaths ?: return true
        return teamDeaths + extraWipes < permitted
    }

    internal fun makeGhost(player: Player) {
        if (!ghosts.add(player)) return
        player.transmog = ServerCacheManager.getNpc(GHOST_NPC.asRSCM(RSCMType.NPC))
        player.rebuildAppearance()
        for ((interf, _) in GHOST_TABS) player.ifCloseOverlay(interf, deps.eventBus)
    }

    internal fun revive(player: Player) {
        if (!ghosts.remove(player)) return
        player.transmog = null
        player.rebuildAppearance()
        for ((interf, target) in GHOST_TABS) {
            player.ifOpenSub(interf, target, IfSubType.Overlay, deps.eventBus)
        }
    }

    fun isLeader(player: Player): Boolean = lobbyParty.isLeader(player)

    val leaderName: String
        get() = lobbyParty.leader?.displayName ?: lobbyParty.leaderName

    fun isInside(player: Player): Boolean = player in locations

    fun encounterOf(player: Player): ToaEncounter? = locations[player]

    fun playersIn(encounter: ToaEncounter): List<Player> =
        players.filter { locations[it] === encounter }

    fun stragglers(): List<Player> {
        val current = current ?: return emptyList()
        return players.filter { locations[it] !== current }
    }

    fun advanceTo(room: ToaRoom): ToaEncounter? {
        val region = deps.regionRepo.add(room.template) ?: return null

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
            room == ToaRoom.CRONDIS_PUZZLE -> CrondisPuzzleEncounter(this, room, region, controllerId)
            room == ToaRoom.CRONDIS_BOSS -> ZebakEncounter(this, room, region, controllerId)
            room.kind == ToaRoom.Kind.BOSS -> ToaBossEncounter(this, room, region, controllerId)

            else -> ToaEncounter(this, room, region, controllerId)
        }

    internal fun place(player: Player, encounter: ToaEncounter) {
        val previous = locations.put(player, encounter)
        if (previous === encounter) return
        previous?.let {
            it.leave(player)
            releaseIfIdle(it)
        }
        encounter.enter(player)
    }

    internal fun remove(player: Player) {
        val previous = locations.remove(player) ?: return
        previous.leave(player)
        releaseIfIdle(previous)
    }

    private fun releaseIfIdle(encounter: ToaEncounter) {
        if (encounter === current || locations.containsValue(encounter)) return
        encounter.destroy()
        encounters.remove(encounter)
    }

    internal fun destroyAll() {
        for (encounter in encounters) {
            encounter.destroy()
        }
        encounters.clear()
        locations.clear()
        current = null
    }

    internal fun startTimer(cycle: Int) {
        if (!timerStarted) startCycle = cycle
    }

    internal fun finish(cycle: Int) {
        if (finished) return
        endCycle = cycle
        val limit = timeLimitMinutes ?: return
        failedTimeLimit = elapsedTicks(cycle) > limit * TICKS_PER_MINUTE
    }

    fun elapsedTicks(cycle: Int): Int =
        when {
            !timerStarted -> 0
            finished -> endCycle - startCycle
            else -> cycle - startCycle
        }

    fun totalChallengeTicks(): Int = challengeResults.sumOf { it.ticks }

    companion object {
        private const val GHOST_NPC = "npc.toa_player_ghost"

        private val GHOST_TABS =
            listOf(
                "interface.inventory" to "component.toplevel_osrs_stretch:side3",
                "interface.wornitems" to "component.toplevel_osrs_stretch:side4",
            )

        private const val DAMAGE_PER_RAID_LEVEL = 0.004
        private const val MAX_DAMAGE_MULTIPLIER = 2.5

        const val TICKS_PER_MINUTE = 100

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
