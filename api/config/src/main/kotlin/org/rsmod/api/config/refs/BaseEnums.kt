@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum
import org.rsmod.api.config.aliases.EnumComp

typealias enums = BaseEnums

object BaseEnums {
    val equipment_tab_to_slots_map: EnumTypeMap<Int, EnumComp> = enum("equipment_tab_to_slots_map")
}
