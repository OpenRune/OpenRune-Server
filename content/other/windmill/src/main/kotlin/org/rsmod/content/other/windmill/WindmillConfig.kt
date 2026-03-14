package org.rsmod.content.other.windmill

import org.rsmod.api.type.refs.loc.LocReferences

internal typealias windmill_locs = WindmillLocs

internal object WindmillLocs : LocReferences() {
    val ladder_up = loc("qip_cook_ladder")
    val ladder_option = loc("qip_cook_ladder_middle")
    val ladder_down = loc("qip_cook_ladder_top")
}
