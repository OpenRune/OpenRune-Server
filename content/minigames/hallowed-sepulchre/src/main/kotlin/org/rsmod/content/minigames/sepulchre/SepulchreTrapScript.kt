package org.rsmod.content.minigames.sepulchre

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.MoveRestrict
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hit.modifier.PlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The statues of every floor. A floor's traps run on its own clock, which starts on the first
 * tick a player is timed on the empty floor and stops when the last one leaves, as live's floor
 * instances do; every player on the floor shares it. At the start of each tick the statues move
 * on and anyone standing in fire, under a sword or on a bolt's path is hit, falls and is sent
 * back to their last checkpoint. Lightning does no damage; it stuns whoever it strikes.
 */
class SepulchreTrapScript
@Inject
constructor(
    private val runs: SepulchreRuns,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val collision: CollisionFlagMap,
    private val mapClock: MapClock,
    private val random: GameRandom,
    private val hitModifier: PlayerHitModifier,
) : PluginScript() {
    private lateinit var floors: List<FloorTraps>

    override fun ScriptContext.startup() {
        val traps = Trap.loadAll().groupBy { it.floor }
        floors =
            SepulchreFloors.load().all.map { floor ->
                FloorTraps(floor, traps[floor.number].orEmpty())
            }
        onEvent<GameLifecycle.StartCycle> { floors.forEach(::tick) }
        onEvent<GameLifecycle.LateCycle> { floors.forEach(::strike) }
        onPlayerQueue(FALL_QUEUE) { fall() }
        onPlayerQueue(STUN_QUEUE) { stun() }
        onPlayerQueue(STUN_RECOVERED_QUEUE) { resetSpotanim() }
    }

    private fun tick(floor: FloorTraps) {
        val timed = runs.onFloor(RunPhase.Start, floor.floor.number)
        if (timed.isEmpty()) {
            floor.stop()
            return
        }
        val base = floor.base ?: mapClock.cycle.also { floor.base = it }
        val t = mapClock.cycle - base
        for (trap in floor.traps) {
            when (trap.kind) {
                TrapKind.Flames -> flames(floor, trap, t, timed)
                TrapKind.Sword -> sword(floor, trap, t, timed)
                TrapKind.Bolts -> bolts(floor, trap, t, timed)
                TrapKind.Lightning -> lightning(floor, trap, t)
            }
        }
    }

    /**
     * Hits whoever ends this tick where a trap will reach on the next one, so the hit shows on the
     * tick the trap gets there, as live's does; lightning stuns instead of hitting.
     */
    private fun strike(floor: FloorTraps) {
        val base = floor.base ?: return
        val next = mapClock.cycle - base
        val hazards = HashSet<CoordGrid>()
        val strikes = HashSet<CoordGrid>()
        for (trap in floor.traps) {
            val since = TrapTiming.sinceAttack(next, trap.period, trap.phases)
            when (trap.kind) {
                TrapKind.Flames ->
                    if (TrapTiming.flameBurns(since, trap.attack)) {
                        trap.fires.forEach { hazards += it.tiles }
                    }
                TrapKind.Sword -> hazards += swordReach(trap, next)
                TrapKind.Bolts -> hazards += boltReach(floor, trap, next)
                TrapKind.Lightning ->
                    if (TrapTiming.lightningStuns(since)) {
                        strikes += trap.strikeTiles
                    }
            }
        }
        for (player in runs.onFloor(RunPhase.Late, floor.floor.number)) {
            if (player.currentFloor != floor.floor.number || player.busy) {
                continue
            }
            if (player.coords in hazards) {
                hit(player, floor.floor)
            } else if (player.coords in strikes && !player.frozen) {
                player.strongQueue(STUN_QUEUE, 1)
            }
        }
    }

    private fun hit(player: Player, floor: SepulchreFloor) {
        val fallDelay = if (player.hasMovedThisCycle) FALL_AFTER_MOVE else FALL_STANDING
        player.busy = true
        player.abortRoute()
        player.queueHit(1, HitType.Typeless, random.of(floor.damage), hitModifier)
        player.soundSynth(random.pick(HIT_SYNTHS), delay = HIT_SYNTH_DELAY)
        player.strongQueue(FALL_QUEUE, fallDelay)
    }

    private fun swordReach(trap: Trap, t: Int): List<CoordGrid> {
        val since = Math.floorMod(t - trap.phases.first(), trap.period)
        val now =
            TrapTiming.swordOffset(since, trap.reach, trap.speed, trap.hold)
                ?: return emptyList()
        val launch = trap.launches.single()
        if (since == 0) {
            return footprint(launch, SWORD_SIZE)
        }
        val before = TrapTiming.swordOffset(since - 1, trap.reach, trap.speed, trap.hold) ?: now
        return swept(trap.along(launch, before), trap.along(launch, now), SWORD_SIZE)
    }

    private fun boltReach(floor: FloorTraps, trap: Trap, t: Int): List<CoordGrid> {
        val tiles = mutableListOf<CoordGrid>()
        for (bolt in floor.bolts) {
            if (bolt.trap !== trap) {
                continue
            }
            val since = t - bolt.fired
            val now = TrapTiming.boltOffset(since, trap.reach, trap.speed) ?: continue
            val before = TrapTiming.boltOffset(since - 1, trap.reach, trap.speed) ?: 0
            val launch = trap.launches[bolt.lane]
            tiles += swept(trap.along(launch, before), trap.along(launch, now), BOLT_SIZE)
        }
        if (Math.floorMod(t - trap.phases.first(), trap.period) == 0) {
            floor.volleys[trap]?.forEach { tiles += trap.launches[it] }
        }
        return tiles
    }

    private fun flames(floor: FloorTraps, trap: Trap, t: Int, timed: List<Player>) {
        val since = TrapTiming.sinceAttack(t, trap.period, trap.phases)
        val until = TrapTiming.untilAttack(t, trap.period, trap.phases)
        val statueSeq = TrapTiming.flameStatueSeq(since, until, trap.attack)
        trap.statues.forEach { floor.anim(it, statueSeq) }
        val fireSeq = TrapTiming.fireSeq(since, trap.attack)
        trap.fires.forEach { floor.anim(it.coords, fireSeq) }
        if (since == 0) {
            nearby(timed, trap.statues).forEach {
                it.soundSynth(STATUE_MOVE)
                it.soundSynth(random.pick(FIRE_SYNTHS), delay = SOUND_DELAY)
            }
        }
    }

    private fun sword(floor: FloorTraps, trap: Trap, t: Int, timed: List<Player>) {
        val statue = trap.statues.single()
        val since = Math.floorMod(t - trap.phases.first(), trap.period)
        val until = Math.floorMod(trap.phases.first() - t, trap.period)
        val life = TrapTiming.swordLife(trap.reach, trap.speed, trap.hold)
        when {
            since == 0 -> floor.anim(statue, SWORD_ATTACK)
            since == life -> floor.anim(statue, SWORD_CATCH)
            until == 2 -> floor.anim(statue, SWORD_READY)
            until == 1 -> floor.anim(statue, SWORD_START)
        }
        if (since == 1) {
            floor.loc(statue)?.let { loc ->
                trap.thrown?.let { locRepo.change(loc, it, life + 1) }
            }
        }
        val launch = trap.launches.single()
        val offset = TrapTiming.swordOffset(since, trap.reach, trap.speed, trap.hold)
        val sword = floor.swords[trap]
        if (offset == null) {
            sword?.let { floor.remove(it) }
            floor.swords -= trap
            return
        }
        if (since == 0) {
            sword?.let { floor.remove(it) }
            val spawned = spawn(trap, launch)
            floor.swords[trap] = spawned
            glide(spawned, launch, trap.along(launch, minOf(trap.speed, trap.reach)), trap)
            nearby(timed, trap.statues).forEach { it.soundSynth(SWORD_THROW) }
            return
        }
        val npc = sword ?: return
        val dest = trap.along(launch, offset)
        val from = npc.coords
        if (dest != from) {
            npc.teleport(collision, dest)
            npc.pendingExactMove = exactMove(from, dest, dest, trap)
        }
        npc.anim(SWORD_SPIN)
        if (dest != from) {
            nearby(timed, listOf(dest)).forEach { it.soundSynth(random.pick(SWORD_WOOSH)) }
        }
    }

    private fun bolts(floor: FloorTraps, trap: Trap, t: Int, timed: List<Player>) {
        val since = Math.floorMod(t - trap.phases.first(), trap.period)
        val until = Math.floorMod(trap.phases.first() - t, trap.period)
        if (until == READY_LEAD || floor.volleys[trap] == null) {
            floor.volleys[trap] = pickVolley(trap)
        }
        val chosen = floor.volleys.getValue(trap)
        for ((index, statue) in trap.statues.withIndex()) {
            val seq =
                when {
                    index !in chosen -> BOLT_IDLE
                    until == READY_LEAD -> BOLT_READY
                    until == 1 -> BOLT_START
                    since == 0 -> BOLT_ATTACK
                    since == 1 -> BOLT_FINISH
                    else -> BOLT_IDLE
                }
            floor.anim(statue, seq)
        }
        if (since == 0) {
            for (lane in chosen) {
                val launch = trap.launches[lane]
                val bolt = spawn(trap, launch)
                glide(bolt, launch, trap.along(launch, minOf(trap.speed, trap.reach)), trap)
                floor.bolts += LiveBolt(trap, lane, t, bolt)
            }
            nearby(timed, trap.statues).forEach { it.soundSynth(random.pick(ARROW_STATUE_MOVE)) }
            floor.volleys -= trap
        }
        val iterator = floor.bolts.iterator()
        while (iterator.hasNext()) {
            val bolt = iterator.next()
            if (bolt.trap !== trap || bolt.fired == t) {
                continue
            }
            val launch = trap.launches[bolt.lane]
            val offset = TrapTiming.boltOffset(t - bolt.fired, trap.reach, trap.speed)
            if (offset == null) {
                floor.remove(bolt.npc)
                iterator.remove()
                continue
            }
            val from = bolt.npc.coords
            val dest = trap.along(launch, offset)
            bolt.npc.teleport(collision, dest)
            bolt.npc.pendingExactMove = exactMove(from, dest, dest, trap)
            bolt.npc.anim(BOLT_FLIGHT)
            nearby(timed, listOf(dest)).forEach { it.soundSynth(random.pick(BOLT_SYNTHS)) }
        }
    }

    private fun pickVolley(trap: Trap): List<Int> {
        val pool = trap.statues.indices.toMutableList()
        return List(minOf(trap.volley, pool.size)) { pool.removeAt(random.of(pool.size)) }
    }

    private fun lightning(floor: FloorTraps, trap: Trap, t: Int) {
        val since = TrapTiming.sinceAttack(t, trap.period, trap.phases)
        val seq = TrapTiming.lightningStatueSeq(since)
        trap.statues.forEach { floor.anim(it, seq) }
        val ground = TrapTiming.lightningGround(since, trap.period) ?: return
        val type = SpotanimType(ground.asRSCM(RSCMType.SPOTANIM))
        val delay = if (ground == TrapTiming.STRIKE_GLOW) STRIKE_GLOW_DELAY else 0
        trap.strikeTiles.forEach { worldRepo.spotanimMap(type, it, delay = delay) }
    }

    private fun ProtectedAccess.stun() {
        mes("You are stunned!", ChatType.Spam)
        anim(STUNNED)
        spotanim(STUNNED_SPOTANIM, height = STUNNED_HEIGHT)
        player.frozen = true
        player.abortRoute()
        player.timer(FREEZE_TIMER, STUN_TICKS)
        player.queue(STUN_RECOVERED_QUEUE, STUNNED_SPOTANIM_TICKS)
    }

    private fun spawn(trap: Trap, at: CoordGrid): Npc {
        val npc = Npc(checkNotNull(trap.npc), at)
        npc.mode = NpcMode.None
        npc.moveRestrict = MoveRestrict.PassThru
        npcRepo.add(npc, Int.MAX_VALUE)
        npc.anim(if (trap.kind == TrapKind.Sword) SWORD_SPIN else BOLT_FLIGHT)
        return npc
    }

    private fun glide(npc: Npc, from: CoordGrid, to: CoordGrid, trap: Trap) {
        npc.pendingExactMove = exactMove(from, to, npc.coords, trap)
    }

    private fun exactMove(
        from: CoordGrid,
        to: CoordGrid,
        at: CoordGrid,
        trap: Trap,
    ): EntityExactMove =
        EntityExactMove(
            deltaX1 = from.x - at.x,
            deltaZ1 = from.z - at.z,
            deltaX2 = to.x - at.x,
            deltaZ2 = to.z - at.z,
            clientDelay1 = 0,
            clientDelay2 = GLIDE_CYCLES,
            direction = angle(trap.heading, from, to),
        )

    private fun angle(heading: Pair<Int, Int>, from: CoordGrid, to: CoordGrid): Int {
        val dx = if (to.x != from.x) Integer.signum(to.x - from.x) else heading.first
        val dz = if (to.z != from.z) Integer.signum(to.z - from.z) else heading.second
        return when {
            dx > 0 -> FACE_EAST
            dx < 0 -> FACE_WEST
            dz > 0 -> FACE_NORTH
            else -> FACE_SOUTH
        }
    }

    private fun footprint(at: CoordGrid, size: Int): List<CoordGrid> =
        (0 until size).flatMap { dx -> (0 until size).map { dz -> at.translate(dx, dz) } }

    private fun swept(from: CoordGrid, to: CoordGrid, size: Int): List<CoordGrid> {
        val steps = Trap.distance(from, to)
        val dx = Integer.signum(to.x - from.x)
        val dz = Integer.signum(to.z - from.z)
        return (0..steps).flatMap { footprint(from.translate(dx * it, dz * it), size) }
    }

    private fun nearby(timed: List<Player>, sources: List<CoordGrid>): List<Player> =
        timed.filter { player ->
            sources.any {
                it.level == player.level && player.coords.chebyshevDistance(it) <= HEARING
            }
        }

    /**
     * The fall after a trap hit, a tick after the hit lands, or two when the player had just
     * stepped onto the trap: they drop and come round at their respawn.
     */
    private suspend fun ProtectedAccess.fall() {
        val floor = floors.firstOrNull { it.floor.number == player.currentFloor }?.floor
        if (floor == null) {
            player.busy = false
            return
        }
        try {
            anim(DEATH)
            ifCloseSub(OVERLAY)
            ifOpenFullOverlay(FADE)
            runClientScript(FADE_LEGACY.asRSCM(RSCMType.CLIENTSCRIPT))
            delay(2)
            telejump(respawn(floor), TeleportType.Exempt)
            resetAnim()
            delay(1)
            fadeOverlay(0, CLEAR, 0, OPAQUE, FADE_CYCLES)
            delay(2)
            ifCloseSub(FADE)
            if (!SepulchreClock.isExpired(player.timeRemaining) && player.currentFloor != 0) {
                ifOpenOverlay(OVERLAY, OVERLAY_TARGET)
            }
        } finally {
            player.busy = false
        }
    }

    private fun ProtectedAccess.respawn(floor: SepulchreFloor): CoordGrid {
        val checkpoint = player.checkpoint
        if (checkpoint in 1..floor.checkpoints.size) {
            return floor.checkpoints[checkpoint - 1].respawn
        }
        return floor.starts.getOrElse(player.startIndex) { floor.starts.first() }
    }

    private inner class FloorTraps(val floor: SepulchreFloor, val traps: List<Trap>) {
        var base: Int? = null
        val swords = HashMap<Trap, Npc>()
        val bolts = ArrayList<LiveBolt>()
        val volleys = HashMap<Trap, List<Int>>()
        private val locs = HashMap<CoordGrid, LocInfo?>()

        fun loc(coords: CoordGrid): LocInfo? =
            locs.getOrPut(coords) { locRepo.findExact(coords, LocShape.CentrepieceStraight) }

        fun anim(coords: CoordGrid, seq: String) {
            loc(coords)?.let { worldRepo.locAnim(it, seq) }
        }

        fun remove(npc: Npc) {
            if (npc.isSlotAssigned) {
                npcRepo.del(npc, Int.MAX_VALUE)
            }
        }

        fun stop() {
            if (base == null) {
                return
            }
            swords.values.forEach(::remove)
            bolts.forEach { remove(it.npc) }
            swords.clear()
            bolts.clear()
            volleys.clear()
            locs.clear()
            base = null
        }
    }

    private class LiveBolt(val trap: Trap, val lane: Int, val fired: Int, val npc: Npc)

    private companion object {
        const val SWORD_SIZE = 3
        const val BOLT_SIZE = 1
        const val READY_LEAD = 2
        const val GLIDE_CYCLES = 30
        const val HEARING = 10
        const val SOUND_DELAY = 30
        const val HIT_SYNTH_DELAY = 50
        const val FALL_STANDING = 2
        const val FALL_AFTER_MOVE = 3
        const val FALL_QUEUE = "queue.hallowed_trap_hit"
        const val FACE_SOUTH = 0
        const val FACE_WEST = 512
        const val FACE_NORTH = 1024
        const val FACE_EAST = 1536
        const val OPAQUE = 255
        const val CLEAR = 0
        const val FADE_CYCLES = 50

        const val FADE = "interface.fade_overlay"
        const val DEATH = "seq.human_death"
        const val SWORD_READY = "seq.hallowed_statue_sword_ready"
        const val SWORD_START = "seq.hallowed_statue_sword_start_attack"
        const val SWORD_ATTACK = "seq.hallowed_statue_sword_attack"
        const val SWORD_CATCH = "seq.hallowed_statue_sword_catch"
        const val SWORD_SPIN = "seq.hallowed_statue_sword_spin"
        const val BOLT_IDLE = "seq.hallowed_statue_projectile_idle"
        const val BOLT_READY = "seq.hallowed_statue_projectile_ready"
        const val BOLT_START = "seq.hallowed_statue_projectile_start_attack"
        const val BOLT_ATTACK = "seq.hallowed_statue_projectile_attack"
        const val BOLT_FINISH = "seq.hallowed_statue_projectile_finish_attack"
        const val BOLT_FLIGHT = "seq.hallowed_statue_bolt"
        const val STRIKE_GLOW_DELAY = 30
        const val STUN_QUEUE = "queue.hallowed_lightning_stun"
        const val STUN_RECOVERED_QUEUE = "queue.hallowed_stun_recovered"
        const val STUNNED = "seq.human_stunned"
        const val STUNNED_SPOTANIM = "spotanim.stunned"
        const val STUNNED_HEIGHT = 124
        const val STUNNED_SPOTANIM_TICKS = 2
        const val STUN_TICKS = 3
        const val FREEZE_TIMER = "timer.combat_freeze"

        const val STATUE_MOVE = "synth.hal_sep_statue_move_1"
        const val SWORD_THROW = "synth.hal_sep_sword_statue_throw"

        val FIRE_SYNTHS =
            listOf(
                "synth.hal_sep_firetourch_1",
                "synth.hal_sep_firetourch_2",
                "synth.hal_sep_firetourch_3",
                "synth.hal_sep_firetourch_4",
                "synth.hal_sep_firetourch_5",
                "synth.hal_sep_firetourch_6",
                "synth.hal_sep_firetourch_7",
            )
        val SWORD_WOOSH =
            listOf(
                "synth.hal_sep_sword_woosh_1",
                "synth.hal_sep_sword_woosh_2",
                "synth.hal_sep_sword_woosh_3",
            )
        val ARROW_STATUE_MOVE =
            listOf(
                "synth.hal_sep_arrow_statue_move_1",
                "synth.hal_sep_arrow_statue_move_2",
                "synth.hal_sep_arrow_statue_move_3",
            )
        val BOLT_SYNTHS =
            listOf(
                "synth.hal_sep_bolt_projectile_1",
                "synth.hal_sep_bolt_projectile_2",
                "synth.hal_sep_bolt_projectile_3",
            )
        val HIT_SYNTHS =
            listOf(
                "synth.human_hit_1",
                "synth.human_hit_2",
                "synth.human_hit_3",
                "synth.human_hit_4",
            )
    }
}
