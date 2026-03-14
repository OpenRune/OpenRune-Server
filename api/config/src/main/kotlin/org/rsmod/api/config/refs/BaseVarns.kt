package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.varn.VarnReferences

typealias varns = BaseVarns

object BaseVarns : VarnReferences() {
    val lastcombat = varn("lastcombat")
    val aggressive_player = varn("aggressive_player")
    val generic_state_2 = varn("generic_state_2")
    val attacking_player = varn("attacking_player")
    val lastattack = varn("lastattack")
    val flat_armour = varn("flat_armour")
}
