package org.alter.objects.doors

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import org.alter.api.ext.*
import org.alter.game.model.attr.AttributeKey
import org.alter.game.model.collision.*
import org.alter.game.model.entity.DynamicObject
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.ObjectTimerMap
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.ObjectClickEvent
import org.alter.game.pluginnew.event.impl.WorldTickEvent
import org.alter.rscm.RSCM
import org.alter.rscm.RSCM.getRSCM
import org.alter.rscm.RSCMType
import java.io.File
import kotlin.math.abs

/**
 * Global door/gate/fence opening handler using the new PluginEvent system.
 * Supports single doors, double doors, and gates with proper rotation and collision handling.
 */
class GlobalDoorEvents : PluginEvent() {

    data class SingleDoorConfig(
        val closed: String, // RSCM name like "objects.door_oak_closed"
        val opened: String   // RSCM name like "objects.door_oak_opened"
    )

    data class DoubleDoorConfig(
        val closed: Map<String, String>, // RSCM names
        val opened: Map<String, String>  // RSCM names
    )

    data class GateConfig(
        val closed: Map<String, String>, // RSCM names
        val opened: Map<String, String>  // RSCM names
    )

    // Internal config classes that use IDs
    private data class DoubleDoorIdConfig(
        val closed: Map<String, Int>,
        val opened: Map<String, Int>
    )

    private data class GateIdConfig(
        val closed: Map<String, Int>,
        val opened: Map<String, Int>
    )

    private val singleDoors = mutableMapOf<Int, Int>() // closed -> opened
    private val singleDoorsReverse = mutableMapOf<Int, Int>() // opened -> closed
    private val doubleDoors = mutableMapOf<Int, DoubleDoorIdConfig>() // closed left -> config
    private val doubleDoorsReverse = mutableMapOf<Int, DoubleDoorIdConfig>() // opened left -> config
    private val gates = mutableMapOf<Int, GateIdConfig>() // closed hinge -> config
    private val gatesReverse = mutableMapOf<Int, GateIdConfig>() // opened hinge -> config
    private val allDoorIds = mutableSetOf<Int>() // All door/gate IDs for quick lookup (closed and opened)

    // Performance optimization: Track only doors with our timer instead of iterating all ObjectTimerMap
    private val doorsWithTimer = mutableSetOf<GameObject>()

    // Performance optimization: Reverse lookup maps to avoid iterating all configs
    private val doubleDoorRightClosedToConfig = mutableMapOf<Int, DoubleDoorIdConfig>() // closed right -> config
    private val doubleDoorRightOpenedToConfig = mutableMapOf<Int, DoubleDoorIdConfig>() // opened right -> config
    private val gateExtensionClosedToConfig = mutableMapOf<Int, GateIdConfig>() // closed extension -> config
    private val gateExtensionOpenedToConfig = mutableMapOf<Int, GateIdConfig>() // opened extension -> config

    // Attribute keys to store the original state of a door before it was opened
    private val ORIGINAL_ROTATION_ATTR = AttributeKey<Int>(temp = true)
    private val ORIGINAL_TILE_ATTR = AttributeKey<org.alter.game.model.Tile>(temp = true)

    // Attribute keys to store the opened state of a door that starts opened (for restoring when reopening)
    private val OPENED_ROTATION_ATTR = AttributeKey<Int>(temp = true)
    private val OPENED_TILE_ATTR = AttributeKey<org.alter.game.model.Tile>(temp = true)

    // Timer key for automatic door restoration
    private val DOOR_AUTO_RESTORE_TIMER = TimerKey()

    // Attribute key to store the default state ID (closed or opened ID) for restoration
    private val DEFAULT_STATE_ID_ATTR = AttributeKey<Int>(temp = true)

    override fun init() {
        loadDoorConfigs()

        // Handle "Open" option on any object using event system
        on<ObjectClickEvent> {
            where {
                optionName.equals("Open", ignoreCase = true) &&
                allDoorIds.contains(gameObject.internalID)
            }
            then {
                handleOpenOption(player, gameObject)
            }
        }

        // Handle "Close" option on any object using event system
        on<ObjectClickEvent> {
            where {
                optionName.equals("Close", ignoreCase = true) &&
                allDoorIds.contains(gameObject.internalID)
            }
            then {
                handleCloseOption(player, gameObject)
            }
        }

        // Check for expired door timers every world tick
        // Performance optimization: Only iterate doors we've registered with our timer
        on<WorldTickEvent> {
            then {
                // Use iterator to safely remove during iteration
                val iterator = doorsWithTimer.iterator()
                while (iterator.hasNext()) {
                    val obj = iterator.next()
                    // Check if object still exists and has our timer
                    if (!obj.hasTimers() || !obj.timers!!.exists(DOOR_AUTO_RESTORE_TIMER)) {
                        iterator.remove()
                        continue
                    }
                    if (obj.getTimeLeft(DOOR_AUTO_RESTORE_TIMER) <= 0) {
                        // Timer expired - restore door to default state
                        restoreDoorToDefaultState(obj)
                        iterator.remove() // Remove after restoration
                    }
                }
            }
        }
    }

