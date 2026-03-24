package org.alter.skills.fishing

import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.impl.NpcClickEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType
import org.generated.tables.fishing.FishingSpotsRow
import org.generated.tables.fishing.FishingToolsRow

/**
 * Core Fishing skill plugin.
 *
 * Fishing spots are NPCs in OSRS. Players interact with them via menu options
 * like "Net", "Bait", "Lure", "Cage", and "Harpoon". Each option maps to a
 * spot type string that determines which fish can be caught and which tool is
 * required.
 */
class FishingPlugin : PluginEvent() {

    companion object {
        private val logger = KotlinLogging.logger {}

        /**
         * All fishing spot rows loaded from the generated table.
         */
        lateinit var allSpots: List<FishingSpotsRow>
            private set

        /**
         * All fishing tool rows loaded from the generated table.
         */
        lateinit var allTools: List<FishingToolsRow>
            private set

        /**
         * Spots grouped by their spot type (e.g. "net", "bait", "lure", "cage", "harpoon").
         * Within each group, spots are sorted by level descending so the highest-level
         * fish is attempted first.
         */
        lateinit var spotsByType: Map<String, List<FishingSpotsRow>>
            private set

        /**
         * Tools grouped by their tool type string.
         */
        lateinit var toolsByType: Map<String, List<FishingToolsRow>>
            private set

        /**
         * Maps an NPC menu option to the corresponding spot type string.
         */
        val OPTION_TO_SPOT_TYPE: Map<String, String> = mapOf(
            "net" to "net",
            "bait" to "bait",
            "lure" to "lure",
            "cage" to "cage",
            "harpoon" to "harpoon",
            "use-rod" to "bait",
            "big net" to "big_net",
            "fish" to "vessel",
        )

        /**
         * All NPC menu option names that trigger fishing.
         */
        val FISHING_OPTIONS: Set<String> = OPTION_TO_SPOT_TYPE.keys
    }

    override fun init() {
        allSpots = FishingSpotsRow.all()
        allTools = FishingToolsRow.all()

        spotsByType = allSpots.groupBy { it.spotType }
            .mapValues { (_, spots) -> spots.sortedByDescending { it.level } }

        toolsByType = allTools.groupBy { it.toolType }

        logger.info { "Loaded ${allSpots.size} fishing spots and ${allTools.size} fishing tools." }

        // Register a single NpcClickEvent listener that matches any fishing-related
        // menu option. This avoids needing individual NPC RSCM IDs for every fishing
        // spot NPC variant, since OSRS has many fishing spot NPC IDs that share the
        // same menu options.
        on<NpcClickEvent> {
            where {
                FISHING_OPTIONS.any { opt -> opt.equals(optionName, ignoreCase = true) }
            }
            then {
                val spotType = OPTION_TO_SPOT_TYPE.entries
                    .firstOrNull { it.key.equals(optionName, ignoreCase = true) }
                    ?.value ?: return@then

                player.queue { fishLoop(player, npc, spotType) }
            }
        }
    }

    /**
     * Finds the best fishing tool the player has for the given spot type.
     * Checks equipped weapon first, then inventory. Prefers tools with the
     * lowest speed modifier (fastest).
     */
    private fun findBestTool(player: Player, spotType: String): FishingToolsRow? {
        val candidates = toolsByType[spotType] ?: return null

        // Check equipped weapon first
        player.equipment[EquipmentType.WEAPON.id]?.let { weapon ->
            candidates.find { it.toolItem == weapon.id }?.let { return it }
        }

        // Check inventory, prefer the tool with the best (lowest) speed modifier
        return player.inventory.asSequence()
            .filterNotNull()
            .mapNotNull { invItem -> candidates.find { it.toolItem == invItem.id } }
            .minByOrNull { it.toolSpeedMod }
    }

    /**
     * Returns true if the player has the required tool anywhere (equipped or inventory).
     */
    private fun hasTool(player: Player, spotType: String): Boolean {
        val candidates = toolsByType[spotType] ?: return false
        player.equipment[EquipmentType.WEAPON.id]?.let { weapon ->
            if (candidates.any { it.toolItem == weapon.id }) return true
        }
        return player.inventory.asSequence()
            .filterNotNull()
            .any { invItem -> candidates.any { it.toolItem == invItem.id } }
    }

