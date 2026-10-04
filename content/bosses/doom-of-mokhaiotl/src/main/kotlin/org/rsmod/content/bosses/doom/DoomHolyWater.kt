package org.rsmod.content.bosses.doom

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid

private var Player.specialEnergy by intVarp("varp.sa_energy")

@Singleton
internal class DoomHolyWater
@Inject
constructor(
    private val deps: BossDeps,
    private val loot: DoomLoot,
    private val acid: DoomAcid,
    private val delves: DoomDelves,
) {
    fun onDeath(access: StandardNpcAccess) {
        val npc = access.npc
        if (npc.vars[DoomVarns.CHARGE] !in KILL_CHARGES) return
        val player = loot.killer(access) ?: return
        if (delves.currentLevel(player) >= DoomDelve.DEEP_LEVEL) return
        val centre = npc.coords.translate(BOSS_HALF, BOSS_HALF)
        val spawn = npc.spawnCoords
        val tiles = mutableSetOf<CoordGrid>()
        while (tiles.size < DROPLETS) {
            val tile = centre.translate(deps.random.of(SPREAD * 2 + 1) - SPREAD, deps.random.of(SPREAD * 2 + 1) - SPREAD)
            if (DoomArena.onFloor(spawn, tile)) tiles += tile
        }
        val restored = mutableSetOf<Player>()
        tiles.forEachIndexed { index, tile -> throwDroplet(player, restored, centre, tile, (index + 1) * CYCLES_PER_TICK) }
    }

    private fun throwDroplet(player: Player, restored: MutableSet<Player>, from: CoordGrid, tile: CoordGrid, endTime: Int) {
        deps.worldRepo.projAnim(
            ProjAnim(
                spotanim = TRAVEL.asRSCM(RSCMType.SPOTANIM),
                startHeight = START_HEIGHT,
                endHeight = 0,
                startTime = 0,
                endTime = endTime,
                angle = ANGLE,
                progress = 0,
                sourceIndex = 0,
                targetIndex = 0,
                startCoord = from,
                endCoord = tile,
            ),
        )
        mapAnim(SHADOW, tile, (endTime - SHADOW_LEAD).coerceAtLeast(0))
        deps.worldQueues.add(endTime / CYCLES_PER_TICK) { land(player, restored, tile) }
    }

    private fun land(player: Player, restored: MutableSet<Player>, tile: CoordGrid) {
        mapAnim(SPLASH, tile, 0)
        for (dx in -1..1) for (dz in -1..1) acid.clear(player, tile.translate(dx, dz))
        if (!player.isSlotAssigned || !inSplash(player.coords, tile) || !restored.add(player)) return
        player.statHeal("stat.hitpoints", HEAL, 0)
        player.statHeal("stat.prayer", PRAYER, 0)
        player.specialEnergy = minOf(MAX_SPEC, player.specialEnergy + SPEC_RESTORE)
    }

    private fun inSplash(player: CoordGrid, tile: CoordGrid): Boolean =
        player.level == tile.level && abs(player.x - tile.x) <= 1 && abs(player.z - tile.z) <= 1

    private fun mapAnim(name: String, tile: CoordGrid, delay: Int) {
        deps.worldRepo.spotanimMap(SpotanimType(name.asRSCM(RSCMType.SPOTANIM)), tile, delay = delay)
    }

    private companion object {
        private val KILL_CHARGES = setOf(DoomVarns.CHARGE_MELEE, DoomVarns.CHARGE_PUNISHED)
        private const val BOSS_HALF = 2
        private const val DROPLETS = 7
        private const val SPREAD = 4
        private const val CYCLES_PER_TICK = 30
        private const val START_HEIGHT = 600
        private const val ANGLE = 10
        private const val SHADOW_LEAD = 60
        private const val HEAL = 28
        private const val PRAYER = 14
        private const val SPEC_RESTORE = 250
        private const val MAX_SPEC = 1000
        private const val TRAVEL = "spotanim.holy_water_travel"
        private const val SHADOW = "spotanim.gargboss_debris_shadow_90"
        private const val SPLASH = "spotanim.watersplash"
    }
}
