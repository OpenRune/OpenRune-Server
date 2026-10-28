package org.rsmod.content.bosses.royaltitans

import com.github.michaelbull.logging.InlineLogger
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.timer.formatInstanceKillTime
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.headbar.Headbar as EngineHeadbar
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.loc.LocLayerConstants

/**
 * Runs one Royal Titans instance: the phase two transition and elemental blast cadence, the survivor
 * state, damage contribution, the kill messages and the corpse/respawn cycle.
 */
@Singleton
class RoyalTitansEncounter
@Inject
constructor(
    private val deps: BossDeps,
    private val manager: InstanceManager,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val patrol: RoyalTitansPatrol,
    private val walls: RoyalTitansWalls,
    private val elementals: RoyalTitansElementals,
    private val loot: RoyalTitansLoot,
    private val worldStats: RoyalTitansWorldStats,
) {
    private val logger = InlineLogger()
    private val runs = HashMap<InstanceId, Run>()

    fun start(session: InstanceSession, queen: Npc, king: Npc, player: Player) {
        val existing = runs[session.id]
        // Instance ids can be reused. A run from an older session must never be adopted by a new
        // instance: it still points at the old npcs and keeps the old phase/loc state.
        val run =
            if (existing != null && existing.session === session) {
                existing
            } else {
                existing?.let(::discard)
                Run(session, queen, king).also { runs[session.id] = it }
            }
        if (!run.started) {
            run.started = true
            run.fightStart = deps.mapClock.cycle
            run.phaseAnchor =
                resolveAnchor(run.queen.spawnCoords)
                    ?: resolveAnchor(run.king.spawnCoords)
                    ?: resolveExitAnchor(run.queen.spawnCoords)
            if (run.phaseAnchor != null) {
                for ((titan, element) in listOf(run.queen to TitanElement.Fire, run.king to TitanElement.Ice)) {
                    if (!titan.isSlotAssigned) continue
                    val spawn = run.spawnFor(element)
                    titan.spawnCoords = spawn
                    PathingEntityCommon.teleport(titan, deps.collision, spawn)
                }
            }
            for (titan in listOf(run.queen, run.king)) {
                if (!titan.isSlotAssigned) continue
                val element = TitanElement.of(titan) ?: continue
                titan.anim(element.spawnSeq)
                element.titanSpawnSpot?.let { titan.spotanim(it) }
            }
        }
        engage(run, player)
        val firstAttack = deps.mapClock.cycle + FIRST_ATTACK_DELAY
        for (titan in listOf(run.queen, run.king)) {
            if (!titan.isSlotAssigned) continue
            val encounter = deps.encounterRegistry.of(titan)
            encounter.busyUntil = firstAttack
            encounter.nextAttackTick = firstAttack
        }
        if (!run.loopRunning) {
            run.loopRunning = true
            tick(run)
        }
    }

    fun runOf(npc: Npc): Run? = manager.instanceForNpc(npc)?.let(runs::get)

    fun isSurvivor(npc: Npc): Boolean {
        val run = runOf(npc) ?: return false
        val element = TitanElement.of(npc) ?: return false
        return run.survivorElement == element
    }

    fun isBlasting(npc: Npc): Boolean {
        val run = runOf(npc) ?: return false
        return deps.mapClock.cycle < run.pulseUntil
    }

    /**
     * The hidden ranged accuracy boost the titans have whenever they are out of melee reach: while
     * patrolling backed off in phase one, and for the duration of the elemental blast in phase two.
     */
    fun rangedAccuracyMultiplier(npc: Npc): Double {
        if (TitanElement.of(npc) == null) return 1.0
        val run = runOf(npc) ?: return 1.0
        if (run.phase == PHASE_TWO) {
            return if (isBlasting(npc)) RANGED_ACCURACY_BOOST else 1.0
        }
        return if (atFront(npc)) 1.0 else RANGED_ACCURACY_BOOST
    }

    private fun atFront(npc: Npc): Boolean {
        val spawn = npc.spawnCoords
        return if (npc.isType("npc.rt_fire_queen")) {
            npc.coords.x >= spawn.x
        } else {
            npc.coords.x <= spawn.x
        }
    }

    fun recordDamage(player: Player, npc: Npc, damage: Int) {
        if (damage <= 0) return
        val run = runOf(npc) ?: return
        val uuid = player.uuid ?: return
        run.damage.merge(uuid, damage, Int::plus)
    }

    /** The phase-two geometry anchor for this npc, falling back to its actual spawn tile. */
    fun spawnFor(npc: Npc): CoordGrid {
        val run = runOf(npc) ?: return npc.spawnCoords
        val element = TitanElement.of(npc) ?: return npc.spawnCoords
        return run.spawnFor(element)
    }

    /**
     * Finds the arena's north mouth from the two standing torches that frame it. The instanced
     * npc spawns can be offset from the copied map, so this map-anchored position is what phase
     * two's teleports and blocking objects must use.
     */
    private fun resolveAnchor(spawn: CoordGrid): CoordGrid? {
        val torches = mutableListOf<CoordGrid>()
        for (x in spawn.x - 12..spawn.x + 12) {
            for (z in spawn.z - 2..spawn.z + 16) {
                val tile = CoordGrid(x, z, spawn.level)
                if (deps.locRepo.findAll(tile).any { it.id == ANCHOR_TORCH }) {
                    torches += tile
                }
            }
        }
        if (torches.size < 2) return null
        val northRow = torches.maxOf { it.z }
        val row = torches.filter { it.z == northRow }
        if (row.size < 2) return null
        val centerX = (row.minOf { it.x } + row.maxOf { it.x }) / 2
        return CoordGrid(centerX, northRow, spawn.level)
    }

    /** Fallback anchor from the instance exit tunnel, which sits four tiles north of the mouth. */
    private fun resolveExitAnchor(spawn: CoordGrid): CoordGrid? {
        for (x in spawn.x - 8..spawn.x + 12) {
            for (z in spawn.z + 4..spawn.z + 16) {
                val tile = CoordGrid(x, z, spawn.level)
                if (deps.locRepo.findAll(tile).any { it.id == ANCHOR_EXIT }) {
                    return CoordGrid(tile.x, tile.z - 4, spawn.level)
                }
            }
        }
        return null
    }

    /** Keeps every landed phase-two titan facing its sibling instead of turning to its target. */
    fun faceEachOther(npc: Npc) {
        val run = runOf(npc) ?: return
        faceEachOther(run)
    }

    private fun faceEachOther(run: Run) {
        if (run.phase != PHASE_TWO || run.queenDead || run.kingDead) return
        for (element in run.landed) {
            faceTitan(run, element)
        }
    }

    private fun faceTitan(run: Run, element: TitanElement) {
        val npc = if (element == TitanElement.Fire) run.queen else run.king
        val dead = if (element == TitanElement.Fire) run.queenDead else run.kingDead
        if (dead || !npc.isSlotAssigned) return
        // Packet capture: Branda faces south (angle 0), Eldric faces north (angle 1024). The
        // post-tick hook applies this override instantly each tick, so the combat AI cannot turn
        // them towards the player.
        npc.resetFaceEntity()
        npc.pendingFaceAngle = EntityFaceAngle.NULL
        npc.faceAngleOverride =
            if (element == TitanElement.Fire) FIRE_FACE_ANGLE else ICE_FACE_ANGLE
    }

    /** Keeps a titan facing across the corridor while it waits for its delayed phase-two entry. */
    private fun setWaitFacing(npc: Npc, element: TitanElement) {
        npc.resetFaceEntity()
        npc.pendingFaceAngle = EntityFaceAngle.NULL
        npc.faceAngleOverride =
            if (element == TitanElement.Fire) FIRE_WAIT_FACE_ANGLE else ICE_WAIT_FACE_ANGLE
    }

    fun onTitanDeath(npc: Npc) {
        val run = runOf(npc) ?: return
        val element = TitanElement.of(npc) ?: return
        if (element == TitanElement.Fire) {
            if (run.queenDead) return
            run.queenDead = true
        } else {
            if (run.kingDead) return
            run.kingDead = true
        }
        deps.encounterRegistry.remove(npc)
        patrol.stop(npc)
        run.pendingEntry -= element
        npc.faceAngleOverride = null
        logger.info {
            "[royal-titans] ${element.displayName} defeated " +
                "(queenDead=${run.queenDead} kingDead=${run.kingDead})"
        }
        if (run.queenDead && run.kingDead) {
            run.survivorElement = null
            clearPulse(run)
            onPairKilled(run)
            return
        }
        val survivor = element.opposite
        run.survivorElement = survivor
        val survivorNpc = if (survivor == TitanElement.Fire) run.queen else run.king
        if (
            run.phase == PHASE_TWO &&
                run.nextPulse == 0 &&
                aliveTitans(run).all { TitanElement.of(it) in run.landed }
        ) {
            run.nextPulse = deps.mapClock.cycle + FIRST_PULSE_DELAY
        }
        deps.worldQueues.add(SURVIVOR_SPEECH_DELAY) {
            if (survivorNpc.isSlotAssigned) {
                survivorNpc.say(survivor.survivorSpeech(deps.random.of(2)))
            }
        }
    }

    fun onLoot(player: Player, npc: Npc, mode: RoyalTitansLootMode) {
        val run = runOf(npc) ?: return
        val element = TitanElement.of(npc) ?: return
        logger.info {
            "[royal-titans] loot request ${element.name} mode=$mode by=${player.username} " +
                "queenDead=${run.queenDead} kingDead=${run.kingDead}"
        }
        if (!run.queenDead || !run.kingDead) {
            return
        }
        val uuid = player.uuid ?: return
        if (!run.looted.add(uuid)) {
            player.mes(
                "You do not feel worthy enough to loot ${element.displayName}.",
                ChatType.GameMessage,
            )
            return
        }
        if (!run.lootStarted) {
            run.lootStarted = true
            if (!run.fadeStarted) {
                run.respawnAt = deps.mapClock.cycle + RESPAWN_TICKS
            }
            broadcast(
                run,
                "<col=ef1020>The Royal Titans will reinvigorate themselves in 18 seconds.</col>",
            )
        }
        val percent = contributionPercent(run, uuid)
        val rolled = loot.roll(element, mode, player, percent)
        for (entry in rolled) {
            loot.give(player, npc.visType.id, run.lootEventId, entry)
        }
        if (mode == RoyalTitansLootMode.Sacrifice && rolled.none { it.item.obj.endsWith("pet") }) {
            player.mes(
                "You don't feel lucky during your sacrifice",
                ChatType.GameMessage,
            )
        }
    }

    /** Whether this player would receive loot from [npc] right now. */
    fun canLoot(player: Player, npc: Npc): Boolean {
        val run = runOf(npc) ?: return false
        if (!run.queenDead || !run.kingDead) return false
        val uuid = player.uuid ?: return false
        return uuid !in run.looted
    }

    /**
     * Phase two is entered from two places: the 35% auto transition for the titan that crossed it,
     * and [PHASE_TWO_ENTRY_ABILITY] for both titans once their current attack cycle ends. OSRS
     * waits for that cycle: in the stagger capture the ice king crossed at tick 1162 but teleported
     * at 1164 (its next attack-ready tick), and the fire queen followed five ticks later when its
     * own cycle finished. Starting both on the crossing tick is what made the old implementation
     * teleport them together.
     */
    fun phaseTwo(npc: Npc) {
        val run = runOf(npc) ?: return
        if (npc.hitpoints <= 0) return
        val element = TitanElement.of(npc) ?: return
        if (element in run.entered) return
        val tick = deps.mapClock.cycle
        if (run.phase != PHASE_TWO) {
            beginPhaseTwo(run, tick)
            return
        }
        if (element !in run.pendingEntry) return
        startPhaseTwoEntry(run, npc, element, tick)
    }

    /**
     * Flips the run into phase two and queues both titans for entry. The forced ability only fires
     * on a titan's next attack-ready tick, so each one leaves after its current attack, which is
     * what produces the staggered teleport seen in OSRS.
     */
    private fun beginPhaseTwo(run: Run, tick: Int) {
        run.phase = PHASE_TWO
        val alive = aliveTitans(run)
        run.phaseAnchor =
            resolveAnchor(run.queen.spawnCoords)
                ?: resolveAnchor(run.king.spawnCoords)
                ?: resolveExitAnchor(run.queen.spawnCoords)
        logger.info {
            "[royal-titans] phase two begins for instance=${run.session.id} " +
                "alive=${alive.joinToString { it.type.internalName }} anchor=${run.phaseAnchor}"
        }
        // Existing walls and elementals are not removed by the transition in OSRS; walls run
        // their normal lifetime out and elementals keep attacking until they self-destruct.
        patrol.stop(run.queen)
        patrol.stop(run.king)
        run.nextPulse = 0
        run.pulseUntil = 0
        for (titan in alive) {
            val element = TitanElement.of(titan) ?: continue
            val encounter = deps.encounterRegistry.of(titan)
            if (encounter.currentPhaseName != PHASE_TWO_NAME) {
                encounter.transitionTo(PHASE_TWO_NAME, tick)
            }
            encounter.markPhaseEntryFired(PHASE_TWO_NAME)
            encounter.forceNext(PHASE_TWO_ENTRY_ABILITY)
            run.pendingEntry += element
            setWaitFacing(titan, element)
        }
    }

    private fun startPhaseTwoEntry(run: Run, npc: Npc, element: TitanElement, tick: Int) {
        if (element in run.entered) return
        run.entered += element
        run.pendingEntry -= element
        val encounter = deps.encounterRegistry.of(npc)
        if (encounter.currentPhaseName != PHASE_TWO_NAME) {
            encounter.transitionTo(PHASE_TWO_NAME, tick)
        }
        encounter.markPhaseEntryFired(PHASE_TWO_NAME)
        encounter.interrupt(tick)
        encounter.busyUntil = tick + PHASE_TWO_ENTRY_TICKS
        encounter.nextAttackTick = tick + PHASE_TWO_ENTRY_TICKS
        npc.clearInteraction()
        npc.abortRoute()
        npc.movementLocked = true
        npc.ignoreCombatInteractions = true
        setWaitFacing(npc, element)
        npc.anim(element.teleportSeq)
        element.teleportSpot?.let { npc.spotanim(it) }
        npc.say(element.phaseTwoSpeech(deps.random.of(2)))
        val spawn = run.spawnFor(element)
        for (tile in RoyalTitansArena.endTiles(spawn, element)) {
            deps.worldRepo.spotanimMap(element.endSpawnSpot, tile)
        }
        deps.worldQueues.add(PHASE_TWO_SPAWN_DELAY) { landPhaseTwo(run, npc, element) }
    }

    private fun landPhaseTwo(run: Run, npc: Npc, element: TitanElement) {
        if (!active(run) || !npc.isSlotAssigned || npc.hitpoints <= 0) return
        val target = players(run).firstOrNull { it.isValidTarget() }
        knockPlayersOffLandingTiles(run, listOf(npc))
        // Assigning coords directly leaves the entity's collision and zone index on the old tile
        // and never sends the teleport to the client, so the titan stays visually on the phase-one
        // ledge. OSRS sends this as a telejump, which is also what stops the client sliding the
        // model between the two tiles while spawn01 plays.
        PathingEntityCommon.telejump(
            npc,
            deps.collision,
            RoyalTitansArena.phaseTwoPosition(run.spawnFor(element), element),
        )
        npc.anim(element.spawnSeq)
        element.titanSpawnSpot?.let { npc.spotanim(it) }
        npc.setBodyModel(element.phaseTwoBodyModel)
        placePhaseTwoLocs(run, npc, element)
        npc.ignoreCombatInteractions = false
        npc.apRangeOverride = ARENA_AP_RANGE
        npc.apRequiresLineOfSight = false
        if (target != null) {
            npc.apPlayer2(target, aiPlayerInteractions)
            deps.encounterRegistry.of(npc).lastTarget = target
        }
        run.landed += element
        if (run.queenDead || run.kingDead) {
            npc.faceAngleOverride = null
            npc.clearFacingLock()
        } else {
            faceTitan(run, element)
        }
        if (aliveTitans(run).all { TitanElement.of(it) in run.landed }) {
            val pulse = deps.mapClock.cycle + FIRST_PULSE_DELAY
            run.nextPulse = pulse
            // The titan that landed first must not start a normal attack while the other is still
            // teleporting in; OSRS waits and opens phase two with the synchronized blast.
            for (titan in aliveTitans(run)) {
                val encounter = deps.encounterRegistry.of(titan)
                encounter.busyUntil = pulse
                encounter.nextAttackTick = pulse
            }
        }
    }

    private fun onPairKilled(run: Run) {
        val tick = deps.mapClock.cycle
        walls.clearAll()
        elementals.killAll()
        run.respawnAt = tick + RESPAWN_TICKS
        run.fadeStarted = false
        run.looted.clear()
        run.lootStarted = false
        run.lootEventId++
        val kills =
            run.damage.keys
                .filter { it in run.session.occupants }
                .mapNotNull(::playerOf)
                .associateWith { player ->
                    val varp = TOTAL_KILLS_VARP
                    val next = player.vars[varp] + 1
                    VarPlayerIntMapSetter.set(player, varp, next)
                    next
                }
        val duration = tick - run.fightStart
        worldStats.addKill()
        worldStats.addKillTime(duration)
        for ((player, count) in kills) {
            player.mes(
                "Your Royal Titans kill count is: <col=ff0000>$count</col>.",
                ChatType.GameMessage,
            )
            reportDuration(player, duration)
            player.mes(
                "You are eligible to take loot from either " +
                    "${TitanElement.Fire.displayName} or ${TitanElement.Ice.displayName}.",
                ChatType.GameMessage,
            )
        }
    }

    private fun reportDuration(player: Player, ticks: Int) {
        val precise = player.vars["varbit.option_precise_timing"] != 0
        val duration = formatInstanceKillTime(ticks, precise)
        val previous = player.vars[BEST_TIME_VARP]
        val improved = previous == 0 || ticks < previous
        if (improved) {
            VarPlayerIntMapSetter.set(player, BEST_TIME_VARP, ticks)
        }
        val best = player.vars[BEST_TIME_VARP]
        val text =
            when {
                improved -> "Fight duration: <col=ff0000>$duration</col> (new personal best)"
                best > 0 ->
                    "Fight duration: <col=ff0000>$duration</col>. Personal best: " +
                        formatInstanceKillTime(best, precise)
                else -> "Fight duration: <col=ff0000>$duration</col>."
            }
        player.mes(text, ChatType.GameMessage)
    }

    fun countDeath(player: Player) {
        val session = manager.sessionForPlayer(player) ?: return
        if (session.key != INSTANCE_KEY) return
        val deaths = player.vars[DEATHS_VARP] + 1
        VarPlayerIntMapSetter.set(player, DEATHS_VARP, deaths)
        worldStats.addDeath()
    }

    private fun engage(run: Run, player: Player) {
        for (titan in listOf(run.queen, run.king)) {
            if (!titan.isSlotAssigned) continue
            val element = TitanElement.of(titan) ?: continue
            titan.faceAngleOverride = null
            titan.movementLocked = false
            titan.apRequiresLineOfSight = false
            titan.apRangeOverride = ARENA_AP_RANGE
            titan.apPlayer2(player, aiPlayerInteractions)
            patrol.start(titan, player, westStrip = element == TitanElement.Fire)
        }
    }

    private fun tick(run: Run) {
        deps.worldQueues.add(1) {
            if (!active(run)) {
                discard(run)
                return@add
            }
            runCatching { advance(run) }
                .onFailure { logger.warn(it) { "[royal-titans] instance tick failed" } }
            tick(run)
        }
    }

    /** Drops a run when its instance is gone, removing everything that run spawned. */
    private fun discard(run: Run) {
        run.loopRunning = false
        runs.remove(run.session.id)
        clearPulse(run)
        removePhaseTwoLocs(run)
    }

    private fun active(run: Run): Boolean {
        if (manager.sessionForId(run.session.id) !== run.session) return false
        if (run.session.occupants.isNotEmpty()) return true
        return false
    }

    private fun advance(run: Run) {
        val tick = deps.mapClock.cycle
        if (run.queenDead && run.kingDead) {
            if (!run.fadeStarted && tick >= run.respawnAt - CORPSE_FADE_LEAD) {
                run.fadeStarted = true
                for (titan in listOf(run.queen, run.king)) {
                    if (titan.isSlotAssigned) playCorpseDeath(run, titan)
                }
            }
            if (run.respawnAt in 1..tick) {
                reset(run)
            }
            return
        }
        if (run.phase != PHASE_TWO) {
            return
        }
        faceEachOther(run)
        if (run.nextPulse == 0 || tick < run.nextPulse) {
            return
        }
        if (startPulse(run, tick)) {
            run.nextPulse = tick + PULSE_MIN_TICKS + PULSE_STEP_TICKS * deps.random.of(PULSE_STEPS)
        }
    }

    private fun startPulse(run: Run, tick: Int): Boolean {
        val alive = aliveTitans(run)
        if (alive.isEmpty()) return false
        val encounters = alive.associateWith { deps.encounterRegistry.of(it) }
        for (encounter in encounters.values) {
            if (tick < encounter.busyUntil) return false
            val target = encounter.lastTarget
            if (target == null || !target.isValidTarget()) return false
        }
        clearPulse(run)
        val spawn = run.spawnFor(TitanElement.Fire)
        val survivor = run.survivorElement
        val safe =
            RoyalTitansArena.safeTile(spawn, TitanElement.Fire, survivor) { deps.random.of(it) }
        run.safeTile = safe
        run.pulseToken++
        run.pulseUntil = tick + PULSE_DURATION
        markSafeTile(run, safe)
        for (titan in alive) {
            val encounter = encounters.getValue(titan)
            encounter.forceNext(PULSE_ABILITY)
            encounter.busyUntil = tick
            encounter.nextAttackTick = tick
        }
        logger.debug {
            "[royal-titans] pulse safe=(${safe.x}, ${safe.z}) " +
                "survivor=$survivor alive=${alive.size}"
        }
        return true
    }

    /**
     * OSRS knocks any player standing on a titan's phase-two landing strip (the full five-by-three
     * end zone, not just the titan's own three-by-three body) three tiles back towards the middle
     * of the arena. The player plays the troll flyback, faces the teleporting titan, and takes a
     * 15-30 typeless hit on the following tick.
     */
    private fun knockPlayersOffLandingTiles(run: Run, alive: List<Npc>) {
        for (titan in alive) {
            if (!titan.isSlotAssigned || titan.hitpoints <= 0) continue
            val element = TitanElement.of(titan) ?: continue
            val spawn = run.spawnFor(element)
            val landing = RoyalTitansArena.endTiles(spawn, element).toHashSet()
            val step = if (element == TitanElement.Fire) -KNOCKBACK_TILES else KNOCKBACK_TILES
            val face = if (element == TitanElement.Fire) Direction.North else Direction.South
            for (player in players(run)) {
                if (!player.isValidTarget()) continue
                if (player.coords !in landing) continue
                val start = player.coords
                val dest = CoordGrid(start.x, start.z + step, start.level)
                PathingEntityCommon.exactMove(
                    player,
                    start,
                    dest,
                    KNOCKBACK_DELAY1,
                    KNOCKBACK_DELAY2,
                    face.angle,
                    deps.collision,
                )
                player.anim(FLYBACK_SEQ, priority = KNOCKBACK_ANIM_PRIORITY)
                player.queueHit(
                    titan,
                    HIT_DELAY,
                    HitType.Typeless,
                    KNOCKBACK_MIN_HIT + deps.random.of(KNOCKBACK_MAX_HIT - KNOCKBACK_MIN_HIT + 1),
                    deps.playerHitModifier,
                )
            }
        }
    }

    fun charge(npc: Npc, element: TitanElement) {
        npc.say(element.castSpeech(deps.random.of(3)))
        deps.encounterRegistry.of(npc).nextAttackTick = deps.mapClock.cycle + PULSE_TOTAL_TICKS
        faceEachOther(npc)
    }

    fun release(npc: Npc, element: TitanElement) {
        faceEachOther(npc)
        val run = runOf(npc) ?: return
        val safe = run.safeTile ?: return
        val token = run.pulseToken
        val tiles =
            RoyalTitansArena.waveTiles(run.spawnFor(element), element, safe, run.survivorElement)
        if (tiles.isEmpty()) return
        val spot = element.aoeWaveSpot
        if (run.pulseSoundToken != run.pulseToken) {
            run.pulseSoundToken = run.pulseToken
            for (player in players(run)) {
                if (!player.isValidTarget()) continue
                for (pass in 0 until WAVE_PASSES) {
                    player.soundSynth(
                        AOE_WAVE_SOUND,
                        delay = AOE_WAVE_SOUND_DELAY + pass * AOE_WAVE_SOUND_GAP,
                    )
                }
            }
        }
        for (pass in 0 until WAVE_PASSES) {
            deps.worldQueues.add(1 + pass * WAVE_GAP) {
                if (run.pulseToken != token || run.safeTile != safe) return@add
                for (tile in tiles) {
                    deps.worldRepo.spotanimMap(spot, tile)
                }
                elementals.destroyIn(tiles, element.opposite)
                hitPlayers(run, npc, tiles)
            }
        }
    }

    private fun hitPlayers(run: Run, npc: Npc, tiles: Collection<CoordGrid>) {
        for (player in players(run)) {
            if (!player.isValidTarget() || player.coords !in tiles) continue
            player.queueHit(
                npc,
                HIT_DELAY,
                HitType.Typeless,
                deps.random.of(BLAST_MAX_HIT + 1),
                deps.playerHitModifier,
            )
        }
    }

    @OptIn(InternalApi::class)
    private fun reset(run: Run) {
        clearPulse(run)
        removePhaseTwoLocs(run)
        walls.clearAll()
        elementals.clearAll()
        val target = players(run).firstOrNull { it.isValidTarget() }
        // The corpses fade first, then the new pair appears a couple of ticks apart (OSRS).
        for (titan in listOf(run.queen, run.king)) {
            if (!titan.isSlotAssigned) continue
            runCatching { deps.npcRepo.del(titan, Int.MAX_VALUE) }
                .onFailure { logger.warn(it) { "[royal-titans] failed to delete defeated titan" } }
        }
        run.phase = PHASE_ONE
        run.queenDead = false
        run.kingDead = false
        run.survivorElement = null
        run.damage.clear()
        run.looted.clear()
        run.lootStarted = false
        run.respawnAt = 0
        run.fadeStarted = false
        run.entered.clear()
        run.landed.clear()
        run.pendingEntry.clear()
        run.nextPulse = 0
        run.pulseUntil = 0
        run.fightStart = deps.mapClock.cycle
        deps.worldQueues.add(RESPAWN_DELAY) { spawnTitan(run, TitanElement.Fire, target) }
        deps.worldQueues.add(RESPAWN_DELAY + RESPAWN_STAGGER) {
            spawnTitan(run, TitanElement.Ice, target)
        }
        logger.info { "[royal-titans] instance=${run.session.id} titans respawning" }
    }

    @OptIn(InternalApi::class)
    private fun spawnTitan(run: Run, element: TitanElement, target: Player?) {
        if (!active(run)) return
        val fightType = ServerCacheManager.getNpc(element.titanId) ?: return
        val npc = Npc(fightType, run.spawnFor(element))
        npc.copyStats(fightType)
        npc.hitpoints = npc.baseHitpointsLvl
        npc.moveRestrict = fightType.moveRestrict
        npc.ignoreCombatInteractions = false
        npc.apRequiresLineOfSight = false
        npc.apRangeOverride = ARENA_AP_RANGE
        npc.respawnDir = if (element == TitanElement.Fire) Direction.East else Direction.West
        val added =
            runCatching { deps.npcRepo.add(npc, Int.MAX_VALUE) }
                .onFailure { logger.warn(it) { "[royal-titans] failed to respawn ${element.displayName}" } }
                .isSuccess
        if (!added) {
            return
        }
        // The avatar only gets its info protocol on Add; setting the spawn animation earlier is a
        // no-op and the titan then pops in on its default idle pose.
        npc.anim(element.spawnSeq)
        element.titanSpawnSpot?.let { npc.spotanim(it) }
        manager.attachNpc(run.session.id, npc)
        if (element == TitanElement.Fire) {
            run.queen = npc
        } else {
            run.king = npc
        }
        val firstAttack = deps.mapClock.cycle + FIRST_ATTACK_DELAY
        val encounter = deps.encounterRegistry.of(npc)
        encounter.busyUntil = firstAttack
        encounter.nextAttackTick = firstAttack
        if (target != null) {
            engage(run, target)
        }
        logger.info {
            "[royal-titans] respawned ${element.displayName} at " +
                "(${npc.coords.x}, ${npc.coords.z})"
        }
    }

    private fun playCorpseDeath(run: Run, npc: Npc) {
        val element = TitanElement.of(npc) ?: return
        npc.anim(element.deathSeq)
        element.deathSpot?.let { npc.spotanim(it) }
        val spawn = npc.spawnCoords
        val locs =
            if (element == TitanElement.Fire) {
                DEATH_FLOOR_FIRE
            } else {
                DEATH_FLOOR_ICE
            }
        // The pools form against the corridor edges either side of the corpse, not against the
        // titan's own spawn column on the ledge.
        val columns = RoyalTitansArena.columns(spawn, element)
        val level = npc.coords.level
        addLoc(run, locs[0], CoordGrid(columns.first, npc.coords.z, level), Int.MAX_VALUE)
        addLoc(run, locs[1], CoordGrid(columns.last, npc.coords.z, level), Int.MAX_VALUE)
        logger.debug { "[royal-titans] ${element.displayName} corpse fades at (${spawn.x}, ${spawn.z})" }
    }

    private fun markSafeTile(run: Run, tile: CoordGrid) {
        addLoc(run, SAFE_MARKER_LOC, tile, SAFE_MARKER_TICKS, marker = true)
        val type = ServerCacheManager.getNpc(TIMER_NPC_ID) ?: return
        val npc = Npc(type, tile)
        deps.npcRepo.add(npc, TIMER_NPC_TICKS)
        npc.mode = NpcMode.None
        npc.movementLocked = true
        npc.ignoreCombatInteractions = true
        val bar = ServerCacheManager.getHealthBar(TIMER_HEADBAR) ?: return
        npc.showHeadbar(
            EngineHeadbar.fromNoSource(bar.id, bar.id, 0, bar.segments, 0, TIMER_HEADBAR_CYCLES)
        )
        run.timerNpcs += npc
    }

    private fun placePhaseTwoLocs(run: Run, npc: Npc, element: TitanElement) {
        val spawn = run.spawnFor(element)
        val level = spawn.level
        val xs = RoyalTitansArena.columns(spawn, element)
        val zs =
            if (element == TitanElement.Fire) {
                spawn.z + 5..spawn.z + 7
            } else {
                spawn.z - 5..spawn.z - 3
            }
        val floorWest = CoordGrid(xs.first, zs.first, level)
        val floorEast = CoordGrid(xs.last, zs.first, level)
        if (element == TitanElement.Fire) {
            addLoc(run, "loc.fire_giant_branda_queen_spawn01_floor01", floorWest, Int.MAX_VALUE)
            addLoc(run, "loc.fire_giant_branda_queen_spawn01_floor02", floorEast, Int.MAX_VALUE)
        } else {
            addLoc(run, "loc.ice_giant_eldric_king_spawn01_floor02", floorWest, Int.MAX_VALUE)
            addLoc(run, "loc.ice_giant_eldric_king_spawn01_floor01", floorEast, Int.MAX_VALUE)
        }
        for (x in xs.first + 1 until xs.last) {
            for (z in zs) {
                addLoc(run, BLOCKING_LOC, CoordGrid(x, z, level), Int.MAX_VALUE)
            }
        }
        logger.debug {
            "[royal-titans] phase two locs ${element.name.lowercase()} " +
                "spawn=(${spawn.x},${spawn.z}) zone=(${xs.first},${zs.first})..(${xs.last},${zs.last}) " +
                "floors=(${floorWest.x},${floorWest.z})/(${floorEast.x},${floorEast.z})"
        }
    }

    private fun addLoc(
        run: Run,
        symbol: String,
        tile: CoordGrid,
        duration: Int,
        marker: Boolean = false,
    ) {
        val id = symbol.asRSCM(RSCMType.LOC)
        val shape = if (marker) LocShape.GroundDecor else LocShape.CentrepieceStraight
        val loc =
            LocInfo(
                LocLayerConstants.of(shape.id),
                tile,
                LocEntity(id, shape.id, LocAngle.West.id),
            )
        if (deps.locRepo.add(loc, duration)) {
            if (marker) run.markerLocs += loc else run.extraLocs += loc
        }
    }

    private fun removePhaseTwoLocs(run: Run) {
        for (loc in run.extraLocs) {
            runCatching { deps.locRepo.del(loc, Int.MAX_VALUE) }
                .onFailure { logger.warn(it) { "[royal-titans] failed to remove phase two loc" } }
        }
        run.extraLocs.clear()
    }

    private fun clearPulse(run: Run) {
        run.safeTile = null
        run.pulseToken++
        for (loc in run.markerLocs) {
            runCatching { deps.locRepo.del(loc, Int.MAX_VALUE) }
                .onFailure { logger.warn(it) { "[royal-titans] failed to remove safe marker" } }
        }
        run.markerLocs.clear()
        for (npc in run.timerNpcs) {
            if (!npc.isSlotAssigned) continue
            runCatching { deps.npcRepo.del(npc, Int.MAX_VALUE) }
                .onFailure { logger.warn(it) { "[royal-titans] failed to remove timer npc" } }
        }
        run.timerNpcs.clear()
    }

    private fun aliveTitans(run: Run): List<Npc> = buildList {
        if (!run.queenDead && run.queen.isSlotAssigned) add(run.queen)
        if (!run.kingDead && run.king.isSlotAssigned) add(run.king)
    }

    private fun players(run: Run): List<Player> =
        run.session.occupants.mapNotNull(::playerOf)

    private fun playerOf(uuid: Long): Player? = deps.playerList.firstOrNull { it.uuid == uuid }

    private fun broadcast(run: Run, text: String) {
        for (player in players(run)) {
            player.mes(text, ChatType.GameMessage)
        }
    }

    private fun contributionPercent(run: Run, uuid: Long): Int =
        RoyalTitansMath.contributionPercent(run.damage[uuid] ?: 0, run.damage.values.sum())

    class Run(val session: InstanceSession, var queen: Npc, var king: Npc) {
        val damage = HashMap<Long, Int>()
        val looted = HashSet<Long>()
        val markerLocs = mutableListOf<LocInfo>()
        val timerNpcs = mutableListOf<Npc>()
        val extraLocs = mutableListOf<LocInfo>()
        var started = false
        var phase = PHASE_ONE
        var queenDead = false
        var kingDead = false
        var survivorElement: TitanElement? = null
        var fightStart = 0
        var nextPulse = 0
        var pulseUntil = 0
        var pulseToken = 0
        var pulseSoundToken = -1
        val entered = mutableSetOf<TitanElement>()
        val landed = mutableSetOf<TitanElement>()
        val pendingEntry = mutableSetOf<TitanElement>()
        var safeTile: CoordGrid? = null
        var lootStarted = false
        var respawnAt = 0
        var fadeStarted = false
        var lootEventId = 1
        var loopRunning = false
        var phaseAnchor: CoordGrid? = null

        /**
         * Spawn tile used for phase-two geometry. The instanced spawn npcs are not always placed
         * on the same tiles as the copied arena map, so phase two is anchored to the map's north
         * mouth instead of [Npc.spawnCoords].
         */
        fun spawnFor(element: TitanElement): CoordGrid {
            val anchor = phaseAnchor
            if (anchor != null) {
                return if (element == TitanElement.Fire) {
                    CoordGrid(anchor.x - 5, anchor.z - 8, anchor.level)
                } else {
                    CoordGrid(anchor.x + 3, anchor.z - 8, anchor.level)
                }
            }
            val actual = if (element == TitanElement.Fire) queen.spawnCoords else king.spawnCoords
            return actual
        }
    }

    companion object {
        const val INSTANCE_KEY = "royal_titans"
        const val PULSE_ABILITY = "pulse"
        const val PHASE_TWO_ENTRY_ABILITY = "phase_two_entry"

        private const val PHASE_ONE = 1
        private const val PHASE_TWO = 2
        private const val PHASE_TWO_NAME = "phase_two"
        private const val PHASE_TWO_ENTRY_TICKS = 10
        private const val PHASE_TWO_SPAWN_DELAY = 2
        private const val FIRST_PULSE_DELAY = PHASE_TWO_ENTRY_TICKS - PHASE_TWO_SPAWN_DELAY
        private const val RESPAWN_DELAY = 2
        private const val RESPAWN_STAGGER = 2
        private const val FIRST_ATTACK_DELAY = 8
        private const val FIRE_FACE_ANGLE = 0
        private const val ICE_FACE_ANGLE = 1024
        private const val FIRE_WAIT_FACE_ANGLE = 1536
        private const val ICE_WAIT_FACE_ANGLE = 512
        private const val SURVIVOR_SPEECH_DELAY = 4
        private const val PULSE_MIN_TICKS = 22
        private const val PULSE_STEP_TICKS = 7
        private const val PULSE_STEPS = 3
        private const val PULSE_DURATION = 12
        private const val PULSE_TOTAL_TICKS = 14
        private const val WAVE_PASSES = 3
        private const val WAVE_GAP = 2
        private const val BLAST_MAX_HIT = 30
        private const val RANGED_ACCURACY_BOOST = 6.0
        private const val RESPAWN_TICKS = 30
        private const val CORPSE_FADE_LEAD = 5
        private const val AOE_WAVE_SOUND = 10155
        private const val AOE_WAVE_SOUND_DELAY = 28
        private const val AOE_WAVE_SOUND_GAP = 60
        private const val KNOCKBACK_MIN_HIT = 15
        private const val KNOCKBACK_MAX_HIT = 30
        private const val KNOCKBACK_TILES = 3
        private const val KNOCKBACK_DELAY1 = 0
        private const val KNOCKBACK_DELAY2 = 15
        private const val KNOCKBACK_ANIM_PRIORITY = 100
        private const val FLYBACK_SEQ = "seq.human_troll_flyback_withsound"
        private const val HIT_DELAY = 1
        private const val ARENA_AP_RANGE = 20
        private const val SAFE_MARKER_LOC = "loc.vfx_giant_attack_aoe_protection01"
        private const val SAFE_MARKER_TICKS = 13
        private const val TIMER_HEADBAR_SYMBOL = "headbar.royal_titans_area_attack_timer"
        private const val TIMER_NPC = "npc.royal_titans_area_attack_timer_handler"
        private const val TIMER_NPC_TICKS = 7
        private const val TIMER_HEADBAR_CYCLES = 210
        private const val BLOCKING_LOC = "loc.invisible_type8_blocking"
        private const val TOTAL_KILLS_VARP = "varp.total_royal_titan_kills"
        private const val DEATHS_VARP = "varp.royal_titans_deaths"
        private const val BEST_TIME_VARP = "varp.royal_titans_best_time"

        private val TIMER_NPC_ID = TIMER_NPC.asRSCM(RSCMType.NPC)
        private val TIMER_HEADBAR = TIMER_HEADBAR_SYMBOL.asRSCM(RSCMType.HEADBAR)
        private val ANCHOR_TORCH = "loc.standing_torch".asRSCM(RSCMType.LOC)
        private val ANCHOR_EXIT = "loc.royal_titans_exit".asRSCM(RSCMType.LOC)
        private val DEATH_FLOOR_FIRE =
            listOf("loc.fire_giant_branda_queen_death01_floor01", "loc.fire_giant_branda_queen_death01_floor02")
        private val DEATH_FLOOR_ICE =
            listOf("loc.ice_giant_eldric_king_death01_floor02", "loc.ice_giant_eldric_king_death01_floor01")

        private val TitanElement.teleportSeq: String
            get() =
                if (this == TitanElement.Fire) {
                    "seq.fire_giant_branda_queen_teleport01"
                } else {
                    "seq.ice_giant_eldric_king_teleport01"
                }

        private val TitanElement.spawnSeq: String
            get() =
                if (this == TitanElement.Fire) {
                    "seq.fire_giant_branda_queen_spawn01"
                } else {
                    "seq.ice_giant_eldric_king_spawn01"
                }

        private val TitanElement.deathSeq: String
            get() =
                if (this == TitanElement.Fire) {
                    "seq.fire_giant_branda_queen_death01"
                } else {
                    "seq.ice_giant_eldric_king_death01"
                }

        private val TitanElement.teleportSpot: String?
            get() =
                if (this == TitanElement.Fire) {
                    "spotanim.vfx_fire_giant_branda_queen_teleport01"
                } else {
                    null
                }

        private val TitanElement.titanSpawnSpot: String?
            get() =
                if (this == TitanElement.Fire) {
                    "spotanim.vfx_fire_giant_branda_queen_spawn01"
                } else {
                    null
                }

        private val TitanElement.deathSpot: String?
            get() =
                if (this == TitanElement.Fire) {
                    "spotanim.vfx_fire_giant_branda_queen_death01"
                } else {
                    null
                }

        private val TitanElement.endSpawnSpot: SpotanimType
            get() =
                if (this == TitanElement.Fire) {
                    SpotanimType("spotanim.vfx_branda_queen_loc01_spawn02".asRSCM(RSCMType.SPOTANIM))
                } else {
                    SpotanimType("spotanim.vfx_eldric_king_loc01_spawn02".asRSCM(RSCMType.SPOTANIM))
                }

        private val TitanElement.aoeWaveSpot: SpotanimType
            get() = SpotanimType(aoeWave.asRSCM(RSCMType.SPOTANIM))

        private fun TitanElement.phaseTwoSpeech(index: Int): String =
            if (this == TitanElement.Fire) {
                listOf("You'll regret provoking me!", "Time to face your doom!")[index % 2]
            } else {
                listOf("Let's end this fight!", "Tremble before the strength of ice!")[index % 2]
            }

        private fun TitanElement.castSpeech(index: Int): String =
            if (this == TitanElement.Fire) {
                listOf(
                    "Behold the power of fire!",
                    "Time to face your doom!",
                    "I'm coming for you!",
                )[index % 3]
            } else {
                listOf(
                    "Feel the wrath of the ice!",
                    "Tremble before the strength of ice!",
                    "No more running, face me now!",
                )[index % 3]
            }

        private fun TitanElement.survivorSpeech(index: Int): String =
            if (this == TitanElement.Fire) {
                listOf(
                    "No more holding back! Prepare to feel my full strength!",
                    "Time to show you my full strength!",
                )[index % 2]
            } else {
                listOf(
                    "No more holding back! Prepare to feel my full strength!",
                    "Time to show you my full strength!",
                )[index % 2]
            }
    }
}
