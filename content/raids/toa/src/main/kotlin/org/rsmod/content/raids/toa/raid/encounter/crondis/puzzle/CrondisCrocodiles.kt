package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.types.NpcMode
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.movement.MoveSpeed
import org.rsmod.map.CoordGrid

/**
 * The crocodiles (OSRS Wiki: Crocodile (Tombs of Amascut)). Capture: the first wave 48 ticks after
 * the first hazard tick, then one every 47 (Offline_Scape: 60, then 60); up to 8 alive. They crawl
 * (a tile every 2 ticks) and wait 4 ticks after spawning before going for anything. Targets,
 * re-evaluated every tick:
 * 1. a player with a non-empty water container within aggro range;
 * 2. the palm, while it has any growth;
 * 3. a player without a water container who has attacked this crocodile.
 * So attacking a crocodile while holding an empty container is safe.
 *
 * Its attack is custom (onAiOpPlayer2 for its type replaces the default npc combat). Killed
 * crocodiles die through the standard npc death, which deletes them.
 */
internal class CrondisCrocodiles(private val room: CrondisPuzzleEncounter) {
    private val deps = room.raid.deps
    private val crocodiles = ArrayList<Npc>()
    private val attackReady = HashMap<Npc, Int>()
    private val wakeAt = HashMap<Npc, Int>()
    private var countdown = FIRST_DELAY

    /** What each crocodile is going for this tick: a [Player], [PalmTarget], or nothing. */
    private val targets = HashMap<Npc, Any>()

    /** Players who have hit each crocodile (its third-priority targets). */
    private val attackers = HashMap<Npc, LinkedHashSet<Player>>()

    /** Map cycles of each player's recent hazard hits, for the attack's damage. */
    private val hazardHits = HashMap<Player, ArrayDeque<Int>>()

    private object PalmTarget

    fun tick() {
        if (countdown-- <= 0) {
            countdown = INTERVAL
            spawnWave()
        }
    }

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
     * Offline_Scape onRoomReset: the next attempt's first wave comes 10 ticks sooner than the
     * first attempt's (unverified).
     */
    fun reset() {
        clear()
        countdown = RESET_DELAY
    }

    /** Offline_Scape process(): ceil(teamSize / 2) crocodiles (at most 4) per wave, max 8 alive. */
    private fun spawnWave() {
        pruneDead()
        if (crocodiles.size >= MAX_ALIVE) return
        val count = minOf((room.teamSize + 1) / 2, CrondisCoords.CROC_SPAWNS.size)
        for (i in 0 until count) {
            val croc = room.spawnRouted(CrondisNpcs.CROCODILE, CrondisCoords.CROC_SPAWNS[i])
            croc.defaultMoveSpeed = MoveSpeed.Crawl
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
        attackReady.remove(croc)
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
        val attackedBy = attackers[croc] ?: return null
        return players.firstOrNull { it in attackedBy && it.containerSlot() == null }
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
                if (fightingPlayer) croc.noneMode()
                approachPalm(croc)
            }
            else -> {
                targets.remove(croc)
                if (fightingPlayer) croc.noneMode()
            }
        }
    }

    /**
     * Crawls next to the palm, then bites it: 2-5 growth every 7 ticks (attack speed 7). Capture:
     * it faces the palm the whole way (entity facing), and each bite plays the attack sound.
     */
    private fun approachPalm(croc: Npc) {
        val palm = room.palm ?: return
        croc.faceNpc(palm)
        if (!croc.isWithinDistance(palm, 1)) {
            croc.walk(besidePalm(croc, palm))
            return
        }
        val now = deps.mapClock.cycle
        if ((attackReady[croc] ?: 0) > now) return
        attackReady[croc] = now + ATTACK_RATE
        croc.anim(CrondisSeqs.CROC_ATTACK)
        attackSound(croc)
        room.drainPalm(deps.random.of(MIN_DRAIN, MAX_DRAIN))
    }

    private fun attackSound(croc: Npc) {
        val sound = CrondisSynths.CROC_ATTACK
        deps.worldRepo.soundArea(croc.coords, sound, radius = ATTACK_SOUND_RADIUS)
    }

    /**
     * The attack on a player (its onAiOpPlayer2), per the wiki:
     * - a normal crush accuracy roll decides success;
     * - a success always hits 18, +3 for each hazard (acid or spear) that hit this player in the
     *   last 30 seconds, at most 36, regardless of raid level;
     * - through Protect from Melee a success still hits a third of that and drains 12 Prayer;
     * - a success also spills half the player's water, like the hazards.
     *
     * The engine can also get here by itself: players who hit a crocodile make it retaliate. The
     * attack only goes ahead against the target the priority rules chose; otherwise it's
     * redirected.
     */
    fun attack(croc: Npc, target: Player) {
        if (room.stage != ToaStage.STARTED) return
        val chosen = targets[croc] as? Player
        if (chosen == null) {
            croc.noneMode()
            return
        }
        if (chosen !== target) {
            croc.opPlayer2(chosen, deps.aiInteractions)
            return
        }

        val now = deps.mapClock.cycle
        if ((attackReady[croc] ?: 0) > now) return
        attackReady[croc] = now + ATTACK_RATE

        croc.facePlayer(target)
        croc.anim(CrondisSeqs.CROC_ATTACK)
        attackSound(croc)
        val accuracy = deps.accuracy
        val success = accuracy.rollMeleeAccuracy(croc, target, MeleeAttackType.Crush, deps.random)
        if (!success) {
            target.queueHit(croc, HIT_DELAY, HitType.Melee, 0, NoopPlayerHitModifier)
            return
        }

        val bonus = DAMAGE_PER_HAZARD * recentHazardHits(target, now)
        var damage = (BASE_DAMAGE + bonus).coerceAtMost(MAX_DAMAGE)
        if (target.vars[CrondisVarbits.PROTECT_FROM_MELEE] > 0) {
            damage /= 3
            target.statSub("stat.prayer", constant = PRAYER_DRAIN, percent = 0)
        }
        // NoopPlayerHitModifier: prayer is already accounted for above, not by the processor.
        target.queueHit(croc, HIT_DELAY, HitType.Melee, damage, NoopPlayerHitModifier)
        room.spillWater(target)
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

    private companion object {
        const val FIRST_DELAY = 48
        const val RESET_DELAY = 38
        const val INTERVAL = 46
        const val WAKE_TICKS = 4
        const val ATTACK_SOUND_RADIUS = 4
        const val MAX_ALIVE = 8
        const val AGGRO_RANGE = 3
        const val ATTACK_RATE = 7
        const val MIN_DRAIN = 2
        const val MAX_DRAIN = 5
        const val HIT_DELAY = 1
        const val BASE_DAMAGE = 18
        const val DAMAGE_PER_HAZARD = 3
        const val MAX_DAMAGE = 36
        const val PRAYER_DRAIN = 12

        /** 30 seconds. */
        const val HAZARD_WINDOW = 50
    }
}
