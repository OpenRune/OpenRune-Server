package org.rsmod.content.raids.toa.raid.encounter.wardens

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.abs
import kotlin.math.min
import org.rsmod.api.config.Constants
import org.rsmod.api.player.output.mes
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.hitTypeless
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.map.util.Translation
import org.rsmod.game.headbar.Headbar
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

internal class WardensCharging(private val room: WardensEncounter) {
    private val orbCharge: Int
        get() = if (room.raid.isActive(ToaInvocationKey.AncientHaste)) ORB_CHARGE * HASTE_FACTOR else ORB_CHARGE

    private val deps = room.raid.deps
    private val orbs = ArrayList<Orb>()
    private val pyramids = WardenGod.entries.associateWith { ArrayList<LocInfo>() }
    private val platforms = HashMap<WardenGod, Int>()
    private val charge = HashMap<WardenGod, Int>()
    private val specials = HashMap<WardenGod, Int>()
    private var spawned = 0
    private var running = false

    private class Orb(val npc: Npc, val god: WardenGod, val path: List<CoordGrid>) {
        var step = 0
        var dying = false
        var ending = false
    }

    fun lifetimeCharge(god: WardenGod): Int =
        (specials[god] ?: 0) * MAX_CHARGE + (charge[god] ?: 0)

    fun start() {
        clear()
        running = true
        for (god in WardenGod.entries) {
            charge[god] = 0
            specials[god] = 0
            showPlatform(god)
            showCharge(god)
        }
    }

    fun stop() {
        running = false
        clear()
        val bar = ServerCacheManager.getHealthBar(CHARGE_BAR.asRSCM(RSCMType.HEADBAR)) ?: return
        for (god in WardenGod.entries) room.warden(god)?.removeHeadbar(bar.id)
    }

    fun clear() {
        for (orb in orbs) room.despawn(orb.npc)
        orbs.clear()
        for (god in WardenGod.entries) {
            clearPyramids(god)
            removePlatform(god)
        }
        platforms.clear()
        spawned = 0
    }

    fun tick(ticks: Int) {
        if (!running) return
        stepOrbs()
        if (ticks >= ORB_FIRST_TICK && ticks % ORB_INTERVAL == 0) spawnPair()
        for (god in WardenGod.entries) {
            addCharge(god, if (ticks % 2 == 0) PASSIVE_EVEN else PASSIVE_ODD)
            if ((charge[god] ?: 0) >= MAX_CHARGE) discharge(god)
            showPlatform(god)
            showCharge(god)
        }
    }

    fun fire(god: WardenGod) {
        if ((specials[god] ?: 0) % 2 == 0) ufo(god) else chargedShot(god)
        specials[god] = (specials[god] ?: 0) + 1
    }

    private fun discharge(god: WardenGod) {
        charge[god] = 0
        fire(god)
    }

    private fun addCharge(god: WardenGod, amount: Int) {
        charge[god] = min(MAX_CHARGE, (charge[god] ?: 0) + amount)
    }

    private fun spawnPair() {
        val index = spawned++
        room.obelisk?.anim("seq.fx_obelisk_wardens01_cast")
        deps.worldRepo.soundArea(
            room.coords(WardenGod.OBELISK_CENTRE),
            "synth.toa_wardens_orb_spawn",
            radius = ORB_SOUND_RADIUS,
        )
        for (god in WardenGod.entries.reversed()) {
            val spawn =WardenGod.OBELISK_CENTRE.translate(god.orbSide * WardenGod.ORB_SPAWN_OFFSET, 0)
            val npc = room.spawn("npc.toa_wardens_energy", room.coords(spawn), god.orbFacing)
            npc.pendingTelejump = false
            npc.anim("seq.zuk_proj")
            npc.infoProtocol.setFaceAngle(
                if (god.orbSide < 0) FACE_WEST else FACE_EAST,
                instant = false,
            )
            npc.pendingExactMove =
                EntityExactMove(
                    deltaX1 = -god.orbSide,
                    deltaZ1 = 0,
                    deltaX2 = 0,
                    deltaZ2 = 0,
                    clientDelay1 = 0,
                    clientDelay2 = ORB_STEP_CYCLES,
                    direction = if (god.orbSide < 0) Constants.em_face_west else Constants.em_face_east,
                )
            val orb = Orb(npc, god, god.lane(index))
            val blockTiles = god.orbBlockTiles.map { room.coords(it) }
            val blocker = room.targets().firstOrNull { it.coords in blockTiles }
            if (blocker != null) {
                blocker.hitTypeless(BLOCK_DAMAGE)
                orb.ending = true
            }
            orbs += orb
        }
    }

