package org.alter.skills.cooking.events

import dev.openrune.ServerCacheManager.getObject
import org.alter.api.EquipmentType
import org.alter.api.Skills
import org.alter.api.computeSkillingSuccess
import org.alter.api.ext.hasEquipped
import org.alter.game.model.entity.GameObject
import org.alter.game.model.entity.Player
import org.alter.skills.cooking.runtime.CookingAction
import org.alter.skills.cooking.runtime.OutcomeKind
import org.alter.skills.cooking.runtime.Trigger
import org.alter.skills.firemaking.ColoredLogs
import org.alter.rscm.RSCM.asRSCM
import kotlin.random.Random

/**
 * Utility functions for cooking events.
 */
object CookingUtils {

    /**
     * Types of cooking stations.
     */
    enum class CookStation {
        FIRE,
        RANGE,
        /** Hosidius Kitchen range — 5% reduced burn chance. */
        HOSIDIUS_RANGE,
        /** Lumbridge Castle range — reduced burn level for certain foods. */
        LUMBRIDGE_RANGE
    }

    /** IDs of spit-skewered items that require firemaking level to roast. */
    val spitSkeweredIds: Set<Int> = setOf(
        "items.spit_skewered_bird_meat",
        "items.spit_skewered_beast_meat",
        "items.spit_skewered_rabbit_meat",
        "items.spit_skewered_chompy"
    ).mapNotNull { key -> runCatching { key.asRSCM() }.getOrNull() }.toSet()

    /** Firemaking level required for spit roasting. */
    const val SPIT_ROAST_FIREMAKING_LEVEL: Int = 20

    private val fireObjectIds: Set<Int> by lazy { buildFireObjectIds() }

    /**
     * Object IDs for the Hosidius Kitchen range (5% burn reduction).
     * In OSRS this is object 21302.
     */
    private val hosidiusRangeIds: Set<Int> by lazy {
        listOfNotNull(
            runCatching { "objects.hosidius_range".asRSCM() }.getOrNull()
        ).toSet()
    }

    /**
     * Object IDs for the Lumbridge Castle range (reduced stop-burn for low-level foods).
     * In OSRS this is object 114 (post-Cook's Assistant).
     */
    private val lumbridgeRangeIds: Set<Int> by lazy {
        listOfNotNull(
            runCatching { "objects.lumbridge_range".asRSCM() }.getOrNull()
        ).toSet()
    }

    // -------------------------------------------------------
    // Cooking gauntlets: items affected & lowered stop-burn
    // -------------------------------------------------------

    /**
     * Items whose stop-burn level is lowered by the Cooking gauntlets.
     * Maps raw item RSCM key -> (gauntlet stop-burn fire, gauntlet stop-burn range).
     */
    private val gauntletStopBurnOverrides: Map<Int, Pair<Int, Int>> by lazy {
        listOf(
            "items.raw_lobster"    to (64 to 64),
            "items.raw_swordfish"  to (81 to 81),
            "items.raw_monkfish"   to (90 to 87),
            "items.raw_shark"      to (94 to 94),
            "items.raw_anglerfish" to (98 to 93)
        ).mapNotNull { (key, stops) ->
            val id = runCatching { key.asRSCM() }.getOrNull() ?: return@mapNotNull null
            id to stops
        }.toMap()
    }

    /** Checks whether the player is wearing cooking gauntlets. */
    fun hasCookingGauntlets(player: Player): Boolean =
        runCatching { player.hasEquipped(EquipmentType.GLOVES, "items.gauntlets_of_cooking") }.getOrDefault(false)

    /** Checks whether the player is wearing the cooking cape (or max cape). */
    fun hasCookingCape(player: Player): Boolean =
        runCatching {
            player.equipment.containsAny(
                "items.skillcape_cooking",
                "items.skillcape_cooking_trimmed",
                "items.skillcape_max"
            )
        }.getOrDefault(false)

    /**
     * Determines the cooking station type for a given game object, or null if it is not a heat source.
     */
    fun heatSourceStationFor(gameObject: GameObject): CookStation? {
        val id = gameObject.internalID
        if (fireObjectIds.contains(id)) return CookStation.FIRE

        // Check for special named ranges first
        if (hosidiusRangeIds.contains(id)) return CookStation.HOSIDIUS_RANGE
        if (lumbridgeRangeIds.contains(id)) return CookStation.LUMBRIDGE_RANGE

        val def = getObject(id) ?: return null
        val hasCookAction = def.actions.any { it?.equals("cook", ignoreCase = true) == true }
        return if (hasCookAction) CookStation.RANGE else null
    }

    fun isHeatSource(gameObject: GameObject): Boolean = heatSourceStationFor(gameObject) != null

    /**
     * Determines the cooking station type for a given game object.
     */
    fun stationFor(gameObject: GameObject): CookStation {
        return heatSourceStationFor(gameObject) ?: CookStation.RANGE
    }

    /**
     * Checks if a station type is allowed for the given action.
     * Hosidius and Lumbridge ranges count as regular ranges for permission checks.
     */
    fun isStationAllowed(action: CookingAction, station: CookStation): Boolean {
        val mask = action.row.stationMask
        return when (station) {
            CookStation.FIRE -> (mask and 1) != 0
            CookStation.RANGE,
            CookStation.HOSIDIUS_RANGE,
            CookStation.LUMBRIDGE_RANGE -> (mask and 2) != 0
        }
    }

