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

/**
 * The entries are named `:01`..`:31` and are static children of the list, not subcomponents of
 * `contents`, so each one carries its own click event.
 */
fun furnitureEntryComponent(slot: Int): String =
    "component.poh_furniture_creation:%02d".format(slot + 1)
const val CLIENTSCRIPT_FURNITURE_ENTRY: String =
    "clientscript.[clientscript,poh_furniture_creation_entry]"

const val FURNITURE_SLOTS: Int = 31

/**
 * The build menu's entries are children 4..34 of the interface, a list layer whose children are
 * `hide=yes` in the cache, so the server shows the ones it fills. `enum_1461` is the client's own
 * slot -> component map and starts at this child.
 */
const val FURNITURE_FIRST_ENTRY_CHILD: Int = 4

/** The entries carry no position of their own and the list layer does not arrange them. */
/** How many children the entry script builds inside one entry; its op resumes from one of them. */
const val FURNITURE_ENTRY_CHILDREN: Int = 16

const val FURNITURE_ENTRY_COLUMNS: Int = 2
const val FURNITURE_ENTRY_WIDTH: Int = 228
const val FURNITURE_ENTRY_HEIGHT: Int = 60

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
