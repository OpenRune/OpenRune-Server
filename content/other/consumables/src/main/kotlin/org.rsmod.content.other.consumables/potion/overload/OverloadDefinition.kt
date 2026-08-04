package org.rsmod.content.other.consumables.potion.overload

import org.rsmod.api.attr.AttributeKey
import org.rsmod.content.other.consumables.potion.PotionStatBoost

internal data class OverloadState(
    val expiresAt: Int,
    val boost: PotionStatBoost,
    val damageHitsRemaining: Int,
    val warned: Boolean,
)

internal data class OverloadDefinition(
    val effectTimer: String,
    val damageTimer: String,
    val timerVarbit: String,
    val tierVarbit: String?,
    val startClientScript: Int?,
    val defaultBoost: PotionStatBoost?,
    val duration: Int,
    val refreshInterval: Int,
    val warningLead: Int,
    val damageInterval: Int,
    val damageHits: Int,
    val damagePerHit: Int,
    val boostedStats: List<String>,
    val warningMessage: String,
    val expiryMessage: String,
    val animation: String,
    val spotAnimation: String,
    val stateKey: AttributeKey<OverloadState> =
        AttributeKey(
            resetOnDeath = true,
            temp = true,
        ),
) {
    val totalDamage: Int
        get() = damageHits * damagePerHit
}
