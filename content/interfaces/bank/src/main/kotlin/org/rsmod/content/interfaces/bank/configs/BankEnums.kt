package org.rsmod.content.interfaces.bank.configs

import dev.openrune.definition.type.widget.ComponentType
import dev.openrune.types.enums.enum

internal typealias bank_enums = BankEnums

object BankEnums {
    val worn_component_map = enum<Int, ComponentType>("bank_equipment_tab_to_slots_map")
}
