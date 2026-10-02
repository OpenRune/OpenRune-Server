package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import java.awt.Color
import org.rsmod.api.player.output.soundSynth
import org.rsmod.api.player.output.spam
import org.rsmod.api.player.stat.statSub
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaHpBar
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.obj.Obj
import org.rsmod.game.region.Region
import org.rsmod.map.CoordGrid

class CrondisPuzzleEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    private val acid = CrondisAcid(this)
    private val spears = CrondisSpears(this)
    internal val crocodiles = CrondisCrocodiles(this)

    internal var palm: Npc? = null
        private set

    private var palmStage = 0

    internal var crocSide: CrocSide = CrondisCoords.CROC_SIDES.first()
        private set

    var water = 0
        private set

    val goal: Int
        get() = WATER_FIRST_PLAYER + (teamSize - 1) * WATER_PER_EXTRA_PLAYER

    private val floorContainers = ArrayList<Obj>()

    override val hpBar =
        ToaHpBar(
            this,
            completion = ToaHpBar.Completion.Fade(BAR_FADE_DELAY, BAR_CLEAR_DELAY),
            remainingOnClose = BAR_REMAINING_DEFAULT,
            currentHp = { water },
            maxHp = { goal },
        ) {
            palm
        }

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
        resetHazards()
    }

    override fun onLeave(player: Player) {
        player.removeContainers()
    }

    override fun onComplete() {
        for (player in players) player.removeContainers()
        for (obj in floorContainers) deps.objRepo.del(obj, Int.MAX_VALUE)
        floorContainers.clear()
        restoreWaterfalls()
        openEndBarrier()
        resetHazards()
        spears.idle()
        crocodiles.clear()
    }

    override fun onReset() {
        for (player in players) player.removeContainers()
        water = 0
        spawnPalm()
        restoreWaterfalls()
        resetHazards()
        spears.idle()
        crocodiles.reset()
    }

    override fun honeyLocusts(): Int = HONEY_LOCUSTS

    fun waterPalm(amount: Int) {
        if (stage != ToaStage.STARTED || amount <= 0) return
        water = (water + amount).coerceAtMost(goal)
        hpBar.update()
        showPalmHit(CrondisMarks.PALM_WATERED, amount)

        val newStage = stageFor(water)
        if (newStage == palmStage) return
        for (player in players) player.soundSynth(CrondisSynths.PALM_GROW)
        setPalmStage(newStage)
        if (newStage == FINAL_STAGE) complete() else pointBarsAtPalm()
    }

    internal fun drainPalm(amount: Int) {
        if (stage != ToaStage.STARTED || water <= 0) return
        val drained = amount.coerceAtMost(water)
        water -= drained
        hpBar.update()
        showPalmHit(CrondisMarks.PALM_DRAINED, drained)
        val newStage = stageFor(water)
        if (newStage < palmStage) {
            for (player in players) player.soundSynth(CrondisSynths.PALM_SHRINK)
            setPalmStage(newStage)
            pointBarsAtPalm()
        }
    }

    private fun stageFor(water: Int): Int {
        val segments = CrondisMarks.PALM_HEADBAR_SEGMENTS
        return (palmFill(water) * FINAL_STAGE / segments).coerceAtMost(FINAL_STAGE)
    }

    private fun palmFill(water: Int): Int {
        if (water <= 0) return 0
        return 1 + water * (CrondisMarks.PALM_HEADBAR_SEGMENTS - 1) / goal
    }

    private fun spawnPalm() {
        palm?.let(::despawn)
        palm = spawn(CrondisNpcs.PALMS[0], coords(CrondisCoords.PALM))
        palmStage = 0
    }

    private fun setPalmStage(stage: Int) {
        val npc = palm ?: return
        palmStage = stage
        if (stage == 0) {
            npc.resetTransmog()
        } else {
            npc.transmog(npcType(CrondisNpcs.PALMS[stage]), Int.MAX_VALUE)
        }
    }

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

    private fun pointBarsAtPalm() {
        schedule(1) { hpBar.point() }
    }

    fun drainWaterfall(waterfall: BoundLocInfo) {
        val refillTicks =
            (BASE_REFILL_TICKS - (teamSize - 1) * REFILL_TICKS_PER_PLAYER).coerceAtLeast(1)
        deps.locRepo.change(waterfall, CrondisLocs.WATER_SOURCE_EMPTY, refillTicks)
    }

    private fun restoreWaterfalls() {
        for (tile in CrondisCoords.WATERFALLS_SOUTH) {
            addLoc(CrondisLocs.WATER_SOURCE, tile, LocAngle.West)
        }
        for (tile in CrondisCoords.WATERFALLS_NORTH) {
            addLoc(CrondisLocs.WATER_SOURCE, tile, LocAngle.East)
        }
    }

    override fun onTick(targets: List<Player>) {
        if ((deps.mapClock.cycle - startCycle) % HEADBAR_INTERVAL == 0) showPalmHeadbar()
        acid.tick(targets)
        spears.tick(targets)
        crocodiles.tick()
    }

    private fun resetHazards() {
        acid.clear()
        spears.clear()
    }

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
        hazardDamage(player, baseDamage, DAMAGE_SPREAD)
        player.statSub("stat.defence", constant = DEFENCE_DRAIN, percent = 0)
        player.statSub("stat.agility", constant = AGILITY_DRAIN, percent = 0)
    }

    internal fun spillWater(player: Player) {
        val slot = player.containerSlot() ?: return
        val water = player.inv[slot]?.vars ?: return
        if (water <= 0) return
        val left =
            if (deps.random.randomBoolean(SMALL_SPILL_CHANCE)) {
                (water - water * SMALL_SPILL_PERCENT / 100).coerceAtLeast(1)
            } else {
                (water + 1) / 2
            }
        player.setContainerWater(slot, left)
        player.spam("Water spills out of your container.")
        player.soundSynth(CrondisSynths.SPILL)
    }

    internal fun biteWater(player: Player) {
        val slot = player.containerSlot() ?: return
        val water = player.inv[slot]?.vars ?: return
        if (water <= 0) return
        val left = if (water < BITE_LOW_WATER) water - BITE_LOW_LOSS else (water + 1) / 2
        player.setContainerWater(slot, left.coerceAtLeast(0))
    }

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

        private const val WATER_FIRST_PLAYER = 175
        private const val WATER_PER_EXTRA_PLAYER = 125
        private const val BASE_REFILL_TICKS = 128
        private const val REFILL_TICKS_PER_PLAYER = 18

        private const val DAMAGE_SPREAD = 8
        private const val DEFENCE_DRAIN = 6
        private const val AGILITY_DRAIN = 3
        private const val END_BARRIER_LENGTH = 3
        private const val HEADBAR_INTERVAL = 2
        private const val HONEY_LOCUSTS = 8
        private const val BITE_LOW_WATER = 15

        private const val SMALL_SPILL_CHANCE = 20
        private const val SMALL_SPILL_PERCENT = 16
        private const val BITE_LOW_LOSS = 10
        private const val BAR_FADE_DELAY = 5
        private const val BAR_CLEAR_DELAY = 9

        private val BAR_REMAINING_DEFAULT = Color(0x00CC00)
    }
}
