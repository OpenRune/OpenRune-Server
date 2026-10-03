package org.rsmod.content.raids.toa.raid.encounter

import org.rsmod.api.instances.InstanceId
import org.rsmod.api.instances.InstanceManager
import org.rsmod.content.raids.toa.raid.ToaRaidManager.currentRaid
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

internal object ToaRooms {
    private lateinit var instances: InstanceManager

    private val rooms = HashMap<InstanceId, ToaEncounter>()

    fun bind(instances: InstanceManager) {
        this.instances = instances
    }

    fun register(room: ToaEncounter) {
        rooms[room.session.id] = room
    }

    fun unregister(room: ToaEncounter) {
        rooms.remove(room.session.id)
    }

    fun roomOf(npc: Npc): ToaEncounter? = instances.instanceForNpc(npc)?.let(rooms::get)

    fun roomOf(player: Player): ToaEncounter? = player.currentRaid?.encounterOf(player)

    inline fun <reified R : ToaEncounter> of(npc: Npc): R? = roomOf(npc) as? R

    inline fun <reified R : ToaEncounter> of(player: Player): R? = roomOf(player) as? R
}