    /**
     * Checks if the player meets extra requirements (e.g., firemaking for spit roasting).
     */
    fun meetsExtraRequirements(player: Player, action: CookingAction): Boolean {
        if (isSpitRoastAction(action)) {
            val fm = player.getSkills().getCurrentLevel(Skills.FIREMAKING)
            return fm >= SPIT_ROAST_FIREMAKING_LEVEL
        }
        return true
    }

    /**
     * Checks if this action is a spit-roast action (fire-only, skewered input).
     */
    fun isSpitRoastAction(action: CookingAction): Boolean {
        if (action.row.trigger != Trigger.HEAT_SOURCE) return false
        return action.inputs.any { it.item in spitSkeweredIds }
    }

    /**
     * Checks if the player has all required inputs for the action.
     */
    fun hasInputs(player: Player, action: CookingAction): Boolean {
        if (action.inputs.isEmpty()) return false
        return action.inputs.all { input -> player.inventory.getItemCount(input.item) >= input.count }
    }

    /**
     * Calculates the maximum number of items that can be produced.
     */
    fun maxProducible(player: Player, action: CookingAction): Int {
        if (action.inputs.isEmpty()) return 0
        return action.inputs
            .map { input -> player.inventory.getItemCount(input.item) / input.count.coerceAtLeast(1) }
            .minOrNull()
            ?: 0
    }

    /**
     * Rolls to determine if cooking succeeds.
     *
     * Uses the OSRS `statrandom` (computeSkillingSuccess) approach with per-food
     * low/high values. The cooking cape guarantees success. Cooking gauntlets lower
     * the stop-burn threshold for affected items.
     */
    fun rollCookSuccess(
        player: Player,
        station: CookStation,
        action: CookingAction
    ): Boolean {
        // Cooking cape / max cape → never burn
        if (hasCookingCape(player)) return true

        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)

        val wearingGauntlets = hasCookingGauntlets(player)

        // For stop-burn lookup, Hosidius/Lumbridge count as RANGE
        val baseStation = when (station) {
            CookStation.FIRE -> CookStation.FIRE
            else -> CookStation.RANGE
        }

        // Determine effective stop-burn level
        val stopBurn: Int = if (wearingGauntlets) {
            val override = gauntletStopBurnOverrides[action.key]
            if (override != null) {
                when (baseStation) {
                    CookStation.FIRE -> override.first
                    else -> override.second
                }
            } else {
                when (baseStation) {
                    CookStation.FIRE -> action.row.stopBurnFire
                    else -> action.row.stopBurnRange
                }
            }
        } else {
            when (baseStation) {
                CookStation.FIRE -> action.row.stopBurnFire
                else -> action.row.stopBurnRange
            }
        }.coerceAtLeast(action.row.level)

        if (cookingLevel >= stopBurn) return true

        // Use computeSkillingSuccess with the level requirement as the base
        // low=level requirement scaled, high=stop-burn scaled
        val effectiveLevel = cookingLevel.coerceIn(1, 99)
        val chance = computeSkillingSuccess(
            low = action.row.level,
            high = stopBurn,
            level = effectiveLevel
        )

        // Hosidius Kitchen range: 5% reduced burn chance (multiply success by 1.05)
        val adjustedChance = when (station) {
            CookStation.HOSIDIUS_RANGE -> (chance * 1.05).coerceAtMost(1.0)
            else -> chance
        }

        return Random.nextDouble() < adjustedChance
    }

    /**
     * Rolls wine fermentation success. Wine has no station; uses fire stop-burn values.
     * Cooking cape never burns. Gauntlets have no effect on wine.
     */
    fun rollWineSuccess(player: Player, action: CookingAction): Boolean {
        if (hasCookingCape(player)) return true

        val cookingLevel = player.getSkills().getCurrentLevel(Skills.COOKING)
        val stopBurn = action.row.stopBurnFire.coerceAtLeast(action.row.level)
        if (cookingLevel >= stopBurn) return true

        val chance = computeSkillingSuccess(
            low = action.row.level,
            high = stopBurn,
            level = cookingLevel.coerceIn(1, 99)
        )
        return Random.nextDouble() < chance
    }

    /**
     * Checks if the player is still at the cooking station.
     */
    fun isStillAtStation(
        player: Player,
        objectTile: org.alter.game.model.Tile,
        objectId: Int
    ): Boolean {
        if (player.tile.getDistance(objectTile) > 1) return false

        val world = player.world
        val obj = world.getObject(objectTile, type = 10) ?: world.getObject(objectTile, type = 11)
        return obj != null && obj.internalID == objectId
    }

    private fun buildFireObjectIds(): Set<Int> {
        val fireKeys = ColoredLogs.COLOURED_LOGS.values.map { it.second } +
            "objects.fire" +
            "objects.forestry_fire"

        return fireKeys.mapNotNull { key ->
            runCatching { key.asRSCM() }.getOrNull()
        }.toSet()
    }
}
