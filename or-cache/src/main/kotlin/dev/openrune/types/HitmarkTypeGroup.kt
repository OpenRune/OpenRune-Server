package dev.openrune.types

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType

public data class HitmarkTypeGroup(
    val lit: String,
    val tint: String? = null,
    val max: String? = null,
) {
    public fun isAssociatedWith(other: HitmarkTypeGroup): Boolean =
        lit.asRSCM(RSCMType.HITMARK) == other.lit.asRSCM(RSCMType.HITMARK)
            && tint?.asRSCM(RSCMType.HITMARK) == other.tint?.asRSCM(RSCMType.HITMARK)
            && max?.asRSCM(RSCMType.HITMARK) == other.max?.asRSCM(RSCMType.HITMARK)
}
