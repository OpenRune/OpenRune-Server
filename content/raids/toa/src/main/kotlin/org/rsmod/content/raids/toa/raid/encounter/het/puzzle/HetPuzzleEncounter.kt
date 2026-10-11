package org.rsmod.content.raids.toa.raid.encounter.het.puzzle

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.min
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.output.spam
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.ToaHetSealRow
import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.content.raids.toa.raid.encounter.ToaEncounter
import org.rsmod.content.raids.toa.raid.encounter.ToaHpBar
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.hit.Hitmark
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.Direction
import org.rsmod.map.CoordGrid

class HetPuzzleEncounter(
    raid: ToaRaid,
    room: ToaRoom,
    session: InstanceSession,
    controllerId: Int,
) : ToaEncounter(raid, room, session, controllerId) {

    internal val board = HetBoard(this)
    private val orbs = HetOrbs(this)

    internal var seal: Npc? = null
        private set

    var sealHp = 0
        private set

    val sealMax: Int
        get() = SEAL_HP.getValue(teamSize.coerceIn(SEAL_HP.keys.min(), SEAL_HP.keys.max()))

    var weakened = false
        private set

    private var weakenEnd = 0
    private var nextShot = 0
    private var layoutIndex = -1

    override val hpBar =
        ToaHpBar(
            this,
            completion = ToaHpBar.Completion.Fade(BAR_FADE_DELAY, BAR_CLEAR_DELAY),
            currentHp = { sealHp },
            maxHp = { sealMax },
        ) {
            seal
        }

    private val level: Int by lazy { coords(HetCoords.SEAL).level }

    override fun onBuilt() {
        spawnSeal()
    }

    override fun onStart() {
        sealHp = sealMax
        weakened = false
        nextShot = startCycle + FIRST_SHOT_DELAY
        draft()
    }

    override fun onEnter(player: Player) {
        schedule(STATUE_DELAY) { addStatues() }
    }

    override fun onLeave(player: Player) {
        player.removeMirrors()
        Camera.camReset(player)
    }

    override fun onComplete() {
        weakened = false
        orbs.clear()
        for (player in players) {
            player.removeMirrors()
            Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, 5, 15, 10)
        }
        schedule(SHAKE_TICKS) { for (player in players) Camera.camReset(player) }
        seal?.let(::despawn)
        deps.locRepo.add(
            coords(HetCoords.SEAL),
            HetLocs.GOAL_DEAD,
            Int.MAX_VALUE,
            LocAngle.West,
            LocShape.CentrepieceStraight,
        )
        openExit()
    }

    override fun onReset() {
        orbs.clear()
        board.clear()
        for (player in players) {
            player.removeMirrors()
            Camera.camReset(player)
        }
        weakened = false
        sealHp = 0
        spawnSeal()
    }

    override fun onTick(targets: List<Player>) {
        val now = deps.mapClock.cycle
        val elapsed = now - startCycle
        if (weakened && now >= weakenEnd) protect(now)
        if (!weakened && now >= nextShot) {
            nextShot = now + BEAM_PERIOD
            fireBeam(targets)
        }
        if (elapsed % GLOW_PERIOD == 0) glow()
        if (elapsed % HEADBAR_INTERVAL == 0) showSealHeadbar()
        orbs.tick(elapsed, targets)
    }

    internal fun damageSeal(damage: Int): Int {
        if (stage != ToaStage.STARTED || !weakened) return 0
        val dealt = min(damage, sealHp)
        sealHp -= dealt
        hpBar.update()
        val npc = seal ?: return dealt
        val mark = HetMarks.SEAL_HITMARK
        npc.showHitmark(Hitmark.fromNoSource(mark, mark, mark, dealt, delay = 0))
        showSealHeadbar()
        if (sealHp <= 0) complete()
        return dealt
    }

    internal fun orbBlocked(tile: CoordGrid): Boolean = board.orbBlocked(tile)

    internal fun orbHitsMirror(tile: CoordGrid) = board.orbHitsMirror(tile)

    internal fun orbSpawnTile(dir: HetDir, random: GameRandom): CoordGrid? =
        board.orbSpawnTile(dir, random)

    internal fun insidePuzzle(tile: CoordGrid): Boolean =
        stage == ToaStage.STARTED && staticCoords(tile).within(HetCoords.MIN, HetCoords.MAX)

    internal fun debugBeam(targets: List<Player>) = fireBeam(targets, damage = false)

    internal fun debugWeaken() {
        if (stage == ToaStage.STARTED) weaken()
    }

    internal fun debugRedraft() {
        if (stage == ToaStage.STARTED) draft()
    }

    private fun spawnSeal() {
        seal?.let(::despawn)
        seal = spawn(HetNpcs.SEAL, coords(HetCoords.SEAL), Direction.South)
    }

    private fun draft() {
        val layouts = HetLayouts.ALL
        var next = deps.random.of(maxExclusive = layouts.size)
        if (layouts.size > 1 && next == layoutIndex) next = (next + 1) % layouts.size
        layoutIndex = next
        for (player in players) player.removeMirrors()
        board.build(layouts[next])
    }

    private fun fireBeam(targets: List<Player>, damage: Boolean = true) {
        val start = coords(HetCoords.BEAM_START)
        val path = HetBeam.trace(board.field, start.x, start.z, HetDir.West)
        showBeam(path)
        if (damage) {
            val lit = path.tiles.mapTo(HashSet()) { it.x to it.z }
            for (player in targets) {
                if (player.coords.x to player.coords.z in lit) {
                    hazardDamage(player, BEAM_DAMAGE, BEAM_SPREAD)
                }
            }
        }
        if (path.reachedTarget && damage) schedule(1) { weaken() }
    }

    private fun showBeam(path: HetBeamPath) {
        val world = deps.worldRepo
        path.tiles.forEachIndexed { index, tile ->
            val spot = spotanim(beamSpot(tile.gfx))
            world.spotanimMap(spot, CoordGrid(tile.x, tile.z, level), BEAM_HEIGHT, index + 1)
        }
        val impact = spotanim(HetSpots.BEAM_IMPACT)
        val end = CoordGrid(path.endX, path.endZ, level)
        world.spotanimMap(impact, end, BEAM_HEIGHT, path.tiles.size)
    }

    private fun beamSpot(gfx: HetBeamGfx): String =
        when (gfx) {
            HetBeamGfx.Horizontal -> HetSpots.BEAM_HORIZONTAL
            HetBeamGfx.Vertical -> HetSpots.BEAM_VERTICAL
            HetBeamGfx.NorthEast -> HetSpots.BEAM_NORTH_EAST
            HetBeamGfx.SouthEast -> HetSpots.BEAM_SOUTH_EAST
            HetBeamGfx.SouthWest -> HetSpots.BEAM_SOUTH_WEST
            HetBeamGfx.NorthWest -> HetSpots.BEAM_NORTH_WEST
        }

    private fun weaken() {
        if (stage != ToaStage.STARTED || weakened) return
        val npc = seal ?: return
        weakened = true
        board.clear()
        weakenEnd = deps.mapClock.cycle + WEAKEN_TICKS
        npc.transmog(npcType(HetNpcs.SEAL_WEAK), Int.MAX_VALUE)
        hpBar.point()
        for (player in players) {
            player.mes(STRUCK_MESSAGE)
            player.spam(SHAKE_MESSAGE)
            Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, 2, 10, 10)
        }
        schedule(SHAKE_TICKS) { for (player in players) Camera.camReset(player) }
    }

    private fun protect(now: Int) {
        weakened = false
        seal?.resetTransmog()
        hpBar.point()
        nextShot = now + FIRST_SHOT_DELAY
        draft()
    }

    private fun glow() {
        val npc = seal ?: return
        npc.spotanim(if (weakened) HetSpots.SEAL_GLOW_WEAK else HetSpots.SEAL_GLOW, 0, 0, 0)
    }

    private fun showSealHeadbar() {
        val npc = seal ?: return
        val fill = (sealHp * HetMarks.SEAL_HEADBAR_SEGMENTS + sealMax / 2) / sealMax
        val id = HetMarks.SEAL_HEADBAR
        npc.showHeadbar(Headbar.fromNoSource(id, id, fill, fill, startTime = 0, endTime = 0))
    }

    private fun addStatues() {
        val shape = LocShape.CentrepieceStraight
        val statues =
            listOf(
                HetCoords.CASTER_MIN to (HetLocs.CASTER to LocAngle.North),
                HetCoords.TARGET_MIN to (HetLocs.TARGET to LocAngle.South),
            )
        for ((static, statue) in statues) {
            val tile = coords(static)
            deps.locRepo.findExact(tile, shape)?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
            deps.locRepo.add(tile, statue.first, Int.MAX_VALUE, statue.second, shape)
        }
    }

    private fun openExit() {
        val shape = LocShape.CentrepieceStraight
        for (static in HetCoords.EXIT_BARRIER) {
            val tile = coords(static)
            deps.locRepo.findExact(tile, shape)?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
            deps.locRepo.add(tile, HetLocs.EXIT_OPEN, Int.MAX_VALUE, LocAngle.South, shape)
        }
    }

    companion object {
        private const val STATUE_DELAY = 2
        private const val FIRST_SHOT_DELAY = 10
        private const val BEAM_PERIOD = 9
        private const val WEAKEN_TICKS = 24
        private const val GLOW_PERIOD = 3
        private const val HEADBAR_INTERVAL = 2
        private const val SHAKE_TICKS = 3
        private const val BEAM_HEIGHT = 255
        private const val BEAM_DAMAGE = 8
        private const val BEAM_SPREAD = 2
        private const val BAR_FADE_DELAY = 5
        private const val BAR_CLEAR_DELAY = 9
        private const val STRUCK_MESSAGE = "<col=185820>The statue has been struck! The seal weakens!</col>"
        private const val SHAKE_MESSAGE = "The room begins to shake violently!"

        private val SEAL_HP: Map<Int, Int> by lazy {
            ToaHetSealRow.all().associate { it.teamSize to it.hp }
        }
    }
}

private val MIRROR_ID: Int by lazy { HetObjs.MIRROR.asRSCM(RSCMType.OBJ) }

internal fun Player.removeMirrors() {
    for (slot in inv.indices) {
        if (inv[slot]?.id == MIRROR_ID) inv[slot] = null
    }
}
