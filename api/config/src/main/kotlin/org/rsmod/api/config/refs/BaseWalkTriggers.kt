@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.walkTrigger

typealias walktriggers = BaseWalkTriggers

object BaseWalkTriggers {
    val frozen = walkTrigger("frozen")
    val pvp_frozen = walkTrigger("pvp_frozen")
    val stunned = walkTrigger("stunned")
}