    private fun stepOrbs() {
        val targets = room.targets()
        val iterator = orbs.iterator()
        while (iterator.hasNext()) {
            val orb = iterator.next()
            if (orb.dying) {
                room.despawn(orb.npc)
                iterator.remove()
                continue
            }
            if (orb.ending) {
                orb.npc.anim("seq.fx_wardens_energy_death01")
                orb.dying = true
                continue
            }
            val tile = room.coords(orb.path[orb.step++])
            val dir = if (orb.god.orbSide < 0) Constants.em_face_west else Constants.em_face_east
            val from = orb.npc.coords
            orb.npc.teleport(deps.collision, tile)
            val delta = Translation.between(from, tile)
            orb.npc.pendingExactMove =
                EntityExactMove(
                    deltaX1 = delta.x,
                    deltaZ1 = delta.z,
                    deltaX2 = 0,
                    deltaZ2 = 0,
                    clientDelay1 = 0,
                    clientDelay2 = ORB_STEP_CYCLES,
                    direction = dir,
                )
            orb.npc.anim("seq.zuk_proj")
            val blocker = targets.firstOrNull { it.coords == tile }
            if (blocker != null) {
                block(orb, blocker)
            } else if (orb.step == orb.path.size) {
                addCharge(orb.god, orbCharge)
                orb.ending = true
            }
        }
    }

    private fun block(orb: Orb, player: Player) {
        player.hitTypeless(BLOCK_DAMAGE)
        orb.ending = true
    }

    private fun ufo(god: WardenGod) {
        val warden = room.warden(god) ?: return
        warden.anim("seq.npc_wardens_impact01")
        warden.spotanim("spotanim.spotanim_wardens01_magic01_activate01")
        val origins = god.pyramids
        room.schedule(UFO_ADD) {
            if (!running) return@schedule
            clearPyramids(god)
            for (origin in origins) pyramids.getValue(god) += place(origin, god.pyramid)
        }
        room.schedule(UFO_LOAD) {
            if (!running) return@schedule
            val list = pyramids.getValue(god)
            val loaded = list.map { place(it.coords, god.pyramidLoad, static = false) }
            list.clear()
            list += loaded
        }
        room.schedule(UFO_ATTACK_ANIM) { animatePyramids("seq.toa_wardens01_pyramid_attack") }
        room.schedule(UFO_LEAVE_ANIM) { animatePyramids("seq.toa_wardens01_pyarmid_leave") }
        for (tick in UFO_DAMAGE_FIRST..UFO_DAMAGE_LAST) {
            room.schedule(tick) { if (running) damagePyramids(origins) }
        }
        room.schedule(UFO_REMOVE) { clearPyramids(god) }
    }

