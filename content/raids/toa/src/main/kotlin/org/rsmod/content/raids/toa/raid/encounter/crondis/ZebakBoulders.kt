package org.rsmod.content.raids.toa.raid.encounter.crondis.zebak

import org.rsmod.game.entity.Npc
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

/**
 * The Great Roar's boulders: an npc plus a loc that blocks its tile. 150 hitpoints, so the third
 * roar wave (50 each) destroys them through their death queue (ZebakScript).
 */
internal class ZebakBoulders(private val room: ZebakEncounter) {
    private val deps = room.raid.deps
    private val boulders = HashMap<Npc, LocInfo>()

    val npcs: Collection<Npc>
        get() = boulders.keys

    fun isBoulder(tile: CoordGrid): Boolean = boulders.values.any { it.coords == tile }

    fun spawn(tile: CoordGrid) {
        val boulder = room.spawn(ZebakNpcs.BOULDER, tile)
        val blocker = ZebakLocs.BOULDER_BLOCKER
        val shape = LocShape.CentrepieceStraight
        boulders[boulder] = deps.locRepo.add(tile, blocker, Int.MAX_VALUE, LocAngle.West, shape)
        deps.worldRepo.soundArea(tile, ZebakSynths.BOULDER_LAND, radius = LAND_SOUND_RADIUS)
    }

    fun remove(boulder: Npc) {
        val loc = boulders.remove(boulder) ?: return
        deps.locRepo.del(loc, Int.MAX_VALUE)
        room.despawn(boulder)
    }

    fun clear() {
        for (boulder in boulders.keys.toList()) remove(boulder)
    }

    private companion object {
        const val LAND_SOUND_RADIUS = 5
    }
}
