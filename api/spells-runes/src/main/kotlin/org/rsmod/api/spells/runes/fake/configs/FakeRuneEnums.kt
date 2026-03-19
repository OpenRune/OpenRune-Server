package org.rsmod.api.spells.runes.fake.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias fake_enums = FakeRuneEnums

internal object FakeRuneEnums {
    val runes: EnumTypeMap<ItemServerType, ItemServerType> = enum("fake_runes")
}
