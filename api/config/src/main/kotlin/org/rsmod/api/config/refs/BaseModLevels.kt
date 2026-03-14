@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.mod.ModLevelReferences

typealias modlevels = BaseModLevels

object BaseModLevels : ModLevelReferences() {
    val player = mod("player")
    val moderator = mod("moderator")
    val admin = mod("admin")
    val owner = mod("owner")
}
