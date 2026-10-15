package org.rsmod.content.raids.toa.raid.encounter.wardens

import dev.openrune.ServerCacheManager
import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.heal
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.CamShakeAxis
import org.rsmod.api.player.output.Camera
import org.rsmod.api.player.output.mes
import org.rsmod.api.table.ToaWardensCollapseRow
import org.rsmod.content.raids.toa.party.ToaInvocationKey
import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.content.raids.toa.raid.encounter.WardensSecondEncounter
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.ZebakSeqs
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.faceAngle
import org.rsmod.content.raids.toa.raid.encounter.npcType
import org.rsmod.content.raids.toa.raid.encounter.spotanim
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.add as addCollisionFlag
import org.rsmod.game.map.collision.remove as removeCollisionFlag
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

internal class WardenEnrage(private val room: WardensSecondEncounter) {
    private val deps = room.raid.deps
    private val occupied = HashSet<CoordGrid>()
    private val floorLocs = ArrayList<LocInfo>()
    private val blockedTiles = ArrayList<CoordGrid>()
    private var token = 0
    private var batches = 0

    var active = false
        private set

    private val insanity: Boolean
        get() = room.raid.isActive(ToaInvocationKey.Insanity)

    fun reset() {
        token++
        active = false
        batches = 0
        occupied.clear()
        for (loc in floorLocs) deps.locRepo.del(loc, Int.MAX_VALUE)
        floorLocs.clear()
        for (tile in blockedTiles) deps.collision.removeCollisionFlag(tile, CollisionFlag.BLOCK_WALK)
        blockedTiles.clear()
    }

    fun start(warden: Npc) {
        if (active) return
        active = true
        val gen = ++token
        warden.heal(warden.baseHitpointsLvl * HEAL_PERCENT / PERCENT, showHitsplat = true)
        room.hpBar?.update()
        warden.anim(ENRAGING_SEQ)
        room.amascut?.let {
            it.anim(AMASCUT_ENRAGE_SEQ)
            it.spotanim(AMASCUT_ENRAGE_SPOT, delay = 1)
        }
        room.phantoms.enrage()
        after(LOOP_START, gen) { loop(gen) }
        after(SHAKE_DELAY, gen) { shake() }
        after(LIGHTNING_START, gen) { lightningCycle(gen) }
        after(COLLAPSE_START, gen) { removeRow(0, gen) }
        after(AMASCUT_TRANSFORM, gen) { amascutEffect(gen, transform = true) }
    }

    private fun loop(gen: Int) {
        room.warden?.anim(ENRAGED_SEQ)
        after(LOOP_STEP, gen) { loop(gen) }
    }

    private fun shake() {
        for (player in room.players) {
            Camera.camShake(player, CamShakeAxis.LEFT_RIGHT, SHAKE_LEFT_RIGHT, 0, 0)
            Camera.camShake(player, CamShakeAxis.UP_DOWN, SHAKE_UP_DOWN, 0, 0)
            Camera.camShake(player, CamShakeAxis.FORWARDS_BACKWARDS, SHAKE_FORWARDS, 0, 0)
        }
    }

    private fun amascutEffect(gen: Int, transform: Boolean) {
        val amascut = room.amascut ?: return
        if (transform) amascut.transmog(npcType(AMASCUT_ENRAGED), Int.MAX_VALUE)
        amascut.spotanim(AMASCUT_LOOP_SPOT)
        after(LOOP_STEP, gen) { amascutEffect(gen, transform = false) }
    }

    private fun lightningCycle(gen: Int) {
        occupied.clear()
        for (step in 0 until LIGHTNING_BATCHES) after(step, gen) { lightning(gen) }
        after(LIGHTNING_CYCLE, gen) { lightningCycle(gen) }
    }

    private fun lightning(gen: Int) {
        val maxZ = FLOOR_MAX_Z - batches / BATCHES_PER_ROW
        val pool = room.freeTiles(FLOOR_MIN, CoordGrid(FLOOR_MAX_X, maxZ, FLOOR_MIN.level), occupied)
        val struck = pool.take(maxOf(MIN_STRIKES, pool.size * STRIKE_PERCENT / PERCENT))
        for (tile in struck) {
            occupied += tile
            deps.worldRepo.spotanimMap(spotanim(SHADOW), tile)
        }
        after(LIGHTNING_DELAY, gen) {
            val hit = struck.toSet()
            for (tile in struck) deps.worldRepo.spotanimMap(spotanim(LIGHTNING), tile)
            for (player in room.targets()) {
                if (player.coords in hit) room.hazardDamage(player, LIGHTNING_DAMAGE, LIGHTNING_SPREAD)
            }
        }
    }

    private fun removeRow(row: Int, gen: Int) {
        if (row >= ROWS) return
        for (step in 0 until BATCHES_PER_ROW) after(step, gen) { applyBatch(row * BATCHES_PER_ROW + step) }
        after(if (insanity) INSANITY_COLLAPSE_RATE else COLLAPSE_RATE, gen) { removeRow(row + 1, gen) }
    }

