package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import java.awt.Color
import kotlin.math.floor
import org.rsmod.api.player.hit.modifier.NoopPlayerHitModifier
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.setColour
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.npcType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.HitType
import org.rsmod.game.hit.Hitmark
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
 * Resourcefulness. The palm needs 175 water, +125 per extra player (OSRS Wiki); it grows a stage
 * every 25% by transforming into toa_crondis_tree_1..5, and at the fifth stage the room is
 * complete and the end barrier opens.
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

    /** Picked when the room is built (see [CrondisCoords.CROC_SIDES]). */
    internal var crocSide: CrocSide = CrondisCoords.CROC_SIDES.first()
        private set

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
        val sides = CrondisCoords.CROC_SIDES
        crocSide = sides[deps.random.of(maxExclusive = sides.size)]
        for ((tile, angle) in crocSide.walls) addLoc(CrondisLocs.CROC_WALL, tile, angle)
        addLoc(CrondisLocs.PALM_BLOCKER, CrondisCoords.PALM, LocAngle.West)
        spawnPalm()
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

    /** Capture: the bar stays up, fades out 5 ticks after completion and is cleared 4 later. */
    override fun onComplete() {
        for (player in players) player.removeContainers()
        schedule(BAR_FADE_DELAY) {
            for (player in players) player.runClientScript(SCRIPT_HP_HUD_FADE_OUT, fadeArgs())
        }
        schedule(BAR_CLEAR_DELAY) {
            for (player in players) {
                closeBar(player)
                player.clearBarVars()
            }
        }
        for (obj in floorContainers) deps.objRepo.del(obj, Int.MAX_VALUE)
        floorContainers.clear()
        restoreWaterfalls()
        openEndBarrier()
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
        spawnPalm()
        restoreWaterfalls()
        resetHazards()
        spears.idle()
        crocodiles.reset()
    }

    /** Capture (solo, raid level 45): 8. */
    override fun honeyLocusts(): Int = HONEY_LOCUSTS

    // ---- The palm ----

    /**
     * Adds [amount] water to the palm. Capture: the hitsplat shows everything poured, even past
     * the goal. Grows the palm when it crosses a 25% step; the last step completes the room.
     */
    fun waterPalm(amount: Int) {
        if (stage != ToaStage.STARTED || amount <= 0) return
        water = (water + amount).coerceAtMost(goal)
        for (player in players) updateBar(player)
        showPalmHit(CrondisMarks.PALM_WATERED, amount)

        val newStage = stageFor(water)
        if (newStage == palmStage) return
        for (player in players) player.soundSynth(CrondisSynths.PALM_GROW)
        setPalmStage(newStage)
        // Capture: the finished palm never gets the bar; it fades out instead (onComplete).
        if (newStage == FINAL_STAGE) complete() else pointBarsAtPalm()
    }

    /**
     * A crocodile's bite: the palm loses water and shrinks back a stage if it drops below one
     * (Offline_Scape PALM_LOWER hit; the capture never drops below a stage). Never undoes
     * completion: at the last stage the room is over.
     */
    internal fun drainPalm(amount: Int) {
        if (stage != ToaStage.STARTED || water <= 0) return
        val drained = amount.coerceAtMost(water)
        water -= drained
        for (player in players) updateBar(player)
        showPalmHit(CrondisMarks.PALM_DRAINED, drained)
        val newStage = stageFor(water)
        if (newStage < palmStage) {
            for (player in players) player.soundSynth(CrondisSynths.PALM_SHRINK)
            setPalmStage(newStage)
            pointBarsAtPalm()
        }
    }

    /**
     * The stage is a quarter of the headbar. Captures: 43 water (fill 30) kept tree_2 and 41
     * (fill 28) dropped to tree_1; Offline_Scape's water * 4 / goal would drop at 43.
     */
    private fun stageFor(water: Int): Int {
        val segments = CrondisMarks.PALM_HEADBAR_SEGMENTS
        return (palmFill(water) * FINAL_STAGE / segments).coerceAtMost(FINAL_STAGE)
    }

    /**
     * Headbar fill: 0 when dry, else 1 + water * (segments - 1) / goal. Matches all 22 capture
     * samples (e.g. 2 -> 2, 50 -> 35, 143 -> 98, 175 -> 120); plain rounding matches few.
     */
    private fun palmFill(water: Int): Int {
        if (water <= 0) return 0
        return 1 + water * (CrondisMarks.PALM_HEADBAR_SEGMENTS - 1) / goal
    }

    /** A new palm at the first stage (room built or reset). */
    private fun spawnPalm() {
        palm?.let(::despawn)
        palm = spawnNpc(CrondisNpcs.PALMS[0], CrondisCoords.PALM)
        palmStage = 0
    }

    /** Capture: the palm transforms (tree_1 -> 2 -> 4 -> 5 in one run); it isn't replaced. */
    private fun setPalmStage(stage: Int) {
        val npc = palm ?: return
        palmStage = stage
        if (stage == 0) {
            npc.resetTransmog()
        } else {
            npc.transmog(npcType(CrondisNpcs.PALMS[stage]), Int.MAX_VALUE)
        }
    }

    // ---- The palm's headbar and hitsplats ----

    private fun showPalmHit(hitmark: Int, amount: Int) {
        val npc = palm ?: return
        npc.showHitmark(Hitmark.fromNoSource(hitmark, hitmark, hitmark, amount, delay = 0))
        showPalmHeadbar()
    }

    private fun showPalmHeadbar() {
        val npc = palm ?: return
        val fill = palmFill(water)
        val id = CrondisMarks.PALM_HEADBAR
        npc.showHeadbar(Headbar.fromNoSource(id, id, fill, fill, startTime = 0, endTime = 0))
    }

    // ---- Progress bar (Offline_Scape HpHud: 0 -> goal as the palm is watered) ----

    /** onOpen points the bar at the base npc; the palm may already be transformed. */
    private fun openBar(player: Player) {
        val npc = palm ?: return
        deps.bossHpBar.onOpen(player, npc)
        player.barNpc = npc.visType.id
        updateBar(player)
    }

    /** Capture: the bar's npc follows a transform a tick later, with no reopen. */
    private fun pointBarsAtPalm() {
        schedule(1) {
            val npc = palm ?: return@schedule
            for (player in players) player.barNpc = npc.visType.id
        }
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

    private fun Player.clearBarVars() {
        barNpc = BAR_NPC_NONE
        barHp = 0
        barBaseHp = 0
        barBoss = 0
    }

    /** hp_hud_fade_out's arguments: the bar's components, then 0. */
    private fun fadeArgs(): List<Any> = deps.bossHpBar.commonComponents.toList() + 0

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
        // Capture: re-sent every 2 ticks from 2 ticks after the start, even when unchanged.
        if ((deps.mapClock.cycle - startCycle) % HEADBAR_INTERVAL == 0) showPalmHeadbar()
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

    /** Offline_Scape spillWater: a hazard hit spills half the container. */
    internal fun spillWater(player: Player) {
        val slot = player.containerSlot() ?: return
        val water = player.inv[slot]?.vars ?: return
        if (water <= 0) return
        val left =
            if (deps.random.randomBoolean(SMALL_SPILL_CHANCE)) {
                // OSRS Wiki: a slight chance to lose 16% instead, never going below 1%.
                (water - water * SMALL_SPILL_PERCENT / 100).coerceAtLeast(1)
            } else {
                // Capture: half, rounded up, stays (25 -> 13 -> 7 -> 4). Offline_Scape: down.
                (water + 1) / 2
            }
        player.setContainerWater(slot, left)
        // Capture: a spam message.
        player.spam("Water spills out of your container.")
        player.soundSynth(CrondisSynths.SPILL)
    }

    /**
     * A crocodile's bite on [player]: half the container, or 10 once it's under 15 (OSRS Wiki).
     * Capture: no message or sound, and 2% went to 0.
     */
    internal fun biteWater(player: Player) {
        val slot = player.containerSlot() ?: return
        val water = player.inv[slot]?.vars ?: return
        if (water <= 0) return
        val left = if (water < BITE_LOW_WATER) water - BITE_LOW_LOSS else (water + 1) / 2
        player.setContainerWater(slot, left.coerceAtLeast(0))
    }

    // ---- Helpers ----

    /**
     * A room npc. `add(npc, Int.MAX_VALUE)` also marks it to respawn after a death, which room
     * npcs never do, so that's switched back off.
     */
    private fun spawnNpc(type: String, static: CoordGrid, facing: Direction? = null): Npc {
        val npc = Npc(type, coords(static))
        if (facing != null) npc.respawnDir = facing
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.respawns = false
        return npc
    }

    /**
     * A room npc whose events (ai timer, attack, hits) are routed back to this room. Rooms can be
     * destroyed without completing or resetting, so their leftover entries are dropped here;
     * otherwise the static map would keep whole raids alive.
     */
    internal fun spawnRouted(type: String, static: CoordGrid, facing: Direction? = null): Npc {
        owners.values.removeIf { it.destroyed }
        val npc = spawnNpc(type, static, facing)
        owners[npc] = this
        return npc
    }

    internal fun despawn(npc: Npc) {
        owners.remove(npc)
        if (npc.isSlotAssigned) deps.npcRepo.del(npc, Int.MAX_VALUE)
    }

    /**
     * Capture: the barrier on the west side becomes invisible_type8_nonblocking (rotation 3).
     * In an instance, adding a loc over a map loc in the same layer only overlays it: the map
     * loc's collision stays. So the barrier is deleted first, which clears its collision.
     */
    private fun openEndBarrier() {
        val shape = LocShape.CentrepieceStraight
        for (dz in 0 until END_BARRIER_LENGTH) {
            val static = CrondisCoords.END_BARRIER.translate(0, dz)
            val barrier = deps.locRepo.findExact(coords(static), shape)
            if (barrier != null) deps.locRepo.del(barrier, Int.MAX_VALUE)
            addLoc(CrondisLocs.BARRIER_OPEN, static, LocAngle.South)
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
        private const val HEADBAR_INTERVAL = 2
        private const val HONEY_LOCUSTS = 8
        private const val BITE_LOW_WATER = 15

        /**
         * The wiki gives no rate for the 16% spill, and 13 captured spills were all halves; a
         * 1-in-20 guess. Tune freely.
         */
        private const val SMALL_SPILL_CHANCE = 20
        private const val SMALL_SPILL_PERCENT = 16
        private const val BITE_LOW_LOSS = 10
        private const val BAR_FADE_DELAY = 5
        private const val BAR_CLEAR_DELAY = 9
        private const val BAR_NPC_NONE = -1

        /** Client script 2889: `hp_hud_fade_out`. */
        private const val SCRIPT_HP_HUD_FADE_OUT = 2889

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

        internal fun onCrocodileCombatTick(croc: Npc, target: Player): Boolean =
            roomOf(croc)?.crocodiles?.mayBite(croc, target) ?: false

        internal fun crocodileBiteDamage(croc: Npc, target: Player): Int =
            roomOf(croc)?.crocodiles?.biteDamage(target) ?: 0

        internal fun onCrocodileBite(croc: Npc, target: Player) {
            roomOf(croc)?.biteWater(target)
        }

        internal fun onCrocodileHit(croc: Npc, player: Player) {
            roomOf(croc)?.crocodiles?.hitBy(croc, player)
        }
    }
}

private var Player.barNpc by intVarp("varp.hpbar_hud_npc")
private var Player.barHp by intVarBit("varbit.hpbar_hud_hp")
private var Player.barBaseHp by intVarBit("varbit.hpbar_hud_basehp")
private var Player.barBoss by intVarBit("varbit.hpbar_hud_boss")
