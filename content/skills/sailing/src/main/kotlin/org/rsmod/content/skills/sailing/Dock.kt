package org.rsmod.content.skills.sailing

import org.rsmod.map.CoordGrid

data class Dock(
    val name: String,
    val returnTile: CoordGrid,
    val gangplankTile: CoordGrid,
    val boatTile: CoordGrid,
    val sailingLevel: Int,
    val angle: Int = 0,
)

object Docks {
    val all =
        listOf(
            dock("Port Sarim", 3050, 3193, 3051, 3194, 3053, 3193, 1),
            dock("The Pandemonium", 3069, 2987, 3070, 2987, 3072, 2987, 1),
            dock("Land's End", 1506, 3403, 1507, 3403, 1509, 3403, 5),
            dock("Hosidius", 1726, 3453, 1726, 3452, 1726, 3450, 5, angle = 512),
            dock("Musa Point", 2960, 3146, 2961, 3146, 2963, 3146, 10, angle = 1024),
            dock("Port Piscarilius", 1845, 3688, 1845, 3687, 1845, 3684, 15),
            dock("Rimmington", 2924, 3175, 2925, 3175, 2927, 3175, 18),
            dock("Catherby", 2793, 3408, 2794, 3408, 2796, 3408, 20),
            dock("Brimhaven", 2751, 3231, 2752, 3231, 2754, 3231, 25),
            dock("Ardougne", 2667, 3259, 2668, 3259, 2670, 3259, 28),
            dock("Port Khazard", 2685, 3162, 2686, 3162, 2688, 3162, 30),
            dock("Witchaven", 2726, 3286, 2727, 3286, 2729, 3286, 34),
            dock("Entrana", 2880, 3336, 2881, 3336, 2883, 3336, 36),
            dock("Civitas illa Fortis", 1766, 3144, 1767, 3144, 1769, 3144, 38),
            dock("Corsair Cove", 2583, 2844, 2584, 2844, 2586, 2844, 40),
            dock("Cairn Isle", 2742, 2952, 2743, 2952, 2745, 2952, 42),
            dock("Sunset Coast", 1513, 2974, 1512, 2974, 1510, 2974, 44),
            dock("The Summer Shore", 3174, 2368, 3172, 2367, 3174, 2364, 45),
            dock("Aldarin", 1452, 2969, 1452, 2970, 1452, 2973, 46, angle = 1024),
            dock("Ruins of Unkah", 3145, 2825, 3141, 2824, 3143, 2824, 48),
            dock("Void Knights' Outpost", 2648, 2683, 2649, 2683, 2651, 2683, 50),
            dock("Port Roberts", 1855, 3307, 1856, 3307, 1858, 3307, 50),
            dock("Red Rock", 2811, 2510, 2812, 2510, 2814, 2510, 50),
            dock("Rellekka", 2627, 3709, 2628, 3709, 2630, 3709, 62),
            dock("Etceteria", 2609, 3836, 2610, 3836, 2612, 3836, 65),
            dock("Port Tyras", 2138, 3115, 2139, 3115, 2141, 3115, 66),
            dock("Deepfin Point", 1920, 2752, 1921, 2752, 1923, 2752, 67),
            dock("Jatizso", 2409, 3776, 2410, 3776, 2412, 3776, 68),
            dock("Neitiznot", 2299, 3782, 2300, 3782, 2302, 3782, 68),
            dock("Prifddinas", 2155, 3319, 2156, 3319, 2158, 3319, 70),
            dock("Piscatoris", 2297, 3689, 2298, 3689, 2300, 3689, 75),
            dock("Lunar Isle", 2154, 3881, 2155, 3881, 2157, 3881, 76),
        )

    fun byName(name: String): Dock? = all.firstOrNull { it.name.equals(name, ignoreCase = true) }

    fun nearest(coords: CoordGrid, maxDistance: Int = MAX_GANGPLANK_DISTANCE): Dock? {
        val dock = all.minByOrNull { it.gangplankTile.chebyshevDistance(coords) } ?: return null
        val within =
            dock.gangplankTile.level == coords.level &&
                dock.gangplankTile.chebyshevDistance(coords) <= maxDistance
        return if (within) dock else null
    }

    private const val MAX_GANGPLANK_DISTANCE = 8

    private fun dock(
        name: String,
        retX: Int,
        retZ: Int,
        plankX: Int,
        plankZ: Int,
        boatX: Int,
        boatZ: Int,
        sailingLevel: Int,
        angle: Int = 0,
    ): Dock =
        Dock(
            name = name,
            returnTile = CoordGrid(retX, retZ),
            gangplankTile = CoordGrid(plankX, plankZ),
            boatTile = CoordGrid(boatX, boatZ),
            sailingLevel = sailingLevel,
            angle = angle,
        )
}
