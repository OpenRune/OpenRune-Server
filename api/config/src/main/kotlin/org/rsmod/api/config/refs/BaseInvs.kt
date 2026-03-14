@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.inv.InvReferences

typealias invs = BaseInvs

object BaseInvs : InvReferences() {
    val tradeoffer = inv("tradeoffer")
    val inv = inv("inv")
    val worn = inv("worn")
    val bank = inv("bank")

    val generalshop1 = inv("generalshop1")
}
