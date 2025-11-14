package org.alter.areas.lumbridge.spawns

import org.alter.game.model.Direction
import org.alter.game.pluginnew.PluginEvent


class ChatSpawnsPlugin() : PluginEvent() {

    override fun init() {
        spawnNpc("npcs.lumbridge_guide2_woman", 3228, 3222, 0, 10, Direction.SOUTH)
        spawnNpc("npcs.father_aereck", 3243, 3206, 0, 3)
        spawnNpc("npcs.hatius_lumbridge_diary", 3233, 3215, 0, 1)
        spawnNpc("npcs.ironman_tutor_1", 3229, 3228, 0, 1)
        spawnNpc("npcs.lumbridge_guide", 3238, 3220, 0, 0, Direction.WEST)

        // Castle - outside
        spawnNpc("npcs.lost_property_merchant_model", 3230, 3215, 0, 10, Direction.SOUTH)

        // Castle - inside
        spawnNpc("npcs.banker1_new", 3209, 3222, 2, 0, Direction.SOUTH)
        spawnNpc("npcs.duke_of_lumbridge", 3212, 3220, 1, 4, Direction.SOUTH)

        // Tutors
        spawnNpc("npcs.aide_tutor_melee", 3220, 3238, 0, 1)
        spawnNpc("npcs.aide_tutor_ranging", 3218, 3238, 0, 1)
        spawnNpc("npcs.aide_tutor_magic", 3216, 3238, 0, 1)
        spawnNpc("npcs.aide_tutor_woodsman", 3228, 3246, 0, 1)
        spawnNpc("npcs.aide_tutor_smithing_apprentice", 3228, 3254, 0, 1)

        spawnNpc("npcs.count_check", 3238, 3199, 0, 0, Direction.EAST)
        spawnNpc("npcs.deadman_nigel_regular", 3243, 3201, 0, 3, Direction.WEST)
    }
}
