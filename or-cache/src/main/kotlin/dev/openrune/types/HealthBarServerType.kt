package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("health")
class HealthBarServerType(override var id: Int = -1, segments: Int = 30) : Definition {
    // The codec stores this as a signed byte, so widths above 127 come back negative.
    var segments: Int = segments and SEGMENTS_MASK
        set(value) {
            field = value and SEGMENTS_MASK
        }

    override fun equals(other: Any?): Boolean =
        other is HealthBarServerType && other.id == id && other.segments == segments

    override fun hashCode(): Int = 31 * id + segments

    override fun toString(): String = "HealthBarServerType(id=$id, segments=$segments)"

    private companion object {
        const val SEGMENTS_MASK = 0xFF
    }
}
