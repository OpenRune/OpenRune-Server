package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.types.NpcMode
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.npc.opPlayer2
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionPlayer
import org.rsmod.game.map.Direction
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid
import org.rsmod.map.util.Bounds

internal class CrondisCrocodiles(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private val crocodiles = ArrayList<Npc>()
    private val wakeAt = HashMap<Npc, Int>()
    private var countdown = nextCountdown()

    private val targets = HashMap<Npc, Any>()

    private val attackers = HashMap<Npc, LinkedHashSet<Player>>()

    private val hazardHits = HashMap<Player, ArrayDeque<Int>>()

    private object PalmTarget

    fun tick() {
        if (countdown-- <= 0) {
            countdown = nextCountdown()
            spawnWave()
        }
    }

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

    fun reset() {
        clear()
        countdown = nextCountdown() - RESET_SOONER
    }

    private fun spawnWave() {
        pruneDead()
        if (crocodiles.size >= MAX_ALIVE) return
        val spawns = room.crocSide.spawns
        val count = minOf((room.teamSize + 1) / 2, spawns.size)
        for (i in 0 until count) {
            val tile = spawns[i]
            val croc = room.spawn(CrondisNpcs.CROCODILE, room.coords(tile), facingPalm(tile))
            croc.defaultMoveSpeed = MoveSpeed.Crawl
            croc.noneMode()
            room.palm?.let { croc.faceSquare(it.coords, it.size, it.size) }
            wakeAt[croc] = deps.mapClock.cycle + WAKE_TICKS
            crocodiles += croc
        }
    }

    private fun facingPalm(spawn: CoordGrid): Direction? {
        val palm = room.palm ?: return null
        val size = npcType(CrondisNpcs.CROCODILE).size
        return Direction.between(Bounds(room.coords(spawn), size, size), palm.bounds())
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
        val players = room.targets()
        players
            .firstOrNull { croc.isWithinDistance(it, AGGRO_RANGE) && it.containerWater() > 0 }
            ?.let { return it }
        if (room.water > 0 && room.palm != null) return PalmTarget
        return null
    }

    fun ai(croc: Npc) {
        if (room.stage != ToaStage.STARTED) return
        if ((wakeAt[croc] ?: 0) > deps.mapClock.cycle) return
        val fightingPlayer = croc.mode == NpcMode.OpPlayer2 || croc.mode == NpcMode.ApPlayer2

        when (val target = chooseTarget(croc)) {
            is Player -> {
                val previous = targets.put(croc, target)
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
            val step = nextStep(croc, palmApproach(croc, palm))
            if (step != null) croc.walk(step) else croc.abortRoute()
            return
        }
        val now = deps.mapClock.cycle
        val encounter = deps.bossDeps.encounter(croc)
        if (now - encounter.lastAbilityTick < CrondisCrocodileCombat.ATTACK_RATE) return
        encounter.lastAbilityTick = now
        croc.anim(CrondisSeqs.CROC_ATTACK)
        attackSound(croc)
        room.drainPalm(deps.random.of(MIN_PALM_BITE, MAX_PALM_BITE))
    }

    private fun nextStep(croc: Npc, spot: CoordGrid): CoordGrid? {
        val strategy = croc.collisionStrategy ?: return null
        val from = croc.coords
        if (from == spot) return null
        val first = deps.routeFactory.create(croc.avatar, spot, strategy).firstOrNull()
        if (first != null) {
            val waypoint = CoordGrid(first.x, first.z, first.level)
            if (waypoint != from) {
                val next = deps.stepFactory.unvalidated(from, waypoint)
                if (deps.stepFactory.validated(croc, from, next, strategy) == next) return next
            }
        }
        val fallback = deps.stepFactory.validated(croc, from, spot, strategy)
        return fallback.takeIf { it != CoordGrid.NULL }
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
        const val MIN_PALM_BITE = 2
        const val MAX_PALM_BITE = 5
        const val BASE_DAMAGE = 18
        const val DAMAGE_PER_HAZARD = 3
        const val MAX_DAMAGE = 36
        const val HAZARD_WINDOW = 50
    }
}
