package org.rsmod.content.bosses.leviathan

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.max
import org.rsmod.api.bossbar.plugin.BossHpBarScript
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.Angles
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.clearOwnedLocs
import org.rsmod.api.bosses.runtime.clearOwnedNpcs
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.removeLocs
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.route.RayCastValidator
import org.rsmod.api.route.RouteFactory
import org.rsmod.api.route.walkTo
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.util.EntityTinting
import org.rsmod.game.hit.HitType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class Leviathan
@Inject
internal constructor(
    deps: BossDeps,
    private val fights: LeviathanFights,
    private val npcDeath: NpcDeath,
) : BossPluginScript(deps) {

    private val biteAnim = oneOf(anim(BITE_ANIM_1), anim(BITE_ANIM_2))

    override val spec: BossSpec =
        boss(LeviathanFights.BOSS_NPC) {
            stats(retaliateOnHit = false)

            val bite =
                ability("bite") {
                    include(biteAnim)
                    hit {
                        damage(0..BITE_MAX_HIT).roll()
                        type(Melee)
                    }
                }

            val volleyRockfall = LeviathanRockfall.rockfall(animated = true, withHints = true)
            val volleyStages =
                LeviathanOrbs.VOLLEY_STAGES.map {
                    LeviathanOrbs.volleyStage(it, volleyRockfall, LeviathanRockfall.RECOVERY)
                }
            val volley = ability("volley") { include(switch(LeviathanVarns.VOLLEY_STAGE, volleyStages)) }

            val arenaPlayers = playersIn(LeviathanRockfall.ARENA)
            val tailsFlinch = external(LeviathanFights.TAILS_FLINCH_EXT)
            val enrageAnnouncement =
                sequence(message(ENRAGE_MESSAGE, arenaPlayers), soundTo(PATHFINDER_SPAWN_SYNTH, arenaPlayers))

            val stun =
                ability("stun") {
                    interrupt()
                    addVarn(LeviathanVarns.STUNS, 1)
                    setVarn(
                        LeviathanVarns.VOLLEY_STAGE,
                        varn(LeviathanVarns.VOLLEY_STAGE) atMost varn(LeviathanVarns.STUNS),
                    )
                    setVarn(LeviathanVarns.STUN_BEARING, bearingTo(CurrentTarget))
                    setVarn(LeviathanVarns.STUNNED, 1)
                    faceTile(CurrentTargetTile)
                    anim(STUN_SEQ)
                    idleAnim(STUN_IDLE_SEQ)
                    include(tailsFlinch)
                    message(STUN_MESSAGE, arenaPlayers)
                    wait(STUN_TICKS)
                    setVarn(LeviathanVarns.STUNNED, 0)
                    clearIdleAnim()
                    resetAnim()
                    forceNext(volley)
                }

            val lightning = ability("lightning", LeviathanSpecials.lightning(volley))
            val smoke = ability("smoke", LeviathanSpecials.smoke(volley))

            val weakSpot =
                ability("weak_spot") {
                    interrupt()
                    setVarn(LeviathanVarns.STUNNED, 0)
                    setVarn(LeviathanVarns.IN_SPECIAL, 1)
                    message(WEAK_SPOT_MESSAGE, arenaPlayers)
                    wait(1)
                    clearIdleAnim()
                    anim(STUN_SEQ)
                    include(tailsFlinch)
                    include(LeviathanSpecials.start(lightning, smoke))
                }

            val stunned = varnIs(LeviathanVarns.STUNNED, 1)
            val behindStunBearing =
                targetInArc(LeviathanVarns.STUN_BEARING, offset = Angles.FULL_TURN / 2, halfArc = WEAK_SPOT_ARC)
            val outsideAura = Condition.Custom(fights::outsideAura)
            val insideAura = Condition.Not(Condition.Custom(fights::outsideAura))

            onIncomingHit(
                stun,
                withObj = SHADOW_SPELLS,
                requires =
                    InPhase(FIGHT_PHASE) and
                        !stunned and
                        varnIs(LeviathanVarns.IN_SPECIAL, 0) and
                        varnAtLeast(LeviathanVarns.VOLLEYS, 1),
            )

            incoming {
                rule(stunned and behindStunBearing) {
                    run(weakSpot)
                    floorPercentOfMaxHit(WEAK_SPOT_MIN_PERCENT, Ranged)
                }
                rule(stunned and behindStunBearing) {
                    run(weakSpot)
                    scalePercent(200, Magic)
                }
                rule(stunned) { cap(STUNNED_DAMAGE_CAP) }
                rule(varnIs(LeviathanVarns.IN_SPECIAL, 1)) { scalePercent(SPECIAL_DAMAGE_PERCENT) }
                rule(InPhase(ENRAGED_PHASE) and insideAura) { scalePercent(INSIDE_AURA_DAMAGE_PERCENT) }
            }

            phase(FIGHT_PHASE) {
                weightedSelectorRandom(noRepeatBias = 0.0) {
                    +random(bite, weight = 1, requires = targetWithin(BITE_RANGE) and !lastAbility(bite))
                    +random(volley, weight = 1)
                }
            }

            val enrageEntry =
                ability("enrage_entry") {
                    interrupt()
                    setVarn(LeviathanVarns.STUNNED, 0)
                    setVarn(LeviathanVarns.IN_SPECIAL, 0)
                    clearIdleAnim()
                    include(external(LeviathanFights.SUMMON_PATHFINDER_EXT))
                    include(after(PATHFINDER_SPAWN_DELAY, enrageAnnouncement))
                    include(LeviathanRockfall.rockfall(animated = true, withHints = false))
                    wait(LeviathanRockfall.RECOVERY)
                }
            val enragedRockfall =
                ability("enraged_rockfall", LeviathanRockfall.rockfall(animated = false, withHints = false))
            val enragedOrb = ability("enraged_orb", LeviathanOrbs.enragedOrb(penetrationWhen = outsideAura))

            phase(
                ENRAGED_PHASE,
                entryHp = LeviathanFights.ENRAGE_HP_FRACTION,
                attackRate = LeviathanFights.ENRAGED_INTERVAL,
            ) {
                entry = enrageEntry.name
                weightedSelectorRandom(noRepeatBias = 0.0) { +random(enragedOrb, weight = 1) }
                forceEvery(LeviathanFights.ENRAGED_ROCKFALL_INTERVAL, enragedRockfall)
            }
        }

    override fun ScriptContext.startup() {
        val type = ServerCacheManager.getNpc(LeviathanFights.BOSS_NPC.asRSCM(RSCMType.NPC))!!

        fights.registerExtensions()
        BossCombat.register(
            this,
            spec,
            deps,
            onHit = { deps.worldRepo.soundArea(npc, HIT_SYNTH, radius = HIT_SYNTH_RADIUS) },
        )

        onNpcQueue(type, "queue.death") {
            val session = fights.sessionOf(npc)
            val dropCoords = fights.dropCoords(npc)
            fights.onDeath(npc)
            npcDeath.deathWithDrops(this, dropCoords)
            session?.let(fights::afterKill)
        }

        onEvent<NpcStateEvents.Delete> { fights.onDeleted(npc) }
    }

    private companion object {
        private const val FIGHT_PHASE = "fight"
        private const val ENRAGED_PHASE = "enraged"
        private const val HIT_SYNTH = "synth.leviathan_hit"
        private const val HIT_SYNTH_RADIUS = 15
        private const val BITE_MAX_HIT = 50
        private const val BITE_RANGE = 5
        private const val BITE_ANIM_1 = "seq.npc_leviathan_01_melee_01"
        private const val BITE_ANIM_2 = "seq.npc_leviathan_01_melee_02"

        private const val STUN_SEQ = "seq.npc_leviathan_01_projectile_end_01"
        private const val STUN_IDLE_SEQ = "seq.npc_leviathan_01_stun_idle"
        private const val STUN_TICKS = 15
        private const val STUNNED_DAMAGE_CAP = 10
        private const val WEAK_SPOT_ARC = 650
        private const val WEAK_SPOT_MIN_PERCENT = 65
        private const val STUN_MESSAGE = "<col=06600c>Your spell stuns the Leviathan!</col>"
        private const val WEAK_SPOT_MESSAGE = "<col=06600c>You hit the Leviathan right in its weak spot!</col>"
        private const val SPECIAL_DAMAGE_PERCENT = 67
        private const val INSIDE_AURA_DAMAGE_PERCENT = 150
        private const val ENRAGE_MESSAGE = "<col=ff289d>The Leviathan focuses on you intensely...</col>"
        private const val PATHFINDER_SPAWN_SYNTH = "synth.leviathan_pathfinder_spawn"
        private const val PATHFINDER_SPAWN_DELAY = LeviathanFights.PATHFINDER_SPAWN_DELAY

        private val SHADOW_SPELLS =
            listOf(
                "obj.52_shadow_rush",
                "obj.64_shadow_burst",
                "obj.76_shadow_blitz",
                "obj.88_shadow_barrage",
            )
    }
}

