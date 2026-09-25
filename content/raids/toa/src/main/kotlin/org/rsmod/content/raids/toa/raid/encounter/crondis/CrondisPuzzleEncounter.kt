package org.rsmod.content.raids.toa.raid.encounter.crondis

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import kotlin.math.abs
import kotlin.math.floor
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.game.obj.Obj
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * The Crondis puzzle, "Test of Resourcefulness". Port of Offline_Scape CrondisPuzzleEncounter.
 *
 * Players take water containers, fill them at the four waterfalls and water the Palm of
 * Resourcefulness. The palm needs 175 water, +125 per extra player (OSRS Wiki); it grows a stage every 25%
 * (npcs toa_crondis_tree_1..5), and at the fifth stage the room is complete and the end barrier
 * opens.
 *
 * This file is the room's state; the ops (take, fill, water, check, empty) are in
 * [CrondisPuzzleScript].
 *
 * Hazards while the challenge runs (Offline_Scape CycleProcessPlugin.process), one tick loop:
 * acid trails from the north and south basins, and the spear statues along the walls. Both hit
 * (scaled by raid level), spill half your container, and drain 3 Defence and Agility.
 *
 * Crocodiles (OSRS Wiki, Crocodile (Tombs of Amascut)): a wave every 60 ticks (50 after a reset),
 * up to 8 alive. Targets, in order: a player within 3 tiles carrying water, the palm while it has
 * growth, a player without a container who hit it. Its attack is custom (onAiOpPlayer2 for its
 * type replaces the default npc combat): a flat 18 on success, +3 per hazard hit in the last 30
 * seconds, max 36; a third of that through Protect from Melee, draining 12 Prayer; and it spills
 * half your water.
 *
 * Sounds: most of the room's synths have no gameval name (the cache calls them synth_6516
 * etc.), so they're played by id. The constants are named after the labels in the cache's
 * sound-list dbtable `synth_pathofcrondis` (osrs-dumps config/dump.dbrow).
 */
class CrondisPuzzleEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    private var palm: Npc? = null
    private var palmStage = 0

    /** Water poured onto the palm so far (Offline_Scape: the palm's missing hitpoints). */
    var water = 0
        private set

    /** Water needed to finish (Offline_Scape: the palm's max hitpoints, teamSize * 200). */
    val goal: Int
        get() = WATER_FIRST_PLAYER + (teamSize - 1) * WATER_PER_EXTRA_PLAYER

    /** The two containers lying in the room; taking one leaves it there (a free supply). */
    private val floorContainers = ArrayList<Obj>()

    // ---- Room lifecycle ----

    override fun onBuilt() {
        for (tile in CONTAINER_TILES) {
            floorContainers += deps.objRepo.add(CONTAINER, coords(tile), Int.MAX_VALUE)
        }
        for (tile in STATUE_SOUTH_TILES) addLoc(STATUE, tile, LocAngle.West)
        for (tile in STATUE_NORTH_TILES) addLoc(STATUE, tile, LocAngle.East)
        addLoc(PALM_BLOCKER, PALM_TILE, LocAngle.West)
        spawnPalm(stage = 0)
    }

    override fun onStart() {
        water = 0
        for (player in players) openBar(player)
        resetHazards()
        schedule(1) { hazardTick() }
    }

    /**
     * Offline_Scape enter(): preload the room's animations (client script 1846 is
     * `seq_prefetch(seq)`, which loads an animation ahead of time so it doesn't hitch the first
     * time it plays). Someone joining a running challenge also gets the bar.
     */
    override fun onEnter(player: Player) {
        for (seq in PRELOAD_SEQS) {
            player.runClientScript(SCRIPT_SEQ_PREFETCH, seq)
        }
        if (stage == ToaStage.STARTED) openBar(player)
    }

    override fun onLeave(player: Player) {
        closeBar(player)
        removeContainers(player)
    }

    override fun onComplete() {
        for (player in players) {
            closeBar(player)
            removeContainers(player)
        }
        for (obj in floorContainers) deps.objRepo.del(obj, Int.MAX_VALUE)
        floorContainers.clear()
        restoreWaterfalls()
        removeEndBarrier()
        resetHazards()
        idleSpears()
        removeCrocodiles()
    }

    override fun onReset() {
        for (player in players) {
            closeBar(player)
            removeContainers(player)
        }
        water = 0
        spawnPalm(stage = 0)
        restoreWaterfalls()
        resetHazards()
        idleSpears()
        removeCrocodiles()
        // Offline_Scape onRoomReset: the next attempt's first wave comes a little sooner.
        crocCountdown = CROC_RESET_DELAY
    }

    // ---- The palm ----

    /**
     * Adds [amount] water to the palm (Offline_Scape: a "shield charge" hit on the palm). Grows
     * the palm when it crosses a 25% step; the last step completes the room.
     */
    fun waterPalm(amount: Int) {
        if (stage != ToaStage.STARTED || amount <= 0) return
        water = (water + amount).coerceAtMost(goal)
        for (player in players) updateBar(player)

        val newStage = stageFor(water)
        if (newStage == palmStage) return
        for (player in players) player.soundSynth(SYNTH_PALM_GROW)
        spawnPalm(newStage)
        if (newStage == FINAL_STAGE) {
            complete()
        } else {
            // The new palm is a new npc, so the bar is re-pointed at it.
            for (player in players) openBar(player)
        }
    }

    /**
     * A crocodile's bite: the palm loses water and shrinks back a stage if it drops below one
     * (Offline_Scape PALM_LOWER hit). Never undoes completion: at the last stage the room is over.
     */
    private fun drainPalm(amount: Int) {
        if (stage != ToaStage.STARTED || water <= 0) return
        water = (water - amount).coerceAtLeast(0)
        for (player in players) updateBar(player)
        val newStage = stageFor(water)
        if (newStage < palmStage) {
            for (player in players) player.soundSynth(SYNTH_PALM_SHRINK)
            spawnPalm(newStage)
            for (player in players) openBar(player)
        }
    }

    /** Offline_Scape getNpcId: stage i while water < goal * (i + 1) / 4. */
    private fun stageFor(water: Int): Int = (water * FINAL_STAGE / goal).coerceAtMost(FINAL_STAGE)

    /**
     * The palm stages are separate npc types, so a stage change replaces the npc (Offline_Scape
     * transformed it).
     */
    private fun spawnPalm(stage: Int) {
        palm?.let { if (it.isSlotAssigned) deps.npcRepo.del(it, Int.MAX_VALUE) }
        val npc = Npc(PALM_STAGES[stage], coords(PALM_TILE))
        npc.respawnDir = Direction.South
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        palm = npc
        palmStage = stage
    }

    // ---- Progress bar (Offline_Scape HpHud: 0 -> goal as the palm is watered) ----

    private fun openBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onOpen(player, npc)
        updateBar(player)
    }

    private fun updateBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onUpdate(player, npc, currentHp = water, maxHp = goal)
    }

    private fun closeBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onClose(player, npc, instant = true)
    }

    // ---- Waterfalls ----

    /**
     * A filled-from waterfall runs dry for a while: the empty variant is added with a duration,
     * and the map's own waterfall comes back when it expires. Offline_Scape: 128 ticks, 18 fewer
     * per extra party member.
     */
    fun drainWaterfall(waterfall: BoundLocInfo) {
        val refillTicks = (BASE_REFILL_TICKS - (teamSize - 1) * REFILL_TICKS_PER_PLAYER).coerceAtLeast(1)
        deps.locRepo.change(waterfall, WATER_SOURCE_EMPTY, refillTicks)
    }

    /** Offline_Scape spawnWaterfalls: every waterfall full again (end of room, or a reset). */
    private fun restoreWaterfalls() {
        for (tile in WATERFALL_SOUTH_TILES) addLoc(WATER_SOURCE, tile, LocAngle.West)
        for (tile in WATERFALL_NORTH_TILES) addLoc(WATER_SOURCE, tile, LocAngle.East)
    }

    // ---- Hazards ----

    private val acidTrails = ArrayList<AcidTrail>()
    private var acidCountdown = 0
    private var spearTick = 0

    /** Per-player cooldowns (map cycle a player can be hit again), Offline_Scape temp attrs. */
    private val acidHitReady = HashMap<Player, Int>()
    private val spearHitReady = HashMap<Player, Int>()

    private fun resetHazards() {
        acidTrails.clear()
        acidCountdown = 0
        spearTick = 0
        acidHitReady.clear()
        spearHitReady.clear()
    }

    /**
     * One tick of Offline_Scape `process()`. Re-arms itself; completing, resetting or destroying
     * the room drops the pending task (ToaEncounter.schedule), which stops the loop.
     */
    private fun hazardTick() {
        if (stage != ToaStage.STARTED) return

        acidTrails.removeIf { it.expire() }
        val targets = hazardTargets()
        for (player in targets) {
            for (trail in acidTrails) trail.check(player)
        }
        if (--acidCountdown <= 0) {
            acidCountdown = ACID_INTERVAL
            spawnAcid(ACID_NORTH_BASES, moveNorth = false)
            spawnAcid(ACID_SOUTH_BASES, moveNorth = true)
        }

        handleSpears(targets)
        spearTick = (spearTick + 1) % SPEAR_CYCLE

        if (crocCountdown-- <= 0) {
            crocCountdown = CROC_INTERVAL
            spawnCrocodiles()
        }

        schedule(1) { hazardTick() }
    }

    /** Players the hazards can hit: alive, not a ghost, inside the challenge area. */
    private fun hazardTargets(): List<Player> =
        players.filter { inChallengeArea(it) && !raid.isGhost(it) && !raid.isDying(it) }

    // -- Acid (Offline_Scape spawnAcidTrails / AcidTrail) --

    /**
     * From each basin, 2-3 of its 5 columns start a trail: an acid orb appears for 3 ticks, then
     * 2 ticks later the trail runs 10 tiles across the room.
     */
    private fun spawnAcid(bases: List<CoordGrid>, moveNorth: Boolean) {
        for (base in bases) {
            val columns = shuffledColumns()
            val count = deps.random.of(ACID_MIN_TRAILS, ACID_MAX_TRAILS)
            for (i in 0 until count) {
                val tile = coords(base.translate(columns[i], 0))
                deps.locRepo.add(tile, ACID_ORB, ACID_ORB_TICKS, LocAngle.West, LocShape.CentrepieceStraight)
                val trail = AcidTrail(tile, moveNorth)
                schedule(ACID_TRAIL_DELAY) { startTrail(trail) }
            }
        }
    }

    /** 0..4 in random order. Hand-rolled because randomness must go through GameRandom. */
    private fun shuffledColumns(): IntArray {
        val columns = IntArray(ACID_COLUMNS) { it }
        for (i in columns.lastIndex downTo 1) {
            val j = deps.random.of(maxExclusive = i + 1)
            val swap = columns[i]
            columns[i] = columns[j]
            columns[j] = swap
        }
        return columns
    }

    private fun startTrail(trail: AcidTrail) {
        acidTrails += trail
        val spotanim = SpotanimType(ACID_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        for (step in 1..ACID_TRAIL_LENGTH) {
            val tile = trail.base.translate(0, if (trail.moveNorth) step else -step)
            // Delay in client cycles: the splash travels along the trail.
            deps.worldRepo.spotanimMap(spotanim, tile, delay = ACID_SPLASH_CYCLES * step)
        }
    }

    private inner class AcidTrail(val base: CoordGrid, val moveNorth: Boolean) {
        private var ticks = ACID_TRAIL_TICKS

        /** Counts down; `true` once the trail is spent. */
        fun expire(): Boolean = --ticks <= 0

        /**
         * Hits a player in the trail's column while the splash passes their tile: the further
         * from the basin, the later (Offline_Scape AcidTrail.check).
         */
        fun check(player: Player) {
            val coords = player.coords
            if (coords.x != base.x) return
            val dz = coords.z - base.z
            val ahead = if (moveNorth) dz else -dz
            if (ahead !in 1..ACID_TRAIL_LENGTH) return
            val sub = 1 + (abs(dz) - 1) / 3
            val window = ACID_TRAIL_TICKS - sub
            if (ticks < window && ticks >= window - 2) {
                hit(player, acidHitReady, ACID_BASE_DAMAGE, ACID_HIT_COOLDOWN)
            }
        }
    }

    // -- Spears (Offline_Scape handleSpears / handleSpearColumn) --

    /**
     * Four statue rows, each with a west and an east wall of five spears on a 10-tick cycle with
     * its own delays. A spear is dangerous on 3 ticks of the cycle, over a 3-tile strip.
     */
    private fun handleSpears(targets: List<Player>) {
        val active = ArrayList<CoordGrid>()
        for (row in SPEAR_ROWS.indices) {
            spearWall(row, east = false, active)
            spearWall(row, east = true, active)
        }
        for (player in targets) {
            val coords = player.coords
            if (active.any { coords.z == it.z && coords.x in it.x..it.x + 2 }) {
                hit(player, spearHitReady, SPEAR_BASE_DAMAGE, SPEAR_HIT_COOLDOWN)
            }
        }
    }

    private fun spearWall(row: Int, east: Boolean, active: MutableList<CoordGrid>) {
        val origin = coords(SPEAR_ROWS[row])
        for (y in 0 until SPEARS_PER_WALL) {
            val delay = SPEAR_DELAYS[row][y + if (east) SPEARS_PER_WALL else 0]
            val dz = y * 2 + if (east) 1 else 0
            val mouth = origin.translate(if (east) 7 else 0, dz)
            val spear = origin.translate(if (east) 4 else 2, dz)
            when (spearTick) {
                delay -> animLoc(ROW_TRAP, mouth, SEQ_TRAP_ACTIVATE)
                (delay + 3) % SPEAR_CYCLE -> animLoc(ROW_TRAP_FIRE, spear, SEQ_TRAP_SPEAR)
                (delay + 4) % SPEAR_CYCLE,
                (delay + 5) % SPEAR_CYCLE -> active += spear
                (delay + 6) % SPEAR_CYCLE -> {
                    animLoc(ROW_TRAP_FIRE, spear, SEQ_TRAP_IDLE)
                    active += spear
                }
                (delay + 7) % SPEAR_CYCLE -> animLoc(ROW_TRAP, mouth, SEQ_TRAP_IDLE)
            }
        }
    }

    /** Offline_Scape clearStatueSpikes: every trap back to idle. */
    private fun idleSpears() {
        for (row in SPEAR_ROWS.indices) {
            val origin = coords(SPEAR_ROWS[row])
            for (east in listOf(false, true)) {
                for (y in 0 until SPEARS_PER_WALL) {
                    val dz = y * 2 + if (east) 1 else 0
                    animLoc(ROW_TRAP_FIRE, origin.translate(if (east) 4 else 2, dz), SEQ_TRAP_IDLE)
                    animLoc(ROW_TRAP, origin.translate(if (east) 7 else 0, dz), SEQ_TRAP_IDLE)
                }
            }
        }
    }

    /**
     * Animates the map's own trap loc at [at]. The traps are part of the map, so they're looked
     * up rather than built; if one isn't found, only the visual is skipped (damage still works).
     */
    private fun animLoc(type: String, at: CoordGrid, seq: String) {
        val locType = ServerCacheManager.getObject(type.asRSCM(RSCMType.LOC)) ?: return
        val loc = deps.locRepo.findExact(at, locType) ?: return
        deps.worldRepo.locAnim(loc, seq)
    }

    // -- Crocodiles (OSRS Wiki: Crocodile (Tombs of Amascut)) --

    private val crocodiles = ArrayList<Npc>()
    private val crocAttackReady = HashMap<Npc, Int>()
    private var crocCountdown = CROC_FIRST_DELAY

    /** What each crocodile is going for this tick: a [Player], [PalmTarget], or nothing. */
    private val crocTargets = HashMap<Npc, Any>()

    /** Players who have hit each crocodile (its third-priority targets). */
    private val crocAttackers = HashMap<Npc, LinkedHashSet<Player>>()

    /** Map cycles of each player's recent hazard hits, for the crocodile's damage. */
    private val obstacleHits = HashMap<Player, ArrayDeque<Int>>()

    private object PalmTarget

    /** Offline_Scape process(): ceil(teamSize / 2) crocodiles (at most 4) per wave, max 8 alive. */
    private fun spawnCrocodiles() {
        pruneDeadCrocodiles()
        if (crocodiles.size >= CROC_MAX_ALIVE) return
        val count = minOf((teamSize + 1) / 2, CROC_SPAWNS.size)
        for (i in 0 until count) {
            val croc = Npc(CROCODILE, coords(CROC_SPAWNS[i]))
            deps.npcRepo.add(croc, Int.MAX_VALUE)
            croc.noneMode()
            palm?.let { croc.faceSquare(it.coords, it.size, it.size) }
            croc.aiTimer(1)
            crocodiles += croc
            owners[croc] = this
        }
    }

    /** Killed crocodiles die through the standard npc death (no respawn); forget them. */
    private fun pruneDeadCrocodiles() {
        crocodiles.removeIf { croc ->
            val dead = !croc.isSlotAssigned
            if (dead) forget(croc)
            dead
        }
    }

    private fun forget(croc: Npc) {
        owners.remove(croc)
        crocAttackReady.remove(croc)
        crocTargets.remove(croc)
        crocAttackers.remove(croc)
    }

    private fun removeCrocodiles() {
        for (croc in crocodiles) {
            forget(croc)
            if (croc.isSlotAssigned) deps.npcRepo.del(croc, Int.MAX_VALUE)
        }
        crocodiles.clear()
        obstacleHits.clear()
    }

    /**
     * The wiki's target priority, re-evaluated every tick:
     * 1. a player with a non-empty water container within aggro range;
     * 2. the palm, while it has any growth;
     * 3. a player without a water container who has attacked this crocodile.
     * So attacking a crocodile while holding an empty container is safe.
     */
    private fun chooseTarget(croc: Npc): Any? {
        val targets = hazardTargets()
        targets.firstOrNull { croc.isWithinDistance(it, CROC_AGGRO_RANGE) && waterIn(it) > 0 }
            ?.let { return it }
        if (water > 0 && palm != null) return PalmTarget
        val attackers = crocAttackers[croc] ?: return null
        return targets.firstOrNull { it in attackers && containerSlot(it) == null }
    }

    /** Once a tick per crocodile (onAiTimer). */
    private fun crocodileAi(croc: Npc) {
        if (stage != ToaStage.STARTED) return
        val fightingPlayer = croc.mode == NpcMode.OpPlayer2 || croc.mode == NpcMode.ApPlayer2

        when (val target = chooseTarget(croc)) {
            is Player -> {
                val previous = crocTargets.put(croc, target)
                // The engine does the chasing; our onAiOpPlayer2 handler does the attack.
                if (!fightingPlayer || previous !== target) {
                    croc.opPlayer2(target, deps.aiInteractions)
                }
            }
            PalmTarget -> {
                crocTargets[croc] = PalmTarget
                if (fightingPlayer) croc.noneMode()
                approachPalm(croc)
            }
            else -> {
                crocTargets.remove(croc)
                if (fightingPlayer) croc.noneMode()
            }
        }
    }

    /** Walks next to the palm, then bites it: 2-5 growth every 7 ticks (attack speed 7). */
    private fun approachPalm(croc: Npc) {
        val palm = palm ?: return
        if (!croc.isWithinDistance(palm, 1)) {
            croc.walk(besidePalm(croc, palm))
            return
        }
        croc.faceSquare(palm.coords, palm.size, palm.size)
        val now = deps.mapClock.cycle
        if ((crocAttackReady[croc] ?: 0) > now) return
        crocAttackReady[croc] = now + CROC_ATTACK_RATE
        croc.anim(CROC_ATTACK_ANIM)
        drainPalm(deps.random.of(CROC_MIN_DRAIN, CROC_MAX_DRAIN))
    }

    /**
     * The crocodile's attack on a player (its onAiOpPlayer2), per the wiki:
     * - a normal crush accuracy roll decides success;
     * - a success always hits 18, +3 for each hazard (acid or spear) that hit this player in the
     *   last 30 seconds, at most 36, regardless of raid level;
     * - through Protect from Melee a success still hits a third of that and drains 12 Prayer;
     * - a success also spills half the player's water, like the hazards.
     *
     * The engine can also get here by itself: players who hit a crocodile make it retaliate. The
     * attack only goes ahead against the target the priority rules chose; otherwise it's redirected.
     */
    private fun crocodileAttack(croc: Npc, target: Player) {
        if (stage != ToaStage.STARTED) return
        val chosen = crocTargets[croc] as? Player
        if (chosen == null) {
            croc.noneMode()
            return
        }
        if (chosen !== target) {
            croc.opPlayer2(chosen, deps.aiInteractions)
            return
        }

        val now = deps.mapClock.cycle
        if ((crocAttackReady[croc] ?: 0) > now) return
        crocAttackReady[croc] = now + CROC_ATTACK_RATE

        croc.facePlayer(target)
        croc.anim(CROC_ATTACK_ANIM)
        val success = deps.accuracy.rollMeleeAccuracy(croc, target, MeleeAttackType.Crush, deps.random)
        if (!success) {
            target.queueHit(croc, HIT_DELAY, HitType.Melee, damage = 0, modifier = NoopPlayerHitModifier)
            return
        }

        var damage = (CROC_BASE_DAMAGE + CROC_DAMAGE_PER_OBSTACLE * recentObstacleHits(target, now))
            .coerceAtMost(CROC_MAX_DAMAGE)
        if (target.vars[PROTECT_FROM_MELEE] > 0) {
            damage /= 3
            target.statSub("stat.prayer", constant = CROC_PRAYER_DRAIN, percent = 0)
        }
        // NoopPlayerHitModifier: prayer is already accounted for above, not by the processor.
        target.queueHit(croc, HIT_DELAY, HitType.Melee, damage, modifier = NoopPlayerHitModifier)
        spillWater(target)
    }

    private fun crocodileHitBy(croc: Npc, player: Player) {
        crocAttackers.getOrPut(croc) { LinkedHashSet() } += player
    }

    /** Hazard hits on [player] within the last 30 seconds (older ones are dropped). */
    private fun recentObstacleHits(player: Player, now: Int): Int {
        val hits = obstacleHits[player] ?: return 0
        while (hits.isNotEmpty() && hits.first() <= now - OBSTACLE_WINDOW) hits.removeFirst()
        return hits.size
    }

    /**
     * The tile next to the palm nearest the crocodile. The palm's own tiles are blocked (the
     * invisible 5x5 blocker), so walking to its coords would go nowhere.
     */
    private fun besidePalm(croc: Npc, palm: Npc): CoordGrid {
        val min = palm.coords
        val x = croc.coords.x.coerceIn(min.x - croc.size, min.x + palm.size)
        val z = croc.coords.z.coerceIn(min.z - croc.size, min.z + palm.size)
        return CoordGrid(x, z, min.level)
    }

    private fun waterIn(player: Player): Int {
        val slot = containerSlot(player) ?: return 0
        return player.inv[slot]?.vars ?: 0
    }

    // -- Shared --

    /**
     * Offline_Scape hit + spillWater + applyDebuffs, with a per-hazard cooldown so a player
     * standing in one isn't hit every tick.
     */
    private fun hit(player: Player, ready: MutableMap<Player, Int>, baseDamage: Int, cooldown: Int) {
        val now = deps.mapClock.cycle
        if ((ready[player] ?: 0) > now) return
        ready[player] = now + cooldown
        obstacleHits.getOrPut(player) { ArrayDeque() }.addLast(now)

        spillWater(player)
        val min = floor(baseDamage * raid.damageMultiplier).toInt()
        player.queueHit(
            delay = HIT_DELAY,
            type = HitType.Typeless,
            damage = deps.random.of(min, min + DAMAGE_SPREAD),
            modifier = NoopPlayerHitModifier,
        )
        player.statSub("stat.defence", constant = DEBUFF, percent = 0)
        player.statSub("stat.agility", constant = DEBUFF, percent = 0)
    }

    /** Offline_Scape spillWater: half the container (rounded up) is lost. */
    private fun spillWater(player: Player) {
        val slot = containerSlot(player) ?: return
        val obj = player.inv[slot] ?: return
        if (obj.vars <= 0) return
        val lost = (obj.vars + 1) / 2
        player.inv[slot] = InvObj(CONTAINER, obj.count, vars = obj.vars - lost)
        player.mes("Water spills out of your container.")
        player.soundSynth(SYNTH_SPILL)
    }

    // ---- Helpers ----

    /** Offline_Scape onRoomEnd: the three barrier tiles on the west side, (3922, 5279..5281). */
    private fun removeEndBarrier() {
        val barrier = ServerCacheManager.getObject(BARRIER.asRSCM(RSCMType.LOC)) ?: return
        for (dz in 0 until END_BARRIER_LENGTH) {
            val loc = deps.locRepo.findExact(coords(END_BARRIER_TILE.translate(0, dz)), barrier) ?: continue
            deps.locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun addLoc(type: String, static: CoordGrid, angle: LocAngle) {
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, angle, LocShape.CentrepieceStraight)
    }

    companion object {
        const val CONTAINER = "obj.toa_crondis_water_container"
        const val CROCODILE = "npc.toa_crondis_crocodile"
        const val WATER_SOURCE = "loc.toa_crondis_water_source"
        const val WATER_SOURCE_EMPTY = "loc.toa_crondis_water_source_empty"

        /** The four stages that can still be watered (op1 "Water"); stage 5 has no ops. */
        val WATERABLE_PALMS =
            listOf(
                "npc.toa_crondis_tree_1",
                "npc.toa_crondis_tree_2",
                "npc.toa_crondis_tree_3",
                "npc.toa_crondis_tree_4",
            )
        private val PALM_STAGES = WATERABLE_PALMS + "npc.toa_crondis_tree_5"
        private const val FINAL_STAGE = 4

        /** Container capacity, stored in the obj's vars as a percentage (Offline_Scape charges). */
        const val CONTAINER_FULL = 100

        /** OSRS Wiki (Tombs of Amascut/Strategies). Offline_Scape used 200 per player. */
        private const val WATER_FIRST_PLAYER = 175
        private const val WATER_PER_EXTRA_PLAYER = 125
        private const val BASE_REFILL_TICKS = 128
        private const val REFILL_TICKS_PER_PLAYER = 18

        private const val STATUE = "loc.toa_crondis_column_trap"
        private const val ROW_TRAP = "loc.toa_crondis_row_trap"
        private const val ROW_TRAP_FIRE = "loc.toa_crondis_row_trap_fire"
        private const val ACID_ORB = "loc.toa_crondis_orb"
        private const val ACID_SPOTANIM = "spotanim.crondis_column_trap_anim" // Offline_Scape gfx 2129
        private const val SEQ_TRAP_IDLE = "seq.crondis_spear_trap_idle" // 9562
        private const val SEQ_TRAP_ACTIVATE = "seq.crondis_spear_trap_activate" // 9563
        private const val SEQ_TRAP_SPEAR = "seq.crondis_spear_trap_spear02" // 9565

        private const val HIT_DELAY = 1
        private const val DAMAGE_SPREAD = 8
        private const val DEBUFF = 3

        private const val ACID_INTERVAL = 5
        private const val ACID_COLUMNS = 5
        private const val ACID_MIN_TRAILS = 2
        private const val ACID_MAX_TRAILS = 3
        private const val ACID_ORB_TICKS = 3
        private const val ACID_TRAIL_DELAY = 2
        private const val ACID_TRAIL_TICKS = 6
        private const val ACID_TRAIL_LENGTH = 10
        private const val ACID_SPLASH_CYCLES = 10
        private const val ACID_BASE_DAMAGE = 5
        private const val ACID_HIT_COOLDOWN = 2

        /** dbtable synth_pathofcrondis: toa_crondis_tree_grow_02. Offline_Scape upgrade sound. */
        private const val SYNTH_PALM_GROW = 6516

        /** dbtable synth_pathofcrondis: toa_crondis_water_lost_02. Offline_Scape downgrade sound. */
        private const val SYNTH_PALM_SHRINK = 6529

        /** dbtable synth_pathofcrondis: liquid. Offline_Scape SPILL_SOUND. */
        private const val SYNTH_SPILL = 2401

        /** Client script 1846: `seq_prefetch(seq)`. */
        private const val SCRIPT_SEQ_PREFETCH = 1846

        /** Offline_Scape CrondisPuzzleEncounter.enter: animations 9618-9646, 9532-9534, 9541. */
        private val PRELOAD_SEQS: List<Int> = (9618..9646).toList() + listOf(9532, 9533, 9534, 9541)

        private const val CROC_ATTACK_ANIM = "seq.croc_attack"
        private const val CROC_FIRST_DELAY = 60
        private const val CROC_RESET_DELAY = 50
        private const val CROC_INTERVAL = 60
        private const val CROC_MAX_ALIVE = 8
        private const val CROC_AGGRO_RANGE = 3
        private const val CROC_ATTACK_RATE = 7
        private const val CROC_MIN_DRAIN = 2
        private const val CROC_MAX_DRAIN = 5
        private const val CROC_BASE_DAMAGE = 18
        private const val CROC_DAMAGE_PER_OBSTACLE = 3
        private const val CROC_MAX_DAMAGE = 36
        private const val CROC_PRAYER_DRAIN = 12
        private const val PROTECT_FROM_MELEE = "varbit.prayer_protectfrommelee"

        /** 30 seconds. */
        private const val OBSTACLE_WINDOW = 50

        /** Offline_Scape CROCODILE_SPAWN_LOCATIONS; waves use the first ceil(teamSize / 2). */
        private val CROC_SPAWNS =
            listOf(
                CoordGrid(3925, 5285, 0),
                CoordGrid(3946, 5285, 0),
                CoordGrid(3925, 5274, 0),
                CoordGrid(3946, 5274, 0),
            )

        /** Which room each live crocodile belongs to, for the shared onAiTimer handler. */
        private val owners = HashMap<Npc, CrondisPuzzleEncounter>()

        /** Called by CrondisPuzzleScript's onAiTimer for every crocodile, every tick. */
        fun crocodileTick(croc: Npc) {
            roomOf(croc)?.crocodileAi(croc)
        }

        /** Called by CrondisPuzzleScript's onAiOpPlayer2: the crocodile's own attack. */
        fun crocodileAttack(croc: Npc, target: Player) {
            roomOf(croc)?.crocodileAttack(croc, target)
        }

        /** Called by CrondisPuzzleScript's onNpcHit when a player hits a crocodile. */
        fun crocodileHitBy(croc: Npc, player: Player) {
            roomOf(croc)?.crocodileHitBy(croc, player)
        }

        private fun roomOf(croc: Npc): CrondisPuzzleEncounter? {
            val room = owners[croc] ?: return null
            if (room.destroyed) {
                owners.remove(croc)
                return null
            }
            return room
        }

        private const val SPEAR_CYCLE = 10
        private const val SPEARS_PER_WALL = 5
        private const val SPEAR_BASE_DAMAGE = 6
        private const val SPEAR_HIT_COOLDOWN = 3

        /** Offline_Scape ACID_BASE_NORTH/SOUTH_LOCATIONS: trails run south / north from these. */
        private val ACID_NORTH_BASES = listOf(CoordGrid(3941, 5303, 0), CoordGrid(3927, 5303, 0))
        private val ACID_SOUTH_BASES = listOf(CoordGrid(3941, 5257, 0), CoordGrid(3927, 5257, 0))

        /** Offline_Scape STATUE_SPEAR_LOCATIONS: the south-west corner of each statue row. */
        private val SPEAR_ROWS =
            listOf(
                CoordGrid(3925, 5293, 0),
                CoordGrid(3939, 5293, 0),
                CoordGrid(3925, 5258, 0),
                CoordGrid(3939, 5258, 0),
            )

        /** Offline_Scape STATUE_SPEAR_DELAYS: per row, 5 west spears then 5 east spears. */
        private val SPEAR_DELAYS =
            arrayOf(
                intArrayOf(0, 2, 4, 6, 8, 0, 2, 4, 6, 8),
                intArrayOf(0, 3, 6, 9, 2, 0, 3, 6, 9, 2),
                intArrayOf(2, 1, 1, 0, 0, 2, 2, 1, 1, 0),
                intArrayOf(0, 1, 2, 3, 4, 4, 3, 2, 1, 0),
            )
        private const val PALM_BLOCKER = "loc.invisible_type8_blocking_size5"
        private const val BARRIER = "loc.toa_path_barrier"

        private val CONTAINER_TILES = listOf(CoordGrid(3934, 5273, 0), CoordGrid(3938, 5287, 0))
        private val STATUE_SOUTH_TILES = listOf(CoordGrid(3943, 5255, 0), CoordGrid(3929, 5255, 0))
        private val STATUE_NORTH_TILES = listOf(CoordGrid(3929, 5304, 0), CoordGrid(3943, 5304, 0))
        private val WATERFALL_SOUTH_TILES = listOf(CoordGrid(3926, 5250, 0), CoordGrid(3940, 5250, 0))
        private val WATERFALL_NORTH_TILES = listOf(CoordGrid(3926, 5306, 0), CoordGrid(3940, 5306, 0))
        private val PALM_TILE = CoordGrid(3934, 5278, 0)
        private val END_BARRIER_TILE = CoordGrid(3922, 5279, 0)
        private const val END_BARRIER_LENGTH = 3

        private val CONTAINER_ID: Int by lazy { CONTAINER.asRSCM(RSCMType.OBJ) }

        /** Takes every water container away from [player] (end of room, reset, leaving). */
        fun removeContainers(player: Player) {
            for (slot in player.inv.indices) {
                if (player.inv[slot]?.id == CONTAINER_ID) player.inv[slot] = null
            }
        }

        /** Slot of [player]'s water container, or `null`. */
        fun containerSlot(player: Player): Int? =
            player.inv.indices.firstOrNull { player.inv[it]?.id == CONTAINER_ID }
    }
}
