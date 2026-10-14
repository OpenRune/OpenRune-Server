package org.rsmod.content.raids.toa.raid.encounter.scabaras.kephri

import org.rsmod.content.raids.toa.raid.encounter.ToaStage
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

internal class KephriAgile(private val room: KephriEncounter) {
    private val scarabs = ArrayList<Npc>()

    val count: Int
        get() = scarabs.size

    fun hatch(tile: CoordGrid) {
        val npc = room.spawn(KephriNpcs.AGILE_SCARAB, tile)
        npc.anim(KephriSeqs.SCARAB_SPAWN)
        scarabs += npc
    }

    fun remove(npc: Npc) {
        scarabs.remove(npc)
        room.despawn(npc)
    }

    fun clear() {
        for (npc in scarabs) room.despawn(npc)
        scarabs.clear()
    }

    fun tick() {
        if (room.stage != ToaStage.STARTED) return
        for (npc in scarabs.toList()) {
            if (!npc.isSlotAssigned || npc.hitpoints <= 0) {
                scarabs.remove(npc)
            } else if (room.dung.hasPile(npc.coords)) {
                remove(npc)
            }
        }
    }
}
