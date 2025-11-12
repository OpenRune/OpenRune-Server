package org.alter.skills.woodcutting

import org.alter.api.*
import org.alter.api.success
import org.alter.api.ext.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.attr.INTERACTING_OBJ_ATTR
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.ObjectTimerMap
import org.alter.game.model.entity.Player
import org.alter.game.model.item.Item
import org.alter.game.model.queue.QueueTask
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.event.EventManager
import org.alter.skills.woodcutting.handlers.BlisterwoodTreeDepleteHandler
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCMType
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.ReturnableEventListener
import org.alter.game.pluginnew.event.impl.onObjectOption
import org.alter.game.util.DbHelper
import org.alter.game.util.DbHelper.Companion.table
import org.alter.game.util.enumKey
import org.alter.game.util.multiColumn
import org.alter.game.util.vars.LocType
import org.alter.skills.woodcutting.WoodcuttingDefinitions.axeData
import org.alter.skills.woodcutting.WoodcuttingDefinitions.tableToTreeData

class WoodcuttingPlugin : PluginEvent() {

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
     * Registers a tree deplete handler for a specific tree type.
     * This allows custom behavior when a tree depletes (e.g., spawning NPCs, special messages, etc.).
     *
     * @param treeTypeId The tree type identifier (e.g., "blisterwood", "oak")
     * @param handler The handler to register
     */
    fun onDeplete(treeTypeId: String, handler: TreeDepleteHandler) {
        require(handler.treeTypeId == treeTypeId) { "Handler tree type (${handler.treeTypeId}) must match provided tree type ($treeTypeId)" }
        RSCM.requireRSCM(RSCMType.ROWTYPES, treeTypeId)
        val columnId = getRSCM(treeTypeId)
        ReturnableEventListener.on<TreeDepleteEvent, Boolean> {
            where { treeType == columnId }
            then {
                return@then handler.handleDeplete(player, treeObject, world)
            }
        }
    }

    override fun init() {
        registerDepleteHandlers()

        table("tables.woodcutting_trees").forEach { tree ->
            val treeObject = tree.multiColumn("columns.woodcutting_trees:tree_object", LocType)
            treeObject.forEach { treeID ->
                try {
                    onObjectOption(treeID,"chop down","chop") {
                        player.queue { chopTree(player, tree) }
                    }
                } catch (e: Exception) {
                    logger.warn { "Tree object '${treeID}' not found in cache or option not available, skipping registration: ${e.message}" }
                }
            }
        }

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
        onDeplete("dbrows.woodcutting_blisterwood_tree", BlisterwoodTreeDepleteHandler())
    }

    /**
     * Gets the stump RSCM identifier for a given tree RSCM identifier.
     * Regular and dead trees have specific stump mappings that take precedence over tree type mappings.
     */
    private fun getStumpForTree(tree: Int, fallback: Int?): Int? {
        enumKey("enums.regular_tree_stumps", tree, LocType, LocType)?.let { return it }
        enumKey("enums.dead_tree_stumps", tree, LocType, LocType)?.let { return it }

        return fallback
    }


    /**
     * Checks if the player has any axe available (equipped or in inventory).
     */
    private fun hasAnyAxe(player: Player): Boolean {
        player.equipment[EquipmentType.WEAPON.id]?.let { weapon ->
            if (axeData.containsKey(weapon.id)) return true
        }

        return player.inventory.asSequence()
            .filterNotNull()
            .any { axeData.containsKey(it.id) }
    }

    /**
     * Gets the best axe the player can use based on their woodcutting level.
     * Checks both equipped weapon and inventory.
     * Returns null if the player has no usable axe.
     */
    private fun getBestAxe(player: Player): WoodcuttingDefinitions.AxeData? {
        val wcLevel = player.getSkills().getBaseLevel(Skills.WOODCUTTING)


        player.equipment[EquipmentType.WEAPON.id]?.let { weapon ->
            axeData[weapon.id]?.takeIf { wcLevel >= it.levelReq }?.let { return it }
        }

        return player.inventory.asSequence()
            .filterNotNull()
            .mapNotNull { axeData[it.id] }
            .filter { wcLevel >= it.levelReq }
            .maxByOrNull { it.levelReq }
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
    private fun handleLogObtained(
        player: Player,
        treeData: WoodcuttingDefinitions.TreeData
    ): Boolean {
        val logItem = treeData.log
        if (player.inventory.add(logItem, 1).hasSucceeded()) {
            player.addXp(Skills.WOODCUTTING, treeData.xp)
            try {
                val logName = Item(logItem).getName().lowercase()
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
    private fun QueueTask.depleteTree(
        player: Player,
        obj: GameObject,
        columnID : Int,
        treeData: WoodcuttingDefinitions.TreeData
    ): Boolean {

        val specialTree = EventManager.postWithResult(TreeDepleteEvent(
            player = player,
            treeObject = obj,
            treeRscm = obj.id,
            treeType = columnID,
            world = world
        ))

        if (specialTree) {
            println("Special Tree Return")
            return true
        }

        getStumpForTree(obj.internalID, treeData.stumpObject)?.let { stump ->
            obj.replaceWith(world, stump, treeData.respawnCycles, restoreOriginal = true)
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
    suspend fun QueueTask.chopTree(player: Player, treeTable: DbHelper) {
        val treeData = tableToTreeData(treeTable)
        val wcLevel = player.getSkills().getBaseLevel(Skills.WOODCUTTING)

        if (wcLevel < treeData.levelReq) {
            player.message("You need a Woodcutting level of ${treeData.levelReq} to cut this tree.")
            return
        }

        if (!hasAnyAxe(player)) {
          player.message("You need an axe to chop down this tree.")
          return
      }

        val axeData = getBestAxe(player)
        if (axeData == null) {
          player.message("You do not have an axe which you have the woodcutting level to use.")
          return
        }

        val obj = player.attr[INTERACTING_OBJ_ATTR]?.get() ?: return

        if (!obj.isSpawned(world)) {
            return
        }

        val stumpId = getStumpForTree(obj.internalID,treeData.stumpObject)?: -1

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
                TreeLogObtainedEvent(player,obj,treeData,treeTable.id).post()
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
                    depleteTree(player, obj, treeTable.id, treeData)
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
