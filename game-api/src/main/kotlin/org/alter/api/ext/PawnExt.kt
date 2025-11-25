package org.alter.api.ext

import dev.openrune.ServerCacheManager.getAnim
import org.alter.api.BonusSlot
import org.alter.api.HitType
import org.alter.api.HitbarType
import org.alter.api.PrayerIcon
import org.alter.game.model.Hit
import org.alter.game.model.attr.*
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.move.stopMovement
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.FROZEN_TIMER
import org.alter.game.model.timer.STUN_TIMER
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.asRSCM
import org.alter.rscm.RSCM.getRSCM

fun Pawn.prepareForTeleport() {
    resetInteractions()
    clearHits()
}

fun Pawn.getCommandArgs(): Array<String> = attr[COMMAND_ARGS_ATTR]!!

fun Pawn.getInteractingSlot(): Int = attr[INTERACTING_SLOT_ATTR]!!

fun Pawn.getInteractingItem(): Item = attr[INTERACTING_ITEM]!!.get()!!

fun Pawn.getInteractingItemId(): Int = attr[INTERACTING_ITEM_ID]!!

fun Pawn.getInteractingItemSlot(): Int = attr[INTERACTING_ITEM_SLOT]!!

fun Pawn.getInteractingOption(): Int = attr[INTERACTING_OPT_ATTR]!!

fun Pawn.getInteractingGameObj(): GameObject = attr[INTERACTING_OBJ_ATTR]!!.get()!!

fun Pawn.getInteractingNpc(): Npc = attr[INTERACTING_NPC_ATTR]!!.get()!!

fun Pawn.getInteractingPlayer(): Player = attr[INTERACTING_PLAYER_ATTR]!!.get()!!

fun Pawn.hasPrayerIcon(icon: PrayerIcon): Boolean = prayerIcon == icon.id

fun Pawn.getBonus(slot: BonusSlot): Int = equipmentBonuses[slot.id]

fun Pawn.hit(
    damage: Int,
    type: HitType = if (damage == 0) HitType.BLOCK else HitType.HIT,
    delay: Int = 0,
    attackersIndex: Int = -1
): Hit {
    val hit =
        Hit.Builder()
            .setDamageDelay(delay)
            .addHit(damage = damage, type = type.id, attackersIndex)
            .setHitbarMaxPercentage(HitbarType.NORMAL.pixelsWide)
            .setAttackerIndex(attackersIndex)
            .build()
    addHit(hit)
    return hit
}

fun Pawn.freeze(
    cycles: Int,
    onFreeze: () -> Unit,
) {
    if (timers.has(FROZEN_TIMER)) {
        return
    }
    stopMovement()
    timers[FROZEN_TIMER] = cycles
    onFreeze()
}

fun Pawn.freeze(cycles: Int) {
    freeze(cycles) {
        if (this is Player) {
            this.message("You have been frozen.")
        }
    }
}

fun Pawn.stun(
    cycles: Int,
    onStun: () -> Unit,
): Boolean {
    if (timers.has(STUN_TIMER)) {
        return false
    }
    stopMovement()
    timers[STUN_TIMER] = cycles
    onStun()
    return true
}

fun Pawn.stun(cycles: Int) {
    stun(cycles) {
        if (this is Player) {
            graphic("spotanims.stunned_thieving", 124)
            resetInteractions()
            interruptQueues()
            message("You have been stunned!")
        }
    }
}

/**
 * Starts looping an animation continuously.
 * The animation will restart automatically when it finishes.
 *
 * @param animId The RSCM animation identifier (e.g., "sequences.human_chop")
 * @param interruptable If true, the animation can be interrupted by other animations or actions
 */
fun Pawn.loopAnim(animId: String, interruptable: Boolean = false) {
    stopLoopAnim()

    // animationLength is already in cycles (from seq.lengthInCycles)
    val animationDuration = getAnim(animId.asRSCM())?.animationLength ?: 60

    attr[LOOPING_ANIMATION_ATTR] = LoopingAnimationData(
        animId = animId,
        duration = animationDuration,
        currentTick = animationDuration, // Start at duration so first tick increments and restarts
        interruptable = interruptable
    )

    animate(animId, interruptable = interruptable)
}

/**
 * Stops looping an animation.
 *
 * @param allowFinish If true, stops looping but allows the current animation to finish naturally.
 *                    If false (default), immediately cancels the animation.
 */
fun Pawn.stopLoopAnim(allowFinish: Boolean = false) {
    attr.remove(LOOPING_ANIMATION_ATTR)
    if (!allowFinish) {
        animate(RSCM.NONE)
    }
}
