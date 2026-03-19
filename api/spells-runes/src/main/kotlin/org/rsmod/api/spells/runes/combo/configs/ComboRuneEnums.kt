package org.rsmod.api.spells.runes.combo.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias combo_enums = ComboRuneEnums

internal object ComboRuneEnums {
    val combos: EnumTypeMap<ItemServerType, EnumTypeMap<Int, ItemServerType>> = enum("combo_runes")
    val mist_rune: EnumTypeMap<Int, ItemServerType> = enum("combo_rune_mist")
    val dust_rune: EnumTypeMap<Int, ItemServerType> = enum("combo_rune_dust")
    val mud_rune: EnumTypeMap<Int, ItemServerType> = enum("combo_rune_mud")
    val smoke_rune: EnumTypeMap<Int, ItemServerType> = enum("combo_rune_smoke")
    val steam_rune: EnumTypeMap<Int, ItemServerType> = enum("combo_rune_steam")
    val lava_rune: EnumTypeMap<Int, ItemServerType> = enum("combo_rune_lava")
}
