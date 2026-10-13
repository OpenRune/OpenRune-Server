package org.rsmod.content.raids.toa.raid.encounter.apmeken.puzzle

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.ceil
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

internal class ApmekenSight(private val room: ApmekenPuzzleEncounter) {
    private val deps = room.raid.deps

    private enum class Kind {
        ROOF,
        VENTS,
    }

    private class Issue(val kind: Kind, val deadline: Int, val required: Int) {
        val done = HashSet<Int>()
    }

    var holder: Player? = null
        private set

    private var granted = false
    private var issue: Issue? = null
    private var grantAt = 0
    private var nextIssueAt = 0
    private var issueCount = 0

    val holderSlot: Int
        get() = holder?.let { room.raid.players.indexOf(it) } ?: -1

    fun start(now: Int) {
        granted = false
        issue = null
        issueCount = 0
        grantAt = now + GRANT_DELAY
        nextIssueAt = now + FIRST_ISSUE
    }

    fun clear() {
        holder?.let { setHud(it, 0) }
        holder = null
        granted = false
        issue = null
    }

    fun tick(now: Int, targets: List<Player>) {
        if (targets.isEmpty()) return
        if (!granted && now >= grantAt) grant(targets.first())
        val current = holder
        if (granted && (current == null || current !in targets)) grant(targets.first())
        val active = issue
        if (active == null) {
            if (granted && now >= nextIssueAt) raise(now, targets)
        } else if (now >= active.deadline) {
            resolve(active, now, targets)
        }
    }

    fun forceIssue(roof: Boolean) {
        if (issue != null) return
        raiseKind(if (roof) Kind.ROOF else Kind.VENTS, deps.mapClock.cycle, room.targets())
    }

    fun grantTo(player: Player) = grant(player)

    fun repair(player: Player, index: Int) {
        val active = issue
        if (active == null || active.kind != Kind.ROOF) {
            return wrongAction(player, ApmekenMessages.ROOF_WRONG, roof = index)
        }
        if (!active.done.add(index)) return player.mes(ApmekenMessages.ROOF_DONE)
        player.mes(ApmekenMessages.ROOF_REPAIRED)
        player.soundSynth(ApmekenSynths.REPAIR)
    }

    fun neutralise(player: Player, index: Int) {
        val active = issue
        if (active == null || active.kind != Kind.VENTS) {
            return wrongAction(player, ApmekenMessages.VENT_WRONG, vent = index)
        }
        if (!active.done.add(index)) return player.mes(ApmekenMessages.VENT_DONE)
        player.mes(ApmekenMessages.VENT_NEUTRALISED)
    }

    private fun wrongAction(player: Player, message: String, roof: Int = -1, vent: Int = -1) {
        player.mes(message)
        player.hitTypeless(room.hazardRoll(FAILURE_DAMAGE, 0))
        if (roof >= 0) rubble(player.coords)
        if (vent >= 0) gas(ApmekenCoords.VENTS[vent])
    }

    private fun grant(player: Player) {
        holder?.takeIf { it !== player }?.let { setHud(it, 0) }
        holder = player
        granted = true
        player.mes(ApmekenMessages.SIGHT_GRANTED)
        player.spotanim(ApmekenSpots.GAIN_SIGHT, 0, SIGHT_HEIGHT, SIGHT_SLOT)
        player.soundSynth(ApmekenSynths.SIGHT)
        setHud(player, 1)
    }

    private fun setHud(player: Player, value: Int) {
        val slot = room.raid.players.indexOf(player)
        if (slot < 0) return
        for (viewer in room.players) {
            VarPlayerIntMapSetter.set(viewer, ApmekenVarbits.sight(slot), value)
        }
    }

    private fun raise(now: Int, targets: List<Player>) {
        val kind = Kind.entries[deps.random.of(maxExclusive = Kind.entries.size)]
        raiseKind(kind, now, targets)
    }