    private fun loadDoorConfigs() {
        val mapper = ObjectMapper().apply {
            registerModule(KotlinModule.Builder().build())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
        val dataDir = File("../data/cfg")

        // Load single doors
        val singleDoorsFile = File(dataDir, "doors/single-doors.json")
        if (singleDoorsFile.exists()) {
            val configs: List<SingleDoorConfig> = mapper.readValue(singleDoorsFile)
            configs.forEach { config ->
                val closedId = getRSCM(config.closed)
                val openedId = getRSCM(config.opened)
                singleDoors[closedId] = openedId
                singleDoorsReverse[openedId] = closedId
                allDoorIds.add(closedId)
                allDoorIds.add(openedId)
            }
        }

        // Load double doors
        val doubleDoorsFile = File(dataDir, "doors/double-doors.json")
        if (doubleDoorsFile.exists()) {
            val configs: List<DoubleDoorConfig> = mapper.readValue(doubleDoorsFile)
            configs.forEach { config ->
                // Convert RSCM names to IDs and create a new config with IDs
                val closedLeftName = config.closed["left"]
                val closedRightName = config.closed["right"]
                val openedLeftName = config.opened["left"]
                val openedRightName = config.opened["right"]

                if (closedLeftName != null && openedLeftName != null) {
                    val closedLeftId = getRSCM(closedLeftName)
                    val openedLeftId = getRSCM(openedLeftName)
                    val closedRightId = closedRightName?.let { getRSCM(it) }
                    val openedRightId = openedRightName?.let { getRSCM(it) }

                    val idConfig = DoubleDoorIdConfig(
                        closed = mapOf(
                            "left" to closedLeftId,
                            "right" to (closedRightId ?: -1)
                        ).filter { it.value != -1 },
                        opened = mapOf(
                            "left" to openedLeftId,
                            "right" to (openedRightId ?: -1)
                        ).filter { it.value != -1 }
                    )

                    doubleDoors[closedLeftId] = idConfig
                    doubleDoorsReverse[openedLeftId] = idConfig
                    allDoorIds.add(closedLeftId)
                    allDoorIds.add(openedLeftId)
                    closedRightId?.let {
                        allDoorIds.add(it)
                        doubleDoorRightClosedToConfig[it] = idConfig // Performance: reverse lookup
                    }
                    openedRightId?.let {
                        allDoorIds.add(it)
                        doubleDoorRightOpenedToConfig[it] = idConfig // Performance: reverse lookup
                    }
                }
            }
        }

        // Load gates
        val gatesFile = File(dataDir, "gates/gates.json")
        if (gatesFile.exists()) {
            val configs: List<GateConfig> = mapper.readValue(gatesFile)
            configs.forEach { config ->
                val closedHingeName = config.closed["hinge"]
                val closedExtensionName = config.closed["extension"]
                val openedHingeName = config.opened["hinge"]
                val openedExtensionName = config.opened["extension"]

                if (closedHingeName != null && openedHingeName != null) {
                    val closedHingeId = getRSCM(closedHingeName)
                    val openedHingeId = getRSCM(openedHingeName)
                    val closedExtensionId = closedExtensionName?.let { getRSCM(it) }
                    val openedExtensionId = openedExtensionName?.let { getRSCM(it) }

                    // Validate IDs are valid (not -1)
                    if (closedHingeId == -1 || openedHingeId == -1) {
                        return@forEach
                    }
                    if (closedExtensionId != null && closedExtensionId == -1) {
                        return@forEach
                    }
                    if (openedExtensionId != null && openedExtensionId == -1) {
                        return@forEach
                    }

                    val idConfig = GateIdConfig(
                        closed = mapOf(
                            "hinge" to closedHingeId,
                            "extension" to (closedExtensionId ?: -1)
                        ).filter { it.value != -1 },
                        opened = mapOf(
                            "hinge" to openedHingeId,
                            "extension" to (openedExtensionId ?: -1)
                        ).filter { it.value != -1 }
                    )

                    gates[closedHingeId] = idConfig
                    // Also add closed extension to gates map so it can be opened (only if valid ID)
                    closedExtensionId?.takeIf { it != -1 }?.let {
                        gates[it] = idConfig
                        gateExtensionClosedToConfig[it] = idConfig // Performance: reverse lookup
                    }
                    gatesReverse[openedHingeId] = idConfig
                    // Also add opened extension to reverse mapping so it can be closed (only if valid ID)
                    openedExtensionId?.takeIf { it != -1 }?.let {
                        gatesReverse[it] = idConfig
                        gateExtensionOpenedToConfig[it] = idConfig // Performance: reverse lookup
                    }
                    allDoorIds.add(closedHingeId)
                    allDoorIds.add(openedHingeId)
                    closedExtensionId?.let { allDoorIds.add(it) }
                    openedExtensionId?.let { allDoorIds.add(it) }
                }
            }
        }
    }

    private fun handleOpenOption(player: org.alter.game.model.entity.Player, obj: GameObject) {
        val objId = obj.internalID

        // First check if it's already opened (in reverse maps) - if so, it's already open, do nothing or close it
        val closedId = singleDoorsReverse[objId]
        if (closedId != null) {
            return
        }

        // Check if it's a single door (closed)
        val openedId = singleDoors[objId]
        if (openedId != null) {
            openSingleDoor(player, obj, openedId)
            return
        }

        // Check if it's a double door (opened left side) - already open
        val doubleDoorConfigReverse = doubleDoorsReverse[objId]
        if (doubleDoorConfigReverse != null) {
            return
        }

        // Check if it's a double door (closed left side)
        val doubleDoorConfig = doubleDoors[objId]
        if (doubleDoorConfig != null) {
            openDoubleDoor(player, obj, doubleDoorConfig)
            return
        }

        // Check if it's a gate (opened) - already open
        val gateConfigReverse = gatesReverse[objId]
        if (gateConfigReverse != null) {
            return
        }

        // Check if it's a gate (closed hinge or extension)
        val gateConfig = gates[objId]
        if (gateConfig != null) {
            // Check if this is the hinge or extension
            val isHinge = gateConfig.closed["hinge"] == objId
            val isExtension = gateConfig.closed["extension"] == objId

            if (isHinge) {
                // This is the hinge, open the gate directly
                openGate(player, obj, gateConfig)
                return
            } else if (isExtension) {
                // This is the extension, find the hinge and open the gate
                val hingeId = gateConfig.closed["hinge"] ?: return
                val hinge = findAdjacentDoor(world, obj.tile, hingeId)
                if (hinge != null) {
                    // Open the entire gate (both hinge and extension)
                    openGate(player, hinge, gateConfig)
                } else {
                    // Hinge not found, but we can still open this extension directly
                    val extensionOpenedId = gateConfig.opened["extension"] ?: return
                    openSingleDoor(player, obj, extensionOpenedId)
                }
                return
            }
        }

        // Check if it's a double door (right side) - need to find the left side
        // Performance: Use reverse lookup map instead of iterating all configs
        val doubleDoorConfigForRight = doubleDoorRightClosedToConfig[objId]
        if (doubleDoorConfigForRight != null) {
            // Find the left door at the same location or adjacent
            val leftId = doubleDoorConfigForRight.closed["left"] ?: return
            val leftDoor = findAdjacentDoor(world, obj.tile, leftId)
            if (leftDoor != null) {
                openDoubleDoor(player, leftDoor, doubleDoorConfigForRight)
                // Also open this right door
                val rightOpenedId = doubleDoorConfigForRight.opened["right"] ?: return
                openSingleDoor(player, obj, rightOpenedId)
            }
            return
        }

        // Check if it's a gate extension (opened) - clicking opened extension should close the gate
        // Performance: Use reverse lookup map instead of iterating all configs
        val gateConfigForExtension = gateExtensionOpenedToConfig[objId]
        if (gateConfigForExtension != null) {
            val hingeOpenedId = gateConfigForExtension.opened["hinge"] ?: return
            val hinge = findAdjacentDoor(world, obj.tile, hingeOpenedId)
            if (hinge != null) {
                // Close the entire gate (both hinge and extension)
                closeGate(player, hinge, gateConfigForExtension)
            } else {
                // Hinge not found, but we can still close this extension directly
                val extensionClosedId = gateConfigForExtension.closed["extension"] ?: return
                closeSingleDoor(player, obj, extensionClosedId)
            }
            return
        }
    }

    private fun openSingleDoor(player: org.alter.game.model.entity.Player, obj: GameObject, openedId: Int, invertTransform: Boolean = false) {
        val oldRot = obj.rot
        val diagonal = obj.type == WALL_DIAGONAL

        // Check if this door has stored opened state (was closed from opened state)
        val storedOpenedTile = obj.attr[OPENED_TILE_ATTR]
        val storedOpenedRot = obj.attr[OPENED_ROTATION_ATTR]

        val newRot: Int
        val newTile: org.alter.game.model.Tile

        if (storedOpenedTile != null && storedOpenedRot != null) {
            // Door was closed from opened state - restore exact opened state
            newRot = storedOpenedRot
            newTile = storedOpenedTile
        } else {
            // Door started closed - calculate opened position
            if (invertTransform) {
                // For double doors left door, calculate based on door orientation
                // Vertical doors (north<>south, rot 0 or 2): move south and use rot 2 (old logic - works)
                // Horizontal doors (east<>west, rot 1 or 3):
                //   - rot 1 (east): move north and use rot 0 (new logic - fixed)
                //   - rot 3 (west): move south and use rot 2 (old logic - keep working)
                newRot = when (oldRot) {
                    0, 2 -> 2 // Vertical doors: orientation 2 (south)
                    1 -> 0     // Horizontal door facing east: orientation 0 (north)
                    3 -> 2     // Horizontal door facing west: orientation 2 (south) - old logic
                    else -> 2
                }
                newTile = when (oldRot) {
                    0, 2 -> obj.tile.transform(0, -1) // Vertical doors: move 1 tile south
                    1 -> obj.tile.transform(0, 1)      // Horizontal door facing east: move 1 tile north
                    3 -> obj.tile.transform(0, -1)     // Horizontal door facing west: move 1 tile south (old logic)
                    else -> obj.tile.transform(0, -1)
                }
            } else {
                // Right door or single door - normal rotation
                newRot = abs((oldRot + 1) and 0x3)
                newTile = when (oldRot) {
                    0 -> if (diagonal) obj.tile.transform(0, 1) else obj.tile.transform(-1, 0)
                    1 -> if (diagonal) obj.tile.transform(1, 0) else obj.tile.transform(0, 1)
                    2 -> if (diagonal) obj.tile.transform(0, -1) else obj.tile.transform(1, 0)
                    3 -> if (diagonal) obj.tile.transform(-1, 0) else obj.tile.transform(0, -1)
                    else -> obj.tile
                }
            }
        }

        // Get the opened door ID as RSCM string
        val openedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, openedId)
            ?: run {
                player.message("Unable to open door.")
                return
            }

        // Remove old door collision and object
        val oldDef = obj.getDef()
        world.collision.removeLoc(obj, oldDef)
        world.remove(obj)
        doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

        // Spawn new opened door
        val newDoor = DynamicObject(
            id = openedRscm,
            type = obj.type,
            rot = newRot,
            tile = newTile
        )

        world.spawn(newDoor)
        val newDef = newDoor.getDef()
        world.collision.addLoc(newDoor, newDef)
        doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

        // Store the original state (tile and rotation) so we can restore it exactly when closing
        newDoor.attr[ORIGINAL_ROTATION_ATTR] = oldRot
        newDoor.attr[ORIGINAL_TILE_ATTR] = obj.tile

        // Store default state (closed) and set timer for auto-restore
        newDoor.attr[DEFAULT_STATE_ID_ATTR] = obj.internalID // The closed door ID is the default
        newDoor.setTimer(DOOR_AUTO_RESTORE_TIMER, 300) // 300 seconds = 300 ticks
        doorsWithTimer.add(newDoor) // Performance: Track door for efficient timer checking
    }

