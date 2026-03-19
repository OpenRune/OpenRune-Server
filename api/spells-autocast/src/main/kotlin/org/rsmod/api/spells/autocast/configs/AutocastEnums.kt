package org.rsmod.api.spells.autocast.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum

internal typealias autocast_enums = AutocastEnums

internal object AutocastEnums {
    val spells = enum<Int, ItemServerType>("autocast_spells")
    val restricted_spells = enum<ItemServerType, Boolean>("autocast_restricted_spells")
}
