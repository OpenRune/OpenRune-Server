package org.alter.plugins.content.skills.woodcutting

import org.alter.game.model.Tile
import org.alter.game.model.World
import org.alter.game.model.entity.Player

/**
 * Manages countdown timers for woodcutting trees.
 *
 * Timer behavior:
 * - Timer starts on first chop attempt (initialized to max)
 * - Counts down 1 tick per game tick while ≥1 player is actively chopping
 * - Regenerates 1 tick per game tick when no one is chopping (up to max)
 * - Multiple players don't speed it up (fixed rate)
 * - When timer reaches 0, tree depletes on next successful log
 *
 * Performance optimizations:
 * - Region-based tracking: Only tracks timers in regions with player activity
 * - Automatic cleanup: Removes timers for inactive regions (no players for 5 minutes)
 * - Lazy initialization: Timers only created when players actually chop
 */
class TreeTimers(private val world: World) {
    /**
     * Data class to track countdown timer per tree.
     * Timer counts down 1 tick per game tick while ≥1 player is chopping.
     * Timer regenerates at same rate when no one is chopping.
     */
    private data class TreeChopTimer(
        var countdown: Int = 0, // Current countdown value (starts at max, counts down to 0)
        var maxCountdown: Int = 0, // Maximum countdown value (despawn time)
        var activeChoppers: MutableSet<Player> = mutableSetOf(), // Players currently chopping this tree
        var lastUpdateCycle: Int = 0 // Last cycle this timer was updated
    )

    /**
     * Region-based map to track countdown timers per tree.
     * Outer key: regionId (Int) - groups trees by region for efficient cleanup
     * Inner key: "x,z,height,objectId" - unique tree identifier
     * Timer starts on first chop, counts down while chopping, regenerates when idle.
     */
    private val treeChopTimersByRegion = mutableMapOf<Int, MutableMap<String, TreeChopTimer>>()

    /**
     * Tracks the last cycle each region had player activity.
     * Used to clean up inactive regions.
     */
    private val regionLastActivity = mutableMapOf<Int, Int>()

    /**
     * Number of cycles (game ticks) before an inactive region is cleaned up.
     * 5 minutes = 500 cycles (100 cycles per minute)
     */
    private val REGION_CLEANUP_CYCLES = 500

