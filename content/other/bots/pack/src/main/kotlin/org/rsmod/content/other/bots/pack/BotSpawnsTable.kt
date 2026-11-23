package org.rsmod.content.other.bots.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

object BotSpawnsTable {
    const val ACTIVITY = 0
    const val COORDS = 1
    const val RADIUS = 2
    const val COUNT = 3
    const val STYLES = 4
    const val BANK = 5

    fun spawns() =
        dbTable("dbtable.bot_spawns", serverOnly = true) {
            column("activity", ACTIVITY, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            column("radius", RADIUS, VarType.INT)
            column("count", COUNT, VarType.INT)
            column("styles", STYLES, VarType.STRING)
            column("bank", BANK, VarType.COORDGRID)
            row("dbrow.bot_spawn_lumbridge_courtyard") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3222, 3219, 0))
                column(RADIUS, 5)
                column(COUNT, 4)
                column(STYLES, "adventurer", "plain", "plain", "graceful")
            }
            row("dbrow.bot_spawn_lumbridge_trees") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(3192, 3240, 0))
                column(RADIUS, 10)
                column(COUNT, 2)
                column(STYLES, "lumberjack", "plain", "plain")
            }
            row("dbrow.bot_spawn_lumbridge_swamp_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(3228, 3147, 0))
                column(RADIUS, 5)
                column(COUNT, 2)
                column(STYLES, "prospector", "plain", "plain")
            }
            row("dbrow.bot_spawn_lumbridge_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(3222, 3218, 0))
                column(RADIUS, 25)
                column(COUNT, 2)
                column(STYLES, "adventurer", "adventurer", "plain")
            }
            row("dbrow.bot_spawn_draynor_willows") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(3087, 3235, 0))
                column(RADIUS, 6)
                column(COUNT, 3)
                column(STYLES, "lumberjack", "plain", "adventurer")
                columnCoord(BANK, CoordGrid(3092, 3245, 0))
            }
            row("dbrow.bot_spawn_draynor_fishing") {
                column(ACTIVITY, "FISHING")
                columnCoord(COORDS, CoordGrid(3087, 3228, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "angler", "plain")
                columnCoord(BANK, CoordGrid(3092, 3245, 0))
            }
            row("dbrow.bot_spawn_draynor_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3093, 3243, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain", "wealthy")
            }
            row("dbrow.bot_spawn_draynor_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(3093, 3256, 0))
                column(RADIUS, 20)
                column(COUNT, 1)
                column(STYLES, "adventurer", "plain")
            }
            row("dbrow.bot_spawn_varrock_east_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(3286, 3366, 0))
                column(RADIUS, 6)
                column(COUNT, 3)
                column(STYLES, "prospector", "plain", "adventurer")
                columnCoord(BANK, CoordGrid(3253, 3420, 0))
            }
            row("dbrow.bot_spawn_varrock_square") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(3212, 3428, 0))
                column(RADIUS, 10)
                column(COUNT, 6)
                column(STYLES, "graceful", "graceful", "adventurer", "plain")
            }
            row("dbrow.bot_spawn_varrock_west_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3185, 3438, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "wealthy", "graceful")
            }
            row("dbrow.bot_spawn_varrock_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(3212, 3428, 0))
                column(RADIUS, 30)
                column(COUNT, 2)
                column(STYLES, "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_grand_exchange") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3165, 3487, 0))
                column(RADIUS, 8)
                column(COUNT, 12)
                column(STYLES, "wealthy", "wealthy", "wealthy", "graceful", "adventurer")
            }
            row("dbrow.bot_spawn_edgeville_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3094, 3492, 0))
                column(RADIUS, 3)
                column(COUNT, 4)
                column(STYLES, "wealthy", "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_barbarian_village_fishing") {
                column(ACTIVITY, "FISHING")
                columnCoord(COORDS, CoordGrid(3105, 3430, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "angler", "plain")
                columnCoord(BANK, CoordGrid(3094, 3491, 0))
            }
            row("dbrow.bot_spawn_falador_park") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(2995, 3375, 0))
                column(RADIUS, 10)
                column(COUNT, 2)
                column(STYLES, "graceful", "adventurer")
            }
            row("dbrow.bot_spawn_falador_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(2965, 3380, 0))
                column(RADIUS, 25)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain")
            }
            row("dbrow.bot_spawn_al_kharid_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(3299, 3300, 0))
                column(RADIUS, 10)
                column(COUNT, 2)
                column(STYLES, "prospector", "plain")
            }
            row("dbrow.bot_spawn_grand_exchange_traders") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(3165, 3487, 0))
                column(RADIUS, 14)
                column(COUNT, 4)
                column(STYLES, "graceful", "wealthy", "wealthy", "adventurer")
            }
            row("dbrow.bot_spawn_varrock_palace_trees") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(3205, 3460, 0))
                column(RADIUS, 12)
                column(COUNT, 2)
                column(STYLES, "lumberjack", "plain", "adventurer")
                columnCoord(BANK, CoordGrid(3185, 3438, 0))
            }
            row("dbrow.bot_spawn_varrock_west_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(3180, 3370, 0))
                column(RADIUS, 8)
                column(COUNT, 3)
                column(STYLES, "prospector", "plain", "adventurer")
            }
            row("dbrow.bot_spawn_barbarian_village_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(3081, 3421, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "prospector", "plain")
            }
            row("dbrow.bot_spawn_edgeville_yews") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(3087, 3470, 0))
                column(RADIUS, 12)
                column(COUNT, 2)
                column(STYLES, "lumberjack", "adventurer", "graceful")
                columnCoord(BANK, CoordGrid(3094, 3491, 0))
            }
            row("dbrow.bot_spawn_lumbridge_west_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(3146, 3148, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "prospector", "adventurer")
            }
            row("dbrow.bot_spawn_draynor_oaks") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(3105, 3245, 0))
                column(RADIUS, 10)
                column(COUNT, 2)
                column(STYLES, "lumberjack", "plain")
                columnCoord(BANK, CoordGrid(3092, 3245, 0))
            }
            row("dbrow.bot_spawn_port_sarim") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(3029, 3217, 0))
                column(RADIUS, 8)
                column(COUNT, 2)
                column(STYLES, "plain", "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_rimmington_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(2977, 3240, 0))
                column(RADIUS, 8)
                column(COUNT, 3)
                column(STYLES, "prospector", "plain", "adventurer")
            }
            row("dbrow.bot_spawn_crafting_guild_mine") {
                column(ACTIVITY, "MINING")
                columnCoord(COORDS, CoordGrid(2938, 3282, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "prospector", "adventurer")
            }
            row("dbrow.bot_spawn_falador_east_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3013, 3357, 0))
                column(RADIUS, 3)
                column(COUNT, 3)
                column(STYLES, "adventurer", "wealthy", "graceful", "plain")
            }
            row("dbrow.bot_spawn_falador_west_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2946, 3370, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain", "graceful")
            }
            row("dbrow.bot_spawn_taverley_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(2895, 3445, 0))
                column(RADIUS, 15)
                column(COUNT, 2)
                column(STYLES, "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_burthorpe") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(2899, 3544, 0))
                column(RADIUS, 8)
                column(COUNT, 2)
                column(STYLES, "adventurer", "graceful", "plain")
            }
            row("dbrow.bot_spawn_seers_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2725, 3491, 0))
                column(RADIUS, 3)
                column(COUNT, 3)
                column(STYLES, "wealthy", "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_seers_maples") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(2729, 3500, 0))
                column(RADIUS, 6)
                column(COUNT, 3)
                column(STYLES, "lumberjack", "plain", "adventurer")
                columnCoord(BANK, CoordGrid(2725, 3491, 0))
            }
            row("dbrow.bot_spawn_seers_yews") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(2709, 3462, 0))
                column(RADIUS, 8)
                column(COUNT, 3)
                column(STYLES, "lumberjack", "adventurer", "graceful")
                columnCoord(BANK, CoordGrid(2725, 3491, 0))
            }
            row("dbrow.bot_spawn_seers_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(2725, 3485, 0))
                column(RADIUS, 25)
                column(COUNT, 2)
                column(STYLES, "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_sorcerers_tower_magics") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(2701, 3397, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "lumberjack", "wealthy")
            }
            row("dbrow.bot_spawn_catherby_fishing") {
                column(ACTIVITY, "FISHING")
                columnCoord(COORDS, CoordGrid(2846, 3431, 0))
                column(RADIUS, 10)
                column(COUNT, 4)
                column(STYLES, "angler", "plain", "adventurer")
                columnCoord(BANK, CoordGrid(2809, 3440, 0))
            }
            row("dbrow.bot_spawn_catherby_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2809, 3440, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain", "graceful")
            }
            row("dbrow.bot_spawn_fishing_guild") {
                column(ACTIVITY, "FISHING")
                columnCoord(COORDS, CoordGrid(2603, 3415, 0))
                column(RADIUS, 10)
                column(COUNT, 3)
                column(STYLES, "angler", "angler", "adventurer")
                columnCoord(BANK, CoordGrid(2586, 3420, 0))
            }
            row("dbrow.bot_spawn_ardougne_market") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(2662, 3306, 0))
                column(RADIUS, 10)
                column(COUNT, 5)
                column(STYLES, "graceful", "adventurer", "plain", "wealthy")
            }
            row("dbrow.bot_spawn_ardougne_north_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2616, 3333, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "wealthy", "plain")
            }
            row("dbrow.bot_spawn_ardougne_south_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2655, 3284, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain", "graceful")
            }
            row("dbrow.bot_spawn_ardougne_quester") {
                column(ACTIVITY, "QUESTING")
                columnCoord(COORDS, CoordGrid(2662, 3306, 0))
                column(RADIUS, 30)
                column(COUNT, 2)
                column(STYLES, "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_yanille_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2612, 3093, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain", "wealthy")
            }
            row("dbrow.bot_spawn_castle_wars") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(2440, 3090, 0))
                column(RADIUS, 6)
                column(COUNT, 3)
                column(STYLES, "wealthy", "adventurer", "adventurer", "graceful")
            }
            row("dbrow.bot_spawn_al_kharid_bank") {
                column(ACTIVITY, "IDLING")
                columnCoord(COORDS, CoordGrid(3269, 3168, 0))
                column(RADIUS, 3)
                column(COUNT, 2)
                column(STYLES, "adventurer", "plain", "graceful")
            }
            row("dbrow.bot_spawn_shantay_pass") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(3304, 3122, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "graceful", "adventurer", "plain")
            }
            row("dbrow.bot_spawn_musa_point_fishing") {
                column(ACTIVITY, "FISHING")
                columnCoord(COORDS, CoordGrid(2925, 3176, 0))
                column(RADIUS, 8)
                column(COUNT, 2)
                column(STYLES, "angler", "plain")
            }
            row("dbrow.bot_spawn_canifis") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(3494, 3480, 0))
                column(RADIUS, 10)
                column(COUNT, 2)
                column(STYLES, "graceful", "adventurer")
            }
            row("dbrow.bot_spawn_rellekka") {
                column(ACTIVITY, "WANDERING")
                columnCoord(COORDS, CoordGrid(2660, 3660, 0))
                column(RADIUS, 10)
                column(COUNT, 2)
                column(STYLES, "adventurer", "graceful", "plain")
            }
            row("dbrow.bot_spawn_woodcutting_guild_yews") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(1592, 3487, 0))
                column(RADIUS, 8)
                column(COUNT, 3)
                column(STYLES, "lumberjack", "lumberjack", "graceful")
            }
            row("dbrow.bot_spawn_woodcutting_guild_magics") {
                column(ACTIVITY, "WOODCUTTING")
                columnCoord(COORDS, CoordGrid(1580, 3482, 0))
                column(RADIUS, 6)
                column(COUNT, 2)
                column(STYLES, "lumberjack", "wealthy")
            }
        }
}
