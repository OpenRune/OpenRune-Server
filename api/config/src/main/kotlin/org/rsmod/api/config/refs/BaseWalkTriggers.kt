@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.walktrig.WalkTriggerReferences

typealias walktriggers = BaseWalkTriggers

object BaseWalkTriggers : WalkTriggerReferences() {
    val frozen = walkTrigger("frozen")
    val pvp_frozen = walkTrigger("pvp_frozen")
    val stunned = walkTrigger("stunned")
}
