package org.alter.areas.lumbridge.spawns

import org.alter.game.Server
import org.alter.game.model.Direction
import org.alter.game.model.World
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.PluginEvent

/**Example
 *spawnNpc(npc = "npcs.ID", x = xxxx, y = zzzz, height = 0, walk = 0, direction = Direction.NORTH)
 */


class SpawnPlugin : PluginEvent() {

    override fun init() {
        spawnNpc(npc = "npcs.man", x = 3206, z = 3219, walkRadius = 20, height = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.man", x = 3216, z = 3219, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npcs.man", x = 3207, z = 3227, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npcs.man3", x = 3209, z = 3215, walkRadius = 20, height = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.man3", x = 3221, z = 3219, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npcs.woman", x = 3211, z = 3213, walkRadius = 20, height = 1, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.woman", x = 3217, z = 3205, walkRadius = 20, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.rat", x = 3207, z = 3202, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.rat", x = 3205, z = 3204, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.rat", x = 3206, z = 3202, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.rat", x = 3207, z = 3203, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.rat", x = 3205, z = 3209, walkRadius = 10, direction = Direction.EAST)
        spawnNpc(npc = "npcs.rat", x = 3207, z = 3209, walkRadius = 10, direction = Direction.EAST)
        spawnNpc(npc = "npcs.imp", x = 3217, z = 3226, walkRadius = 20, direction = Direction.EAST)
        spawnNpc(npc = "npcs.sheepunsheeredshaggy2", x = 3196, z = 3263, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.sheepunsheeredshaggy2", x = 3199, z = 3261, walkRadius = 10, direction = Direction.EAST)
        spawnNpc(npc = "npcs.sheepunsheeredshaggy2", x = 3201, z = 3272, walkRadius = 10, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.sheepunsheeredshaggy2", x = 3202, z = 3268, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.sheepunsheeredshaggy2", x = 3206, z = 3266, walkRadius = 10, direction = Direction.WEST)
        spawnNpc(npc = "npcs.ramunsheeredshaggy", x = 3201, z = 3263, walkRadius = 10, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.ramunsheeredshaggy", x = 3207, z = 3271, walkRadius = 10, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.ramunsheeredshaggy", x = 3195, z = 3271, walkRadius = 10, direction = Direction.EAST)
        spawnNpc(npc = "npcs.poh_giantspider", x = 3168, z = 3243, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.sos_pest_giantspider1", x = 3165, z = 3251, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npcs.giantspider2", x = 3246, z = 3248, walkRadius = 7, direction = Direction.EAST)
        spawnNpc(npc = "npcs.giantspider1", x = 3241, z = 3245, walkRadius = 7, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.giantspider1", x = 3253, z = 3243, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npcs.giantspider1", x = 3245, z = 3235, walkRadius = 7, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.giantspider1", x = 3253, z = 3234, walkRadius = 7, direction = Direction.WEST)
        spawnNpc(npc = "npcs.goblin", x = 3264, z = 3232, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npcs.godwars_goblin2", x = 3247, z = 3244, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.godwars_goblin4", x = 3244, z = 3244, walkRadius = 8, direction = Direction.NORTH)
        spawnNpc(npc = "npcs.sos_war_goblin_armed", x = 3241, z = 3242, walkRadius = 8, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.goblin", x = 3253, z = 3245, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npcs.goblin_unarmed_melee_in_3", x = 3255, z = 3236, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npcs.goblin_armed_melee_4", x = 3256, z = 3230, walkRadius = 8, direction = Direction.WEST)
        spawnNpc(npc = "npcs.goblin", x = 3221, z = 3271, walkRadius = 8, direction = Direction.WEST)
        // TODO: Add more goblin spawns
        spawnNpc(npc = "npcs.falador_man1", x = 3230, z = 3241, walkRadius = 3, direction = Direction.EAST)
        spawnNpc(npc = "npcs.man4", x = 3228, z = 3239, walkRadius = 3, direction = Direction.WEST)
        spawnNpc(npc = "npcs.woman2", x = 3229, z = 3238, walkRadius = 3, direction = Direction.SOUTH)
        spawnNpc(npc = "npcs.varrock_man1", x = 3231, z = 3236, walkRadius = 3, direction = Direction.WEST)
        spawnNpc(npc = "npcs.dragonslayer_giantrat_2", x = 3246, z = 3198, walkRadius = 5, direction = Direction.WEST)
        spawnNpc(npc = "npcs.dragonslayer_giantrat_1_key", x = 3239, z = 3198, walkRadius = 5, direction = Direction.WEST)

        // Item spawns
        spawnItem(item = "items.logs", amount = 1, x = 3205, z = 3224, height = 2)
        spawnItem(item = "items.logs", amount = 1, x = 3205, z = 3226, height = 2)
        spawnItem(item = "items.logs", amount = 1, x = 3208, z = 3225, height = 2)
        spawnItem(item = "items.logs", amount = 1, x = 3209, z = 3224, height = 2)
        spawnItem(item = "items.mindrune", amount = 1, x = 3206, z = 3208)
        spawnItem(item = "items.bronze_arrow", amount = 1, x = 3205, z = 3227)
        spawnItem(item = "items.bronze_dagger", amount = 1, x = 3213, z = 3216, height = 1)
        spawnItem(item = "items.knife", amount = 1, x = 3205, z = 3212)
        spawnItem(item = "items.knife", amount = 1, x = 3224, z = 3202)
        spawnItem(item = "items.pot_empty", amount = 1, x = 3209, z = 3214)
        spawnItem(item = "items.bowl_empty", amount = 1, x = 3208, z = 3214)
        spawnItem(item = "items.jug_empty", amount = 1, x = 3211, z = 3212)
    }
}