    private fun applyBatch(index: Int) {
        val data = ToaWardensCollapseRow.getRow("dbrow.toa_wardens_collapse_${index + 1}")
        val currentRow = FLOOR_MAX_Z - index / BATCHES_PER_ROW
        val void = room.coords(VOID)
        val pushed = ArrayList<Player>()
        for ((i, tile) in data.tiles.withIndex()) {
            val coords = room.coords(tile)
            floorLocs +=
                deps.locRepo.add(
                    coords,
                    checkNotNull(ServerCacheManager.getObject(data.pieces[i] / ROTATION_SLOTS)),
                    Int.MAX_VALUE,
                    LocAngle[data.pieces[i] % ROTATION_SLOTS],
                    LocShape.GroundDecor,
                )
            deps.collision.addCollisionFlag(coords, CollisionFlag.BLOCK_WALK)
            blockedTiles += coords
            if (tile.z == currentRow) projectile(coords, void)
            pushed += room.targets().filter { it.coords == coords }
        }
        batches = index + 1
        val pushRow = room.coords(CoordGrid(FLOOR_MIN.x, currentRow - 1, FLOOR_MIN.level)).z
        for (player in pushed) push(player, pushRow)
    }

    private fun projectile(from: CoordGrid, to: CoordGrid) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = spotanim(TILE_SPOT).id,
                startHeight = 0,
                endHeight = TILE_END_HEIGHT,
                startTime = 0,
                endTime = TILE_FLIGHT,
                angle = TILE_ANGLE,
                progress = 0,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = to,
            )
        )
    }

    @OptIn(InternalApi::class)
    private fun push(player: Player, z: Int) {
        val from = player.coords
        val dest =
            (1..PUSH_RADIUS)
                .flatMap { radius -> (-radius..radius).map { CoordGrid(from.x + it, z, from.level) } }
                .firstOrNull(room::isOpenFloor) ?: return
        player.abortRoute()
        player.mes(PUSHED)
        deps.launcher.launchLenient(player) {
            anim(ZebakSeqs.PLAYER_KNOCKED)
            exactMove(
                from,
                dest,
                0,
                PUSH_CYCLES,
                faceAngle(dest.x - from.x, dest.z - from.z),
                TeleportType.Exempt,
            )
        }
    }

    private fun after(ticks: Int, gen: Int, action: () -> Unit) {
        room.schedule(ticks) {
            if (gen == token && room.stage == ToaStage.STARTED && (room.warden?.hitpoints ?: 0) > 0) {
                action()
            }
        }
    }

    private companion object {
        const val ENRAGING_SEQ = "seq.npc_wardens_enraging01"
        const val ENRAGED_SEQ = "seq.npc_wardens_enraged01"
        const val AMASCUT_ENRAGE_SEQ = "seq.npc_amascut_giant01_enrage01"
        const val AMASCUT_ENRAGE_SPOT = "spotanim.spotanim_amascut01_sucking01_enraged01"
        const val AMASCUT_LOOP_SPOT = "spotanim.spotanim_amascut01_sucking01"
        const val AMASCUT_ENRAGED = "npc.toa_amascut_p3_enrage"
        const val SHADOW = "spotanim.gargboss_debris_shadow_90"
        const val LIGHTNING = "spotanim.wardens_lightning"
        const val TILE_SPOT = "spotanim.spotanim_wardens_tiles01"
        const val PUSHED = "You are pushed off."

        val FLOOR_MIN = CoordGrid(3926, 5157, 1)
        val VOID = CoordGrid(3936, 5130, 1)
        const val FLOOR_MAX_X = 3946
        const val FLOOR_MAX_Z = 5165

        const val HEAL_PERCENT = 20
        const val PERCENT = 100
        const val LOOP_START = 2
        const val LOOP_STEP = 2
        const val SHAKE_DELAY = 4
        const val SHAKE_LEFT_RIGHT = 4
        const val SHAKE_UP_DOWN = 4
        const val SHAKE_FORWARDS = 3
        const val AMASCUT_TRANSFORM = 9

        const val LIGHTNING_START = 5
        const val LIGHTNING_CYCLE = 6
        const val LIGHTNING_BATCHES = 3
        const val LIGHTNING_DELAY = 3
        const val LIGHTNING_DAMAGE = 7
        const val LIGHTNING_SPREAD = 2
        const val MIN_STRIKES = 2
        const val STRIKE_PERCENT = 15

        const val COLLAPSE_START = 6
        const val COLLAPSE_RATE = 18
        const val INSANITY_COLLAPSE_RATE = 12
        const val ROWS = 8
        const val BATCHES_PER_ROW = 4
        const val TILE_END_HEIGHT = 800
        const val TILE_FLIGHT = 90
        const val TILE_ANGLE = 20
        const val PUSH_RADIUS = 2
        const val PUSH_CYCLES = 30
        const val ROTATION_SLOTS = 4
    }
}
