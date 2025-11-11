package org.alter.plugins.content.skills.woodcutting

import org.alter.api.*
import org.alter.api.success
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.Tile
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.ObjectTimerMap
import org.alter.game.model.entity.Player
import org.alter.game.model.entity.Npc
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.event.impl.TreeDepleteEvent
import org.alter.plugins.content.skills.woodcutting.TreeDepleteHandler
import org.alter.plugins.content.skills.woodcutting.handlers.BlisterwoodTreeDepleteHandler
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCMType
import kotlin.random.Random
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * TODO: Remaining features for Woodcutting to be feature-complete:
 * - Bird nests (random drops while chopping)
 * - Forestry system (group bonuses, events like Rising Roots)
 * - Canoes (travel system)
 * - Woodcutting Guild (level 60+ area with bonuses)
 * - Woodcutting pet (Beaver)
 * - Temporary boosts (dragon axe special, stews, etc.)
 * - Additional tree types (redwood, jungle, etc.)
 *
 * Reference: https://oldschool.runescape.wiki/w/Woodcutting
 */
class WoodcuttingPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        private val logger = KotlinLogging.logger {}

        val CHOP_SOUND = 2053
        val LOG_OBTAINED_SOUND = 2734
        val AXE_ANIMATION_DURATION = 3
    }

    /**
     * Manages countdown timers for all trees.
     */
    private val treeTimers = TreeTimers(world)

    /**
     * Registry of tree deplete handlers.
     * Each tree type can have a custom handler that defines what happens when it depletes.
     */
    private val treeDepleteHandlers: MutableMap<WoodcuttingDefinitions.TreeType, TreeDepleteHandler> = mutableMapOf()

    /**
     * Registers a tree deplete handler for a specific tree type.
     * This allows custom behavior when a tree depletes (e.g., spawning NPCs, special messages, etc.).
     *
     * @param treeType The tree type this handler is for
     * @param handler The handler to register
     */
    fun onDeplete(treeType: WoodcuttingDefinitions.TreeType, handler: TreeDepleteHandler) {
        require(handler.treeType == treeType) { "Handler tree type (${handler.treeType}) must match provided tree type ($treeType)" }
        treeDepleteHandlers[treeType] = handler
    }

    init {
        registerTreeHandlers()
        registerDepleteHandlers()
        startCleanupTask()
    }

    /**
     * Starts a periodic cleanup task to remove timers from inactive regions.
     * Runs every 100 cycles (1 minute) to prevent memory leaks.
     */
    private fun startCleanupTask() {
        // Schedule recurring cleanup task
        world.queue {
            while (true) {
                wait(100) // Wait 100 cycles (1 minute)
                treeTimers.cleanupInactiveRegions()
            }
        }
    }

    /**
     * Registers default tree deplete handlers.
     */
    private fun registerDepleteHandlers() {
        onDeplete(WoodcuttingDefinitions.TreeType.BLISTERWOOD, BlisterwoodTreeDepleteHandler())
    }

    /**
     * Registers chop handlers for all tree variants.
     * Uses RSCM identifiers exclusively.
     */
    private fun registerTreeHandlers() {
        WoodcuttingDefinitions.TREE_RSCM_TO_TYPE.forEach { (treeRscm, _) ->
            try {
                val treeId = getRSCM(treeRscm)
                if (objHasOption(obj = treeRscm, option = "chop down")) {
                    onObjOption(obj = treeId, option = "chop down") {
                        player.queue { chopTree(player, treeRscm) }
                    }
                } else if (objHasOption(obj = treeRscm, option = "chop")) {
                  onObjOption(obj = treeId, option = "chop") {
                      player.queue { chopTree(player, treeRscm) }
                  }
              }
            } catch (e: IllegalStateException) {
                logger.warn { "Tree object '$treeRscm' not found in cache, skipping registration" }
            }
        }
    }

    /**
     * Gets the tree type for a given tree RSCM identifier.
     * Returns null if the tree is not recognized.
     */
    private fun getTreeTypeForRscm(treeRscm: String): WoodcuttingDefinitions.TreeType? {
        return WoodcuttingDefinitions.TREE_RSCM_TO_TYPE[treeRscm]
    }

    /**
     * Gets the stump RSCM identifier for a given tree type.
     * Returns null if no stump mapping exists.
     */
    private fun getStumpRscmForTreeType(treeType: WoodcuttingDefinitions.TreeType): String? {
        return WoodcuttingDefinitions.TREE_TYPE_TO_STUMP_RSCM[treeType]
    }

    /**
     * Gets the stump RSCM identifier for a given tree RSCM identifier.
     * This is a convenience function that combines tree type lookup and stump mapping.
     * Regular and dead trees have specific stump mappings that take precedence over tree type mappings.
     */
    private fun getStumpRscmForTreeRscm(treeRscm: String): String? {
        // Check for regular tree specific stump mapping first
        WoodcuttingDefinitions.REGULAR_TREE_TO_STUMP_RSCM[treeRscm]?.let { return it }

        // Check for dead tree specific stump mapping
        WoodcuttingDefinitions.DEAD_TREE_TO_STUMP_RSCM[treeRscm]?.let { return it }

        // Fall back to tree type mapping
        val treeType = getTreeTypeForRscm(treeRscm) ?: return null
        return getStumpRscmForTreeType(treeType)
    }

    /**
     * Checks if the player has any axe available (equipped or in inventory).
     */
    private fun hasAnyAxe(player: Player): Boolean {
        val equippedWeapon = player.equipment[EquipmentType.WEAPON.id]
        equippedWeapon?.let {
            val axeId = getAxeIdentifier(it.id)
            if (axeId != null && WoodcuttingDefinitions.AXE_DATA.containsKey(axeId)) {
                return true
            }
        }

        for (i in 0 until player.inventory.capacity) {
            val item = player.inventory[i] ?: continue
            val axeId = getAxeIdentifier(item.id)
            if (axeId != null && WoodcuttingDefinitions.AXE_DATA.containsKey(axeId)) {
                return true
            }
        }

        return false
    }

    /**
     * Gets the best axe the player can use based on their woodcutting level.
     * Checks both equipped weapon and inventory.
     */
    private fun getBestAxe(player: Player): String? {
        val wcLevel = player.getSkills().getBaseLevel(Skills.WOODCUTTING)
        val equippedWeapon = player.equipment[EquipmentType.WEAPON.id]
        equippedWeapon?.let {
            val axeId = getAxeIdentifier(it.id)
            if (axeId != null && WoodcuttingDefinitions.AXE_DATA.containsKey(axeId)) {
                val axeData = WoodcuttingDefinitions.AXE_DATA[axeId] ?: return@let
                if (wcLevel >= axeData.levelReq) {
                    return axeId
                }
            }
        }

        var bestAxe: String? = null
        var bestLevel = -1

        for (i in 0 until player.inventory.capacity) {
            val item = player.inventory[i] ?: continue
            val axeId = getAxeIdentifier(item.id)
            if (axeId != null && WoodcuttingDefinitions.AXE_DATA.containsKey(axeId)) {
                val axeData = WoodcuttingDefinitions.AXE_DATA[axeId] ?: continue
                if (axeData.levelReq > bestLevel && wcLevel >= axeData.levelReq) {
                    bestLevel = axeData.levelReq
                    bestAxe = axeId
                }
            }
        }

        return bestAxe
    }

    /**
     * Gets the RSCM identifier for an axe item ID.
     * Returns null if the item is not an axe.
     */
    private fun getAxeIdentifier(itemId: Int): String? {
        return try {
            val axeId = RSCM.getReverseMapping(RSCMType.OBJTYPES, itemId)?.lowercase() ?: return null
            if (WoodcuttingDefinitions.AXE_DATA.containsKey(axeId)) {
                axeId
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the object is a stump by comparing its transform to the expected stump RSCM.
     */
    private fun isStump(obj: GameObject, treeRscm: String, player: Player): Boolean {
        val stumpRscm = getStumpRscmForTreeRscm(treeRscm) ?: return false
        val stumpId = try {
            getRSCM(stumpRscm)
        } catch (e: Exception) {
            return false
        }
        return obj.getTransform(player) == stumpId
    }

    /**
     * Handles when a player successfully obtains a log from a tree.
     * Adds the log to inventory, gives XP, and shows a message.
     * Returns true if log was successfully added, false if inventory is full.
     */
    private suspend fun QueueTask.handleLogObtained(
        player: Player,
        treeType: WoodcuttingDefinitions.TreeType
    ): Boolean {
        val logItem = treeType.logRscm
        if (player.inventory.add(logItem, 1).hasSucceeded()) {
            player.addXp(Skills.WOODCUTTING, treeType.xp)
            try {
                val logItemId = getRSCM(logItem)
                val logName = Item(logItemId).getName().lowercase()
                player.message("You get some $logName.")
            } catch (e: Exception) {
                // Fallback to generic message if item name lookup fails
                player.message("You get some logs.")
            }
            player.playSound(LOG_OBTAINED_SOUND, volume = 1, delay = 0)
            return true
        } else {
            player.message("Your inventory is too full to hold any more logs.")
            player.animate(RSCM.NONE)
            return false
        }
    }


    /**
     * Handles tree depletion: creates stump, removes tree, and schedules respawn.
     *
     * Tree depletes when:
     * - Countdown timer reaches 0 (for countdown trees: depletes on next successful log)
     * - Always (for regular trees: depletes after 1 log)
     *
     * Returns true if depletion was handled, false if handler prevented default behavior.
     */
    private suspend fun QueueTask.depleteTree(
        player: Player,
        obj: GameObject,
        treeRscm: String,
        treeType: WoodcuttingDefinitions.TreeType
    ): Boolean {
        // Post event for plugin system to handle
        TreeDepleteEvent(player, obj, treeRscm).post()

        // Check for custom deplete handler
        val handler = treeDepleteHandlers[treeType]
        val handled = if (handler != null) {
            handler.handleDeplete(this@depleteTree, player, obj, treeRscm, world)
        } else {
            false
        }

        // If handler processed the depletion, stop here (no stump creation)
        if (handled) {
            return true
        }

        // Default behavior: create stump
        val stumpRscm = getStumpRscmForTreeRscm(treeRscm)
        if (stumpRscm != null) {
            try {
                ObjectTimerMap.removeObject(obj)
                val originalTreeId = obj.id
                val originalTreeTile = obj.tile
                val originalTreeType = obj.type
                val originalTreeRot = obj.rot

                world.remove(obj)

                val stump = DynamicObject(stumpRscm, originalTreeType, originalTreeRot, originalTreeTile)
                world.spawn(stump)

                world.queue {
                    wait(treeType.respawnCycles)
                    if (stump.isSpawned(world)) {
                        world.remove(stump)
                    }
                    val restoredTree = DynamicObject(originalTreeId, originalTreeType, originalTreeRot, originalTreeTile)
                    if (restoredTree.isSpawned(world).not()) {
                        world.spawn(restoredTree)
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Stump RSCM '$stumpRscm' for tree '$treeRscm' not found, skipping stump creation" }
                // Fallback: respawn tree without stump
                world.queue {
                    wait(treeType.respawnCycles)
                    val restoredTree = DynamicObject(obj.id, obj.type, obj.rot, obj.tile)
                    if (restoredTree.isSpawned(world).not()) {
                        world.spawn(restoredTree)
                    }
                }
            }
        }

        player.message("You have cut down this tree.")
        player.animate(RSCM.NONE)

        // Clear the countdown timer for this tree since it's been depleted
        val treeKey = treeTimers.generateTreeKey(obj.tile.x, obj.tile.z, obj.tile.height, obj.id)
        treeTimers.clearTimer(obj.tile, treeKey)

        return false
    }

    /**
     * Main tree chopping logic.
     * Uses RSCM identifiers exclusively - no hardcoded IDs.
     */
    suspend fun QueueTask.chopTree(player: Player, treeRscm: String) {
        val treeType = getTreeTypeForRscm(treeRscm) ?: return
        val wcLevel = player.getSkills().getBaseLevel(Skills.WOODCUTTING)

        if (wcLevel < treeType.levelReq) {
            player.message("You need a Woodcutting level of ${treeType.levelReq} to cut this tree.")
            return
        }

        if (!hasAnyAxe(player)) {
            player.message("You need an axe to chop down this tree.")
            return
        }

        val axeId = getBestAxe(player)
        if (axeId == null) {
            player.message("You do not have an axe which you have the woodcutting level to use.")
            return
        }

        val axeData = WoodcuttingDefinitions.AXE_DATA[axeId] ?: return
        val obj = player.attr[INTERACTING_OBJ_ATTR]?.get() ?: return

        if (!obj.isSpawned(world)) {
            return
        }

        val nearestTile = obj.findNearestTile(player.tile)
        player.faceTile(nearestTile)
        player.message("You swing your axe at the tree.")

        val tickDelay = axeData.tickDelay
        val (low, high) = treeType.successRateLow to treeType.successRateHigh
        val chopAnimation = RSCM.getReverseMapping(RSCMType.SEQTYPES, axeData.animationId) ?: return

        // Start the animation once - it will loop naturally
        player.animate(chopAnimation)
        player.playSound(CHOP_SOUND, volume = 1, delay = 0)

        // Get tree key for timer tracking
        val treeKey = treeTimers.generateTreeKey(obj.tile.x, obj.tile.z, obj.tile.height, obj.id)
        var animationTicksElapsed = 0
        var timerReachedZero = false

        repeatUntil(delay = tickDelay, immediate = false, predicate = {
            val currentNearestTile = obj.findNearestTile(player.tile)
            !player.tile.isWithinRadius(currentNearestTile, 1) ||
            player.inventory.isFull ||
            !obj.isSpawned(world) ||
            isStump(obj, treeRscm, player)
        }) {
            // Only restart animation when it's finished (every animationDuration ticks)
            animationTicksElapsed += tickDelay
            if (animationTicksElapsed >= AXE_ANIMATION_DURATION) {
                player.animate(chopAnimation)
                animationTicksElapsed = 0
            }
            player.playSound(CHOP_SOUND, volume = 1, delay = 0)

            // Update countdown timer (counts down while chopping, regenerates when idle)
            // Only update for trees that use countdown (not regular trees)
            if (treeType.depleteMechanic is WoodcuttingDefinitions.DepleteMechanic.Countdown) {
                timerReachedZero = treeTimers.updateTreeTimer(obj.tile, treeKey, treeType, player, isChopping = true)
            }

            val success = success(low, high, wcLevel)

            if (success) {
                val logObtained = handleLogObtained(player, treeType)
                if (!logObtained) {
                    // Inventory full, stop woodcutting
                    // Remove player from active choppers
                    if (treeType.depleteMechanic is WoodcuttingDefinitions.DepleteMechanic.Countdown) {
                        treeTimers.updateTreeTimer(obj.tile, treeKey, treeType, player, isChopping = false)
                    }
                    return@repeatUntil
                }

                // Check if tree should deplete
                val shouldDeplete = when (val mechanic = treeType.depleteMechanic) {
                    is WoodcuttingDefinitions.DepleteMechanic.Always -> true
                    is WoodcuttingDefinitions.DepleteMechanic.Countdown -> timerReachedZero
                }

                if (shouldDeplete) {
                    // Remove player from active choppers before depleting
                    if (treeType.depleteMechanic is WoodcuttingDefinitions.DepleteMechanic.Countdown) {
                        treeTimers.updateTreeTimer(obj.tile, treeKey, treeType, player, isChopping = false)
                    }
                    depleteTree(player, obj, treeRscm, treeType)
                    return@repeatUntil
                }
            }
        }

        // Remove player from active choppers when they stop chopping
        if (treeType.depleteMechanic is WoodcuttingDefinitions.DepleteMechanic.Countdown) {
            treeTimers.updateTreeTimer(obj.tile, treeKey, treeType, player, isChopping = false)
        }

        player.animate(RSCM.NONE)
    }
}
