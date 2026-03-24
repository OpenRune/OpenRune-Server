package org.alter.areas.lumbridge.spawns

import org.alter.game.pluginnew.PluginEvent

class FishingSpotSpawns : PluginEvent() {

    override fun init() {
        // Net/bait fishing spot at Lumbridge swamp
        spawnNpc(npc = "npcs.0_48_50_saltfish", x = 3246, z = 3152, walkRadius = 0)

        // Lure/bait fishing spot at River Lum
        spawnNpc(npc = "npcs.0_48_53_freshfish", x = 3239, z = 3244, walkRadius = 0)
    }
}
