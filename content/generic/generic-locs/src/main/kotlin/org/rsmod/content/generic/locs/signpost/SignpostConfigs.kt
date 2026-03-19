package org.rsmod.content.generic.locs.signpost

import dev.openrune.component
import dev.openrune.inter
import dev.openrune.loc
import dev.openrune.types.enums.enum
import org.rsmod.map.CoordGrid

internal typealias signpost_locs = SignpostLocs

internal typealias signpost_interfaces = SignpostInterfaces

internal typealias signpost_components = SignpostComponents

internal object SignpostLocs {
    val signpost = loc("aide_signpost_1")
}

internal object SignpostInterfaces {
    val signpost = inter("aide_compass")
}

internal object SignpostComponents {
    val signpost_north = component("aide_compass:aide_north_text_2")
    val signpost_east = component("aide_compass:aide_east_text_2")
    val signpost_south = component("aide_compass:aide_south_text_2")
    val signpost_west = component("aide_compass:aide_west_text_2")
}

internal object SignpostEnums {
    val signpost_directions = enum<CoordGrid, String>("signpost_directions")
}
