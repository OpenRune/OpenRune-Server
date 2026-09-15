package org.rsmod.content.skills.construction

const val STAT_CONSTRUCTION: String = "stat.construction"

const val OBJ_COINS: String = "obj.coins"
const val OBJ_SAW: String = "obj.poh_saw"
const val OBJ_HAMMER: String = "obj.hammer"

const val SEQ_BUILD: String = "seq.human_poh_build"

const val VARBIT_BUILD_MODE: String = "varbit.poh_building_mode"
const val VARBIT_HOUSE_STYLE: String = "varbit.poh_house_style"
const val VARBIT_HOUSE_LOCATION: String = "varbit.poh_house_location"

const val INTERFACE_FURNITURE: String = "interface.poh_furniture_creation"
const val COMPONENT_FURNITURE_CONTENTS: String = "component.poh_furniture_creation:contents"
const val CLIENTSCRIPT_FURNITURE_ENTRY: String =
    "clientscript.[clientscript,poh_furniture_creation_entry]"

const val FURNITURE_SLOTS: Int = 31

const val EXIT_PORTAL: String = "loc.poh_exit_portal"

val TOWN_PORTALS: List<String> =
    listOf(
        "loc.poh_rimmington_portal",
        "loc.poh_taverly_portal",
        "loc.poh_pollnivneach_portal",
        "loc.poh_rellekka_portal",
        "loc.poh_brimhaven_portal",
        "loc.poh_yanille_portal",
        "loc.poh_kourend_portal",
        "loc.poh_prifddinas_portal",
        "loc.poh_aldarin_portal",
    )

/**
 * Construction xp is not in the cache tables, so it is derived from what a build consumes. These are
 * the per-unit values every plank-and-nails build in OSRS works out to.
 *
 * ponytail: covers the common materials; anything unlisted builds for no xp until someone needs it.
 */
val MATERIAL_XP: Map<String, Double> =
    mapOf(
        "obj.woodplank" to 29.0,
        "obj.plank_oak" to 60.0,
        "obj.plank_teak" to 90.0,
        "obj.plank_mahogany" to 140.0,
        "obj.cloth" to 15.0,
        "obj.limestone_brick" to 20.0,
        "obj.gold_leaf" to 300.0,
        "obj.marble_block" to 500.0,
        "obj.magic_stone" to 1000.0,
    )
