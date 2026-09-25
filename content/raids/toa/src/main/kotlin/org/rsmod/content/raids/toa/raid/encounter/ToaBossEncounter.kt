package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.content.raids.toa.raid.ToaRaid
import org.rsmod.content.raids.toa.raid.ToaRoom
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.Direction
import org.rsmod.game.region.Region

/**
 * What every boss room does when its boss is beaten. Offline_Scape repeated this in each boss's
 * `onRoomEnd` (Kephri, Akkha, Ba-Ba, Zebak): mark the path completed and summon Osmumten, whose
 * "Proceed" takes you back to the nexus.
 *
 * `open` so Phase B's boss rooms extend it; they call `super.onComplete()`.
 */
open class ToaBossEncounter(raid: ToaRaid, room: ToaRoom, region: Region, controllerId: Int) :
    ToaEncounter(raid, room, region, controllerId) {

    override fun onComplete() {
        val path = room.path ?: return
        if (path !in raid.pathsCompleted) {
            raid.pathsCompleted += path
        }
        spawnOsmumten()
    }

    /** Offline_Scape `spawnTeleportNPC`. TODO: jingle 296. */
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
        /** op1 Talk-to, op3 Proceed; handled in ToaRaidScript. */
        const val OSMUMTEN = "npc.toa_osmumten_vis"

        /** Offline_Scape Osmumten SPAWN_ANIM 9795. */
        private const val OSMUMTEN_SPAWN_ANIM = "seq.ghost_summon2_priority"
    }
}
