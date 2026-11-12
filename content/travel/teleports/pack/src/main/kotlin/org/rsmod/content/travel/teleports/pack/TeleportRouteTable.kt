package org.rsmod.content.travel.teleports.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * One-hop transport: a boat captain, ferryman or boat loc whose op takes the player from one
 * place to another. Each `teleport_route` row is one op of one npc or loc, the place it is used
 * from, and where it lands; an op with several rows near the player offers a choice.
 *
 * Generated from the shortest-path plugin's ships and boats transport lists, keeping the routes
 * whose op and target resolve against the cache and leaving out the ferries other modules handle.
 */
object TeleportRouteTable {
    const val NPC = 0
    const val LOC = 1
    const val OP = 2
    const val ORIGIN = 3
    const val NAME = 4
    const val DESTINATION = 5

    class Route(
        val target: String,
        val op: Int,
        val ox: Int,
        val oz: Int,
        val ol: Int,
        val name: String,
        val dx: Int,
        val dz: Int,
        val dl: Int,
    )

    val ROUTES: List<Route> =
        listOf(
            Route("npc.captain_tobias_2op", 3, 3029, 3217, 0, "Musa Point", 2956, 3146, 0),
            Route("npc.customs_officer_2op", 3, 2956, 3146, 0, "Port Sarim", 3029, 3217, 0),
            Route("npc.captain_tobias_2op", 4, 3029, 3217, 0, "Pandemonium", 3064, 3003, 0),
            Route("npc.seaman_morris", 3, 3064, 3003, 0, "Port Sarim", 3029, 3217, 0),
            Route("npc.customs_officer_2op", 4, 2956, 3146, 0, "Pandemonium", 3064, 3003, 0),
            Route("npc.seaman_morris", 4, 3064, 3003, 0, "Musa Point", 2956, 3146, 0),
            Route(
                "npc.captain_barnaby_ardougne_model",
                3, 2683, 3271, 0,
                "Brimhaven",
                2772, 3234, 0,
            ),
            Route(
                "npc.captain_barnaby_ardougne_model",
                4, 2683, 3271, 0,
                "Rimmington",
                2915, 3225, 0,
            ),
            Route("npc.captain_barnaby_karamja", 3, 2772, 3234, 0, "Ardougne", 2683, 3271, 0),
            Route("npc.captain_barnaby_karamja", 4, 2772, 3234, 0, "Rimmington", 2915, 3225, 0),
            Route("npc.captain_barnaby_rimmington", 3, 2915, 3225, 0, "Ardougne", 2683, 3271, 0),
            Route("npc.captain_barnaby_rimmington", 4, 2915, 3225, 0, "Brimhaven", 2772, 3234, 0),
            Route(
                "npc.corscurs_cabinboy_2ops",
                3, 2909, 3226, 0,
                "Rimmington, Corsair Cove",
                2578, 2840, 0,
            ),
            Route(
                "npc.corscurs_cabinboy_2ops",
                3, 2578, 2840, 0,
                "Rimmington, Corsair Cove",
                2909, 3226, 0,
            ),
            Route(
                "npc.fossilquest_barge_guard_port_postquest",
                4, 3362, 3445, 0,
                "Digsite, Fossil Island",
                3724, 3807, 0,
            ),
            Route(
                "loc.fossil_rowboat_camp",
                1, 3724, 3807, 0,
                "Digsite, Fossil Island",
                3362, 3445, 0,
            ),
            Route("npc.captain_shanks", 1, 2763, 2960, 1, "Port Khazard", 2680, 3150, 0),
            Route("npc.captain_shanks", 1, 2763, 2960, 1, "Port Sarim", 3050, 3192, 0),
            Route(
                "npc.fever_harmless_port_ship_teach",
                3, 3709, 3496, 0,
                "Port Phasmatys, Mos Le'Harmless",
                3684, 2953, 0,
            ),
            Route(
                "npc.fever_harmless_port_ship_teach",
                3, 3684, 2953, 0,
                "Port Phasmatys, Mos Le'Harmless",
                3709, 3496, 0,
            ),
            Route("npc.viking_sailor", 3, 2629, 3693, 0, "Rellekka, Miscellania", 2581, 3847, 0),
            Route("npc.misc_sailor", 3, 2581, 3847, 0, "Rellekka, Miscellania", 2629, 3693, 0),
            Route(
                "npc.lunar_pirate_captain_2ops",
                3, 2222, 3796, 2,
                "Pirates' Cove, Lunar Isle",
                2138, 3899, 2,
            ),
            Route(
                "npc.lunar_pirate_captain_2ops",
                3, 2138, 3899, 2,
                "Pirates' Cove, Lunar Isle",
                2222, 3796, 2,
            ),
            Route("npc.veos_visible_travel", 3, 3055, 3245, 0, "Port Piscarilius", 1824, 3691, 0),
            Route("npc.veos_visible_travel", 4, 3055, 3245, 0, "Land's End", 1504, 3399, 0),
            Route("npc.veos_vis", 3, 1824, 3691, 0, "Port Sarim", 3055, 3245, 0),
            Route("npc.veos_vis", 4, 1824, 3691, 0, "Land's End", 1504, 3399, 0),
            Route("npc.zeah_magoro", 3, 1504, 3399, 0, "Port Sarim", 3055, 3245, 0),
            Route("npc.zeah_magoro", 4, 1504, 3399, 0, "Port Piscarilius", 1824, 3691, 0),
            Route("npc.sunset_coast_to_aldarin_sailor", 3, 1494, 2985, 0, "Aldarin", 1443, 2977, 0),
            Route(
                "npc.aldarin_to_sunset_coast_sailor",
                3, 1443, 2977, 0,
                "Sunset Coast",
                1494, 2985, 0,
            ),
            Route("npc.holgartlandtravel", 3, 2720, 3301, 0, "Fishing Platform", 2782, 3273, 0),
            Route("npc.holgartplatform", 3, 2782, 3273, 0, "Fishing Platform", 2720, 3301, 0),
            Route(
                "npc.deal_pete",
                1, 3680, 3536, 0,
                "Post Phasmatys, Braindeath Island",
                2162, 5114, 1,
            ),
            Route(
                "npc.deal_island_pete",
                1, 2162, 5114, 1,
                "Post Phasmatys, Braindeath Island",
                3680, 3536, 0,
            ),
            Route(
                "npc.ahoy_ghost_captain_1",
                3, 3703, 3487, 0,
                "Port Phasmatys, Dragontooth Island",
                3792, 3560, 0,
            ),
            Route(
                "npc.ahoy_ghost_captain_1",
                3, 3792, 3560, 0,
                "Port Phasmatys, Dragontooth Island",
                3703, 3487, 0,
            ),
            Route(
                "loc.route_rowboat_hollows",
                1, 3498, 3380, 0,
                "Mort Myre, Mort'ton",
                3522, 3285, 0,
            ),
            Route(
                "loc.route_rowboat_mortton",
                2, 3522, 3284, 0,
                "Mort Myre, Mort'ton",
                3498, 3380, 0,
            ),
            Route(
                "npc.viking_dagganoth_cave_waterbirth_island",
                3, 2544, 3761, 0,
                "Rellekka, Waterbirth Island",
                2621, 3683, 0,
            ),
            Route(
                "npc.fris_r_ferryman_rellekka",
                3, 2644, 3709, 0,
                "Rellekka, Jatizso",
                2421, 3781, 0,
            ),
            Route("npc.fris_r_ferryman_izso", 3, 2421, 3781, 0, "Rellekka, Jatizso", 2644, 3709, 0),
            Route(
                "npc.fris_r_ferry_rellikka",
                3, 2644, 3710, 0,
                "Rellekka, Neitiznot",
                2311, 3781, 0,
            ),
            Route("npc.fris_r_ferry_iznot", 3, 2311, 3781, 0, "Rellekka, Neitiznot", 2644, 3710, 0),
            Route(
                "npc.lunar_fremennik_pirate_by_pirateship",
                3, 2620, 3693, 0,
                "Rellekka, Pirates' Cove",
                2213, 3794, 0,
            ),
            Route(
                "npc.lunar_fremennik_pirate_piratecove",
                3, 2213, 3794, 0,
                "Rellekka, Pirates' Cove",
                2620, 3693, 0,
            ),
            Route(
                "loc.peng_row_boat_clickzone",
                1, 2658, 3988, 1,
                "Rellekka, Iceberg",
                2706, 3735, 0,
            ),
            Route("loc.peng_boat_ice", 1, 2848, 3966, 0, "Rellekka, Weiss", 2706, 3735, 0),
            Route(
                "loc.island_of_stone_boat",
                1, 2472, 3994, 0,
                "Rellekka, Island of Stone",
                2621, 3692, 0,
            ),
            Route("loc.ungael_boat", 1, 2277, 4034, 0, "Rellekka, Ungael", 2640, 3697, 0),
            Route(
                "loc.lithkren_rowboat",
                1, 3583, 3973, 0,
                "Fossil Island, Lithkren",
                3660, 3849, 0,
            ),
            Route(
                "loc.fossil_rowboat_camp",
                1, 3724, 3807, 0,
                "Fossil Island, Underwater",
                3734, 3893, 0,
            ),
            Route(
                "loc.fossil_rowboat_north",
                1, 3734, 3893, 0,
                "Fossil Island, Underwater",
                3724, 3807, 0,
            ),
            Route(
                "loc.fossil_rowboat_north",
                1, 3734, 3893, 0,
                "Fossil Island, Underwater",
                3764, 3899, 0,
            ),
            Route(
                "loc.fossil_rowboat_diving",
                1, 3763, 3899, 0,
                "Fossil Island, Underwater",
                3734, 3893, 0,
            ),
            Route("npc.tribes_boatman", 3, 1783, 3458, 0, "Hosidius, Crabclaw Isle", 1778, 3417, 0),
            Route(
                "npc.tribes_boatman_island",
                3, 1779, 3417, 0,
                "Hosidius, Crabclaw Isle",
                1784, 3458, 0,
            ),
            Route("npc.mm_lumdo_2ops", 3, 2892, 2724, 0, "Crash Island, Ape Atoll", 2803, 2706, 0),
            Route("npc.mm_lumdo_2ops", 3, 2803, 2706, 0, "Crash Island, Ape Atoll", 2892, 2724, 0),
            Route(
                "loc.tempoross_lobby_boat",
                1, 3271, 3144, 0,
                "Al Kharid, Ruins of Unkah / Tempoross",
                3148, 2843, 0,
            ),
            Route(
                "loc.tempoross_lobby_boat",
                1, 3148, 2843, 0,
                "Al Kharid, Ruins of Unkah / Tempoross",
                3272, 3143, 0,
            ),
            Route(
                "loc.dt2_stranglewood_boat_vis",
                1, 1229, 3470, 0,
                "Stranglewood, Mount Quidamortem",
                1194, 3452, 0,
            ),
            Route(
                "loc.dt2_stranglewood_boat_out",
                1, 1194, 3452, 0,
                "Stranglewood, Mount Quidamortem",
                1229, 3469, 0,
            ),
            Route("loc.aerial_fishing_boat", 1, 1384, 3665, 0, "Molch Island", 1369, 3639, 0),
            Route("loc.aerial_fishing_boat", 1, 1384, 3665, 0, "Molch", 1341, 3645, 0),
            Route("loc.aerial_fishing_boat", 1, 1384, 3665, 0, "Shayzien", 1408, 3612, 0),
            Route("loc.aerial_fishing_boat", 1, 1342, 3645, 0, "Battlefront", 1384, 3665, 0),
            Route("loc.myq5_boat_vis", 1, 3525, 3170, 0, "Meiyerditch", 3605, 3161, 1),
            Route("loc.myq5_boat_vis", 1, 3525, 3170, 0, "Icyene Graveyard", 3684, 3176, 0),
            Route("loc.myq5_boat_vis", 1, 3525, 3170, 0, "Slepe", 3663, 3277, 0),
            Route("loc.myq5_boat_vis", 1, 3605, 3163, 1, "Burgh de Rott", 3525, 3170, 0),
            Route("loc.slepe_boat", 1, 3671, 3544, 0, "Port Phasmatys, Slepe", 3661, 3277, 0),
            Route(
                "loc.slepe_boat_return",
                1, 3661, 3278, 0,
                "Port Phasmatys, Slepe",
                3671, 3542, 0,
            ),
            Route("npc.tlati_riverboat_owner_south", 3, 1390, 3073, 0, "Tal Teklan", 1257, 3126, 0),
            Route("npc.tlati_riverboat_owner_west", 3, 1258, 3124, 0, "Kastori", 1390, 3074, 0),
            Route("npc.tlati_riverboat_owner_north", 3, 1399, 3246, 0, "Tal Teklan", 1257, 3126, 0),
            Route("npc.tlati_riverboat_owner_north", 4, 1399, 3246, 0, "Kastori", 1390, 3074, 0),
            Route(
                "loc.custodia_rowboat_west",
                1, 1256, 3394, 0,
                "Rowboat over Custodia's lake",
                1282, 3385, 0,
            ),
            Route(
                "loc.custodia_rowboat_south",
                1, 1281, 3385, 0,
                "Rowboat over Custodia's lake",
                1256, 3395, 0,
            ),
            Route(
                "npc.dwarf_city_boatman_mines_postquest",
                3, 2841, 10129, 0,
                "Keldagrim boats",
                2892, 10225, 0,
            ),
            Route(
                "npc.dwarf_city_boatman_mines_postquest",
                3, 2888, 10226, 0,
                "Keldagrim boats",
                2838, 10127, 0,
            ),
            Route("npc.dwarfrock_ferryman1", 3, 2837, 10128, 0, "Keldagrim boats", 2837, 10143, 0),
            Route("npc.dwarfrock_ferryman2", 3, 2855, 10145, 0, "Keldagrim boats", 2863, 10133, 0),
            Route("npc.swan_boatwoman_2", 3, 2357, 3641, 0, "Gnome Stronghold", 2368, 3486, 0),
            Route("loc.conch_rowboat_north", 1, 3197, 2531, 0, "The Summer Shore", 3191, 2370, 0),
            Route("loc.conch_rowboat_north", 1, 3197, 2531, 0, "Eastern Coast", 3264, 2463, 0),
            Route("loc.conch_rowboat_east", 1, 3264, 2463, 0, "The Summer Shore", 3191, 2370, 0),
            Route("loc.conch_rowboat_east", 1, 3264, 2463, 0, "Northern Coast", 3197, 2531, 0),
            Route("loc.conch_rowboat_south", 1, 3191, 2370, 0, "Northern Coast", 3197, 2531, 0),
            Route("loc.conch_rowboat_south", 1, 3191, 2370, 0, "Eastern Coast", 3264, 2463, 0),
        )

    fun routes() =
        dbTable("dbtable.teleport_route", serverOnly = true) {
            column("npc", NPC, VarType.NPC)
            column("loc", LOC, VarType.LOC)
            column("op", OP, VarType.INT)
            column("origin", ORIGIN, VarType.COORDGRID)
            column("name", NAME, VarType.STRING)
            column("destination", DESTINATION, VarType.COORDGRID)

            ROUTES.forEachIndexed { index, route ->
                row("dbrow.teleport_route_$index") {
                    if (route.target.startsWith("npc.")) {
                        columnRSCM(NPC, route.target)
                    } else {
                        columnRSCM(LOC, route.target)
                    }
                    column(OP, route.op)
                    columnCoord(ORIGIN, CoordGrid(route.ox, route.oz, route.ol))
                    column(NAME, route.name)
                    columnCoord(DESTINATION, CoordGrid(route.dx, route.dz, route.dl))
                }
            }
        }
}
