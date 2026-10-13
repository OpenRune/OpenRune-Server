package org.rsmod.content.raids.toa.raid.encounter.apmeken.baba

import org.rsmod.annotations.InternalApi
import org.rsmod.api.npc.hit.queueHit as queueNpcHit
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.NOOP_NPC_MODIFIER
import org.rsmod.content.raids.toa.raid.encounter.crondis.zebak.faceAngle
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

internal class BabaRockfall(private val room: BabaEncounter) {
    private val deps = room.raid.deps
    private val rubble = HashMap<Npc, LocInfo>()

    fun clear() {
        for (npc in rubble.keys.toList()) remove(npc)
    }

    fun resolveThrow(player: Player) {
        if (player !in room.targets()) return
        val tile = player.coords
        val cover = rubble.keys.firstOrNull { covers(it, tile) } ?: room.baboons.coverNear(tile)
        if (cover == null) {
            val sharing = room.targets().count { it.coords.chebyshevDistance(tile) <= 1 }
            room.hazardDamage(player, BabaRocks.THROW_DAMAGE / sharing.coerceAtLeast(1), 0)
            return
        }
        val damage =
            when {
                cover in rubble -> BabaRocks.COVER_DAMAGE_RUBBLE
                cover.isVisType(BabaNpcs.SARCOPHAGUS) -> BabaRocks.COVER_DAMAGE_SARCOPHAGUS
                else -> BabaRocks.COVER_DAMAGE_BABOON
            }
        cover.queueNpcHit(1, HitType.Typeless, damage, NOOP_NPC_MODIFIER)
    }

    fun died(npc: Npc) {
        if (npc !in rubble) return
        remove(npc)
        room.repeatRockfall()
    }

    fun crush(boulder: Npc) {
        for (npc in rubble.keys.toList()) {
            val dx = npc.coords.x - boulder.coords.x
            val dz = npc.coords.z - boulder.coords.z
            val reach = -RUBBLE_SIZE + 1 until RUBBLE_SIZE
            if (dx in reach && dz in reach) remove(npc)
        }
    }

    fun spawnRubble(marker: CoordGrid) {
        val origin = marker.translate(-1, -1)
        if (rubble.keys.any { it.coords.z == origin.z }) return
        for (player in room.targets()) if (inFootprint(origin, player.coords)) pushOut(player, origin)
        val npc = room.spawn(BabaNpcs.RUBBLE, origin)
        npc.ignoreCombatInteractions = true
        val hp = BabaRocks.RUBBLE_HP_PER_PLAYER * room.teamSize
        npc.baseHitpointsLvl = hp
        npc.hitpoints = hp
        val shape = LocShape.CentrepieceStraight
        rubble[npc] = deps.locRepo.add(origin, BabaRocks.BLOCKER, Int.MAX_VALUE, LocAngle.West, shape)
    }

    @OptIn(InternalApi::class)
    private fun pushOut(player: Player, origin: CoordGrid) {
        val from = player.coords
        val dest =
            outsideTiles(origin)
                .filter { room.isOpenFloor(it) && !isRubbleTile(it) }
                .minByOrNull { it.chebyshevDistance(from) } ?: return
        deps.launcher.launchLenient(player) {
            anim(BabaSeqs.KNOCKDOWN)
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

    private fun outsideTiles(origin: CoordGrid): List<CoordGrid> {
        val tiles = ArrayList<CoordGrid>()
        for (dx in -1..RUBBLE_SIZE) {
            for (dz in -1..RUBBLE_SIZE) {
                if (dx in 0 until RUBBLE_SIZE && dz in 0 until RUBBLE_SIZE) continue
                tiles += origin.translate(dx, dz)
            }
        }
        return tiles
    }

    private fun inFootprint(origin: CoordGrid, tile: CoordGrid): Boolean =
        tile.x - origin.x in 0 until RUBBLE_SIZE && tile.z - origin.z in 0 until RUBBLE_SIZE

    private fun isRubbleTile(tile: CoordGrid): Boolean =
        rubble.keys.any { inFootprint(it.coords, tile) }

    private fun remove(npc: Npc) {
        rubble.remove(npc)?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
        if (npc.isSlotAssigned) room.despawn(npc)
    }

    private fun covers(npc: Npc, tile: CoordGrid): Boolean {
        val dx = tile.x - npc.coords.x
        val dz = tile.z - npc.coords.z
        return dx in -1..RUBBLE_SIZE && dz in -1..RUBBLE_SIZE
    }

    private companion object {
        const val RUBBLE_SIZE = 3
        const val PUSH_CYCLES = 30
    }
}
