package org.rsmod.api.spells.runes.compact.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias compact_enums = CompactRuneEnums

internal object CompactRuneEnums {
    val compact_ids: EnumTypeMap<ItemServerType, Int> = enum("rune_compact_ids")
}
