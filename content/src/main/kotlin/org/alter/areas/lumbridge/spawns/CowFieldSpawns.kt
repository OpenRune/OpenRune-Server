package org.alter.areas.lumbridge.spawns

import org.alter.game.model.Direction
import org.alter.game.pluginnew.PluginEvent

class CowFieldSpawns : PluginEvent() {

    override fun init() {
        // Cows in the cow field east of the river
        spawnNpc(npc = "npcs.cow", x = 3253, z = 3270, walkRadius = 3, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.cow", x = 3255, z = 3266, walkRadius = 3, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.cow", x = 3258, z = 3272, walkRadius = 3, direction = Direction.EAST)
        spawnNpc(npc = "npcs.cow", x = 3260, z = 3268, walkRadius = 3, direction = Direction.WEST)
        spawnNpc(npc = "npcs.cow", x = 3256, z = 3275, walkRadius = 3, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.cow", x = 3262, z = 3263, walkRadius = 3, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.cow", x = 3265, z = 3260, walkRadius = 3, direction = Direction.EAST)
        spawnNpc(npc = "npcs.cow", x = 3257, z = 3258, walkRadius = 3, direction = Direction.WEST)

        // Calves in the cow field
        spawnNpc(npc = "npcs.calf", x = 3256, z = 3269, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.calf", x = 3261, z = 3265, walkRadius = 2, direction = Direction.NORTH)

        // Chickens at Fred's farm area
        spawnNpc(npc = "npcs.chicken", x = 3185, z = 3275, walkRadius = 2, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.chicken", x = 3187, z = 3276, walkRadius = 2, direction = Direction.EAST)
        spawnNpc(npc = "npcs.chicken", x = 3189, z = 3278, walkRadius = 2, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.chicken", x = 3191, z = 3275, walkRadius = 2, direction = Direction.WEST)
        spawnNpc(npc = "npcs.chicken", x = 3193, z = 3280, walkRadius = 2, direction = Direction.SOUTH)
    }
}
