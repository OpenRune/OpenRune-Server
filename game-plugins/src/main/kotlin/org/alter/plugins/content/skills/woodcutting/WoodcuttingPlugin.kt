package org.alter.plugins.content.skills.woodcutting

import org.alter.api.*
import org.alter.api.success
import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.ObjectTimerMap
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.EventManager
import org.alter.game.pluginnew.event.impl.TreeDepleteEvent
import org.alter.plugins.content.skills.woodcutting.TreeDepleteHandler
import org.alter.plugins.content.skills.woodcutting.handlers.BlisterwoodTreeDepleteHandler
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCMType
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

        /**
         * Timer key for tree countdown timers.
         * Timers are stored directly on the GameObject.
         */
        private val TREE_COUNTDOWN_TIMER = TimerKey()

        /**
         * Attribute key for tracking players actively chopping a tree.
         * Similar to PLAYERS_COUNT_ATTR in campfires.
         */
        val ACTIVE_CHOPPERS_ATTR = AttributeKey<MutableSet<Player>>()

        /**
         * Attribute key for storing the max countdown value for a tree.
         * Used to know when to stop regenerating the timer.
         */
        val MAX_COUNTDOWN_ATTR = AttributeKey<Int>()
    }

    /**
     * Registry of tree deplete handlers.
     * Each tree type can have a custom handler that defines what happens when it depletes.
     */
    private val treeDepleteHandlers: MutableMap<String, TreeDepleteHandler> = mutableMapOf()

    /**
     * Registers a tree deplete handler for a specific tree type.
     * This allows custom behavior when a tree depletes (e.g., spawning NPCs, special messages, etc.).
     *
     * @param treeTypeId The tree type identifier (e.g., "blisterwood", "oak")
     * @param handler The handler to register
     */
    fun onDeplete(treeTypeId: String, handler: TreeDepleteHandler) {
        require(handler.treeTypeId == treeTypeId) { "Handler tree type (${handler.treeTypeId}) must match provided tree type ($treeTypeId)" }
        treeDepleteHandlers[treeTypeId] = handler
    }

    init {
        registerTreeHandlers()
        registerDepleteHandlers()
        startTimerUpdateTask()
    }

    /**
     * Starts a periodic task to update tree countdown timers.
     * Timers count down while chopping, regenerate when idle.
     * Runs every game tick via ObjectTimerMap, but we need to handle the countdown logic.
     */
    private fun startTimerUpdateTask() {
        world.queue {
            while (true) {
                wait(1) // Every game tick
                updateAllTreeTimers()
            }
        }
    }

    /**
     * Updates all tree timers that have active choppers or are regenerating.
     * Timers count down 1 tick per game tick while chopping, regenerate when idle.
     *
     * Performance optimizations:
     * - Only processes objects with our tree-specific attributes (fast attribute check first)
     * - Skips objects that don't have TREE_COUNTDOWN_TIMER (filters out campfires, other skills)
     * - ObjectTimerMap only tracks objects with timers, not all objects in the world
     */
    private fun updateAllTreeTimers() {
        ObjectTimerMap.forEach { obj ->
            val activeChoppers = obj.attr[ACTIVE_CHOPPERS_ATTR] ?: return@forEach
            val maxCountdown = obj.attr[MAX_COUNTDOWN_ATTR] ?: return@forEach

            val currentTime = obj.getTimeLeft(TREE_COUNTDOWN_TIMER)
            if (currentTime == 0) {
                if (activeChoppers.isEmpty()) {
                    obj.attr.remove(ACTIVE_CHOPPERS_ATTR)
                    obj.attr.remove(MAX_COUNTDOWN_ATTR)
                }
                return@forEach
            }

            if (activeChoppers.isNotEmpty()) {
                val newTime = (currentTime - 1).coerceAtLeast(0)
                if (newTime > 0) {
                    obj.setTimer(TREE_COUNTDOWN_TIMER, newTime)
                } else {
                    obj.removeTimer(TREE_COUNTDOWN_TIMER)
                }
            } else {
                if (currentTime < maxCountdown) {
                    obj.setTimer(TREE_COUNTDOWN_TIMER, (currentTime + 1).coerceAtMost(maxCountdown))
                } else {
                    obj.removeTimer(TREE_COUNTDOWN_TIMER)
                    obj.attr.remove(MAX_COUNTDOWN_ATTR)
                    obj.attr.remove(ACTIVE_CHOPPERS_ATTR)
                }
            }
        }
    }

    /**
     * Registers default tree deplete handlers.
     */
    private fun registerDepleteHandlers() {
        onDeplete("blisterwood", BlisterwoodTreeDepleteHandler())
    }

    /**
     * Registers chop handlers for all tree variants.
     * Uses RSCM identifiers exclusively.
     */
    private fun registerTreeHandlers() {
        WoodcuttingDefinitions.TREE_RSCM_TO_REPRESENTATIVE.forEach { (treeRscm, _) ->
            try {
                // Use RSCM string directly (onObjOption handles conversion internally)
                if (objHasOption(obj = treeRscm, option = "chop down")) {
                    onObjOption(obj = treeRscm, option = "chop down") {
                        player.queue { chopTree(player, treeRscm) }
                    }
                } else if (objHasOption(obj = treeRscm, option = "chop")) {
                    onObjOption(obj = treeRscm, option = "chop") {
                        player.queue { chopTree(player, treeRscm) }
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Tree object '$treeRscm' not found in cache or option not available, skipping registration: ${e.message}" }
            }
        }
    }

    /**
     * Gets the tree data for a given tree RSCM identifier.
     * Returns null if the tree is not recognized.
     */
    private fun getTreeDataForRscm(treeRscm: String): WoodcuttingDefinitions.TreeData? {
        val representativeRscm = WoodcuttingDefinitions.TREE_RSCM_TO_REPRESENTATIVE[treeRscm] ?: return null
        val representativeId = try {
            getRSCM(representativeRscm)
        } catch (e: Exception) {
            return null
        }
        return WoodcuttingDefinitions.TREE_DATA_BY_OBJECT[representativeId]
    }

    /**
     * Gets the tree type identifier for a given tree RSCM identifier.
     * Returns null if the tree is not recognized.
     */
    private fun getTreeTypeIdForRscm(treeRscm: String): String? {
        val representativeRscm = WoodcuttingDefinitions.TREE_RSCM_TO_REPRESENTATIVE[treeRscm] ?: return null
        return WoodcuttingDefinitions.REPRESENTATIVE_TO_TREE_TYPE_ID[representativeRscm]
    }

    /**
     * Gets the stump RSCM identifier for a given tree RSCM identifier.
     * Regular and dead trees have specific stump mappings that take precedence over tree type mappings.
     */
    private fun getStumpRscmForTreeRscm(treeRscm: String): String? {
        // Check for regular tree specific stump mapping first
        WoodcuttingDefinitions.REGULAR_TREE_TO_STUMP_RSCM[treeRscm]?.let { return it }

        // Check for dead tree specific stump mapping
        WoodcuttingDefinitions.DEAD_TREE_TO_STUMP_RSCM[treeRscm]?.let { return it }

        // Fall back to tree type mapping
        val treeTypeId = getTreeTypeIdForRscm(treeRscm) ?: return null
        return WoodcuttingDefinitions.TREE_TYPE_ID_TO_STUMP_RSCM[treeTypeId]
    }

    /**
     * Checks if the player has any axe available (equipped or in inventory).
     */
    private fun hasAnyAxe(player: Player): Boolean {
        val equippedWeapon = player.equipment[EquipmentType.WEAPON.id]
        equippedWeapon?.let {
            val axeId = getAxeIdentifier(it.id)
            if (axeId != null) {
                return true
            }
        }

        for (i in 0 until player.inventory.capacity) {
            val item = player.inventory[i] ?: continue
            val axeId = getAxeIdentifier(item.id)
            if (axeId != null) {
                return true
            }
        }

        return false
    }

    /**
     * Gets the best axe the player can use based on their woodcutting level.
     * Checks both equipped weapon and inventory.
     * Returns null if the player has no usable axe.
     */
    private fun getBestAxe(player: Player): String? {
        val wcLevel = player.getSkills().getBaseLevel(Skills.WOODCUTTING)
        val equippedWeapon = player.equipment[EquipmentType.WEAPON.id]
        equippedWeapon?.let {
            val axeId = getAxeIdentifier(it.id)
            if (axeId != null) {
                val axeData = WoodcuttingDefinitions.AXE_DATA[axeId]
                if (axeData != null && wcLevel >= axeData.levelReq) {
                    return axeId
                }
            }
        }

        var bestAxe: String? = null
        var bestLevel = -1

        for (i in 0 until player.inventory.capacity) {
            val item = player.inventory[i] ?: continue
            val axeId = getAxeIdentifier(item.id)
            if (axeId != null) {
                val axeData = WoodcuttingDefinitions.AXE_DATA[axeId]
                if (axeData != null && axeData.levelReq > bestLevel && wcLevel >= axeData.levelReq) {
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
            if (axeId in WoodcuttingDefinitions.AXE_DATA) axeId else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if the object is a stump by comparing its transform to the expected stump ID.
     */
    private fun isStump(obj: GameObject, stumpId: Int, player: Player): Boolean {
        return obj.getTransform(player) == stumpId
    }

    /**
     * Handles when a player successfully obtains a log from a tree.
     * Adds the log to inventory, gives XP, and shows a message.
     * Returns true if log was successfully added, false if inventory is full.
     */
    private suspend fun QueueTask.handleLogObtained(
        player: Player,
        treeData: WoodcuttingDefinitions.TreeData
    ): Boolean {
        val logItem = treeData.logRscm
        if (player.inventory.add(logItem, 1).hasSucceeded()) {
            player.addXp(Skills.WOODCUTTING, treeData.xp)
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
        treeData: WoodcuttingDefinitions.TreeData
    ): Boolean {
        val eventHandled = EventManager.postWithResult(TreeDepleteEvent(player, obj, treeRscm))

        val treeTypeId = getTreeTypeIdForRscm(treeRscm)
        val handler = treeTypeId?.let { treeDepleteHandlers[it] }
        val handled = if (handler != null) {
            handler.handleDeplete(this@depleteTree, player, obj, treeRscm, world)
        } else {
            false
        }

        // If event system or handler processed the depletion, stop here (no stump creation)
        if (eventHandled || handled) {
            return true
        }

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
                    wait(treeData.respawnCycles)
                    if (stump.isSpawned(world)) {
                        world.remove(stump)
                    }
                    val restoredTree = DynamicObject(originalTreeId, originalTreeType, originalTreeRot, originalTreeTile)
                    if (!restoredTree.isSpawned(world)) {
                        world.spawn(restoredTree)
                    }
                }
            } catch (e: Exception) {
                logger.warn { "Stump RSCM '$stumpRscm' for tree '$treeRscm' not found, skipping stump creation: ${e.message}" }
                world.queue {
                    wait(treeData.respawnCycles)
                    val restoredTree = DynamicObject(obj.id, obj.type, obj.rot, obj.tile)
                    if (!restoredTree.isSpawned(world)) {
                        world.spawn(restoredTree)
                    }
                }
            }
        }

        player.message("You have cut down this tree.")
        player.animate(RSCM.NONE)

        return false
    }

    /**
     * Main tree chopping logic.
     * Uses RSCM identifiers exclusively - no hardcoded IDs.
     * Timers are stored directly on the GameObject.
     */
    suspend fun QueueTask.chopTree(player: Player, treeRscm: String) {
        val treeData = getTreeDataForRscm(treeRscm) ?: return
        val wcLevel = player.getSkills().getBaseLevel(Skills.WOODCUTTING)

        if (wcLevel < treeData.levelReq) {
            player.message("You need a Woodcutting level of ${treeData.levelReq} to cut this tree.")
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

        val stumpRscm = getStumpRscmForTreeRscm(treeRscm)
        val stumpId = stumpRscm?.let {
            try {
                getRSCM(it)
            } catch (e: Exception) {
                null
            }
        } ?: -1

        val nearestTile = obj.findNearestTile(player.tile)
        player.faceTile(nearestTile)
        player.message("You swing your axe at the tree.")

        val tickDelay = axeData.tickDelay
        val (low, high) = treeData.successRateLow to treeData.successRateHigh
        val chopAnimation = RSCM.getReverseMapping(RSCMType.SEQTYPES, axeData.animationId) ?: return

        player.loopAnim(chopAnimation)

        if (treeData.usesCountdown()) {
            val activeChoppers = obj.attr.getOrPut(ACTIVE_CHOPPERS_ATTR) { mutableSetOf() }
            activeChoppers.add(player)

            if (!obj.hasTimers() || obj.getTimeLeft(TREE_COUNTDOWN_TIMER) == 0) {
                obj.setTimer(TREE_COUNTDOWN_TIMER, treeData.despawnTicks)
                obj.attr[MAX_COUNTDOWN_ATTR] = treeData.despawnTicks
            }
        }

        repeatUntil(delay = tickDelay, immediate = false, predicate = {
            val currentNearestTile = obj.findNearestTile(player.tile)
            !player.tile.isWithinRadius(currentNearestTile, 1) ||
            player.inventory.isFull ||
            !obj.isSpawned(world) ||
            isStump(obj, stumpId, player)
        }) {
            player.playSound(CHOP_SOUND, volume = 1, delay = 0)

            val success = success(low, high, wcLevel)

            if (success) {
                val logObtained = handleLogObtained(player, treeData)
                if (!logObtained) {
                    if (treeData.usesCountdown()) {
                        obj.attr[ACTIVE_CHOPPERS_ATTR]?.remove(player)
                    }
                    return@repeatUntil
                }

                val shouldDeplete = if (treeData.usesCountdown()) {
                    obj.getTimeLeft(TREE_COUNTDOWN_TIMER) <= 0
                } else {
                    true
                }

                if (shouldDeplete) {
                    if (treeData.usesCountdown()) {
                        obj.attr[ACTIVE_CHOPPERS_ATTR]?.remove(player)
                    }
                    depleteTree(player, obj, treeRscm, treeData)
                    return@repeatUntil
                }
            }
        }

        if (treeData.usesCountdown()) {
            obj.attr[ACTIVE_CHOPPERS_ATTR]?.remove(player)
        }

        player.stopLoopAnim()
    }
}
