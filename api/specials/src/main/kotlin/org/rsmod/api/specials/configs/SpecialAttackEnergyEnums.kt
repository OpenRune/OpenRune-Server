package org.rsmod.api.specials.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum

internal typealias energy_enums = SpecialAttackEnergyEnums

internal object SpecialAttackEnergyEnums {
    val energy_requirements = enum<ItemServerType, Int>("sa_energy_requirements")
    val descriptions = enum<ItemServerType, String>("sa_descriptions")
}