    private fun openDoubleDoor(player: org.alter.game.model.entity.Player, leftDoor: GameObject, config: DoubleDoorIdConfig) {
        val leftOpenedId = config.opened["left"] ?: return
        val rightOpenedId = config.opened["right"] ?: return

        // Open left door (with invertTransform=true to use orientation 2 and move 1 tile south)
        openSingleDoor(player, leftDoor, leftOpenedId, invertTransform = true)

        // Find and open right door
        val rightClosedId = config.closed["right"] ?: return
        val rightDoor = findAdjacentDoor(world, leftDoor.tile, rightClosedId)
        if (rightDoor != null) {
            openSingleDoor(player, rightDoor, rightOpenedId)
        }
    }

    private fun openGate(player: org.alter.game.model.entity.Player, hinge: GameObject, config: GateIdConfig) {
        val hingeOpenedId = config.opened["hinge"] ?: return
        val extensionOpenedId = config.opened["extension"] ?: return

        // For gates, the hinge (right part, _r) stays in place, only changes rotation
        // Store original state
        val originalHingeTile = hinge.tile
        val originalHingeRot = hinge.rot

        // Calculate opened rotation for hinge (rotate 90 degrees clockwise)
        // For gates, we rotate 90 degrees: 0->1, 1->2, 2->3, 3->0
        val hingeOpenedRot = (hinge.rot + 1) and 0x3

        // Get the opened hinge ID as RSCM string
        val hingeOpenedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, hingeOpenedId)
            ?: run {
                player.message("Unable to open gate.")
                return
            }

