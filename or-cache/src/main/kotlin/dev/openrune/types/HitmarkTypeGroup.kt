package dev.openrune.types

import dev.openrune.definition.type.HitSplatType

public data class HitmarkTypeGroup(
    val lit: HitSplatType,
    val tint: HitSplatType? = null,
    val max: HitSplatType? = null,
) {
    public fun isAssociatedWith(other: HitmarkTypeGroup): Boolean =
        lit.id == other.lit.id && tint?.id == other.tint?.id && max?.id == other.max?.id
}
