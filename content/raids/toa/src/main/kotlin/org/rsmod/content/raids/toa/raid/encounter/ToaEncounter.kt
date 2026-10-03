package org.rsmod.content.raids.toa.raid.encounter

import kotlin.math.floor
import kotlin.math.min
import org.rsmod.annotations.InternalApi
import org.rsmod.api.death.NpcAttackValidateResult
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.mechanics.toxins.Toxin.cureAllToxins
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.content.raids.toa.raid.ChallengeResult
import org.rsmod.content.raids.toa.raid.ToaDamage
import org.rsmod.content.raids.toa.raid.ToaKillCount
import org.rsmod.content.raids.toa.raid.ToaPath
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRaidDeps
import org.rsmod.content.raids.toa.raid.ToaRaidManager
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.ToaStats
import org.rsmod.content.raids.toa.raid.personalContribution
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.content.raids.toa.raid.toaBossRestore
import org.rsmod.content.raids.toa.raid.wipeAftermath
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

enum class ToaStage {
    NOT_STARTED,
    STARTED,
    COMPLETED,
}

data class Arrival(val coords: CoordGrid, val facing: Direction?)

open class ToaEncounter(
    val raid: ToaRaid,
    val room: ToaRoom,
    val session: InstanceSession,
    val controllerId: Int,
) {
    protected val deps: ToaRaidDeps
        get() = raid.deps

    val region: Region = checkNotNull(deps.instances.regionOf(session))

    var stage: ToaStage = ToaStage.NOT_STARTED
        private set

    var destroyed: Boolean = false
        private set

    var teamSize: Int = 1
        private set

    var startCycle: Int = 0
        private set

    var challengeName: String = room.challengeName ?: ""
        private set

    private var taskGeneration = 0

    val players: List<Player>
        get() = raid.playersIn(this)

    val challengePlayers: List<Player>
        get() = players.filter(::inChallengeArea)

    internal open val hpBar: ToaHpBar? = null

    internal open val combatantSpecs: List<ToaCombatant.Spec>
        get() = emptyList()

    private val combatants = LinkedHashMap<Npc, ToaCombatant>()

    private val scalingPartySize: Int
        get() = if (stage == ToaStage.NOT_STARTED) raid.players.size.coerceAtLeast(1) else teamSize

    internal val pathLevel: Int
        get() = room.path?.let { raid.pathLevels[it.ordinal] } ?: 0

    internal val pathTier: Int
        get() = min(MAX_PATH_TIER, pathLevel / PATH_LEVELS_PER_TIER)

    fun coords(static: CoordGrid): CoordGrid = region.normal[static]

    fun staticCoords(coords: CoordGrid): CoordGrid = deps.regions.normalizeCoords(coords)

    fun inChallengeArea(player: Player): Boolean {
        val static = staticCoords(player.coords)
        if (static == CoordGrid.NULL) return false
        return room.inChallengeArea(static)
    }

    internal fun targets(): List<Player> =
        players.filter { inChallengeArea(it) && !raid.isGhost(it) && !raid.isDying(it) }

    open fun arrival(): Arrival = Arrival(coords(room.randomSpawn(deps.random)), facing = null)

    open fun onBuilt() {}

    protected open fun onEnter(player: Player) {}

    protected open fun onLeave(player: Player) {}

    protected open fun onStart() {}

    protected open fun onTick(targets: List<Player>) {}

    protected open fun onComplete() {}

    protected open fun onReset() {}

    internal open fun validateAttack(player: Player, npc: Npc): NpcAttackValidateResult =
        NpcAttackValidateResult.Pass

    open fun honeyLocusts(): Int = deps.random.of(HONEY_LOCUSTS_MIN, HONEY_LOCUSTS_MAX)

    fun pointMultiplier(npc: Npc): Double = combatants[npc]?.pointMultiplier ?: 1.0

    open val roomPointsCap: Int
        get() = if (room.kind == ToaRoom.Kind.WARDENS) WARDENS_POINTS_CAP else ROOM_POINTS_CAP

    private val awardsMvp: Boolean
        get() = room.kind == ToaRoom.Kind.PUZZLE || room.kind == ToaRoom.Kind.BOSS

    private val completionPoints: Int
        get() = if (room.kind == ToaRoom.Kind.PUZZLE) PUZZLE_POINTS[room.path] ?: 0 else 0

    fun start() {
        if (stage != ToaStage.NOT_STARTED) return
        stage = ToaStage.STARTED
        startCycle = deps.mapClock.cycle
        teamSize = raid.players.size.coerceAtLeast(1)
        onStart()
        for (player in players) hpBar?.open(player)
        schedule(1) { tick() }
        for (player in raid.players) {
            player.mes("Challenge started: $challengeName")
            ToaDamage.resetCurrent(player)
        }
    }

    private fun tick() {
        if (stage != ToaStage.STARTED) return
        onTick(targets())
        schedule(1) { tick() }
    }

    internal fun enter(player: Player) {
        for (seq in room.path?.preloadSeqs.orEmpty()) {
            player.runClientScript(SEQ_PREFETCH_SCRIPT, seq)
        }
        if (stage == ToaStage.STARTED) hpBar?.open(player)
        onEnter(player)
    }

    internal fun leave(player: Player) {
        hpBar?.close(player)
        onLeave(player)
    }

    internal fun continueChallenge(from: ToaEncounter) {
        stage = ToaStage.STARTED
        startCycle = from.startCycle
        challengeName = from.challengeName
        teamSize = from.teamSize
    }

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
        raid.points.completeRoom(teamSize, completionPoints, awardsMvp)
        if (isEnd) {
            for (player in raid.players) {
                player.personalContribution = raid.points.lootPoints(player)
            }
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
        hpBar?.complete()
        onComplete()
        if (isEnd) {
            ToaRaidManager.refreshTimers(raid)
        }
    }

    private fun sendRaidCompleteMessages(player: Player, duration: String, total: String, now: Int) {
        val mode = raid.settings.mode
        val name = "Tombs of Amascut${ToaKillCount.modeSuffix(mode)}"
        val raidTime = ToaRaid.formatTicks(raid.elapsedTicks(now))
        player.mes(
            "Challenge complete: $challengeName. Duration: <col=ef1020>$duration</col><br>" +
                "$name challenge completion time: <col=ef1020>$total</col>"
        )
        player.mes("Tombs of Amascut total completion time: <col=ef1020>$raidTime</col>")
        ToaKillCount.record(player, mode)
        ToaStats.recordTimes(
            player,
            mode,
            raid.partySize,
            raid.totalChallengeTicks(),
            raid.elapsedTicks(now),
        )
        val limit = raid.timeLimitMinutes ?: return
        if (raid.failedTimeLimit) {
            player.mes("<col=FF0000>Your party failed to beat the overall target time of $limit:00</col>")
        } else {
            player.mes("<col=00FF00>Your party beat the overall target time of $limit:00!</col>")
        }
    }

    @OptIn(InternalApi::class)
    private fun recoverPlayers() {
        val challengeSpawn = room.challengeSpawn
        val boss = room.kind != ToaRoom.Kind.PUZZLE
        for (player in players) {
            raid.revive(player)
            if (boss) player.toaBossRestore() else player.cureAllToxins()
            if (challengeSpawn != null && !inChallengeArea(player)) {
                val dest = coords(challengeSpawn)
                deps.launcher.launchLenient(player) { telejump(dest, TeleportType.Exempt) }
            }
        }
        ToaRaidManager.refreshHudStates(raid)
    }

    @OptIn(InternalApi::class)
    fun checkRoomReset() {
        if (destroyed || stage != ToaStage.STARTED) return
        val inRoom = players
        if (inRoom.isEmpty()) return
        if (inRoom.any { inChallengeArea(it) || !raid.isGhost(it) }) return

        raid.teamDeaths++
        raid.points.resetRoom()
        val retry = raid.canRetryAfter()
        val encounter = this
        for (player in inRoom) {
            deps.launcher.launchLenient(player) { wipeAftermath(encounter, retry) }
        }
        reset()
    }

    fun reset() {
        stage = ToaStage.NOT_STARTED
        stopTasks()
        for (player in players) hpBar?.close(player)
        onReset()
    }

    open fun debugComplete() {
        start()
        complete()
    }

    internal fun spawn(type: String, tile: CoordGrid, facing: Direction? = null): Npc {
        val npc = Npc(type, tile)
        if (facing != null) npc.respawnDir = facing
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        adopt(npc)
        return npc
    }

    internal fun adopt(npc: Npc) {
        npc.respawns = false
        deps.instances.attachNpc(session.id, npc)
        pruneCombatants()
        if (npc in combatants) return
        val spec = combatantSpecs.firstOrNull { it.typeId == npc.type.id } ?: return
        combatants[npc] = ToaCombatant(this, npc, spec).also { it.scale(scalingPartySize) }
    }

    internal fun release(npc: Npc) {
        deps.instances.detachNpc(npc)
        combatants.remove(npc)
    }

    internal fun combatantOf(npc: Npc): ToaCombatant? = combatants[npc]

    protected fun scaleCombatants() {
        pruneCombatants()
        for (combatant in combatants.values) combatant.scale(teamSize)
    }

    protected fun holdDefenceFloors() {
        pruneCombatants()
        for (combatant in combatants.values) combatant.holdDefenceFloor()
    }

    private fun pruneCombatants() {
        combatants.keys.removeIf { !it.isSlotAssigned }
    }

    internal fun despawn(npc: Npc) {
        release(npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    internal fun isOpenFloor(tile: CoordGrid): Boolean =
        !deps.collision.isWalkBlocked(tile) &&
            deps.locRepo.findExact(tile, LocShape.CentrepieceStraight) == null

    internal open fun isTaken(tile: CoordGrid): Boolean = false

    internal fun freeTiles(
        min: CoordGrid,
        max: CoordGrid,
        excludes: Collection<CoordGrid>,
    ): List<CoordGrid> {
        val from = coords(min)
        val to = coords(max)
        val tiles = ArrayList<CoordGrid>()
        for (x in from.x..to.x) {
            for (z in from.z..to.z) {
                val tile = CoordGrid(x, z, from.level)
                if (tile in excludes || isTaken(tile) || !isOpenFloor(tile)) continue
                tiles += tile
            }
        }
        return deps.random.shuffled(tiles)
    }

    internal fun hazardDamage(player: Player, base: Int, spread: Int) {
        val min = floor(base * raid.damageMultiplier).toInt()
        player.hitTypeless(deps.random.of(min, min + spread))
    }

    fun schedule(ticks: Int, action: () -> Unit) {
        val generation = taskGeneration
        deps.worldQueues.add(ticks) {
            if (!destroyed && generation == taskGeneration) {
                action()
            }
        }
    }

    fun stopTasks() {
        taskGeneration++
    }

    internal fun destroy() {
        if (destroyed) return
        destroyed = true
        stopTasks()
        ToaRooms.unregister(this)
        deps.instances.end(session)
    }

    override fun toString(): String = "ToaEncounter(room=$room, controllerId=$controllerId, stage=$stage)"

    private companion object {
        const val SEQ_PREFETCH_SCRIPT = 1846
        const val MAX_PATH_TIER = 2
        const val PATH_LEVELS_PER_TIER = 2
        const val HONEY_LOCUSTS_MIN = 4
        const val HONEY_LOCUSTS_MAX = 6

        const val ROOM_POINTS_CAP = 20_000
        const val WARDENS_POINTS_CAP = 60_000
        val PUZZLE_POINTS =
            mapOf(ToaPath.SCABARAS to 300, ToaPath.APMEKEN to 450, ToaPath.CRONDIS to 400)
    }
}
