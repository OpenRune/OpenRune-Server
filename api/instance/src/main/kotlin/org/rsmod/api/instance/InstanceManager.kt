package org.rsmod.api.instance

import org.rsmod.game.entity.Player

public class InstanceManager {
    private val map: HashMap<Player, Instance> = HashMap()

    public fun instanceOf(player: Player): Instance? = map[player]

    internal fun register(player: Player, instance: Instance) {
        map[player] = instance
    }

    internal fun unregister(player: Player) {
        map.remove(player)
    }
}