    private fun raiseKind(kind: Kind, now: Int, targets: List<Player>) {
        val required = minOf(MAX_REQUIRED, targets.size.coerceAtLeast(1))
        issue = Issue(kind, now + WINDOW, required)
        val seer = holder
        for (player in targets) {
            player.soundSynth(ApmekenSynths.SIGHT)
            if (player !== seer) player.mes(ApmekenMessages.OTHER_ISSUE)
        }
        if (seer == null) return
        when (kind) {
            Kind.ROOF -> {
                seer.mes(ApmekenMessages.ROOF_ISSUE)
                markers(ApmekenCoords.SUPPORT_FRONTS, ROOF_MARK_HEIGHT)
            }
            Kind.VENTS -> {
                seer.mes(ApmekenMessages.VENT_ISSUE)
                markers(ApmekenCoords.VENTS, VENT_MARK_HEIGHT)
            }
        }
    }

    private fun markers(tiles: List<CoordGrid>, height: Int) {
        for (tile in tiles) {
            val north = tile.z > NORTH_Z
            val spot = if (north) ApmekenSpots.WARNING_NORTH else ApmekenSpots.WARNING_SOUTH
            deps.worldRepo.spotanimMap(spotanim(spot), room.coords(tile), height, 0)
        }
    }

    private fun resolve(active: Issue, now: Int, targets: List<Player>) {
        issue = null
        nextIssueAt = now + GAPS[issueCount.coerceAtMost(GAPS.lastIndex)]
        issueCount++
        val missed = (active.required - active.done.size).coerceAtLeast(0)
        if (missed == 0) {
            val message =
                if (active.kind == Kind.ROOF) ApmekenMessages.ROOF_SUCCESS
                else ApmekenMessages.VENT_SUCCESS
            for (player in targets) {
                player.mes(message)
                player.soundSynth(ApmekenSynths.RESOLVED)
            }
            return
        }
        val scaled = ceil(FAILURE_DAMAGE * missed / active.required.toDouble()).toInt()
        for (player in targets) player.hitTypeless(room.hazardRoll(scaled, 0))
        if (active.kind == Kind.ROOF) roofFailure(targets) else ventFailure(targets)
    }

    private fun roofFailure(targets: List<Player>) {
        for (player in targets) {
            player.mes(ApmekenMessages.ROOF_FAILURE)
            player.soundSynth(ApmekenSynths.ROOF_COLLAPSE)
            player.soundSynth(ApmekenSynths.ROOF_COLLAPSE_TAIL)
            rubble(player.coords)
        }
        val shaking = ServerCacheManager.getObject(ApmekenLocs.PILLAR_SHAKING.asRSCM(RSCMType.LOC))
        val idle = ApmekenLocs.PILLAR.asRSCM(RSCMType.LOC)
        if (shaking == null) return
        for (tile in ApmekenCoords.SUPPORTS) {
            val loc = deps.locRepo.findAll(room.coords(tile)).firstOrNull { it.id == idle }
            if (loc != null) deps.locRepo.change(loc, shaking, SHAKE_TICKS)
        }
    }

    private fun ventFailure(targets: List<Player>) {
        for (player in targets) {
            player.mes(ApmekenMessages.VENT_FAILURE)
            player.soundSynth(ApmekenSynths.VENT_FAILURE)
        }
        for (tile in ApmekenCoords.VENTS) gas(tile)
    }

    private fun rubble(tile: CoordGrid) {
        deps.worldRepo.spotanimMap(spotanim(ApmekenSpots.RUBBLE), tile, 0, 0)
    }

    private fun gas(static: CoordGrid) {
        deps.worldRepo.spotanimMap(spotanim(ApmekenSpots.GAS), room.coords(static), 0, 0)
    }

    private companion object {
        const val GRANT_DELAY = 2
        const val FIRST_ISSUE = 30
        const val WINDOW = 20
        const val MAX_REQUIRED = 4
        const val FAILURE_DAMAGE = 22
        const val SIGHT_HEIGHT = 180
        const val SIGHT_SLOT = 4
        const val ROOF_MARK_HEIGHT = 96
        const val VENT_MARK_HEIGHT = 46
        const val NORTH_Z = 5280
        const val SHAKE_TICKS = 4
        val GAPS = listOf(30, 26, 26)
    }
}
