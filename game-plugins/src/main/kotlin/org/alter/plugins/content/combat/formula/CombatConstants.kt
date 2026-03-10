package org.alter.plugins.content.combat.formula

import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * Shared constants and utility functions for combat formulas.
 */
object CombatConstants {

    val BLACK_MASKS = arrayOf(
        "items.harmless_black_mask",
        "items.harmless_black_mask_1", "items.harmless_black_mask_2", "items.harmless_black_mask_3",
        "items.harmless_black_mask_4", "items.harmless_black_mask_5", "items.harmless_black_mask_6",
        "items.harmless_black_mask_7", "items.harmless_black_mask_8", "items.harmless_black_mask_9",
        "items.harmless_black_mask_10"
    )

    val BLACK_MASKS_I = arrayOf(
        "items.nzone_black_mask",
        "items.nzone_black_mask_1", "items.nzone_black_mask_2", "items.nzone_black_mask_3",
        "items.nzone_black_mask_4", "items.nzone_black_mask_5", "items.nzone_black_mask_6",
        "items.nzone_black_mask_7", "items.nzone_black_mask_8", "items.nzone_black_mask_9",
        "items.nzone_black_mask_10"
    )

    val SLAYER_HELM_I = arrayOf(
        "items.slayer_helm_i",
        "items.slayer_helm_i_black",
        "items.slayer_helm_i_green",
        "items.slayer_helm_i_purple",
        "items.slayer_helm_i_red",
        "items.slayer_helm_i_turquoise"
    )

    /**
     * Check if black mask/slayer helmet bonus should apply.
     * Bonus only applies when player is on a slayer task for the target NPC.
     *
     * TODO: Implement slayer task system check:
     * 1. Player has an active slayer task
     * 2. Target NPC matches the slayer task (exact match or category match)
     * 3. Task is not completed
     */
    fun hasSlayerTaskBonus(player: Player, target: Pawn): Boolean {
        return false
    }
}
