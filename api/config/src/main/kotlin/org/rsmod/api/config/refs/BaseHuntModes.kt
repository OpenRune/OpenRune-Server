package org.rsmod.api.config.refs

import dev.openrune.hunt

typealias huntmodes = BaseHuntModes

object BaseHuntModes {
    val ranged = hunt("ranged")
    val constant_melee = hunt("constant_melee")
    val constant_ranged = hunt("constant_ranged")
    val cowardly = hunt("cowardly")
    val notbusy_melee = hunt("notbusy_melee")
    val notbusy_range = hunt("notbusy_range")
    val aggressive_melee = hunt("aggressive_melee")
    val aggressive_melee_extra = hunt("aggressive_melee_extra")
    val aggressive_ranged = hunt("aggressive_ranged")
}
