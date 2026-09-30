package org.rsmod.api.bosses.runtime

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.add
import org.rsmod.game.map.collision.remove
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

fun BossDeps.bossProjectile(
    spotanim: Int,
    src: CoordGrid,
    target: CoordGrid,
    startHeight: Int,
    endHeight: Int,
    delay: Int,
    travel: Int,
    curve: Int,
    progress: Int = 0,
    homing: Player? = null,
): ProjAnim {
    val proj =
        ProjAnim(
            spotanim = spotanim,
            startHeight = startHeight,
            endHeight = endHeight,
            startTime = delay,
            endTime = delay + travel,
            angle = curve,
            progress = progress,
            sourceIndex = 0,
            targetIndex = homing?.let { -(it.slotId + 1) } ?: 0,
            startCoord = src,
            endCoord = homing?.coords ?: target,
        )
    worldRepo.projAnim(proj)
    return proj
}

fun BossDeps.suppressAttacks(npc: Npc, ticks: Int) {
    val encounter = encounterRegistry.of(npc)
    encounter.busyUntil = maxOf(encounter.busyUntil, mapClock.cycle + ticks)
}

fun BossDeps.forceNext(npc: Npc, ability: String) {
    encounterRegistry.of(npc).forceNext(ability)
}

fun BossDeps.encounter(npc: Npc): BossEncounter = encounterRegistry.of(npc)

/**
 * Starts [npc]'s encounter on [spec], replacing any previous one (its owned locs and npcs are
 * removed, as on respawn). [spec] must be one of the specs registered for the npc's type.
 */
fun BossDeps.startEncounter(npc: Npc, spec: BossSpec): BossEncounter {
    encounterRegistry.remove(npc)?.let(::disposeOwned)
    return encounterRegistry.start(npc, spec)
}

fun BossDeps.runAbility(npc: Npc, target: Player, ability: String) {
    val encounter = encounterRegistry.of(npc)
    val effect =
        requireNotNull(encounter.spec.abilities[ability]) {
            "Ability '$ability' does not exist in boss spec."
        }
    EffectInterpreter(npc, target, encounter.spec, encounter, this).run(null, effect)
}

fun BossDeps.interrupt(npc: Npc) {
    encounterRegistry.of(npc).interrupt(mapClock.cycle)
}

fun BossDeps.repeatTick(
    ticks: Int,
    onTick: (remaining: Int) -> Boolean,
    onStop: () -> Unit = {},
) {
    fun step(remaining: Int) {
        worldQueues.add(1) {
            if (remaining <= 0 || !onTick(remaining)) {
                onStop()
            } else {
                step(remaining - 1)
            }
        }
    }
    step(ticks)
}

fun BossDeps.lob(
    npc: Npc,
    targetTile: CoordGrid,
    targetUid: PlayerUid,
    spotanim: Int,
    startHeight: Int,
    endHeight: Int,
    delay: Int,
    travel: Int,
    curve: Int,
    landTicks: Int,
    landGfx: Int,
    landGfxHeight: Int = 0,
    progress: Int = 0,
    onLand: (Player) -> Unit,
) {
    bossProjectile(
        spotanim,
        npc.coords.translate(2, 2),
        targetTile,
        startHeight,
        endHeight,
        delay,
        travel,
        curve,
        progress,
    )
    worldQueues.add(landTicks) {
        worldRepo.spotanimMap(SpotanimType(landGfx), targetTile, landGfxHeight)
        val player = targetUid.resolve(playerList) ?: return@add
        if (player.hitpoints > 0) {
            onLand(player)
        }
    }
}

class OwnedLoc internal constructor(val info: LocInfo, val blockPlayersOnly: Boolean)

fun BossDeps.spawnOwnedLoc(npc: Npc, tile: CoordGrid, loc: String, angle: Int, blockPlayersOnly: Boolean) {
    val encounter = encounterRegistry.of(npc)
    if (encounter.ownsLocAt(tile)) return
    val info = locRepo.add(tile, loc, Int.MAX_VALUE, LocAngle[angle], LocShape.CentrepieceStraight)
    if (blockPlayersOnly) {
        collision.remove(tile, CollisionFlag.LOC or CollisionFlag.LOC_ROUTE_BLOCKER)
        collision.add(tile, CollisionFlag.BLOCK_PLAYERS)
    }
    encounter.addOwnedLoc(tile, OwnedLoc(info, blockPlayersOnly))
}

fun BossDeps.removeLocs(locs: Map<CoordGrid, OwnedLoc>, breakSpotanim: String? = null) {
    val spot = breakSpotanim?.let { SpotanimType(it.asRSCM(RSCMType.SPOTANIM)) }
    for ((tile, loc) in locs) {
        locRepo.del(loc.info, Int.MAX_VALUE)
        if (loc.blockPlayersOnly) collision.remove(tile, CollisionFlag.BLOCK_PLAYERS)
        spot?.let { worldRepo.spotanimMap(it, tile) }
    }
}

fun BossDeps.clearOwnedLocs(npc: Npc, breakSpotanim: String? = null) {
    removeLocs(encounterRegistry.of(npc).releaseOwnedLocs(), breakSpotanim)
}

fun BossDeps.spawnOwnedNpc(
    owner: Npc,
    type: String,
    tile: CoordGrid,
    duration: Int = Int.MAX_VALUE,
): Npc? {
    val npcType = ServerCacheManager.getNpc(type.asRSCM(RSCMType.NPC)) ?: return null
    val spawned = Npc(npcType, tile)
    npcRepo.add(spawned, duration)
    encounterRegistry.of(owner).addOwnedNpc(spawned)
    return spawned
}

fun BossDeps.clearOwnedNpcs(owner: Npc) {
    removeNpcs(encounterRegistry.of(owner).releaseOwnedNpcs())
}

internal fun BossDeps.disposeOwned(encounter: BossEncounter) {
    removeLocs(encounter.releaseOwnedLocs())
    removeNpcs(encounter.releaseOwnedNpcs())
}

private fun BossDeps.removeNpcs(npcs: List<Npc>) {
    for (npc in npcs) {
        if (npc.isSlotAssigned) npcRepo.del(npc, Int.MAX_VALUE)
    }
}
