@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import dev.openrune.mod

typealias modlevels = BaseModLevels

object BaseModLevels {
    val player = mod("player")
    val moderator = mod("moderator")
    val admin = mod("admin")
    val owner = mod("owner")
}
