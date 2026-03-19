package org.rsmod.api.spells.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias spell_enums = SpellEnums

internal object SpellEnums {
    val spellbooks: EnumTypeMap<Int, EnumTypeMap<Int, ItemServerType>> = enum("spellbooks")
    val autocast_spells = enum<Int, ItemServerType>("autocast_spells")
}
