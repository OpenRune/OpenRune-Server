package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.opPlayer2
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid

/**
 * The crocodiles (OSRS Wiki: Crocodile (Tombs of Amascut)). Capture: the first wave 48 ticks after
 * the first hazard tick, then one every 47 (Offline_Scape: 60, then 60); up to 8 alive. They crawl
 * (a tile every 2 ticks) and wait 4 ticks after spawning before going for anything. Targets,
 * re-evaluated every tick:
 * 1. a player with a non-empty water container within aggro range;
 * 2. the palm, while it has any growth;
 * 3. while retaliating, it only bites an attacker without a water container.
 * So attacking a crocodile while holding an empty container is safe.
 *
 * Killed crocodiles die through the standard npc death, which deletes them.
 */
internal class CrondisCrocodiles(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private val crocodiles = ArrayList<Npc>()
    private val wakeAt = HashMap<Npc, Int>()
    private var countdown = nextCountdown()

    /** What each crocodile is going for this tick: a [Player], [PalmTarget], or nothing. */
    private val targets = HashMap<Npc, Any>()

    /** Players who have hit each crocodile. */
    private val attackers = HashMap<Npc, LinkedHashSet<Player>>()

    /** Map cycles of each player's recent hazard hits, for the attack's damage. */
    private val hazardHits = HashMap<Player, ArrayDeque<Int>>()

    private object PalmTarget

    fun tick() {
        if (countdown-- <= 0) {
            countdown = nextCountdown()
            spawnWave()
        }
    }

    /**
     * Captures: waves 46-50 ticks apart (47, 50, 46, 48, 50 and 47), the first 47 and 49 ticks
     * after the start. Offline_Scape used a fixed 48 then 46. The tick after the countdown hits 0
     * spawns, hence the -1.
     */
    private fun nextCountdown(): Int = deps.random.of(MIN_WAVE_GAP, MAX_WAVE_GAP) - 1

    fun onHazardHit(player: Player, cycle: Int) {
        hazardHits.getOrPut(player) { ArrayDeque() }.addLast(cycle)
    }

    fun clear() {
        for (croc in crocodiles) {
            forget(croc)
            room.despawn(croc)
        }
        crocodiles.clear()
        hazardHits.clear()
    }

    /**
     * Offline_Scape onRoomReset: the next attempt's first wave comes 10 ticks sooner than a
     * normal one (60 then 50 there), here on top of the captured spacing.
     */
    fun reset() {
        clear()
        countdown = nextCountdown() - RESET_SOONER
    }

    /** Offline_Scape process(): ceil(teamSize / 2) crocodiles (at most 4) per wave, max 8 alive. */
    private fun spawnWave() {
        pruneDead()
        if (crocodiles.size >= MAX_ALIVE) return
        val spawns = room.crocSide.spawns
        val count = minOf((room.teamSize + 1) / 2, spawns.size)
        for (i in 0 until count) {
            val croc = room.spawnRouted(CrondisNpcs.CROCODILE, spawns[i])
            croc.defaultMoveSpeed = MoveSpeed.Crawl
            croc.noneMode()
            room.palm?.let { croc.faceSquare(it.coords, it.size, it.size) }
            wakeAt[croc] = deps.mapClock.cycle + WAKE_TICKS
            crocodiles += croc
        }
    }

    private fun pruneDead() {
        crocodiles.removeIf { croc ->
            val dead = !croc.isSlotAssigned
            if (dead) {
                forget(croc)
                room.despawn(croc)
            }
            dead
        }
    }

    private fun forget(croc: Npc) {
        wakeAt.remove(croc)
        targets.remove(croc)
        attackers.remove(croc)
    }

    private fun chooseTarget(croc: Npc): Any? {
        val players = room.hazardTargets()
        players
            .firstOrNull { croc.isWithinDistance(it, AGGRO_RANGE) && it.containerWater() > 0 }
            ?.let { return it }
        if (room.water > 0 && room.palm != null) return PalmTarget
        return null
    }

    /** Once a tick per crocodile (onAiTimer). */
    fun ai(croc: Npc) {
        if (room.stage != ToaStage.STARTED) return
        if ((wakeAt[croc] ?: 0) > deps.mapClock.cycle) return
        val fightingPlayer = croc.mode == NpcMode.OpPlayer2 || croc.mode == NpcMode.ApPlayer2

        when (val target = chooseTarget(croc)) {
            is Player -> {
                val previous = targets.put(croc, target)
                // The engine does the chasing; the onAiOpPlayer2 handler does the attack.
                if (!fightingPlayer || previous !== target) {
                    croc.opPlayer2(target, deps.aiInteractions)
                }
            }
            PalmTarget -> {
                targets[croc] = PalmTarget
                if (croc.mode != NpcMode.None) croc.noneMode()
                approachPalm(croc)
            }
            else -> {
                targets.remove(croc)
                val opponent = (croc.interaction as? InteractionPlayer)?.target
                val retaliating = fightingPlayer && opponent != null && mayRetaliate(croc, opponent)
                if (!retaliating && croc.mode != croc.defaultMode) {
                    croc.noneMode()
                    croc.defaultMode()
                }
            }
        }
    }

    private fun approachPalm(croc: Npc) {
        val palm = room.palm ?: return
        croc.faceNpc(palm)
        if (!isBesidePalm(croc, palm)) {
            croc.walk(palmApproach(croc, palm))
            return
        }
        val now = deps.mapClock.cycle
        val encounter = deps.bossDeps.encounter(croc)
        if (now - encounter.lastAbilityTick < CrondisCrocodileCombat.ATTACK_RATE) return
        encounter.lastAbilityTick = now
        croc.anim(CrondisSeqs.CROC_ATTACK)
        attackSound(croc)
        room.drainPalm(PALM_BITE)
    }

    private fun attackSound(croc: Npc) {
        val sound = CrondisSynths.CROC_ATTACK
        deps.worldRepo.soundArea(croc.coords, sound, radius = ATTACK_SOUND_RADIUS)
    }

    fun mayBite(croc: Npc, target: Player): Boolean {
        if (room.stage != ToaStage.STARTED) return false
        val chosen = targets[croc] as? Player
        if (chosen != null) {
            if (chosen === target) return true
            croc.opPlayer2(chosen, deps.aiInteractions)
            return false
        }
        return mayRetaliate(croc, target)
    }

    private fun mayRetaliate(croc: Npc, player: Player): Boolean =
        attackers[croc]?.contains(player) == true && player.containerSlot() == null

    fun biteDamage(target: Player): Int {
        val bonus = DAMAGE_PER_HAZARD * recentHazardHits(target, deps.mapClock.cycle)
        val damage = (BASE_DAMAGE + bonus).coerceAtMost(MAX_DAMAGE)
        return if (target.vars[CrondisVarbits.PROTECT_FROM_MELEE] > 0) damage / 3 else damage
    }

    fun hitBy(croc: Npc, player: Player) {
        attackers.getOrPut(croc) { LinkedHashSet() } += player
    }

    /** Hazard hits on [player] within the last 30 seconds (older ones are dropped). */
    private fun recentHazardHits(player: Player, now: Int): Int {
        val hits = hazardHits[player] ?: return 0
        while (hits.isNotEmpty() && hits.first() <= now - HAZARD_WINDOW) hits.removeFirst()
        return hits.size
    }

    private fun isBesidePalm(croc: Npc, palm: Npc): Boolean {
        val c = croc.coords
        val p = palm.coords
        if (c.level != p.level) return false
        val xOverlap = c.x < p.x + palm.size && c.x + croc.size > p.x
        val zOverlap = c.z < p.z + palm.size && c.z + croc.size > p.z
        val xTouch = c.x + croc.size == p.x || c.x == p.x + palm.size
        val zTouch = c.z + croc.size == p.z || c.z == p.z + palm.size
        return (xOverlap && zTouch) || (zOverlap && xTouch)
    }

    private fun palmApproach(croc: Npc, palm: Npc): CoordGrid {
        val c = croc.coords
        val p = palm.coords
        val gapX = maxOf(p.x - (c.x + croc.size - 1), c.x - (p.x + palm.size - 1))
        val gapZ = maxOf(p.z - (c.z + croc.size - 1), c.z - (p.z + palm.size - 1))
        return if (gapX >= gapZ) {
            val x = c.x.coerceIn(p.x - croc.size, p.x + palm.size)
            val z = c.z.coerceIn(p.z - croc.size + 1, p.z + palm.size - 1)
            CoordGrid(x, z, p.level)
        } else {
            val x = c.x.coerceIn(p.x - croc.size + 1, p.x + palm.size - 1)
            val z = c.z.coerceIn(p.z - croc.size, p.z + palm.size)
            CoordGrid(x, z, p.level)
        }
    }

    private companion object {
        const val RESET_SOONER = 10
        const val MIN_WAVE_GAP = 46
        const val MAX_WAVE_GAP = 50
        const val WAKE_TICKS = 4
        const val ATTACK_SOUND_RADIUS = 4
        const val MAX_ALIVE = 8
        const val AGGRO_RANGE = 3
        const val PALM_BITE = 5
        const val BASE_DAMAGE = 18
        const val DAMAGE_PER_HAZARD = 3
        const val MAX_DAMAGE = 36

        /** 30 seconds. */
        const val HAZARD_WINDOW = 50
    }
}
