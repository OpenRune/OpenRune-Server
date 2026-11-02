package org.rsmod.content.skills.hunter

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.hit.modifier.StandardPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.table.FiremakingLogsRow
import org.rsmod.api.table.hunter.HunterPitfallsRow
import org.rsmod.content.skills.hunter.HunterPouch.Companion.pouchHunted
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.catchChance256
import org.rsmod.content.skills.hunter.HunterTrapScript.Companion.distance
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@Singleton
class PitfallScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val routes: RouteFactory,
    private val collision: CollisionFlagMap,
    private val areas: AreaChecker,
    private val random: GameRandom,
    private val clock: MapClock,
    private val rumours: HunterRumours,
    private val limits: HunterTrapLimits,
    private val hitModifier: StandardPlayerHitModifier,
) : PluginScript() {
    private val creatures: List<Creature> by lazy { HunterPitfallsRow.all().map(::Creature) }
    private val pits: List<Pit> by lazy { (1..PIT_COUNT).map(::pit) }
    private val hunts = HashMap<Player, Hunt>()
    private val logs: List<String> by lazy {
        FiremakingLogsRow.all()
            .sortedBy { row -> row.statReq.firstOrNull()?.t1 ?: 0 }
            .map { it.input.internalName }
            .filter { it !in DeadfallScript.REFUSED_LOGS }
            .distinct()
    }

    override fun ScriptContext.startup() {
        limits.register { player -> hunts[player]?.pits?.count { it.value.state != PitState.Empty } ?: 0 }
        for (pit in pits) {
            onOpLoc3(pit.loc) { trap(pit, it.loc) }
            onOpLoc1(pit.loc) { jump(pit, it.loc) }
            onOpLoc2(pit.loc) { dismantle(pit) }
        }
        for (creature in creatures) onOpNpc1(creature.npc) { tease(it.npc, creature) }
        onPlayerSoftTimer(TIMER) { player.tick() }
        onPlayerLogout { hunts.remove(player)?.let { release(it) } }
    }

    private fun pit(index: Int): Pit {
        val loc = "loc.hunting_pitfall_$index"
        val type = ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC))
        val full = type?.transforms?.getOrNull(STATE_FULL)
        val kinds = creatures.filter { full != null && full in it.fullLocIds }
        return Pit(index, loc, "varbit.hunt_pitfall_state$index", kinds)
    }

    private fun hunt(player: Player): Hunt = hunts.getOrPut(player) { Hunt() }

    private suspend fun ProtectedAccess.trap(pit: Pit, bound: BoundLocInfo) {
        if (vars[pit.varbit] != STATE_EMPTY) return
        val level = pit.kinds.minOfOrNull { it.level } ?: return
        if (stat(HUNTER) < level) {
            mes("You need a Hunter level of $level to set a pitfall trap here.")
            return
        }
        val log = logs.firstOrNull { it in inv }
        if (log == null || DeadfallScript.KNIVES.none { it in inv || it in player.worn }) {
            mes("You need some logs and a knife to set a pitfall trap.")
            return
        }
        if (!limits.canLay(player, areas)) return
        anim(SET_SEQ)
        if (invDel(inv, log).failure) return
        delay(1)
        VarPlayerIntMapSetter.set(player, pit.varbit, STATE_SPIKED)
        soundSynth(SET_SYNTH)
        hunt(player).pits[pit.index] = PitTimer(PitState.Spiked, clock.cycle, bound.coords)
        player.softTimer(TIMER, 1)
    }

    private suspend fun ProtectedAccess.jump(pit: Pit, bound: BoundLocInfo) {
        if (vars[pit.varbit] != STATE_SPIKED) return
        if (jumpPath(bound.coords, player.coords) == null) {
            val side = sideTiles(bound.coords).minByOrNull { distance(it, player.coords) } ?: return
            if (!walkOnto(side)) return
        }
        val (start, dest, dir) = jumpPath(bound.coords, player.coords) ?: return
        if (player.coords != start) return
        faceSquare(dest)
        exactMove(start, dest, JUMP_DELAY1, JUMP_DELAY2, dir.angle, TeleportType.Exempt)
        anim(JUMP_SEQ, delay = JUMP_SEQ_DELAY)
        worldRepo.soundArea(start, JUMP_SYNTH)
        val chase = hunt(player).chase ?: return
        chase.crossing = Crossing(pit, bound.coords, dir, chase.npc.size, clock.cycle)
    }

    private suspend fun ProtectedAccess.dismantle(pit: Pit) {
        val state = vars[pit.varbit]
        if (state == STATE_SPIKED) {
            anim(SET_SEQ)
            delay(1)
            if (vars[pit.varbit] != STATE_SPIKED) return
            VarPlayerIntMapSetter.set(player, pit.varbit, STATE_EMPTY)
            hunt(player).pits.remove(pit.index)
            mes("You dismantle the trap.", ChatType.Spam)
            return
        }
        if (state != STATE_FULL && state != STATE_FULL_180) return
        val timer = hunt(player).pits[pit.index] ?: return
        val creature = timer.caught ?: return
        val loot = rollLoot(creature)
        if (!canAdd(loot)) {
            mes("You don't have enough inventory space.")
            return
        }
        mes("You dismantle the trap.", ChatType.Spam)
        anim(SET_SEQ)
        delay(1)
        if (vars[pit.varbit] != state || !canAdd(loot)) return
        VarPlayerIntMapSetter.set(player, pit.varbit, STATE_EMPTY)
        hunt(player).pits.remove(pit.index)
        for ((obj, count) in loot) if (!pouchHunted(obj, count)) invAdd(inv, obj, count)
        statAdvance(HUNTER, creature.xp.toDouble())
        soundSynth(DISMANTLE_SYNTH)
        rumours.onCatch(player, creature.npc)
        mes("You've caught ${HunterRumours.article(creature.name)} ${creature.name}!", ChatType.Spam)
    }

    private fun ProtectedAccess.canAdd(items: List<Pair<String, Int>>): Boolean {
        val into = inv
        return player
            .invTransaction(into, autoCommit = false) {
                val target = select(into)
                for ((obj, count) in items) add(target, obj.asRSCM(RSCMType.OBJ), count)
            }
            .success
    }

    private fun ProtectedAccess.rollLoot(creature: Creature): List<Pair<String, Int>> {
        val loot = creature.loot.map { it to 1 }.toMutableList()
        if (creature.furs.size == 2) {
            val perfect = random.of(256) < catchChance256(FUR_LOW, FUR_HIGH, stat(HUNTER))
            loot += (if (perfect) creature.furs[0] else creature.furs[1]) to 1
        }
        creature.extra?.let { (obj, min, max) -> loot += obj to random.of(min, max) }
        return loot
    }

    private fun ProtectedAccess.tease(npc: Npc, creature: Creature) {
        if (TEASING_STICK !in inv && TEASING_STICK !in player.worn && SPEAR !in player.worn) {
            mes("You need a teasing stick or a hunter's spear to tease this creature.")
            return
        }
        val hunt = hunt(player)
        val current = hunt.chase
        if (current != null && current.npc !== npc && current.npc.isSlotAssigned && current.npc.isVisible) {
            mes("I'm already under attack!")
            return
        }
        if (hunts.values.any { (it !== hunt && it.chase?.npc === npc) || it.pits.values.any { pit -> pit.npc === npc } }) return
        faceEntitySquare(npc)
        anim(TEASE_SEQ)
        soundSynth(TEASE_SYNTH)
        npc.mode = NpcMode.None
        npc.facePlayer(player)
        hunt.chase = Chase(npc, creature, clock.cycle + FIRST_ATTACK_TICKS)
        player.softTimer(TIMER, 1)
    }

    private fun Player.tick() {
        val hunt = hunts[this]
        if (hunt == null || (hunt.chase == null && hunt.pits.isEmpty())) {
            hunts.remove(this)
            clearSoftTimer(TIMER)
            return
        }
        val now = clock.cycle
        hunt.chase?.let { chaseTick(hunt, it, now) }
        for ((index, timer) in hunt.pits.toList()) {
            val pit = pits[index - 1]
            val age = now - timer.cycle
            when (timer.state) {
                PitState.Spiked ->
                    if (age >= SPIKED_COLLAPSE_TICKS && vars[pit.varbit] == STATE_SPIKED) {
                        VarPlayerIntMapSetter.set(this, pit.varbit, STATE_EMPTY)
                        hunt.pits.remove(index)
                        mes(COLLAPSE_TEXT)
                    }
                PitState.Falling ->
                    if (age >= FALL_TICKS) {
                        VarPlayerIntMapSetter.set(this, pit.varbit, timer.fullState)
                        timer.npc?.let { removeFallen(it, timer.caught!!) }
                        worldRepo.soundArea(timer.coords, timer.caught!!.caughtSynth)
                        hunt.pits[index] = timer.copy(state = PitState.Full, cycle = now, npc = null)
                    }
                PitState.Full ->
                    if (age >= FULL_COLLAPSE_TICKS) {
                        if (vars[pit.varbit] == timer.fullState) VarPlayerIntMapSetter.set(this, pit.varbit, STATE_EMPTY)
                        hunt.pits.remove(index)
                        mes(COLLAPSE_TEXT)
                    }
                PitState.Empty -> hunt.pits.remove(index)
            }
        }
    }

    private fun Player.chaseTick(hunt: Hunt, chase: Chase, now: Int) {
        val npc = chase.npc
        if (!npc.isSlotAssigned || !npc.isVisible || distance(npc.coords, coords) > CHASE_RANGE || npc.coords.level != coords.level) {
            release(hunt)
            return
        }
        val crossing = chase.crossing
        if (crossing != null) return crossTick(hunt, chase, crossing, now)
        val adjacent = HunterTrapScript.cardinal(coords).any { tile -> npc.coords == tile } ||
            distance(npc.coords, coords) <= npc.size
        if (!adjacent) {
            if (now % 2 == 0) npc.walkTo(routes, coords)
            return
        }
        npc.facePlayer(this)
        if (now < chase.nextAttack) return
        chase.nextAttack = now + ATTACK_TICKS
        npc.anim(chase.creature.attackSeq)
        worldRepo.soundArea(coords, chase.creature.attackSynth)
        val reduction = gearReduction(this, chase.creature)
        val damage = random.of(0, chase.creature.maxHit) * (100 - reduction) / 100
        queueHit(npc, 1, HitType.Melee, damage, hitModifier)
    }

    private fun Player.crossTick(hunt: Hunt, chase: Chase, crossing: Crossing, now: Int) {
        val npc = chase.npc
        val pitCoords = crossing.pitCoords
        when (crossing.stage) {
            CrossStage.Approach -> {
                val edge = crossing.edge
                if (npc.coords != edge) {
                    if (now - crossing.started > CROSS_TIMEOUT) {
                        chase.crossing = null
                        return
                    }
                    npc.walkTo(routes, edge)
                    return
                }
                crossing.stage = CrossStage.Decide
                crossing.started = now
            }
            CrossStage.Decide -> {
                if (now - crossing.started < DECIDE_TICKS) return
                val creature = chase.creature
                val level = statBase(HUNTER)
                val spear = if (SPEAR in worn) SPEAR_BONUS else 0
                val chance = HunterOutfit.applyTo(catchChance256(creature.low, creature.high, level), this) + spear
                val repeat = chase.lastLeapt == crossing.pit.index
                val caught =
                    !repeat && level >= creature.level && creature in crossing.pit.kinds &&
                        vars[crossing.pit.varbit] == STATE_SPIKED && random.of(256) < chance
                if (!caught) return leap(chase, crossing)
                npc.teleport(collision, pitCoords)
                crossing.stage = CrossStage.OnPit
                crossing.started = now
            }
            CrossStage.OnPit -> {
                if (now - crossing.started < ON_PIT_TICKS) return
                val creature = chase.creature
                VarPlayerIntMapSetter.set(this, crossing.pit.varbit, STATE_FALLING)
                val reversed = crossing.dir == Dir.East || crossing.dir == Dir.North
                npc.spotanim(if (reversed) COLLAPSE_SPOT_180 else COLLAPSE_SPOT)
                npc.anim(creature.fallSeq)
                worldRepo.soundArea(pitCoords, FALL_SYNTH)
                hunt.pits[crossing.pit.index] =
                    PitTimer(PitState.Falling, now, pitCoords, creature, if (reversed) STATE_FULL_180 else STATE_FULL, npc)
                hunt.chase = null
            }
            CrossStage.Leaping -> {
                if (now - crossing.started < LEAP_TICKS) return
                npc.telejump(collision, crossing.landing)
                chase.crossing = null
            }
        }
    }

    private fun leap(chase: Chase, crossing: Crossing) {
        val npc = chase.npc
        npc.faceSquare(crossing.pitCoords, PIT_SIZE, PIT_SIZE)
        npc.anim(LEAP_SEQ, delay = LEAP_SEQ_DELAY)
        val landing = crossing.landing
        for (spot in chase.creature.leapSpotanims) {
            worldRepo.projAnim(
                ProjAnim(spot.asRSCM(RSCMType.SPOTANIM), 0, 0, LEAP_PROJ_START, LEAP_PROJ_END, 0, 0, 0, 0, npc.coords, landing)
            )
        }
        worldRepo.soundArea(npc.coords, LEAP_SYNTH)
        chase.lastLeapt = crossing.pit.index
        crossing.stage = CrossStage.Leaping
        crossing.started = clock.cycle
    }

    private fun release(hunt: Hunt) {
        for (timer in hunt.pits.values) timer.npc?.let { removeFallen(it, timer.caught!!) }
        val chase = hunt.chase ?: return
        hunt.chase = null
        if (chase.npc.isSlotAssigned) chase.npc.defaultMode()
    }

    private fun removeFallen(npc: Npc, creature: Creature) {
        if (!npc.isSlotAssigned) return
        npc.defaultMode()
        npcRepo.despawn(npc, creature.respawnTicks)
    }

    private fun gearReduction(player: Player, creature: Creature): Int =
        creature.gear.filter { (pieces, _) -> pieces.all { it in player.worn } }.maxOfOrNull { it.second } ?: 0

    private fun sideTiles(pit: CoordGrid): List<CoordGrid> =
        (0 until PIT_SIZE).flatMap {
            listOf(
                pit.translate(-1, it),
                pit.translate(PIT_SIZE, it),
                pit.translate(it, -1),
                pit.translate(it, PIT_SIZE),
            )
        }

    private fun jumpPath(pit: CoordGrid, from: CoordGrid): Triple<CoordGrid, CoordGrid, Dir>? {
        val inX = from.x in pit.x until pit.x + PIT_SIZE
        val inZ = from.z in pit.z until pit.z + PIT_SIZE
        return when {
            inZ && from.x == pit.x - 1 -> Triple(from, from.translate(JUMP_LENGTH, 0), Dir.East)
            inZ && from.x == pit.x + PIT_SIZE -> Triple(from, from.translate(-JUMP_LENGTH, 0), Dir.West)
            inX && from.z == pit.z - 1 -> Triple(from, from.translate(0, JUMP_LENGTH), Dir.North)
            inX && from.z == pit.z + PIT_SIZE -> Triple(from, from.translate(0, -JUMP_LENGTH), Dir.South)
            else -> null
        }
    }

    enum class Dir(val angle: Int, val dx: Int, val dz: Int) {
        North(1024, 0, 1),
        East(1536, 1, 0),
        South(0, 0, -1),
        West(512, -1, 0),
    }

    enum class PitState {
        Empty,
        Spiked,
        Falling,
        Full,
    }

    enum class CrossStage {
        Approach,
        Decide,
        OnPit,
        Leaping,
    }

    data class PitTimer(
        val state: PitState,
        val cycle: Int,
        val coords: CoordGrid,
        val caught: Creature? = null,
        val fullState: Int = STATE_FULL,
        val npc: Npc? = null,
    )

    class Crossing(val pit: Pit, val pitCoords: CoordGrid, val dir: Dir, size: Int, var started: Int) {
        var stage: CrossStage = CrossStage.Approach
        val edge: CoordGrid =
            when (dir) {
                Dir.East -> pitCoords.translate(-size, 0)
                Dir.West -> pitCoords.translate(PIT_SIZE, 0)
                Dir.North -> pitCoords.translate(0, -size)
                Dir.South -> pitCoords.translate(0, PIT_SIZE)
            }
        val landing: CoordGrid =
            when (dir) {
                Dir.East -> pitCoords.translate(PIT_SIZE, 0)
                Dir.West -> pitCoords.translate(-size, 0)
                Dir.North -> pitCoords.translate(0, PIT_SIZE)
                Dir.South -> pitCoords.translate(0, -size)
            }
    }

    class Chase(val npc: Npc, val creature: Creature, var nextAttack: Int) {
        var crossing: Crossing? = null
        var lastLeapt: Int = -1
    }

    class Hunt {
        val pits = HashMap<Int, PitTimer>()
        var chase: Chase? = null
    }

    class Pit(val index: Int, val loc: String, val varbit: String, val kinds: List<Creature>)

    class Creature(row: HunterPitfallsRow) {
        val name: String = row.name
        val npc: String = RSCM.getReverseMapping(RSCMType.NPC, row.npc.id)
        val level: Int = row.level
        val xp: Int = row.xp
        val fullLocIds: Set<Int> = row.fullLocs.map { it.id }.toSet()
        val loot: List<String> = row.loot.map { it.internalName }
        val furs: List<String> = row.furs.map { it.internalName }
        val maxHit: Int = row.maxHit
        val attackSeq: String = RSCM.getReverseMapping(RSCMType.SEQ, row.attackSeq.id)
        val attackSynth: String = RSCM.getReverseMapping(RSCMType.SYNTH, row.attackSynth)
        val fallSeq: String = RSCM.getReverseMapping(RSCMType.SEQ, row.fallSeq.id)
        val caughtSynth: String = RSCM.getReverseMapping(RSCMType.SYNTH, row.caughtSynth)
        val leapSpotanims: List<String> = row.leapSpotanims.filter { it.isNotEmpty() }
        val respawnTicks: Int = row.respawnTicks
        val low: Int = row.low
        val high: Int = row.high
        val extra: Triple<String, Int, Int>? = row.extra.firstOrNull()?.let { Triple(it.t0.internalName, it.t1, it.t2) }
        val gear: List<Pair<List<String>, Int>> =
            row.gear.map { listOf(it.t0.internalName, it.t1.internalName, it.t2.internalName) to it.t3 }
    }

    companion object {
        const val HUNTER = "stat.hunter"
        const val TIMER = "timer.hunter_pitfalls"
        const val TEASING_STICK = "obj.hunting_teasing_stick"
        const val SPEAR = "obj.hg_hunter_spear"
        const val SET_SEQ = "seq.hunting_setting_trap_small"
        const val TEASE_SEQ = "seq.hunting_teasing_animal"
        const val JUMP_SEQ = "seq.agility_pyramid_gap_jump"
        const val LEAP_SEQ = "seq.hunter_bigcat_leap_npc"
        const val COLLAPSE_SPOT = "spotanim.hunting_pitfall_collapse_0"
        const val COLLAPSE_SPOT_180 = "spotanim.hunting_pitfall_collapse_2"
        const val SET_SYNTH = "synth.hunting_pitfall_set"
        const val DISMANTLE_SYNTH = "synth.hunting_pitfall_dismantle"
        const val TEASE_SYNTH = "synth.hunting_tease"
        const val JUMP_SYNTH = "synth.hunting_pitfall_jump"
        const val FALL_SYNTH = "synth.hunting_pitfall_collapse"
        const val LEAP_SYNTH = "synth.hunting_pitfall_leap"
        const val COLLAPSE_TEXT = "The pitfall trap that you constructed has collapsed."
        const val PIT_COUNT = 25
        const val PIT_SIZE = 2
        const val STATE_EMPTY = 0
        const val STATE_SPIKED = 1
        const val STATE_FALLING = 2
        const val STATE_FULL = 3
        const val STATE_FULL_180 = 4
        const val JUMP_LENGTH = 3
        const val LEAP_LENGTH = 4
        const val JUMP_DELAY1 = 53
        const val JUMP_DELAY2 = 85
        const val JUMP_SEQ_DELAY = 20
        const val LEAP_SEQ_DELAY = 20
        const val LEAP_PROJ_START = 53
        const val LEAP_PROJ_END = 90
        const val FIRST_ATTACK_TICKS = 3
        const val ATTACK_TICKS = 4
        const val CHASE_RANGE = 15
        const val CROSS_TIMEOUT = 10
        const val DECIDE_TICKS = 2
        const val ON_PIT_TICKS = 2
        const val FALL_TICKS = 3
        const val LEAP_TICKS = 2
        const val SPIKED_COLLAPSE_TICKS = 300
        const val FULL_COLLAPSE_TICKS = 142
        const val SPEAR_BONUS = 13
        const val FUR_LOW = 96
        const val FUR_HIGH = 320
    }
}
