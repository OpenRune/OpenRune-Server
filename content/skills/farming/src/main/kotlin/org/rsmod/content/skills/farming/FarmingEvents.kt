package org.rsmod.content.skills.farming

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

data class FarmingPlantEvent(val player: Player, val patch: PatchDef, val crop: Crop) : UnboundEvent

data class FarmingCheckHealthEvent(val player: Player, val patch: PatchDef, val crop: Crop) :
    UnboundEvent

/**
 * One committed harvest action. [final] marks harvest exhaustion: most crops leave empty soil,
 * grape vines leave a dead vine. Renewable crops that keep producing never publish a final.
 */
data class FarmingHarvestEvent(
    val player: Player,
    val patch: PatchDef,
    val crop: Crop,
    val obj: String,
    val count: Int,
    val final: Boolean,
) : UnboundEvent

data class FarmingClearEvent(val player: Player, val patch: PatchDef, val crop: Crop?, val dead: Boolean) :
    UnboundEvent

data class FarmingRefreshEvent(
    val player: Player,
    val before: Array<PatchState>,
    val after: Array<PatchState>,
) : UnboundEvent

data class FarmingPatchChangeEvent(
    val player: Player,
    val patch: PatchDef,
    val before: PatchState,
    val after: PatchState,
) : UnboundEvent

enum class CureResult {
    STALE,
    EMPTY,
    NOT_DISEASED,
    DEAD,
    PRUNE_ONLY,
    UNPAID,
    CURED,
}

enum class TreatResult {
    STALE,
    WEEDS,
    DEAD,
    GROWN,
    ALREADY,
    UNPAID,
    TREATED,
}

enum class ResurrectResult {
    STALE,
    NOT_DEAD,
    EXCLUDED,
    ALREADY_RESURRECTED,
    UNPAID,
    RESURRECTED,
    CLEARED,
}

enum class ProtectResult {
    STALE,
    EMPTY,
    DEAD,
    DISEASED,
    GROWN,
    ALREADY,
    UNPAID,
    PROTECTED,
}
