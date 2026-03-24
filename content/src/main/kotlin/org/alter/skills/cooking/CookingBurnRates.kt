package org.alter.skills.cooking

import org.alter.api.Skills
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.asRSCM

/**
 * Calculates burn chances for cooking.
 *
 * Factors:
 * - Cooking level vs burn stop level
 * - Fire vs range (ranges have lower burn stop levels)
 * - Cooking gauntlets (reduce burn stop for specific fish)
 * - Cooking cape (never burn)
 * - Lumbridge range bonus (post-Cook's Assistant)
 */
object CookingBurnRates {

    // Cooking gauntlet overrides: raw item ID -> adjusted burn stop level on range
    // These are the levels at which you stop burning WITH gauntlets equipped
    private val GAUNTLET_OVERRIDES: Map<Int, Int> by lazy {
        mapOf(
            "items.raw_lobster".asRSCM() to 64,
            "items.raw_swordfish".asRSCM() to 81,
            "items.raw_monkfish".asRSCM() to 87,
            "items.raw_shark".asRSCM() to 94,
        )
    }

    // Lumbridge castle kitchen range tile area
    private val LUMBRIDGE_RANGE_X = 3208..3212
    private val LUMBRIDGE_RANGE_Z = 3212..3216

    /**
     * Returns true if the food should burn.
     *
     * @param player The player cooking
     * @param rawItem The raw item ID (used for gauntlet overrides)
     * @param burnStopFire The level at which burning stops on fire
     * @param burnStopRange The level at which burning stops on range
     * @param isFire True if cooking on a campfire, false if on a range
     */
    fun shouldBurn(
        player: Player,
        rawItem: Int,
        burnStopFire: Int,
        burnStopRange: Int,
        isFire: Boolean,
    ): Boolean {
        // Cooking cape = never burn
        if (hasCookingCape(player)) return false

        val baseBurnStop = if (isFire) burnStopFire else burnStopRange

        // Apply cooking gauntlets override (only if it gives a better burn stop)
        val gauntletStop = if (hasGauntlets(player)) GAUNTLET_OVERRIDES[rawItem] else null
        val effectiveBurnStop = if (gauntletStop != null && gauntletStop < baseBurnStop) {
            gauntletStop
        } else {
            baseBurnStop
        }

        val level = player.getSkills().getCurrentLevel(Skills.COOKING)

        // Already past burn stop level
        if (level >= effectiveBurnStop) return false

        // Linear burn chance
        var burnChance = (effectiveBurnStop - level).toDouble() / effectiveBurnStop.toDouble()

        // Lumbridge range bonus: 5% reduction
        if (!isFire && isLumbridgeRange(player)) {
            burnChance *= 0.95
        }

        return Math.random() < burnChance
    }

    private fun hasCookingCape(player: Player): Boolean {
        return player.equipment.containsAny(
            "items.skillcape_cooking",
            "items.skillcape_cooking_trimmed",
            "items.skillcape_max",
        )
    }

    private fun hasGauntlets(player: Player): Boolean {
        return player.equipment.contains("items.gauntlets_of_cooking")
    }

    private fun isLumbridgeRange(player: Player): Boolean {
        val tile = player.tile
        return tile.x in LUMBRIDGE_RANGE_X && tile.z in LUMBRIDGE_RANGE_Z
    }
}
