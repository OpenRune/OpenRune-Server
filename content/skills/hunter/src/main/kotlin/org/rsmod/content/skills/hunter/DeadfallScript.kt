package org.rsmod.content.skills.hunter

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.api.table.hunter.HunterDeadfallsRow
import org.rsmod.content.skills.hunter.HunterPouch.Companion.pouchHunted
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.catchChance256
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.distance
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class DeadfallScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val routes: RouteFactory,
    private val collision: CollisionFlagMap,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
) : PluginScript() {
    private val traps = HashMap<CoordGrid, Deadfall>()
    private val creatures: List<Creature> by lazy { HunterDeadfallsRow.all().map(::Creature) }
    private val logs: List<String> by lazy {
        FiremakingLogsRow.all()
            .sortedBy { row -> row.statReq.firstOrNull()?.t1 ?: 0 }
            .map { it.input.internalName }
            .filter { it !in REFUSED_LOGS }
            .distinct()
    }

    override fun ScriptContext.startup() {
        onOpLoc1(BOULDER) { set(it.loc) }
        onOpLoc1(TRAP) { own(it.loc.coords)?.let { trap -> dismantle(trap) } }
        onOpLoc2(TRAP) { own(it.loc.coords)?.let { mes("This deadfall hasn't caught anything yet.") } }
        for (torch in HunterTrapScript.TORCHES) onOpLocU(TRAP, torch) { own(it.loc.coords)?.let { trap -> smoke(trap) } }
        for (bait in creatures.map { it.bait }.distinct()) {
            onOpLocU(TRAP, bait) { own(it.loc.coords)?.let { trap -> bait(trap, bait) } }
        }
        for (creature in creatures) onOpLoc1(creature.full) { own(it.loc.coords)?.let { trap -> check(trap) } }
        onPlayerSoftTimer(TIMER) { player.tick() }
        onPlayerLogout { player.clearTraps() }
    }

    private fun ProtectedAccess.own(coords: CoordGrid): Deadfall? {
        val trap = traps[coords] ?: return null
        if (trap.owner != player) {
            mes("This isn't your trap.")
            return null
        }
        return trap
    }

    private suspend fun ProtectedAccess.set(bound: BoundLocInfo) {
        val boulder = LocInfo(bound.layer, bound.coords, bound.entity)
        if (traps.containsKey(boulder.coords)) return
        if (traps.values.count { it.owner == player } >= LIMIT) {
            mes("You can only have two deadfall traps set up at a time.")
            return
        }
        val log = logs.firstOrNull { it in inv }
        val knife = KNIVES.any { it in inv || it in player.worn }
        if (log == null || !knife) {
            mes("You need some logs and a knife to set a deadfall trap.")
            return
        }
        val trap = Deadfall(player, boulder, bound.adjustedWidth, bound.adjustedLength)
        traps[boulder.coords] = trap
        replace(trap, SETTING)
        mes("You begin setting up a trap.", ChatType.Spam)
        anim(SET_SEQ)
        delay(1)
        if (traps[boulder.coords] !== trap || log !in inv) {
            clear(trap)
            return
        }
        if (random.of(LOG_KEEP_DENOMINATOR) == 0) invDel(inv, log)
        HunterTrapScript.overlayTimer(player, replace(trap, TRAP), HunterTrapScript.SET_COLOUR)
        soundSynth(SET_SYNTH)
        trap.state = State.Set
        trap.stateCycle = clock.cycle
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.dismantle(trap: Deadfall) {
        if (trap.state != State.Set) return
        anim(SET_SEQ)
        delay(CHECK_TICKS)
        if (traps[trap.boulder.coords] !== trap || trap.state != State.Set) return
        clear(trap)
        mes("You dismantle the trap.", ChatType.Spam)
    }

    private fun ProtectedAccess.bait(trap: Deadfall, bait: String) {
        if (trap.state != State.Set || trap.baited || invDel(inv, bait).failure) return
        anim(HunterTrapScript.BAIT_SEQ)
        trap.baited = true
        mes("You bait the trap.", ChatType.Spam)
    }

    private fun ProtectedAccess.smoke(trap: Deadfall) {
        if (trap.state != State.Set) return
        if (stat(HUNTER) < HunterTrapScript.SMOKE_LEVEL) {
            mes("You need a Hunter level of ${HunterTrapScript.SMOKE_LEVEL} to smoke a trap.")
            return
        }
        if (trap.smoked) {
            mes("This trap has already been smoked.")
            return
        }
        anim(HunterTrapScript.BAIT_SEQ)
        trap.smoked = true
        mes("You use the smoke from the torch to remove your scent from the trap.", ChatType.Spam)
    }

    private suspend fun ProtectedAccess.check(trap: Deadfall) {
        val creature = trap.caught ?: return
        if (trap.state != State.Full) return
        if (!canAdd(creature.loot)) {
            mes("You don't have enough inventory space.")
            return
        }
        anim(SET_SEQ)
        delay(CHECK_TICKS)
        if (traps[trap.boulder.coords] !== trap || trap.state != State.Full || !canAdd(creature.loot)) return
        clear(trap)
        for (obj in creature.loot) if (!pouchHunted(obj)) invAdd(inv, obj)
        statAdvance(HUNTER, creature.xp)
        soundSynth(CHECK_SYNTH)
        rumours.onCatch(player, creature.npc)
        mes("You dismantle the trap.", ChatType.Spam)
        mes("You've caught ${HunterRumours.article(creature.name)} ${creature.name}.", ChatType.Spam)
    }

    private fun ProtectedAccess.canAdd(items: List<String>): Boolean {
        val into = inv
        return player
            .invTransaction(into, autoCommit = false) {
                val target = select(into)
                for (obj in items) add(target, obj.asRSCM(RSCMType.OBJ), 1)
            }
            .success
    }

    private fun Player.tick() {
        val own = traps.values.filter { it.owner == this }
        if (own.isEmpty()) {
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        for (trap in own) {
            val age = now - trap.stateCycle
            when (trap.state) {
                State.Setting -> {}
                State.Set -> {
                    val lured = trap.lured
                    when {
                        lured != null -> approach(trap, lured)
                        age > COLLAPSE_TICKS -> collapse(trap)
                        age >= ATTEMPT_START -> lure(trap)
                    }
                }
                State.Trapping -> if (age >= SETTLE_TICKS) {
                    val creature = trap.caught!!
                    HunterTrapScript.overlayTimer(this, replace(trap, creature.full), HunterTrapScript.FULL_COLOUR)
                    worldRepo.soundArea(trap.boulder.coords, creature.fullSynth)
                    trap.state = State.Full
                    trap.stateCycle = now
                }
                State.Failing -> if (age >= SETTLE_TICKS) clear(trap)
                State.Full -> if (age >= COLLAPSE_TICKS) collapse(trap)
            }
        }
    }

    private fun lure(trap: Deadfall) {
        val busy = traps.values.mapNotNull { it.lured?.npc }.toSet()
        val center = trap.boulder.coords
        val npc =
            npcRepo
                .findAll(ZoneKey.from(center), zoneRadius = 1)
                .filter { n -> creatures.any { it.npcId == n.id } && n.isSlotAssigned && n.isVisible && n !in busy }
                .filter { it.coords.level == center.level && distance(it.coords, center) <= LURE_RANGE }
                .minByOrNull { distance(it.coords, center) } ?: return
        val side = sideTiles(trap).filter { !collision.isWalkBlocked(it) }.minByOrNull { distance(it, npc.coords) } ?: return
        trap.lured = Lure(npc, creatures.first { it.npcId == npc.id }, side, clock.cycle)
        npc.walkTo(routes, side)
    }

    private fun Player.approach(trap: Deadfall, lure: Lure) {
        val npc = lure.npc
        if (!npc.isSlotAssigned || !npc.isVisible) {
            trap.lured = null
            return
        }
        val now = clock.cycle
        if (lure.ready == 0) {
            if (npc.coords == lure.side) {
                npc.faceSquare(trap.boulder.coords, trap.width, trap.length)
                npc.anim(lure.creature.approachSeq)
                lure.ready = now
            } else if (now - lure.started > APPROACH_TIMEOUT) {
                trap.lured = null
            }
            return
        }
        if (now - lure.ready < 1) return
        trap.lured = null
        resolve(trap, npc, lure)
    }

    private fun Player.resolve(trap: Deadfall, npc: Npc, lure: Lure) {
        val creature = lure.creature
        val level = statBase(HUNTER)
        val chance =
            HunterOutfit.applyTo(catchChance256(creature.low, creature.high, level), this) +
                (if (trap.baited) HunterTrapScript.BAIT_BONUS else 0) +
                (if (trap.smoked) HunterTrapScript.SMOKE_BONUS else 0)
        val now = clock.cycle
        if (level >= creature.level && random.of(256) < chance) {
            npcRepo.despawn(npc, creature.respawnTicks)
            trap.caught = creature
            replace(trap, if (mirrored(trap, lure.side)) creature.trappingMirrored else creature.trapping)
            worldRepo.soundArea(trap.boulder.coords, TRAPPING_SYNTH)
            trap.state = State.Trapping
        } else {
            replace(trap, FAILING)
            trap.state = State.Failing
        }
        trap.stateCycle = now
    }

    private fun Player.collapse(trap: Deadfall) {
        clear(trap)
        mes("The deadfall trap that you constructed has collapsed.")
    }

    private fun Player.clearTraps() {
        for (trap in traps.values.filter { it.owner == this }) clear(trap)
    }

    private fun sideTiles(trap: Deadfall): List<CoordGrid> {
        val c = trap.boulder.coords
        return if (trap.width >= trap.length) {
            (0 until trap.width).flatMap { listOf(c.translate(it, trap.length), c.translate(it, -1)) }
        } else {
            (0 until trap.length).flatMap { listOf(c.translate(trap.width, it), c.translate(-1, it)) }
        }
    }

    private fun mirrored(trap: Deadfall, side: CoordGrid): Boolean =
        if (trap.width >= trap.length) side.z < trap.boulder.coords.z else side.x < trap.boulder.coords.x

    private fun replace(trap: Deadfall, internal: String): LocInfo {
        val b = trap.boulder
        val loc = LocInfo(b.layer, b.coords, LocEntity(internal.asRSCM(RSCMType.LOC), b.shapeId, b.angleId))
        locRepo.add(loc, Int.MAX_VALUE)
        return loc
    }

    private fun clear(trap: Deadfall) {
        traps.remove(trap.boulder.coords)
        trap.lured = null
        locRepo.add(trap.boulder, Int.MAX_VALUE)
    }

    internal enum class State {
        Setting,
        Set,
        Trapping,
        Full,
        Failing,
    }

    internal class Lure(val npc: Npc, val creature: Creature, val side: CoordGrid, val started: Int) {
        var ready: Int = 0
    }

    internal class Deadfall(val owner: Player, val boulder: LocInfo, val width: Int, val length: Int) {
        var state: State = State.Setting
        var stateCycle: Int = 0
        var lured: Lure? = null
        var caught: Creature? = null
        var baited: Boolean = false
        var smoked: Boolean = false
    }

    internal class Creature(row: HunterDeadfallsRow) {
        val name: String = row.name
        val npc: String = RSCM.getReverseMapping(RSCMType.NPC, row.npc.id)
        val npcId: Int = row.npc.id
        val level: Int = row.level
        val xp: Double = row.xpTenths / 10.0
        val trapping: String = RSCM.getReverseMapping(RSCMType.LOC, row.trappingLocs[0].id)
        val trappingMirrored: String = RSCM.getReverseMapping(RSCMType.LOC, row.trappingLocs[1].id)
        val full: String = RSCM.getReverseMapping(RSCMType.LOC, row.fullLoc.id)
        val loot: List<String> = row.loot.map { it.internalName }
        val low: Int = row.low
        val high: Int = row.high
        val bait: String = row.bait.internalName
        val approachSeq: String = RSCM.getReverseMapping(RSCMType.SEQ, row.approachSeq.id)
        val respawnTicks: Int = row.respawnTicks
        val fullSynth: String = RSCM.getReverseMapping(RSCMType.SYNTH, row.fullSynth)
    }

    internal companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_deadfalls"
        const val BOULDER = "loc.hunting_deadfall_boulder"
        const val SETTING = "loc.hunting_deadfall_setting"
        const val TRAP = "loc.hunting_deadfall_trap"
        const val FAILING = "loc.hunting_deadfall_failing"
        const val SET_SEQ = "seq.hunting_setting_trap_small"
        const val SET_SYNTH = "synth.hunting_deadfall_set"
        const val TRAPPING_SYNTH = "synth.hunting_deadfall_trapping"
        const val CHECK_SYNTH = "synth.hunting_trap_dismantle"
        val KNIVES = listOf("obj.knife", "obj.fletching_knife")
        val REFUSED_LOGS = setOf("obj.redwood_logs", "obj.arctic_pine_log")
        const val LIMIT = 2
        const val LOG_KEEP_DENOMINATOR = 4
        const val ATTEMPT_START = 8
        const val LURE_RANGE = 5
        const val APPROACH_TIMEOUT = 8
        const val SETTLE_TICKS = 2
        const val CHECK_TICKS = 2
        const val COLLAPSE_TICKS = 100
    }
}
