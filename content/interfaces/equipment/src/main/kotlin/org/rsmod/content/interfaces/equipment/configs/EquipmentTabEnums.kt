package org.rsmod.content.interfaces.equipment.configs

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.enums.enum

typealias equip_enums = EquipmentTabEnums

object EquipmentTabEnums {
    val mapped_wearpos = enum<Int, ComponentType>("equipment_stats_to_slots_map")
}
