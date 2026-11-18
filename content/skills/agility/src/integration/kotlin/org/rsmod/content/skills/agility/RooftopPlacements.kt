package org.rsmod.content.skills.agility

import org.rsmod.map.CoordGrid

/** The locs on bridge tiles (Canifis, Ardougne's last gap) sit a level below their map level. */
internal val ROOFTOP_PLACEMENTS =
    listOf(
        Placement("loc.rooftops_ardy_jump", CoordGrid(2670, 3310, 3), 3, 10),
        Placement("loc.rooftops_ardy_jump_2", CoordGrid(2653, 3317, 3), 2, 10),
        Placement("loc.rooftops_ardy_jump_3", CoordGrid(2653, 3308, 3), 2, 10),
        Placement("loc.rooftops_ardy_jump_4", CoordGrid(2656, 3296, 3), 1, 10),
        Placement("loc.rooftops_ardy_plank", CoordGrid(2661, 3318, 3), 3, 22),
        Placement("loc.rooftops_ardy_wallclimb", CoordGrid(2673, 3298, 0), 1, 5),
        Placement("loc.rooftops_ardy_wallcrossing", CoordGrid(2654, 3300, 3), 1, 11),
        Placement("loc.rooftops_canifis_jump", CoordGrid(3505, 3498, 2), 3, 11),
        Placement("loc.rooftops_canifis_jump_2", CoordGrid(3496, 3504, 2), 3, 10),
        Placement("loc.rooftops_canifis_jump_3", CoordGrid(3478, 3491, 3), 2, 10),
        Placement("loc.rooftops_canifis_jump_4", CoordGrid(3503, 3476, 3), 1, 10),
        Placement("loc.rooftops_canifis_jump_5", CoordGrid(3485, 3499, 2), 3, 10),
        Placement("loc.rooftops_canifis_leapdown", CoordGrid(3510, 3483, 2), 0, 10),
        Placement("loc.rooftops_canifis_polevault", CoordGrid(3480, 3483, 2), 3, 11),
        Placement("loc.rooftops_canifis_start_tree", CoordGrid(3505, 3489, 0), 2, 10),
        Placement("loc.rooftops_kharid_bamboo_tree_top", CoordGrid(3318, 3166, 1), 0, 10),
        Placement("loc.rooftops_kharid_leapdown", CoordGrid(3300, 3193, 3), 3, 11),
        Placement("loc.rooftops_kharid_rope_swing", CoordGrid(3269, 3166, 3), 1, 10),
        Placement("loc.rooftops_kharid_slide_side", CoordGrid(3302, 3163, 3), 3, 10),
        Placement("loc.rooftops_kharid_tightrope_1", CoordGrid(3272, 3181, 3), 2, 22),
        Placement("loc.rooftops_kharid_tightrope_4", CoordGrid(3313, 3186, 3), 3, 22),
        Placement("loc.rooftops_kharid_wallclimb", CoordGrid(3273, 3195, 0), 3, 4),
        Placement("loc.rooftops_kharid_wallclimb_2", CoordGrid(3316, 3179, 2), 1, 4),
        Placement("loc.rooftops_pollnivneach_line", CoordGrid(3363, 3000, 2), 0, 10),
        Placement("loc.rooftops_rellekka_tightrope_1", CoordGrid(2623, 3658, 3), 0, 10),
        Placement("loc.rooftops_seers_jump", CoordGrid(2720, 3492, 3), 0, 10),
        Placement("loc.rooftops_seers_jump_1", CoordGrid(2710, 3476, 2), 0, 10),
        Placement("loc.rooftops_seers_jump_2", CoordGrid(2700, 3469, 3), 0, 10),
        Placement("loc.rooftops_seers_leapdown", CoordGrid(2703, 3461, 2), 0, 10),
        Placement("loc.rooftops_seers_tightrope", CoordGrid(2710, 3489, 2), 2, 22),
        Placement("loc.rooftops_seers_wallclimb", CoordGrid(2729, 3489, 0), 1, 5),
    )
