package org.rsmod.content.skills.woodcutting.configs

import dev.openrune.ParamReferences.param
import dev.openrune.types.ItemServerType
import dev.openrune.types.enums.EnumTypeMap

object WoodcuttingParams {
    val success_rates = param<EnumTypeMap<ItemServerType, Int>>("woodcutting_axe_success_rates")
    val fixed_yields = param<Int>("woodcutting_fixed_yields")
}