        // Remove closed hinge
        val oldHingeDef = hinge.getDef()
        world.collision.removeLoc(hinge, oldHingeDef)
        world.remove(hinge)

        // Calculate shift direction based on gate's closed orientation
        // Shift perpendicular to the gate's closed orientation to align with the wall
        val shiftTile = when (originalHingeRot) {
            0 -> originalHingeTile.transform(-1, 0)   // Gate facing north (horizontal), shift west
            1 -> originalHingeTile.transform(0, 1)     // Gate facing east (vertical), shift north
            2 -> originalHingeTile.transform(1, 0)    // Gate facing south (horizontal), shift east
            3 -> originalHingeTile.transform(0, -1)    // Gate facing west (vertical), shift south
            else -> originalHingeTile
        }
        val hingeOpenedTile = shiftTile
        val newHinge = DynamicObject(
            id = hingeOpenedRscm,
            type = hinge.type,
            rot = hingeOpenedRot,
            tile = hingeOpenedTile  // Hinge shifted one tile up
        )
        world.spawn(newHinge)
        val newHingeDef = newHinge.getDef()
        world.collision.addLoc(newHinge, newHingeDef)

        // Store original state for closing
        newHinge.attr[ORIGINAL_TILE_ATTR] = originalHingeTile
        newHinge.attr[ORIGINAL_ROTATION_ATTR] = originalHingeRot

        // Find and open extension (left part, _l)
        val extensionClosedId = config.closed["extension"] ?: return
        val extension = findAdjacentDoor(world, originalHingeTile, extensionClosedId)
        if (extension != null) {
            val originalExtensionTile = extension.tile
            val originalExtensionRot = extension.rot

            // Calculate where the extension should move to (next tile after hinge based on rotation)
            // The extension moves to be adjacent to the hinge in the opposite direction the gate opens
            // Both are shifted one tile up (north) to align with the wall
            val extensionOpenedTile = when (hingeOpenedRot) {
                0 -> hingeOpenedTile.transform(0, -1) // South (opposite of North)
                1 -> hingeOpenedTile.transform(-1, 0)  // West (opposite of East)
                2 -> hingeOpenedTile.transform(0, 1)   // North (opposite of South)
                3 -> hingeOpenedTile.transform(1, 0)    // East (opposite of West)
                else -> hingeOpenedTile
            }
            val extensionOpenedRot = hingeOpenedRot

            // Get the opened extension ID as RSCM string
            val extensionOpenedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, extensionOpenedId)
                ?: return

            // Remove closed extension
            val oldExtensionDef = extension.getDef()
            world.collision.removeLoc(extension, oldExtensionDef)
            world.remove(extension)

            // Spawn opened extension at new position
            val newExtension = DynamicObject(
                id = extensionOpenedRscm,
                type = extension.type,
                rot = extensionOpenedRot,
                tile = extensionOpenedTile  // Extension moves to next tile after hinge
            )
            world.spawn(newExtension)
            val newExtensionDef = newExtension.getDef()
            world.collision.addLoc(newExtension, newExtensionDef)

            // Store original state for closing
            newExtension.attr[ORIGINAL_TILE_ATTR] = originalExtensionTile
            newExtension.attr[ORIGINAL_ROTATION_ATTR] = originalExtensionRot

            // Store opened state on both parts (using shifted positions)
            newHinge.attr[OPENED_TILE_ATTR] = hingeOpenedTile
            newHinge.attr[OPENED_ROTATION_ATTR] = hingeOpenedRot
            newExtension.attr[OPENED_TILE_ATTR] = extensionOpenedTile
            newExtension.attr[OPENED_ROTATION_ATTR] = extensionOpenedRot

