package org.rsmod.api.spells.runes.staves.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.enum

internal typealias staff_enums = StaffSubstituteEnums

internal object StaffSubstituteEnums {
    val staves: EnumTypeMap<ItemServerType, EnumTypeMap<Int, ItemServerType>> =
        enum("staff_substitutes")
    val guthix_staff: EnumTypeMap<Int, ItemServerType> = enum("guthix_staff_substitutes")
    val zamorak_staff: EnumTypeMap<Int, ItemServerType> = enum("zamorak_staff_substitutes")
    val saradomin_staff: EnumTypeMap<Int, ItemServerType> = enum("saradomin_staff_substitutes")
    val slayer_staff: EnumTypeMap<Int, ItemServerType> = enum("slayer_staff_substitutes")
    val iban_staff: EnumTypeMap<Int, ItemServerType> = enum("iban_staff_substitutes")
}