    private fun place(tile: CoordGrid, loc: String, static: Boolean = true): LocInfo {
        val coords = if (static) room.coords(tile) else tile
        return deps.locRepo.add(coords, loc, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
    }

    private fun animatePyramids(seq: String) {
        if (!running) return
        for (list in pyramids.values) for (loc in list) deps.worldRepo.locAnim(loc, seq)
    }

    private fun damagePyramids(origins: List<CoordGrid>) {
        for (player in room.targets()) {
            val tile = room.staticCoords(player.coords)
            val inside =
                origins.any {
                    tile.x in it.x..it.x + PYRAMID_SIZE - 1 && tile.z in it.z..it.z + PYRAMID_SIZE - 1
                }
            if (inside) room.hazardDamage(player, PYRAMID_DAMAGE, PYRAMID_SPREAD)
        }
    }

    private fun clearPyramids(god: WardenGod) {
        val list = pyramids.getValue(god)
        for (loc in list) deps.locRepo.del(loc, Int.MAX_VALUE)
        list.clear()
    }

    private fun chargedShot(god: WardenGod) {
        val warden = room.warden(god) ?: return
        warden.anim("seq.npc_wardens_impact01")
        warden.spotanim("spotanim.spotanim_wardens01_magic02_activate01")
        room.schedule(SHOT_LAUNCH) {
            if (!running) return@schedule
            val ball = spotanim(god.ball).id
            for (player in room.targets()) {
                player.mes("<col=${god.colour}>A large ball of energy is shot your way...</col>")
                deps.worldRepo.projAnim(
                    ProjAnim(
                        spotanim = ball,
                        startHeight = SHOT_START_HEIGHT,
                        endHeight = SHOT_END_HEIGHT,
                        startTime = SHOT_START_TIME,
                        endTime = SHOT_FLIGHT,
                        angle = SHOT_ANGLE,
                        progress = SHOT_PROGRESS,
                        sourceIndex = 0,
                        targetIndex = -(player.slotId + 1),
                        startCoord = room.coords(god.centre),
                        endCoord = player.coords,
                    )
                )
            }
        }
        room.schedule(SHOT_DAMAGE) {
            val targets = room.targets()
            for (player in targets) player.hitTypeless(shotDamage(god, player, targets))
        }
    }

    private fun shotDamage(god: WardenGod, player: Player, targets: List<Player>): Int {
        val others = targets.filter { it !== player }
        return if (god == WardenGod.ELIDINIS) {
            val base = room.hazardRoll(ELIDINIS_SHOT, 0)
            val stacked = others.count { near(it, player) }
            min(ELIDINIS_SHOT_CAP, base * (1 + SHOT_STACK_FACTOR * stacked))
        } else {
            val base = room.hazardRoll(TUMEKEN_SHOT, 0)
            val spread = others.count { it.coords != player.coords }
            min(TUMEKEN_SHOT_CAP, base * (1 + SHOT_STACK_FACTOR * spread))
        }
    }

    private fun near(a: Player, b: Player): Boolean =
        abs(a.coords.x - b.coords.x) <= 1 && abs(a.coords.z - b.coords.z) <= 1

    private fun showPlatform(god: WardenGod) {
        val level = (charge[god] ?: 0) / PLATFORM_STEP
        if (platforms[god] == level) return
        platforms[god] = level
        deps.locRepo.add(
            room.coords(god.tile),
            "loc.toa_wardens_default_charging_platform_$level",
            Int.MAX_VALUE,
            god.platformAngle,
            LocShape.CentrepieceStraight,
        )
    }

    private fun removePlatform(god: WardenGod) {
        val loc =
            deps.locRepo.findExact(room.coords(god.tile), LocShape.CentrepieceStraight) ?: return
        deps.locRepo.del(loc, Int.MAX_VALUE)
    }

    private fun showCharge(god: WardenGod) {
        val warden = room.warden(god) ?: return
        val bar = ServerCacheManager.getHealthBar(CHARGE_BAR.asRSCM(RSCMType.HEADBAR)) ?: return
        val fill = (charge[god] ?: 0) * bar.segments / MAX_CHARGE
        warden.showHeadbar(
            Headbar.fromNoSource(
                self = bar.id,
                public = bar.id,
                startFill = fill,
                endFill = fill,
                startTime = 0,
                endTime = 0,
            )
        )
    }

    private companion object {
        const val MAX_CHARGE = 100
        const val PASSIVE_EVEN = 2
        const val PASSIVE_ODD = 1
        const val ORB_CHARGE = 2
        const val HASTE_FACTOR = 2
        const val BLOCK_DAMAGE = 3
        const val ORB_STEP_CYCLES = 30
        const val ORB_SOUND_RADIUS = 9
        const val FACE_EAST = 1536
        const val FACE_WEST = 512
        const val ORB_FIRST_TICK = 2
        const val ORB_INTERVAL = 2
        const val PLATFORM_STEP = 5
        const val CHARGE_BAR = "headbar.charge_100"

        const val UFO_ADD = 3
        const val UFO_LOAD = 4
        const val UFO_DAMAGE_FIRST = 6
        const val UFO_DAMAGE_LAST = 10
        const val UFO_ATTACK_ANIM = 7
        const val UFO_LEAVE_ANIM = 11
        const val UFO_REMOVE = 14
        const val PYRAMID_SIZE = 3
        const val PYRAMID_DAMAGE = 11
        const val PYRAMID_SPREAD = 2

        const val SHOT_LAUNCH = 3
        const val SHOT_DAMAGE = 16
        const val SHOT_START_HEIGHT = 348
        const val SHOT_END_HEIGHT = 80
        const val SHOT_START_TIME = 30
        const val SHOT_FLIGHT = 420
        const val SHOT_ANGLE = 40
        const val SHOT_PROGRESS = 64
        const val ELIDINIS_SHOT = 12
        const val ELIDINIS_SHOT_CAP = 45
        const val TUMEKEN_SHOT = 13
        const val TUMEKEN_SHOT_CAP = 30
        const val SHOT_STACK_FACTOR = 3
    }
}
