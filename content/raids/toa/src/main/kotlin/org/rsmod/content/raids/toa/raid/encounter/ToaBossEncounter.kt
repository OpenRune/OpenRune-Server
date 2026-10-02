package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region

open class ToaBossEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    protected open val osmumtenDelay: Int = 0

    override fun onComplete() {
        val path = room.path ?: return
        if (path !in raid.pathsCompleted) {
            raid.pathsCompleted += path
        }
        if (osmumtenDelay > 0) schedule(osmumtenDelay) { spawnOsmumten() } else spawnOsmumten()
    }

    // TODO: play jingle 296 when Osmumten spawns.
    private fun spawnOsmumten() {
        val tile = room.osmumtenTile ?: return
        val challengeSpawn = room.challengeSpawn ?: return
        val npc = Npc(OSMUMTEN, coords(tile))
        npc.respawnDir = if (challengeSpawn.x > tile.x) Direction.East else Direction.West
        deps.npcRepo.add(npc, Int.MAX_VALUE)
        npc.noneMode()
        npc.anim(OSMUMTEN_SPAWN_ANIM)
    }

    companion object {
        const val OSMUMTEN = "npc.toa_osmumten_vis"

        private const val OSMUMTEN_SPAWN_ANIM = "seq.ghost_summon2_priority"
    }
}
