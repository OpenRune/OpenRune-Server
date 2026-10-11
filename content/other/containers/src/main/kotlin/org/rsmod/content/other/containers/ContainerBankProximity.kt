package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import jakarta.inject.Inject
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.game.entity.Player
import org.rsmod.map.zone.ZoneKey

/** Uses actual nearby actors/objects; it does not designate arbitrary bank rectangles. */
internal class ContainerBankProximity @Inject constructor(private val locs: LocRepository, private val npcs: NpcRepository) {
    fun available(player: Player): Boolean {
        if (player.ui.containsModal("interface.bankmain")) return true
        val zone = ZoneKey.from(player.coords)
        if (npcs.findAll(zone, 1).any { npc -> npc.coords.level == player.coords.level && npc.coords.chebyshevDistance(player.coords) <= 3 && (1..5).any { npc.visType.actions.getOpOrNull(it)?.equals("Bank", true) == true } }) return true
        return (-1..1).any { dx -> (-1..1).any { dz ->
            val x = zone.x + dx; val z = zone.z + dz
            if (x !in 0..ZoneKey.X_BIT_MASK || z !in 0..ZoneKey.Z_BIT_MASK) false else locs.findAll(ZoneKey(x = x, z = z, level = zone.level)).any { loc ->
                val type = ServerCacheManager.getObject(loc.id)
                loc.coords.chebyshevDistance(player.coords) <= 2 && type != null && (type.name == "Bank chest" || type.name == "Bank booth")
            }
        } }
    }
}
