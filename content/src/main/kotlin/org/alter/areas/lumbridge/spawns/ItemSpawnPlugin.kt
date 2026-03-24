package org.alter.areas.lumbridge.spawns

import org.alter.game.pluginnew.PluginEvent

class ItemSpawnPlugin : PluginEvent() {

    override fun init() {
        // Eggs at chicken coop near Fred's farm
        spawnItem(item = "items.egg", amount = 1, x = 3185, z = 3275)

        // Eggs at second coop east of river
        spawnItem(item = "items.egg", amount = 1, x = 3236, z = 3298)
    }
}