    /**
     * Updates the countdown timer for a tree.
     * Counts down 1 tick per game tick while ≥1 player is chopping.
     * Regenerates at same rate when no one is chopping.
     * Timer starts on first chop attempt (initialized to max).
     *
     * @param treeTile The tile where the tree is located (used to determine region)
     * @param treeKey Unique identifier for the tree (e.g., "x,z,height,objectId")
     * @param treeType The type of tree being chopped
     * @param player The player chopping (or stopping chopping)
     * @param isChopping Whether the player is currently chopping
     * @return true if timer has reached 0 (tree should deplete on next log)
     */
    fun updateTreeTimer(
        treeTile: Tile,
        treeKey: String,
        treeType: WoodcuttingDefinitions.TreeType,
        player: Player,
        isChopping: Boolean
    ): Boolean {
        val regionId = treeTile.regionId
        val maxCountdown = when (val mechanic = treeType.depleteMechanic) {
            is WoodcuttingDefinitions.DepleteMechanic.Countdown -> mechanic.despawnTicks
            else -> 0 // Regular trees don't use countdown
        }

        // Regular trees don't use countdown timers
        if (maxCountdown == 0) {
            return false
        }

        // Mark region as active (has player activity)
        regionLastActivity[regionId] = world.currentCycle

        // Get or create region's timer map
        val regionTimers = treeChopTimersByRegion.getOrPut(regionId) { mutableMapOf() }

        // Only create timer when player starts chopping
        // If tree doesn't have a timer, it's fresh/maxed
        val timer = if (isChopping) {
            regionTimers.getOrPut(treeKey) {
                // Timer starts on first chop attempt (initialized to max)
                TreeChopTimer(countdown = maxCountdown, maxCountdown = maxCountdown, lastUpdateCycle = world.currentCycle)
            }
        } else {
            // When not chopping, check if timer exists (might be removing player from active choppers)
            regionTimers[treeKey]
        }

        // If no timer exists (tree is fresh/maxed), return false (no depletion)
        if (timer == null) {
            return false
        }

        val currentCycle = world.currentCycle
        val cyclesSinceUpdate = currentCycle - timer.lastUpdateCycle

        // Update active choppers
        if (isChopping) {
            timer.activeChoppers.add(player)
        } else {
            timer.activeChoppers.remove(player)
        }

        // Update countdown based on whether anyone is chopping
        // Counts down 1 tick per game tick while chopping, regenerates 1 tick per game tick when idle
        if (cyclesSinceUpdate > 0) {
            if (timer.activeChoppers.isNotEmpty()) {
                // Count down while chopping (1 tick per game tick)
                timer.countdown = (timer.countdown - cyclesSinceUpdate).coerceAtLeast(0)
            } else {
                // Regenerate when idle (1 tick per game tick, up to max)
                timer.countdown = (timer.countdown + cyclesSinceUpdate).coerceAtMost(timer.maxCountdown)
            }
            timer.lastUpdateCycle = currentCycle
        }

        // Remove timer from tracking if it has fully regenerated (reached max) and no one is chopping
        // This way, if a tree doesn't have a timer, we know it's fresh/maxed
        if (timer.countdown >= timer.maxCountdown && timer.activeChoppers.isEmpty()) {
            regionTimers.remove(treeKey)
            // Clean up empty region maps
            if (regionTimers.isEmpty()) {
                treeChopTimersByRegion.remove(regionId)
                regionLastActivity.remove(regionId)
            }
            return false // Tree is fresh, no depletion
        }

        // Return true if timer reached 0 (tree should deplete on next log)
        return timer.countdown <= 0
    }

    /**
     * Cleans up timers for inactive regions (no player activity for REGION_CLEANUP_CYCLES).
     * Should be called periodically (e.g., every 100 cycles) to prevent memory leaks.
     */
    fun cleanupInactiveRegions() {
        val currentCycle = world.currentCycle
        val regionsToCleanup = mutableListOf<Int>()

        // Find regions that haven't had activity for REGION_CLEANUP_CYCLES
        regionLastActivity.forEach { (regionId, lastActivity) ->
            if (currentCycle - lastActivity > REGION_CLEANUP_CYCLES) {
                // Check if region has any players nearby
                val hasPlayers = world.players.any { player ->
                    player.tile.regionId == regionId
                }

                // Only cleanup if no players are in the region
                if (!hasPlayers) {
                    regionsToCleanup.add(regionId)
                }
            }
        }

        // Remove inactive regions
        regionsToCleanup.forEach { regionId ->
            treeChopTimersByRegion.remove(regionId)
            regionLastActivity.remove(regionId)
        }
    }

    /**
     * Clears the countdown timer for a tree (e.g., when tree is depleted).
     *
     * @param treeTile The tile where the tree is located (used to determine region)
     * @param treeKey Unique identifier for the tree
     */
    fun clearTimer(treeTile: Tile, treeKey: String) {
        val regionId = treeTile.regionId
        val regionTimers = treeChopTimersByRegion[regionId]
        regionTimers?.remove(treeKey)

        // Clean up empty region maps
        if (regionTimers?.isEmpty() == true) {
            treeChopTimersByRegion.remove(regionId)
            regionLastActivity.remove(regionId)
        }
    }

    /**
     * Generates a unique tree key from tree coordinates and object ID.
     *
     * @param x Tree X coordinate
     * @param z Tree Z coordinate
     * @param height Tree height
     * @param objectId Tree object ID (RSCM identifier string)
     * @return Unique tree key string
     */
    fun generateTreeKey(x: Int, z: Int, height: Int, objectId: String): String {
        return "$x,$z,$height,$objectId"
    }
}

