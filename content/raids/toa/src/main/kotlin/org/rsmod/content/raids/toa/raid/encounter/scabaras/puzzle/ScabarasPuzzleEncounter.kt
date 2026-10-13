package org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle

import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.midiSong
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.soundSynth
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.Arrival
import org.rsmod.content.raids.toa.raid.encounter.ToaCombatant
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.scabaras.puzzle.ScabarasCoords.plus
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.content.raids.toa.raid.shuffled
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class ScabarasPuzzleEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    internal var layout: List<PuzzleType> = PuzzleType.entries
        private set

    internal val completed = BooleanArray(ScabarasCoords.SLOT_COUNT)

    internal val memory = ScabarasMemory(this)
    internal val sum = ScabarasSum(this)
    internal val lights = ScabarasLights(this)
    internal val pillars = ScabarasPillars(this)
    internal val match = ScabarasMatch(this)
    private val scarabs = ScabarasScarabs(this)
    private val gates = arrayOfNulls<LocInfo>(ScabarasCoords.SLOT_COUNT)
    private val exit = ArrayList<LocInfo>()
    private val lastTiles = HashMap<Player, CoordGrid>()

    override val combatantSpecs: List<ToaCombatant.Spec>
        get() = listOf(ToaCombatant.Spec(ScabarasNpcs.SCARAB, pointMultiplier = SCARAB_POINTS))

    /**
     * The client's scene is built around wherever the player is when the map loads. OSRS lands the
     * player on the room's centre for one tick before the real spawn, which fixes the scene base;
     * RuneLite puzzle solvers locate the plates by those fixed scene coordinates.
     */
    override fun arrival(): Arrival =
        Arrival(
            coords(room.randomSpawn(deps.random)),
            facing = null,
            sceneCentre = coords(ScabarasCoords.SCENE_CENTRE),
        )

    override fun onBuilt() {
        layout = deps.random.shuffled(PuzzleType.entries)
        build()
    }

    override fun onStart() {
        for (player in players) player.midiSong(ScabarasMidis.PATH)
        scarabs.start()
    }

    override fun onTick(targets: List<Player>) {
        for (player in targets) {
            val coords = player.coords
            if (lastTiles.put(player, coords) != coords) step(player, staticCoords(coords))
        }
        scarabs.tick(targets)
    }

    override fun onComplete() {
        scarabs.clear()
    }

    override fun onReset() {
        lastTiles.clear()
        clear()
        build()
    }

    internal fun slotOf(type: PuzzleType): Int = layout.indexOf(type)

    internal fun playing(): Boolean = stage == ToaStage.STARTED

    internal fun slotOf(player: Player): Int {
        val static = staticCoords(player.coords)
        return ScabarasCoords.slotOf(static)
    }

    internal fun canUse(slot: Int): Boolean {
        val prerequisite = ScabarasCoords.PREREQUISITE[slot]
        return playing() && (prerequisite < 0 || completed[prerequisite])
    }

    internal fun step(player: Player, static: CoordGrid) {
        if (!playing()) return
        memory.step(player, static)
        sum.step(player, static)
        lights.step(player, static)
    }

    internal fun completeSlot(slot: Int) {
        if (completed[slot]) return
        completed[slot] = true
        openGate(slot)
        val label = ScabarasCoords.LABELS[slot]
        for (player in players) player.mes(ScabarasMessages.completed(label))
        for (player in challengePlayers) player.soundSynth(ScabarasSynths.COMPLETE)
    }

    internal fun completeFinal() {
        if (stage != ToaStage.STARTED) return
        for (player in players) player.mes(ScabarasMessages.completed(ScabarasCoords.FINAL_LABEL))
        for (player in challengePlayers) player.soundSynth(ScabarasSynths.COMPLETE)
        for (loc in exit) replaceWithOpen(loc)
        exit.clear()
        complete()
    }

    internal fun smallRocks(player: Player, damage: Int = SMALL_ROCK_DAMAGE) {
        player.soundSynth(ScabarasSynths.WRONG)
        deps.worldRepo.spotanimMap(spotanim(ScabarasSpots.SMALL_ROCKFALL), player.coords, 0, 0)
        shake(player)
        hazardDamage(player, damage, SMALL_ROCK_SPREAD)
    }

    internal fun shake(player: Player) {
        Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, 5, 5, 7)
        schedule(SHAKE_TICKS) { Camera.camReset(player) }
    }

    internal fun addLoc(
        static: CoordGrid,
        loc: String,
        angle: LocAngle = LocAngle.West,
        shape: LocShape = LocShape.GroundDecor,
    ): LocInfo = deps.locRepo.add(coords(static), loc, Int.MAX_VALUE, angle, shape)

    internal fun delLoc(loc: LocInfo) {
        deps.locRepo.del(loc, Int.MAX_VALUE)
    }

    internal fun base(slot: Int): CoordGrid = ScabarasCoords.BASES[slot]

    internal fun plate(slot: Int, offset: Pair<Int, Int>): CoordGrid = base(slot).plus(offset)

    private fun build() {
        completed.fill(false)
        for (slot in 0 until ScabarasCoords.SLOT_COUNT) {
            gates[slot] =
                addLoc(
                    ScabarasCoords.GATES[slot],
                    ScabarasLocs.BARRIER,
                    LocAngle.South,
                    LocShape.CentrepieceStraight,
                )
        }
        for (tile in ScabarasCoords.EXIT_BARRIER) {
            exit +=
                addLoc(tile, ScabarasLocs.BARRIER, LocAngle.South, LocShape.CentrepieceStraight)
        }
        memory.build(slotOf(PuzzleType.MEMORY))
        sum.build(slotOf(PuzzleType.SUM))
        lights.build(slotOf(PuzzleType.LIGHT))
        pillars.build(slotOf(PuzzleType.PILLAR))
        match.build()
    }

    private fun clear() {
        memory.clear()
        sum.clear()
        lights.clear()
        pillars.clear()
        match.clear()
        scarabs.clear()
        for (gate in gates) gate?.let(::delLoc)
        gates.fill(null)
        for (loc in exit) delLoc(loc)
        exit.clear()
    }

    private fun openGate(slot: Int) {
        val gate = gates[slot] ?: return
        replaceWithOpen(gate)
        gates[slot] = null
    }

    private fun replaceWithOpen(loc: LocInfo) {
        deps.locRepo.add(loc.coords, ScabarasLocs.OPEN, Int.MAX_VALUE, loc.angle, loc.shape)
    }

    internal fun debugCompleteSlot(slot: Int) = completeSlot(slot)

    internal fun layoutText(): String =
        layout.withIndex().joinToString { (slot, type) -> "$slot=$type" }

    private companion object {
        const val SCARAB_POINTS = 0.5
        const val SMALL_ROCK_DAMAGE = 6
        const val SMALL_ROCK_SPREAD = 2
        const val SHAKE_TICKS = 3
    }
}
