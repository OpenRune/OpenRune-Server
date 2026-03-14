package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.area.AreaReferences

typealias areas = BaseAreas

object BaseAreas : AreaReferences() {
    val lumbridge = area("lumbridge")
    val singles_plus = area("singles_plus")
    val multiway = area("multiway")
}
