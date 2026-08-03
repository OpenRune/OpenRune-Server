package dev.openrune.tables.skills

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.tables.columnCoord
import org.rsmod.map.CoordGrid

object ShootingStars {

    const val KEY = 0
    const val DESC = 1
    const val COORDS = 2

    fun locations() =
        dbTable("dbtable.shooting_star_locations", serverOnly = true) {
            column("key", KEY, VarType.STRING)
            column("desc", DESC, VarType.STRING)
            column("coords", COORDS, VarType.COORDGRID)
            row("dbrow.shooting_star_dwarven_mine") {
                column(KEY, "DWARVEN_MINE")
                column(DESC, "near the Dwarven Mine northern entrance")
                columnCoord(COORDS, CoordGrid(3018, 3444, 0))
            }
            row("dbrow.shooting_star_mining_guild") {
                column(KEY, "MINING_GUILD")
                column(DESC, "near the Mining Guild entrance")
                columnCoord(COORDS, CoordGrid(3030, 3348, 0))
            }
            row("dbrow.shooting_star_west_falador_mine") {
                column(KEY, "WEST_FALADOR_MINE")
                column(DESC, "at the West Falador mine")
                columnCoord(COORDS, CoordGrid(2906, 3355, 0))
            }
            row("dbrow.shooting_star_taverley") {
                column(KEY, "TAVERLEY")
                column(DESC, "near Taverley by the White Wolf Tunnel entrance")
                columnCoord(COORDS, CoordGrid(2882, 3474, 0))
            }
            row("dbrow.shooting_star_crafting_guild") {
                column(KEY, "CRAFTING_GUILD")
                column(DESC, "at the Crafting Guild")
                columnCoord(COORDS, CoordGrid(2940, 3280, 0))
            }
            row("dbrow.shooting_star_rimmington_mine") {
                column(KEY, "RIMMINGTON_MINE")
                column(DESC, "at the Rimmington mine")
                columnCoord(COORDS, CoordGrid(2974, 3241, 0))
            }
            row("dbrow.shooting_star_south_crandor_mine") {
                column(KEY, "SOUTH_CRANDOR_MINE")
                column(DESC, "at the South Crandor mine")
                columnCoord(COORDS, CoordGrid(2822, 3238, 0))
            }
            row("dbrow.shooting_star_north_crandor_mine") {
                column(KEY, "NORTH_CRANDOR_MINE")
                column(DESC, "at the North Crandor mine")
                columnCoord(COORDS, CoordGrid(2835, 3296, 0))
            }
            row("dbrow.shooting_star_north_brimhaven_mine") {
                column(KEY, "NORTH_BRIMHAVEN_MINE")
                column(DESC, "at the North Brimhaven mine")
                columnCoord(COORDS, CoordGrid(2736, 3221, 0))
            }
            row("dbrow.shooting_star_south_brimhaven_mine") {
                column(KEY, "SOUTH_BRIMHAVEN_MINE")
                column(DESC, "at the South Brimhaven mine")
                columnCoord(COORDS, CoordGrid(2742, 3143, 0))
            }
            row("dbrow.shooting_star_karamja_jungle_mine") {
                column(KEY, "KARAMJA_JUNGLE_MINE")
                column(DESC, "at the Karamja Jungle mine near the Nature Altar")
                columnCoord(COORDS, CoordGrid(2845, 3037, 0))
            }
            row("dbrow.shooting_star_shilo_village_mine") {
                column(KEY, "SHILO_VILLAGE_MINE")
                column(DESC, "at the Shilo Village mine")
                columnCoord(COORDS, CoordGrid(2827, 2999, 0))
            }
            row("dbrow.shooting_star_feldip_hunter_area") {
                column(KEY, "FELDIP_HUNTER_AREA")
                column(DESC, "in the Feldip Hunter area")
                columnCoord(COORDS, CoordGrid(2571, 2964, 0))
            }
            row("dbrow.shooting_star_rantz_cave") {
                column(KEY, "RANTZ_CAVE")
                column(DESC, "near Rantz's cave")
                columnCoord(COORDS, CoordGrid(2630, 2993, 0))
            }
            row("dbrow.shooting_star_corsair_cove") {
                column(KEY, "CORSAIR_COVE")
                column(DESC, "at Corsair Cove")
                columnCoord(COORDS, CoordGrid(2567, 2858, 0))
            }
            row("dbrow.shooting_star_corsair_cove_resource_area") {
                column(KEY, "CORSAIR_COVE_RESOURCE_AREA")
                column(DESC, "in the Corsair Cove Resource Area")
                columnCoord(COORDS, CoordGrid(2483, 2886, 0))
            }
            row("dbrow.shooting_star_myths_guild") {
                column(KEY, "MYTHS_GUILD")
                column(DESC, "near the Myths' Guild")
                columnCoord(COORDS, CoordGrid(2468, 2842, 0))
            }
            row("dbrow.shooting_star_isle_of_souls_mine") {
                column(KEY, "ISLE_OF_SOULS_MINE")
                column(DESC, "at the Isle of Souls mine")
                columnCoord(COORDS, CoordGrid(2200, 2792, 0))
            }
            row("dbrow.shooting_star_fossil_island_mine") {
                column(KEY, "FOSSIL_ISLAND_MINE")
                column(DESC, "at the Fossil Island mine")
                columnCoord(COORDS, CoordGrid(3774, 3814, 0))
            }
            row("dbrow.shooting_star_volcanic_mine") {
                column(KEY, "VOLCANIC_MINE")
                column(DESC, "near the Volcanic Mine entrance")
                columnCoord(COORDS, CoordGrid(3818, 3801, 0))
            }
            row("dbrow.shooting_star_mos_le_harmless") {
                column(KEY, "MOS_LE_HARMLESS")
                column(DESC, "on Mos Le'Harmless")
                columnCoord(COORDS, CoordGrid(3686, 2969, 0))
            }
            row("dbrow.shooting_star_rellekka_mine") {
                column(KEY, "RELLEKKA_MINE")
                column(DESC, "at the Rellekka mine")
                columnCoord(COORDS, CoordGrid(2683, 3699, 0))
            }
            row("dbrow.shooting_star_keldagrim_entrance_mine") {
                column(KEY, "KELDAGRIM_ENTRANCE_MINE")
                column(DESC, "at the Keldagrim entrance mine")
                columnCoord(COORDS, CoordGrid(2727, 3683, 0))
            }
            row("dbrow.shooting_star_miscellania_mine") {
                column(KEY, "MISCELLANIA_MINE")
                column(DESC, "at the Miscellania mine")
                columnCoord(COORDS, CoordGrid(2528, 3887, 0))
            }
            row("dbrow.shooting_star_jatizso_mine") {
                column(KEY, "JATIZSO_MINE")
                column(DESC, "near the Jatizso mine entrance")
                columnCoord(COORDS, CoordGrid(2393, 3814, 0))
            }
            row("dbrow.shooting_star_central_fremennik_isles_mine") {
                column(KEY, "CENTRAL_FREMENNIK_ISLES_MINE")
                column(DESC, "at the Central Fremennik Isles mine")
                columnCoord(COORDS, CoordGrid(2375, 3832, 0))
            }
            row("dbrow.shooting_star_lunar_isle_mine") {
                column(KEY, "LUNAR_ISLE_MINE")
                column(DESC, "near the Lunar Isle mine entrance")
                columnCoord(COORDS, CoordGrid(2139, 3938, 0))
            }
            row("dbrow.shooting_star_hosidius_mine") {
                column(KEY, "HOSIDIUS_MINE")
                column(DESC, "at the Hosidius mine")
                columnCoord(COORDS, CoordGrid(1778, 3493, 0))
            }
            row("dbrow.shooting_star_shayzien_mine") {
                column(KEY, "SHAYZIEN_MINE")
                column(DESC, "at the Shayzien mine")
                columnCoord(COORDS, CoordGrid(1597, 3648, 0))
            }
            row("dbrow.shooting_star_port_piscarilius_mine") {
                column(KEY, "PORT_PISCARILIUS_MINE")
                column(DESC, "at the Port Piscarilius mine")
                columnCoord(COORDS, CoordGrid(1769, 3709, 0))
            }
            row("dbrow.shooting_star_dense_essence_mine") {
                column(KEY, "DENSE_ESSENCE_MINE")
                column(DESC, "at the Dense essence mine")
                columnCoord(COORDS, CoordGrid(1760, 3853, 0))
            }
            row("dbrow.shooting_star_lovakite_mine") {
                column(KEY, "LOVAKITE_MINE")
                column(DESC, "at the Lovakite mine")
                columnCoord(COORDS, CoordGrid(1437, 3840, 0))
            }
            row("dbrow.shooting_star_lovakengj_bank") {
                column(KEY, "LOVAKENGJ_BANK")
                column(DESC, "near the Lovakengj bank")
                columnCoord(COORDS, CoordGrid(1534, 3747, 0))
            }
            row("dbrow.shooting_star_catherby_bank") {
                column(KEY, "CATHERBY_BANK")
                column(DESC, "near the Catherby bank")
                columnCoord(COORDS, CoordGrid(2804, 3434, 0))
            }
            row("dbrow.shooting_star_yanille_bank") {
                column(KEY, "YANILLE_BANK")
                column(DESC, "near the Yanille bank")
                columnCoord(COORDS, CoordGrid(2602, 3086, 0))
            }
            row("dbrow.shooting_star_port_khazard_mine") {
                column(KEY, "PORT_KHAZARD_MINE")
                column(DESC, "at the Port Khazard mine")
                columnCoord(COORDS, CoordGrid(2624, 3141, 0))
            }
            row("dbrow.shooting_star_legends_guild_mine") {
                column(KEY, "LEGENDS_GUILD_MINE")
                column(DESC, "at the Legends' Guild mine")
                columnCoord(COORDS, CoordGrid(2705, 3333, 0))
            }
            row("dbrow.shooting_star_coal_trucks") {
                column(KEY, "COAL_TRUCKS")
                column(DESC, "at the Coal Trucks")
                columnCoord(COORDS, CoordGrid(2589, 3478, 0))
            }
            row("dbrow.shooting_star_south_east_ardougne_mine") {
                column(KEY, "SOUTH_EAST_ARDOUGNE_MINE")
                column(DESC, "at the South-east Ardougne mine by the monastery")
                columnCoord(COORDS, CoordGrid(2608, 3233, 0))
            }
            row("dbrow.shooting_star_kebos_swamp_mine") {
                column(KEY, "KEBOS_SWAMP_MINE")
                column(DESC, "at the Kebos Lowlands mine in Kebos Swamp")
                columnCoord(COORDS, CoordGrid(1210, 3651, 0))
            }
            row("dbrow.shooting_star_mount_karuulm_mine") {
                column(KEY, "MOUNT_KARUULM_MINE")
                column(DESC, "at the Mount Karuulm mine")
                columnCoord(COORDS, CoordGrid(1279, 3817, 0))
            }
            row("dbrow.shooting_star_mount_karuulm_bank") {
                column(KEY, "MOUNT_KARUULM_BANK")
                column(DESC, "near the Mount Karuulm bank")
                columnCoord(COORDS, CoordGrid(1322, 3816, 0))
            }
            row("dbrow.shooting_star_mount_quidamortem_bank") {
                column(KEY, "MOUNT_QUIDAMORTEM_BANK")
                column(DESC, "near the Mount Quidamortem bank")
                columnCoord(COORDS, CoordGrid(1258, 3564, 0))
            }
            row("dbrow.shooting_star_al_kharid_mine") {
                column(KEY, "AL_KHARID_MINE")
                column(DESC, "at the Al Kharid mine")
                columnCoord(COORDS, CoordGrid(3296, 3298, 0))
            }
            row("dbrow.shooting_star_al_kharid_bank") {
                column(KEY, "AL_KHARID_BANK")
                column(DESC, "near the Al Kharid bank")
                columnCoord(COORDS, CoordGrid(3276, 3164, 0))
            }
            row("dbrow.shooting_star_uzer_mine") {
                column(KEY, "UZER_MINE")
                column(DESC, "at the Uzer mine")
                columnCoord(COORDS, CoordGrid(3424, 3160, 0))
            }
            row("dbrow.shooting_star_desert_quarry") {
                column(KEY, "DESERT_QUARRY")
                column(DESC, "at the Desert quarry")
                columnCoord(COORDS, CoordGrid(3171, 2910, 0))
            }
            row("dbrow.shooting_star_agility_pyramid_mine") {
                column(KEY, "AGILITY_PYRAMID_MINE")
                column(DESC, "at the Agility Pyramid mine")
                columnCoord(COORDS, CoordGrid(3316, 2867, 0))
            }
            row("dbrow.shooting_star_nardah") {
                column(KEY, "NARDAH")
                column(DESC, "in Nardah")
                columnCoord(COORDS, CoordGrid(3434, 2889, 0))
            }
            row("dbrow.shooting_star_emirs_arena") {
                column(KEY, "EMIRS_ARENA")
                column(DESC, "near Emir's Arena")
                columnCoord(COORDS, CoordGrid(3351, 3281, 0))
            }
            row("dbrow.shooting_star_east_lumbridge_swamp_mine") {
                column(KEY, "EAST_LUMBRIDGE_SWAMP_MINE")
                column(DESC, "at the East Lumbridge Swamp mine")
                columnCoord(COORDS, CoordGrid(3230, 3155, 0))
            }
            row("dbrow.shooting_star_west_lumbridge_swamp_mine") {
                column(KEY, "WEST_LUMBRIDGE_SWAMP_MINE")
                column(DESC, "at the West Lumbridge Swamp mine")
                columnCoord(COORDS, CoordGrid(3153, 3150, 0))
            }
            row("dbrow.shooting_star_draynor_village") {
                column(KEY, "DRAYNOR_VILLAGE")
                column(DESC, "in Draynor Village")
                columnCoord(COORDS, CoordGrid(3094, 3235, 0))
            }
            row("dbrow.shooting_star_varrock_east_bank") {
                column(KEY, "VARROCK_EAST_BANK")
                column(DESC, "near the Varrock east bank")
                columnCoord(COORDS, CoordGrid(3258, 3408, 0))
            }
            row("dbrow.shooting_star_south_east_varrock_mine") {
                column(KEY, "SOUTH_EAST_VARROCK_MINE")
                column(DESC, "at the South-east Varrock mine")
                columnCoord(COORDS, CoordGrid(3290, 3353, 0))
            }
            row("dbrow.shooting_star_south_west_varrock_mine") {
                column(KEY, "SOUTH_WEST_VARROCK_MINE")
                column(DESC, "at the South-west Varrock mine")
                columnCoord(COORDS, CoordGrid(3175, 3362, 0))
            }
            row("dbrow.shooting_star_canifis_bank") {
                column(KEY, "CANIFIS_BANK")
                column(DESC, "near the Canifis bank")
                columnCoord(COORDS, CoordGrid(3505, 3485, 0))
            }
            row("dbrow.shooting_star_burgh_de_rott_bank") {
                column(KEY, "BURGH_DE_ROTT_BANK")
                column(DESC, "near the Burgh de Rott bank")
                columnCoord(COORDS, CoordGrid(3500, 3219, 0))
            }
            row("dbrow.shooting_star_abandoned_mine") {
                column(KEY, "ABANDONED_MINE")
                column(DESC, "at the Abandoned Mine")
                columnCoord(COORDS, CoordGrid(3451, 3233, 0))
            }
            row("dbrow.shooting_star_ver_sinhaza_bank") {
                column(KEY, "VER_SINHAZA_BANK")
                column(DESC, "near the Ver Sinhaza bank")
                columnCoord(COORDS, CoordGrid(3650, 3214, 0))
            }
            row("dbrow.shooting_star_daeyalt_essence_mine") {
                column(KEY, "DAEYALT_ESSENCE_MINE")
                column(DESC, "near the Daeyalt essence mine entrance")
                columnCoord(COORDS, CoordGrid(3635, 3340, 0))
            }
            row("dbrow.shooting_star_piscatoris_mine") {
                column(KEY, "PISCATORIS_MINE")
                column(DESC, "at the Piscatoris mine")
                columnCoord(COORDS, CoordGrid(2341, 3635, 0))
            }
            row("dbrow.shooting_star_grand_tree") {
                column(KEY, "GRAND_TREE")
                column(DESC, "near the Grand Tree")
                columnCoord(COORDS, CoordGrid(2444, 3490, 0))
            }
            row("dbrow.shooting_star_tree_gnome_stronghold_bank") {
                column(KEY, "TREE_GNOME_STRONGHOLD_BANK")
                column(DESC, "near the Tree Gnome Stronghold bank")
                columnCoord(COORDS, CoordGrid(2448, 3436, 0))
            }
            row("dbrow.shooting_star_isafdar_mine") {
                column(KEY, "ISAFDAR_MINE")
                column(DESC, "at the Isafdar mine")
                columnCoord(COORDS, CoordGrid(2269, 3158, 0))
            }
            row("dbrow.shooting_star_arandar_mine") {
                column(KEY, "ARANDAR_MINE")
                column(DESC, "at the Arandar mine")
                columnCoord(COORDS, CoordGrid(2318, 3269, 0))
            }
            row("dbrow.shooting_star_lletya") {
                column(KEY, "LLETYA")
                column(DESC, "in Lletya")
                columnCoord(COORDS, CoordGrid(2329, 3163, 0))
            }
            row("dbrow.shooting_star_trahaearn_mine") {
                column(KEY, "TRAHAEARN_MINE")
                column(DESC, "near the Trahaearn mine entrance")
                columnCoord(COORDS, CoordGrid(3274, 6055, 0))
            }
            row("dbrow.shooting_star_mynydd_mine") {
                column(KEY, "MYNYDD_MINE")
                column(DESC, "at the Mynydd mine")
                columnCoord(COORDS, CoordGrid(2173, 3409, 0))
            }
            row("dbrow.shooting_star_civitas_illa_fortis") {
                column(KEY, "CIVITAS_ILLA_FORTIS")
                column(DESC, "in Civitas illa Fortis near the east bank")
                columnCoord(COORDS, CoordGrid(1771, 3102, 0))
            }
            row("dbrow.shooting_star_stonecutter_outpost") {
                column(KEY, "STONECUTTER_OUTPOST")
                column(DESC, "at the Stonecutter Outpost")
                columnCoord(COORDS, CoordGrid(1742, 2954, 0))
            }
            row("dbrow.shooting_star_ralos_rise_mine") {
                column(KEY, "RALOS_RISE_MINE")
                column(DESC, "at the Ralos' Rise mining site")
                columnCoord(COORDS, CoordGrid(1486, 3089, 0))
            }
            row("dbrow.shooting_star_mistrock_mine") {
                column(KEY, "MISTROCK_MINE")
                column(DESC, "at the Mistrock mine")
                columnCoord(COORDS, CoordGrid(1422, 2873, 0))
            }
            row("dbrow.shooting_star_salvager_overlook_mine") {
                column(KEY, "SALVAGER_OVERLOOK_MINE")
                column(DESC, "at the Salvager Overlook mine")
                columnCoord(COORDS, CoordGrid(1625, 3275, 0))
            }
            row("dbrow.shooting_star_custodia_mountains_mine") {
                column(KEY, "CUSTODIA_MOUNTAINS_MINE")
                column(DESC, "at the Custodia Mountains mine")
                columnCoord(COORDS, CoordGrid(1287, 3413, 0))
            }
            row("dbrow.shooting_star_south_wilderness_mine") {
                column(KEY, "SOUTH_WILDERNESS_MINE")
                column(DESC, "at the South Wilderness mine near the Mage of Zamorak")
                columnCoord(COORDS, CoordGrid(3108, 3569, 0))
            }
            row("dbrow.shooting_star_south_west_wilderness_mine") {
                column(KEY, "SOUTH_WEST_WILDERNESS_MINE")
                column(DESC, "at the South-west Wilderness mine south of Dark Warriors' Fortress")
                columnCoord(COORDS, CoordGrid(3018, 3593, 0))
            }
            row("dbrow.shooting_star_bandit_camp_mine") {
                column(KEY, "BANDIT_CAMP_MINE")
                column(DESC, "at the Bandit Camp mine among the hobgoblins")
                columnCoord(COORDS, CoordGrid(3093, 3756, 0))
            }
            row("dbrow.shooting_star_lava_maze_runite_mine") {
                column(KEY, "LAVA_MAZE_RUNITE_MINE")
                column(DESC, "at the Lava Maze runite mine")
                columnCoord(COORDS, CoordGrid(3057, 3887, 0))
            }
            row("dbrow.shooting_star_resource_area") {
                column(KEY, "RESOURCE_AREA")
                column(DESC, "in the Resource Area")
                columnCoord(COORDS, CoordGrid(3188, 3932, 0))
            }
            row("dbrow.shooting_star_mage_arena") {
                column(KEY, "MAGE_ARENA")
                column(DESC, "at the Mage Arena")
                columnCoord(COORDS, CoordGrid(3091, 3962, 0))
            }
            row("dbrow.shooting_star_pirates_hideout_mine") {
                column(KEY, "PIRATES_HIDEOUT_MINE")
                column(DESC, "at the Pirates' Hideout mine")
                columnCoord(COORDS, CoordGrid(3049, 3940, 0))
            }
        }
}
