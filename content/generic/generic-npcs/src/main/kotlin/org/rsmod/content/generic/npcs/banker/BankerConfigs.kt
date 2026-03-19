@file:Suppress("ConstPropertyName")

package org.rsmod.content.generic.npcs.banker

import dev.openrune.types.enums.enum
import dev.openrune.varBit

internal typealias banker_varbits = BankerVarBits

internal typealias banker_enums = BankerEnums

object BankerVarBits {
    val blocks_purchased = varBit("bank_extra_blocks_purchased")
}

object BankerEnums {
    val block_costs = enum<Int, Int>("bank_space_purchase_block_cost")
}
