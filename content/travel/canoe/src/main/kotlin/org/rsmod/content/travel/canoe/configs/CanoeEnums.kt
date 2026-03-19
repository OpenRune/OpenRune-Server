package org.rsmod.content.travel.canoe.configs

import dev.openrune.types.ItemServerType
import dev.openrune.types.SequenceServerType
import dev.openrune.types.enums.enum

typealias canoe_enums = CanoeEnums

object CanoeEnums {
    val station_axe_rates = enum<ItemServerType, Int>("canoe_station_axe_rates")
    val shaping_axe_rates = enum<ItemServerType, Int>("canoe_shaping_axe_rates")
    val shaping_axe_anims = enum<ItemServerType, SequenceServerType>("canoe_shaping_axe_anims")
}
