package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.awt.Color
import kotlin.math.floor
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.setColour
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.obj.Obj
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

/**
 * The Crondis puzzle, "Test of Resourcefulness". Port of Offline_Scape CrondisPuzzleEncounter.
 *
 * Players take water containers, fill them at the four waterfalls and water the Palm of
 * Resourcefulness. The palm needs 175 water, +125 per extra player (OSRS Wiki); it grows a stage
 * every 25% (npcs toa_crondis_tree_1..5), and at the fifth stage the room is complete and the end
 * barrier opens.
 *
 * This file is the room's state: the palm and its progress bar, the waterfalls, and the hazard
 * tick that runs [CrondisAcid], [CrondisSpears] and [CrondisCrocodiles]. The ops (take, fill,
 * water, check, empty) are in [CrondisPuzzleScript]; npc behaviour is in `toa_crondis.toml`.
 */
class CrondisPuzzleEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    private val acid = CrondisAcid(this)
    private val spears = CrondisSpears(this)
    private val crocodiles = CrondisCrocodiles(this)

    internal var palm: Npc? = null
        private set

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
        for (tile in CrondisCoords.CONTAINERS) {
            floorContainers += deps.objRepo.add(CrondisObjs.CONTAINER, coords(tile), Int.MAX_VALUE)
        }
        for (tile in CrondisCoords.STATUES_SOUTH) addLoc(CrondisLocs.STATUE, tile, LocAngle.West)
        for (tile in CrondisCoords.STATUES_NORTH) addLoc(CrondisLocs.STATUE, tile, LocAngle.East)
        addLoc(CrondisLocs.PALM_BLOCKER, CrondisCoords.PALM, LocAngle.West)
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
        player.removeContainers()
    }

    override fun onComplete() {
        for (player in players) {
            closeBar(player)
            player.removeContainers()
        }
        for (obj in floorContainers) deps.objRepo.del(obj, Int.MAX_VALUE)
        floorContainers.clear()
        restoreWaterfalls()
        removeEndBarrier()
        resetHazards()
        spears.idle()
        crocodiles.clear()
    }

    override fun onReset() {
        for (player in players) {
            closeBar(player)
            player.removeContainers()
        }
        water = 0
        spawnPalm(stage = 0)
        restoreWaterfalls()
        resetHazards()
        spears.idle()
        crocodiles.reset()
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
        for (player in players) player.soundSynth(CrondisSynths.PALM_GROW)
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
    internal fun drainPalm(amount: Int) {
        if (stage != ToaStage.STARTED || water <= 0) return
        water = (water - amount).coerceAtLeast(0)
        for (player in players) updateBar(player)
        val newStage = stageFor(water)
        if (newStage < palmStage) {
            for (player in players) player.soundSynth(CrondisSynths.PALM_SHRINK)
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
        palm?.let(::despawn)
        palm = spawnNpc(CrondisNpcs.PALMS[stage], CrondisCoords.PALM)
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

    /**
     * The palm's colours (toa_crondis.toml) stay on the overlay after it closes, and
     * BossHpBarScript.onOpen only resets the back and sliding parts, so the next bar (Zebak's)
     * would keep the palm's remaining colour. It goes back to the cache default here.
     */
    private fun closeBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onClose(player, npc, instant = true)
        player.setColour(CrondisComponents.BAR_REMAINING, BAR_REMAINING_DEFAULT)
    }

    // ---- Waterfalls ----

    /**
     * A filled-from waterfall runs dry for a while: the empty variant is added with a duration,
     * and the map's own waterfall comes back when it expires. Offline_Scape: 128 ticks, 18 fewer
     * per extra party member.
     */
    fun drainWaterfall(waterfall: BoundLocInfo) {
        val refillTicks =
            (BASE_REFILL_TICKS - (teamSize - 1) * REFILL_TICKS_PER_PLAYER).coerceAtLeast(1)
        deps.locRepo.change(waterfall, CrondisLocs.WATER_SOURCE_EMPTY, refillTicks)
    }

    /** Offline_Scape spawnWaterfalls: every waterfall full again (end of room, or a reset). */
    private fun restoreWaterfalls() {
        for (tile in CrondisCoords.WATERFALLS_SOUTH) {
            addLoc(CrondisLocs.WATER_SOURCE, tile, LocAngle.West)
        }
        for (tile in CrondisCoords.WATERFALLS_NORTH) {
            addLoc(CrondisLocs.WATER_SOURCE, tile, LocAngle.East)
        }
    }

    // ---- Hazards (Offline_Scape process()) ----

    /**
     * One tick of the hazards. Re-arms itself; completing, resetting or destroying the room drops
     * the pending task (ToaEncounter.schedule), which stops the loop.
     */
    private fun hazardTick() {
        if (stage != ToaStage.STARTED) return
        val targets = hazardTargets()
        acid.tick(targets)
        spears.tick(targets)
        crocodiles.tick()
        schedule(1) { hazardTick() }
    }

    private fun resetHazards() {
        acid.clear()
        spears.clear()
    }

    /** Players the hazards can hit: alive, not a ghost, inside the challenge area. */
    internal fun hazardTargets(): List<Player> =
        players.filter { inChallengeArea(it) && !raid.isGhost(it) && !raid.isDying(it) }

    /**
     * An acid or spear hit (Offline_Scape hit + spillWater + applyDebuffs): damage scaled by raid
     * level, half the container spilled, 6 Defence and 3 Agility drained (capture; Offline_Scape
     * drained 3 of each). [ready] is the hazard's per-player cooldown, so a player standing in one
     * isn't hit every tick.
     */
    internal fun hazardHit(
        player: Player,
        ready: MutableMap<Player, Int>,
        baseDamage: Int,
        cooldown: Int,
    ) {
        val now = deps.mapClock.cycle
        if ((ready[player] ?: 0) > now) return
        ready[player] = now + cooldown
        crocodiles.onHazardHit(player, now)

        spillWater(player)
        val min = floor(baseDamage * raid.damageMultiplier).toInt()
        player.queueHit(
            delay = HIT_DELAY,
            type = HitType.Typeless,
            damage = deps.random.of(min, min + DAMAGE_SPREAD),
            modifier = NoopPlayerHitModifier,
        )
        player.statSub("stat.defence", constant = DEFENCE_DRAIN, percent = 0)
        player.statSub("stat.agility", constant = AGILITY_DRAIN, percent = 0)
    }

    /** Offline_Scape spillWater: half the container (rounded up) is lost. */
    internal fun spillWater(player: Player) {
        val slot = player.containerSlot() ?: return
        val water = player.inv[slot]?.vars ?: return
        if (water <= 0) return
        player.setContainerWater(slot, water - (water + 1) / 2)
        player.mes("Water spills out of your container.")
        player.soundSynth(CrondisSynths.SPILL)
    }

    // ---- Helpers ----

    /**
     * A room npc. `add(npc, Int.MAX_VALUE)` also marks it to respawn after a death, which room
     * npcs never do, so that's switched back off.
     */
    private fun spawnNpc(type: String, static: CoordGrid): Npc {
        val npc = Npc(type, coords(static))
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        return npc
    }

    /**
     * A room npc whose events (ai timer, attack, hits) are routed back to this room. Rooms can be
     * destroyed without completing or resetting, so their leftover entries are dropped here;
     * otherwise the static map would keep whole raids alive.
     */
    internal fun spawnRouted(type: String, static: CoordGrid): Npc {
        owners.values.removeIf { it.destroyed }
        val npc = spawnNpc(type, static)
        owners[npc] = this
        return npc
    }

    internal fun despawn(npc: Npc) {
        owners.remove(npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    /** Offline_Scape onRoomEnd: the barrier on the west side. */
    private fun removeEndBarrier() {
        val barrierId = CrondisLocs.BARRIER.asRSCM(RSCMType.LOC)
        val barrier = ServerCacheManager.getObject(barrierId) ?: return
        for (dz in 0 until END_BARRIER_LENGTH) {
            val tile = coords(CrondisCoords.END_BARRIER.translate(0, dz))
            val loc = deps.locRepo.findExact(tile, barrier) ?: continue
            deps.locRepo.del(loc, Int.MAX_VALUE)
        }
    }

    private fun addLoc(type: String, static: CoordGrid, angle: LocAngle) {
        val shape = LocShape.CentrepieceStraight
        deps.locRepo.add(coords(static), type, Int.MAX_VALUE, angle, shape)
    }

    companion object {
        private const val FINAL_STAGE = 4

        /** OSRS Wiki (Tombs of Amascut/Strategies). Offline_Scape used 200 per player. */
        private const val WATER_FIRST_PLAYER = 175
        private const val WATER_PER_EXTRA_PLAYER = 125
        private const val BASE_REFILL_TICKS = 128
        private const val REFILL_TICKS_PER_PLAYER = 18

        private const val HIT_DELAY = 1
        private const val DAMAGE_SPREAD = 8
        private const val DEFENCE_DRAIN = 6
        private const val AGILITY_DRAIN = 3
        private const val END_BARRIER_LENGTH = 3

        /** osrs-dumps interface/hpbar_hud.if3, health_bar_remaining. */
        private val BAR_REMAINING_DEFAULT = Color(0x00CC00)

        /** Client script 1846: `seq_prefetch(seq)`. */
        private const val SCRIPT_SEQ_PREFETCH = 1846

        /** Offline_Scape CrondisPuzzleEncounter.enter: animations 9618-9646, 9532-9534, 9541. */
        private val PRELOAD_SEQS: List<Int> = (9618..9646).toList() + listOf(9532, 9533, 9534, 9541)

        // ---- Event routing (CrondisPuzzleScript) ----

        private val owners = HashMap<Npc, CrondisPuzzleEncounter>()

        private fun roomOf(npc: Npc): CrondisPuzzleEncounter? {
            val room = owners[npc] ?: return null
            if (room.destroyed) {
                owners.remove(npc)
                return null
            }
            return room
        }

        internal fun onCrocodileTick(croc: Npc) {
            roomOf(croc)?.crocodiles?.ai(croc)
        }

        internal fun onCrocodileAttack(croc: Npc, target: Player) {
            roomOf(croc)?.crocodiles?.attack(croc, target)
        }

        internal fun onCrocodileHit(croc: Npc, player: Player) {
            roomOf(croc)?.crocodiles?.hitBy(croc, player)
        }
    }
}
