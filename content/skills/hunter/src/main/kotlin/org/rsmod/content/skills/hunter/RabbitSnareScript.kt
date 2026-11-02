package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.utils.skills.SkillingSuccessRate
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class RabbitSnareScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val random: GameRandom,
    private val traps: HunterTrapScript,
) : PluginScript() {
    private val holeId: Int by lazy { HOLE.asRSCM(RSCMType.LOC) }

    override fun ScriptContext.startup() {
        onOpLoc1(HOLE) { flush(it.loc) }
    }

    private suspend fun ProtectedAccess.flush(hole: BoundLocInfo) {
        if (stat(HUNTER) < LEVEL) {
            mes("You need a Hunter level of $LEVEL to flush rabbits out of their holes.")
            return
        }
        if (
            !QuestRequirements.hasCompleted(player, EAGLES_PEAK) &&
                !QuestRequirements.isOnQuest(player, EAGLES_PEAK)
        ) {
            mes("You need to have completed Eagles' Peak to hunt rabbits.")
            return
        }
        if (FERRET !in inv) {
            mes("You need a ferret to flush the rabbit out of its hole.")
            return
        }
        faceSquare(hole.coords)
        anim(FLUSH_SEQ)
        delay(FLUSH_TICKS)
        val destination = otherHoles(hole.coords).randomOrNull() ?: return
        locRepo.change(hole, EMPTY_HOLE, EMPTY_TICKS)
        val path = line(hole.coords, destination)
        val kind = traps.kinds().firstOrNull { it.item == SNARE }
        val snare =
            path.firstNotNullOfOrNull { tile ->
                traps.trapAt(tile)?.takeIf { it.owner == player && it.kind === kind && it.state == HunterTrapScript.TrapState.Set }
            }
        val end = snare?.coords ?: destination
        val travelCycles = TRAVEL_STEP * distance(hole.coords, end)
        worldRepo.projAnim(
            ProjAnim(TRAVEL_SPOTANIM.asRSCM(RSCMType.SPOTANIM), 0, 0, 0, travelCycles, 0, 0, 0, 0, hole.coords, end)
        )
        val keep = random.of(256) < (SkillingSuccessRate.successRate(FERRET_LOW, FERRET_HIGH, stat(HUNTER).coerceIn(1, 99), 99) * 256).toInt()
        if (!keep && invDel(inv, FERRET).success) mes("Your ferret runs away.", ChatType.Spam)
        if (snare == null || kind == null) return
        delay((travelCycles + 29) / 30)
        val creature = kind.creatures.firstOrNull() ?: return
        val mirrored = end.x < hole.coords.x || end.z < hole.coords.z
        traps.catchWithoutLure(snare, creature, creature.trappingLocs[if (mirrored) 1 else 0])
        worldRepo.locAnim(snare.loc, CAPTURE_SEQS[random.of(CAPTURE_SEQS.size)])
    }

    private fun otherHoles(from: CoordGrid): List<CoordGrid> {
        val zone = ZoneKey.from(from)
        return locRepo
            .findAll(zone)
            .plus((-1..1).flatMap { dx -> (-1..1).map { dz -> ZoneKey(zone.x + dx, zone.z + dz, zone.level) } }
                .filter { it != zone }
                .flatMap { locRepo.findAll(it) })
            .filter { it.id == holeId && it.coords != from && distance(it.coords, from) <= HOLE_RANGE }
            .map { it.coords }
            .toList()
    }

    private fun line(from: CoordGrid, to: CoordGrid): List<CoordGrid> {
        val steps = distance(from, to)
        if (steps == 0) return emptyList()
        return (1 until steps).map { i ->
            CoordGrid(
                from.x + Math.round((to.x - from.x) * i / steps.toDouble()).toInt(),
                from.z + Math.round((to.z - from.z) * i / steps.toDouble()).toInt(),
                from.level,
            )
        }
    }

    private fun distance(a: CoordGrid, b: CoordGrid): Int = max(abs(a.x - b.x), abs(a.z - b.z))

    private fun <T> List<T>.randomOrNull(): T? = if (isEmpty()) null else this[random.of(size)]

    private companion object {
        const val HUNTER = "stat.hunter"
        const val HOLE = "loc.hunting_rabbit_launcher1"
        const val EMPTY_HOLE = "loc.hunting_rabbit_launcher_empty"
        const val FERRET = "obj.hunting_ferret"
        const val SNARE = "obj.hunting_snare"
        const val EAGLES_PEAK = "quest_eaglespeak"
        const val FLUSH_SEQ = "seq.human_pickupfloor"
        const val TRAVEL_SPOTANIM = "spotanim.hunting_rabbit_travel"
        val CAPTURE_SEQS =
            listOf(
                "seq.hunting_snare_capture_rabbit0",
                "seq.hunting_snare_capture_rabbit1",
                "seq.hunting_snare_capture_rabbit2",
            )
        const val LEVEL = 27
        const val FLUSH_TICKS = 1
        const val EMPTY_TICKS = 50
        const val HOLE_RANGE = 12
        const val TRAVEL_STEP = 15
        const val FERRET_LOW = 192
        const val FERRET_HIGH = 256
    }
}