    /**
     * Returns true if the player has the required bait for a given fish spot,
     * or if the spot does not require bait.
     */
    private fun hasBait(player: Player, spot: FishingSpotsRow): Boolean {
        val bait = spot.bait ?: return true
        return player.inventory.contains(bait)
    }

    /**
     * Consumes one unit of bait from the player's inventory, if the spot requires bait.
     */
    private fun consumeBait(player: Player, spot: FishingSpotsRow) {
        val bait = spot.bait ?: return
        player.inventory.remove(bait, 1)
    }

    /**
     * Main fishing loop. Runs as a queued task on the player.
     */
    private suspend fun QueueTask.fishLoop(
        player: Player,
        spotNpc: Npc,
        spotType: String,
    ) {
        val spots = spotsByType[spotType]
        if (spots.isNullOrEmpty()) {
            player.message("Nothing interesting happens.")
            return
        }

        val fishingLevel = player.getSkills().getBaseLevel(Skills.FISHING)

        // Find the fish the player can catch at their level for this spot type
        val catchableSpots = spots.filter { fishingLevel >= it.level }
        if (catchableSpots.isEmpty()) {
            val lowestReq = spots.minOf { it.level }
            player.message("You need a Fishing level of at least $lowestReq to fish here.")
            return
        }

        // Validate tool
        if (!hasTool(player, spotType)) {
            player.message("You need a suitable tool to fish here.")
            return
        }

        val tool = findBestTool(player, spotType)
        if (tool == null) {
            player.message("You do not have a tool which you have the level to use.")
            return
        }

        // Check bait — any spot of this type that requires bait, the player must have it
        val baitSpot = catchableSpots.firstOrNull { it.bait != null }
        if (baitSpot != null && !hasBait(player, baitSpot)) {
            val baitName = try {
                Item(baitSpot.bait!!).getName().lowercase()
            } catch (_: Exception) {
                "bait"
            }
            player.message("You don't have any $baitName.")
            return
        }

        if (player.inventory.isFull()) {
            player.message("Your inventory is too full to hold any more fish.")
            return
        }

        // Resolve the animation RSCM string from the tool's animation ID
        val animRscm = RSCM.getReverseMapping(RSCMType.SEQTYPES, tool.toolAnimation)
        if (animRscm == null) {
            logger.warn { "Could not resolve animation RSCM for tool animation id=${tool.toolAnimation}" }
            return
        }

        player.message("You cast out your ${toolDisplayName(tool)}.")
        player.loopAnim(animRscm)

        repeatWhile(
            delay = 5,
            immediate = false,
            canRepeat = {
                !player.inventory.isFull()
                    && spotNpc.isSpawned()
                    && hasTool(player, spotType)
                    && (baitSpot == null || hasBait(player, baitSpot))
            }
        ) {
            // Re-check catchable spots each tick (level could theoretically change)
            val currentLevel = player.getSkills().getBaseLevel(Skills.FISHING)
            val available = spots.filter { currentLevel >= it.level }
            if (available.isEmpty()) return@repeatWhile

            // Try highest-level fish first
            for (spot in available) {
                // If this spot requires bait, check we still have it
                if (spot.bait != null && !hasBait(player, spot)) continue

                val caught = success(spot.catchRateLow, spot.catchRateHigh, currentLevel)
                if (caught) {
                    // Consume bait before giving fish
                    consumeBait(player, spot)

                    val addResult = player.inventory.add(spot.fishItem, 1)
                    if (!addResult.hasSucceeded()) {
                        player.message("Your inventory is too full to hold any more fish.")
                        player.stopLoopAnim()
                        return@repeatWhile
                    }

                    val xp = spot.xp.toDouble()
                    player.addXp(Skills.FISHING, xp)

                    val fishName = try {
                        Item(spot.fishItem).getName().lowercase()
                    } catch (_: Exception) {
                        "fish"
                    }
                    player.message("You catch some $fishName.")

                    // Fire the FishObtainedEvent for enhancers and cross-cutting systems
                    FishObtainedEvent(
                        player = player,
                        spotNpc = spotNpc,
                        fishItem = spot.fishItem,
                        xp = xp
                    ).post()

                    // Only catch one fish per tick
                    break
                }
            }
        }

        player.stopLoopAnim()
    }

    /**
     * Returns a display-friendly tool name for chat messages.
     */
    private fun toolDisplayName(tool: FishingToolsRow): String {
        return try {
            Item(tool.toolItem).getName().lowercase()
        } catch (_: Exception) {
            "fishing tool"
        }
    }
}
