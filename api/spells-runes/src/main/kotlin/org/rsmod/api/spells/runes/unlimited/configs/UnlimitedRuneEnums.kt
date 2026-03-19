package org.rsmod.api.spells.runes.unlimited.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias unlimited_enums = UnlimitedRuneEnums

internal object UnlimitedRuneEnums {
    val rune_staves: EnumTypeMap<ItemServerType, EnumTypeMap<ItemServerType, Boolean>> =
        enum("rune_staves")
    val air_staves: EnumTypeMap<ItemServerType, Boolean> = enum("air_rune_staves")
    val water_staves: EnumTypeMap<ItemServerType, Boolean> = enum("water_rune_staves")
    val earth_staves: EnumTypeMap<ItemServerType, Boolean> = enum("earth_rune_staves")
    val fire_staves: EnumTypeMap<ItemServerType, Boolean> = enum("fire_rune_staves")

    val high_priority: EnumTypeMap<ItemServerType, EnumTypeMap<Int, ItemServerType>> =
        enum("unlimited_runes_hiprio")
    val air_high_priority: EnumTypeMap<Int, ItemServerType> = enum("air_unlimited_runes_hiprio")
    val water_high_priority: EnumTypeMap<Int, ItemServerType> = enum("water_unlimited_runes_hiprio")
    val earth_high_priority: EnumTypeMap<Int, ItemServerType> = enum("earth_unlimited_runes_hiprio")
    val fire_high_priority: EnumTypeMap<Int, ItemServerType> = enum("fire_unlimited_runes_hiprio")

    val low_priority: EnumTypeMap<ItemServerType, EnumTypeMap<Int, ItemServerType>> =
        enum("unlimited_runes_loprio")
    val water_low_priority: EnumTypeMap<Int, ItemServerType> = enum("water_unlimited_runes_loprio")
    val earth_low_priority: EnumTypeMap<Int, ItemServerType> = enum("earth_unlimited_runes_loprio")
    val fire_low_priority: EnumTypeMap<Int, ItemServerType> = enum("fire_unlimited_runes_loprio")
    val nature_low_priority: EnumTypeMap<Int, ItemServerType> =
        enum("nature_unlimited_runes_loprio")
}
