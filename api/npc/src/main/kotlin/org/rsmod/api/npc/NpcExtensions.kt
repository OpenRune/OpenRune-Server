package org.rsmod.api.npc

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.headbar.InternalNpcHeadbars
import org.rsmod.game.entity.Npc
import org.rsmod.game.hit.Hitmark

public fun Npc.clearInteractionRoute() {
    clearInteraction()
    abortRoute()
}

public fun Npc.queueDeath() {
    queue("queue.death", 1)
}

public fun Npc.isValidTarget(): Boolean {
    return isSlotAssigned && isVisible && isNotDelayed && hitpoints > 0
}

/**
 * Restores the npc's [Npc.hitpoints] by [amount], capped at its max ([Npc.baseHitpointsLvl]).
 *
 * @param amount the number of hitpoints to restore; must not be negative.
 * @param showHitsplat when `true`, displays a heal hitsplat with the amount actually healed and
 *   updates the npc's headbar to reflect the new hitpoints. No hitsplat is shown when nothing was
 *   healed (already at max).
 * @return the amount actually healed after capping at the npc's max hitpoints.
 */
public fun Npc.heal(amount: Int, showHitsplat: Boolean = false): Int {
    require(amount >= 0) { "Heal amount `$amount` must not be negative." }
    val before = hitpoints
    hitpoints = (hitpoints + amount).coerceAtMost(baseHitpointsLvl)
    val healed = hitpoints - before
    if (showHitsplat && healed > 0) {
        val hitmarkId = hitmark_groups.heal.lit.asRSCM(RSCMType.HITMARK)
        val hitmark =
            Hitmark.fromNoSource(
                self = hitmarkId,
                source = hitmarkId,
                public = hitmarkId,
                damage = healed,
                delay = 0,
            )
        showHitmark(hitmark)
        val headbar =
            InternalNpcHeadbars.createFromHitmark(
                hitmark,
                hitpoints,
                baseHitpointsLvl,
                visHeadbar(params.headbar),
            )
        showHeadbar(headbar)
    }
    return healed
}

public fun Npc.isOutOfCombat(): Boolean = !isInCombat()

public fun Npc.isInCombat(): Boolean {
    if (vars["varn.lastattack"] + constants.combat_activecombat_delay >= currentMapClock) {
        return true
    }
    return mode == NpcMode.OpPlayer2 || mode == NpcMode.ApPlayer2 || mode == NpcMode.PlayerEscape
}

/** @return `true` if the npc is **currently** in a multi-combat area. */
public fun Npc.mapMultiway(checker: AreaChecker): Boolean {
    return checker.inArea("area.multiway", coords)
}