@Singleton
internal class LeviathanFights
@Inject
constructor(
    private val deps: BossDeps,
    private val instances: InstanceManager,
    private val arenaLocs: LeviathanArenaLocs,
    private val rayCast: RayCastValidator,
    private val routeFactory: RouteFactory,
    private val hpBar: BossHpBarScript,
    private val aiPlayerInteractions: AiPlayerInteractions,
) {
    private val fights: MutableMap<Npc, LeviathanFight> = IdentityHashMap()
    private val pendingSpawns: MutableSet<Long> = HashSet()

    fun registerExtensions() {
        deps.extensionRegistry.register(SUMMON_PATHFINDER_EXT) { _, npc, _, _ -> summonPathfinder(npc) }
        deps.extensionRegistry.register(TAILS_FLINCH_EXT) { _, npc, _, _ -> fights[npc]?.let(::animateTails) }
        deps.extensionRegistry.register(BEAM_AIM_EXT) { _, npc, target, _ -> aimBeam(npc, target) }
        deps.extensionRegistry.register(BEAM_WINDUP_EXT) { _, npc, target, _ ->
            turnBeam(npc, target, LIGHTNING_WINDUP_TURN)
        }
        deps.extensionRegistry.register(BEAM_FIRE_EXT) { _, npc, target, _ ->
            fights[npc]?.let(::fireBeam)
            turnBeam(npc, target, LIGHTNING_BEAM_TURN)
        }
        deps.extensionRegistry.register(SMOKE_WAVE_EXT) { _, npc, _, _ -> fights[npc]?.let(::smokeWave) }
    }

    fun hasBoss(session: InstanceSession): Boolean =
        session.id.value in pendingSpawns || bossOf(session) != null

    fun bossOf(session: InstanceSession): Npc? =
        fights.keys.firstOrNull { it.isSlotAssigned && instances.instanceForNpc(it) == session.id }

    fun scheduleSpawn(session: InstanceSession, player: Player, delay: Int) {
        if (!pendingSpawns.add(session.id.value)) return
        deps.worldQueues.add(delay) {
            pendingSpawns.remove(session.id.value)
            if (instances.sessionForPlayer(player)?.id != session.id) return@add
            if (bossOf(session) != null) return@add
            spawn(session, player)
        }
    }

    private fun spawn(session: InstanceSession, player: Player) {
        val coords = instances.resolveCoord(session, LeviathanArena.BOSS_SPAWN) ?: return
        val arena = Arena.forBoss(coords)
        if (!arena.inSearchBox(player.coords) || arenaLocs.isOnIsland(arena, player.coords)) return

        val type = ServerCacheManager.getNpc(BOSS_NPC.asRSCM(RSCMType.NPC)) ?: return
        val npc = Npc(type, coords)
        npc.movementLocked = true
        npc.apRequiresLineOfSight = false
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        instances.registerSessionNpc(player, npc)

        val fight = LeviathanFight(npc, arena)
        fights[npc] = fight

        npc.faceSquare(arena.at(RISE_FACE))
        npc.anim(RISE_SEQ)
        player.soundSynth(RISE_SYNTH)
        hpBar.onOpen(player, npc)
        deps.suppressAttacks(npc, FIRST_VOLLEY_DELAY)
        deps.worldQueues.add(1) { if (npc.isSlotAssigned) refacePlayer(npc, player) }
        npc.apPlayer2(player, aiPlayerInteractions)
        watchAbandonment(fight)
    }

    private fun watchAbandonment(fight: LeviathanFight) {
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (fight.ended || !fight.npc.isSlotAssigned) return@repeatTick false
                if (arenaLocs.arenaPlayers(fight).isEmpty()) {
                    fight.absentTicks++
                    if (fight.absentTicks >= ABANDON_TICKS) {
                        abandon(fight)
                        return@repeatTick false
                    }
                } else {
                    fight.absentTicks = 0
                }
                true
            },
        )
    }

    private fun refacePlayer(npc: Npc, target: Player) {
        npc.resetFaceEntity()
        npc.facePlayer(target)
    }

    private fun animateTails(fight: LeviathanFight) {
        sessionOf(fight.npc)?.let(arenaLocs::animateTails)
    }

    private fun aimBeam(npc: Npc, target: Player) {
        val fight = fights[npc] ?: return
        fight.specialAngle = Angles.bearing(fight.arena.centre, target.coords)
        npc.resetFaceEntity()
    }

    private fun turnBeam(npc: Npc, target: Player, maxStep: Int) {
        val fight = fights[npc] ?: return
        val live = if (target.isValidTarget()) target else arenaLocs.arenaPlayers(fight).firstOrNull()
        val wanted = live?.let { Angles.bearing(fight.arena.centre, it.coords) } ?: fight.specialAngle
        fight.specialAngle = turnTowards(fight.specialAngle, wanted, maxStep)
        npc.infoProtocol.setFaceAngle(fight.specialAngle, instant = false)
    }

    private fun turnTowards(current: Int, wanted: Int, maxStep: Int): Int {
        val delta = Angles.delta(wanted, current).coerceIn(-maxStep, maxStep)
        return (current + delta + Angles.FULL_TURN) % Angles.FULL_TURN
    }

    private fun fireBeam(fight: LeviathanFight) {
        val centre = fight.arena.centre
        val angle = fight.specialAngle
        val struck = HashSet<CoordGrid>()
        val groundSpot = SpotanimType(BEAM_GROUND_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        for (depth in 1..LIGHTNING_BEAM_LENGTH) {
            val width = 1 + (depth - 1) / LIGHTNING_WIDEN_EVERY
            val forward = Angles.step(centre, angle, depth.toDouble())
            val tiles = mutableListOf(forward)
            for (side in 1..(width - 1)) {
                val offset = (side + 1) / 2
                val perpendicular = angle + if (side % 2 == 1) PERPENDICULAR else -PERPENDICULAR
                tiles += Angles.step(forward, perpendicular, offset.toDouble())
            }
            for (tile in tiles) {
                if (width > 1 && !fight.arena.inSearchBox(tile)) continue
                if (!struck.add(tile)) continue
                deps.worldRepo.spotanimMap(groundSpot, tile, delay = BEAM_GROUND_BASE_DELAY + depth * BEAM_GROUND_STEP_DELAY)
            }
        }
        for (player in arenaLocs.arenaPlayers(fight)) {
            player.soundSynth(LIGHTNING_SHOT_SYNTH)
            if (player.coords in struck) {
                player.queueHit(fight.npc, 1, HitType.Typeless, deps.random.of(LIGHTNING_DAMAGE), deps.playerHitModifier)
            }
        }
    }

    private fun smokeWave(fight: LeviathanFight) {
        val npc = fight.npc
        val centre = fight.arena.centre
        for (tile in arenaLocs.freeArenaTiles(fight)) {
            if (!hasLineOfSight(fight, tile)) continue
            val spot = SpotanimType(LeviathanArena.smokeSpotanim(centre, tile).asRSCM(RSCMType.SPOTANIM))
            deps.worldRepo.spotanimMap(spot, tile, delay = LeviathanArena.smokeDelay(centre, tile))
        }
        for (player in arenaLocs.arenaPlayers(fight)) {
            val arrival = LeviathanArena.smokeDelay(centre, player.coords) / CYCLES_PER_TICK
            deps.worldQueues.add(max(0, arrival)) {
                if (fight.ended || !player.isValidTarget() || !hasLineOfSight(fight, player.coords)) return@add
                player.queueHit(npc, 1, HitType.Typeless, deps.random.of(0, SMOKE_MAX_HIT), deps.playerHitModifier)
            }
        }
    }

    private fun hasLineOfSight(fight: LeviathanFight, tile: CoordGrid): Boolean =
        rayCast.hasLineOfSight(
            source = fight.npc.coords,
            destination = tile,
            srcWidth = LeviathanArena.BOSS_SIZE,
            srcLength = LeviathanArena.BOSS_SIZE,
        )

    private fun summonPathfinder(npc: Npc) {
        val fight = fights[npc] ?: return
        val corner = pathfinderStartCorner(fight)
        fight.pathfinderCorner = corner
        val sw = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[corner])
        val middle = sw.translate(1, 1)
        for (step in 0 until PATHFINDER_SPAWN_PULSES) {
            val delay = step * CYCLES_PER_TICK
            (listOf(0 to 0) + CARDINALS).forEach { (dx, dz) ->
                mapSpot(PATHFINDER_SPAWN_SPOTANIM, middle.translate(dx, dz), delay = delay, height = PATHFINDER_SPAWN_HEIGHT)
            }
        }
        deps.worldQueues.add(PATHFINDER_SPAWN_DELAY) { spawnPathfinder(fight, sw) }
    }

    private fun pathfinderStartCorner(fight: LeviathanFight): Int {
        val encounter = deps.encounter(fight.npc)
        return LeviathanArena.PATHFINDER_CORNERS.indices.firstOrNull { index ->
            val sw = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[index])
            (0 until LeviathanArena.PATHFINDER_SIZE).all { dx ->
                (0 until LeviathanArena.PATHFINDER_SIZE).all { dz -> !encounter.ownsLocAt(sw.translate(dx, dz)) }
            }
        } ?: 0
    }

    private fun spawnPathfinder(fight: LeviathanFight, sw: CoordGrid) {
        if (fight.ended || !fight.npc.isSlotAssigned) return
        val pathfinder = deps.spawnOwnedNpc(fight.npc, PATHFINDER_NPC, sw) ?: return
        pathfinder.mode = NpcMode.None
        pathfinder.anim(PATHFINDER_SPAWN_SEQ)
        fight.pathfinder = pathfinder
        val next = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[(fight.pathfinderCorner + 1) % 4])
        pathfinder.faceSquare(next)
        deps.worldQueues.add(PATHFINDER_WALK_DELAY) { patrol(fight) }
        watchAura(fight)
    }

    private fun patrol(fight: LeviathanFight) {
        val pathfinder = fight.pathfinder ?: return
        if (fight.ended || !pathfinder.isSlotAssigned) return
        fight.pathfinderCorner = (fight.pathfinderCorner + 1) % LeviathanArena.PATHFINDER_CORNERS.size
        val dest = fight.arena.at(LeviathanArena.PATHFINDER_CORNERS[fight.pathfinderCorner])
        pathfinder.walkTo(routeFactory, dest) { patrol(fight) }
    }

    private fun watchAura(fight: LeviathanFight) {
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                if (fight.ended || fight.pathfinder?.isSlotAssigned != true) return@repeatTick false
                arenaLocs.arenaPlayers(fight).filter { insideAura(fight, it) }.forEach { it.tint(AURA_TINT) }
                true
            },
        )
    }

    fun outsideAura(npc: Npc, player: Player?): Boolean {
        val fight = fights[npc] ?: return false
        return player != null && !insideAura(fight, player)
    }

    private fun insideAura(fight: LeviathanFight, player: Player): Boolean {
        val pathfinder = fight.pathfinder ?: return false
        if (!pathfinder.isSlotAssigned) return false
        val border = AURA_BORDER
        val sw = pathfinder.coords
        val size = LeviathanArena.PATHFINDER_SIZE
        return player.coords.x in (sw.x - border)..(sw.x + size - 1 + border) &&
            player.coords.z in (sw.z - border)..(sw.z + size - 1 + border)
    }

    fun onDeath(npc: Npc) {
        val fight = fights[npc] ?: return
        deps.worldRepo.soundArea(fight.arena.centre, DEATH_SYNTH, delay = DEATH_SYNTH_DELAY, radius = 15)
        end(fight)
        val rubble = deps.encounter(npc).releaseOwnedLocs()
        deps.worldQueues.add(RUBBLE_CLEAR_DELAY) { deps.removeLocs(rubble, RUBBLE_BREAK_SPOTANIM) }
    }

    private fun abandon(fight: LeviathanFight) {
        end(fight)
        deps.clearOwnedLocs(fight.npc, RUBBLE_BREAK_SPOTANIM)
        if (fight.npc.isSlotAssigned) deps.npcRepo.del(fight.npc, Int.MAX_VALUE)
    }

    fun onDeleted(npc: Npc) {
        fights.remove(npc)?.ended = true
    }

    private fun end(fight: LeviathanFight) {
        fight.ended = true
        fight.pathfinder = null
        deps.clearOwnedNpcs(fight.npc)
    }

    fun sessionOf(npc: Npc): InstanceSession? = instances.instanceForNpc(npc)?.let(instances::sessionForId)

    fun dropCoords(npc: Npc): CoordGrid {
        val fight = fights[npc] ?: return npc.coords
        return arenaLocs.arenaPlayers(fight).firstOrNull()?.coords ?: npc.coords
    }

    fun afterKill(session: InstanceSession) {
        arenaLocs.setBoat(session, escape = false)
        arenaLocs.setHandholds(session, LeviathanArenaLocs.HANDHOLDS_EXIT_LOC)
        deps.worldQueues.add(RESPAWN_TICKS) {
            if (bossOf(session) != null || session.id.value in pendingSpawns) return@add
            val coords = instances.resolveCoord(session, LeviathanArena.BOSS_SPAWN) ?: return@add
            val arena = Arena.forBoss(coords)
            val player =
                deps.playerList.firstOrNull {
                    it.isValidTarget() &&
                        instances.sessionForPlayer(it)?.id == session.id &&
                        arena.inSearchBox(it.coords) &&
                        !arenaLocs.isOnIsland(arena, it.coords)
                } ?: return@add
            beginEncounter(session, player, delay = 0)
        }
    }

    fun beginEncounter(session: InstanceSession, player: Player, delay: Int) {
        arenaLocs.setHandholds(session, LeviathanArenaLocs.HANDHOLDS_SEALED_LOC)
        arenaLocs.setBoat(session, escape = true)
        if (delay <= 0) {
            if (bossOf(session) == null) spawn(session, player)
        } else {
            scheduleSpawn(session, player, delay)
        }
    }

    fun setHandholds(session: InstanceSession, loc: String) = arenaLocs.setHandholds(session, loc)

    fun placeArenaLocs(session: InstanceSession) = arenaLocs.placeArenaLocs(session, hasBoss(session))

    fun ensureTails(session: InstanceSession) = arenaLocs.ensureTails(session)

    private fun mapSpot(spot: String, tile: CoordGrid, delay: Int = 0, height: Int = 0) {
        deps.worldRepo.spotanimMap(SpotanimType(spot.asRSCM(RSCMType.SPOTANIM)), tile, height, delay)
    }

    companion object {
        const val BOSS_NPC = "npc.leviathan"

        const val SUMMON_PATHFINDER_EXT = "leviathan.summon_pathfinder"
        const val TAILS_FLINCH_EXT = "leviathan.tails_flinch"
        const val BEAM_AIM_EXT = "leviathan.beam_aim"
        const val BEAM_WINDUP_EXT = "leviathan.beam_windup"
        const val BEAM_FIRE_EXT = "leviathan.beam_fire"
        const val SMOKE_WAVE_EXT = "leviathan.smoke_wave"

        private const val RESPAWN_TICKS = 30
        private const val CYCLES_PER_TICK = 30

        private val RISE_FACE = CoordGrid(2064, 6369, 0)
        private const val RISE_SEQ = "seq.npc_leviathan_01_spawn"
        private const val RISE_SYNTH = "synth.leviathan_rise"
        private const val FIRST_VOLLEY_DELAY = 4
        private const val ABANDON_TICKS = 5

        private const val RUBBLE_BREAK_SPOTANIM = "spotanim.projanim_brain_01_impact_02"
        private const val RUBBLE_CLEAR_DELAY = 7

        private val CARDINALS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

        private const val LIGHTNING_SHOT_SYNTH = "synth.leviathan_lightning_shot"
        private const val BEAM_GROUND_SPOTANIM = "spotanim.vfx_leviathan_360_ground"
        private const val BEAM_GROUND_BASE_DELAY = 2
        private const val BEAM_GROUND_STEP_DELAY = 2
        private const val LIGHTNING_WINDUP_TURN = 300
        private const val LIGHTNING_BEAM_TURN = 100
        private const val LIGHTNING_BEAM_LENGTH = 15
        private const val LIGHTNING_WIDEN_EVERY = 3
        private const val PERPENDICULAR = 512
        private val LIGHTNING_DAMAGE = 20..30

        private const val SMOKE_MAX_HIT = 60

        const val ENRAGE_HP_FRACTION = 0.2
        const val ENRAGED_ROCKFALL_INTERVAL = 8
        const val ENRAGED_INTERVAL = 2
        private const val AURA_BORDER = 1
        private val AURA_TINT =
            EntityTinting(startCycle = 0, endCycle = 30, hue = 9, saturation = 7, lightness = 70, weight = 70)

        private const val PATHFINDER_NPC = "npc.leviathan_buff_npc"
        private const val PATHFINDER_SPAWN_SEQ = "seq.npc_buff01_spawn01"
        private const val PATHFINDER_SPAWN_SPOTANIM = "spotanim.leviathan_buff_spawn"
        private const val PATHFINDER_SPAWN_PULSES = 4
        private const val PATHFINDER_SPAWN_HEIGHT = 124
        const val PATHFINDER_SPAWN_DELAY = 3
        private const val PATHFINDER_WALK_DELAY = 3

        private const val DEATH_SYNTH = "synth.leviathan_death"
        private const val DEATH_SYNTH_DELAY = 9
    }
}
