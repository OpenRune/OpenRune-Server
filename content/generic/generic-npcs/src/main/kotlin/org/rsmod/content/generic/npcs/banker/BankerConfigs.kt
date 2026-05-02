@file:Suppress("ConstPropertyName")

package org.rsmod.content.generic.npcs.banker

import dev.openrune.types.enums.enum
import dev.openrune.varBit

internal typealias banker_varbits = BankerVarBits

object BankerVarBits {
    val blocks_purchased = varBit("bank_extra_blocks_purchased")
}