            // Set default state and timers
            val defaultStateId = config.closed["hinge"] ?: return
            newHinge.attr[DEFAULT_STATE_ID_ATTR] = defaultStateId
            newExtension.attr[DEFAULT_STATE_ID_ATTR] = defaultStateId
            newHinge.setTimer(DOOR_AUTO_RESTORE_TIMER, 300)
            newExtension.setTimer(DOOR_AUTO_RESTORE_TIMER, 300)
            doorsWithTimer.add(newHinge) // Performance: Track door for efficient timer checking
            doorsWithTimer.add(newExtension) // Performance: Track door for efficient timer checking
        }
    }

    private fun findAdjacentDoor(world: org.alter.game.model.World, tile: org.alter.game.model.Tile, doorId: Int): GameObject? {
        // Check the same tile first
        val chunk = world.chunks.get(tile, createIfNeeded = false) ?: return null
        chunk.getEntities<GameObject>(tile, org.alter.game.model.EntityType.STATIC_OBJECT, org.alter.game.model.EntityType.DYNAMIC_OBJECT)
            .firstOrNull { it.internalID == doorId }
            ?.let { return it }

        // Check adjacent tiles (within 2 tiles for gates/doors)
        for (dx in -2..2) {
            for (dz in -2..2) {
                if (dx == 0 && dz == 0) continue
                val checkTile = tile.transform(dx, dz)
                val checkChunk = world.chunks.get(checkTile, createIfNeeded = false) ?: continue
                checkChunk.getEntities<GameObject>(checkTile, org.alter.game.model.EntityType.STATIC_OBJECT, org.alter.game.model.EntityType.DYNAMIC_OBJECT)
                    .firstOrNull { it.internalID == doorId }
                    ?.let { return it }
            }
        }

        return null
    }

    private fun handleCloseOption(player: org.alter.game.model.entity.Player, obj: GameObject) {
        val objId = obj.internalID

        // First check if it's already closed (in forward maps) - if so, it's already closed, do nothing or open it
        val openedId = singleDoors[objId]
        if (openedId != null) {
            return
        }

        // Check if it's a single door (opened)
        val closedId = singleDoorsReverse[objId]
        if (closedId != null) {
            closeSingleDoor(player, obj, closedId)
            return
        }

        // Check if it's a double door (closed left side) - already closed
        val doubleDoorConfigForward = doubleDoors[objId]
        if (doubleDoorConfigForward != null) {
            return
        }

        // Check if it's a double door (opened left side)
        val doubleDoorConfig = doubleDoorsReverse[objId]
        if (doubleDoorConfig != null) {
            closeDoubleDoor(player, obj, doubleDoorConfig)
            return
        }

        // Check if it's a gate (closed) - already closed
        val gateConfigForward = gates[objId]
        if (gateConfigForward != null) {
            return
        }

        // Check if it's a gate (opened hinge or extension)
        val gateConfig = gatesReverse[objId]
        if (gateConfig != null) {
            // Check if this is the hinge or extension
            val isHinge = gateConfig.opened["hinge"] == objId
            val isExtension = gateConfig.opened["extension"] == objId

            if (isHinge) {
                // This is the hinge, close the gate directly
                closeGate(player, obj, gateConfig)
                return
            } else if (isExtension) {
                // This is the extension, find the hinge and close the gate
                val hingeOpenedId = gateConfig.opened["hinge"] ?: return
                val hinge = findAdjacentDoor(world, obj.tile, hingeOpenedId)
                if (hinge != null) {
                    closeGate(player, hinge, gateConfig)
                } else {
                    // Hinge not found, close extension directly
                    val extensionClosedId = gateConfig.closed["extension"] ?: return
                    closeSingleDoor(player, obj, extensionClosedId)
                }
                return
            }
        }

        // Check if it's a double door (opened right side) - need to find the left side
        // Performance: Use reverse lookup map instead of iterating all configs
        val doubleDoorConfigForRightOpened = doubleDoorRightOpenedToConfig[objId]
        if (doubleDoorConfigForRightOpened != null) {
            // Find the left door at the same location or adjacent
            val leftOpenedId = doubleDoorConfigForRightOpened.opened["left"] ?: return
            val leftDoor = findAdjacentDoor(world, obj.tile, leftOpenedId)
            if (leftDoor != null) {
                closeDoubleDoor(player, leftDoor, doubleDoorConfigForRightOpened)
                // Also close this right door
                val rightClosedId = doubleDoorConfigForRightOpened.closed["right"] ?: return
                closeSingleDoor(player, obj, rightClosedId)
            }
            return
        }
    }

    private fun closeSingleDoor(player: org.alter.game.model.entity.Player, obj: GameObject, closedId: Int) {
        // Check if this door has stored original state (was opened from closed state)
        val originalTile = obj.attr[ORIGINAL_TILE_ATTR]
        val originalRot = obj.attr[ORIGINAL_ROTATION_ATTR]

        // If no stored state, this door started opened - store its opened state for later restoration
        val closedTile: org.alter.game.model.Tile
        val closedRot: Int

        if (originalTile != null && originalRot != null) {
            // Door was opened from closed state - restore original closed state
            closedTile = originalTile
            closedRot = originalRot
        } else {
            // Door started opened - calculate closed position based on opened orientation
            // Move one tile back relative to the opened orientation
            closedRot = abs((obj.rot - 1) and 0x3) // Reverse the opening rotation

            // Calculate closed tile by moving one tile back relative to opened rotation
            // For rotation 1 (east), move east (x+1) as specified by user
            closedTile = when (obj.rot) {
                0 -> obj.tile.transform(0, 1) // Move north (z+1)
                1 -> obj.tile.transform(1, 0) // Move east (x+1) - as user specified
                2 -> obj.tile.transform(0, -1) // Move south (z-1)
                3 -> obj.tile.transform(-1, 0) // Move west (x-1)
                else -> obj.tile
            }
        }

        // Get the closed door ID as RSCM string
        val closedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, closedId)
            ?: run {
                player.message("Unable to close door.")
                return
            }

        // Remove opened door collision and object
        val oldDef = obj.getDef()
        world.collision.removeLoc(obj, oldDef)
        world.remove(obj)

        // Spawn new closed door at exact original position and rotation
        val newDoor = DynamicObject(
            id = closedRscm,
            type = obj.type,
            rot = closedRot,
            tile = closedTile
        )

        world.spawn(newDoor)
        val newDef = newDoor.getDef()
        world.collision.addLoc(newDoor, newDef)
        doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

        // Store the opened state on the closed door so we can restore it when reopening
        // This applies to both: doors that started opened, and doors that were opened from closed state
        newDoor.attr[OPENED_ROTATION_ATTR] = obj.rot
        newDoor.attr[OPENED_TILE_ATTR] = obj.tile

        // Determine default state and set timer for auto-restore
        // If door started opened (no original state), default is opened. Otherwise default is closed.
        val defaultStateId = if (originalTile == null || originalRot == null) {
            obj.internalID // Door started opened, so opened is default
        } else {
            closedId // Door was opened from closed, so closed is default
        }
        newDoor.attr[DEFAULT_STATE_ID_ATTR] = defaultStateId
        newDoor.setTimer(DOOR_AUTO_RESTORE_TIMER, 300) // 300 seconds = 300 ticks
        doorsWithTimer.add(newDoor) // Performance: Track door for efficient timer checking
    }

    private fun restoreDoorToDefaultState(obj: GameObject) {
        val defaultStateId = obj.attr[DEFAULT_STATE_ID_ATTR] ?: return

        // Check if this is the default state already
        if (obj.internalID == defaultStateId) {
            obj.removeTimer(DOOR_AUTO_RESTORE_TIMER)
            doorsWithTimer.remove(obj) // Performance: Remove from tracking
            return // Already in default state
        }

        // Determine if we need to open or close
        val isCurrentlyOpened = singleDoorsReverse.containsKey(obj.internalID) ||
                doubleDoorsReverse.containsKey(obj.internalID) ||
                gatesReverse.containsKey(obj.internalID)

        val shouldBeOpened = singleDoorsReverse.containsKey(defaultStateId) ||
                doubleDoorsReverse.containsKey(defaultStateId) ||
                gatesReverse.containsKey(defaultStateId)

        if (isCurrentlyOpened && !shouldBeOpened) {
            // Currently opened, need to close - use stored original state
            val originalTile = obj.attr[ORIGINAL_TILE_ATTR] ?: return
            val originalRot = obj.attr[ORIGINAL_ROTATION_ATTR] ?: return

            // Check if this is a gate (hinge or extension)
            val gateConfig = gatesReverse[obj.internalID]
            if (gateConfig != null) {
                // This is a gate - need to restore both hinge and extension
                val isHinge = gateConfig.opened["hinge"] == obj.internalID
                val isExtension = gateConfig.opened["extension"] == obj.internalID

                if (isHinge) {
                    // Restore the hinge
                    val closedHingeId = gateConfig.closed["hinge"] ?: return
                    val closedHingeRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, closedHingeId) ?: return
                    val oldDef = obj.getDef()
                    world.collision.removeLoc(obj, oldDef)
                    world.remove(obj)
                    doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

                    val newHinge = DynamicObject(
                        id = closedHingeRscm,
                        type = obj.type,
                        rot = originalRot,
                        tile = originalTile
                    )
                    world.spawn(newHinge)
                    val newDef = newHinge.getDef()
                    world.collision.addLoc(newHinge, newDef)
                    newHinge.attr[OPENED_ROTATION_ATTR] = obj.rot
                    newHinge.attr[OPENED_TILE_ATTR] = obj.tile

                    // Also restore the extension if it exists
                    val extensionOpenedId = gateConfig.opened["extension"]
                    if (extensionOpenedId != null) {
                        val extension = findAdjacentDoor(world, obj.tile, extensionOpenedId)
                        if (extension != null) {
                            val extensionOriginalTile = extension.attr[ORIGINAL_TILE_ATTR] ?: extension.tile
                            val extensionOriginalRot = extension.attr[ORIGINAL_ROTATION_ATTR] ?: extension.rot
                            val extensionClosedId = gateConfig.closed["extension"] ?: return
                            val extensionClosedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, extensionClosedId) ?: return
                            val extOldDef = extension.getDef()
                            world.collision.removeLoc(extension, extOldDef)
                            world.remove(extension)
                            doorsWithTimer.remove(extension) // Performance: Remove old door from tracking

                            val newExtension = DynamicObject(
                                id = extensionClosedRscm,
                                type = extension.type,
                                rot = extensionOriginalRot,
                                tile = extensionOriginalTile
                            )
                            world.spawn(newExtension)
                            val extNewDef = newExtension.getDef()
                            world.collision.addLoc(newExtension, extNewDef)
                        }
                    }
                } else if (isExtension) {
                    // Extension - restore it, but also check if hinge needs restoring
                    val closedExtensionId = gateConfig.closed["extension"] ?: return
                    val closedExtensionRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, closedExtensionId) ?: return
                    val oldDef = obj.getDef()
                    world.collision.removeLoc(obj, oldDef)
                    world.remove(obj)
                    doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

                    val newExtension = DynamicObject(
                        id = closedExtensionRscm,
                        type = obj.type,
                        rot = originalRot,
                        tile = originalTile
                    )
                    world.spawn(newExtension)
                    val newDef = newExtension.getDef()
                    world.collision.addLoc(newExtension, newDef)
                    newExtension.attr[OPENED_ROTATION_ATTR] = obj.rot
                    newExtension.attr[OPENED_TILE_ATTR] = obj.tile
                }
                return
            }

            // Not a gate, handle as regular door
            val closedId = singleDoorsReverse[obj.internalID]
                ?: doubleDoorsReverse[obj.internalID]?.closed?.values?.firstOrNull()
                ?: return

            // Restore to stored original state
            val closedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, closedId) ?: return
            val oldDef = obj.getDef()
            world.collision.removeLoc(obj, oldDef)
            world.remove(obj)
            doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

            val newDoor = DynamicObject(
                id = closedRscm,
                type = obj.type,
                rot = originalRot,
                tile = originalTile
            )
            world.spawn(newDoor)
            val newDef = newDoor.getDef()
            world.collision.addLoc(newDoor, newDef)

            // Store opened state on the closed door for future reopening
            newDoor.attr[OPENED_ROTATION_ATTR] = obj.rot
            newDoor.attr[OPENED_TILE_ATTR] = obj.tile
        } else if (!isCurrentlyOpened && shouldBeOpened) {
            // Currently closed, need to open - use stored opened state
            val openedTile = obj.attr[OPENED_TILE_ATTR] ?: return
            val openedRot = obj.attr[OPENED_ROTATION_ATTR] ?: return

            // Check if this is a gate (hinge or extension)
            val gateConfig = gates[obj.internalID]
            if (gateConfig != null) {
                // This is a gate - need to restore both hinge and extension
                val isHinge = gateConfig.closed["hinge"] == obj.internalID
                val isExtension = gateConfig.closed["extension"] == obj.internalID

                if (isHinge) {
                    // Restore the hinge
                    val openedHingeId = gateConfig.opened["hinge"] ?: return
                    val openedHingeRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, openedHingeId) ?: return
                    val oldDef = obj.getDef()
                    world.collision.removeLoc(obj, oldDef)
                    world.remove(obj)

                    // Calculate shift direction based on gate's closed orientation
                    // Shift perpendicular to the gate's closed orientation to align with the wall
                    val originalRot = obj.rot
                    val shiftedTile = when (originalRot) {
                        0 -> obj.tile.transform(-1, 0)   // Gate facing north (horizontal), shift west
                        1 -> obj.tile.transform(0, 1)    // Gate facing east (vertical), shift north
                        2 -> obj.tile.transform(1, 0)    // Gate facing south (horizontal), shift east
                        3 -> obj.tile.transform(0, -1)   // Gate facing west (vertical), shift south
                        else -> openedTile
                    }

                    val newHinge = DynamicObject(
                        id = openedHingeRscm,
                        type = obj.type,
                        rot = openedRot,
                        tile = shiftedTile
                    )
                    world.spawn(newHinge)
                    val newDef = newHinge.getDef()
                    world.collision.addLoc(newHinge, newDef)
                    newHinge.attr[ORIGINAL_ROTATION_ATTR] = obj.rot
                    newHinge.attr[ORIGINAL_TILE_ATTR] = obj.tile

                    // Also restore the extension if it exists
                    val extensionClosedId = gateConfig.closed["extension"]
                    if (extensionClosedId != null) {
                        val extension = findAdjacentDoor(world, obj.tile, extensionClosedId)
                        if (extension != null) {
                            // Calculate extension opened position based on hinge opened rotation
                            // The extension moves to be adjacent to the hinge in the opposite direction the gate opens
                            // Both are shifted relative to the gate's closed orientation to align with the wall
                            // Note: shiftedTile is already calculated based on original rotation
                            val extensionOpenedTile = when (openedRot) {
                                0 -> shiftedTile.transform(0, -1) // South (opposite of North)
                                1 -> shiftedTile.transform(-1, 0)  // West (opposite of East)
                                2 -> shiftedTile.transform(0, 1)   // North (opposite of South)
                                3 -> shiftedTile.transform(1, 0)    // East (opposite of West)
                                else -> extension.attr[OPENED_TILE_ATTR] ?: extension.tile
                            }
                            val extensionOpenedRot = openedRot
                            val extensionOpenedId = gateConfig.opened["extension"] ?: return
                            val extensionOpenedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, extensionOpenedId) ?: return
                            val extOldDef = extension.getDef()
                            world.collision.removeLoc(extension, extOldDef)
                            world.remove(extension)
                            doorsWithTimer.remove(extension) // Performance: Remove old door from tracking

                            val newExtension = DynamicObject(
                                id = extensionOpenedRscm,
                                type = extension.type,
                                rot = extensionOpenedRot,
                                tile = extensionOpenedTile
                            )
                            world.spawn(newExtension)
                            val extNewDef = newExtension.getDef()
                            world.collision.addLoc(newExtension, extNewDef)

                            // Store original state for closing
                            newExtension.attr[ORIGINAL_TILE_ATTR] = extension.tile
                            newExtension.attr[ORIGINAL_ROTATION_ATTR] = extension.rot

                            // Store opened state
                            newExtension.attr[OPENED_TILE_ATTR] = extensionOpenedTile
                            newExtension.attr[OPENED_ROTATION_ATTR] = extensionOpenedRot
                        }
                    }
                } else if (isExtension) {
                    // Extension - restore it, but also check if hinge needs restoring
                    val openedExtensionId = gateConfig.opened["extension"] ?: return
                    val openedExtensionRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, openedExtensionId) ?: return

                    // Find the hinge to get its opened state
                    val hingeClosedId = gateConfig.closed["hinge"] ?: return
                    val hinge = findAdjacentDoor(world, obj.tile, hingeClosedId)
                    val hingeOpenedRot = if (hinge != null && hinge.attr[OPENED_ROTATION_ATTR] != null) {
                        hinge.attr[OPENED_ROTATION_ATTR] as Int
                    } else {
                        // Calculate based on extension's opened rotation
                        openedRot
                    }
                    // Calculate shift direction based on gate's closed orientation
                    val originalRot = obj.rot
                    val (shiftX, shiftZ) = when (originalRot) {
                        0 -> Pair(-1, 0)   // Gate facing north (horizontal), shift west
                        1 -> Pair(0, 1)    // Gate facing east (vertical), shift north
                        2 -> Pair(1, 0)    // Gate facing south (horizontal), shift east
                        3 -> Pair(0, -1)   // Gate facing west (vertical), shift south
                        else -> Pair(0, 0)
                    }

                    val hingeOpenedTile = if (hinge != null && hinge.attr[OPENED_TILE_ATTR] != null) {
                        hinge.attr[OPENED_TILE_ATTR] as org.alter.game.model.Tile // Use stored shifted position
                    } else if (hinge != null) {
                        hinge.tile.transform(shiftX, shiftZ) // Shift based on orientation if no stored position
                    } else {
                        // Calculate based on extension's position (extension is already shifted)
                        val extensionShifted = when (openedRot) {
                            0 -> openedTile.transform(0, -1)  // South of extension
                            1 -> openedTile.transform(-1, 0)   // West of extension
                            2 -> openedTile.transform(0, 1)     // North of extension
                            3 -> openedTile.transform(1, 0)     // East of extension
                            else -> obj.tile
                        }
                        extensionShifted.transform(shiftX, shiftZ)
                    }

                    // Calculate extension opened position based on hinge opened rotation
                    // The extension moves to be adjacent to the hinge in the opposite direction the gate opens
                    // Both are shifted relative to the gate's closed orientation to align with the wall
                    val extensionOpenedTile = when (hingeOpenedRot) {
                        0 -> hingeOpenedTile.transform(0, -1) // South (opposite of North)
                        1 -> hingeOpenedTile.transform(-1, 0)  // West (opposite of East)
                        2 -> hingeOpenedTile.transform(0, 1)   // North (opposite of South)
                        3 -> hingeOpenedTile.transform(1, 0)    // East (opposite of West)
                        else -> openedTile
                    }

                    val oldDef = obj.getDef()
                    world.collision.removeLoc(obj, oldDef)
                    world.remove(obj)
                    doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

                    val newExtension = DynamicObject(
                        id = openedExtensionRscm,
                        type = obj.type,
                        rot = hingeOpenedRot,
                        tile = extensionOpenedTile
                    )
                    world.spawn(newExtension)
                    val newDef = newExtension.getDef()
                    world.collision.addLoc(newExtension, newDef)
                    newExtension.attr[ORIGINAL_ROTATION_ATTR] = obj.rot
                    newExtension.attr[ORIGINAL_TILE_ATTR] = obj.tile
                    newExtension.attr[OPENED_TILE_ATTR] = extensionOpenedTile
                    newExtension.attr[OPENED_ROTATION_ATTR] = hingeOpenedRot
                }
                return
            }

            // Not a gate, handle as regular door
            val openedId = singleDoors[obj.internalID]
                ?: doubleDoors[obj.internalID]?.opened?.values?.firstOrNull()
                ?: return

            // Restore to stored opened state
            val openedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, openedId) ?: return
            val oldDef = obj.getDef()
            world.collision.removeLoc(obj, oldDef)
            world.remove(obj)
            doorsWithTimer.remove(obj) // Performance: Remove old door from tracking

            val newDoor = DynamicObject(
                id = openedRscm,
                type = obj.type,
                rot = openedRot,
                tile = openedTile
            )
            world.spawn(newDoor)
            val newDef = newDoor.getDef()
            world.collision.addLoc(newDoor, newDef)

            // Store original state on the opened door for future closing
            newDoor.attr[ORIGINAL_ROTATION_ATTR] = obj.rot
            newDoor.attr[ORIGINAL_TILE_ATTR] = obj.tile
        }
    }

    private fun closeDoubleDoor(player: org.alter.game.model.entity.Player, leftDoor: GameObject, config: DoubleDoorIdConfig) {
        val leftClosedId = config.closed["left"] ?: return
        val rightClosedId = config.closed["right"] ?: return

        // Close left door
        closeSingleDoor(player, leftDoor, leftClosedId)

        // Find and close right door
        val rightOpenedId = config.opened["right"] ?: return
        val rightDoor = findAdjacentDoor(world, leftDoor.tile, rightOpenedId)
        if (rightDoor != null) {
            closeSingleDoor(player, rightDoor, rightClosedId)
        }
    }

    private fun closeGate(player: org.alter.game.model.entity.Player, hinge: GameObject, config: GateIdConfig) {
        val hingeClosedId = config.closed["hinge"] ?: return
        val extensionClosedId = config.closed["extension"] ?: return

        // Get original state
        val originalHingeTile = hinge.attr[ORIGINAL_TILE_ATTR] ?: hinge.tile
        val originalHingeRot = hinge.attr[ORIGINAL_ROTATION_ATTR] ?: hinge.rot

        // Get the closed hinge ID as RSCM string
        val hingeClosedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, hingeClosedId)
            ?: run {
                player.message("Unable to close gate.")
                return
            }

        // Remove opened hinge
        val oldHingeDef = hinge.getDef()
        world.collision.removeLoc(hinge, oldHingeDef)
        world.remove(hinge)
        doorsWithTimer.remove(hinge) // Performance: Remove old door from tracking

        // Spawn closed hinge at original position
        val newHinge = DynamicObject(
            id = hingeClosedRscm,
            type = hinge.type,
            rot = originalHingeRot,
            tile = originalHingeTile  // Hinge returns to original position
        )
        world.spawn(newHinge)
        val newHingeDef = newHinge.getDef()
        world.collision.addLoc(newHinge, newHingeDef)

        // Store opened state for reopening
        newHinge.attr[OPENED_TILE_ATTR] = hinge.tile
        newHinge.attr[OPENED_ROTATION_ATTR] = hinge.rot

        // Find and close extension
        val extensionOpenedId = config.opened["extension"] ?: return
        val extension = findAdjacentDoor(world, hinge.tile, extensionOpenedId)
        if (extension != null) {
            val originalExtensionTile = extension.attr[ORIGINAL_TILE_ATTR] ?: extension.tile
            val originalExtensionRot = extension.attr[ORIGINAL_ROTATION_ATTR] ?: extension.rot

            // Get the closed extension ID as RSCM string
            val extensionClosedRscm = RSCM.getReverseMapping(RSCMType.LOCTYPES, extensionClosedId)
                ?: return

            // Remove opened extension
            val oldExtensionDef = extension.getDef()
            world.collision.removeLoc(extension, oldExtensionDef)
            world.remove(extension)
            doorsWithTimer.remove(extension) // Performance: Remove old door from tracking

            // Spawn closed extension at original position
            val newExtension = DynamicObject(
                id = extensionClosedRscm,
                type = extension.type,
                rot = originalExtensionRot,
                tile = originalExtensionTile  // Extension returns to original position
            )
            world.spawn(newExtension)
            val newExtensionDef = newExtension.getDef()
            world.collision.addLoc(newExtension, newExtensionDef)

            // Store opened state for reopening
            newExtension.attr[OPENED_TILE_ATTR] = extension.tile
            newExtension.attr[OPENED_ROTATION_ATTR] = extension.rot
        }
    }
}
