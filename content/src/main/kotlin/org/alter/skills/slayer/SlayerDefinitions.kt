package org.alter.skills.slayer

import org.generated.tables.slayer.*

/**
 * Definitions for slayer tasks, masters, areas, and unlocks.
 * All data is loaded from cache tables via generated Row classes.
 */
object SlayerDefinitions {

    /**
     * All slayer task definitions from the cache.
     * Contains task names, level requirements, regions, etc.
     */
    val tasks: List<SlayerTaskRow> by lazy { SlayerTaskRow.all() }

    /**
     * All slayer master task assignments from the cache.
     * Links tasks to masters with weights, amounts, and area restrictions.
     */
    val masterTasks: List<SlayerMasterTaskRow> by lazy { SlayerMasterTaskRow.all() }

    /**
     * All slayer area definitions (for Konar tasks).
     */
    val areas: List<SlayerAreaRow> by lazy { SlayerAreaRow.all() }

    /**
     * All slayer unlock/reward definitions from the cache.
     */
    val unlocks: List<SlayerUnlockRow> by lazy { SlayerUnlockRow.all() }

    /**
     * Task sublists for variant monsters (e.g., different demon types).
     */
    val taskSublists: List<SlayerTaskSublistRow> by lazy { SlayerTaskSublistRow.all() }

    /**
     * Get tasks available for a specific slayer master.
     * @param masterId The master's ID (1=Turael, 2=Mazchna, etc.)
     */
    fun getTasksForMaster(masterId: Int): List<SlayerMasterTaskRow> {
        return masterTasks.filter { it.masterId == masterId }
    }

    /**
     * Get a task definition by its row ID.
     */
    fun getTaskById(taskId: Int): SlayerTaskRow? {
        return tasks.find { it.id == taskId }
    }

    /**
     * Get an area definition by its row ID.
     */
    fun getAreaById(areaId: Int): SlayerAreaRow? {
        return areas.find { it.areaId == areaId }
    }

    /**
     * Get an unlock definition by its varbit.
     */
    fun getUnlockByBit(bit: Int): SlayerUnlockRow? {
        return unlocks.find { it.bit == bit }
    }

    /**
     * Slayer master IDs mapped to their RSCM NPC keys.
     */
    enum class SlayerMaster(val id: Int, val npcKey: String, val displayName: String, val combatReq: Int, val slayerReq: Int) {
        TURAEL(1, "npcs.slayer_master_1", "Turael", 0, 0),
        SPRIA(1, "npcs.slayer_master_1", "Spria", 0, 0), // Shares Turael's task list
        MAZCHNA(2, "npcs.slayer_master_2", "Mazchna", 20, 0),
        VANNAKA(3, "npcs.slayer_master_3", "Vannaka", 40, 0),
        CHAELDAR(4, "npcs.slayer_master_4", "Chaeldar", 70, 0),
        KONAR(6, "npcs.slayer_master_6", "Konar quo Maten", 75, 0),
        NIEVE(7, "npcs.slayer_master_7", "Nieve", 85, 0),
        STEVE(7, "npcs.slayer_master_7", "Steve", 85, 0), // Same as Nieve
        DURADEL(5, "npcs.slayer_master_5", "Duradel", 100, 50),
        KRYSTILIA(8, "npcs.slayer_master_8", "Krystilia", 0, 0), // Wilderness master
        ;

        companion object {
            fun fromId(id: Int): SlayerMaster? = entries.find { it.id == id }
            fun fromNpcKey(key: String): SlayerMaster? = entries.find { it.npcKey == key }
        }
    }

    /**
     * Points awarded per task completion based on master.
     * Streak bonuses: 10th = 5x, 50th = 15x, 100th = 25x, 250th = 35x, 1000th = 50x
     */
    val POINTS_PER_MASTER = mapOf(
        1 to 0,    // Turael - no points
        2 to 2,    // Mazchna
        3 to 4,    // Vannaka
        4 to 10,   // Chaeldar
        5 to 15,   // Duradel
        6 to 18,   // Konar (extra for location restriction)
        7 to 12,   // Nieve/Steve
        8 to 25,   // Krystilia (wilderness bonus)
    )

    /**
     * Task streak milestones and their point multipliers.
     */
    val STREAK_MULTIPLIERS = mapOf(
        10 to 5,
        50 to 15,
        100 to 25,
        250 to 35,
        1000 to 50
    )

    /**
     * Calculate points for completing a task.
     */
    fun calculatePoints(masterId: Int, streak: Int): Int {
        val basePoints = POINTS_PER_MASTER[masterId] ?: 0
        if (basePoints == 0) return 0

        // Check for streak milestone
        val multiplier = STREAK_MULTIPLIERS.entries
            .filter { streak % it.key == 0 }
            .maxByOrNull { it.key }?.value ?: 1

        return basePoints * multiplier
    }
}

