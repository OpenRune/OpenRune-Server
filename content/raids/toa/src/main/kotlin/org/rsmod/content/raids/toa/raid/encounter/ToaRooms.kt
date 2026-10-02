package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal object ToaRooms {
    private val owners = HashMap<Npc, ToaEncounter>()

    fun adopt(npc: Npc, room: ToaEncounter) {
        owners.entries.removeIf { it.value.destroyed || !it.key.isSlotAssigned }
        owners[npc] = room
    }

    fun release(npc: Npc) {
        owners.remove(npc)
    }

    fun roomOf(npc: Npc): ToaEncounter? {
        val room = owners[npc] ?: return null
        if (room.destroyed) {
            owners.remove(npc)
            return null
        }
        return room
    }

    fun roomOf(player: Player): ToaEncounter? = player.currentRaid?.encounterOf(player)

    inline fun <reified R : ToaEncounter> of(npc: Npc): R? = roomOf(npc) as? R

    inline fun <reified R : ToaEncounter> of(player: Player): R? = roomOf(player) as? R
}
