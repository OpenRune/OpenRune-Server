package org.rsmod.content.interfaces.prayer.tab.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.enum

internal typealias prayer_enums = PrayerTabEnums

object PrayerTabEnums {
    val obj_configs = enum<Int, ItemServerType>("prayer_oc")
    val attack_collisions = enum<Int, Boolean>("prayer_attack_collisions")
    val strength_collisions = enum<Int, Boolean>("prayer_strength_collisions")
    val defence_collisions = enum<Int, Boolean>("prayer_defence_collisions")
    val overhead_collisions = enum<Int, Boolean>("prayer_overhead_collisions")
}
