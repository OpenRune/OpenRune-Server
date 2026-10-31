package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.table.hunter.HunterTrackingRow
import org.rsmod.content.skills.hunter.HunterPouch.Companion.pouchHunted
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

@Singleton
class KebbitTrackingScript
@Inject
constructor(
    private val worldRepo: WorldRepository,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
    private val rumours: HunterRumours,
) : PluginScript() {
    private val areas: List<Area> by lazy { HunterTrackingRow.all().map(::Area) }
    private val tracks = HashMap<Player, Track>()

    override fun ScriptContext.startup() {
        for (area in areas) {
            for ((node, den) in area.dens) onOpLoc1(den) { inspectDen(area, node) }
            for ((index, segment) in area.segments.withIndex()) {
                onOpLoc1(segment.clue) { inspectConnector(area, index) }
            }
            for (spot in area.spotLocs) {
                onOpLoc1(spot) { search(area, it.loc.coords) }
                onOpLoc2(spot) { attack(area, it.loc.coords) }
            }
        }
        onPlayerLogout { tracks.remove(player) }
    }

    private suspend fun ProtectedAccess.inspectDen(area: Area, start: Int) {
        anim(SEARCH_SEQ)
        delay(INSPECT_TICKS)
        if (stat(HUNTER) < area.level) {
            mes("You need a Hunter level of ${area.level} to track a ${area.name}.")
            return
        }
        clear(player, area)
        val track = Track(area, mutableSetOf(), visits = 1)
        tracks[player] = track
        val first = choose(track, start) ?: return
        track.used += first.segment
        reveal(player, area, first)
        mes("Closer inspection reveals tracks leading away from the burrow.")
        arrive(track, first)
        if (RING_OF_PURSUIT in player.worn) {
            while (track.pending != null) {
                val next = track.pending!!
                reveal(player, area, next)
                arrive(track, next)
            }
            VarPlayerIntMapSetter.set(player, area.usedVarbit, 1)
        }
    }

    private suspend fun ProtectedAccess.inspectConnector(area: Area, index: Int) {
        anim(SEARCH_SEQ)
        delay(INSPECT_TICKS)
        val track = tracks[player]
        val pending = track?.pending
        if (track == null || track.area !== area || pending == null || pending.segment != index) {
            mes("Nothing seems to be out of place here.")
            return
        }
        reveal(player, area, pending)
        VarPlayerIntMapSetter.set(player, area.usedVarbit, 1)
        val fromTunnel = area.tunnels.containsKey(pending.from)
        val intoTunnel = area.tunnels.containsKey(pending.to)
        when {
            fromTunnel -> mes("Closer investigation reveals more tracks leading away from this tunnel.")
            intoTunnel -> mes("You can just make out some tracks leading into the tunnel.<p>Maybe they come out again somewhere nearby.")
            else -> mes("Closer investigation reveals more tracks leading away from here.")
        }
        arrive(track, pending)
    }

    private fun ProtectedAccess.search(area: Area, coords: CoordGrid) {
        val track = tracks[player]
        val target = track?.target
        if (track == null || track.area !== area || target == null || area.spotCoords[target] != coords) {
            mes("There doesn't seem to be anything hiding ${area.spotWhere}.")
            return
        }
        anim(SEARCH_SEQ)
        mes("It looks like there might be something hiding ${area.spotWhere}.")
    }

    private fun ProtectedAccess.attack(area: Area, coords: CoordGrid) {
        val track = tracks[player]
        val target = track?.target
        if (track == null || track.area !== area || target == null || area.spotCoords[target] != coords) {
            mes("There doesn't seem to be anything hiding ${area.spotWhere}.")
            return
        }
        tracks.remove(player)
        clear(player, area)
        if (NOOSE_WAND !in player.worn) {
            anim(KICK_SEQ)
            mes("As you kick randomly, a creature scrabbles out from the ${area.spotNoun} and runs off.")
            mes("Maybe you would be better off using a noose wand.")
            val away = coords.translate(random.of(-FLEE_RANGE, FLEE_RANGE), random.of(-FLEE_RANGE, FLEE_RANGE))
            worldRepo.projAnim(
                ProjAnim(area.fleeSpotanim.asRSCM(RSCMType.SPOTANIM), 0, 0, FLEE_START, FLEE_END, 0, 0, 0, 0, coords, away)
            )
            return
        }
        anim(area.catchSeq)
        for (obj in area.loot) if (!pouchHunted(obj)) invAddOrDrop(objRepo, obj)
        statAdvance(HUNTER, area.xp)
        rumours.onCatch(player, area.rumourKey)
        mes("You manage to noose ${HunterRumours.article(area.name)} ${area.name} that is hiding in the ${area.spotNoun}.")
    }

    private fun choose(track: Track, node: Int): Step? {
        val options =
            track.area.segments.withIndex().filter { (index, segment) ->
                index !in track.used && (segment.a == node || segment.b == node)
            }
        if (options.isEmpty()) return null
        val (index, segment) = options[random.of(options.size)]
        val to = if (segment.a == node) segment.b else segment.a
        val forward = (segment.a == node) == segment.forwardFromA
        return Step(index, node, to, forward)
    }

    private fun ProtectedAccess.arrive(track: Track, step: Step) {
        val area = track.area
        track.pending = null
        val node = step.to
        val partner = area.tunnels[node]
        if (partner != null) {
            val next = choose(track, partner) ?: return fail(track)
            pend(track, next)
            return
        }
        track.visits++
        if (area.spotIndex.containsKey(node) && track.visits >= area.nmin) {
            track.target = node
            VarPlayerIntMapSetter.set(player, area.endsVarbit, area.spotIndex.getValue(node))
            return
        }
        val next = choose(track, node)
        if (next == null) {
            if (area.spotIndex.containsKey(node)) {
                track.target = node
                VarPlayerIntMapSetter.set(player, area.endsVarbit, area.spotIndex.getValue(node))
                return
            }
            return fail(track)
        }
        pend(track, next)
    }

    private fun ProtectedAccess.pend(track: Track, step: Step) {
        track.used += step.segment
        track.pending = step
        val segment = track.area.segments[step.segment]
        VarPlayerIntMapSetter.set(player, segment.varbit, if (step.forward) HIDDEN else HIDDEN_REVERSED)
    }

    private fun ProtectedAccess.fail(track: Track) {
        tracks.remove(player)
        clear(player, track.area)
    }

    private fun reveal(player: Player, area: Area, step: Step) {
        val segment = area.segments[step.segment]
        VarPlayerIntMapSetter.set(player, segment.varbit, if (step.forward) VISIBLE else VISIBLE_REVERSED)
    }

    private fun clear(player: Player, area: Area) {
        for (segment in area.segments) VarPlayerIntMapSetter.set(player, segment.varbit, 0)
        VarPlayerIntMapSetter.set(player, area.usedVarbit, 0)
        VarPlayerIntMapSetter.set(player, area.endsVarbit, 0)
    }

    data class Step(val segment: Int, val from: Int, val to: Int, val forward: Boolean)

    class Track(val area: Area, val used: MutableSet<Int>, var visits: Int) {
        var pending: Step? = null
        var target: Int? = null
    }

    class Segment(val varbit: String, val a: Int, val b: Int, val forwardFromA: Boolean, val clue: String)

    class Area(row: HunterTrackingRow) {
        val name: String = row.name
        val level: Int = row.level
        val xp: Double = row.xpTenths / 10.0
        val loot: List<String> = row.loot.map { it.internalName }
        val catchSeq: String = RSCM.getReverseMapping(RSCMType.SEQ, row.catchSeq.id)
        val fleeSpotanim: String = row.fleeSpotanim
        val spotNoun: String = row.spotNoun
        val spotWhere: String = row.spotWhere
        val usedVarbit: String = row.usedVarbit
        val endsVarbit: String = row.endsVarbit
        val nmin: Int = row.nmin
        val dens: List<Pair<Int, String>> = row.dens.map { it.t0 to RSCM.getReverseMapping(RSCMType.LOC, it.t1.id) }
        val spotCoords: Map<Int, CoordGrid> = row.spots.associate { it.t0 to it.t1 }
        val spotIndex: Map<Int, Int> = row.spots.associate { it.t0 to it.t2 }
        val tunnels: Map<Int, Int> = row.tunnels.chunked(2).filter { it[0] >= 0 }.associate { it[0] to it[1] }
        val segments: List<Segment> =
            row.segments.map { Segment(it.t0, it.t1, it.t2, it.t3, RSCM.getReverseMapping(RSCMType.LOC, it.t4.id)) }
        val spotLocs: List<String> = listOf(RSCM.getReverseMapping(RSCMType.LOC, row.spotLoc.id))
        val rumourKey: String = "tracking:$name"
    }

    companion object {
        const val HUNTER = "stat.hunter"
        const val NOOSE_WAND = "obj.noose_wand"
        const val RING_OF_PURSUIT = "obj.ring_of_pursuit"
        const val SEARCH_SEQ = "seq.hunting_searching_bushes"
        const val KICK_SEQ = "seq.human_unarmedkick"
        const val INSPECT_TICKS = 2
        const val HIDDEN = 1
        const val HIDDEN_REVERSED = 2
        const val VISIBLE = 3
        const val VISIBLE_REVERSED = 4
        const val FLEE_RANGE = 6
        const val FLEE_START = 30
        const val FLEE_END = 67
    }
}
