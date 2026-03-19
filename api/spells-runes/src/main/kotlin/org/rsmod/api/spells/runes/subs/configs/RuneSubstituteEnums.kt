package org.rsmod.api.spells.runes.subs.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias runesub_enums = RuneSubstituteEnums

internal object RuneSubstituteEnums {
    val runes: EnumTypeMap<ItemServerType, EnumTypeMap<Int, ItemServerType>> =
        enum("rune_substitutes")
    val air_runes: EnumTypeMap<Int, ItemServerType> = enum("air_rune_substitutes")
    val water_runes: EnumTypeMap<Int, ItemServerType> = enum("water_rune_substitutes")
    val earth_runes: EnumTypeMap<Int, ItemServerType> = enum("earth_rune_substitutes")
    val fire_runes: EnumTypeMap<Int, ItemServerType> = enum("fire_rune_substitutes")
    val chaos_runes: EnumTypeMap<Int, ItemServerType> = enum("chaos_rune_substitutes")
    val death_runes: EnumTypeMap<Int, ItemServerType> = enum("death_rune_substitutes")
    val blood_runes: EnumTypeMap<Int, ItemServerType> = enum("blood_rune_substitutes")
}
