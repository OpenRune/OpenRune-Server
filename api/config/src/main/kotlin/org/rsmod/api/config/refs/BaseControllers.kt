@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ControllerType

typealias controllers = BaseControllers

object BaseControllers {
    val woodcutting_tree_duration = ControllerType("controller.woodcutting_tree_duration".asRSCM())
}
