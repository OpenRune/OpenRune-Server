package org.rsmod.content.skills.construction

import org.rsmod.api.enums.ConstructionEnums.construction_material_xp
import org.rsmod.api.enums.ConstructionEnums.construction_nails

const val STAT_CONSTRUCTION: String = "stat.construction"
const val STAT_MAGIC: String = "stat.magic"

const val OBJ_COINS: String = "obj.coins"
const val OBJ_SAW: String = "obj.poh_saw"
const val OBJ_HAMMER: String = "obj.hammer"

/**
 * `dbtable.furniture` asks for `obj.any_nails`, which is a build menu placeholder rather than an obj
 * anyone can hold, so a build that wants nails is paid for with whichever tier the player has,
 * cheapest first so better nails are kept back.
 */
const val OBJ_ANY_NAILS: String = "obj.any_nails"

fun materialOptions(material: String): List<String> {
    if (material != OBJ_ANY_NAILS) {
        return listOf(material)
    }
    return construction_nails.filterValuesNotNull().backing.entries
        .sortedBy { it.key }
        .map { it.value.internalName }
}

/**
 * Construction xp per unit of [material], for furniture `dbtable.construction_furniture_build` has
 * no xp for. Only the standard plank, cloth and stone materials carry a value.
 */
fun materialXp(material: String): Double {
    val tenths = construction_material_xp.backing.entries
        .firstOrNull { it.key.internalName == material }
        ?.value
    return (tenths ?: 0) / 10.0
}

const val SEQ_BUILD: String = "seq.human_poh_build"

/**
 * The build runs on a queue rather than inline. Resuming from the menu's button does not survive a
 * suspension: closing the modal costs the script its protected access, so the wait for the build
 * animation has to happen in a fresh one.
 */
const val QUEUE_BUILD: String = "queue.construction_build"
const val QUEUE_REMOVE: String = "queue.construction_remove"

const val VARBIT_BUILD_MODE: String = "varbit.poh_building_mode"
const val VARBIT_DEFAULT_BUILD_MODE: String = "varbit.poh_teleport_building_mode"
const val VARBIT_TELE_OUTSIDE: String = "varbit.poh_tele_toggle"

const val INTERFACE_HOUSE_OPTIONS: String = "interface.poh_options"
const val COMPONENT_ROOM_COUNT: String = "component.poh_options:roomcount"
const val COMPONENT_BUILD_MODE_ON: String = "component.poh_options:build_mode_on"
const val COMPONENT_BUILD_MODE_OFF: String = "component.poh_options:build_mode_off"
const val COMPONENT_LEAVE_HOUSE: String = "component.poh_options:leave_house"
const val COMPONENT_TELE_INSIDE: String = "component.poh_options:tele_on"
const val COMPONENT_TELE_OUTSIDE: String = "component.poh_options:tele_off"
const val COMPONENT_DEFAULT_BUILD_ON: String = "component.poh_options:default_build_mode_on"
const val COMPONENT_DEFAULT_BUILD_OFF: String = "component.poh_options:default_build_mode_off"
const val VARBIT_HOUSE_STYLE: String = "varbit.poh_house_style"
const val VARBIT_HOUSE_LOCATION: String = "varbit.poh_house_location"

const val INTERFACE_FURNITURE: String = "interface.poh_furniture_creation"
const val COMPONENT_FURNITURE_CONTENTS: String = "component.poh_furniture_creation:contents"

fun furnitureEntryComponent(slot: Int): String =
    "component.poh_furniture_creation:%02d".format(slot + 1)

const val CLIENTSCRIPT_FURNITURE_ENTRY: String =
    "clientscript.[clientscript,poh_furniture_creation_entry]"

const val FURNITURE_SLOTS: Int = 31

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
