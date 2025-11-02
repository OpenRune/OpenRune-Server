package org.alter.skills.firemaking

import org.alter.rscm.RSCM.asRSCM

/**
 * Enum representing different colored logs used in firemaking.
 * Each entry contains the colored log item, its corresponding firelighter, and the fire object it creates.
 */
enum class ColoredLogs(
    val logItem: String,
    val firelighter: String,
    val fireObject: String,
    val campfireObject: String
) {
    BLUE(
        "items.blue_logs",
        "items.gnomish_firelighter_blue",
        "objects.blue_fire",
        "objects.forestry_fire_blue"
    ),
    GREEN(
        "items.green_logs",
        "items.gnomish_firelighter_green",
        "objects.green_fire",
        "objects.forestry_fire_green"
    ),
    PURPLE(
        "items.trail_logs_purple",
        "items.trail_gnomish_firelighter_purple",
        "objects.trail_purple_fire",
        "objects.forestry_fire_purple"
    ),
    RED(
        "items.red_logs",
        "items.gnomish_firelighter_red",
        "objects.red_fire",
        "objects.forestry_fire_red"),
    WHITE(
        "items.trail_logs_white",
        "items.trail_gnomish_firelighter_white",
        "objects.trail_white_fire",
        "objects.forestry_fire_white"
    );

    companion object {
        /**
         * Map of colored log RSCM IDs to their firelighters and fire objects.
         */
        val COLOURED_LOGS: Map<Int, Pair<String, String>> = ColoredLogs.entries.associate {
            it.logItem.asRSCM() to (it.firelighter to it.fireObject)
        }

        /**
         * Check if an item ID is a colored log.
         */
        fun isColoredLog(itemId: Int): Boolean {
            return COLOURED_LOGS.containsKey(itemId)
        }

        /**
         * Map of campfire type indices to campfire objects.
         * Index 0-4 are colored campfires, 5 is the default campfire.
         */
        val CAMPFIRE_OBJECTS: Map<Int, String> = buildMap {
            ColoredLogs.entries.forEachIndexed { index, coloredLog ->
                put(index, coloredLog.campfireObject)
            }
            put(ColoredLogs.entries.size, "objects.forestry_fire")
        }
    }
}
