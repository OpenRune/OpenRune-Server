package org.rsmod.content.other.commands

import org.rsmod.map.CoordGrid

internal data class TeleLocation(
    val name: String,
    val coords: CoordGrid,
    val aliases: List<String> = emptyList(),
)

internal object TeleLocations {
    // Developer convenience spots (not a game mechanic): spellbook landing tiles where one
    // exists (`param.spell_telecoord` on the spell objs), otherwise a walkable tile near the
    // town centre or bank.
    val all: List<TeleLocation> =
        listOf(
            TeleLocation("lumbridge", CoordGrid(3222, 3218), listOf("lumby")),
            TeleLocation("varrock", CoordGrid(3213, 3424)),
            TeleLocation("grand_exchange", CoordGrid(3164, 3486), listOf("ge")),
            TeleLocation("edgeville", CoordGrid(3093, 3493), listOf("edge")),
            TeleLocation("falador", CoordGrid(2965, 3378), listOf("fally")),
            TeleLocation("draynor", CoordGrid(3093, 3244)),
            TeleLocation("port_sarim", CoordGrid(3023, 3208), listOf("sarim")),
            TeleLocation("rimmington", CoordGrid(2957, 3214), listOf("rimmy")),
            TeleLocation("al_kharid", CoordGrid(3293, 3174), listOf("alkharid", "kharid")),
            TeleLocation("taverley", CoordGrid(2895, 3443), listOf("tav")),
            TeleLocation("burthorpe", CoordGrid(2899, 3544)),
            TeleLocation("camelot", CoordGrid(2757, 3477), listOf("cammy")),
            TeleLocation("seers_village", CoordGrid(2726, 3485), listOf("seers")),
            TeleLocation("catherby", CoordGrid(2809, 3435)),
            TeleLocation("ardougne", CoordGrid(2662, 3305), listOf("ardy")),
            TeleLocation("yanille", CoordGrid(2606, 3093)),
            TeleLocation("gnome_stronghold", CoordGrid(2461, 3443), listOf("stronghold")),
            TeleLocation("rellekka", CoordGrid(2643, 3676)),
            TeleLocation("karamja", CoordGrid(2918, 3176), listOf("musa_point")),
            TeleLocation("brimhaven", CoordGrid(2760, 3178)),
            TeleLocation("shilo_village", CoordGrid(2852, 2955), listOf("shilo")),
            TeleLocation("canifis", CoordGrid(3506, 3496)),
            TeleLocation("barrows", CoordGrid(3565, 3289)),
            TeleLocation("kourend", CoordGrid(1639, 3673), listOf("kourend_castle")),
            TeleLocation("arceuus", CoordGrid(1699, 3879)),
            TeleLocation("arceuus_library", CoordGrid(1633, 3838), listOf("library")),
            TeleLocation("battlefront", CoordGrid(1348, 3739)),
            TeleLocation(
                "civitas_illa_fortis",
                CoordGrid(1681, 3133),
                listOf("civitas", "varlamore"),
            ),
            TeleLocation("trollheim", CoordGrid(2890, 3679)),
            TeleLocation("ape_atoll", CoordGrid(2797, 2798), listOf("ape")),
            TeleLocation("ape_atoll_dungeon", CoordGrid(2771, 9101)),
            TeleLocation("edgeville_dungeon", CoordGrid(3098, 9882), listOf("paddewwa")),
            TeleLocation("digsite", CoordGrid(3321, 3336), listOf("senntisten")),
            TeleLocation("ice_mountain", CoordGrid(3004, 3470), listOf("lassar")),
            TeleLocation("draynor_manor", CoordGrid(3108, 3351), listOf("manor")),
            TeleLocation("mind_altar", CoordGrid(2980, 3509)),
            TeleLocation("salve_graveyard", CoordGrid(3432, 3461), listOf("salve")),
            TeleLocation("fenkenstrains_castle", CoordGrid(3548, 3529), listOf("fenkenstrain")),
            TeleLocation("west_ardougne", CoordGrid(2500, 3291), listOf("west_ardy")),
            TeleLocation("harmony_island", CoordGrid(3797, 2867), listOf("harmony")),
            TeleLocation("lunar_isle", CoordGrid(2114, 3915), listOf("moonclan", "lunar")),
            TeleLocation("ourania", CoordGrid(2468, 3246), listOf("zmi", "ourania_altar")),
            TeleLocation("waterbirth", CoordGrid(2546, 3756)),
            TeleLocation("barbarian_outpost", CoordGrid(2543, 3569), listOf("barb_outpost")),
            TeleLocation("port_khazard", CoordGrid(2636, 3167), listOf("khazard")),
            TeleLocation("fishing_guild", CoordGrid(2611, 3391)),
            // Wilderness.
            TeleLocation("ruins_west", CoordGrid(2968, 3696), listOf("dareeyak")),
            TeleLocation("graveyard_of_shadows", CoordGrid(3158, 3666), listOf("carrallanger")),
            TeleLocation("wilderness_cemetery", CoordGrid(2980, 3763), listOf("cemetery")),
            TeleLocation("demonic_ruins", CoordGrid(3288, 3886), listOf("annakarl")),
            TeleLocation("frozen_waste_plateau", CoordGrid(2976, 3872), listOf("ghorrock")),
        )

    /** Matches a name or alias exactly, else falls back to a unique prefix match. */
    fun find(input: String): TeleLocation? {
        val key = input.lowercase().replace("-", "_").replace(" ", "_")
        val exact = all.firstOrNull { key == it.name || key in it.aliases }
        return exact
            ?: all.filter { loc ->
                    loc.name.startsWith(key) || loc.aliases.any { it.startsWith(key) }
                }
                .singleOrNull()
    }
}
